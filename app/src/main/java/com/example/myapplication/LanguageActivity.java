package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class LanguageActivity extends AppCompatActivity {

    Button btnEnglish, btnTurkish, btnGerman;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);

        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        btnEnglish = findViewById(R.id.btnEnglish);
        btnTurkish = findViewById(R.id.btnTurkish);
        btnGerman = findViewById(R.id.btnGerman);

        btnEnglish.setOnClickListener(v -> setLocale("en"));
        btnTurkish.setOnClickListener(v -> setLocale("tr"));
        btnGerman.setOnClickListener(v -> setLocale("de"));
    }

    private void setLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);

        android.content.res.Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        prefs.edit().putString("app_language", langCode).apply();

        // Uygulamayı yeniden başlat
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
