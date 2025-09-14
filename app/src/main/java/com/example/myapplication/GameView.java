package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewTreeObserver;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import androidx.annotation.NonNull;

public class GameView extends View {

    public interface GameEventListener {
        void onGameOver(int score);
    }
    private GameEventListener gameEventListener;

    public void setGameEventListener(GameEventListener listener) {
        this.gameEventListener = listener;
    }

    private final Paint paint;
    private Bitmap rocketBitmap, starBitmap, boosterBitmap, asteroidBitmap, backgroundBitmap;
    private Bitmap scaledRocket, scaledStar, scaledBooster, scaledAsteroid, scaledBackground;
    private float rocketX;
    private float rocketY;
    private final float rocketWidth = 140;
    private final float rocketHeight = 200;
    private float boostVelocity = 0;
    private final List<GameObject> stars = new ArrayList<>();
    private final List<GameObject> boosters = new ArrayList<>();
    private final List<GameObject> asteroids = new ArrayList<>();
    private final Random random = new Random();
    private boolean isInitialized = false;
    private boolean isGameOver = false;
    private int score = 0;
    private int level = 1;
    private float rocketScreenY;

    public GameView(Context context) {
        super(context);
        paint = new Paint();

        try {
            rocketBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.rocket);
            starBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.star);
            boosterBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.booster);
            asteroidBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.asteroid);
            backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.spacebackground);
        } catch (Exception e) {
            Log.e("GameView", "Error loading bitmaps", e);
        }

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!isInitialized && getWidth() > 0 && getHeight() > 0) {
                    isInitialized = true;
                    rocketX = getWidth() / 2f - rocketWidth / 2f;
                    rocketScreenY = getHeight() * 0.7f;
                    rocketY = rocketScreenY;

                    scaledRocket = Bitmap.createScaledBitmap(rocketBitmap, (int) rocketWidth, (int) rocketHeight, false);
                    scaledStar = Bitmap.createScaledBitmap(starBitmap, 80, 80, false);
                    scaledBooster = Bitmap.createScaledBitmap(boosterBitmap, 80, 80, false);
                    scaledAsteroid = Bitmap.createScaledBitmap(asteroidBitmap, 120, 120, false);
                    scaledBackground = Bitmap.createScaledBitmap(backgroundBitmap, getWidth(), getHeight(), false);

                    for (int i = 0; i < maxStars(); i++) spawnStar();
                    for (int i = 0; i < maxBoosters(); i++) spawnBooster();
                    for (int i = 0; i < maxAsteroids(); i++) spawnAsteroid();

                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (!isInitialized || isGameOver) return;

        if (scaledBackground != null) {
            canvas.drawBitmap(scaledBackground, 0, 0, null);
        } else {
            canvas.drawColor(Color.BLACK);
        }

        rocketY = rocketScreenY;
        if (scaledRocket != null) {
            canvas.drawBitmap(scaledRocket, rocketX, rocketY, null);
        } else {
            paint.setColor(Color.WHITE);
            canvas.drawRect(rocketX, rocketY, rocketX + rocketWidth, rocketY + rocketHeight, paint);
        }

        updateAndDrawObjects(canvas);

        paint.setColor(Color.WHITE);
        paint.setTextSize(60);
        canvas.drawText("Score: " + score, 50, 100, paint);
        canvas.drawText("Level: " + level, getWidth() - 300, 100, paint);

        level = score / 10 + 1;

        if (!isGameOver) {
            postInvalidateOnAnimation();
        }
    }
    private void updateAndDrawObjects(Canvas canvas) {
        List<GameObject> starsToRemove = new ArrayList<>();
        float baseVelocityY = 5;
        for (GameObject star : stars) {
            star.y += baseVelocityY + boostVelocity;

            if (checkCollisionWithTolerance(star.x, star.y, star.size, star.size,
                    rocketX, rocketY, rocketWidth, rocketHeight, 0)) {
                starsToRemove.add(star);
                score++;
            } else if (star.y > getHeight()) {
                starsToRemove.add(star);
            }

            drawGameObject(canvas, scaledStar, star, Color.YELLOW);
        }
        stars.removeAll(starsToRemove);
        while (stars.size() < maxStars()) spawnStar();

        List<GameObject> boostersToRemove = new ArrayList<>();
        for (GameObject booster : boosters) {
            booster.y += baseVelocityY + boostVelocity;

            if (checkCollisionWithTolerance(booster.x, booster.y, booster.size, booster.size,
                    rocketX, rocketY, rocketWidth, rocketHeight, 0)) {
                boostersToRemove.add(booster);
                activateSpeedBoost();
            } else if (booster.y > getHeight()) {
                boostersToRemove.add(booster);
            }

            drawGameObject(canvas, scaledBooster, booster, Color.GREEN);
        }
        boosters.removeAll(boostersToRemove);
        while (boosters.size() < maxBoosters()) spawnBooster();

        List<GameObject> asteroidsToRemove = new ArrayList<>();
        for (GameObject asteroid : asteroids) {
            asteroid.y += baseVelocityY + boostVelocity;

            float COLLISION_TOLERANCE = 20f;
            if (checkCollisionWithTolerance(asteroid.x, asteroid.y, asteroid.size, asteroid.size,
                    rocketX, rocketY, rocketWidth, rocketHeight, COLLISION_TOLERANCE)) {
                isGameOver = true;
                post(() -> {
                    if (gameEventListener != null) {
                        gameEventListener.onGameOver(score);
                    }
                });
                return;
            } else if (asteroid.y > getHeight()) {
                asteroidsToRemove.add(asteroid);
            }
            drawGameObject(canvas, scaledAsteroid, asteroid, Color.RED);
        }
        asteroids.removeAll(asteroidsToRemove);
        while (asteroids.size() < maxAsteroids()) spawnAsteroid();
    }

    private void drawGameObject(Canvas canvas, Bitmap bitmap, GameObject gameObject, int fallbackColor) {
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, gameObject.x, gameObject.y, null);
        } else {
            paint.setColor(fallbackColor);
            canvas.drawRect(gameObject.x, gameObject.y,
                    gameObject.x + gameObject.size, gameObject.y + gameObject.size, paint);
        }
    }

    private boolean checkCollisionWithTolerance(float x1, float y1, float width1, float height1,
                                                float x2, float y2, float width2, float height2, float tolerance) {
        float adjustedX1 = x1 + tolerance;
        float adjustedY1 = y1 + tolerance;
        float adjustedWidth1 = width1 - (tolerance * 2);
        float adjustedHeight1 = height1 - (tolerance * 2);
        float adjustedX2 = x2 + tolerance;
        float adjustedY2 = y2 + tolerance;
        float adjustedWidth2 = width2 - (tolerance * 2);
        float adjustedHeight2 = height2 - (tolerance * 2);
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
        if (isGameOver) return;

        rocketX += direction;
        if (rocketX < 0) rocketX = 0;
        if (rocketX > getWidth() - rocketWidth) rocketX = getWidth() - rocketWidth;
    }
    public void activateSpeedBoost() {
        if (isGameOver) return;

        boostVelocity = 8;
        postDelayed(() -> boostVelocity = 0, 1000);
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