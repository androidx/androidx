/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.widget;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

/**
 * A helper class to handle transition of swiping to dismiss and dismiss animation.
 */
class SwipeDismissTransitionHelper {

    private static final String TAG = "SwipeDismissTransitionHelper";
    private static final float SCALE_MIN = 0.7f;
    private static final float SCALE_MAX = 1.0f;
    public static final float SCRIM_BACKGROUND_MAX = 0.5f;
    private static final float DIM_FOREGROUND_PROGRESS_FACTOR = 2.0f;
    private static final float DIM_FOREGROUND_MIN = 0.3f;
    private static final int VELOCITY_UNIT = 1000;
    // Spring properties
    private static final float SPRING_STIFFNESS = 600f;
    private static final float SPRING_DAMPING_RATIO = SpringForce.DAMPING_RATIO_NO_BOUNCY;
    private static final float SPRING_MIN_VISIBLE_CHANGE = 0.5f;
    private static final int SPRING_ANIMATION_PROGRESS_FINISH_THRESHOLD_PX = 5;
    private final DismissibleFrameLayout mLayout;

    private final int mScreenWidth;
    private final SparseArray<ColorFilter> mDimmingColorFilterCache = new SparseArray<>();
    private final Drawable mScrimBackground;
    private final boolean mIsScreenRound;
    private final Paint mCompositingPaint = new Paint();

    private VelocityTracker mVelocityTracker;
    private boolean mStarted;
    private int mOriginalViewWidth;
    private float mTranslationX;
    private float mScale;
    private float mProgress;
    private float mDimming;
    private SpringAnimation mDismissalSpring;
    private SpringAnimation mRecoverySpring;
    // Variable to restore the parent's background which is added below mScrimBackground.
    private Drawable mPrevParentBackground = null;

    SwipeDismissTransitionHelper(@NonNull Context context,
            @NonNull DismissibleFrameLayout layout) {
        mLayout = layout;
        mIsScreenRound = layout.getResources().getConfiguration().isScreenRound();
        mScreenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        mScrimBackground = generateScrimBackgroundDrawable(mScreenWidth,
                Resources.getSystem().getDisplayMetrics().heightPixels);
    }

