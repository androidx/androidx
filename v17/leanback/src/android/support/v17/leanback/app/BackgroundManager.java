/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.app;

import android.support.v17.leanback.R;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;

/**
 * Supports background continuity between multiple activities.
 *
 * An activity should instantiate a BackgroundManager and {@link #attach}
 * to the activity's window.  When the activity is started, the background is
 * initialized to the current background values stored in a continuity service.
 * The background continuity service is updated as the background is updated.
 *
 * At some point, for example when stopped, the activity may release its background
 * state.
 *
 * When an activity is resumed, if the BM has not been released, the continuity service
 * is updated from the BM state.  If the BM was released, the BM inherits the current
 * state from the continuity service.
 *
 * When the last activity is destroyed, the background state is reset.
 *
 * Backgrounds consist of several layers, from back to front:
 * - the background drawable of the theme
 * - a solid color (set via setColor)
 * - two drawables, previous and current (set via setBitmap or setDrawable),
 *   which may be in transition
 *
 * BackgroundManager holds references to potentially large bitmap drawables.
 * Call {@link #release} to release these references when the activity is not
 * visible.
 *
 * TODO: support for multiple app processes requires a proper android service
 * instead of the shared memory "service" implemented here. Such a service could
 * support continuity between fragments of different applications if desired.
 */
public final class BackgroundManager {
    private static final String TAG = "BackgroundManager";
    private static final boolean DEBUG = false;

    private static final int FULL_ALPHA = 255;
    private static final int DIM_ALPHA_ON_SOLID = (int) (0.8f * FULL_ALPHA);
    private static final int CHANGE_BG_DELAY_MS = 500;
    private static final int FADE_DURATION_QUICK = 200;
    private static final int FADE_DURATION_SLOW = 1000;

    /**
     * Using a separate window for backgrounds can improve graphics performance by
     * leveraging hardware display layers.
     * TODO: support a leanback configuration option.
     */
    private static final boolean USE_SEPARATE_WINDOW = false;

    /**
     * If true, bitmaps will be scaled to the exact display size.
     * Small bitmaps will be scaled up, using more memory but improving display quality.
     * Large bitmaps will be scaled down to use less memory.
     * Introduces an allocation overhead.
     * TODO: support a leanback configuration option.
     */
    private static final boolean SCALE_BITMAPS_TO_FIT = true;

    private static final String WINDOW_NAME = "BackgroundManager";

    private Context mContext;
    private Handler mHandler;
    private Window mWindow;
    private WindowManager mWindowManager;
    private View mBgView;
    private BackgroundContinuityService mService;
    private int mThemeDrawableResourceId;

    private int mHeightPx;
    private int mWidthPx;
    private Drawable mBackgroundDrawable;
    private int mBackgroundColor;
    private boolean mAttached;

    private class DrawableWrapper {
        protected int mAlpha;
        protected Drawable mDrawable;
        protected ObjectAnimator mAnimator;
        protected boolean mAnimationPending;

        public DrawableWrapper(Drawable drawable) {
            mDrawable = drawable;
            setAlpha(FULL_ALPHA);
        }

        public Drawable getDrawable() {
            return mDrawable;
        }
        public void setAlpha(int alpha) {
            mAlpha = alpha;
            mDrawable.setAlpha(alpha);
        }
        public int getAlpha() {
            return mAlpha;
        }
        public void setColor(int color) {
            ((ColorDrawable) mDrawable).setColor(color);
        }
        public void fadeIn(int durationMs, int delayMs) {
            fade(durationMs, delayMs, FULL_ALPHA);
        }
        public void fadeOut(int durationMs) {
            fade(durationMs, 0, 0);
        }
        public void fade(int durationMs, int delayMs, int alpha) {
            if (mAnimator != null && mAnimator.isStarted()) {
                mAnimator.cancel();
            }
            mAnimator = ObjectAnimator.ofInt(this, "alpha", alpha);
            mAnimator.setInterpolator(new LinearInterpolator());
            mAnimator.setDuration(durationMs);
            mAnimator.setStartDelay(delayMs);
            mAnimationPending = true;
        }
        public boolean isAnimationPending() {
            return mAnimationPending;
        }
        public boolean isAnimationStarted() {
            return mAnimator != null && mAnimator.isStarted();
        }
        public void startAnimation() {
            mAnimator.start();
            mAnimationPending = false;
        }
    }

