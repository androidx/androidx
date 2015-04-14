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

import android.support.annotation.ColorInt;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
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
import android.support.v17.leanback.widget.BackgroundHelper;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.animation.AnimationUtils;
import android.support.v4.app.FragmentActivity;
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

    interface FragmentStateQueriable {
        public boolean isResumed();
    }

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
    private FragmentStateQueriable mFragmentState;

    private int mHeightPx;
    private int mWidthPx;
    private Drawable mBackgroundDrawable;
    private int mBackgroundColor;
    private boolean mAttached;
    private long mLastSetTime;

    private final Interpolator mAccelerateInterpolator;
    private final Interpolator mDecelerateInterpolator;
    private final ValueAnimator mAnimator;
    private final ValueAnimator mDimAnimator;

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
            if (mState.mPaint.getAlpha() < FULL_ALPHA && mState.mPaint.getColorFilter() != null) {
                throw new IllegalStateException("Can't draw with translucent alpha and color filter");
            }
            canvas.drawBitmap(mState.mBitmap, mState.mMatrix, mState.mPaint);
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }

        @Override
        public void setAlpha(int alpha) {
            if (mState.mPaint.getAlpha() != alpha) {
                mState.mPaint.setAlpha(alpha);
                invalidateSelf();
            }
        }

        /**
         * Does not invalidateSelf to avoid recursion issues.
         * Caller must ensure appropriate invalidation.
         */
        @Override
        public void setColorFilter(ColorFilter cf) {
            mState.mPaint.setColorFilter(cf);
        }

        public ColorFilter getColorFilter() {
            return mState.mPaint.getColorFilter();
        }

        @Override
        public ConstantState getConstantState() {
            return mState;
        }
    }

    private static class DrawableWrapper {
        private int mAlpha = FULL_ALPHA;
        private Drawable mDrawable;
        private ColorFilter mColorFilter;

        public DrawableWrapper(Drawable drawable) {
            mDrawable = drawable;
            updateAlpha();
            updateColorFilter();
        }
        public DrawableWrapper(DrawableWrapper wrapper, Drawable drawable) {
            mDrawable = drawable;
            mAlpha = wrapper.getAlpha();
            updateAlpha();
            mColorFilter = wrapper.getColorFilter();
            updateColorFilter();
        }

        public Drawable getDrawable() {
            return mDrawable;
        }
        public void setAlpha(int alpha) {
            mAlpha = alpha;
            updateAlpha();
        }
        public int getAlpha() {
            return mAlpha;
        }
        private void updateAlpha() {
            mDrawable.setAlpha(mAlpha);
        }

        public ColorFilter getColorFilter() {
            return mColorFilter;
        }
        public void setColorFilter(ColorFilter colorFilter) {
            mColorFilter = colorFilter;
            updateColorFilter();
        }
        private void updateColorFilter() {
            mDrawable.setColorFilter(mColorFilter);
        }

        public void setColor(int color) {
            ((ColorDrawable) mDrawable).setColor(color);
        }
    }

    static class TranslucentLayerDrawable extends LayerDrawable {
        private DrawableWrapper[] mWrapper;
        private Paint mPaint = new Paint();

        public TranslucentLayerDrawable(Drawable[] drawables) {
            super(drawables);
            int count = drawables.length;
            mWrapper = new DrawableWrapper[count];
            for (int i = 0; i < count; i++) {
                mWrapper[i] = new DrawableWrapper(drawables[i]);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            if (mPaint.getAlpha() != alpha) {
                int previousAlpha = mPaint.getAlpha();
                mPaint.setAlpha(alpha);
                invalidateSelf();
                onAlphaChanged(previousAlpha, alpha);
            }
        }

        // Queried by system transitions
        public int getAlpha() {
            return mPaint.getAlpha();
        }

        protected void onAlphaChanged(int oldAlpha, int newAlpha) {
        }

        @Override
        public Drawable mutate() {
            Drawable drawable = super.mutate();
            int count = getNumberOfLayers();
            for (int i = 0; i < count; i++) {
                if (mWrapper[i] != null) {
                    mWrapper[i] = new DrawableWrapper(mWrapper[i], getDrawable(i));
                }
            }
            invalidateSelf();
            return drawable;
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public boolean setDrawableByLayerId(int id, Drawable drawable) {
            return updateDrawable(id, drawable) != null;
        }

        public DrawableWrapper updateDrawable(int id, Drawable drawable) {
            super.setDrawableByLayerId(id, drawable);
            for (int i = 0; i < getNumberOfLayers(); i++) {
                if (getId(i) == id) {
                    mWrapper[i] = new DrawableWrapper(drawable);
                    // Must come after mWrapper was updated so it can be seen by updateColorFilter
                    invalidateSelf();
                    return mWrapper[i];
                }
            }
            return null;
        }

        public void clearDrawable(int id, Context context) {
            for (int i = 0; i < getNumberOfLayers(); i++) {
                if (getId(i) == id) {
                    mWrapper[i] = null;
                    super.setDrawableByLayerId(id, createEmptyDrawable(context));
                    break;
                }
            }
        }

        public DrawableWrapper findWrapperById(int id) {
            for (int i = 0; i < getNumberOfLayers(); i++) {
                if (getId(i) == id) {
                    return mWrapper[i];
                }
            }
            return null;
        }

        @Override
        public void draw(Canvas canvas) {
            if (mPaint.getAlpha() < FULL_ALPHA) {
                canvas.saveLayer(0, 0, canvas.getWidth(), canvas.getHeight(),
                        mPaint, Canvas.ALL_SAVE_FLAG);
            }
            super.draw(canvas);
            if (mPaint.getAlpha() < FULL_ALPHA) {
                canvas.restore();
            }
        }
    }

    /**
     * Optimizes drawing when the dim drawable is an alpha-only color and imagein is opaque.
     * When the layer drawable is translucent (activity transition) then we can avoid the slow
     * saveLayer/restore draw path.
     */
    private class OptimizedTranslucentLayerDrawable extends TranslucentLayerDrawable {
        private PorterDuffColorFilter mColorFilter;
        private boolean mUpdatingColorFilter;

        public OptimizedTranslucentLayerDrawable(Drawable[] drawables) {
            super(drawables);
        }

        @Override
        protected void onAlphaChanged(int oldAlpha, int newAlpha) {
            if (newAlpha == FULL_ALPHA && oldAlpha < FULL_ALPHA) {
                if (DEBUG) Log.v(TAG, "transition complete");
                postChangeRunnable();
            }
        }

        @Override
        public void invalidateSelf() {
            super.invalidateSelf();
            updateColorFilter();
        }

        @Override
        public void invalidateDrawable(Drawable who) {
            if (!mUpdatingColorFilter) {
                invalidateSelf();
            }
        }

        private void updateColorFilter() {
            DrawableWrapper dimWrapper = findWrapperById(R.id.background_dim);
            DrawableWrapper imageInWrapper = findWrapperById(R.id.background_imagein);
            DrawableWrapper imageOutWrapper = findWrapperById(R.id.background_imageout);

            mColorFilter = null;
            if (imageInWrapper != null && imageInWrapper.getAlpha() == FULL_ALPHA &&
                    dimWrapper.getDrawable() instanceof ColorDrawable) {
                int dimColor = ((ColorDrawable) dimWrapper.getDrawable()).getColor();
                if (Color.red(dimColor) == 0 &&
                        Color.green(dimColor) == 0 &&
                        Color.blue(dimColor) == 0) {
                    int dimAlpha = 255 - Color.alpha(dimColor);
                    int color = Color.argb(getAlpha(), dimAlpha, dimAlpha, dimAlpha);
                    mColorFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY);
                }
            }
            mUpdatingColorFilter = true;
            if (imageInWrapper != null) {
                imageInWrapper.setColorFilter(mColorFilter);
            }
            if (imageOutWrapper != null) {
                imageOutWrapper.setColorFilter(null);
            }
            mUpdatingColorFilter = false;
        }

        @Override
        public void draw(Canvas canvas) {
            DrawableWrapper imageInWrapper = findWrapperById(R.id.background_imagein);
            if (imageInWrapper != null && imageInWrapper.getDrawable() != null &&
                    imageInWrapper.getColorFilter() != null) {
                imageInWrapper.getDrawable().draw(canvas);
            } else {
                super.draw(canvas);
            }
        }
    }

    private TranslucentLayerDrawable createOptimizedTranslucentLayerDrawable(
            LayerDrawable layerDrawable) {
        int numChildren = layerDrawable.getNumberOfLayers();
        Drawable[] drawables = new Drawable[numChildren];
        for (int i = 0; i < numChildren; i++) {
            drawables[i] = layerDrawable.getDrawable(i);
        }
        TranslucentLayerDrawable result = new OptimizedTranslucentLayerDrawable(drawables);
        for (int i = 0; i < numChildren; i++) {
            result.setId(i, layerDrawable.getId(i));
        }
        return result;
    }

    private TranslucentLayerDrawable mLayerDrawable;
    private Drawable mDimDrawable;
    private ChangeBackgroundRunnable mChangeRunnable;
    private boolean mChangeRunnablePending;

    private final Animator.AnimatorListener mAnimationListener = new Animator.AnimatorListener() {
        final Runnable mRunnable = new Runnable() {
            @Override
            public void run() {
                postChangeRunnable();
            }
        };

        @Override
        public void onAnimationStart(Animator animation) {
        }
        @Override
        public void onAnimationRepeat(Animator animation) {
        }
        @Override
        public void onAnimationEnd(Animator animation) {
            if (mLayerDrawable != null) {
                mLayerDrawable.clearDrawable(R.id.background_imageout, mContext);
            }
            mHandler.post(mRunnable);
        }
        @Override
        public void onAnimationCancel(Animator animation) {
        }
    };

    private final ValueAnimator.AnimatorUpdateListener mAnimationUpdateListener =
            new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            int fadeInAlpha = (Integer) animation.getAnimatedValue();
            DrawableWrapper imageInWrapper = getImageInWrapper();
            if (imageInWrapper != null) {
                imageInWrapper.setAlpha(fadeInAlpha);
            } else {
                DrawableWrapper imageOutWrapper = getImageOutWrapper();
                if (imageOutWrapper != null) {
                    imageOutWrapper.setAlpha(255 - fadeInAlpha);
                }
            }
        }
    };

    private final ValueAnimator.AnimatorUpdateListener mDimUpdateListener =
            new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            DrawableWrapper dimWrapper = getDimWrapper();
            if (dimWrapper != null) {
                dimWrapper.setAlpha((Integer) animation.getAnimatedValue());
            }
        }
    };

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
        private WeakReference<Drawable.ConstantState> mLastThemeDrawableState;

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
            if (mLastThemeDrawableState != null && mLastThemeDrawableId == themeDrawableId) {
                Drawable.ConstantState drawableState = mLastThemeDrawableState.get();
                if (DEBUG) Log.v(TAG, "got cached theme drawable state " + drawableState);
                if (drawableState != null) {
                    drawable = drawableState.newDrawable();
                }
            }
            if (drawable == null) {
                drawable = ContextCompat.getDrawable(context, themeDrawableId);
                if (DEBUG) Log.v(TAG, "loaded theme drawable " + drawable);
                mLastThemeDrawableState = new WeakReference<Drawable.ConstantState>(
                        drawable.getConstantState());
                mLastThemeDrawableId = themeDrawableId;
            }
            // No mutate required because this drawable is never manipulated.
            return drawable;
        }
    }

    private Drawable getThemeDrawable() {
        Drawable drawable = null;
        if (mThemeDrawableResourceId != -1) {
            drawable = mService.getThemeDrawable(mContext, mThemeDrawableResourceId);
        }
        if (drawable == null) {
            drawable = createEmptyDrawable(mContext);
        }
        return drawable;
    }

    /**
     * Returns the BackgroundManager associated with the given Activity.
     * <p>
     * The BackgroundManager will be created on-demand for each individual
     * Activity. Subsequent calls will return the same BackgroundManager created
     * for this Activity.
     */
    public static BackgroundManager getInstance(Activity activity) {
        if (activity instanceof FragmentActivity) {
            return getSupportInstance((FragmentActivity) activity);
        }
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
        return new BackgroundManager(activity, false);
    }

    private static BackgroundManager getSupportInstance(FragmentActivity activity) {
        BackgroundSupportFragment fragment = (BackgroundSupportFragment) activity
                .getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment != null) {
            BackgroundManager manager = fragment.getBackgroundManager();
            if (manager != null) {
                return manager;
            }
            // manager is null: this is a fragment restored by FragmentManager,
            // fall through to create a BackgroundManager attach to it.
        }
        return new BackgroundManager(activity, true);
    }

    private BackgroundManager(Activity activity, boolean isSupportFragmentActivity) {
        mContext = activity;
        mService = BackgroundContinuityService.getInstance();
        mHeightPx = mContext.getResources().getDisplayMetrics().heightPixels;
        mWidthPx = mContext.getResources().getDisplayMetrics().widthPixels;
        mHandler = new Handler();

        Interpolator defaultInterpolator = new FastOutLinearInInterpolator();
        mAccelerateInterpolator = AnimationUtils.loadInterpolator(mContext,
                android.R.anim.accelerate_interpolator);
        mDecelerateInterpolator = AnimationUtils.loadInterpolator(mContext,
                android.R.anim.decelerate_interpolator);

        mAnimator = ValueAnimator.ofInt(0, FULL_ALPHA);
        mAnimator.addListener(mAnimationListener);
        mAnimator.addUpdateListener(mAnimationUpdateListener);
        mAnimator.setInterpolator(defaultInterpolator);

        mDimAnimator = new ValueAnimator();
        mDimAnimator.addUpdateListener(mDimUpdateListener);

        TypedArray ta = activity.getTheme().obtainStyledAttributes(new int[] {
                android.R.attr.windowBackground });
        mThemeDrawableResourceId = ta.getResourceId(0, -1);
        if (mThemeDrawableResourceId < 0) {
            if (DEBUG) Log.v(TAG, "BackgroundManager no window background resource!");
        }
        ta.recycle();

        if (isSupportFragmentActivity) {
            createSupportFragment((FragmentActivity) activity);
        } else {
            createFragment(activity);
        }
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
        mFragmentState = fragment;
    }

    private void createSupportFragment(FragmentActivity activity) {
        // Use a fragment to ensure the background manager gets detached properly.
        BackgroundSupportFragment fragment = (BackgroundSupportFragment) activity
                .getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            fragment = new BackgroundSupportFragment();
            activity.getSupportFragmentManager().beginTransaction().add(fragment, FRAGMENT_TAG)
                    .commit();
        } else {
            if (fragment.getBackgroundManager() != null) {
                throw new IllegalStateException("Created duplicated BackgroundManager for same " +
                    "activity, please use getInstance() instead");
            }
        }
        fragment.setBackgroundManager(this);
        mFragmentState = fragment;
    }

    private DrawableWrapper getImageInWrapper() {
        return mLayerDrawable == null ? null :
                mLayerDrawable.findWrapperById(R.id.background_imagein);
    }

    private DrawableWrapper getImageOutWrapper() {
        return mLayerDrawable == null ? null :
                mLayerDrawable.findWrapperById(R.id.background_imageout);
    }

    private DrawableWrapper getDimWrapper() {
        return mLayerDrawable == null ? null :
                mLayerDrawable.findWrapperById(R.id.background_dim);
    }

    private DrawableWrapper getColorWrapper() {
        return mLayerDrawable == null ? null :
                mLayerDrawable.findWrapperById(R.id.background_color);
    }

    /**
     * Synchronizes state when the owning Activity is started.
     * At that point the view becomes visible.
     */
    void onActivityStart() {
        if (mService == null) {
            return;
        }
        if (mLayerDrawable == null) {
            if (DEBUG) Log.v(TAG, "onActivityStart " + this +
                    " released state, syncing with service");
            syncWithService();
        } else {
            if (DEBUG) Log.v(TAG, "onActivityStart " + this + " updating service color "
                    + mBackgroundColor + " drawable " + mBackgroundDrawable);
            mService.setColor(mBackgroundColor);
            mService.setDrawable(mBackgroundDrawable);
        }
    }

    void onResume() {
        if (DEBUG) Log.v(TAG, "onResume " + this);
        postChangeRunnable();
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

        LayerDrawable layerDrawable = (LayerDrawable)
                ContextCompat.getDrawable(mContext, R.drawable.lb_background).mutate();
        mLayerDrawable = createOptimizedTranslucentLayerDrawable(layerDrawable);
        BackgroundHelper.setBackgroundPreservingAlpha(mBgView, mLayerDrawable);

        mLayerDrawable.clearDrawable(R.id.background_imageout, mContext);
        mLayerDrawable.updateDrawable(R.id.background_theme, getThemeDrawable());

        updateDimWrapper();
    }

    private void updateDimWrapper() {
        if (mDimDrawable == null) {
            mDimDrawable = getDefaultDimLayer();
        }
        Drawable dimDrawable = mDimDrawable.getConstantState().newDrawable(
                mContext.getResources()).mutate();
        if (mLayerDrawable != null) {
            mLayerDrawable.updateDrawable(R.id.background_dim, dimDrawable);
        }
    }

    /**
     * Makes the background visible on the given Window.  The background manager must be attached
     * when the background is set.
     */
    public void attach(Window window) {
        if (USE_SEPARATE_WINDOW) {
            attachBehindWindow(window);
        } else {
            attachToView(window.getDecorView());
        }
    }

    /**
     * Sets the resource id for the drawable to be shown when there is no background set.
     * Overrides the window background drawable from the theme. This should
     * be called before attaching.
     */
    public void setThemeDrawableResourceId(int resourceId) {
        mThemeDrawableResourceId = resourceId;
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
     * Returns true if the background manager is currently attached; false otherwise.
     */
    public boolean isAttached() {
        return mAttached;
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
     * When an Activity is started, if the BackgroundManager has not been
     * released, the continuity service is updated from the BackgroundManager
     * state. If the BackgroundManager was released, the BackgroundManager
     * inherits the current state from the continuity service.
     */
    public void release() {
        if (DEBUG) Log.v(TAG, "release " + this);
        if (mLayerDrawable != null) {
            mLayerDrawable.clearDrawable(R.id.background_imagein, mContext);
            mLayerDrawable.clearDrawable(R.id.background_imageout, mContext);
            mLayerDrawable.clearDrawable(R.id.background_theme, mContext);
            mLayerDrawable = null;
        }
        if (mChangeRunnable != null) {
            mHandler.removeCallbacks(mChangeRunnable);
            mChangeRunnable = null;
        }
        releaseBackgroundBitmap();
    }

    private void releaseBackgroundBitmap() {
        mBackgroundDrawable = null;
    }

    private void setBackgroundDrawable(Drawable drawable) {
        mBackgroundDrawable = drawable;
        mService.setDrawable(mBackgroundDrawable);
    }

    /**
     * Sets the drawable used as a dim layer.
     */
    public void setDimLayer(Drawable drawable) {
        mDimDrawable = drawable;
        updateDimWrapper();
    }

    /**
     * Returns the drawable used as a dim layer.
     */
    public Drawable getDimLayer() {
        return mDimDrawable;
    }

    /**
     * Returns the default drawable used as a dim layer.
     */
    public Drawable getDefaultDimLayer() {
        return ContextCompat.getDrawable(mContext, R.color.lb_background_protection);
    }

    private void postChangeRunnable() {
        if (mChangeRunnable == null || !mChangeRunnablePending) {
            return;
        }

        // Postpone a pending change runnable until: no existing change animation in progress &&
        // activity is resumed (in the foreground) && layerdrawable fully opaque.
        // If the layerdrawable is translucent then an activity transition is in progress
        // and we want to use the optimized drawing path for performance reasons (see
        // OptimizedTranslucentLayerDrawable).
        if (mAnimator.isStarted()) {
            if (DEBUG) Log.v(TAG, "animation in progress");
        } else if (!mFragmentState.isResumed()) {
            if (DEBUG) Log.v(TAG, "not resumed");
        } else if (mLayerDrawable.getAlpha() < FULL_ALPHA) {
            if (DEBUG) Log.v(TAG, "in transition, alpha " + mLayerDrawable.getAlpha());
        } else {
            long delayMs = getRunnableDelay();
            if (DEBUG) Log.v(TAG, "posting runnable delayMs " + delayMs);
            mLastSetTime = System.currentTimeMillis();
            mHandler.postDelayed(mChangeRunnable, delayMs);
            mChangeRunnablePending = false;
        }
    }

    private void updateImmediate() {
        lazyInit();

        DrawableWrapper colorWrapper = getColorWrapper();
        if (colorWrapper != null) {
            colorWrapper.setColor(mBackgroundColor);
        }
        DrawableWrapper dimWrapper = getDimWrapper();
        if (dimWrapper != null) {
            dimWrapper.setAlpha(mBackgroundColor == Color.TRANSPARENT ? 0 : DIM_ALPHA_ON_SOLID);
        }
        showWallpaper(mBackgroundColor == Color.TRANSPARENT);

        if (mBackgroundDrawable == null) {
            mLayerDrawable.clearDrawable(R.id.background_imagein, mContext);
        } else {
            if (DEBUG) Log.v(TAG, "Background drawable is available");
            mLayerDrawable.updateDrawable(
                    R.id.background_imagein, mBackgroundDrawable);
            if (dimWrapper != null) {
                dimWrapper.setAlpha(FULL_ALPHA);
            }
        }
    }

    /**
     * Sets the background to the given color. The timing for when this becomes
     * visible in the app is undefined and may take place after a small delay.
     */
    public void setColor(@ColorInt int color) {
        if (DEBUG) Log.v(TAG, "setColor " + Integer.toHexString(color));

        mBackgroundColor = color;
        mService.setColor(mBackgroundColor);

        DrawableWrapper colorWrapper = getColorWrapper();
        if (colorWrapper != null) {
            colorWrapper.setColor(mBackgroundColor);
        }
    }

    /**
     * Sets the given drawable into the background. The provided Drawable will be
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
                if (DEBUG) Log.v(TAG, "new drawable same as pending");
                return;
            }
            mHandler.removeCallbacks(mChangeRunnable);
            mChangeRunnable = null;
        }

        // If layer drawable is null then the activity hasn't started yet.
        // If the layer drawable alpha is zero then the activity transition hasn't started yet.
        // In these cases we can update the background immediately and let activity transition
        // fade it in.
        if (mLayerDrawable == null || mLayerDrawable.getAlpha() == 0) {
            if (DEBUG) Log.v(TAG, "setDrawableInternal null or alpha is zero");
            setBackgroundDrawable(drawable);
            updateImmediate();
            return;
        }

        mChangeRunnable = new ChangeBackgroundRunnable(drawable);
        mChangeRunnablePending = true;

        postChangeRunnable();
    }

    private long getRunnableDelay() {
        return Math.max(0, mLastSetTime + CHANGE_BG_DELAY_MS - System.currentTimeMillis());
    }

    /**
     * Sets the given bitmap into the background. When using setBitmap to set the
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
        if (!mAttached) {
            return;
        }

        if (DEBUG) Log.v(TAG, "applyBackgroundChanges drawable " + mBackgroundDrawable);

        int dimAlpha = -1;

        if (getImageOutWrapper() != null) {
            dimAlpha = mBackgroundColor == Color.TRANSPARENT ? 0 : DIM_ALPHA_ON_SOLID;
        }

        DrawableWrapper imageInWrapper = getImageInWrapper();
        if (imageInWrapper == null && mBackgroundDrawable != null) {
            if (DEBUG) Log.v(TAG, "creating new imagein drawable");
            imageInWrapper = mLayerDrawable.updateDrawable(
                    R.id.background_imagein, mBackgroundDrawable);
            if (DEBUG) Log.v(TAG, "imageInWrapper animation starting");
            imageInWrapper.setAlpha(0);
            dimAlpha = FULL_ALPHA;
        }

        mAnimator.setDuration(FADE_DURATION);
        mAnimator.start();

        DrawableWrapper dimWrapper = getDimWrapper();
        if (dimWrapper != null && dimAlpha >= 0) {
            if (DEBUG) Log.v(TAG, "dimwrapper animation starting to " + dimAlpha);
            mDimAnimator.cancel();
            mDimAnimator.setIntValues(dimWrapper.getAlpha(), dimAlpha);
            mDimAnimator.setDuration(FADE_DURATION);
            mDimAnimator.setInterpolator(
                    dimAlpha == FULL_ALPHA ? mDecelerateInterpolator : mAccelerateInterpolator);
            mDimAnimator.start();
        }
    }

    /**
     * Returns the current background color.
     */
    @ColorInt
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

        ChangeBackgroundRunnable(Drawable drawable) {
            mDrawable = drawable;
        }

        @Override
        public void run() {
            runTask();
            mChangeRunnable = null;
        }

        private void runTask() {
            if (mLayerDrawable == null) {
                if (DEBUG) Log.v(TAG, "runTask while released - should not happen");
                return;
            }

            if (sameDrawable(mDrawable, mBackgroundDrawable)) {
                if (DEBUG) Log.v(TAG, "new drawable same as current");
                return;
            }

            releaseBackgroundBitmap();

            DrawableWrapper imageInWrapper = getImageInWrapper();
            if (imageInWrapper != null) {
                if (DEBUG) Log.v(TAG, "moving image in to image out");
                // Order is important! Setting a drawable "removes" the
                // previous one from the view
                mLayerDrawable.clearDrawable(R.id.background_imagein, mContext);
                mLayerDrawable.updateDrawable(R.id.background_imageout,
                        imageInWrapper.getDrawable());
            }

            setBackgroundDrawable(mDrawable);
            applyBackgroundChanges();
        }
    }

    private static Drawable createEmptyDrawable(Context context) {
        Bitmap bitmap = null;
        return new BitmapDrawable(context.getResources(), bitmap);
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
