package com.example.todoappsqlite;

import static com.example.todoappsqlite.R.id.radioGroup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.todoappsqlite.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {
    private LinearLayout todayTaskList;
    private LinearLayout tomorrowTaskList;
    private Button btnAddTask, btnHideCompleted;
    private ArrayList<Task> todayTasks;
    private ArrayList<Task> tomorrowTasks;
    private boolean hideCompleted = false;
    private DatabaseHelper dbHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        dbHelper = new DatabaseHelper(this);
        todayTaskList = findViewById(R.id.linearLayout);
        tomorrowTaskList = findViewById(R.id.linearLayout2);
        btnAddTask = findViewById(R.id.btcAdd);
        btnHideCompleted = findViewById(R.id.btnCompleted);
        todayTasks = new ArrayList<>();
        tomorrowTasks = new ArrayList<>();
        btnAddTask.setOnClickListener(v -> showAddTaskDialog());
        btnHideCompleted.setOnClickListener(v -> {
            hideCompleted = !hideCompleted;
            btnHideCompleted.setText(hideCompleted ? "Show Completed" : "Hide Completed");
            updateTaskViews();
        });
        loadTasks();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void loadTasks() {
        todayTasks = dbHelper.getAllTasks("Today");
        tomorrowTasks = dbHelper.getAllTasks("Tomorrow");
        updateTaskViews();
    }
    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        final EditText taskNameInput = dialogView.findViewById(R.id.editTextTaskName);
        RadioButton radioToday = dialogView.findViewById(R.id.radioToday);
        RadioButton radioTomorrow = dialogView.findViewById(R.id.radioTomorrow);
        Button addTaskButton = dialogView.findViewById(R.id.btnAddTaskDialog);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar todayCal = Calendar.getInstance();
        Calendar tomorrowCal = Calendar.getInstance();
        tomorrowCal.add(Calendar.DAY_OF_YEAR, 1);

        String todayDate = dateFormat.format(todayCal.getTime());
        String tomorrowDate = dateFormat.format(tomorrowCal.getTime());

        radioToday.setText("Today (" + todayDate + ")");
        radioTomorrow.setText("Tomorrow (" + tomorrowDate + ")");

        AlertDialog dialog = builder.create();
        addTaskButton.setOnClickListener(v -> {
            String taskName = taskNameInput.getText().toString().trim();
            boolean isTodayChecked = radioToday.isChecked();
            boolean isTomorrowChecked = radioTomorrow.isChecked();

            if (!taskName.isEmpty() && (isTodayChecked || isTomorrowChecked)) {
                if (isTodayChecked) {
                    addTask(taskName, true);
                } else if (isTomorrowChecked) {
                    addTask(taskName, false);
                }
                dialog.dismiss();
            } else {
                String message = "Task name cannot be empty and a date must be selected.";
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
    private void addTask(String taskName, boolean isToday) {
        Task task = new Task(taskName, false);
        if (isToday) {
            todayTasks.add(task);
            dbHelper.addTask(task, "Today");
        } else {
            tomorrowTasks.add(task);
            dbHelper.addTask(task, "Tomorrow");
        }
        updateTaskViews();
    }
    private void updateTaskViews() {
        todayTaskList.removeAllViews();
        tomorrowTaskList.removeAllViews();

        for (Task task : todayTasks) {
            if (!hideCompleted || !task.isCompleted()) {
                LinearLayout taskLayout = createTaskLayout(task, true);
                todayTaskList.addView(taskLayout);
            }
        }

        for (Task task : tomorrowTasks) {
            if (!hideCompleted || !task.isCompleted()) {
                LinearLayout taskLayout = createTaskLayout(task, false);
                tomorrowTaskList.addView(taskLayout);
            }
        }
    }
    private LinearLayout createTaskLayout(Task task, boolean isToday) {
        LinearLayout taskLayout = new LinearLayout(this);
        taskLayout.setOrientation(LinearLayout.HORIZONTAL);

        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(task.getName());
        checkBox.setChecked(task.isCompleted());
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setCompleted(isChecked);
            dbHelper.updateTask(task, task.getName());
            updateTaskViews();
        });

        LinearLayout.LayoutParams checkBoxParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        checkBox.setLayoutParams(checkBoxParams);

        taskLayout.addView(checkBox);

        ImageButton editButton = new ImageButton(this);
        editButton.setImageResource(R.drawable.pencil);
        editButton.setBackgroundColor(0);
        editButton.setOnClickListener(v -> showEditTaskDialog(task));
        taskLayout.addView(editButton);

        ImageButton deleteButton = new ImageButton(this);
        deleteButton.setImageResource(R.drawable.delete);
        deleteButton.setBackgroundColor(0);
        deleteButton.setOnClickListener(v -> confirmDeleteTask(task));
        taskLayout.addView(deleteButton);

        return taskLayout;
    }


    private void confirmDeleteTask(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm");
        builder.setMessage("Delete this task?");

        builder.setPositiveButton("Yes", (dialog, which) -> {
            deleteTask(task);
            updateTaskViews();
        });

        builder.setNegativeButton("Nope", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void showEditTaskDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Task");

        final EditText input = new EditText(this);
        input.setText(task.getName());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newTaskName = input.getText().toString().trim();
            if (!newTaskName.isEmpty()) {
                dbHelper.updateTask(task, task.getName());
                task.setName(newTaskName);
                updateTaskViews();
            } else {
                Toast.makeText(MainActivity.this, "Task name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteTask(Task task) {
        todayTasks.remove(task);
        tomorrowTasks.remove(task);
        dbHelper.deleteTask(task);
        updateTaskViews();
    }

    static class Task {
        private String name;
        private boolean completed;

        public Task(String name, boolean completed) {
            this.name = name;
            this.completed = completed;
        }

        public String getName() {
            return name;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
        public void setName(String name) {
            this.name = name;
        }
    }
}