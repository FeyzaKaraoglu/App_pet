package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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

    Button btnSettings;
    Button btnGoals;
    Button btnGame;

    // Goal completion indicators (lives)
    TextView lvl1, lvl2, lvl3, lvl4;
    TextView petNameView;

    SharedPreferences goalPrefs;

    // Goal completion status
    boolean waterGoalCompleted = false;
    boolean stepsGoalCompleted = false;
    boolean sleepGoalCompleted = false;
    boolean focusGoalCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupPreferences();
        checkDailyReset();
        updateGoalChart();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update chart every time we return to main activity
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

        // Reset goal completion status for new day
        if (!today.equals(savedDate)) {
            SharedPreferences.Editor editor = goalPrefs.edit();
            editor.putBoolean("waterCompleted", false);
            editor.putBoolean("stepsCompleted", false);
            editor.putBoolean("sleepCompleted", false);
            editor.putBoolean("focusCompleted", false);
            editor.putString("lastGoalDate", today);
            editor.apply();
        }

        // Load current day's completion status
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

        // Update each life indicator based on goal completion
        updateLifeIndicator(lvl1, waterGoalCompleted, "💧");
        updateLifeIndicator(lvl2, stepsGoalCompleted, "👟");
        updateLifeIndicator(lvl3, sleepGoalCompleted, "😴");
        updateLifeIndicator(lvl4, focusGoalCompleted, "🎯");

        // Check if mini-game should be unlocked
        updateGameButton();
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

    private void updateGameButton() {
        int completedGoals = getCompletedGoalsCount();

        if (completedGoals == 4) {
            // All goals completed - unlock mini-game
            btnGame.setText("🎮 Play Mini-Game!");
            btnGame.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light));
            btnGame.setEnabled(true);
        } else {
            // Show progress towards unlocking game
            btnGame.setText("Complete " + (4 - completedGoals) + " more goals");
            btnGame.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            btnGame.setEnabled(false);
        }
    }

    private int getCompletedGoalsCount() {
        int count = 0;
        if (waterGoalCompleted) count++;
        if (stepsGoalCompleted) count++;
        if (sleepGoalCompleted) count++;
        if (focusGoalCompleted) count++;
        return count;
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

        // Check if all goals are completed
        if (getCompletedGoalsCount() == 4) {
            showAllGoalsCompletedMessage();
        }
    }

    private void showGoalCompletedMessage(String message) {
        Toast.makeText(this, message + " Your pet gained a life!", Toast.LENGTH_LONG).show();
    }

    private void showAllGoalsCompletedMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎉 Congratulations!");
        builder.setMessage("You've completed all your daily goals!\nYour pet is ready to play mini-games!");
        builder.setPositiveButton("Awesome!", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void setupClickListeners() {
        // Settings menu
        btnSettings.setOnClickListener(v -> showAyarlarDialog());

        // Goals menu
        btnGoals.setOnClickListener(v -> showGoalsDialog());

        // Game button
        btnGame.setOnClickListener(v -> {
            if (getCompletedGoalsCount() == 4) {
                // Launch mini-game activity
                startMiniGame();
            } else {
                Toast.makeText(this, "Complete all daily goals to unlock mini-games!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startMiniGame() {
        // You can create different mini-games and randomize
        String[] games = {"Memory Game", "Pet Care Game", "Puzzle Game"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose a Mini-Game");
        builder.setItems(games, (dialog, which) -> {
            Toast.makeText(this, "Starting " + games[which] + "...", Toast.LENGTH_SHORT).show();
            // Here you would start the actual game activity
            // startActivity(new Intent(this, MiniGameActivity.class));
        });
        builder.show();
    }

    private void showAyarlarDialog() {
        String[] options = {"Languages", "Pet name", "Color", "Reset Progress"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    // Language change
                    Toast.makeText(this, "Language settings coming soon!", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    // Pet name change
                    changePetName();
                    break;
                case 2:
                    // Color change
                    Toast.makeText(this, "Color settings coming soon!", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    // Reset progress
                    resetDailyProgress();
                    break;
            }
        });
        builder.show();
    }

    private void changePetName() {
        // Simple pet name change dialog
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

            // Also reset individual goal progress
            getSharedPreferences("waterPrefs", MODE_PRIVATE).edit().putInt("totalDrank", 0).apply();

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