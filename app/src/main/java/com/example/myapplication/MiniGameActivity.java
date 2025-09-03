package com.example.myapplication;

import android.os.Bundle;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;

public class MiniGameActivity extends AppCompatActivity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // GameView’i oluştur ve ekrana ekle
        gameView = new GameView(this);
        setContentView(gameView);
    }

    // Parmağı kaydırarak roketi hareket ettirme
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float touchX = event.getX();
                // Roketi parmağın x konumuna göre hareket ettir
                gameView.moveRocket(touchX - gameView.getWidth() / 2f);
                break;
        }
        return true;
    }
}
