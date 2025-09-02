package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSettings = findViewById(R.id.btnSettings);

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAyarlarDialog();
            }
        });
    }

    private void showAyarlarDialog() {
        // Ayarlar menüsü seçenekleri
        String[] options = {"Languages", "pet name", "color", "reset"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    // Dil değiştir
                    // Buraya dil değiştirme kodunu yaz
                    break;
                case 1:
                    // Pet ismi değiştir
                    // Buraya pet ismi değiştirme kodunu yaz
                    break;
                case 2:
                    // Renk değiştir
                    // Buraya renk değiştirme kodunu yaz
                    break;
                case 3:
                    // Sıfırla
                    // Buraya sıfırlama kodunu yaz
                    break;
            }
        });

        builder.show();
    }
}
