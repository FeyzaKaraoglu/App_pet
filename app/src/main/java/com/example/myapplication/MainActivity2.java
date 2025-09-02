package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity2 extends AppCompatActivity {

    Button btnLang, btnName, btnColor, btnReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        btnLang = findViewById(R.id.btnLang);
        btnName = findViewById(R.id.btnPetName);
        btnColor = findViewById(R.id.btnColor);
        btnReset = findViewById(R.id.btnReset);

        btnLang.setOnClickListener(v -> {
            // Dil değiştirme kodu
        });

        btnName.setOnClickListener(v -> {
            // Pet ismi değiştirme kodu
        });

        btnColor.setOnClickListener(v -> {
            // Renk değiştirme kodu
        });

        btnReset.setOnClickListener(v -> {
            // Sıfırlama kodu
        });
    }
}
