package com.example.myapplication;

import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WaterGoalActivity extends AppCompatActivity {

    EditText etGoal, etDrink;
    Button btnSetGoal, btnAddDrink, btnAddCup, btnBack;
    TextView tvProgress, tvStatus;
    ProgressBar progressBar;

    int dailyGoal = 0;
    int totalDrank = 0;
    final int CUP_AMOUNT = 200; // 1 cup = 200 ml

    SharedPreferences prefs;
    SharedPreferences goalPrefs;
    boolean goalCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_water_goal);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupPreferences();
        checkDailyReset();
        updateProgress();
        setupClickListeners();
    }

    private void initializeViews() {
        etGoal = findViewById(R.id.etGoal);
        etDrink = findViewById(R.id.etDrink);
        btnSetGoal = findViewById(R.id.btnSetGoal);
        btnAddDrink = findViewById(R.id.btnAddDrink);
        btnAddCup = findViewById(R.id.btnAddCup);
        btnBack = findViewById(R.id.btnBack);
        tvProgress = findViewById(R.id.tvProgress);

        // Add these views to your XML layout
        tvStatus = findViewById(R.id.tvStatus); // Add this TextView to show completion status
        progressBar = findViewById(R.id.progressBar); // Add this ProgressBar to show visual progress
    }

    private void setupPreferences() {
        prefs = getSharedPreferences("waterPrefs", MODE_PRIVATE);
        goalPrefs = getSharedPreferences("goalCompletionPrefs", MODE_PRIVATE);
    }

    private void checkDailyReset() {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String savedDate = prefs.getString("lastDate", today);

        if (!today.equals(savedDate)) {
            totalDrank = 0; // new day, reset
            goalCompleted = false;
            prefs.edit().putString("lastDate", today).apply();
            prefs.edit().putInt("totalDrank", totalDrank).apply();
        } else {
            totalDrank = prefs.getInt("totalDrank", 0);
            goalCompleted = goalPrefs.getBoolean("waterCompleted", false);
        }

        dailyGoal = prefs.getInt("dailyGoal", 2000); // Default 2L goal

        // Set default goal if none exists
        if (dailyGoal == 0) {
            dailyGoal = 2000;
            prefs.edit().putInt("dailyGoal", dailyGoal).apply();
        }

        etGoal.setText(String.valueOf(dailyGoal));
    }

    private void setupClickListeners() {
        // Set daily goal
        btnSetGoal.setOnClickListener(v -> setDailyGoal());

        // Add custom amount of water
        btnAddDrink.setOnClickListener(v -> addCustomWater());

        // Add one cup of water
        btnAddCup.setOnClickListener(v -> addCupOfWater());

        // Back to MainActivity
        btnBack.setOnClickListener(v -> goBackToMain());
    }

    private void setDailyGoal() {
        String goalText = etGoal.getText().toString().trim();
        if (goalText.isEmpty()) {
            Toast.makeText(this, "Please enter a daily goal!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int newGoal = Integer.parseInt(goalText);
            if (newGoal <= 0) {
                Toast.makeText(this, "Goal must be greater than 0!", Toast.LENGTH_SHORT).show();
                return;
            }

            dailyGoal = newGoal;

            // Reset progress if changing goal
            totalDrank = 0;
            goalCompleted = false;

            // Save to preferences
            String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("dailyGoal", dailyGoal);
            editor.putInt("totalDrank", totalDrank);
            editor.putString("lastDate", today);
            editor.apply();

            // Update goal completion status
            goalPrefs.edit().putBoolean("waterCompleted", false).apply();

            updateProgress();
            Toast.makeText(this, "Daily goal set: " + dailyGoal + " ml", Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number!", Toast.LENGTH_SHORT).show();
        }
    }

    private void addCustomWater() {
        String drinkText = etDrink.getText().toString().trim();
        if (drinkText.isEmpty()) {
            Toast.makeText(this, "Please enter the amount of water!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int amount = Integer.parseInt(drinkText);
            if (amount <= 0) {
                Toast.makeText(this, "Amount must be greater than 0!", Toast.LENGTH_SHORT).show();
                return;
            }

            addWater(amount);
            etDrink.setText(""); // Clear input field

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number!", Toast.LENGTH_SHORT).show();
        }
    }

    private void addCupOfWater() {
        addWater(CUP_AMOUNT);
    }

    private void addWater(int amount) {
        if (goalCompleted) {
            Toast.makeText(this, "Daily water goal already completed! 🎉", Toast.LENGTH_SHORT).show();
            return;
        }

        totalDrank += amount;
        updateProgress();

        // Save progress
        prefs.edit().putInt("totalDrank", totalDrank).apply();

        // Check if goal is completed
        if (totalDrank >= dailyGoal && !goalCompleted) {
            goalCompleted = true;

            // Save completion status
            goalPrefs.edit().putBoolean("waterCompleted", true).apply();

            // Notify MainActivity about goal completion
            notifyGoalCompletion();

            // Show completion message
            Toast.makeText(this, "🎉 Water goal completed! Your pet gained a life!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Added " + amount + " ml. Keep going!", Toast.LENGTH_SHORT).show();
        }
    }

    private void notifyGoalCompletion() {
        // This method can be used to send a broadcast or call MainActivity method
        // For now, we'll use SharedPreferences which MainActivity checks

        // You could also use a more sophisticated approach like:
        // Intent intent = new Intent("GOAL_COMPLETED");
        // intent.putExtra("goalType", "water");
        // sendBroadcast(intent);
    }

    private void updateProgress() {
        // Update progress text
        tvProgress.setText("Progress: " + totalDrank + " / " + dailyGoal + " ml");

        // Update progress bar
        if (dailyGoal > 0) {
            int percentage = Math.min(100, (totalDrank * 100) / dailyGoal);
            progressBar.setProgress(percentage);
        }

        // Update status
        if (goalCompleted || totalDrank >= dailyGoal) {
            tvStatus.setText("✅ Goal Completed! 🎉");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            int remaining = dailyGoal - totalDrank;
            tvStatus.setText("💧 " + remaining + " ml remaining");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        }

        // Disable buttons if goal completed
        if (goalCompleted) {
            btnAddDrink.setText("Goal Completed! 🎉");
            btnAddCup.setText("Goal Completed! 🎉");
            btnAddDrink.setEnabled(false);
            btnAddCup.setEnabled(false);
        } else {
            btnAddDrink.setText("Add Water");
            btnAddCup.setText("Add Cup (200ml)");
            btnAddDrink.setEnabled(true);
            btnAddCup.setEnabled(true);
        }
    }

    private void goBackToMain() {
        Intent intent = new Intent(WaterGoalActivity.this, MainActivity.class);
        // Clear stack and return to MainActivity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}