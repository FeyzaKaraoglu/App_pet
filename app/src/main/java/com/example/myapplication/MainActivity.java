package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.app.AlertDialog;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static MainActivity context;

    Button btnSettings;
    Button btnGoals;
    Button btnGame;

    TextView lvl1, lvl2, lvl3, lvl4;
    TextView petNameView;

    SharedPreferences goalPrefs;

    boolean waterGoalCompleted = false;
    boolean stepsGoalCompleted = false;
    boolean sleepGoalCompleted = false;
    boolean focusGoalCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        initializeViews();
        setupPreferences();
        checkDailyReset();
        updateGoalChart();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGoalChart();
    }

    private void initializeViews() {
        btnSettings = findViewById(R.id.btnSettings);
        btnGoals = findViewById(R.id.btnGoals);
        btnGame = findViewById(R.id.btnGame);

        // Goal completion boxes (lives)
        lvl1 = findViewById(R.id.lvl1);
        lvl2 = findViewById(R.id.lvl2);
        lvl3 = findViewById(R.id.lvl3);
        lvl4 = findViewById(R.id.lvl4);

        petNameView = findViewById(R.id.textView);
    }

    private void setupPreferences() {
        goalPrefs = getSharedPreferences("goalCompletionPrefs", MODE_PRIVATE);
    }

    private void checkDailyReset() {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String savedDate = goalPrefs.getString("lastGoalDate", today);

        if (!today.equals(savedDate)) {
            SharedPreferences.Editor editor = goalPrefs.edit();
            editor.putBoolean("waterCompleted", false);
            editor.putBoolean("stepsCompleted", false);
            editor.putBoolean("sleepCompleted", false);
            editor.putBoolean("focusCompleted", false);
            editor.putString("lastGoalDate", today);
            editor.apply();
        }

        loadGoalCompletionStatus();
    }

    private void loadGoalCompletionStatus() {
        waterGoalCompleted = goalPrefs.getBoolean("waterCompleted", false);
        stepsGoalCompleted = goalPrefs.getBoolean("stepsCompleted", false);
        sleepGoalCompleted = goalPrefs.getBoolean("sleepCompleted", false);
        focusGoalCompleted = goalPrefs.getBoolean("focusCompleted", false);
    }

    private void updateGoalChart() {
        loadGoalCompletionStatus();
        updateLifeIndicator(lvl1, waterGoalCompleted, "💧");
        updateLifeIndicator(lvl2, stepsGoalCompleted, "👟");
        updateLifeIndicator(lvl3, sleepGoalCompleted, "😴");
        updateLifeIndicator(lvl4, focusGoalCompleted, "🎯");
    }

    private void updateLifeIndicator(TextView lifeView, boolean completed, String emoji) {
        if (completed) {
            lifeView.setText(emoji);
            lifeView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light));
        } else {
            lifeView.setText("");
            lifeView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        }
    }

    public void markGoalCompleted(String goalType) {
        SharedPreferences.Editor editor = goalPrefs.edit();

        switch (goalType.toLowerCase()) {
            case "water":
                editor.putBoolean("waterCompleted", true);
                waterGoalCompleted = true;
                showGoalCompletedMessage("Water goal completed! 💧");
                break;
            case "steps":
                editor.putBoolean("stepsCompleted", true);
                stepsGoalCompleted = true;
                showGoalCompletedMessage("Steps goal completed! 👟");
                break;
            case "sleep":
                editor.putBoolean("sleepCompleted", true);
                sleepGoalCompleted = true;
                showGoalCompletedMessage("Sleep goal completed! 😴");
                break;
            case "focus":
                editor.putBoolean("focusCompleted", true);
                focusGoalCompleted = true;
                showGoalCompletedMessage("Focus goal completed! 🎯");
                break;
        }

        editor.apply();
        updateGoalChart();
    }

    private void showGoalCompletedMessage(String message) {
        Toast.makeText(this, message + " Your pet gained a life!", Toast.LENGTH_LONG).show();
    }

    private void setupClickListeners() {
        btnSettings.setOnClickListener(v -> showAyarlarDialog());
        btnGoals.setOnClickListener(v -> showGoalsDialog());

        // 🎮 Oyun butonu: her zaman direkt MiniGameActivity açsın
        btnGame.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MiniGameActivity.class);
            startActivity(intent);
        });
    }

    private void showAyarlarDialog() {
        String[] options = {"Languages", "Pet name", "Color", "Reset Progress"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    Toast.makeText(this, "Language settings coming soon!", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    changePetName();
                    break;
                case 2:
                    Toast.makeText(this, "Color settings coming soon!", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    resetDailyProgress();
                    break;
            }
        });
        builder.show();
    }

    private void changePetName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Pet Name");
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter new name");
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                petNameView.setText(newName);
                goalPrefs.edit().putString("petName", newName).apply();
                Toast.makeText(this, "Pet name changed to " + newName, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void resetDailyProgress() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Daily Progress");
        builder.setMessage("This will reset all today's goal progress. Are you sure?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            SharedPreferences.Editor editor = goalPrefs.edit();
            editor.putBoolean("waterCompleted", false);
            editor.putBoolean("stepsCompleted", false);
            editor.putBoolean("sleepCompleted", false);
            editor.putBoolean("focusCompleted", false);
            editor.apply();

            getSharedPreferences("waterPrefs", MODE_PRIVATE).edit().putInt("totalDrank", 0).apply();

            if (StepsGoalActivity.instance != null) {
                StepsGoalActivity.instance.forceResetFromMain();
            }

            if (SleepGoalActivity.instance != null) {
                SleepGoalActivity.instance.forceResetFromMain();
            }

            if (FocusGoalActivity.instance != null) {
                FocusGoalActivity.instance.forceResetFromMain();
            }

            updateGoalChart();
            Toast.makeText(this, "Daily progress reset!", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showGoalsDialog() {
        String[] options = {"💧 Water Goal", "👟 Steps Goal", "😴 Sleeping Goal", "🎯 Focusing Goal"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Daily Goals");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    startActivity(new Intent(this, WaterGoalActivity.class));
                    break;
                case 1:
                    startActivity(new Intent(this, StepsGoalActivity.class));
                    break;
                case 2:
                    startActivity(new Intent(this, SleepGoalActivity.class));
                    break;
                case 3:
                    startActivity(new Intent(this, FocusGoalActivity.class));
                    break;
            }
        });
        builder.show();
    }
}
