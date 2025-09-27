package com.example.todolist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class TodoAdapter(
    private val tasks: MutableList<Task>,
    private val onCompleteClick: (Int) -> Unit,
    private val onEditClick: (Int) -> Unit,
    private val onLongClick: (Int) -> Unit
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    // 用于跟踪日期变化的标记
    private var currentDateStr: String = ""

    class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateHeader: TextView = itemView.findViewById(R.id.dateHeader)
        val todoText: TextView = itemView.findViewById(R.id.todoText)
        val todoTime: TextView = itemView.findViewById(R.id.todoTime)
        val completeButton: ImageButton = itemView.findViewById(R.id.completeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val task = tasks[position]
        
        // 设置任务内容和样式
        holder.todoText.text = task.content
        
        // 根据完成状态设置样式
        if (task.isCompleted) {
            // 已完成任务：中划线和灰度效果
            holder.todoText.paintFlags = holder.todoText.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            holder.todoText.alpha = 0.5f
            holder.todoTime.alpha = 0.5f
        } else {
            // 未完成任务：正常样式
            holder.todoText.paintFlags = holder.todoText.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.todoText.alpha = 1.0f
            holder.todoTime.alpha = 1.0f
        }
        
        // 设置时间 - 显示计划执行时间
        holder.todoTime.text = timeFormat.format(task.scheduledTime)
        
        // 处理日期标题 - 使用计划执行时间来分组
        val dateStr = dateFormat.format(task.scheduledTime)
        val today = dateFormat.format(Date())
        val yesterday = dateFormat.format(Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))
        
        // 确定是否显示日期标题
        val showDateHeader = position == 0 || dateStr != dateFormat.format(tasks[position - 1].scheduledTime)
        
        if (showDateHeader) {
            holder.dateHeader.visibility = View.VISIBLE
            // 设置友好的日期显示
            val displayDate = when (dateStr) {
                today -> "今天"
                yesterday -> "昨天"
                else -> dateStr
            }
            holder.dateHeader.text = displayDate
        } else {
            holder.dateHeader.visibility = View.GONE
        }
        
        // 设置完成按钮点击事件
        holder.completeButton.setOnClickListener {
            onCompleteClick(position)
        }
        
        // 设置整个项目的点击事件（编辑）
        holder.itemView.setOnClickListener {
            onEditClick(position)
        }
        
        // 设置长按事件（删除）
        holder.itemView.setOnLongClickListener {
            onLongClick(position)
            true
        }
    }

    override fun getItemCount(): Int = tasks.size
}