    private static void clipOutline(@NonNull View view, boolean useRoundShape) {
        view.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                if (useRoundShape) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                } else {
                    outline.setRect(0, 0, view.getWidth(), view.getHeight());
                }
                outline.setAlpha(0);
            }
        });
        view.setClipToOutline(true);
    }


    private static float lerp(float min, float max, float value) {
        return min + (max - min) * value;
    }

    private static float clamp(float min, float max, float value) {
        return max(min, min(max, value));
    }

    private static float lerpInv(float min, float max, float value) {
        return min != max ? ((value - min) / (max - min)) : 0.0f;
    }

    private ColorFilter createDimmingColorFilter(float level) {
        level = clamp(0, 1, level);
        int alpha = (int) (0xFF * level);
        int color = Color.argb(alpha, 0, 0, 0);
        ColorFilter colorFilter = mDimmingColorFilterCache.get(alpha);
        if (colorFilter != null) {
            return colorFilter;
        }
        colorFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        mDimmingColorFilterCache.put(alpha, colorFilter);
        return colorFilter;
    }

    private SpringAnimation createSpringAnimation(float startValue,
            float finalValue,
            float startVelocity,
            DynamicAnimation.OnAnimationUpdateListener onUpdateListener,
            DynamicAnimation.OnAnimationEndListener onEndListener) {
        SpringAnimation animation = new SpringAnimation(new FloatValueHolder());
        animation.setStartValue(startValue);
        animation.setMinimumVisibleChange(SPRING_MIN_VISIBLE_CHANGE);
        SpringForce spring = new SpringForce();
        spring.setFinalPosition(finalValue);
        spring.setDampingRatio(SPRING_DAMPING_RATIO);
        spring.setStiffness(SPRING_STIFFNESS);
        animation.setMinValue(0.0f);
        animation.setMaxValue(mScreenWidth);
        animation.setStartVelocity(startVelocity);
        animation.setSpring(spring);
        animation.addUpdateListener(onUpdateListener);
        animation.addEndListener(onEndListener);
        animation.start();
        return animation;
    }

    /**
     * Updates the swipe progress
     *
     * @param deltaX The X delta of gesture
     * @param ev     The motion event
     */
    void onSwipeProgressChanged(float deltaX, @NonNull MotionEvent ev) {
        if (!mStarted) {
            initializeTransition();
        }

        mVelocityTracker.addMovement(ev);
        mOriginalViewWidth = mLayout.getWidth();
        // For swiping, mProgress is directly manipulated
        // mProgress = 0 (no swipe) - 0.5 (swiped to mid screen) - 1 (swipe to right of screen)
        mProgress = deltaX / mOriginalViewWidth;
        // Solve for other variables
        // Scale = lerp 100% -> 70% when swiping from left edge to right edge
        mScale = lerp(SCALE_MAX, SCALE_MIN, mProgress);
        // Translation: make sure the right edge of mOriginalView touches right edge of screen
        mTranslationX = max(0f, 1 - mScale) * mLayout.getWidth() / 2.0f;
        mDimming = Math.min(DIM_FOREGROUND_MIN, mProgress / DIM_FOREGROUND_PROGRESS_FACTOR);

        updateView();
    }

    private void onDismissalRecoveryAnimationProgressChanged(float translationX) {
        mOriginalViewWidth = mLayout.getWidth();
        mTranslationX = translationX;

        mScale = 1 - mTranslationX * 2 / mOriginalViewWidth;
        // Clamp mScale so that we can solve for mProgress
        mScale = Math.max(SCALE_MIN, Math.min(mScale, SCALE_MAX));
        float nextProgress = lerpInv(SCALE_MAX, SCALE_MIN, mScale);
        if (nextProgress > mProgress) {
            mProgress = nextProgress;
        }
        mDimming = Math.min(DIM_FOREGROUND_MIN, mProgress / DIM_FOREGROUND_PROGRESS_FACTOR);
        updateView();
    }

    private void updateView() {
        mLayout.setScaleX(mScale);
        mLayout.setScaleY(mScale);
        mLayout.setTranslationX(mTranslationX);
        updateDim();
        updateScrim();
    }

    private void updateDim() {
        mCompositingPaint.setColorFilter(createDimmingColorFilter(mDimming));
        mLayout.setLayerPaint(mCompositingPaint);
    }

    private void updateScrim() {
        float alpha = SCRIM_BACKGROUND_MAX * (1 - mProgress);
        // Scaling alpha between 0 to 255, as Drawable.setAlpha expects it in range [0,255].
        mScrimBackground.setAlpha((int) (alpha * 255));
    }

    private void initializeTransition() {
        mStarted = true;
        ViewGroup originalParentView = getOriginalParentView();

        if (originalParentView == null) {
            return;
        }

        if (mPrevParentBackground == null) {
            mPrevParentBackground = originalParentView.getBackground();
        }

        // Adding scrim over parent background if it exists.
        Drawable parentBackgroundLayers;
        if (mPrevParentBackground != null) {
            parentBackgroundLayers = new LayerDrawable(new Drawable[]{mPrevParentBackground,
                    mScrimBackground});
        } else {
            parentBackgroundLayers = mScrimBackground;
        }
        originalParentView.setBackground(parentBackgroundLayers);

        mCompositingPaint.setColorFilter(null);
        mLayout.setLayerType(View.LAYER_TYPE_HARDWARE, mCompositingPaint);
        clipOutline(mLayout, mIsScreenRound);
    }

    private void resetTranslationAndAlpha() {
        // resetting variables
        mStarted = false;
        mTranslationX = 0;
        mProgress = 0;
        mScale = 1;
        // resetting layout params
        mLayout.setTranslationX(0);
        mLayout.setScaleX(1);
        mLayout.setScaleY(1);
        mLayout.setAlpha(1);
        mScrimBackground.setAlpha(0);

        mCompositingPaint.setColorFilter(null);
        mLayout.setLayerType(View.LAYER_TYPE_NONE, null);
        mLayout.setClipToOutline(false);

        // Restoring previous background
        ViewGroup originalParentView = getOriginalParentView();
        if (originalParentView != null) {
            originalParentView.setBackground(mPrevParentBackground);
        }
        mPrevParentBackground = null;
    }
    private Drawable generateScrimBackgroundDrawable(int width, int height) {
        ShapeDrawable shape = new ShapeDrawable(new RectShape());
        shape.setBounds(0, 0, width, height);
        shape.getPaint().setColor(Color.BLACK);
        return shape;
    }

    /**
     * @return If dismiss or recovery animation is running.
     */
    boolean isAnimating() {
        return (mDismissalSpring != null && mDismissalSpring.isRunning()) || (
                mRecoverySpring != null && mRecoverySpring.isRunning());
    }

    /**
     * Triggers the recovery animation.
     */
    void animateRecovery(@Nullable DismissController.OnDismissListener dismissListener) {
        mVelocityTracker.computeCurrentVelocity(VELOCITY_UNIT);
        mRecoverySpring = createSpringAnimation(mTranslationX, 0, mVelocityTracker.getXVelocity(),
                (animation, value, velocity) -> {
                    float distanceRemaining = Math.max(0, (value - 0));
                    if (distanceRemaining <= SPRING_ANIMATION_PROGRESS_FINISH_THRESHOLD_PX
                            && mRecoverySpring != null) {
                        // Skip last 2% of animation.
                        mRecoverySpring.skipToEnd();
                    }
                    onDismissalRecoveryAnimationProgressChanged(value);
                }, (animation, canceled, value, velocity) -> {

                    resetTranslationAndAlpha();
                    if (dismissListener != null) {
                        dismissListener.onDismissCanceled();
                    }
                });
    }

    /**
     * Triggers the dismiss animation.
     */
    void animateDismissal(@Nullable DismissController.OnDismissListener dismissListener) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.computeCurrentVelocity(VELOCITY_UNIT);
        // Dismissal has started
        if (dismissListener != null) {
            dismissListener.onDismissStarted();
        }

        mDismissalSpring = createSpringAnimation(mTranslationX, mScreenWidth,
                mVelocityTracker.getXVelocity(), (animation, value, velocity) -> {
                    float distanceRemaining = Math.max(0, (mScreenWidth - value));
                    if (distanceRemaining <= SPRING_ANIMATION_PROGRESS_FINISH_THRESHOLD_PX
                            && mDismissalSpring != null) {
                        // Skip last 2% of animation.
                        mDismissalSpring.skipToEnd();
                    }
                    onDismissalRecoveryAnimationProgressChanged(value);
                }, (animation, canceled, value, velocity) -> {
                    resetTranslationAndAlpha();
                    if (dismissListener != null) {
                        dismissListener.onDismissed();
                    }
                });
    }

    private @Nullable ViewGroup getOriginalParentView() {
        if (mLayout.getParent() instanceof ViewGroup) {
            return (ViewGroup) mLayout.getParent();
        }
        return null;
    }

    /**
     * @return The velocity tracker.
     */
    @Nullable
    VelocityTracker getVelocityTracker() {
        return mVelocityTracker;
    }

    /**
     * Obtain velocity tracker.
     */
    void obtainVelocityTracker() {
        mVelocityTracker = VelocityTracker.obtain();
    }

    /**
     * Reset velocity tracker to null.
     */
    void resetVelocityTracker() {
        mVelocityTracker = null;
    }
}
