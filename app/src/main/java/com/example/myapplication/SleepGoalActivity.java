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
    int totalSleptMinutes = 0;
    boolean isSleeping = false;

    // Wake-up detection variables
    private boolean lightThresholdMet = false;
    private boolean movementThresholdMet = false;
    private long lastWakeCheckTime = 0;
    private static final long WAKE_CHECK_COOLDOWN = 5000; // 5 seconds cooldown
    private static final float LIGHT_WAKE_THRESHOLD = 30.0f; // lux
    private static final float MOVEMENT_WAKE_THRESHOLD = 2.0f; // m/s²

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
                handler.postDelayed(this, 60000); // Update every minute
            }
        }
    };

    // Runnable to reset wake detection flags after some time
    Runnable resetWakeFlags = new Runnable() {
        @Override
        public void run() {
            lightThresholdMet = false;
            movementThresholdMet = false;
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
            String text = etSleepGoal.getText().toString().trim();
            if (!text.isEmpty()) {
                try {
                    goalHours = Integer.parseInt(text);
                    if (goalHours <= 0) {
                        Toast.makeText(this, "Please enter a positive number!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    prefs.edit().putInt("goal", goalHours).apply();
                    updateProgress();
                    Toast.makeText(this, "Sleep goal set: " + goalHours + "h", Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter a valid number!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please enter a sleep goal!", Toast.LENGTH_SHORT).show();
            }
        });

        btnStartSleep.setOnClickListener(v -> startSleepMode());
        btnStopSleep.setOnClickListener(v -> stopSleepMode());

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

        // Reset wake detection flags
        lightThresholdMet = false;
        movementThresholdMet = false;
        lastWakeCheckTime = 0;

        // Register sensors
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(this, "Light sensor not available", Toast.LENGTH_SHORT).show();
        }

        if (accelSensor != null) {
            sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(this, "Accelerometer not available", Toast.LENGTH_SHORT).show();
        }

        handler.post(sleepRunnable);
        updateButtonStates();
        Toast.makeText(this, "Sleep mode started! Place phone face down in dark room", Toast.LENGTH_LONG).show();
    }

    private void stopSleepMode() {
        if (!isSleeping) return;

        isSleeping = false;
        handler.removeCallbacks(sleepRunnable);
        handler.removeCallbacks(resetWakeFlags);

        long elapsedMillis = System.currentTimeMillis() - startTime;
        int minutesElapsed = (int) (elapsedMillis / 1000 / 60);
        totalSleptMinutes += minutesElapsed;
        prefs.edit().putInt("progress", totalSleptMinutes).apply();

        sensorManager.unregisterListener(this);
        updateProgress();
        updateButtonStates();
        tvSleepTimer.setText("Elapsed: 0h 0m");

        Toast.makeText(this, "Sleep session recorded: " + minutesElapsed + " minutes", Toast.LENGTH_SHORT).show();

        // Check if goal is completed and notify MainActivity
        if (totalSleptMinutes / 60 >= goalHours) {
            if (MainActivity.context != null) {
                MainActivity.context.markGoalCompleted("sleep");
            }
        }

        // Reset wake detection flags
        lightThresholdMet = false;
        movementThresholdMet = false;
    }

    private void updateProgress() {
        int hoursSlept = totalSleptMinutes / 60;
        int minutesSlept = totalSleptMinutes % 60;
        String progressText = "Progress: " + hoursSlept + "h " + minutesSlept + "m / " + goalHours + "h";
        if (hoursSlept >= goalHours) progressText += " ✓ GOAL ACHIEVED!";
        tvSleepProgress.setText(progressText);
    }

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

        long currentTime = System.currentTimeMillis();

        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lux = event.values[0];
            if (lux > LIGHT_WAKE_THRESHOLD) {
                lightThresholdMet = true;
                checkWakeUp(currentTime);
            }
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Calculate acceleration magnitude excluding gravity
            double magnitude = Math.sqrt(x*x + y*y + z*z);
            double acceleration = Math.abs(magnitude - SensorManager.GRAVITY_EARTH);

            if (acceleration > MOVEMENT_WAKE_THRESHOLD) {
                movementThresholdMet = true;
                checkWakeUp(currentTime);
            }
        }
    }

    private void checkWakeUp(long currentTime) {
        // Check if both conditions are met and we're not in cooldown period
        if (lightThresholdMet && movementThresholdMet &&
                (currentTime - lastWakeCheckTime) > WAKE_CHECK_COOLDOWN) {

            lastWakeCheckTime = currentTime;

            // Show toast for debugging
            Toast.makeText(this, "Wake-up detected: Light + Movement", Toast.LENGTH_SHORT).show();

            // Stop sleep mode
            stopSleepMode();
        } else if (lightThresholdMet || movementThresholdMet) {
            // Reset flags after 10 seconds if only one condition is met
            handler.removeCallbacks(resetWakeFlags);
            handler.postDelayed(resetWakeFlags, 10000);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            if (sensor.getType() == Sensor.TYPE_LIGHT) {
                Toast.makeText(this, "Light sensor accuracy is low", Toast.LENGTH_SHORT).show();
            } else if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                Toast.makeText(this, "Accelerometer accuracy is low", Toast.LENGTH_SHORT).show();
            }
        }
    }
}