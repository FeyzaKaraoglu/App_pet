package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class GoalsActivity extends AppCompatActivity {

    Button btnWater, btnSteps, btnSleep, btnFocus, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goals);

        btnWater = findViewById(R.id.wButton);
        btnSteps = findViewById(R.id.stButton);
        btnSleep = findViewById(R.id.slButton);
        btnFocus = findViewById(R.id.fButton);
        btnBack  = findViewById(R.id.mButton);

        btnWater.setOnClickListener(v -> {
            startActivity(new Intent(this, WaterGoalActivity.class));
        });

        btnSteps.setOnClickListener(v -> {
            startActivity(new Intent(this, StepsGoalActivity.class));
        });

        btnSleep.setOnClickListener(v -> {
            startActivity(new Intent(this, SleepGoalActivity.class));
        });

        btnFocus.setOnClickListener(v -> {
            startActivity(new Intent(this, FocusGoalActivity.class));
        });

        btnBack.setOnClickListener(v -> {
            finish(); // ana menüye geri dön
        });
    }
}