    private LayerDrawable mLayerDrawable;
    private DrawableWrapper mLayerWrapper;
    private DrawableWrapper mImageInWrapper;
    private DrawableWrapper mImageOutWrapper;
    private DrawableWrapper mColorWrapper;
    private DrawableWrapper mDimWrapper;

    private Drawable mThemeDrawable;
    private ChangeBackgroundRunnable mChangeRunnable;

    /**
     * Shared memory continuity service.
     */
    private static class BackgroundContinuityService {
        private static final String TAG = "BackgroundContinuityService";
        private static boolean DEBUG = BackgroundManager.DEBUG;

        private static BackgroundContinuityService sService = new BackgroundContinuityService();

        private int mColor;
        private Drawable mDrawable;
        private int mCount;

        private BackgroundContinuityService() {
            reset();
        }

        private void reset() {
            mColor = Color.TRANSPARENT;
            mDrawable = null;
        }

        public static BackgroundContinuityService getInstance() {
            final int count = sService.mCount++;
            if (DEBUG) Log.v(TAG, "Returning instance with new count " + count);
            return sService;
        }

        public void unref() {
            if (mCount <= 0) throw new IllegalStateException("Can't unref, count " + mCount);
            if (--mCount == 0) {
                if (DEBUG) Log.v(TAG, "mCount is zero, resetting");
                reset();
            }
        }
        public int getColor() {
            return mColor;
        }
        public Drawable getDrawable() {
            return mDrawable;
        }
        public void setColor(int color) {
            mColor = color;
        }
        public void setDrawable(Drawable drawable) {
            mDrawable = drawable;
        }
    }

    private Drawable getThemeDrawable() {
        Drawable drawable = null;
        if (mThemeDrawableResourceId != -1) {
            drawable = mContext.getResources().getDrawable(mThemeDrawableResourceId);
        }
        if (drawable == null) {
            drawable = createEmptyDrawable();
        }
        return drawable;
    }

    /**
     * Construct a background manager instance.
     * Initial background set from continuity service.
     */
    public BackgroundManager(Activity activity) {
        mContext = activity;
        mService = BackgroundContinuityService.getInstance();
        mHeightPx = mContext.getResources().getDisplayMetrics().heightPixels;
        mWidthPx = mContext.getResources().getDisplayMetrics().widthPixels;
        mHandler = new Handler();

        TypedArray ta = activity.getTheme().obtainStyledAttributes(new int[] {
                android.R.attr.windowBackground });
        mThemeDrawableResourceId = ta.getResourceId(0, -1);
        if (mThemeDrawableResourceId < 0) {
            if (DEBUG) Log.v(TAG, "BackgroundManager no window background resource!");
        }
        ta.recycle();

        createFragment(activity);
    }

    private void createFragment(Activity activity) {
        // Use a fragment to ensure the background manager gets detached properly.
        BackgroundFragment fragment = (BackgroundFragment) activity.getFragmentManager()
                .findFragmentByTag(TAG);
        if (fragment == null) {
            fragment = new BackgroundFragment();
            activity.getFragmentManager().beginTransaction().add(fragment, TAG).commit();
        }
        fragment.setBackgroundManager(this);
    }

    /**
     * Synchronizes state when the owning activity is resumed.
     */
    void onActivityResume() {
        if (mService == null) {
            return;
        }
        if (mLayerDrawable == null) {
            if (DEBUG) Log.v(TAG, "onActivityResume: released state, syncing with service");
            syncWithService();
        } else {
            if (DEBUG) Log.v(TAG, "onActivityResume: updating service color "
                    + mBackgroundColor + " drawable " + mBackgroundDrawable);
            mService.setColor(mBackgroundColor);
            mService.setDrawable(mBackgroundDrawable);
        }
    }

