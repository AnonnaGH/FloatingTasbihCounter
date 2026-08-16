package com.tareq.floatingtasbih;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {
    private SharedPreferences prefs;
    private TextView mainCount, targetText, opacityValue, duroodVolumeValue;
    private EditText customTarget;
    private Spinner zikrSpinner, duroodSpinner;
    private Switch duroodSwitch;

    private final String[] zikrs = {
            "সুবহানাল্লাহ — سُبْحَانَ الله",
            "আলহামদুলিল্লাহ — الْحَمْدُ لِلَّه",
            "আল্লাহু আকবার — اللهُ أَكْبَر",
            "লা ইলাহা ইল্লাল্লাহ — لَا إِلٰهَ إِلَّا الله",
            "আস্তাগফিরুল্লাহ — أَسْتَغْفِرُ الله",
            "লা হাওলা ওয়ালা কুওয়াতা ইল্লা বিল্লাহ — لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّه"
    };

    private final String[] duroods = {
            "صلى الله عليه وسلم",
            "اللهم صل وسلم على نبينا محمد"
    };

    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener = (sp, key) -> {
        if ("count".equals(key) || "target".equals(key)) runOnUiThread(this::refresh);
    };

    private final BroadcastReceiver countReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { refresh(); }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("tasbih_prefs", MODE_PRIVATE);

        mainCount = findViewById(R.id.mainCount);
        targetText = findViewById(R.id.targetText);
        customTarget = findViewById(R.id.customTarget);
        opacityValue = findViewById(R.id.opacityValue);
        duroodVolumeValue = findViewById(R.id.duroodVolumeValue);
        zikrSpinner = findViewById(R.id.zikrSpinner);
        duroodSpinner = findViewById(R.id.duroodSpinner);
        duroodSwitch = findViewById(R.id.duroodSwitch);

        ArrayAdapter<String> zikrAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, zikrs);
        zikrAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        zikrSpinner.setAdapter(zikrAdapter);
        zikrSpinner.setSelection(prefs.getInt("zikr_index", 4));
        zikrSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                prefs.edit().putInt("zikr_index", pos).putString("zikr_name", zikrs[pos]).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        ArrayAdapter<String> duroodAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, duroods);
        duroodAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        duroodSpinner.setAdapter(duroodAdapter);
        duroodSpinner.setSelection(prefs.getInt("durood_index", 0));
        duroodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                prefs.edit().putInt("durood_index", pos).putString("durood_text", duroods[pos]).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        setupTarget();
        bindSwitch(R.id.vibrationSwitch, "vibration", true);
        bindSwitch(R.id.soundSwitch, "sound", true);
        bindSwitch(R.id.targetSoundSwitch, "target_sound", true);

        SeekBar opacity = findViewById(R.id.opacitySeek);
        opacity.setProgress(prefs.getInt("opacity", 85));
        opacityValue.setText(prefs.getInt("opacity",85) + "%");
        opacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int value = Math.max(25, p);
                prefs.edit().putInt("opacity", value).apply();
                opacityValue.setText(value + "%");
                sendUpdate();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        duroodSwitch.setChecked(prefs.getBoolean("durood_alert", false));
        duroodSwitch.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean("durood_alert", checked).apply();
            if (checked) startDuroodService(); else stopService(new Intent(this, DuroodReminderService.class));
        });

        SeekBar duroodVolume = findViewById(R.id.duroodVolumeSeek);
        duroodVolume.setProgress(prefs.getInt("durood_volume", 80));
        duroodVolumeValue.setText(prefs.getInt("durood_volume",80) + "%");
        duroodVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                prefs.edit().putInt("durood_volume", p).apply();
                duroodVolumeValue.setText(p + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        mainCount.setOnClickListener(v -> changeCount());
        findViewById(R.id.countButton).setOnClickListener(v -> changeCount());
        findViewById(R.id.plusButton).setOnClickListener(v -> changeCount());
        findViewById(R.id.minusButton).setOnClickListener(v -> decreaseCount());
        findViewById(R.id.startButton).setOnClickListener(v -> startCounter());
        findViewById(R.id.stopButton).setOnClickListener(v -> {
            prefs.edit().putBoolean("floating_enabled", false).apply();
            stopService(new Intent(this, FloatingTasbihService.class));
        });
        findViewById(R.id.resetButton).setOnClickListener(v -> {
            prefs.edit().putInt("count", 0).apply();
            refresh();
            sendUpdate();
        });

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 20);
        }
        refresh();
    }

    private void setupTarget() {
        int t = prefs.getInt("target", 33);
        if (t == 33) ((RadioButton)findViewById(R.id.target33)).setChecked(true);
        else if (t == 100) ((RadioButton)findViewById(R.id.target100)).setChecked(true);

        else {
            ((RadioButton)findViewById(R.id.targetCustom)).setChecked(true);
            customTarget.setVisibility(View.VISIBLE);
            customTarget.setText(String.valueOf(t));
        }
        ((RadioGroup)findViewById(R.id.targetGroup)).setOnCheckedChangeListener((g,id) -> {
            customTarget.setVisibility(id == R.id.targetCustom ? View.VISIBLE : View.GONE);
            if (id == R.id.target33) saveTarget(33);
            else if (id == R.id.target100) saveTarget(100);

        });
        customTarget.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) saveCustomTarget();
        });
        customTarget.setOnEditorActionListener((v, actionId, event) -> { saveCustomTarget(); return false; });
    }

    private void saveCustomTarget() {
        try {
            int x = Integer.parseInt(customTarget.getText().toString().trim());
            if (x > 0) saveTarget(x);
        } catch (Exception ignored) {}
    }

    private void bindSwitch(int id, String key, boolean def) {
        Switch s = findViewById(id);
        s.setChecked(prefs.getBoolean(key, def));
        s.setOnCheckedChangeListener((button, checked) -> prefs.edit().putBoolean(key, checked).apply());
    }

    private void changeCount() {
        int c = prefs.getInt("count",0) + 1;
        prefs.edit().putInt("count", c).apply();
        feedback(c);
        refresh();
        sendUpdate();
        broadcastCount();
    }

    private void decreaseCount() {
        int c = Math.max(0, prefs.getInt("count",0) - 1);
        prefs.edit().putInt("count", c).apply();
        refresh();
        sendUpdate();
        broadcastCount();
    }

    private void feedback(int c) {
        if (prefs.getBoolean("vibration", true)) vibrate(35);
        if (prefs.getBoolean("sound", true)) tone(45);
        if (c == prefs.getInt("target", 33) && prefs.getBoolean("target_sound", true)) tone(260);
    }

    private void vibrate(long ms) {
        try {
            Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
                else vibrator.vibrate(ms);
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

    private void sendUpdate() {
        Intent i = new Intent(this, FloatingTasbihService.class);
        i.setAction(FloatingTasbihService.ACTION_UPDATE);
        try { startService(i); } catch (Exception ignored) {}
    }

    private void broadcastCount() {
        sendBroadcast(new Intent(FloatingTasbihService.ACTION_COUNT_CHANGED).setPackage(getPackageName()));
    }

    private void saveTarget(int t) {
        prefs.edit().putInt("target", t).apply();
        targetText.setText("Target: " + t);
        sendUpdate();
    }

    private void refresh() {
        mainCount.setText(String.valueOf(prefs.getInt("count",0)));
        targetText.setText("Target: " + prefs.getInt("target",33));
    }

    private void startCounter() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            Toast.makeText(this, "Display over other apps permission Allow করুন", Toast.LENGTH_LONG).show();
            return;
        }
        prefs.edit().putBoolean("floating_enabled", true).apply();
        Intent i = new Intent(this, FloatingTasbihService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
    }

    private void startDuroodService() {
        Intent i = new Intent(this, DuroodReminderService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
    }

    @Override protected void onStart() {
        super.onStart();
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
        IntentFilter f = new IntentFilter(FloatingTasbihService.ACTION_COUNT_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(countReceiver, f, RECEIVER_NOT_EXPORTED);
        else registerReceiver(countReceiver, f);
        if (prefs.getBoolean("durood_alert", false)) startDuroodService();
    }

    @Override protected void onStop() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
        try { unregisterReceiver(countReceiver); } catch (Exception ignored) {}
        super.onStop();
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
        if (prefs.getBoolean("floating_enabled", false) && Settings.canDrawOverlays(this)) {
            Intent i = new Intent(this, FloatingTasbihService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        }
    }
}
