package com.sevtinge.hyperceiler.hook.module.app.SystemUI.Phone.gesture;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.sevtinge.hyperceiler.hook.module.app.SystemUI.Phone.actions.ActionExecutor;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsMap;

public class NavIslandGestureDetector extends GestureDetector.SimpleOnGestureListener {

    private static final int SWIPE_THRESHOLD = 50;
    private static final int SWIPE_VELOCITY_THRESHOLD = 50;

    private ActionExecutor mActionExecutor;
    private PrefsMap<String, Object> mPrefs;

    public NavIslandGestureDetector(Context context, PrefsMap<String, Object> prefs, ActionExecutor actionExecutor) {
        this.mPrefs = prefs;
        this.mActionExecutor = actionExecutor;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        String action = mPrefs.getString("nav_island_gesture_tap", "expand_pill");
        mActionExecutor.executeAction(action);
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        String action = mPrefs.getString("nav_island_gesture_double_tap", "none");
        mActionExecutor.executeAction(action);
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        String action = mPrefs.getString("nav_island_gesture_long_press", "none");
        mActionExecutor.executeAction(action);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1 == null || e2 == null) return false;

        boolean result = false;
        try {
            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        onSwipeRight();
                    } else {
                        onSwipeLeft();
                    }
                    result = true;
                }
            } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY > 0) {
                    onSwipeBottom();
                } else {
                    onSwipeTop();
                }
                result = true;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return result;
    }

    private void onSwipeRight() {
        String action = mPrefs.getString("nav_island_gesture_swipe_right", "media_next");
        mActionExecutor.executeAction(action);
    }

    private void onSwipeLeft() {
        String action = mPrefs.getString("nav_island_gesture_swipe_left", "media_previous");
        mActionExecutor.executeAction(action);
    }

    private void onSwipeTop() {
        String action = mPrefs.getString("nav_island_gesture_swipe_up", "expand_pill");
        mActionExecutor.executeAction(action);
    }

    private void onSwipeBottom() {
        String action = mPrefs.getString("nav_island_gesture_swipe_down", "none");
        mActionExecutor.executeAction(action);
    }
}
