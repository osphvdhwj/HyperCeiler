package com.sevtinge.hyperceiler.hook.module.app.SystemUI.Phone;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class NavIslandSlider extends View {

    private Paint mTrackPaint;
    private Paint mProgressPaint;
    
    private RectF mTrackRect = new RectF();
    private RectF mProgressRect = new RectF();
    
    private float mCornerRadius;
    private int mMax = 100;
    private int mProgress = 0;
    
    private float mAnimatedProgress = 0f;
    private ValueAnimator mProgressAnimator;
    
    private OnProgressListener mListener;

    public interface OnProgressListener {
        void onProgressChanged(int progress, boolean fromUser);
    }

    public NavIslandSlider(Context context) {
        super(context);
        init();
    }

    private void init() {
        mTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTrackPaint.setColor(Color.parseColor("#40FFFFFF")); // Semi-transparent white
        
        mProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mProgressPaint.setColor(Color.parseColor("#FFFFFFFF")); // Solid white for active track
        
        mCornerRadius = 24f * getResources().getDisplayMetrics().density; // 24dp
    }
    
    public void setTrackColor(int color) {
        mTrackPaint.setColor(color);
        invalidate();
    }
    
    public void setProgressColor(int color) {
        mProgressPaint.setColor(color);
        invalidate();
    }

    public void setMax(int max) {
        this.mMax = Math.max(1, max);
        invalidate();
    }

    public void setProgress(int progress) {
        progress = Math.max(0, Math.min(progress, mMax));
        if (this.mProgress != progress) {
            this.mProgress = progress;
            animateProgress((float) progress / mMax);
        }
    }
    
    public void setOnProgressListener(OnProgressListener listener) {
        this.mListener = listener;
    }

    private void animateProgress(float targetRatio) {
        if (mProgressAnimator != null && mProgressAnimator.isRunning()) {
            mProgressAnimator.cancel();
        }
        mProgressAnimator = ValueAnimator.ofFloat(mAnimatedProgress, targetRatio);
        mProgressAnimator.setDuration(250);
        mProgressAnimator.setInterpolator(new DecelerateInterpolator());
        mProgressAnimator.addUpdateListener(anim -> {
            mAnimatedProgress = (float) anim.getAnimatedValue();
            invalidate();
        });
        mProgressAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int w = getWidth();
        int h = getHeight();
        
        mTrackRect.set(0, 0, w, h);
        canvas.drawRoundRect(mTrackRect, mCornerRadius, mCornerRadius, mTrackPaint);
        
        float rightEdge = w * mAnimatedProgress;
        if (rightEdge > 0) {
            // Ensure the progress bar doesn't look squished if width is smaller than 2 * radius
            if (rightEdge < 2 * mCornerRadius && mAnimatedProgress > 0) {
                rightEdge = 2 * mCornerRadius;
            }
            mProgressRect.set(0, 0, rightEdge, h);
            canvas.drawRoundRect(mProgressRect, mCornerRadius, mCornerRadius, mProgressPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        
        float x = event.getX();
        float ratio = x / getWidth();
        ratio = Math.max(0f, Math.min(ratio, 1f));
        
        int newProgress = Math.round(ratio * mMax);
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                mAnimatedProgress = ratio; // Update instantly for drag
                invalidate();
                if (mProgress != newProgress) {
                    mProgress = newProgress;
                    if (mListener != null) {
                        mListener.onProgressChanged(mProgress, true);
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                animateProgress((float) mProgress / mMax);
                return true;
        }
        
        return super.onTouchEvent(event);
    }
}
