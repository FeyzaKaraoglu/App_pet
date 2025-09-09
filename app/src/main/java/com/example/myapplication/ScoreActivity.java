package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class ScoreActivity extends AppCompatActivity {

    private TextView scoreText;
    private TextView highScoreText;
    private Button retryButton;
    private Button mainMenuButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.score_activity);

        scoreText = findViewById(R.id.scoreText);
        highScoreText = findViewById(R.id.highScoreText);
        retryButton = findViewById(R.id.retryButton);
        mainMenuButton = findViewById(R.id.retryButton2);

        int score = getIntent().getIntExtra("score", 0);

        // High score kontrolü
        SharedPreferences prefs = getSharedPreferences("game_prefs", Context.MODE_PRIVATE);
        int highScore = prefs.getInt("high_score", 0);
        if (score > highScore) {
            highScore = score;
            prefs.edit().putInt("high_score", highScore).apply();
        }

        // Skorları göster
        scoreText.setText("Your Score: " + score);
        highScoreText.setText("High Score: " + highScore);

        // Retry butonu: fuel kontrolü
        retryButton.setOnClickListener(v -> {
            SharedPreferences fuelPrefs = getSharedPreferences("fuelPrefs", Context.MODE_PRIVATE);
            int currentFuel = fuelPrefs.getInt("totalFuel", 0);

            if (currentFuel > 0) {
                // Fuel varsa 1 fuel harca ve oyunu başlat
                fuelPrefs.edit().putInt("totalFuel", currentFuel - 1).apply();

                Intent intent = new Intent(ScoreActivity.this, MiniGameActivity.class);
                startActivity(intent);
                finish();
            } else {
                boolean anyGoalCompleted = MainActivity.context != null &&
                        (MainActivity.context.waterGoalCompleted ||
                                MainActivity.context.stepsGoalCompleted ||
                                MainActivity.context.sleepGoalCompleted ||
                                MainActivity.context.focusGoalCompleted);

                if (!anyGoalCompleted) {
                    new AlertDialog.Builder(ScoreActivity.this)
                            .setTitle("No Fuel Available! ⛽")
                            .setMessage("You need to complete daily goals to earn fuel and play games!")
                            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                            .show();
                } else {
                    Toast.makeText(ScoreActivity.this,
                            "Not enough fuel! Complete more daily goals to earn fuel ⛽",
                            Toast.LENGTH_LONG).show();
                }
            }

            if (MainActivity.context != null) {
                MainActivity.context.updateFuelDisplay();
            }
        });

        // Main Menu butonu
        mainMenuButton.setOnClickListener(v -> {
            Intent intent = new Intent(ScoreActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}
