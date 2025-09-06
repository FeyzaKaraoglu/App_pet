package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ayarlar butonu
        btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> showAyarlarDialog());

        // Bottom bar içindeki Mini Game butonu
        LinearLayout bottomBar = findViewById(R.id.bottomBar);
        Button btnGame = bottomBar.findViewById(R.id.btnGame);

        btnGame.setOnClickListener(v -> {
            // MiniGameActivity'yi aç
            Intent intent = new Intent(MainActivity.this, MiniGameActivity.class);
            startActivity(intent);
        });
    }

    private void showAyarlarDialog() {
        String[] options = {"Languages", "pet name", "color", "reset"};

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
}
