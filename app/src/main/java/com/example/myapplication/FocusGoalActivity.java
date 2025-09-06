package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FocusGoalActivity extends AppCompatActivity {

    EditText etFocusGoal;
    Button btnSaveGoal, btnStartFocus, btnStopFocus, btnBack;
    TextView tvFocusTimer, tvFocusProgress;

    SharedPreferences prefs;

    int goalMinutes = 0;
    int totalFocused = 0;
    boolean isRunning = false;

    Handler handler = new Handler();
    long startTime;

    public static FocusGoalActivity instance;

    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                long elapsedMillis = System.currentTimeMillis() - startTime;
                int minutes = (int) (elapsedMillis / 1000) / 60;
                int seconds = (int) (elapsedMillis / 1000) % 60;
                tvFocusTimer.setText(String.format("Elapsed: %02d:%02d", minutes, seconds));
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus_goal);

        instance = this;

        etFocusGoal = findViewById(R.id.etFocusGoal);
        btnSaveGoal = findViewById(R.id.btnSaveGoal);
        btnStartFocus = findViewById(R.id.btnStartFocus);
        btnStopFocus = findViewById(R.id.btnStopFocus);
        btnBack = findViewById(R.id.btnBack);
        tvFocusTimer = findViewById(R.id.tvFocusTimer);
        tvFocusProgress = findViewById(R.id.tvFocusProgress);

        prefs = getSharedPreferences("FocusPrefs", MODE_PRIVATE);

        resetDailyIfNeeded();
        loadGoalAndProgress();
        updateButtonStates();

        btnSaveGoal.setOnClickListener(v -> saveGoal());
        btnStartFocus.setOnClickListener(v -> startFocusMode());
        btnStopFocus.setOnClickListener(v -> stopFocusMode());
        btnBack.setOnClickListener(v -> goBack());
    }

    private void saveGoal() {
        String text = etFocusGoal.getText().toString().trim();
        if (!text.isEmpty()) {
            goalMinutes = Integer.parseInt(text);
            if (goalMinutes > 0) {
                prefs.edit().putInt("goal", goalMinutes).apply();
                updateProgress();
                Toast.makeText(this, "Goal saved: " + goalMinutes + " minutes", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Enter positive number", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Enter your goal", Toast.LENGTH_SHORT).show();
        }
    }

    private void startFocusMode() {
        if (isRunning) return;
        if (goalMinutes <= 0) {
            Toast.makeText(this, "Set a goal first", Toast.LENGTH_SHORT).show();
            return;
        }

        isRunning = true;
        startTime = System.currentTimeMillis();
        updateButtonStates();
        handler.post(timerRunnable);
    }

    private void stopFocusMode() {
        if (!isRunning) return;

        isRunning = false;
        handler.removeCallbacks(timerRunnable);

        long elapsedMillis = System.currentTimeMillis() - startTime;
        int minutes = (int) (elapsedMillis / 1000) / 60;

        if (minutes > 0) {
            totalFocused += minutes;
            prefs.edit().putInt("progress", totalFocused).apply();
            Toast.makeText(this, "Focused " + minutes + " min", Toast.LENGTH_SHORT).show();
        }

        updateProgress();
        updateButtonStates();
        tvFocusTimer.setText("Elapsed: 00:00");

        // 🔹 Hedef tamamlandıysa MainActivity'yi bilgilendir
        if (goalMinutes > 0 && totalFocused >= goalMinutes) {
            if (MainActivity.context != null) {
                MainActivity.context.markGoalCompleted("focus");
            }
        }
    }

    private void updateProgress() {
        tvFocusProgress.setText("Progress: " + totalFocused + " / " + goalMinutes + " min" +
                ((goalMinutes > 0 && totalFocused >= goalMinutes) ? " ✓ GOAL ACHIEVED!" : ""));
    }

    private void updateButtonStates() {
        btnStartFocus.setEnabled(!isRunning);
        btnStopFocus.setEnabled(isRunning);
        btnSaveGoal.setEnabled(!isRunning);
        etFocusGoal.setEnabled(!isRunning);
    }

    private void loadGoalAndProgress() {
        goalMinutes = prefs.getInt("goal", 0);
        totalFocused = prefs.getInt("progress", 0);
        if (goalMinutes > 0) etFocusGoal.setText(String.valueOf(goalMinutes));
        updateProgress();
        tvFocusTimer.setText("Elapsed: 00:00");
    }

    private void resetDailyIfNeeded() {
        String lastDate = prefs.getString("lastDate", "");
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        if (!today.equals(lastDate)) {
            prefs.edit().putInt("progress", 0).putString("lastDate", today).apply();
            totalFocused = 0;
        }
    }

    public void forceResetFromMain() {
        totalFocused = 0;
        prefs.edit().putInt("progress", 0).apply();
        updateProgress();
    }

    private void goBack() {
        if (isRunning) {
            Toast.makeText(this, "Stop focus mode first", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(FocusGoalActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        goBack();
    }
}
