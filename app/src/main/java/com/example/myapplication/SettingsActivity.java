package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    Button btnLang, btnName, btnColor, btnReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        btnLang = findViewById(R.id.btnLang);
        btnName = findViewById(R.id.btnPetName);
        btnColor = findViewById(R.id.btnColor);
        btnReset = findViewById(R.id.btnReset);

        // Strings.xml’den metinler
        btnLang.setText(getResources().getString(R.string.settings_language));
        btnName.setText(getResources().getString(R.string.settings_pet_name));
        btnColor.setText(getResources().getString(R.string.settings_color));
        btnReset.setText(getResources().getString(R.string.settings_reset));

        btnLang.setOnClickListener(v -> startActivity(new Intent(SettingsActivity.this, LanguageActivity.class)));
        btnName.setOnClickListener(v -> {/* Pet name değişimi */});
        btnColor.setOnClickListener(v -> {/* Renk değişimi */});
        btnReset.setOnClickListener(v -> {/* Sıfırlama işlemi */});
    }
}
