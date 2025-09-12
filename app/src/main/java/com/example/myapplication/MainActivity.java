package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static MainActivity context;

    Button btnSettings, btnGoals, btnGame;
    TextView fuelCountText, lvl1, lvl2, lvl3, lvl4,petNameView;

    SharedPreferences goalPrefs, fuelPrefs;

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
        updateFuelDisplay();
        setupClickListeners();
        updatePetColor();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGoalChart();
        updatePetName();
        updateFuelDisplay();
        updatePetColor();
    }

    private void initializeViews() {
        btnSettings = findViewById(R.id.btnSettings);
        btnGoals = findViewById(R.id.btnGoals);
        LinearLayout bottomBar = findViewById(R.id.bottomBar);
        btnGame = bottomBar.findViewById(R.id.btnGame);

        fuelCountText = findViewById(R.id.fuelCountText);
        lvl1 = findViewById(R.id.lvl1);
        lvl2 = findViewById(R.id.lvl2);
        lvl3 = findViewById(R.id.lvl3);
        lvl4 = findViewById(R.id.lvl4);

        petNameView = findViewById(R.id.textView);
    }

    private void setupPreferences() {
        goalPrefs = getSharedPreferences("goalCompletionPrefs", MODE_PRIVATE);
        fuelPrefs = getSharedPreferences("fuelPrefs", MODE_PRIVATE);
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

            fuelPrefs.edit().putInt("totalFuel", 0).apply();
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

        updatePetName();
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

    public void updateFuelDisplay() {
        int currentFuel = fuelPrefs.getInt("totalFuel", 0);
        fuelCountText.setText("⛽ " + currentFuel);
        updatePetName();
    }

    public void markGoalCompleted(String goalType) {
        SharedPreferences.Editor goalEditor = goalPrefs.edit();

        switch (goalType.toLowerCase()) {
            case "water":
                if (!waterGoalCompleted) {
                    goalEditor.putBoolean("waterCompleted", true);
                    waterGoalCompleted = true;
                    showGoalCompletedMessage("Water goal completed! +1 Fuel ⛽");
                }
                break;
            case "steps":
                if (!stepsGoalCompleted) {
                    goalEditor.putBoolean("stepsCompleted", true);
                    stepsGoalCompleted = true;
                    showGoalCompletedMessage("Steps goal completed! +1 Fuel ⛽");
                }
                break;
            case "sleep":
                if (!sleepGoalCompleted) {
                    goalEditor.putBoolean("sleepCompleted", true);
                    sleepGoalCompleted = true;
                    showGoalCompletedMessage("Sleep goal completed! +1 Fuel ⛽");
                }
                break;
            case "focus":
                if (!focusGoalCompleted) {
                    goalEditor.putBoolean("focusCompleted", true);
                    focusGoalCompleted = true;
                    showGoalCompletedMessage("Focus goal completed! +1 Fuel ⛽");
                }
                break;
        }

        goalEditor.apply();
        updateGoalChart();
        updateFuelDisplay();
    }

    public static void consumeFuel(int amount) {
        if (context != null) {
            int currentFuel = context.fuelPrefs.getInt("totalFuel", 0);
            int newFuel = Math.max(0, currentFuel - amount);
            context.fuelPrefs.edit().putInt("totalFuel", newFuel).apply();
            context.updateFuelDisplay();
        }
    }

    private void showGoalCompletedMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void setupClickListeners() {
        btnSettings.setOnClickListener(v -> showSettingsDialog());
        btnGoals.setOnClickListener(v -> showGoalsDialog());
        btnGame.setOnClickListener(v -> {
            int currentFuel = fuelPrefs.getInt("totalFuel", 0);
            if (currentFuel > 0) {
                consumeFuel(1);
                startActivity(new Intent(MainActivity.this, MiniGameActivity.class));
            } else {
                boolean anyGoalCompleted = waterGoalCompleted || stepsGoalCompleted ||
                        sleepGoalCompleted || focusGoalCompleted;

                if (!anyGoalCompleted) {
                    showNoFuelDialog();
                } else {
                    Toast.makeText(this,
                            "Not enough fuel! Complete more daily goals to earn fuel ⛽",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showNoFuelDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No Fuel Available! ⛽");
        builder.setMessage("You need to complete daily goals to earn fuel and play games!\n\n" +
                "Available goals:\n" +
                "💧 Water Goal - Drink your daily water\n" +
                "👟 Steps Goal - Walk your daily steps\n" +
                "😴 Sleep Goal - Get enough sleep\n" +
                "🎯 Focus Goal - Focus on your tasks\n\n" +
                "Each completed goal gives you 1 fuel!");

        builder.setPositiveButton("View Goals", (dialog, which) -> {
            showGoalsDialog();
        });

        builder.setNegativeButton("OK", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.show();
    }

    private void showGoalsDialog() {
        String[] options = {"💧 Water Goal", "👟 Steps Goal", "😴 Sleeping Goal", "🎯 Focusing Goal"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Daily Goals - Earn Fuel!");
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

    private void showSettingsDialog() {
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
                    changePetColor();
                    break;
                case 3:
                    resetDailyProgress();
                    break;
            }
        });
        builder.show();
    }
    private void changePetColor() {
        String[] colors = {"Purple", "Yellow", "Green"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Pet Color");
        builder.setItems(colors, (dialog, which) -> {
            String selectedColor = colors[which];
            goalPrefs.edit().putString("petColor", selectedColor).apply(); // seçimi kaydet
            updatePetColor(); // ekranda değiştir
        });
        builder.show();
    }
    private void updatePetColor() {
        String color = goalPrefs.getString("petColor", "Green"); // varsayılan Red
        ImageView petImage = findViewById(R.id.petImage);

        switch (color) {
            case "Purple":
                petImage.setImageResource(R.drawable.pet_purple);
                break;
            case "Yellow":
                petImage.setImageResource(R.drawable.pet_yellow);
                break;
            default:
                petImage.setImageResource(R.drawable.pet_green);
                break;
        }
    }

    private void changePetName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.change_pet_name));
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint(getString(R.string.enter_new_name));
        builder.setView(input);
        builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                petNameView.setText(newName);
                goalPrefs.edit().putString("petName", newName).apply();
                Toast.makeText(this, getString(R.string.pet_name_changed, newName), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());
        builder.show();
    }
    private void updatePetName() {
        try {
            String savedPetName = goalPrefs.getString("petName", "MyPet");
            if (petNameView != null) {
                petNameView.setText(savedPetName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void resetDailyProgress() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Daily Progress");
        builder.setMessage("This will reset all today's goal progress AND fuel. Are you sure?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            SharedPreferences.Editor editor = goalPrefs.edit();
            editor.putBoolean("waterCompleted", false);
            editor.putBoolean("stepsCompleted", false);
            editor.putBoolean("sleepCompleted", false);
            editor.putBoolean("focusCompleted", false);
            editor.apply();

            fuelPrefs.edit().putInt("totalFuel", 0).apply();
            updateGoalChart();
            updateFuelDisplay();

            Toast.makeText(this, "Daily progress and fuel reset!", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
