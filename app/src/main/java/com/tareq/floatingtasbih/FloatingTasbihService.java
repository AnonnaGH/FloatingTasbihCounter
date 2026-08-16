package com.tareq.floatingtasbih;

import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.TextView;

public class FloatingTasbihService extends Service {
    public static final String ACTION_UPDATE = "com.tareq.floatingtasbih.UPDATE";
    public static final String ACTION_COUNT_CHANGED = "com.tareq.floatingtasbih.COUNT_CHANGED";
    private static final String CHANNEL = "tasbih_v5";

    private WindowManager wm;
    private View bubbleView, removeView;
    private WindowManager.LayoutParams bubbleParams, removeParams;
    private SharedPreferences prefs;
    private TextView countView;
    private boolean removeHighlighted = false;

    @Override public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("tasbih_prefs", MODE_PRIVATE);
        createChannel();
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        startForeground(22, b.setContentTitle("Floating Tasbih চলছে")
                .setContentText("Tap to count • Drag to move")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true).build());
        if (Settings.canDrawOverlays(this)) showBubble(); else stopSelf();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "Floating Tasbih", NotificationManager.IMPORTANCE_LOW);
            c.setSound(null, null);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    @Override public int onStartCommand(Intent i, int flags, int startId) {
        if (i != null && ACTION_UPDATE.equals(i.getAction())) update();
        if (!prefs.getBoolean("floating_enabled", true)) { stopSelf(); return START_NOT_STICKY; }
        return START_STICKY;
    }

    private void showBubble() {
        if (bubbleView != null) return;
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        bubbleView = LayoutInflater.from(this).inflate(R.layout.overlay_tasbih, null);
        bubbleParams = new WindowManager.LayoutParams(dp(76), dp(76), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = prefs.getInt("overlay_x", 40);
        bubbleParams.y = prefs.getInt("overlay_y", 300);
        clampPosition();
        bubbleView.setAlpha(prefs.getInt("opacity",85) / 100f);
        countView = bubbleView.findViewById(R.id.countView);
        update();

        bubbleView.setOnTouchListener(new View.OnTouchListener() {
            int initialX, initialY;
            float startRawX, startRawY;
            boolean dragging;
            final int slop = dp(8);

            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = bubbleParams.x;
                        initialY = bubbleParams.y;
                        startRawX = e.getRawX();
                        startRawY = e.getRawY();
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - startRawX;
                        float dy = e.getRawY() - startRawY;
                        if (!dragging && (Math.abs(dx) > slop || Math.abs(dy) > slop)) {
                            dragging = true;
                            showRemoveTarget();
                        }
                        if (dragging) {
                            bubbleParams.x = initialX + (int)dx;
                            bubbleParams.y = initialY + (int)dy;
                            clampPosition();
                            try { wm.updateViewLayout(bubbleView, bubbleParams); } catch (Exception ignored) {}
                            setRemoveHighlighted(isOverRemoveTarget());
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (dragging) {
                            boolean remove = isOverRemoveTarget();
                            hideRemoveTarget();
                            if (remove) {
                                prefs.edit().putBoolean("floating_enabled", false).apply();
                                stopSelf();
                            } else {
                                prefs.edit().putInt("overlay_x", bubbleParams.x).putInt("overlay_y", bubbleParams.y).apply();
                            }
                        } else if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                            changeCount();
                        }
                        return true;
                }
                return true;
            }
        });
        wm.addView(bubbleView, bubbleParams);
    }

    private void showRemoveTarget() {
        if (removeView != null) return;
        removeView = LayoutInflater.from(this).inflate(R.layout.overlay_remove, null);
        removeParams = new WindowManager.LayoutParams(dp(150), dp(86), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        removeParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        removeParams.y = dp(24);
        try { wm.addView(removeView, removeParams); } catch (Exception ignored) { removeView = null; }
    }

    private void hideRemoveTarget() {
        if (wm != null && removeView != null) {
            try { wm.removeView(removeView); } catch (Exception ignored) {}
        }
        removeView = null;
        removeHighlighted = false;
    }

    private void setRemoveHighlighted(boolean highlighted) {
        if (removeView == null || removeHighlighted == highlighted) return;
        removeHighlighted = highlighted;
        removeView.setScaleX(highlighted ? 1.12f : 1f);
        removeView.setScaleY(highlighted ? 1.12f : 1f);
        removeView.setAlpha(highlighted ? 1f : .88f);
        if (highlighted) vibrate(25);
    }

    private boolean isOverRemoveTarget() {
        if (removeView == null || bubbleView == null) return false;
        int[] bLoc = new int[2];
        int[] rLoc = new int[2];
        bubbleView.getLocationOnScreen(bLoc);
        removeView.getLocationOnScreen(rLoc);
        float bx = bLoc[0] + bubbleView.getWidth()/2f;
        float by = bLoc[1] + bubbleView.getHeight()/2f;
        float rx = rLoc[0] + removeView.getWidth()/2f;
        float ry = rLoc[1] + removeView.getHeight()/2f;
        float dx = bx-rx, dy = by-ry;
        return Math.sqrt(dx*dx + dy*dy) < dp(105);
    }

    private void clampPosition() {
        Rect safe = getSafeRect();
        int bw = dp(76), bh = dp(76);
        bubbleParams.x = Math.max(safe.left, Math.min(bubbleParams.x, safe.right - bw));
        bubbleParams.y = Math.max(safe.top, Math.min(bubbleParams.y, safe.bottom - bh));
    }

    private Rect getSafeRect() {
        int width, height, topInset = dp(24), bottomInset = dp(56);
        if (Build.VERSION.SDK_INT >= 30) {
            WindowMetrics metrics = wm.getCurrentWindowMetrics();
            Rect b = metrics.getBounds();
            width = b.width(); height = b.height();
            android.graphics.Insets insets = metrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
            topInset = Math.max(topInset, insets.top);
            bottomInset = Math.max(bottomInset, insets.bottom);
        } else {
            android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
            wm.getDefaultDisplay().getRealMetrics(dm);
            width = dm.widthPixels; height = dm.heightPixels;
        }
        return new Rect(dp(4), topInset + dp(4), width - dp(4), height - bottomInset - dp(4));
    }

    private void changeCount() {
        int c = prefs.getInt("count",0) + 1;
        prefs.edit().putInt("count", c).apply();
        update();
        if (prefs.getBoolean("vibration", true)) vibrate(35);
        if (prefs.getBoolean("sound", true)) tone(45);
        if (c == prefs.getInt("target",33) && prefs.getBoolean("target_sound", true)) tone(260);
        sendBroadcast(new Intent(ACTION_COUNT_CHANGED).setPackage(getPackageName()));
    }

    private void update() {
        if (countView != null) countView.setText(String.valueOf(prefs.getInt("count",0)));
        if (bubbleView != null) bubbleView.setAlpha(prefs.getInt("opacity",85)/100f);
    }

    private void vibrate(long ms) {
        try {
            Vibrator v = (Vibrator)getSystemService(VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
                else v.vibrate(ms);
            }
        } catch (Exception ignored) {}
    }

    private void tone(int ms) {
        try {
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 55);
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, ms);
            new Handler(Looper.getMainLooper()).postDelayed(tg::release, ms + 100L);
        } catch (Exception ignored) {}
    }

    private int dp(int x) { return Math.round(x * getResources().getDisplayMetrics().density); }

    @Override public void onDestroy() {
        hideRemoveTarget();
        if (wm != null && bubbleView != null) try { wm.removeView(bubbleView); } catch (Exception ignored) {}
        bubbleView = null;
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
