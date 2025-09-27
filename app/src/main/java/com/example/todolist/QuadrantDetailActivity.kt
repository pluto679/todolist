package com.example.todolist

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AlertDialog
import java.util.*

class QuadrantDetailActivity : AppCompatActivity() {
    private lateinit var taskStorage: TaskStorage
    private lateinit var backButton: ImageButton
    private lateinit var quadrantTitle: TextView
    private lateinit var taskRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var addTaskButton: ImageButton

    private var taskList = mutableListOf<Task>()
    private lateinit var adapter: TodoAdapter
    private var currentQuadrant = 0
    private val quadrantNames = arrayOf("重要且紧急", "重要不紧急", "紧急不重要", "不重要不紧急")

    companion object {
        const val EXTRA_QUADRANT = "extra_quadrant"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quadrant_detail)

        // 获取传入的象限参数
        currentQuadrant = intent.getIntExtra(EXTRA_QUADRANT, 0)

        // 初始化存储
        taskStorage = TaskStorage(this)

        // 初始化视图
        initViews()
        
        // 加载任务数据
        loadTasks()
        
        // 设置事件监听器
        setupEventListeners()
        
        // 更新界面
        updateUI()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        quadrantTitle = findViewById(R.id.quadrantTitle)
        taskRecyclerView = findViewById(R.id.quadrantTaskRecyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        addTaskButton = findViewById(R.id.addTaskButton)

        // 设置标题
        quadrantTitle.text = quadrantNames[currentQuadrant]

        // 设置RecyclerView
        adapter = TodoAdapter(
            taskList,
            onCompleteClick = { position ->
                val task = taskList[position]
                task.isCompleted = !task.isCompleted
                sortTasksByTime()
                adapter.notifyDataSetChanged()
                saveTasksToGlobalList()
            },
            onEditClick = { position ->
                val task = taskList[position]
                showEditTaskDialog(task, position)
            },
            onLongClick = { position ->
                val task = taskList[position]
                showDeleteConfirmDialog(task, position)
            }
        )
        taskRecyclerView.layoutManager = LinearLayoutManager(this)
        taskRecyclerView.adapter = adapter
    }

    private fun loadTasks() {
        val allTasks = taskStorage.loadTasks()
        taskList.clear()
        
        // 筛选当前象限的任务并按时间排序
        val quadrantTasks = allTasks.filter { it.quadrant == currentQuadrant }
            .sortedByDescending { it.scheduledTime }
        
        taskList.addAll(quadrantTasks)
    }

    private fun setupEventListeners() {
        // 返回按钮
        backButton.setOnClickListener {
            finish()
        }

        // 添加任务按钮
        addTaskButton.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun updateUI() {
        if (taskList.isEmpty()) {
            taskRecyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            taskRecyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
        }
        adapter.notifyDataSetChanged()
    }

    private fun showAddTaskDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task_with_time, null)
        val editTaskContent = dialogView.findViewById<EditText>(R.id.editTaskContent)
        val btnSelectDateTime = dialogView.findViewById<Button>(R.id.btnSelectDateTime)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

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
            val taskContent = editTaskContent.text.toString().trim()
            if (taskContent.isNotBlank()) {
                val task = Task(
                    content = taskContent,
                    quadrant = currentQuadrant,
                    scheduledTime = selectedDateTime
                )
                
                // 添加到当前列表
                taskList.add(0, task)
                
                // 保存到全局任务列表
                val allTasks = taskStorage.loadTasks()
                allTasks.add(task)
                taskStorage.saveTasks(allTasks)
                
                updateUI()
                dialog.dismiss()
                //Toast.makeText(this, "任务已添加", Toast.LENGTH_SHORT).show()
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

    private fun showEditTaskDialog(task: Task, position: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_task_with_time, null)
        val editTaskContent = dialogView.findViewById<EditText>(R.id.editTaskContent)
        val spinnerQuadrant = dialogView.findViewById<Spinner>(R.id.spinnerQuadrant)
        val btnSelectDateTime = dialogView.findViewById<Button>(R.id.btnSelectDateTime)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        // 预填充当前任务内容
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
                val hasContentChanged = task.content != newContent
                val hasQuadrantChanged = task.quadrant != selectedQuadrant
                val hasTimeChanged = task.scheduledTime != selectedDateTime
                
                if (hasContentChanged || hasQuadrantChanged || hasTimeChanged) {
                    // 更新任务
                    task.content = newContent
                    task.quadrant = selectedQuadrant
                    task.scheduledTime = selectedDateTime
                    task.modifiedAt = Date()
                    
                    // 重新排序
                    taskList.sortByDescending { it.scheduledTime }
                    
                    // 保存到全局任务列表
                    val allTasks = taskStorage.loadTasks()
                    val globalTaskIndex = allTasks.indexOfFirst { 
                        it.content == task.content && it.createdAt == task.createdAt 
                    }
                    if (globalTaskIndex != -1) {
                        allTasks[globalTaskIndex] = task
                        taskStorage.saveTasks(allTasks)
                    } else {
                        // 如果没找到，可能是新任务，直接添加
                        allTasks.add(task)
                        taskStorage.saveTasks(allTasks)
                    }
                    
                    // 如果象限改变了，需要重新加载当前象限的任务
                    if (hasQuadrantChanged) {
                        loadTasks()
                        updateUI()
                        Toast.makeText(this, "任务已移动到${quadrantNames[selectedQuadrant]}", Toast.LENGTH_SHORT).show()
                    } else {
                        updateUI()
                        Toast.makeText(this, "任务已更新", Toast.LENGTH_SHORT).show()
                    }
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

    private fun deleteTask(task: Task, position: Int) {
        taskList.removeAt(position)
        adapter.notifyItemRemoved(position)
        
        // 从全局任务列表中删除
        val allTasks = taskStorage.loadTasks()
        allTasks.removeAll { it.content == task.content && it.createdAt == task.createdAt }
        taskStorage.saveTasks(allTasks)
        
        updateUI()
        Toast.makeText(this, "任务已删除", Toast.LENGTH_SHORT).show()
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
    
    private fun showDeleteConfirmDialog(task: Task, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("删除任务")
            .setMessage("确定要删除这个任务吗？")
            .setPositiveButton("确定") { _, _ ->
                deleteTask(task, position)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun saveTasksToGlobalList() {
        val allTasks = taskStorage.loadTasks()
        
        // 更新全局任务列表中的任务状态
        for (task in taskList) {
            val globalTaskIndex = allTasks.indexOfFirst { 
                it.content == task.content && it.createdAt == task.createdAt 
            }
            if (globalTaskIndex != -1) {
                allTasks[globalTaskIndex] = task
            }
        }
        
        taskStorage.saveTasks(allTasks)
    }
}