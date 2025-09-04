package com.example.myapplication;

import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
    TextView tvProgress;

    int dailyGoal = 0;
    int totalDrank = 0;
    final int CUP_AMOUNT = 200; // 1 bardak = 200 ml

    SharedPreferences prefs;

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

        // View'leri bağla
        etGoal = findViewById(R.id.etGoal);
        etDrink = findViewById(R.id.etDrink);
        btnSetGoal = findViewById(R.id.btnSetGoal);
        btnAddDrink = findViewById(R.id.btnAddDrink);
        btnAddCup = findViewById(R.id.btnAddCup);
        btnBack = findViewById(R.id.btnBack);
        tvProgress = findViewById(R.id.tvProgress);

        // SharedPreferences
        prefs = getSharedPreferences("waterPrefs", MODE_PRIVATE);

        // Günlük sıfırlama kontrolü
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String savedDate = prefs.getString("lastDate", today);

        if (!today.equals(savedDate)) {
            totalDrank = 0; // yeni gün, sıfırla
            prefs.edit().putString("lastDate", today).apply();
            prefs.edit().putInt("totalDrank", totalDrank).apply();
        } else {
            totalDrank = prefs.getInt("totalDrank", 0);
        }

        dailyGoal = prefs.getInt("dailyGoal", 0);

        updateProgress();

        // Günlük hedef belirleme
        btnSetGoal.setOnClickListener(v -> {
            String goalText = etGoal.getText().toString();
            if (goalText.isEmpty()) {
                Toast.makeText(this, "Please enter a daily goal!", Toast.LENGTH_SHORT).show();
                return;
            }
            dailyGoal = Integer.parseInt(goalText);
            totalDrank = 0;
            updateProgress();

            // Kaydet
            prefs.edit().putInt("dailyGoal", dailyGoal).apply();
            prefs.edit().putInt("totalDrank", totalDrank).apply();
            prefs.edit().putString("lastDate", today).apply();

            Toast.makeText(this, "Daily goal set: " + dailyGoal + " ml", Toast.LENGTH_SHORT).show();
        });

        // Su ekleme
        btnAddDrink.setOnClickListener(v -> {
            String drinkText = etDrink.getText().toString();
            if (drinkText.isEmpty()) {
                Toast.makeText(this, "Please enter the amount of water!", Toast.LENGTH_SHORT).show();
                return;
            }
            int amount = Integer.parseInt(drinkText);
            totalDrank += amount;
            updateProgress();

            prefs.edit().putInt("totalDrank", totalDrank).apply();
        });

        // 1 bardak ekle
        btnAddCup.setOnClickListener(v -> {
            totalDrank += CUP_AMOUNT;
            updateProgress();

            prefs.edit().putInt("totalDrank", totalDrank).apply();
        });

        // 🔹 Artık GoalsActivity yerine MainActivity’ye dönüyor
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(WaterGoalActivity.this, MainActivity.class);
            // Stack'i temizle → direkt Main'e dön
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void updateProgress() {
        tvProgress.setText("Progress: " + totalDrank + " / " + dailyGoal + " ml");
    }
}
