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

import java.lang.ref.WeakReference;

import android.support.v17.leanback.R;
import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.support.v4.content.ContextCompat;

/**
 * Supports background image continuity between multiple Activities.
 *
 * <p>An Activity should instantiate a BackgroundManager and {@link #attach}
 * to the Activity's window.  When the Activity is started, the background is
 * initialized to the current background values stored in a continuity service.
 * The background continuity service is updated as the background is updated.
 *
 * <p>At some point, for example when it is stopped, the Activity may release
 * its background state.
 *
 * <p>When an Activity is resumed, if the BackgroundManager has not been
 * released, the continuity service is updated from the BackgroundManager state.
 * If the BackgroundManager was released, the BackgroundManager inherits the
 * current state from the continuity service.
 *
 * <p>When the last Activity is destroyed, the background state is reset.
 *
 * <p>Backgrounds consist of several layers, from back to front:
 * <ul>
 *   <li>the background Drawable of the theme</li>
 *   <li>a solid color (set via {@link #setColor})</li>
 *   <li>two Drawables, previous and current (set via {@link #setBitmap} or
 *   {@link #setDrawable}), which may be in transition</li>
 * </ul>
 *
 * <p>BackgroundManager holds references to potentially large bitmap Drawables.
 * Call {@link #release} to release these references when the Activity is not
 * visible.
 */
// TODO: support for multiple app processes requires a proper android service
// instead of the shared memory "service" implemented here. Such a service could
// support continuity between fragments of different applications if desired.
public final class BackgroundManager {
    private static final String TAG = "BackgroundManager";
    private static final boolean DEBUG = false;

    private static final int FULL_ALPHA = 255;
    private static final int DIM_ALPHA_ON_SOLID = (int) (0.8f * FULL_ALPHA);
    private static final int CHANGE_BG_DELAY_MS = 500;
    private static final int FADE_DURATION = 500;

    /**
     * Using a separate window for backgrounds can improve graphics performance by
     * leveraging hardware display layers.
     * TODO: support a leanback configuration option.
     */
    private static final boolean USE_SEPARATE_WINDOW = false;

    private static final String WINDOW_NAME = "BackgroundManager";
    private static final String FRAGMENT_TAG = BackgroundManager.class.getCanonicalName();

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

    private static class BitmapDrawable extends Drawable {

        static class ConstantState extends Drawable.ConstantState {
            Bitmap mBitmap;
            Matrix mMatrix;
            Paint mPaint;

            @Override
            public Drawable newDrawable() {
                return new BitmapDrawable(null, mBitmap, mMatrix);
            }

            @Override
            public int getChangingConfigurations() {
                return 0;
            }
        }

        private ConstantState mState = new ConstantState();

        BitmapDrawable(Resources resources, Bitmap bitmap) {
            this(resources, bitmap, null);
        }

        BitmapDrawable(Resources resources, Bitmap bitmap, Matrix matrix) {
            mState.mBitmap = bitmap;
            mState.mMatrix = matrix != null ? matrix : new Matrix();
            mState.mPaint = new Paint();
            mState.mPaint.setFilterBitmap(true);
        }

        Bitmap getBitmap() {
            return mState.mBitmap;
        }

        @Override
        public void draw(Canvas canvas) {
            if (mState.mBitmap == null) {
                return;
            }
            canvas.drawBitmap(mState.mBitmap, mState.mMatrix, mState.mPaint);
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.OPAQUE;
        }

        @Override
        public void setAlpha(int alpha) {
            if (mState.mPaint.getAlpha() != alpha) {
                mState.mPaint.setAlpha(alpha);
                invalidateSelf();
            }
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            // Abstract in Drawable, not implemented
        }

        @Override
        public ConstantState getConstantState() {
            return mState;
        }
    }

    private static class DrawableWrapper {
        protected int mAlpha;
        protected Drawable mDrawable;
        protected ValueAnimator mAnimator;
        protected boolean mAnimationPending;

