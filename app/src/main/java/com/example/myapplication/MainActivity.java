package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;

public class MainActivity extends AppCompatActivity {

    Button btnSettings;
    Button btnGoals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSettings = findViewById(R.id.btnSettings);
        btnGoals = findViewById(R.id.btnGoals);

        // Settings menüsü
        btnSettings.setOnClickListener(v -> showAyarlarDialog());

        // Goals menüsü
        btnGoals.setOnClickListener(v -> showGoalsDialog());
    }

    private void showAyarlarDialog() {
        String[] options = {"Languages", "Pet name", "Color", "Reset"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    // Dil değiştir
                    break;
                case 1:
                    // Pet ismi değiştir
                    break;
                case 2:
                    // Renk değiştir
                    break;
                case 3:
                    // Sıfırla
                    break;
            }
        });
        builder.show();
    }

    private void showGoalsDialog() {
        String[] options = {"Water Goal", "Steps Goal", "Sleeping Goal", "Focusing Goal"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Goals");
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
