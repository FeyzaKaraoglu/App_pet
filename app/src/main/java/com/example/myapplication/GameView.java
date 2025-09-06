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

    public interface GameEventListener {
        void onGameOver(int score);
    }

    private GameEventListener gameEventListener;

    public void setGameEventListener(GameEventListener listener) {
        this.gameEventListener = listener;
    }

    private Paint paint;
    private Bitmap rocketBitmap;
    private Bitmap starBitmap;
    private Bitmap boosterBitmap;
    private Bitmap asteroidBitmap;
    private Bitmap backgroundBitmap;

    private float rocketX;
    private float rocketY;
    private float rocketWidth = 140;  // Roket genişliği (daha ince)
    private float rocketHeight = 200; // Roket yüksekliği (daha uzun)

    private float rocketSpeedX = 15;
    private float baseVelocityY = 5;
    private float boostVelocity = 0;

    private List<GameObject> stars = new ArrayList<>();
    private List<GameObject> boosters = new ArrayList<>();
    private List<GameObject> asteroids = new ArrayList<>();
    private Random random = new Random();

    private boolean isInitialized = false;
    private boolean isGameOver = false;

    private int score = 0;
    private int level = 1;

    private float rocketScreenY;

    // Çarpışma mesafesi için tolerans değeri
    private final float COLLISION_TOLERANCE = 20f;

    public GameView(Context context) {
        super(context);
        paint = new Paint();

        // Bitmap yükleme hatalarını kontrol et
        try {
            rocketBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.rocket);
            starBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.star);
            boosterBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.booster);
            asteroidBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.asteroid);
            backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.spacebackground);
        } catch (Exception e) {
            e.printStackTrace();
        }

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!isInitialized && getWidth() > 0 && getHeight() > 0) {
                    isInitialized = true;
                    rocketX = getWidth() / 2f - rocketWidth / 2f;
                    rocketScreenY = getHeight() * 0.7f;
                    rocketY = rocketScreenY;

                    for (int i = 0; i < maxStars(); i++) spawnStar();
                    for (int i = 0; i < maxBoosters(); i++) spawnBooster();
                    for (int i = 0; i < maxAsteroids(); i++) spawnAsteroid();

                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isInitialized || isGameOver) return;

        // Arkaplan çiz
        if (backgroundBitmap != null) {
            canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        } else {
            canvas.drawColor(Color.BLACK);
        }

        // Roket çiz
        rocketY = rocketScreenY;
        if (rocketBitmap != null) {
            try {
                Bitmap scaledRocket = Bitmap.createScaledBitmap(rocketBitmap, (int) rocketWidth, (int) rocketHeight, false);
                canvas.drawBitmap(scaledRocket, rocketX, rocketY, null);
            } catch (Exception e) {
                // Bitmap hatası durumunda basit bir dikdörtgen çiz
                paint.setColor(Color.WHITE);
                canvas.drawRect(rocketX, rocketY, rocketX + rocketWidth, rocketY + rocketHeight, paint);
            }
        }

        // Objeleri güncelle ve çiz
        updateAndDrawObjects(canvas);

        // Skor ve level yazdır
        paint.setColor(Color.WHITE);
        paint.setTextSize(60);
        canvas.drawText("Score: " + score, 50, 100, paint);
        canvas.drawText("Level: " + level, getWidth() - 300, 100, paint);

        if (score >= level * 10) level++;

        // Oyun bitmediyse animasyonu devam ettir
        if (!isGameOver) {
            postInvalidateOnAnimation();
        }
    }

    private void updateAndDrawObjects(Canvas canvas) {
        // --- Stars ---
        List<GameObject> starsToRemove = new ArrayList<>();
        for (GameObject star : stars) {
            star.y += baseVelocityY + boostVelocity;

            if (checkCollisionWithTolerance(star.x, star.y, star.size, star.size, rocketX, rocketY, rocketWidth, rocketHeight, 0)) {
                starsToRemove.add(star);
                score++;
            } else if (star.y > getHeight()) {
                starsToRemove.add(star);
            }

            // Star çiz
            drawGameObject(canvas, starBitmap, star, Color.YELLOW);
        }
        stars.removeAll(starsToRemove);
        while (stars.size() < maxStars()) spawnStar();

        // --- Boosters ---
        List<GameObject> boostersToRemove = new ArrayList<>();
        for (GameObject booster : boosters) {
            booster.y += baseVelocityY + boostVelocity;

            if (checkCollisionWithTolerance(booster.x, booster.y, booster.size, booster.size, rocketX, rocketY, rocketWidth, rocketHeight, 0)) {
                boostersToRemove.add(booster);
                activateSpeedBoost();
            } else if (booster.y > getHeight()) {
                boostersToRemove.add(booster);
            }

            // Booster çiz
            drawGameObject(canvas, boosterBitmap, booster, Color.GREEN);
        }
        boosters.removeAll(boostersToRemove);
        while (boosters.size() < maxBoosters()) spawnBooster();

        // --- Asteroids ---
        List<GameObject> asteroidsToRemove = new ArrayList<>();
        for (GameObject asteroid : asteroids) {
            asteroid.y += baseVelocityY + boostVelocity;

            // Asteroidler için daha sıkı çarpışma kontrolü (tolerans kullan)
            if (checkCollisionWithTolerance(asteroid.x, asteroid.y, asteroid.size, asteroid.size, rocketX, rocketY, rocketWidth, rocketHeight, COLLISION_TOLERANCE)) {
                // Oyun bitişini işaretle
                isGameOver = true;

                // Callback'i post ile güvenli şekilde çağır
                post(() -> {
                    if (gameEventListener != null) {
                        gameEventListener.onGameOver(score);
                    }
                });
                return;
            } else if (asteroid.y > getHeight()) {
                asteroidsToRemove.add(asteroid);
            }

            // Asteroid çiz
            drawGameObject(canvas, asteroidBitmap, asteroid, Color.RED);
        }
        asteroids.removeAll(asteroidsToRemove);
        while (asteroids.size() < maxAsteroids()) spawnAsteroid();
    }

    private void drawGameObject(Canvas canvas, Bitmap bitmap, GameObject gameObject, int fallbackColor) {
        if (bitmap != null) {
            try {
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, (int) gameObject.size, (int) gameObject.size, false);
                canvas.drawBitmap(scaledBitmap, gameObject.x, gameObject.y, null);
            } catch (Exception e) {
                // Bitmap hatası durumunda renkli dikdörtgen çiz
                paint.setColor(fallbackColor);
                canvas.drawRect(gameObject.x, gameObject.y,
                        gameObject.x + gameObject.size, gameObject.y + gameObject.size, paint);
            }
        } else {
            // Bitmap null ise renkli dikdörtgen çiz
            paint.setColor(fallbackColor);
            canvas.drawRect(gameObject.x, gameObject.y,
                    gameObject.x + gameObject.size, gameObject.y + gameObject.size, paint);
        }
    }

    // Eski çarpışma kontrolü (referans için saklı)
    private boolean checkCollision(float x1, float y1, float size1, float x2, float y2, float size2) {
        return x1 + size1 > x2 && x1 < x2 + size2 &&
                y1 + size1 > y2 && y1 < y2 + size2;
    }

    // Yeni toleranslı çarpışma kontrolü - hem kare hem dikdörtgen nesneler için
    private boolean checkCollisionWithTolerance(float x1, float y1, float width1, float height1,
                                                float x2, float y2, float width2, float height2, float tolerance) {
        // Her iki nesnenin de kenarlarına tolerans ekle
        float adjustedX1 = x1 + tolerance;
        float adjustedY1 = y1 + tolerance;
        float adjustedWidth1 = width1 - (tolerance * 2);
        float adjustedHeight1 = height1 - (tolerance * 2);

        float adjustedX2 = x2 + tolerance;
        float adjustedY2 = y2 + tolerance;
        float adjustedWidth2 = width2 - (tolerance * 2);
        float adjustedHeight2 = height2 - (tolerance * 2);

        // Negatif boyut kontrolü
        if (adjustedWidth1 <= 0 || adjustedHeight1 <= 0 || adjustedWidth2 <= 0 || adjustedHeight2 <= 0) {
            return false;
        }

        return adjustedX1 + adjustedWidth1 > adjustedX2 && adjustedX1 < adjustedX2 + adjustedWidth2 &&
                adjustedY1 + adjustedHeight1 > adjustedY2 && adjustedY1 < adjustedY2 + adjustedHeight2;
    }

    private int maxStars() { return Math.min(3 + level, 7); }
    private int maxBoosters() { return Math.min(1 + level / 2, 3); }
    private int maxAsteroids() { return Math.min(1 + level, 5); }

    private void spawnStar() {
        if (getWidth() > 80) {
            stars.add(new GameObject(random.nextInt(getWidth() - 80), -200, 80, 0));
        }
    }

    private void spawnBooster() {
        if (getWidth() > 80) {
            boosters.add(new GameObject(random.nextInt(getWidth() - 80), -300, 80, 0));
        }
    }

    private void spawnAsteroid() {
        if (getWidth() > 120) {
            asteroids.add(new GameObject(random.nextInt(getWidth() - 120), -400, 120, 0));
        }
    }

    public void moveRocket(float direction) {
        if (isGameOver) return; // Oyun bittiyse hareket etme

        rocketX += direction;

        if (rocketX < 0) rocketX = 0;
        if (rocketX > getWidth() - rocketWidth) rocketX = getWidth() - rocketWidth;
    }

    public void activateSpeedBoost() {
        if (isGameOver) return; // Oyun bittiyse boost verme

        boostVelocity = 8;
        postDelayed(() -> boostVelocity = 0, 1000);
    }

    // Oyunu yeniden başlatmak için
    public void resetGame() {
        isGameOver = false;
        score = 0;
        level = 1;
        boostVelocity = 0;

        stars.clear();
        boosters.clear();
        asteroids.clear();

        if (isInitialized) {
            rocketX = getWidth() / 2f - rocketWidth / 2f;
            rocketY = rocketScreenY;

            for (int i = 0; i < maxStars(); i++) spawnStar();
            for (int i = 0; i < maxBoosters(); i++) spawnBooster();
            for (int i = 0; i < maxAsteroids(); i++) spawnAsteroid();
        }
    }

    private static class GameObject {
        float x, y, size, speed;
        GameObject(float x, float y, float size, float speed) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
        }
    }
}