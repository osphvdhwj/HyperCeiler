package com.sevtinge.hyperceiler.hook.module.app.SystemUI.Phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsMap;

import java.util.ArrayList;
import java.util.List;

public class NavIslandContentContainer extends LinearLayout {

    private PrefsMap<String, Object> mPrefs;
    private AudioManager mAudioManager;
    
    private List<NavIslandSlider> mVolumeSliders = new ArrayList<>();
    private List<Integer> mVolumeStreamTypes = new ArrayList<>();
    private List<TextView> mVolumeValueTexts = new ArrayList<>();
    
    private NavIslandSlider mBrightnessSlider;
    private TextView mBrightnessValueText;
    
    private ContentObserver mBrightnessObserver;
    private BroadcastReceiver mVolumeReceiver;

    public NavIslandContentContainer(Context context, PrefsMap<String, Object> prefs) {
        super(context);
        this.mPrefs = prefs;
        setOrientation(LinearLayout.VERTICAL);
        setPadding(40, 40, 40, 40);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        initViews();
        registerObservers();
    }

    private void registerObservers() {
        // Brightness Observer
        mBrightnessObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                if (mBrightnessSlider != null) {
                    try {
                        int brightness = Settings.System.getInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
                        mBrightnessSlider.setProgress(brightness);
                        mBrightnessValueText.setText(Math.round((brightness / 255f) * 100) + "%");
                    } catch (Settings.SettingNotFoundException ignored) {}
                }
            }
        };
        getContext().getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                false, mBrightnessObserver);
                
        // Volume Receiver
        mVolumeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("android.media.VOLUME_CHANGED_ACTION".equals(intent.getAction())) {
                    int streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
                    for (int i = 0; i < mVolumeStreamTypes.size(); i++) {
                        if (mVolumeStreamTypes.get(i) == streamType) {
                            int current = mAudioManager.getStreamVolume(streamType);
                            int max = mAudioManager.getStreamMaxVolume(streamType);
                            mVolumeSliders.get(i).setProgress(current);
                            mVolumeValueTexts.get(i).setText(Math.round(((float)current / max) * 100) + "%");
                        }
                    }
                }
            }
        };
        getContext().registerReceiver(mVolumeReceiver, new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        try {
            getContext().getContentResolver().unregisterContentObserver(mBrightnessObserver);
            getContext().unregisterReceiver(mVolumeReceiver);
        } catch (Exception ignored) {}
    }

    private void initViews() {
        boolean showBrightness = mPrefs.getBoolean("nav_island_bar_brightness_enable", true);
        boolean showVolume = mPrefs.getBoolean("nav_island_bar_volume_enable", true);

        if (showBrightness) {
            addBrightnessSection();
        }

        if (showVolume) {
            addVolumeSection();
        }
    }

    private void addBrightnessSection() {
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        
        TextView label = new TextView(getContext());
        label.setText("Brightness");
        label.setTextColor(Color.WHITE);
        label.setTextSize(14f);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(label);

        mBrightnessValueText = new TextView(getContext());
        mBrightnessValueText.setTextColor(Color.WHITE);
        mBrightnessValueText.setTextSize(14f);
        header.addView(mBrightnessValueText);

        addView(header);

        mBrightnessSlider = new NavIslandSlider(getContext());
        mBrightnessSlider.setMax(255);
        try {
            int currentBrightness = Settings.System.getInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            mBrightnessSlider.setProgress(currentBrightness);
            mBrightnessValueText.setText(Math.round((currentBrightness / 255f) * 100) + "%");
        } catch (Settings.SettingNotFoundException e) {
            mBrightnessSlider.setProgress(128);
        }

        mBrightnessSlider.setOnProgressListener((progress, fromUser) -> {
            mBrightnessValueText.setText(Math.round((progress / 255f) * 100) + "%");
            if (fromUser) {
                try {
                    Settings.System.putInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, progress);
                } catch (Exception ignored) { }
            }
        });

        float density = getResources().getDisplayMetrics().density;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(48 * density));
        lp.setMargins(0, (int)(12 * density), 0, (int)(32 * density));
        addView(mBrightnessSlider, lp);
    }

    private void addVolumeSection() {
        addVolumeSlider("Media", AudioManager.STREAM_MUSIC, "#378ADD");
        addVolumeSlider("Ring", AudioManager.STREAM_RING, "#B89500");
        addVolumeSlider("Alarm", AudioManager.STREAM_ALARM, "#9D2C1C");
        addSpatialAudioButton();
    }

    private void addVolumeSlider(String name, int streamType, String colorHex) {
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView label = new TextView(getContext());
        label.setText(name);
        label.setTextColor(Color.WHITE);
        label.setTextSize(14f);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(label);

        TextView valueTxt = new TextView(getContext());
        valueTxt.setTextColor(Color.WHITE);
        valueTxt.setTextSize(14f);
        header.addView(valueTxt);

        addView(header);

        NavIslandSlider slider = new NavIslandSlider(getContext());
        slider.setProgressColor(Color.parseColor(colorHex));
        if (mAudioManager != null) {
            int max = mAudioManager.getStreamMaxVolume(streamType);
            int current = mAudioManager.getStreamVolume(streamType);
            slider.setMax(max);
            slider.setProgress(current);
            valueTxt.setText(Math.round(((float)current / max) * 100) + "%");
        }

        mVolumeSliders.add(slider);
        mVolumeStreamTypes.add(streamType);
        mVolumeValueTexts.add(valueTxt);

        slider.setOnProgressListener((progress, fromUser) -> {
            if (mAudioManager != null) {
                int max = mAudioManager.getStreamMaxVolume(streamType);
                valueTxt.setText(Math.round(((float)progress / max) * 100) + "%");
                if (fromUser) {
                    mAudioManager.setStreamVolume(streamType, progress, 0);
                }
            }
        });

        float density = getResources().getDisplayMetrics().density;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(32 * density));
        lp.setMargins(0, (int)(8 * density), 0, (int)(16 * density));
        addView(slider, lp);
    }
    
    private void addSpatialAudioButton() {
        TextView spatialBtn = new TextView(getContext());
        spatialBtn.setText("🔇 Spatial Audio");
        spatialBtn.setTextColor(Color.WHITE);
        spatialBtn.setTextSize(14f);
        spatialBtn.setGravity(Gravity.CENTER);
        spatialBtn.setBackgroundColor(Color.parseColor("#40FFFFFF"));
        
        float density = getResources().getDisplayMetrics().density;
        spatialBtn.setPadding(0, (int)(12 * density), 0, (int)(12 * density));
        
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.parseColor("#40FFFFFF"));
        bg.setCornerRadius(24f * density);
        spatialBtn.setBackground(bg);
        
        spatialBtn.setOnClickListener(v -> {
            // Toggle spatial audio logic here, or just visual for now
            if (spatialBtn.getText().toString().contains("🔇")) {
                spatialBtn.setText("🔊 Spatial Audio");
                bg.setColor(Color.parseColor("#378ADD"));
            } else {
                spatialBtn.setText("🔇 Spatial Audio");
                bg.setColor(Color.parseColor("#40FFFFFF"));
            }
        });
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, (int)(16 * density), 0, (int)(16 * density));
        addView(spatialBtn, lp);
    }
}
