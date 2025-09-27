package com.example.todolist

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.gridlayout.widget.GridLayout
import androidx.appcompat.app.AlertDialog
import java.util.*
import java.io.*
import android.content.Context
import android.content.Intent

class MainActivity : AppCompatActivity() {
    private lateinit var taskStorage: TaskStorage
    private lateinit var addTaskButton: ImageButton
    private lateinit var taskInputField: EditText
    private lateinit var submitTaskButton: Button
    private lateinit var cancelTaskButton: Button
    private lateinit var toggleLayoutButton: ImageButton
    private lateinit var quadrantSelector: Spinner
    private lateinit var btnSelectDateTime: Button
    private lateinit var taskRecyclerView: RecyclerView
    private lateinit var quadrantLayout: GridLayout
    private lateinit var taskInputPanel: View

    private lateinit var quadrant1Tasks: LinearLayout
    private lateinit var quadrant2Tasks: LinearLayout
    private lateinit var quadrant3Tasks: LinearLayout
    private lateinit var quadrant4Tasks: LinearLayout

    private lateinit var quadrant1TextView: TextView
    private lateinit var quadrant2TextView: TextView
    private lateinit var quadrant3TextView: TextView
    private lateinit var quadrant4TextView: TextView

    private var taskList = mutableListOf<Task>()
    private lateinit var adapter: TodoAdapter
    private var isQuadrantLayout = false // 默认为列表布局
    private var selectedDateTime: Date? = null // 存储选择的执行时间

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化任务存储
        taskStorage = TaskStorage(this)
        
        // 从存储加载任务
        taskList = taskStorage.loadTasks()

        // Initialize views
        addTaskButton = findViewById(R.id.addTaskButton)
        taskInputField = findViewById(R.id.taskInputField)
        submitTaskButton = findViewById(R.id.submitTaskButton)
        cancelTaskButton = findViewById(R.id.cancelTaskButton)
        toggleLayoutButton = findViewById(R.id.toggleLayoutButton)
        quadrantSelector = findViewById(R.id.quadrantSelector)
        btnSelectDateTime = findViewById(R.id.btnSelectDateTime)
        taskRecyclerView = findViewById(R.id.taskRecyclerView)
        quadrantLayout = findViewById(R.id.quadrantLayout)
        taskInputPanel = findViewById(R.id.taskInputPanel)

        quadrant1Tasks = findViewById(R.id.quadrant1Tasks)
        quadrant2Tasks = findViewById(R.id.quadrant2Tasks)
        quadrant3Tasks = findViewById(R.id.quadrant3Tasks)
        quadrant4Tasks = findViewById(R.id.quadrant4Tasks)

        quadrant1TextView = findViewById(R.id.quadrant1TextView)
        quadrant2TextView = findViewById(R.id.quadrant2TextView)
        quadrant3TextView = findViewById(R.id.quadrant3TextView)
        quadrant4TextView = findViewById(R.id.quadrant4TextView)

        // Setup RecyclerView
        adapter = TodoAdapter(taskList, 
            onCompleteClick = { position ->
                val task = taskList[position]
                task.isCompleted = !task.isCompleted
                sortTasksByTime()
                adapter.notifyDataSetChanged()
                updateQuadrants()
                taskStorage.saveTasks(taskList)
            },
            onEditClick = { position ->
                val task = taskList[position]
                showEditTaskDialog(task, position)
            },
            onLongClick = { position ->
                val task = taskList[position]
                showDeleteConfirmDialog(task)
            }
        )
        taskRecyclerView.layoutManager = LinearLayoutManager(this)
        taskRecyclerView.adapter = adapter

        // Setup quadrant selector
        val quadrantOptions = arrayOf("重要且紧急", "重要不紧急", "紧急不重要", "不重要不紧急")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, quadrantOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        quadrantSelector.adapter = spinnerAdapter

        // Show input field and submit button
        addTaskButton.setOnClickListener {
            addTaskButton.visibility = View.GONE
            taskInputPanel.visibility = View.VISIBLE
        }

