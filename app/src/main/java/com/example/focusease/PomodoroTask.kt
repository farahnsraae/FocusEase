package com.example.focusease.ui.pomodoro

data class PomodoroTask(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val estimatedPomodoros: Int,
    var completedPomodoros: Int = 0,
    var isCompleted: Boolean = false
) {
    fun incrementCompleted() {
        if (completedPomodoros < estimatedPomodoros) {
            completedPomodoros++
            if (completedPomodoros >= estimatedPomodoros) {
                isCompleted = true
            }
        }
    }

    fun getProgress(): Float {
        return if (estimatedPomodoros > 0) {
            completedPomodoros.toFloat() / estimatedPomodoros.toFloat()
        } else {
            0f
        }
    }
}