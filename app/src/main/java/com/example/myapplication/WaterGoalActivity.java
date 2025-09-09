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
    final int CUP_AMOUNT = 200;

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
        tvStatus = findViewById(R.id.tvStatus);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupPreferences() {
        prefs = getSharedPreferences("waterPrefs", MODE_PRIVATE);
        goalPrefs = getSharedPreferences("goalCompletionPrefs", MODE_PRIVATE);
    }

    private void checkDailyReset() {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String savedDate = prefs.getString("lastDate", today);

        if (!today.equals(savedDate)) {
            totalDrank = 0;
            goalCompleted = false;
            prefs.edit().putString("lastDate", today).apply();
            prefs.edit().putInt("totalDrank", totalDrank).apply();
        } else {
            totalDrank = prefs.getInt("totalDrank", 0);
            goalCompleted = goalPrefs.getBoolean("waterCompleted", false);
        }

        dailyGoal = prefs.getInt("dailyGoal", 2000);

        if (dailyGoal == 0) {
            dailyGoal = 2000;
            prefs.edit().putInt("dailyGoal", dailyGoal).apply();
        }

        etGoal.setText(String.valueOf(dailyGoal));
    }

    private void setupClickListeners() {
        btnSetGoal.setOnClickListener(v -> setDailyGoal());
        btnAddDrink.setOnClickListener(v -> addCustomWater());
        btnAddCup.setOnClickListener(v -> addCupOfWater());
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
            totalDrank = 0;
            goalCompleted = false;

            String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("dailyGoal", dailyGoal);
            editor.putInt("totalDrank", totalDrank);
            editor.putString("lastDate", today);
            editor.apply();

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
            etDrink.setText("");

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
        prefs.edit().putInt("totalDrank", totalDrank).apply();
        updateProgress();

        if (totalDrank >= dailyGoal && !goalCompleted) {
            goalCompleted = true;
            goalPrefs.edit().putBoolean("waterCompleted", true).apply();

            // 🔥 Fuel ekle (artık burada)
            SharedPreferences fuelPrefs = getSharedPreferences("fuelPrefs", MODE_PRIVATE);
            int currentFuel = fuelPrefs.getInt("totalFuel", 0);
            fuelPrefs.edit().putInt("totalFuel", currentFuel + 1).apply();

            Toast.makeText(this, "🎉 Water goal completed! +1 Fuel ⛽", Toast.LENGTH_LONG).show();
            btnAddDrink.setText("Goal Completed! 🎉");
            btnAddCup.setText("Goal Completed! 🎉");
            btnAddDrink.setEnabled(false);
            btnAddCup.setEnabled(false);
        } else {
            Toast.makeText(this, "Added " + amount + " ml. Keep going!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProgress() {
        tvProgress.setText("Progress: " + totalDrank + " / " + dailyGoal + " ml");

        if (dailyGoal > 0) {
            int percentage = Math.min(100, (totalDrank * 100) / dailyGoal);
            progressBar.setProgress(percentage);
        }

        if (goalCompleted || totalDrank >= dailyGoal) {
            tvStatus.setText("✅ Goal Completed! 🎉");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            int remaining = dailyGoal - totalDrank;
            tvStatus.setText("💧 " + remaining + " ml remaining");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        }

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
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
