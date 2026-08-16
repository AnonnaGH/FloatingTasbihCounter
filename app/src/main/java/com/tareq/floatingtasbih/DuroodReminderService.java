package com.tareq.floatingtasbih;

import android.app.*;
import android.content.*;
import android.media.AudioAttributes;
import android.os.*;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class DuroodReminderService extends Service implements TextToSpeech.OnInitListener {
    private static final String CHANNEL = "durood_reminder_v5";
    private SharedPreferences prefs;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    private final BroadcastReceiver unlockReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_USER_PRESENT.equals(intent.getAction()) && prefs.getBoolean("durood_alert", false)) speakDurood();
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("tasbih_prefs", MODE_PRIVATE);
        createChannel();
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        startForeground(33, b.setContentTitle("Durood Alert active")
                .setContentText("Selected Durood will play when phone is unlocked")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setOngoing(true).build());
        tts = new TextToSpeech(this, this);
        IntentFilter f = new IntentFilter(Intent.ACTION_USER_PRESENT);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(unlockReceiver, f, RECEIVER_NOT_EXPORTED);
        else registerReceiver(unlockReceiver, f);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "Durood Reminder", NotificationManager.IMPORTANCE_LOW);
            c.setSound(null, null);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!prefs.getBoolean("durood_alert", false)) { stopSelf(); return START_NOT_STICKY; }
        return START_STICKY;
    }

    @Override public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(new Locale("ar"));
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
            tts.setSpeechRate(0.9f);
        }
    }

    private void speakDurood() {
        if (!ttsReady || tts == null) return;
        String[] duroods = {"صلى الله عليه وسلم", "اللهم صل وسلم على نبينا محمد"};
        int index = prefs.getInt("durood_index",0);
        if (index < 0 || index >= duroods.length) index = 0;
        float volume = Math.max(0f, Math.min(1f, prefs.getInt("durood_volume",80)/100f));
        Bundle params = new Bundle();
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume);
        if (Build.VERSION.SDK_INT >= 21) tts.speak(duroods[index], TextToSpeech.QUEUE_FLUSH, params, "unlock_durood");
    }

    @Override public void onDestroy() {
        try { unregisterReceiver(unlockReceiver); } catch (Exception ignored) {}
        if (tts != null) { tts.stop(); tts.shutdown(); }
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
