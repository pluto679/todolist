package com.example.todolist

import android.content.Context
import java.io.*

class TaskStorage(private val context: Context) {
    private val fileName = "tasks.dat"

    // 保存任务列表到文件
    fun saveTasks(tasks: List<Task>) {
        try {
            val fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
            val objectOutputStream = ObjectOutputStream(fileOutputStream)
            objectOutputStream.writeObject(ArrayList(tasks))
            objectOutputStream.close()
            fileOutputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 从文件加载任务列表
    fun loadTasks(): MutableList<Task> {
        try {
            val fileInputStream = context.openFileInput(fileName)
            val objectInputStream = ObjectInputStream(fileInputStream)
            val taskList = objectInputStream.readObject() as List<Task>
            objectInputStream.close()
            fileInputStream.close()
            return taskList.toMutableList()
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果文件不存在或出错，返回空列表
            return mutableListOf()
        }
    }
}