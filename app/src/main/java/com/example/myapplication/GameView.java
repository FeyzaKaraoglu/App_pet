package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends View {

    private Paint paint;
    private Bitmap rocketBitmap;
    private Bitmap starBitmap;
    private Bitmap boosterBitmap;

    private float rocketX;
    private float rocketY;
    private float rocketSize = 150;

    private float rocketSpeedX = 15; // yatay sabit hız
    private float baseVelocityY = 5; // objelerin düşüş hızı
    private float boostVelocity = 0;
    private long boostEndTime = 0;

    private List<GameObject> stars = new ArrayList<>();
    private List<GameObject> boosters = new ArrayList<>();
    private Random random = new Random();

    private boolean isInitialized = false;

    private int score = 0;
    private int level = 1;

    private float rocketScreenY; // roket ekranın sabit Y'si

    public GameView(Context context) {
        super(context);
        paint = new Paint();

        rocketBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.rocket);
        starBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.star);
        boosterBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.booster);

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!isInitialized && getWidth() > 0 && getHeight() > 0) {
                    isInitialized = true;
                    rocketX = getWidth() / 2f - rocketSize / 2f;
                    rocketScreenY = getHeight() * 0.7f; // ekranın altına yakın sabit pozisyon
                    rocketY = rocketScreenY;

                    for (int i = 0; i < maxStars(); i++) spawnStar();
                    for (int i = 0; i < maxBoosters(); i++) spawnBooster();

                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isInitialized) return;

        canvas.drawColor(Color.BLACK);

        // Roket sabit Y pozisyonunda
        rocketY = rocketScreenY;
        if (rocketBitmap != null) {
            canvas.drawBitmap(
                    Bitmap.createScaledBitmap(rocketBitmap, (int) rocketSize, (int) rocketSize, false),
                    rocketX,
                    rocketY,
                    null
            );
        }

        // Objeleri çiz ve güncelle
        updateAndDrawObjects(canvas);

        // Skor ve level
        paint.setColor(Color.WHITE);
        paint.setTextSize(60);
        canvas.drawText("Score: " + score, 50, 100, paint);
        canvas.drawText("Level: " + level, getWidth() - 300, 100, paint);

        if (score >= level * 10) level++;

        postInvalidateOnAnimation();
    }

    private void updateAndDrawObjects(Canvas canvas) {
        List<GameObject> starsToRemove = new ArrayList<>();
        for (GameObject star : stars) {
            // objeler düşüyor
            star.y += baseVelocityY + boostVelocity;

            if (checkCollision(star.x, star.y, star.size, rocketX, rocketY, rocketSize)) {
                starsToRemove.add(star);
                score++;
            } else if (star.y > getHeight()) starsToRemove.add(star);

            if (starBitmap != null) {
                canvas.drawBitmap(
                        Bitmap.createScaledBitmap(starBitmap, (int) star.size, (int) star.size, false),
                        star.x,
                        star.y,
                        null
                );
            }
        }
        stars.removeAll(starsToRemove);
        while (stars.size() < maxStars()) spawnStar();

        List<GameObject> boostersToRemove = new ArrayList<>();
        for (GameObject booster : boosters) {
            booster.y += baseVelocityY + boostVelocity;

            if (checkCollision(booster.x, booster.y, booster.size, rocketX, rocketY, rocketSize)) {
                boostersToRemove.add(booster);
                activateSpeedBoost();
            } else if (booster.y > getHeight()) boostersToRemove.add(booster);

            if (boosterBitmap != null) {
                canvas.drawBitmap(
                        Bitmap.createScaledBitmap(boosterBitmap, (int) booster.size, (int) booster.size, false),
                        booster.x,
                        booster.y,
                        null
                );
            }
        }
        boosters.removeAll(boostersToRemove);
        while (boosters.size() < maxBoosters()) spawnBooster();
    }

    private boolean checkCollision(float x1, float y1, float size1, float x2, float y2, float size2) {
        return x1 + size1 > x2 && x1 < x2 + size2 &&
                y1 + size1 > y2 && y1 < y2 + size2;
    }

    private int maxStars() { return Math.min(3 + level, 7); }
    private int maxBoosters() { return Math.min(1 + level / 2, 3); }

    private void spawnStar() {
        stars.add(new GameObject(random.nextInt(getWidth() - 80), -200, 80, 0));
    }

    private void spawnBooster() {
        boosters.add(new GameObject(random.nextInt(getWidth() - 80), -300, 80, 0));
    }

    // Sabit yatay hareket kullanıcı kontrolü ile
    public void moveRocket(float direction) {
        if (direction > 0) rocketX += rocketSpeedX;
        else if (direction < 0) rocketX -= rocketSpeedX;

        if (rocketX < 0) rocketX = 0;
        if (rocketX > getWidth() - rocketSize) rocketX = getWidth() - rocketSize;
    }

    public void activateSpeedBoost() {
        boostVelocity = 8; // objeler daha hızlı düşer
        boostEndTime = System.currentTimeMillis() + 1000; // 1 saniye
    }

    private static class GameObject {
        float x, y, size, speed;
        GameObject(float x, float y, float size, float speed) {
            this.x = x; this.y = y; this.size = size; this.speed = speed;
        }
    }
}
