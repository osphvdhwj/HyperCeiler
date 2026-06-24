package com.sevtinge.hyperceiler.hook.module.app.SystemUI.Phone;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsMap;

import android.animation.ValueAnimator;
import android.graphics.Path;
import android.view.animation.PathInterpolator;
import android.view.MotionEvent;

public class NavIslandView extends FrameLayout {
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private PrefsMap<String, Object> mPrefs;
    
    private FrameLayout mPillContainer;
    private GradientDrawable mPillDrawable;
    private NavIslandContentContainer mContentContainer;
    private android.widget.TextView mNotificationText;
    
    private android.view.GestureDetector mGestureDetector;
    private com.sevtinge.hyperceiler.hook.module.app.SystemUI.Phone.actions.ActionExecutor mActionExecutor;
    
    private boolean mIsExpanded = false;
    private int mCollapsedWidth, mCollapsedHeight, mCollapsedBottomMargin;
    private int mExpandedWidth, mExpandedHeight, mExpandedBottomMargin;
    private float mCollapsedCorner, mExpandedCorner;
    private int mBaseColor;
    private int mAlpha;
    private int mBlurRadius;

    private ValueAnimator mAnimator;
    private PathInterpolator mEmphasizedInterpolator = new PathInterpolator(0.2f, 0f, 0f, 1f);

    public NavIslandView(Context context, PrefsMap<String, Object> prefs) {
        super(context);
        this.mPrefs = prefs;
        
        mPillContainer = new FrameLayout(context);
        mPillDrawable = new GradientDrawable();
        mPillDrawable.setShape(GradientDrawable.RECTANGLE);
        mPillContainer.setBackground(mPillDrawable);
        
        mContentContainer = new NavIslandContentContainer(context, prefs);
        mContentContainer.setAlpha(0f);
        mContentContainer.setVisibility(View.GONE);
        mPillContainer.addView(mContentContainer);

        addView(mPillContainer);
        
        mNotificationText = new android.widget.TextView(context);
        mNotificationText.setTextColor(android.graphics.Color.WHITE);
        mNotificationText.setTextSize(14f);
        mNotificationText.setGravity(android.view.Gravity.CENTER);
        mNotificationText.setVisibility(View.GONE);
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        textParams.gravity = android.view.Gravity.CENTER;
        mPillContainer.addView(mNotificationText, textParams);
        
        initView();
    }