    private void syncWithService() {
        int color = mService.getColor();
        Drawable drawable = mService.getDrawable();

        if (DEBUG) Log.v(TAG, "syncWithService color " + Integer.toHexString(color)
                + " drawable " + drawable);

        if (drawable != null) {
            drawable = drawable.getConstantState().newDrawable(mContext.getResources()).mutate();
        }

        mBackgroundColor = color;
        mBackgroundDrawable = drawable;

        updateImmediate();
    }

    private void lazyInit() {
        if (mLayerDrawable != null) {
            return;
        }

        mLayerDrawable = (LayerDrawable) mContext.getResources().getDrawable(
                R.drawable.lb_background);
        mBgView.setBackground(mLayerDrawable);

        mLayerDrawable.setDrawableByLayerId(R.id.background_imageout, createEmptyDrawable());

        mDimWrapper = new DrawableWrapper(
                mLayerDrawable.findDrawableByLayerId(R.id.background_dim));

        mLayerWrapper = new DrawableWrapper(mLayerDrawable);

        mColorWrapper = new DrawableWrapper(
                mLayerDrawable.findDrawableByLayerId(R.id.background_color));
    }

    /**
     * Make the background visible on the given window.
     */
    public void attach(Window window) {
        if (USE_SEPARATE_WINDOW) {
            attachBehindWindow(window);
        } else {
            attachToView(window.getDecorView());
        }
    }

    private void attachBehindWindow(Window window) {
        if (DEBUG) Log.v(TAG, "attachBehindWindow " + window);
        mWindow = window;
        mWindowManager = window.getWindowManager();

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                // Media window sits behind the main application window
                WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA,
                // Avoid default to software format RGBA
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                android.graphics.PixelFormat.TRANSLUCENT);
        params.setTitle(WINDOW_NAME);
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;

        View backgroundView = LayoutInflater.from(mContext).inflate(
                R.layout.lb_background_window, null);
        mWindowManager.addView(backgroundView, params);

