package com.example.servicelab

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

// Вспомогательный объект для запуска и остановки StopwatchService
object ServiceLauncher {

    // Запуск сервиса
    fun start(context: Context) {
        Log.d("StopwatchService", "ServiceLauncher.start вызван")
        val intent = Intent(context, StopwatchService::class.java)
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (e: IllegalStateException) {
            // Сюда попадает и ForegroundServiceStartNotAllowedException (Android 12+)
            Log.e(StopwatchService.TAG, "Запуск запрещён: ${e::class.simpleName}: ${e.message}")
        }
    }

    // Остановка сервиса
    fun stop(context: Context) {
        context.stopService(Intent(context, StopwatchService::class.java))
    }
}