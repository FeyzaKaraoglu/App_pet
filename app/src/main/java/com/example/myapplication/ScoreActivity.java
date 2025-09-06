package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

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
        mainMenuButton = findViewById(R.id.retryButton2); // XML'de retryButton2 olarak tanımlanmış

        int score = getIntent().getIntExtra("score", 0);

        // High score'u kaydetmek için SharedPreferences kullan
        SharedPreferences prefs = getSharedPreferences("game_prefs", Context.MODE_PRIVATE);
        int highScore = prefs.getInt("high_score", 0);

        if (score > highScore) {
            highScore = score;
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("high_score", highScore);
            editor.apply();
        }

        // TextView'lere skorları yazdır
        scoreText.setText("Your Score: " + score);
        highScoreText.setText("High Score: " + highScore);

        // Retry butonu: yeniden oyunu başlat
        retryButton.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(ScoreActivity.this, MiniGameActivity.class);
                startActivity(intent);
                finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Main Menu butonu: Ana menüye dön
        mainMenuButton.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(ScoreActivity.this, MainActivity.class);
                // Tüm activity stack'i temizle ve ana menüye dön
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}