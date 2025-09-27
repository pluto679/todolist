package com.example.todolist

import java.io.Serializable
import java.util.Date

data class Task(
    var content: String,
    val createdAt: Date = Date(),
    var quadrant: Int = 0,
    var scheduledTime: Date = Date(), // 任务设置的执行时间
    var modifiedAt: Date? = null,
    var isCompleted: Boolean = false // 任务完成状态
) : Serializable