        // Submit task
        submitTaskButton.setOnClickListener {
            val taskContent = taskInputField.text.toString().trim()
            val selectedQuadrant = quadrantSelector.selectedItemPosition

            if (taskContent.isNotBlank()) {
                // 使用选择的时间或当前时间作为默认值
                val executionTime = selectedDateTime ?: Date()
                val newTask = Task(
                    content = taskContent,
                    quadrant = selectedQuadrant,
                    scheduledTime = executionTime
                )
                taskList.add(newTask)
                taskStorage.saveTasks(taskList)
                
                // 清空输入并隐藏面板
                taskInputField.text.clear()
                selectedDateTime = null
                btnSelectDateTime.text = "📅 选择执行时间"
                taskInputPanel.visibility = View.GONE
                addTaskButton.visibility = View.VISIBLE
                
                // 更新界面
                if (isQuadrantLayout) {
                    updateQuadrants()
                } else {
                    sortTasksByTime()
                    adapter.notifyDataSetChanged()
                }
                
                Toast.makeText(this, "任务添加成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "请输入任务内容", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Cancel task input
        cancelTaskButton.setOnClickListener {
            taskInputField.text.clear()
            selectedDateTime = null
            btnSelectDateTime.text = "📅 选择执行时间"
            taskInputPanel.visibility = View.GONE
            addTaskButton.visibility = View.VISIBLE
        }

        // Toggle layout
        toggleLayoutButton.setOnClickListener {
            isQuadrantLayout = !isQuadrantLayout
            toggleLayout()
        }
        
        // 设置执行时间按钮点击事件
        btnSelectDateTime.setOnClickListener {
            showDateTimePicker { dateTime ->
                selectedDateTime = dateTime
                // 更新按钮文本显示选择的时间
                val formatter = java.text.SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                btnSelectDateTime.text = "📅 ${formatter.format(dateTime)}"
            }
        }
        
        // 设置象限点击事件监听器
        setupQuadrantClickListeners()
        
        // 如果没有任务，添加示例任务
        if (taskList.isEmpty()) {
            addSampleTasks()
        } else {
            // 更新四象限视图
            updateQuadrants()
        }
        
        // 确保默认显示列表布局
        toggleLayout()
    }

    private fun addTaskToQuadrant(task: Task) {
        val taskView = TextView(this).apply {
            text = task.content
            textSize = 16f
            setPadding(8, 8, 8, 8)
        }

        when (task.quadrant) {
            0 -> quadrant1Tasks.addView(taskView)
            1 -> quadrant2Tasks.addView(taskView)
            2 -> quadrant3Tasks.addView(taskView)
            3 -> quadrant4Tasks.addView(taskView)
        }
    }

    private fun deleteTask(task: Task) {
        taskList.remove(task)
        adapter.notifyDataSetChanged()
        updateQuadrants()
        taskStorage.saveTasks(taskList)
        
        // 显示删除成功的Toast
        Toast.makeText(this, "任务已删除", Toast.LENGTH_SHORT).show()
    }
    
    private fun showDeleteConfirmDialog(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("删除任务")
            .setMessage("确定要删除这个任务吗？")
            .setPositiveButton("确定") { _, _ ->
                deleteTask(task)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun sortTasksByTime() {
        // 按日期分组，然后在每个日期内按完成状态排序（未完成在前），最后按时间降序排列
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        taskList.sortWith(
            compareByDescending<Task> { dateFormat.format(it.scheduledTime) }
                .thenBy { it.isCompleted }
                .thenByDescending { it.scheduledTime }
        )
    }

    private fun updateQuadrants() {
        // 清空并重建四象限视图
        quadrant1Tasks.removeAllViews()
        quadrant2Tasks.removeAllViews()
        quadrant3Tasks.removeAllViews()
        quadrant4Tasks.removeAllViews()
        
        // 按日期分组，然后在每个日期内按完成状态排序（未完成在前），最后按时间降序排列
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val sortedTasks = taskList.sortedWith(
            compareByDescending<Task> { dateFormat.format(it.scheduledTime) }
                .thenBy { it.isCompleted }
                .thenByDescending { it.scheduledTime }
        )
        
        // 为每个象限分组任务并限制显示数量
        val quadrantTasks = Array(4) { mutableListOf<Task>() }
        sortedTasks.forEach { task ->
            quadrantTasks[task.quadrant].add(task)
        }
        
        // 限制每个象限最多显示3个任务
        val maxTasksPerQuadrant = 3
        
        quadrantTasks.forEachIndexed { quadrant, tasks ->
            val tasksToShow = tasks.take(maxTasksPerQuadrant)
            tasksToShow.forEach { task ->
                addTaskToQuadrant(task)
            }
            
            // 如果有更多任务，显示"查看更多"提示
            if (tasks.size > maxTasksPerQuadrant) {
                addMoreTasksIndicator(quadrant, tasks.size - maxTasksPerQuadrant)
            }
        }
    }
    
    private fun addMoreTasksIndicator(quadrant: Int, moreCount: Int) {
        val moreTasksView = TextView(this).apply {
            text = "... 还有${moreCount}个任务"
            textSize = 12f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
            setPadding(8, 4, 8, 4)
            setTypeface(null, android.graphics.Typeface.ITALIC)
        }

        when (quadrant) {
            0 -> quadrant1Tasks.addView(moreTasksView)
            1 -> quadrant2Tasks.addView(moreTasksView)
            2 -> quadrant3Tasks.addView(moreTasksView)
            3 -> quadrant4Tasks.addView(moreTasksView)
        }
    }

    private fun addSampleTasks() {
        val sampleTasks = listOf(
            Task("点击记录可以进行修改", Date(), 0),
            Task("点击小花猫可以切换视图", Date(), 0),
            Task("点击小橘猫可以增加清单记录", Date(), 1),
            Task("记录已完成？点击->", Date(), 1),
            Task("长按记录进行删除（不可恢复）", Date(), 2),
        )
        
        taskList.addAll(sampleTasks)
        adapter.notifyDataSetChanged()
        
        // 添加到四象限
        sampleTasks.forEach { task ->
            addTaskToQuadrant(task)
        }
        
        // 保存示例任务到存储
        taskStorage.saveTasks(taskList)
    }

    private fun toggleLayout() {
        if (isQuadrantLayout) {
            taskRecyclerView.visibility = View.GONE
            quadrantLayout.visibility = View.VISIBLE
            //Toast.makeText(this, "切换到四象限布局", Toast.LENGTH_SHORT).show()
        } else {
            quadrantLayout.visibility = View.GONE
            taskRecyclerView.visibility = View.VISIBLE
            //Toast.makeText(this, "切换到列表布局", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showEditTaskDialog(task: Task, position: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_task_with_time, null)
        
        val editTaskContent = dialogView.findViewById<EditText>(R.id.editTaskContent)
        val spinnerQuadrant = dialogView.findViewById<Spinner>(R.id.spinnerQuadrant)
        val btnSelectDateTime = dialogView.findViewById<Button>(R.id.btnSelectDateTime)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        
        // 设置当前任务内容
        editTaskContent.setText(task.content)
        
        // 设置Spinner的选项
        val quadrantOptions = arrayOf(
            "第一象限 - 重要且紧急",
            "第二象限 - 重要但不紧急", 
            "第三象限 - 不重要但紧急",
            "第四象限 - 不重要且不紧急"
        )
        
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, quadrantOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerQuadrant.adapter = spinnerAdapter
        
        // 设置当前象限选择
        spinnerQuadrant.setSelection(task.quadrant)
        
        // 设置当前时间
        var selectedDateTime = task.scheduledTime
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        // 日期时间选择
        btnSelectDateTime.setOnClickListener {
            showDateTimePicker { dateTime ->
                selectedDateTime = dateTime
            }
        }
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnSave.setOnClickListener {
            val newContent = editTaskContent.text.toString().trim()
            val selectedQuadrant = spinnerQuadrant.selectedItemPosition
            
            if (newContent.isNotBlank()) {
                // 检查是否有实际修改
                val hasContentChanged = task.content != newContent
                val hasQuadrantChanged = task.quadrant != selectedQuadrant
                val hasTimeChanged = task.scheduledTime != selectedDateTime
                
                if (hasContentChanged || hasQuadrantChanged || hasTimeChanged) {
                    // 更新任务
                    task.content = newContent
                    task.quadrant = selectedQuadrant
                    task.scheduledTime = selectedDateTime
                    task.modifiedAt = Date() // 设置修改时间
                    
                    // 重新排序任务列表
                    sortTasksByTime()
                    
                    // 通知适配器更新
                    adapter.notifyDataSetChanged()
                    
                    // 更新四象限视图
                    updateQuadrants()
                    
                    // 保存更改到存储
                    taskStorage.saveTasks(taskList)
                    
                    Toast.makeText(this, "任务已更新", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "没有修改内容", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            } else {
                Toast.makeText(this, "请输入任务内容", Toast.LENGTH_SHORT).show()
            }
        }
        
        dialog.show()
    }
    
    private fun showAddTaskWithTimeDialog(taskContent: String, selectedQuadrant: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task_with_time, null)
        
        val editTaskContent = dialogView.findViewById<EditText>(R.id.editTaskContent)
        val btnSelectDateTime = dialogView.findViewById<Button>(R.id.btnSelectDateTime)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        
        // 设置任务内容
        editTaskContent.setText(taskContent)
        
        // 初始化为当前时间
        var selectedDateTime = Date()
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        // 日期时间选择
        btnSelectDateTime.setOnClickListener {
            showDateTimePicker { dateTime ->
                selectedDateTime = dateTime
            }
        }
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnSave.setOnClickListener {
            val content = editTaskContent.text.toString().trim()
            
            if (content.isNotBlank()) {
                val newTask = Task(
                    content = content,
                    quadrant = selectedQuadrant,
                    scheduledTime = selectedDateTime
                )
                
                taskList.add(newTask)
                sortTasksByTime()
                adapter.notifyDataSetChanged()
                updateQuadrants()
                taskStorage.saveTasks(taskList)
                
                Toast.makeText(this, "任务已添加", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "请输入任务内容", Toast.LENGTH_SHORT).show()
            }
        }
        
        dialog.show()
    }
    
    private fun showDateTimePicker(callback: (Date) -> Unit) {
        val calendar = java.util.Calendar.getInstance()
        
        // 先选择日期
        val datePickerDialog = android.app.DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                
                // 然后选择时间
                val timePickerDialog = android.app.TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
                        calendar.set(java.util.Calendar.MINUTE, minute)
                        callback(calendar.time)
                    },
                    calendar.get(java.util.Calendar.HOUR_OF_DAY),
                    calendar.get(java.util.Calendar.MINUTE),
                    true
                )
                timePickerDialog.show()
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showDatePicker(onDateSelected: (year: Int, month: Int, day: Int) -> Unit) {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH)
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        
        val datePickerDialog = android.app.DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                onDateSelected(selectedYear, selectedMonth, selectedDay)
            },
            year, month, day
        )
        
        datePickerDialog.show()
    }
    
    private fun showTimePicker(onTimeSelected: (hour: Int, minute: Int) -> Unit) {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        
        val timePickerDialog = android.app.TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                onTimeSelected(selectedHour, selectedMinute)
            },
            hour, minute, true
        )
        
        timePickerDialog.show()
    }
    
    private fun updateTimeDisplay(textView: TextView, dateTime: Date) {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        textView.text = "选定时间: ${formatter.format(dateTime)}"
    }
    
    private fun setupQuadrantClickListeners() {
        val quadrantLayouts = arrayOf(
            findViewById<LinearLayout>(R.id.quadrant1Tasks),
            findViewById<LinearLayout>(R.id.quadrant2Tasks),
            findViewById<LinearLayout>(R.id.quadrant3Tasks),
            findViewById<LinearLayout>(R.id.quadrant4Tasks)
        )
        
        quadrantLayouts.forEachIndexed { index, layout ->
            layout.setOnClickListener {
                // 启动象限详情Activity
                val intent = Intent(this, QuadrantDetailActivity::class.java)
                intent.putExtra(QuadrantDetailActivity.EXTRA_QUADRANT, index)
                startActivity(intent)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 从象限详情页面返回时重新加载数据
        taskList.clear()
        taskList.addAll(taskStorage.loadTasks())
        sortTasksByTime()
        adapter.notifyDataSetChanged()
        updateQuadrants()
    }
}