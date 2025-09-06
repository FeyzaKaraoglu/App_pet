package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepsGoalActivity extends AppCompatActivity implements SensorEventListener {

    EditText etStepGoal;
    Button btnSetStepGoal, btnBack;
    TextView tvStepProgress;

    int dailyStepGoal = 0;
    int stepsAtDayStart = 0;
    int todaySteps = 0;
    int totalStepsFromSensor = 0;

    SharedPreferences stepsPrefs;
    SharedPreferences goalPrefs;
    boolean goalCompleted = false;

    SensorManager sensorManager;
    Sensor stepCounter;
    Sensor stepDetector;

    String today;

    private static final int PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 1001;
    public static StepsGoalActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_steps_goal);

        instance = this;
        etStepGoal = findViewById(R.id.etStepGoal);
        btnSetStepGoal = findViewById(R.id.btnSetStepGoal);
        btnBack = findViewById(R.id.btnBack);
        tvStepProgress = findViewById(R.id.tvStepProgress);

        stepsPrefs = getSharedPreferences("stepsPrefs", MODE_PRIVATE);
        goalPrefs = getSharedPreferences("goalCompletionPrefs", MODE_PRIVATE);

        today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String savedDate = stepsPrefs.getString("lastDate", "");

        dailyStepGoal = stepsPrefs.getInt("dailyStepGoal", 0);
        if (dailyStepGoal > 0) {
            etStepGoal.setText(String.valueOf(dailyStepGoal));
        }
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        }
        if (!today.equals(savedDate)) {
            resetDailyProgress();
        } else {
            stepsAtDayStart = stepsPrefs.getInt("stepsAtDayStart", 0);
            todaySteps = stepsPrefs.getInt("todaySteps", 0);
            goalCompleted = goalPrefs.getBoolean("stepsCompleted", false);
        }

        btnSetStepGoal.setOnClickListener(v -> {
            String goalText = etStepGoal.getText().toString().trim();
            if (goalText.isEmpty()) {
                Toast.makeText(this, "Please enter a daily step goal!", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                dailyStepGoal = Integer.parseInt(goalText);
                if (dailyStepGoal <= 0) {
                    Toast.makeText(this, "Please enter a positive number!", Toast.LENGTH_SHORT).show();
                    return;
                }
                stepsPrefs.edit().putInt("dailyStepGoal", dailyStepGoal).apply();
                goalPrefs.edit().putBoolean("stepsCompleted", false).apply();
                goalCompleted = false;
                updateStepProgress();
                Toast.makeText(this, "Step goal set: " + dailyStepGoal, Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number!", Toast.LENGTH_SHORT).show();
            }
        });

        btnBack.setOnClickListener(v -> goBackToMain());

        checkActivityRecognitionPermission();
    }

    private void resetDailyProgress() {
        todaySteps = 0;
        stepsAtDayStart = totalStepsFromSensor;
        goalCompleted = false;

        stepsPrefs.edit()
                .putString("lastDate", today)
                .putInt("stepsAtDayStart", stepsAtDayStart)
                .putInt("todaySteps", 0)
                .apply();

        goalPrefs.edit().putBoolean("stepsCompleted", false).apply();
    }
    public void forceResetFromMain() {
        resetDailyProgress();
        updateStepProgress();
    }

    private void checkActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, PERMISSION_REQUEST_ACTIVITY_RECOGNITION);
            } else {
                registerStepSensors();
            }
        } else {
            registerStepSensors();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_ACTIVITY_RECOGNITION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                registerStepSensors();
            } else {
                Toast.makeText(this, "Permission denied, steps won't be counted!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkActivityRecognitionPermission();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterStepSensors();
    }

    private void registerStepSensors() {
        if (sensorManager != null) {
            if (stepCounter != null) {
                sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_UI);
            } else if (stepDetector != null) {
                sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_UI);
            } else {
                Toast.makeText(this, "Step sensors not available!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void unregisterStepSensors() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            totalStepsFromSensor = (int) event.values[0];

            if (stepsAtDayStart == 0) {
                stepsAtDayStart = totalStepsFromSensor;
                stepsPrefs.edit().putInt("stepsAtDayStart", stepsAtDayStart).apply();
            }

            todaySteps = totalStepsFromSensor - stepsAtDayStart;
            if (todaySteps < 0) todaySteps = 0;

            stepsPrefs.edit().putInt("todaySteps", todaySteps).apply();
            updateStepProgress();
            checkGoalCompletion();

        } else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            todaySteps++;
            stepsPrefs.edit().putInt("todaySteps", todaySteps).apply();
            updateStepProgress();
            checkGoalCompletion();
        }
    }

    private void checkGoalCompletion() {
        if (dailyStepGoal > 0 && todaySteps >= dailyStepGoal && !goalCompleted) {
            goalCompleted = true;
            goalPrefs.edit().putBoolean("stepsCompleted", true).apply();

            if (MainActivity.context != null) {
                MainActivity.context.markGoalCompleted("steps");
            }

            Toast.makeText(this, "🎉 Steps goal completed! Your pet gained a life!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void updateStepProgress() {
        String progressText = "Steps: " + todaySteps + " / " + dailyStepGoal;
        if (goalCompleted || (dailyStepGoal > 0 && todaySteps >= dailyStepGoal)) {
            progressText += " ✓ GOAL ACHIEVED!";
        }
        tvStepProgress.setText(progressText);
    }

    private void goBackToMain() {
        Intent intent = new Intent(StepsGoalActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        goBackToMain();
    }
}
