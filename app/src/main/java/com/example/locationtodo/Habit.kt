package com.example.locationtodo

data class Habit(
    val id: String,
    val name: String,
    val completedDates: Set<String>
)
