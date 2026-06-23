package com.sevtinge.hyperceiler.hook.module.app.SystemUI.Phone.actions;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.view.KeyEvent;

import com.sevtinge.hyperceiler.hook.module.app.SystemUI.Phone.NavIslandView;

public class ActionExecutor {
    
    private Context mContext;
    private NavIslandView mNavIslandView;
    private AudioManager mAudioManager;

    public ActionExecutor(Context context, NavIslandView navIslandView) {
        this.mContext = context;
        this.mNavIslandView = navIslandView;
        this.mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public void executeAction(String actionCode) {
        if (actionCode == null || actionCode.equals("none")) {
            return;
        }

        // Support for gesture macros (comma-separated actions)
        if (actionCode.contains(",")) {
            String[] actions = actionCode.split(",");
            for (String action : actions) {
                executeSingleAction(action.trim());
            }
        } else {
            executeSingleAction(actionCode.trim());
        }
    }

    private void executeSingleAction(String actionCode) {
        // If-Then Automation Example: "if_media_playing?media_pause:media_play"
        if (actionCode.contains("?")) {
            String[] parts = actionCode.split("\\?", 2);
            String condition = parts[0];
            String[] branches = parts[1].split(":", 2);
            
            boolean conditionMet = evaluateCondition(condition);
            if (conditionMet) {
                executeSingleAction(branches[0]);
            } else if (branches.length > 1) {
                executeSingleAction(branches[1]);
            }
            return;
        }

        if (actionCode.startsWith("launch_app:")) {
            String packageName = actionCode.substring("launch_app:".length());
            launchApp(packageName);
            return;
        }

        switch (actionCode) {
            case "expand_pill":
                if (mNavIslandView != null) {
                    mNavIslandView.toggleExpansion();
                }
                break;
            case "media_play_pause":
                sendMediaButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                break;
            case "media_next":
                sendMediaButton(KeyEvent.KEYCODE_MEDIA_NEXT);
                break;
            case "media_previous":
                sendMediaButton(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                break;
            case "volume_up":
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                break;
            case "volume_down":
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                break;
            case "toggle_wifi":
                toggleWifi();
                break;
            case "toggle_bluetooth":
                toggleBluetooth();
                break;
            default:
                // Do nothing
                break;
        }
    }

    private boolean evaluateCondition(String condition) {
        if ("if_media_playing".equals(condition)) {
            return mAudioManager != null && mAudioManager.isMusicActive();
        }
        if ("if_pill_expanded".equals(condition)) {
            return mNavIslandView != null && mNavIslandView.isExpanded();
        }
        return false;
    }

    private void launchApp(String packageName) {
        try {
            Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleWifi() {
        try {
            android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                wifiManager.setWifiEnabled(!wifiManager.isWifiEnabled());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleBluetooth() {
        try {
            android.bluetooth.BluetoothAdapter bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null) {
                if (bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.disable();
                } else {
                    bluetoothAdapter.enable();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMediaButton(int keyCode) {
        if (mAudioManager == null) return;
        long eventTime = android.os.SystemClock.uptimeMillis();
        KeyEvent downEvent = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0);
        KeyEvent upEvent = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0);
        mAudioManager.dispatchMediaKeyEvent(downEvent);
        mAudioManager.dispatchMediaKeyEvent(upEvent);
    }
}
