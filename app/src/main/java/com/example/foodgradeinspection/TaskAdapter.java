package com.example.foodgradeinspection;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    private List<Task> tasks;
    private OnTaskClickListener listener;

    public TaskAdapter(List<Task> tasks, OnTaskClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.bind(task, listener);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public void updateTasks(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleTV;
        TextView statusTV;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTV = itemView.findViewById(R.id.task_title);
            statusTV = itemView.findViewById(R.id.task_status);
        }

        public void bind(Task task, OnTaskClickListener listener) {

            // ---------- title ----------
            titleTV.setText(
                    task.getLocationName() != null
                            ? task.getLocationName()
                            : task.getLocationId());

            // ---------- status ----------
            String statusRaw = task.getStatus() == null ? "" : task.getStatus().toLowerCase();
            String statusCap = statusRaw.isEmpty()
                    ? ""
                    : Character.toUpperCase(statusRaw.charAt(0)) + statusRaw.substring(1);

            statusTV.setText(statusCap);  // shows “Open” or “Completed”

            if ("open".equals(statusRaw)) {
                statusTV.setBackgroundResource(R.drawable.status_badge_open);
            } else { // "completed" is the only other value you use
                statusTV.setBackgroundResource(R.drawable.status_badge_completed);
            }

            // ---------- click ----------
            itemView.setOnClickListener(v -> listener.onTaskClick(task));
        }

    }
}