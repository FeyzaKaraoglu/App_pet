package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    public static MainActivity context;

    Button btnSettings, btnGoals, btnGame;
    TextView fuelCountText, lvl1, lvl2, lvl3, lvl4, petNameView;

    SharedPreferences goalPrefs, fuelPrefs;

    boolean waterGoalCompleted = false;
    boolean stepsGoalCompleted = false;
    boolean sleepGoalCompleted = false;
    boolean focusGoalCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applyLanguageSettings(); // Dil ayarını uygula

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

    // -----------------------------
    // DİL AYARI METOTLARI
    // -----------------------------
    private void applyLanguageSettings() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String langCode = prefs.getString("app_language", "en");
        setAppLocale(langCode);
    }

    private void setAppLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
    // -----------------------------

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
        updateLifeIndicator(lvl1, waterGoalCompleted, "");
        updateLifeIndicator(lvl2, stepsGoalCompleted, "");
        updateLifeIndicator(lvl3, sleepGoalCompleted, "");
        updateLifeIndicator(lvl4, focusGoalCompleted, "");

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
        fuelCountText.setText(" " + currentFuel);
        updatePetName();
    }

    public void markGoalCompleted(String goalType) {
        SharedPreferences.Editor goalEditor = goalPrefs.edit();

        switch (goalType.toLowerCase()) {
            case "water":
                if (!waterGoalCompleted) {
                    goalEditor.putBoolean("waterCompleted", true);
                    waterGoalCompleted = true;
                    showGoalCompletedMessage(getString(R.string.water_goal_completed));
                }
                break;
            case "steps":
                if (!stepsGoalCompleted) {
                    goalEditor.putBoolean("stepsCompleted", true);
                    stepsGoalCompleted = true;
                    showGoalCompletedMessage(getString(R.string.steps_goal_completed));
                }
                break;
            case "sleep":
                if (!sleepGoalCompleted) {
                    goalEditor.putBoolean("sleepCompleted", true);
                    sleepGoalCompleted = true;
                    showGoalCompletedMessage(getString(R.string.sleep_goal_completed));
                }
                break;
            case "focus":
                if (!focusGoalCompleted) {
                    goalEditor.putBoolean("focusCompleted", true);
                    focusGoalCompleted = true;
                    showGoalCompletedMessage(getString(R.string.focus_goal_completed));
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
                            getString(R.string.not_enough_fuel_message),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // -----------------------------
    // SETTINGS DIALOG
    // -----------------------------
    private void showSettingsDialog() {
        String[] settingsOptions = {
                getString(R.string.settings_language),
                getString(R.string.settings_pet_name),
                getString(R.string.settings_color),
                getString(R.string.settings_reset)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.settings));
        builder.setItems(settingsOptions, (dialog, which) -> {
            switch (which) {
                case 0: showLanguageDialog(); break;
                case 1: showPetNameDialog(); break;
                case 2: showColorDialog(); break;
                case 3: confirmResetProgress(); break;
            }
        });
        builder.show();
    }

    private void showLanguageDialog() {
        String[] languages = {"🇺🇸 English", "🇹🇷 Türkçe", "🇩🇪 Deutsch"};
        final String[] langCodes = {"en","tr","de"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.languages));
        builder.setItems(languages, (dialog, which) -> {
            String selectedLang = langCodes[which];
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            prefs.edit().putString("app_language", selectedLang).apply();
            recreate();
        });
        builder.show();
    }

    private void showPetNameDialog() {
        EditText input = new EditText(this);
        input.setHint(getString(R.string.pet_name_hint));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.change_pet_name));
        builder.setView(input);
        builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                goalPrefs.edit().putString("petName", newName).apply();
                updatePetName();
                Toast.makeText(this, String.format(getString(R.string.pet_name_changed), newName), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showColorDialog() {
        String[] colors = {
                getString(R.string.dialog_color_purple),
                getString(R.string.dialog_color_yellow),
                getString(R.string.dialog_color_green)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.choose_pet_color));
        builder.setItems(colors, (dialog, which) -> {
            String selectedColor;
            switch (which) {
                case 0: selectedColor = "Purple"; break;
                case 1: selectedColor = "Yellow"; break;
                default: selectedColor = "Green"; break;
            }

            goalPrefs.edit().putString("petColor", selectedColor).apply();
            updatePetColor();

            Toast.makeText(this,
                    String.format(getString(R.string.pet_color_changed), colors[which]),
                    Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    private void confirmResetProgress() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.reset_progress_title));
        builder.setMessage(getString(R.string.reset_progress_message));
        builder.setPositiveButton(getString(R.string.yes), (dialog, which) -> resetDailyProgress());
        builder.setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void resetDailyProgress() {
        // Clear main activity preferences
        goalPrefs.edit().clear().apply();
        fuelPrefs.edit().clear().apply();

        // Clear individual goal activity preferences with correct names
        getSharedPreferences("waterPrefs", MODE_PRIVATE).edit().clear().apply();
        getSharedPreferences("stepsPrefs", MODE_PRIVATE).edit().clear().apply();
        getSharedPreferences("SleepPrefs", MODE_PRIVATE).edit().clear().apply();  // Note: Capital S
        getSharedPreferences("FocusPrefs", MODE_PRIVATE).edit().clear().apply();  // Note: Capital F

        // Call forceResetFromMain() methods if activities are running
        if (FocusGoalActivity.instance != null) {
            FocusGoalActivity.instance.forceResetFromMain();
        }
        if (SleepGoalActivity.instance != null) {
            SleepGoalActivity.instance.forceResetFromMain();
        }
        if (StepsGoalActivity.instance != null) {
            StepsGoalActivity.instance.forceResetFromMain();
        }

        // Clear any other app-wide preferences except language settings
        SharedPreferences appPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String savedLanguage = appPrefs.getString("app_language", "en");
        appPrefs.edit().clear().apply();
        appPrefs.edit().putString("app_language", savedLanguage).apply();

        // Reinitialize the data
        checkDailyReset();
        updateGoalChart();
        updateFuelDisplay();
        updatePetName();
        updatePetColor();

        Toast.makeText(this, getString(R.string.progress_reset_success), Toast.LENGTH_SHORT).show();
    }

    // -----------------------------
    // GOALS
    // -----------------------------
    private void showNoFuelDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.no_fuel_title));
        builder.setMessage(getString(R.string.no_fuel_message));
        builder.setPositiveButton(getString(R.string.view_goals), (dialog, which) -> showGoalsDialog());
        builder.setNegativeButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showGoalsDialog() {
        String[] options = {
                getString(R.string.goal_water),
                getString(R.string.goal_steps),
                getString(R.string.goal_sleep),
                getString(R.string.goal_focus)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.daily_goals));
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: startActivity(new Intent(this, WaterGoalActivity.class)); break;
                case 1: startActivity(new Intent(this, StepsGoalActivity.class)); break;
                case 2: startActivity(new Intent(this, SleepGoalActivity.class)); break;
                case 3: startActivity(new Intent(this, FocusGoalActivity.class)); break;
            }
        });
        builder.show();
    }

    private void updatePetColor() {
        String color = goalPrefs.getString("petColor", "Green");
        ImageView petImage = findViewById(R.id.petImage);

        switch (color) {
            case "Purple": petImage.setImageResource(R.drawable.pet_purple); break;
            case "Yellow": petImage.setImageResource(R.drawable.pet_yellow); break;
            default: petImage.setImageResource(R.drawable.pet_green); break;
        }
    }

    private void updatePetName() {
        try {
            String savedPetName = goalPrefs.getString("petName", getString(R.string.default_pet_name));
            if (petNameView != null) petNameView.setText(savedPetName);
        } catch (Exception e) { e.printStackTrace(); }
    }
}