package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SleepGoalActivity extends AppCompatActivity implements SensorEventListener {

    EditText etSleepGoal;
    Button btnSaveGoal, btnStartSleep, btnStopSleep, btnBack;
    TextView tvSleepTimer, tvSleepProgress;

    SharedPreferences prefs;
    SensorManager sensorManager;
    Sensor lightSensor, accelSensor;

    int goalHours = 0;
    int totalSleptMinutes = 0; // progress artık dakika cinsinden
    boolean isSleeping = false;

    Handler handler = new Handler();
    long startTime;

    Runnable sleepRunnable = new Runnable() {
        @Override
        public void run() {
            if (isSleeping) {
                long elapsedMillis = System.currentTimeMillis() - startTime;
                int hours = (int) (elapsedMillis / 1000 / 60 / 60);
                int minutes = (int) ((elapsedMillis / 1000 / 60) % 60);
                tvSleepTimer.setText("Elapsed: " + hours + "h " + minutes + "m");
                handler.postDelayed(this, 60000); // her dakika güncelle
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_goal);

        etSleepGoal = findViewById(R.id.etSleepGoal);
        btnSaveGoal = findViewById(R.id.btnSaveGoal);
        btnStartSleep = findViewById(R.id.btnStartSleep);
        btnStopSleep = findViewById(R.id.btnStopSleep);
        btnBack = findViewById(R.id.btnBack);
        tvSleepTimer = findViewById(R.id.tvSleepTimer);
        tvSleepProgress = findViewById(R.id.tvSleepProgress);

        prefs = getSharedPreferences("SleepPrefs", MODE_PRIVATE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        resetDailyIfNeeded();
        loadGoalAndProgress();
        updateButtonStates();

        btnSaveGoal.setOnClickListener(v -> {
            String text = etSleepGoal.getText().toString();
            if (!text.isEmpty()) {
                goalHours = Integer.parseInt(text);
                prefs.edit().putInt("goal", goalHours).apply();
                updateProgress();
                Toast.makeText(this, "Sleep goal set: " + goalHours + "h", Toast.LENGTH_SHORT).show();
            }
        });

        btnStartSleep.setOnClickListener(v -> startSleepMode());
        btnStopSleep.setOnClickListener(v -> stopSleepMode());

        // 🔹 Back button → MainActivity
        btnBack.setOnClickListener(v -> {
            if (isSleeping) {
                Toast.makeText(this, "Please stop sleep mode first", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(SleepGoalActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void startSleepMode() {
        if (isSleeping) return;

        if (goalHours <= 0) {
            Toast.makeText(this, "Please set a sleep goal first", Toast.LENGTH_SHORT).show();
            return;
        }

        isSleeping = true;
        startTime = System.currentTimeMillis();
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
        handler.post(sleepRunnable);
        updateButtonStates();
        Toast.makeText(this, "Sleep mode started!", Toast.LENGTH_SHORT).show();
    }

    private void stopSleepMode() {
        if (!isSleeping) return;

        isSleeping = false;
        handler.removeCallbacks(sleepRunnable);

        long elapsedMillis = System.currentTimeMillis() - startTime;
        int minutesElapsed = (int) (elapsedMillis / 1000 / 60);
        totalSleptMinutes += minutesElapsed;
        prefs.edit().putInt("progress", totalSleptMinutes).apply();

        sensorManager.unregisterListener(this);
        updateProgress();
        updateButtonStates();
        tvSleepTimer.setText("Elapsed: 0h 0m");

        Toast.makeText(this, "Sleep session recorded: " + minutesElapsed + " minutes", Toast.LENGTH_SHORT).show();
    }

    private void updateProgress() {
        int hoursSlept = totalSleptMinutes / 60;
        int minutesSlept = totalSleptMinutes % 60;
        String progressText = "Progress: " + hoursSlept + "h " + minutesSlept + "m / " + goalHours + "h";
        if (hoursSlept >= goalHours) progressText += " ✓ GOAL ACHIEVED!";
        tvSleepProgress.setText(progressText);
    }

    // 🔹 Yeni eklenen metod
    private void updateButtonStates() {
        btnStartSleep.setEnabled(!isSleeping);
        btnStopSleep.setEnabled(isSleeping);
        btnSaveGoal.setEnabled(!isSleeping);
        etSleepGoal.setEnabled(!isSleeping);

        if (isSleeping) {
            btnStartSleep.setText("Sleep Mode Active...");
            btnStopSleep.setText("Stop Sleep Mode");
        } else {
            btnStartSleep.setText("Start Sleep Mode");
            btnStopSleep.setText("Stop Sleep Mode");
        }
    }

    private void loadGoalAndProgress() {
        goalHours = prefs.getInt("goal", 0);
        totalSleptMinutes = prefs.getInt("progress", 0);
        if (goalHours > 0) etSleepGoal.setText(String.valueOf(goalHours));
        updateProgress();
    }

    private void resetDailyIfNeeded() {
        String lastDate = prefs.getString("lastDate", "");
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        if (!today.equals(lastDate)) {
            prefs.edit().putInt("progress", 0).putString("lastDate", today).apply();
            totalSleptMinutes = 0;
        }
    }

    @Override
    public void onBackPressed() {
        if (isSleeping) {
            Toast.makeText(this, "Please stop sleep mode first", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(SleepGoalActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isSleeping) return;

        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lux = event.values[0];
            if (lux > 20) stopSleepMode();
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            double magnitude = Math.sqrt(x*x + y*y + z*z);
            if (Math.abs(magnitude - SensorManager.GRAVITY_EARTH) > 1) stopSleepMode();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

}