    private void initView() {
        float density = getResources().getDisplayMetrics().density;
        
        // Settings for Collapsed State
        int widthDp = mPrefs.getInt("nav_island_pill_width", 320);
        int cornerRadiusDp = mPrefs.getInt("nav_island_pill_corner_radius", 28);
        int opacityPercent = mPrefs.getInt("nav_island_opacity", 85);
        int blurRadiusScaled = mPrefs.getInt("nav_island_blur_radius", 800);
        mBaseColor = mPrefs.getInt("nav_island_base_color", 0x0FFFFFFF);

        mCollapsedWidth = (int) (widthDp * density);
        mCollapsedHeight = (int) (28 * density); // Standard pill height
        mCollapsedBottomMargin = (int) (12 * density); // Bottom padding
        mCollapsedCorner = cornerRadiusDp * density;
        
        // Settings for Expanded State
        mExpandedWidth = (int) (340 * density);
        mExpandedHeight = (int) (380 * density); // Fits Brightness + Volume
        mExpandedBottomMargin = (int) (24 * density);
        mExpandedCorner = 32 * density;

        mBlurRadius = (int) (blurRadiusScaled / 100f * density);
        mAlpha = (int) (opacityPercent / 100f * 255f);

        mPillDrawable.setColor(mBaseColor);
        mPillDrawable.setAlpha(mAlpha);
        mPillDrawable.setCornerRadius(mCollapsedCorner);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (mBlurRadius > 0) {
                mPillContainer.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(
                        mBlurRadius, mBlurRadius, android.graphics.Shader.TileMode.CLAMP));
            } else {
                mPillContainer.setRenderEffect(null);
            }
        }

        // Initially collapsed wrapper
        mLayoutParams = new WindowManager.LayoutParams(
                mCollapsedWidth,
                mCollapsedHeight,
                2024, // TYPE_STATUS_BAR_ADDITIONAL
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        mLayoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        mLayoutParams.y = mCollapsedBottomMargin;
        
        FrameLayout.LayoutParams pillParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        mPillContainer.setLayoutParams(pillParams);
        
        mActionExecutor = new com.sevtinge.hyperceiler.hook.module.app.SystemUI.Phone.actions.ActionExecutor(getContext(), this);
        mGestureDetector = new android.view.GestureDetector(getContext(), 
                new com.sevtinge.hyperceiler.hook.module.app.SystemUI.Phone.gesture.NavIslandGestureDetector(getContext(), mPrefs, mActionExecutor));
        
        // Touch to expand / gesture mapping
        mPillContainer.setOnTouchListener((v, event) -> {
            mGestureDetector.onTouchEvent(event);
            return true;
        });
        
        // Immersive mode detection to hide pill during full-screen games/video
        setOnApplyWindowInsetsListener((v, insets) -> {
            boolean isNavBarVisible = insets.isVisible(android.view.WindowInsets.Type.navigationBars());
            if (!isNavBarVisible) {
                mPillContainer.setVisibility(View.GONE);
                if (mIsExpanded) toggleExpansion(); // auto-collapse if expanded
            } else {
                mPillContainer.setVisibility(View.VISIBLE);
            }
            return insets;
        });
    }
    
    public void toggleExpansion() {
        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }
        
        mIsExpanded = !mIsExpanded;
        
        // Pre-animation window adjustments
        if (mIsExpanded) {
            // Give window full touch intercept and screen size for the expanded overlay and scrim
            mLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            mLayoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
            mLayoutParams.y = 0;
            // Remove NOT_TOUCH_MODAL so we can detect clicks outside to dismiss
            mLayoutParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            if (mWindowManager != null && isAttachedToWindow()) {
                mWindowManager.updateViewLayout(this, mLayoutParams);
            }
            
            // Set inner container to absolute size at bottom
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mPillContainer.getLayoutParams();
            lp.width = mCollapsedWidth;
            lp.height = mCollapsedHeight;
            lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            lp.bottomMargin = mCollapsedBottomMargin;
            mPillContainer.setLayoutParams(lp);
        }
        
        // Animate
        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setDuration(350);
        mAnimator.setInterpolator(mEmphasizedInterpolator);
        
        final int startWidth = mIsExpanded ? mCollapsedWidth : mExpandedWidth;
        final int endWidth = mIsExpanded ? mExpandedWidth : mCollapsedWidth;
        final int startHeight = mIsExpanded ? mCollapsedHeight : mExpandedHeight;
        final int endHeight = mIsExpanded ? mExpandedHeight : mCollapsedHeight;
        final int startMargin = mIsExpanded ? mCollapsedBottomMargin : mExpandedBottomMargin;
        final int endMargin = mIsExpanded ? mExpandedBottomMargin : mCollapsedBottomMargin;
        final float startCorner = mIsExpanded ? mCollapsedCorner : mExpandedCorner;
        final float endCorner = mIsExpanded ? mExpandedCorner : mCollapsedCorner;
        
        mAnimator.addUpdateListener(anim -> {
            float v = (float) anim.getAnimatedValue();
            
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mPillContainer.getLayoutParams();
            lp.width = (int) (startWidth + (endWidth - startWidth) * v);
            lp.height = (int) (startHeight + (endHeight - startHeight) * v);
            lp.bottomMargin = (int) (startMargin + (endMargin - startMargin) * v);
            mPillContainer.setLayoutParams(lp);
            
            mPillDrawable.setCornerRadius(startCorner + (endCorner - startCorner) * v);

            // Animate content alpha
            if (mContentContainer != null) {
                float targetAlpha = mIsExpanded ? v : (1f - v);
                mContentContainer.setAlpha(targetAlpha);
                mContentContainer.setVisibility(targetAlpha > 0.05f ? View.VISIBLE : View.GONE);
            }
            
            // Animate Scrim (darkens rest of screen to 40% black)
            float scrimAlpha = mIsExpanded ? (v * 0.4f) : ((1f - v) * 0.4f);
            setBackgroundColor(Color.argb((int)(scrimAlpha * 255), 0, 0, 0));
        });
        
        mAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (!mIsExpanded) {
                    // Restore window bounds to minimalist state
                    mLayoutParams.width = mCollapsedWidth;
                    mLayoutParams.height = mCollapsedHeight;
                    mLayoutParams.y = mCollapsedBottomMargin;
                    mLayoutParams.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
                    if (mWindowManager != null && isAttachedToWindow()) {
                        mWindowManager.updateViewLayout(NavIslandView.this, mLayoutParams);
                    }
                    
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mPillContainer.getLayoutParams();
                    lp.width = FrameLayout.LayoutParams.MATCH_PARENT;
                    lp.height = FrameLayout.LayoutParams.MATCH_PARENT;
                    lp.bottomMargin = 0;
                    mPillContainer.setLayoutParams(lp);
                    
                    setBackgroundColor(Color.TRANSPARENT);
                }
            }
        });
        
        mAnimator.start();
    }
    
    public boolean isExpanded() {
        return mIsExpanded;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mIsExpanded && event.getAction() == MotionEvent.ACTION_DOWN) {
            // Clicked outside the expanded pill
            toggleExpansion();
            return true;
        }
        return super.onTouchEvent(event);
    }

    public void attachToWindow(WindowManager windowManager) {
        this.mWindowManager = windowManager;
        try {
            mWindowManager.addView(this, mLayoutParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateConfiguration() {
        if (!mIsExpanded) {
            removeAllViews();
            addView(mPillContainer);
            initView();
            if (mWindowManager != null && isAttachedToWindow()) {
                mWindowManager.updateViewLayout(this, mLayoutParams);
            }
        }
    }

    public void showNotification(String tickerText) {
        if (mNotificationText != null) {
            mNotificationText.setText(tickerText);
            mNotificationText.setVisibility(View.VISIBLE);
            if (mContentContainer != null) mContentContainer.setVisibility(View.GONE);
            
            // Auto hide after 3 seconds
            postDelayed(() -> {
                if (mNotificationText != null) mNotificationText.setVisibility(View.GONE);
                if (mContentContainer != null && mIsExpanded) mContentContainer.setVisibility(View.VISIBLE);
            }, 3000);
        }
    }

    public void updatePerAppProfile(String foregroundPackage) {
        // Adjust pill behavior/macros based on foreground package
        if (foregroundPackage == null) return;
        
        if (foregroundPackage.equals("com.android.camera")) {
            // Immersive mode for camera
            mPillContainer.setVisibility(View.GONE);
        } else if (foregroundPackage.contains("youtube") || foregroundPackage.contains("video")) {
            // Darker, smaller profile for media
            mPillDrawable.setAlpha((int) (mAlpha * 0.5f));
            mPillContainer.setVisibility(View.VISIBLE);
        } else {
            // Default behavior
            mPillDrawable.setAlpha(mAlpha);
            mPillContainer.setVisibility(View.VISIBLE);
        }
    }
}
