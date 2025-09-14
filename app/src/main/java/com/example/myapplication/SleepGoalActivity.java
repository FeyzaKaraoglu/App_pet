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
    SharedPreferences goalPrefs;
    SensorManager sensorManager;
    Sensor lightSensor, accelSensor;

    int goalHours = 0;
    int totalSleptMinutes = 0;
    boolean isSleeping = false;
    boolean goalCompleted = false;

    private boolean lightThresholdMet = false;
    private boolean movementThresholdMet = false;
    private long lastWakeCheckTime = 0;
    private static final long WAKE_CHECK_COOLDOWN = 5000; // 5 seconds cooldown
    private static final float LIGHT_WAKE_THRESHOLD = 30.0f; // lux
    private static final float MOVEMENT_WAKE_THRESHOLD = 2.0f; // m/s²

    Handler handler = new Handler();
    long startTime;

    public static SleepGoalActivity instance;

    Runnable sleepRunnable = new Runnable() {
        @Override
        public void run() {
            if (isSleeping) {
                long elapsedMillis = System.currentTimeMillis() - startTime;
                int hours = (int) (elapsedMillis / 1000 / 60 / 60);
                int minutes = (int) ((elapsedMillis / 1000 / 60) % 60);
                tvSleepTimer.setText(getString(R.string.elapsed_format, hours, minutes));
                handler.postDelayed(this, 60000); // Update every minute
            }
        }
    };

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

        instance = this;

        etSleepGoal = findViewById(R.id.etSleepGoal);
        btnSaveGoal = findViewById(R.id.btnSaveGoal);
        btnStartSleep = findViewById(R.id.btnStartSleep);
        btnStopSleep = findViewById(R.id.btnStopSleep);
        btnBack = findViewById(R.id.btnBack);
        tvSleepTimer = findViewById(R.id.tvSleepTimer);
        tvSleepProgress = findViewById(R.id.tvSleepProgress);

        prefs = getSharedPreferences("SleepPrefs", MODE_PRIVATE);
        goalPrefs = getSharedPreferences("goalCompletionPrefs", MODE_PRIVATE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        resetDailyIfNeeded();
        loadGoalAndProgress();
        updateButtonStates();

        btnSaveGoal.setOnClickListener(v -> saveGoal());
        btnStartSleep.setOnClickListener(v -> startSleepMode());
        btnStopSleep.setOnClickListener(v -> stopSleepMode());
        btnBack.setOnClickListener(v -> goBack());
    }

    private void saveGoal() {
        String text = etSleepGoal.getText().toString().trim();
        if (!text.isEmpty()) {
            try {
                goalHours = Integer.parseInt(text);
                if (goalHours <= 0) {
                    Toast.makeText(this, getString(R.string.enter_positive_number), Toast.LENGTH_SHORT).show();
                    return;
                }
                prefs.edit().putInt("goal", goalHours).apply();
                goalPrefs.edit().putBoolean("sleepCompleted", false).apply();
                goalCompleted = false;
                updateProgress();
                Toast.makeText(this, getString(R.string.sleep_goal_set, goalHours), Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(this, getString(R.string.enter_valid_number), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, getString(R.string.enter_sleep_goal), Toast.LENGTH_SHORT).show();
        }
    }

    private void startSleepMode() {
        if (isSleeping) return;
        if (goalHours <= 0) {
            Toast.makeText(this, getString(R.string.set_sleep_goal_first), Toast.LENGTH_SHORT).show();
            return;
        }

        isSleeping = true;
        startTime = System.currentTimeMillis();
        lightThresholdMet = false;
        movementThresholdMet = false;
        lastWakeCheckTime = 0;

        if (lightSensor != null) sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        if (accelSensor != null) sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);

        handler.post(sleepRunnable);
        updateButtonStates();
        Toast.makeText(this, getString(R.string.sleep_mode_started), Toast.LENGTH_LONG).show();
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
        tvSleepTimer.setText(getString(R.string.elapsed_format, 0, 0));

        Toast.makeText(this, getString(R.string.sleep_session_recorded, minutesElapsed), Toast.LENGTH_SHORT).show();

        if (!goalCompleted && (totalSleptMinutes / 60 >= goalHours)) {
            goalCompleted = true;
            goalPrefs.edit().putBoolean("sleepCompleted", true).apply();

            SharedPreferences fuelPrefs = getSharedPreferences("fuelPrefs", MODE_PRIVATE);
            int currentFuel = fuelPrefs.getInt("totalFuel", 0);
            fuelPrefs.edit().putInt("totalFuel", currentFuel + 1).apply();

            Toast.makeText(this, getString(R.string.sleep_goal_completed), Toast.LENGTH_LONG).show();
        }

        lightThresholdMet = false;
        movementThresholdMet = false;
    }

    private void updateProgress() {
        int hoursSlept = totalSleptMinutes / 60;
        int minutesSlept = totalSleptMinutes % 60;
        String progressText = getString(R.string.progress_format, hoursSlept, minutesSlept, goalHours);
        if (hoursSlept >= goalHours) progressText += getString(R.string.goal_achieved_suffix);
        tvSleepProgress.setText(progressText);
    }

    private void updateButtonStates() {
        btnStartSleep.setEnabled(!isSleeping);
        btnStopSleep.setEnabled(isSleeping);
        btnSaveGoal.setEnabled(!isSleeping);
        etSleepGoal.setEnabled(!isSleeping);
    }

    private void loadGoalAndProgress() {
        goalHours = prefs.getInt("goal", 0);
        totalSleptMinutes = prefs.getInt("progress", 0);
        goalCompleted = goalPrefs.getBoolean("sleepCompleted", false);
        if (goalHours > 0) etSleepGoal.setText(String.valueOf(goalHours));
        updateProgress();
    }

    private void resetDailyIfNeeded() {
        String lastDate = prefs.getString("lastDate", "");
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        if (!today.equals(lastDate)) {
            prefs.edit().putInt("progress", 0).putString("lastDate", today).apply();
            goalPrefs.edit().putBoolean("sleepCompleted", false).apply();
            totalSleptMinutes = 0;
            goalCompleted = false;
        }
    }

    public void forceResetFromMain() {
        totalSleptMinutes = 0;
        prefs.edit().putInt("progress", 0).apply();
        goalPrefs.edit().putBoolean("sleepCompleted", false).apply();
        goalCompleted = false;
        updateProgress();
    }

    @Override
    public void onBackPressed() {
        goBack();
    }

    private void goBack() {
        if (isSleeping) {
            Toast.makeText(this, getString(R.string.stop_sleep_first), Toast.LENGTH_SHORT).show();
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
            if (event.values[0] > LIGHT_WAKE_THRESHOLD) {
                lightThresholdMet = true;
                checkWakeUp(currentTime);
            }
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0], y = event.values[1], z = event.values[2];
            double magnitude = Math.sqrt(x * x + y * y + z * z);
            double acceleration = Math.abs(magnitude - SensorManager.GRAVITY_EARTH);
            if (acceleration > MOVEMENT_WAKE_THRESHOLD) {
                movementThresholdMet = true;
                checkWakeUp(currentTime);
            }
        }
    }

    private void checkWakeUp(long currentTime) {
        if (lightThresholdMet && movementThresholdMet &&
                (currentTime - lastWakeCheckTime) > WAKE_CHECK_COOLDOWN) {
            lastWakeCheckTime = currentTime;
            Toast.makeText(this, getString(R.string.wake_detected), Toast.LENGTH_SHORT).show();
            stopSleepMode();
        } else if (lightThresholdMet || movementThresholdMet) {
            handler.removeCallbacks(resetWakeFlags);
            handler.postDelayed(resetWakeFlags, 10000);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
