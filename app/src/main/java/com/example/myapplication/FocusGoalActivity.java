package com.example.myapplication;

import android.content.SharedPreferences;
import android.content.Intent;
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
    TextView tvTimer, tvProgress;

    SharedPreferences prefs;

    int goalMinutes = 0;
    int totalFocused = 0;
    boolean isRunning = false;

    Handler handler = new Handler();
    long startTime;

    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                long elapsedMillis = System.currentTimeMillis() - startTime;
                int minutes = (int) (elapsedMillis / 1000) / 60;
                int seconds = (int) (elapsedMillis / 1000) % 60;
                tvTimer.setText(String.format("Focus Time: %02d:%02d", minutes, seconds));
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus_goal);

        // View'leri bağla
        etFocusGoal = findViewById(R.id.etFocusGoal);
        btnSaveGoal = findViewById(R.id.btnSaveGoal);
        btnStartFocus = findViewById(R.id.btnStartFocus);
        btnStopFocus = findViewById(R.id.btnStopFocus);
        btnBack = findViewById(R.id.btnBack);
        tvTimer = findViewById(R.id.tvTimer);
        tvProgress = findViewById(R.id.tvProgress);

        prefs = getSharedPreferences("FocusPrefs", MODE_PRIVATE);

        resetDailyIfNeeded();
        loadGoalAndProgress();
        updateButtonStates();

        // Save Goal button
        btnSaveGoal.setOnClickListener(v -> {
            String text = etFocusGoal.getText().toString().trim();
            if (!text.isEmpty()) {
                goalMinutes = Integer.parseInt(text);
                if (goalMinutes > 0) {
                    prefs.edit().putInt("goal", goalMinutes).apply();
                    updateProgress();
                    Toast.makeText(this, "Goal saved: " + goalMinutes + " minutes", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Please enter a positive number", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please enter your goal in minutes", Toast.LENGTH_SHORT).show();
            }
        });

        // Start Focus button
        btnStartFocus.setOnClickListener(v -> startFocusMode());

        // Stop Focus button
        btnStopFocus.setOnClickListener(v -> stopFocusMode());

        // 🔹 Back button → MainActivity’ye dön
        btnBack.setOnClickListener(v -> {
            if (isRunning) {
                Toast.makeText(this, "Please stop focus mode first", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(FocusGoalActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void startFocusMode() {
        if (isRunning) return;

        if (goalMinutes <= 0) {
            Toast.makeText(this, "Please set a goal first", Toast.LENGTH_SHORT).show();
            return;
        }

        isRunning = true;
        startTime = System.currentTimeMillis();
        updateButtonStates();
        Toast.makeText(this, "Focus mode started! Stay focused!", Toast.LENGTH_SHORT).show();
        handler.post(timerRunnable);
    }

    private void stopFocusMode() {
        if (!isRunning) return;

        isRunning = false;
        handler.removeCallbacks(timerRunnable);

        long elapsedMillis = System.currentTimeMillis() - startTime;
        int minutes = (int) (elapsedMillis / 1000) / 60;
        int seconds = (int) (elapsedMillis / 1000) % 60;

        if (minutes >= 1) {
            totalFocused += minutes;
            prefs.edit().putInt("progress", totalFocused).apply();
            Toast.makeText(this, "Great job! Focused " + minutes + "m " + seconds + "s", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Focus session too short: " + seconds + " seconds", Toast.LENGTH_SHORT).show();
        }

        updateProgress();
        updateButtonStates();
        tvTimer.setText("Focus Time: 00:00");
    }

    private void updateProgress() {
        String progressText = "Daily Progress: " + totalFocused + "/" + goalMinutes + " min";
        if (goalMinutes > 0 && totalFocused >= goalMinutes) {
            progressText += " ✓ GOAL ACHIEVED!";
        }
        tvProgress.setText(progressText);
    }

    private void updateButtonStates() {
        btnStartFocus.setEnabled(!isRunning);
        btnStopFocus.setEnabled(isRunning);
        btnSaveGoal.setEnabled(!isRunning);
        etFocusGoal.setEnabled(!isRunning);

        btnStartFocus.setText(isRunning ? "Focus Mode Active..." : "Start Focus Mode");
        btnStopFocus.setText("Stop Focus Mode");
    }

    private void loadGoalAndProgress() {
        goalMinutes = prefs.getInt("goal", 0);
        totalFocused = prefs.getInt("progress", 0);
        if (goalMinutes > 0) {
            etFocusGoal.setText(String.valueOf(goalMinutes));
        }
        updateProgress();
        tvTimer.setText("Focus Time: 00:00");
    }

    private void resetDailyIfNeeded() {
        String lastDate = prefs.getString("lastDate", "");
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        if (!today.equals(lastDate)) {
            prefs.edit().putInt("progress", 0).putString("lastDate", today).apply();
        }
    }

    @Override
    public void onBackPressed() {
        if (isRunning) {
            Toast.makeText(this, "Please stop focus mode first", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(FocusGoalActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }
}
