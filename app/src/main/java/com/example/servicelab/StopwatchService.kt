package com.example.servicelab

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StopwatchService : Service() {
    // Запуск корутины
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Ссылка на конкретную задачу
    private var tickJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        // Показываем что произошло, и в каком потоке находимся
        Log.d(TAG, "onCreate (поток: ${Thread.currentThread().name})")
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Выводим информацию: какое действие передано через Intent и какой
        // идентификатор запуска получила служба
        Log.d(TAG, "onStartCommand action=${intent?.action} startId=$startId")

        if (intent?.action == ACTION_STOP) {
            // Убирает уведомление и режим переднего плана
            stopForeground(STOP_FOREGROUND_REMOVE)
            // Остановка службы
            stopSelf()
            // Возвращает режим поведения при неожиданном завершении
            return START_NOT_STICKY
        }

        // 1. Сразу переводим сервис foreground. Нельзя откладывать:
        // после startForegroundService() на это даётся около 5 секунд
        startInForeground(buildNotification(0))

        //2. Повторные нажатия "Старт" не должны запускать второй таймер
        if (tickJob?.isActive != true) {
            // Получаем время с момента последней загрузки устройства
            val startElapsed = SystemClock.elapsedRealtime()
            StopwatchState.running.value = true

            tickJob = scope.launch {
                while (isActive) {
                    // Считаем от реальных часов, а не прибавляем +1 на каждом тике:
                    // delay() не точен и может "спать" вместе с процессом
                    val seconds = (SystemClock.elapsedRealtime() - startElapsed) / 1_000

                    StopwatchState.seconds.value = seconds

                    // Обновляем уведомление, показывающее текущее время секундомера
                    getSystemService(NotificationManager::class.java)
                        .notify(NOTIFICATION_ID, buildNotification(seconds))

                    delay(1_000)
                }
            }
        }
        return START_NOT_STICKY
    }

    // Работа секундомера в фоне
    private fun startInForeground(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= 34) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    // Остановка секундомера из уведомлений
    private fun buildNotification(seconds: Long): Notification {
        /* PendingIntent — это объект, который позволяет системе Android
            выполнить действие от имени приложения позднее например
            после нажатия кнопки в уведомлении. */

        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, StopwatchService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Секундомер")
            .setContentText("Прошло: %02d:%02d".format(seconds / 60, seconds % 60))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_pause, "Стоп", stopIntent)
            .build()
    }

    // Канал уведомления низкой важности (без звука и всплывающего уведомления)
    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Секундомер", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        scope.cancel() // Отменяем корутины
        StopwatchState.running.value = false
        super.onDestroy()
    }

    companion object {
        const val TAG = "StopwatchService"
        const val ACTION_STOP = "com.example.servicelab.ACTION_STOP"
        private const val CHANNEL_ID = "stopwatch"
        private const val NOTIFICATION_ID = 42
    }
}