        private final Interpolator mInterpolator = new LinearInterpolator();
        private final ValueAnimator.AnimatorUpdateListener mAnimationUpdateListener =
                new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setAlpha((Integer) animation.getAnimatedValue());
            }
        };

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
            mAnimator = ValueAnimator.ofInt(getAlpha(), alpha);
            mAnimator.addUpdateListener(mAnimationUpdateListener);
            mAnimator.setInterpolator(mInterpolator);
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
            startAnimation(null);
        }
        public void startAnimation(Animator.AnimatorListener listener) {
            if (listener != null) {
                mAnimator.addListener(listener);
            }
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

        /** Single cache of theme drawable */
        private int mLastThemeDrawableId;
        private WeakReference<Drawable> mLastThemeDrawable;

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
        public Drawable getThemeDrawable(Context context, int themeDrawableId) {
            Drawable drawable = null;
            if (mLastThemeDrawable != null && mLastThemeDrawableId == themeDrawableId) {
                drawable = mLastThemeDrawable.get();
            }
            if (drawable == null) {
                drawable = ContextCompat.getDrawable(context, themeDrawableId);
                mLastThemeDrawable = new WeakReference<Drawable>(drawable);
                mLastThemeDrawableId = themeDrawableId;
            }
            return drawable.getConstantState().newDrawable(context.getResources()).mutate();
        }
    }

    private Drawable getThemeDrawable() {
        Drawable drawable = null;
        if (mThemeDrawableResourceId != -1) {
            drawable = mService.getThemeDrawable(mContext, mThemeDrawableResourceId);
        }
        if (drawable == null) {
            drawable = createEmptyDrawable();
        }
        return drawable;
    }

    /**
     * Get the BackgroundManager associated with the Activity.
     * <p>
     * The BackgroundManager will be created on-demand for each individual
     * Activity. Subsequent calls will return the same BackgroundManager created
     * for this Activity.
     */
    public static BackgroundManager getInstance(Activity activity) {
        BackgroundFragment fragment = (BackgroundFragment) activity.getFragmentManager()
                .findFragmentByTag(FRAGMENT_TAG);
        if (fragment != null) {
            BackgroundManager manager = fragment.getBackgroundManager();
            if (manager != null) {
                return manager;
            }
            // manager is null: this is a fragment restored by FragmentManager,
            // fall through to create a BackgroundManager attach to it.
        }
        return new BackgroundManager(activity);
    }

    private BackgroundManager(Activity activity) {
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
                .findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            fragment = new BackgroundFragment();
            activity.getFragmentManager().beginTransaction().add(fragment, FRAGMENT_TAG).commit();
        } else {
            if (fragment.getBackgroundManager() != null) {
                throw new IllegalStateException("Created duplicated BackgroundManager for same " +
                        "activity, please use getInstance() instead");
            }
        }
        fragment.setBackgroundManager(this);
    }

    /**
     * Synchronizes state when the owning Activity is resumed.
     */
    void onActivityResume() {
        if (mService == null) {
            return;
        }
        if (mLayerDrawable == null) {
            if (DEBUG) Log.v(TAG, "onActivityResume " + this +
                    " released state, syncing with service");
            syncWithService();
        } else {
            if (DEBUG) Log.v(TAG, "onActivityResume " + this + " updating service color "
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

        mBackgroundColor = color;
        mBackgroundDrawable = drawable == null ? null :
            drawable.getConstantState().newDrawable().mutate();

        updateImmediate();
    }

    private void lazyInit() {
        if (mLayerDrawable != null) {
            return;
        }

        mLayerDrawable = (LayerDrawable) ContextCompat.getDrawable(mContext,
                R.drawable.lb_background).mutate();
        mBgView.setBackground(mLayerDrawable);

        mLayerDrawable.setDrawableByLayerId(R.id.background_imageout, createEmptyDrawable());

        mDimWrapper = new DrawableWrapper(
                mLayerDrawable.findDrawableByLayerId(R.id.background_dim));

        mLayerWrapper = new DrawableWrapper(mLayerDrawable);

        mColorWrapper = new DrawableWrapper(
                mLayerDrawable.findDrawableByLayerId(R.id.background_color));
    }

    /**
     * Make the background visible on the given Window.
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
     * Release references to Drawables and put the BackgroundManager into the
     * detached state. Called when the associated Activity is destroyed.
     * @hide
     */
    void detach() {
        if (DEBUG) Log.v(TAG, "detach " + this);
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
     * Release references to Drawables. Typically called to reduce memory
     * overhead when not visible.
     * <p>
     * When an Activity is resumed, if the BackgroundManager has not been
     * released, the continuity service is updated from the BackgroundManager
     * state. If the BackgroundManager was released, the BackgroundManager
     * inherits the current state from the continuity service.
     */
    public void release() {
        if (DEBUG) Log.v(TAG, "release " + this);
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
     * Set the background to the given color. The timing for when this becomes
     * visible in the app is undefined and may take place after a small delay.
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
     * Set the given drawable into the background. The provided Drawable will be
     * used unmodified as the background, without any scaling or cropping
     * applied to it. The timing for when this becomes visible in the app is
     * undefined and may take place after a small delay.
     */
    public void setDrawable(Drawable drawable) {
        if (DEBUG) Log.v(TAG, "setBackgroundDrawable " + drawable);
        setDrawableInternal(drawable);
    }

    private void setDrawableInternal(Drawable drawable) {
        if (!mAttached) {
            throw new IllegalStateException("Must attach before setting background drawable");
        }

        if (mChangeRunnable != null) {
            if (sameDrawable(drawable, mChangeRunnable.mDrawable)) {
                if (DEBUG) Log.v(TAG, "setting same drawable");
                return;
            }
            mChangeRunnable.cancel();
        }
        mChangeRunnable = new ChangeBackgroundRunnable(drawable);

        if (mImageInWrapper != null && mImageInWrapper.isAnimationStarted()) {
            if (DEBUG) Log.v(TAG, "animation in progress");
        } else {
            mHandler.postDelayed(mChangeRunnable, CHANGE_BG_DELAY_MS);
        }
    }

    /**
     * Set the given bitmap into the background. When using setBitmap to set the
     * background, the provided bitmap will be scaled and cropped to correctly
     * fit within the dimensions of the view. The timing for when this becomes
     * visible in the app is undefined and may take place after a small delay.
     */
    public void setBitmap(Bitmap bitmap) {
        if (DEBUG) {
            Log.v(TAG, "setBitmap " + bitmap);
        }

        if (bitmap == null) {
            setDrawableInternal(null);
            return;
        }

        if (bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
            if (DEBUG) {
                Log.v(TAG, "invalid bitmap width or height");
            }
            return;
        }

        Matrix matrix = null;

        if ((bitmap.getWidth() != mWidthPx || bitmap.getHeight() != mHeightPx)) {
            int dwidth = bitmap.getWidth();
            int dheight = bitmap.getHeight();
            float scale;

            // Scale proportionately to fit width and height.
            if (dwidth * mHeightPx > mWidthPx * dheight) {
                scale = (float) mHeightPx / (float) dheight;
            } else {
                scale = (float) mWidthPx / (float) dwidth;
            }

            int subX = Math.min((int) (mWidthPx / scale), dwidth);
            int dx = Math.max(0, (dwidth - subX) / 2);

            matrix = new Matrix();
            matrix.setScale(scale, scale);
            matrix.preTranslate(-dx, 0);

            if (DEBUG) Log.v(TAG, "original image size " + bitmap.getWidth() + "x" + bitmap.getHeight() +
                    " scale " + scale + " dx " + dx);
        }

        BitmapDrawable bitmapDrawable = new BitmapDrawable(mContext.getResources(), bitmap, matrix);

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
            mImageInWrapper.fadeIn(FADE_DURATION, 0);
            mImageInWrapper.startAnimation(mImageInListener);
            dimAlpha = FULL_ALPHA;
        }

        if (mDimWrapper != null && dimAlpha != 0) {
            if (DEBUG) Log.v(TAG, "dimwrapper animation starting to " + dimAlpha);
            mDimWrapper.fade(FADE_DURATION, 0, dimAlpha);
            mDimWrapper.startAnimation();
        }
    }

    private final Animator.AnimatorListener mImageInListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
        }
        @Override
        public void onAnimationRepeat(Animator animation) {
        }
        @Override
        public void onAnimationEnd(Animator animation) {
            if (mChangeRunnable != null) {
                if (DEBUG) Log.v(TAG, "animation ended, found change runnable");
                mChangeRunnable.run();
            }
        }
        @Override
        public void onAnimationCancel(Animator animation) {
        }
    };

    /**
     * Returns the current background color.
     */
    public final int getColor() {
        return mBackgroundColor;
    }

    /**
     * Returns the current background {@link Drawable}.
     */
    public Drawable getDrawable() {
        return mBackgroundDrawable;
    }

    private boolean sameDrawable(Drawable first, Drawable second) {
        if (first == null || second == null) {
            return false;
        }
        if (first == second) {
            return true;
        }
        if (first instanceof BitmapDrawable && second instanceof BitmapDrawable) {
            if (((BitmapDrawable) first).getBitmap().sameAs(((BitmapDrawable) second).getBitmap())) {
                return true;
            }
        }
        return false;
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
            lazyInit();

            if (sameDrawable(mDrawable, mBackgroundDrawable)) {
                if (DEBUG) Log.v(TAG, "same bitmap detected");
                return;
            }

            releaseBackgroundBitmap();

            if (mImageInWrapper != null) {
                mImageOutWrapper = new DrawableWrapper(mImageInWrapper.getDrawable());
                mImageOutWrapper.setAlpha(mImageInWrapper.getAlpha());
                mImageOutWrapper.fadeOut(FADE_DURATION);

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

            mChangeRunnable = null;
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
