package com.example.focusease.ui.pomodoro

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.focusease.R
import com.google.android.material.card.MaterialCardView

class TaskAdapter(
    private val tasks: MutableList<PomodoroTask>,
    private val onTaskClick: (PomodoroTask) -> Unit,
    private val onTaskDelete: (PomodoroTask) -> Unit,
    private val onTaskCompleteToggle: (PomodoroTask) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardTask: MaterialCardView = itemView.findViewById(R.id.cardTask)
        val checkboxTask: CheckBox = itemView.findViewById(R.id.checkboxTask)
        val tvTaskName: TextView = itemView.findViewById(R.id.tvTaskName)
        val tvCompletedPomodoros: TextView = itemView.findViewById(R.id.tvCompletedPomodoros)
        val tvEstimatedPomodoros: TextView = itemView.findViewById(R.id.tvEstimatedPomodoros)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pomodoro_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        holder.tvTaskName.text = task.title
        holder.tvCompletedPomodoros.text = task.completedPomodoros.toString()
        holder.tvEstimatedPomodoros.text = task.estimatedPomodoros.toString()

        // PENTING: Set checkbox TANPA trigger listener
        holder.checkboxTask.setOnCheckedChangeListener(null)
        holder.checkboxTask.isChecked = task.isCompleted

        // Apply strikethrough if task is completed
        if (task.isCompleted) {
            holder.tvTaskName.paintFlags = holder.tvTaskName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvTaskName.alpha = 0.5f
        } else {
            holder.tvTaskName.paintFlags = holder.tvTaskName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvTaskName.alpha = 1.0f
        }

        // Click listener untuk card - pilih task
        holder.cardTask.setOnClickListener {
            onTaskClick(task)
        }

        // Checkbox listener - toggle complete status
        holder.checkboxTask.setOnCheckedChangeListener { _, isChecked ->
            // Buat task baru dengan status updated (immutable)
            val updatedTask = task.copy(isCompleted = isChecked)

            // Update task di list
            val index = tasks.indexOfFirst { it.id == task.id }
            if (index != -1) {
                tasks[index] = updatedTask
            }

            // Callback ke Activity
            onTaskCompleteToggle(updatedTask)

            // Update UI
            notifyItemChanged(position)
        }

        // Delete button listener
        holder.btnDelete.setOnClickListener {
            onTaskDelete(task)
        }
    }

    override fun getItemCount(): Int = tasks.size

    fun addTask(task: PomodoroTask) {
        tasks.add(0, task)
        notifyItemInserted(0)
    }

    fun removeTask(task: PomodoroTask) {
        val position = tasks.indexOf(task)
        if (position != -1) {
            tasks.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateTask(task: PomodoroTask) {
        val position = tasks.indexOfFirst { it.id == task.id }
        if (position != -1) {
            tasks[position] = task
            notifyItemChanged(position)
        }
    }

    fun getTasks(): List<PomodoroTask> = tasks
}