        attachToView(backgroundView);
    }

    private void attachToView(View sceneRoot) {
        mBgView = sceneRoot;
        mAttached = true;
        syncWithService();
    }

    /**
     * Releases references to drawables and puts the background manager into
     * detached state.
     * Called when the associated activity is destroyed.
     * @hide
     */
    void detach() {
        if (DEBUG) Log.v(TAG, "detach");
        release();

        if (mWindowManager != null && mBgView != null) {
            mWindowManager.removeViewImmediate(mBgView);
        }

        mWindowManager = null;
        mWindow = null;
        mBgView = null;
        mAttached = false;

        if (mService != null) {
            mService.unref();
            mService = null;
        }
    }

    /**
     * Releases references to drawables.
     * Typically called to reduce memory overhead when not visible.
     * <p>
     * When an activity is resumed, if the BM has not been released, the continuity service
     * is updated from the BM state.  If the BM was released, the BM inherits the current
     * state from the continuity service.
     * </p>
     */
    public void release() {
        if (DEBUG) Log.v(TAG, "release");
        if (mLayerDrawable != null) {
            mLayerDrawable.setDrawableByLayerId(R.id.background_imagein, createEmptyDrawable());
            mLayerDrawable.setDrawableByLayerId(R.id.background_imageout, createEmptyDrawable());
            mLayerDrawable = null;
        }
        mLayerWrapper = null;
        mImageInWrapper = null;
        mImageOutWrapper = null;
        mColorWrapper = null;
        mDimWrapper = null;
        mThemeDrawable = null;
        if (mChangeRunnable != null) {
            mChangeRunnable.cancel();
            mChangeRunnable = null;
        }
        releaseBackgroundBitmap();
    }

    private void releaseBackgroundBitmap() {
        mBackgroundDrawable = null;
    }

    private void updateImmediate() {
        lazyInit();

        mColorWrapper.setColor(mBackgroundColor);
        if (mDimWrapper != null) {
            mDimWrapper.setAlpha(mBackgroundColor == Color.TRANSPARENT ? 0 : DIM_ALPHA_ON_SOLID);
        }
        showWallpaper(mBackgroundColor == Color.TRANSPARENT);

        mThemeDrawable = getThemeDrawable();
        mLayerDrawable.setDrawableByLayerId(R.id.background_theme, mThemeDrawable);

        if (mBackgroundDrawable == null) {
            mLayerDrawable.setDrawableByLayerId(R.id.background_imagein, createEmptyDrawable());
        } else {
            if (DEBUG) Log.v(TAG, "Background drawable is available");
            mImageInWrapper = new DrawableWrapper(mBackgroundDrawable);
            mLayerDrawable.setDrawableByLayerId(R.id.background_imagein, mBackgroundDrawable);
            if (mDimWrapper != null) {
                mDimWrapper.setAlpha(FULL_ALPHA);
            }
        }
    }

    /**
     * Sets the given color into the background.
     * Timing is undefined.
     */
    public void setColor(int color) {
        if (DEBUG) Log.v(TAG, "setColor " + Integer.toHexString(color));

        mBackgroundColor = color;
        mService.setColor(mBackgroundColor);

        if (mColorWrapper != null) {
            mColorWrapper.setColor(mBackgroundColor);
        }
    }

    /**
     * Set the given drawable into the background.
     * Timing is undefined.
     */
    public void setDrawable(Drawable drawable) {
        if (DEBUG) Log.v(TAG, "setBackgroundDrawable " + drawable);
        setDrawableInternal(drawable);
    }

    private void setDrawableInternal(Drawable drawable) {
        if (!mAttached) throw new IllegalStateException("Must attach before setting background drawable");

        if (mChangeRunnable != null) {
            mChangeRunnable.cancel();
        }
        mChangeRunnable = new ChangeBackgroundRunnable(drawable);

        mHandler.postDelayed(mChangeRunnable, CHANGE_BG_DELAY_MS);
    }

    /**
     * Set the given bitmap into the background.
     * Timing is undefined.
     */
    public void setBitmap(Bitmap bitmap) {
        if (DEBUG) Log.v(TAG, "setBitmap " + bitmap);

        if (bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
            if (DEBUG) Log.v(TAG, "invalid bitmap width or height");
            return;
        }

        if (mBackgroundDrawable instanceof BitmapDrawable &&
                ((BitmapDrawable) mBackgroundDrawable).getBitmap() == bitmap) {
            if (DEBUG) Log.v(TAG, "same bitmap detected");
            mService.setDrawable(mBackgroundDrawable);
            return;
        }

        if (SCALE_BITMAPS_TO_FIT && bitmap.getWidth() != mWidthPx) {
            // Scale proportionately to fit width.
            final float scale = (float) mWidthPx / (float) bitmap.getWidth();
            int height = (int) (mHeightPx / scale);
            if (height > bitmap.getHeight()) {
                height = bitmap.getHeight();
            }

            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);

            if (DEBUG) Log.v(TAG, "original image size " + bitmap.getWidth() + "x" + bitmap.getHeight() +
                    " extracting height " + height);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), height, matrix, true);
            if (DEBUG) Log.v(TAG, "new image size " + bitmap.getWidth() + "x" + bitmap.getHeight());
        }

        BitmapDrawable bitmapDrawable = new BitmapDrawable(mContext.getResources(), bitmap);
        bitmapDrawable.setGravity(Gravity.CLIP_HORIZONTAL);

        setDrawableInternal(bitmapDrawable);
    }

    private void applyBackgroundChanges() {
        if (!mAttached || mLayerWrapper == null) {
            return;
        }

        if (DEBUG) Log.v(TAG, "applyBackgroundChanges drawable " + mBackgroundDrawable);

        int dimAlpha = 0;

        if (mImageOutWrapper != null && mImageOutWrapper.isAnimationPending()) {
            if (DEBUG) Log.v(TAG, "mImageOutWrapper animation starting");
            mImageOutWrapper.startAnimation();
            mImageOutWrapper = null;
            dimAlpha = DIM_ALPHA_ON_SOLID;
        }

        if (mImageInWrapper == null && mBackgroundDrawable != null) {
            if (DEBUG) Log.v(TAG, "creating new imagein drawable");
            mImageInWrapper = new DrawableWrapper(mBackgroundDrawable);
            mLayerDrawable.setDrawableByLayerId(R.id.background_imagein, mBackgroundDrawable);
            if (DEBUG) Log.v(TAG, "mImageInWrapper animation starting");
            mImageInWrapper.setAlpha(0);
            mImageInWrapper.fadeIn(FADE_DURATION_SLOW, 0);
            mImageInWrapper.startAnimation();
            dimAlpha = FULL_ALPHA;
        }

        if (mDimWrapper != null && dimAlpha != 0) {
            if (DEBUG) Log.v(TAG, "dimwrapper animation starting to " + dimAlpha);
            mDimWrapper.fade(FADE_DURATION_SLOW, 0, dimAlpha);
            mDimWrapper.startAnimation();
        }
    }

    /**
     * Returns the color currently in use by the background.
     */
    public final int getColor() {
        return mBackgroundColor;
    }

    /**
     * Returns the {@link Drawable} currently in use by the background.
     */
    public Drawable getDrawable() {
        return mBackgroundDrawable;
    }

    /**
     * Task which changes the background.
     */
    class ChangeBackgroundRunnable implements Runnable {
        private Drawable mDrawable;
        private boolean mCancel;

        ChangeBackgroundRunnable(Drawable drawable) {
            mDrawable = drawable;
        }

        public void cancel() {
            mCancel = true;
        }

        @Override
        public void run() {
            if (!mCancel) {
                runTask();
            }
        }

        private void runTask() {
            boolean newBackground = false;
            lazyInit();

            if (mDrawable != mBackgroundDrawable) {
                newBackground = true;
                if (mDrawable instanceof BitmapDrawable &&
                        mBackgroundDrawable instanceof BitmapDrawable) {
                    if (((BitmapDrawable) mDrawable).getBitmap() ==
                            ((BitmapDrawable) mBackgroundDrawable).getBitmap()) {
                        if (DEBUG) Log.v(TAG, "same underlying bitmap detected");
                        newBackground = false;
                    }
                }
            }

            if (!newBackground) {
                return;
            }

            releaseBackgroundBitmap();

            if (mImageInWrapper != null) {
                mImageOutWrapper = new DrawableWrapper(mImageInWrapper.getDrawable());
                mImageOutWrapper.setAlpha(mImageInWrapper.getAlpha());
                mImageOutWrapper.fadeOut(FADE_DURATION_QUICK);

                // Order is important! Setting a drawable "removes" the
                // previous one from the view
                mLayerDrawable.setDrawableByLayerId(R.id.background_imagein, createEmptyDrawable());
                mLayerDrawable.setDrawableByLayerId(R.id.background_imageout,
                        mImageOutWrapper.getDrawable());
                mImageInWrapper.setAlpha(0);
                mImageInWrapper = null;
            }

            mBackgroundDrawable = mDrawable;
            mService.setDrawable(mBackgroundDrawable);

            applyBackgroundChanges();
        }
    }

    private Drawable createEmptyDrawable() {
        Bitmap bitmap = null;
        return new BitmapDrawable(mContext.getResources(), bitmap);
    }

    private void showWallpaper(boolean show) {
        if (mWindow == null) {
            return;
        }

        WindowManager.LayoutParams layoutParams = mWindow.getAttributes();
        if (show) {
            if ((layoutParams.flags & WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER) != 0) {
                return;
            }
            if (DEBUG) Log.v(TAG, "showing wallpaper");
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
        } else {
            if ((layoutParams.flags & WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER) == 0) {
                return;
            }
            if (DEBUG) Log.v(TAG, "hiding wallpaper");
            layoutParams.flags &= ~WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
        }

        mWindow.setAttributes(layoutParams);
    }
}
