package com.example.servicelab

import kotlinx.coroutines.flow.MutableStateFlow

// Пример общего состояния (в реальном проекте repository)
object StopwatchState {
    val seconds = MutableStateFlow(0L)
    val running = MutableStateFlow(false)
}