package com.example.servicelab

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Lab() } }
    }
}

@Composable
fun Lab() {
    val context = LocalContext.current.applicationContext
    val seconds by StopwatchState.seconds.collectAsState()
    val running by StopwatchState.running.collectAsState()

    // Запрос разрешения у пользователя
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    // Для работы спрашиваем сразу. В реальном приложении запрашивается в нужный момент
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "%02d:%02d".format(seconds / 60, seconds % 60),
            style = MaterialTheme.typography.displayLarge
        )
        Text(if (running) "Сервис работает" else "Сервис остановлен")

        Button(onClick = { ServiceLauncher.start(context) }) { Text("Старт") }
        Button(onClick = { ServiceLauncher.stop(context) }) { Text("Стоп") }

        // Эксперимент с ограничениями: нажать и свернуть приложение
        Button(onClick = {
            Handler(Looper.getMainLooper()).postDelayed(
                { ServiceLauncher.start(context) }, 15_000
            )
        }) { Text("Старт через 15 с (сверните приложение)") }
    }
}