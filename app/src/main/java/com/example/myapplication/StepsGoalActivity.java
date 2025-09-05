package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepsGoalActivity extends AppCompatActivity implements SensorEventListener {

    EditText etStepGoal;
    Button btnSetStepGoal, btnBack;
    TextView tvStepProgress;

    int dailyStepGoal = 0;
    int stepsAtReset = 0;
    int todaySteps = 0;

    SharedPreferences stepsPrefs;
    SharedPreferences goalPrefs;
    SensorManager sensorManager;
    Sensor stepCounter;

    String today;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_steps_goal);

        etStepGoal = findViewById(R.id.etStepGoal);
        btnSetStepGoal = findViewById(R.id.btnSetStepGoal);
        btnBack = findViewById(R.id.btnBack);
        tvStepProgress = findViewById(R.id.tvStepProgress);

        stepsPrefs = getSharedPreferences("stepsPrefs", MODE_PRIVATE);
        goalPrefs = getSharedPreferences("goalCompletionPrefs", MODE_PRIVATE);

        today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String savedDate = stepsPrefs.getString("lastDate", today);

        dailyStepGoal = stepsPrefs.getInt("dailyStepGoal", 0);

        if (!today.equals(savedDate)) {
            stepsAtReset = 0;
            stepsPrefs.edit().putString("lastDate", today).apply();
            stepsPrefs.edit().putInt("stepsAtReset", 0).apply();
            stepsPrefs.edit().putInt("todaySteps", 0).apply();
        } else {
            stepsAtReset = stepsPrefs.getInt("stepsAtReset", 0);
        }

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }

        if (stepCounter == null) {
            Toast.makeText(this, "Step Counter sensor not available!", Toast.LENGTH_LONG).show();
        }

        updateStepProgress();

        btnSetStepGoal.setOnClickListener(v -> {
            String goalText = etStepGoal.getText().toString();
            if (goalText.isEmpty()) {
                Toast.makeText(this, "Please enter a daily step goal!", Toast.LENGTH_SHORT).show();
                return;
            }
            dailyStepGoal = Integer.parseInt(goalText);
            stepsAtReset = todaySteps; // Yeni hedefte sıfırdan başlat
            stepsPrefs.edit().putInt("dailyStepGoal", dailyStepGoal).apply();
            stepsPrefs.edit().putInt("stepsAtReset", stepsAtReset).apply();
            stepsPrefs.edit().putString("lastDate", today).apply();

            updateStepProgress();
            Toast.makeText(this, "Step goal set: " + dailyStepGoal, Toast.LENGTH_SHORT).show();
        });

        btnBack.setOnClickListener(v -> {
            // MainActivity’ye dön
            Intent intent = new Intent(StepsGoalActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (stepCounter != null) {
            sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (stepCounter != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int totalSinceBoot = (int) event.values[0];
            todaySteps = totalSinceBoot - stepsAtReset;
            stepsPrefs.edit().putInt("todaySteps", todaySteps).apply();
            updateStepProgress();

            // Hedef tamamlandıysa goalPrefs güncelle
            if (dailyStepGoal > 0 && todaySteps >= dailyStepGoal) {
                boolean alreadyCompleted = goalPrefs.getBoolean("stepsCompleted", false);
                if (!alreadyCompleted) {
                    goalPrefs.edit().putBoolean("stepsCompleted", true).apply();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void updateStepProgress() {
        todaySteps = stepsPrefs.getInt("todaySteps", 0);
        tvStepProgress.setText("Steps: " + todaySteps + " / " + dailyStepGoal);
    }
}
