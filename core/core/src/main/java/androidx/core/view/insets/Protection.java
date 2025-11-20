/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.core.view.insets;

import android.animation.ValueAnimator;
import android.graphics.drawable.Drawable;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

import androidx.annotation.FloatRange;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsCompat.Side.InsetsSide;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An abstract class which describes a layer to be placed on the content of a window and underneath
 * the system bar (which has a transparent background) to ensure the readability of the foreground
 * elements (e.g., text, icons, ..., etc) of the system bar.
 *
 * <p>Concrete derived classes would describe how the protection should be drawn by supplying the
 * {@link Drawable}.
 *
 * <p>The object of this class is stateful, and can only be used by one {@link ProtectionLayout} at
 * a time.
 *
 * @see ColorProtection
 * @see GradientProtection
 * @see ProtectionLayout
 */
public abstract class Protection {

    private static final Interpolator DEFAULT_INTERPOLATOR_MOVE_IN =
            new PathInterpolator(0f, 0f, 0f, 1f);
    private static final Interpolator DEFAULT_INTERPOLATOR_MOVE_OUT =
            new PathInterpolator(0.6f, 0f, 1f, 1f);
    private static final Interpolator DEFAULT_INTERPOLATOR_FADE_IN =
            new PathInterpolator(0f, 0f, 0.2f, 1f);
    private static final Interpolator DEFAULT_INTERPOLATOR_FADE_OUT =
            new PathInterpolator(0.4f, 0f, 1f, 1f);

    // In milliseconds
    private static final long DEFAULT_DURATION_IN = 333;
    private static final long DEFAULT_DURATION_OUT = 166;

    @InsetsSide
    private final int mSide;

    private final Attributes mAttributes = new Attributes();
    private Insets mInsets = Insets.NONE;
    private Insets mInsetsIgnoringVisibility = Insets.NONE;
    private float mSystemAlpha = 1f;
    private float mUserAlpha = 1f;
    private float mSystemInsetAmount = 1f;
    private float mUserInsetAmount = 1f;
    private Object mController = null;

    // These animators are driven by the explicit calls to {@link #animateAlpha()} or
    // {@link #animateInsetsAmount()}, not by the system bar animation.
    private ValueAnimator mUserAlphaAnimator = null;
    private ValueAnimator mUserInsetAmountAnimator = null;

    /**
     * Creates an instance associated with a {@link WindowInsetsCompat.Side}.
     *
     * @param side the given {@link WindowInsetsCompat.Side}.
     * @throws IllegalArgumentException if the given side is not one of the four sides.
     */
    public Protection(@InsetsSide int side) {
        switch (side) {
            case WindowInsetsCompat.Side.LEFT:
            case WindowInsetsCompat.Side.TOP:
            case WindowInsetsCompat.Side.RIGHT:
            case WindowInsetsCompat.Side.BOTTOM:
                break;
            default:
                throw new IllegalArgumentException("Unexpected side: " + side);
        }
        mSide = side;
    }

    /**
     * Gets the side of this protection.
     *
     * @return the side this protection is associated with.
     */
    @InsetsSide
    public int getSide() {
        return mSide;
    }

    /**
     * Gets the attributes of this protection.
     *
     * @return the attributes this protection is associated with.
     */
    @NonNull
    Attributes getAttributes() {
        return mAttributes;
    }

    /**
     * Returns the expected thickness of the protection.
     *
     * <p>Derived classes can override this class to specify a different thickness.
     *
     * @param inset the actual thickness of the intersection between this window and system bars at
     *              the {@link #mSide}.
     * @return the expected thickness of the side.
     */
    int getThickness(int inset) {
        return inset;
    }

    /**
     * Indicates if this protection excludes adjacent protections with lower z-orders from drawing
     * into the sharing corners.
     *
     * <p>Derived classes can override this class to specify whether to occupy the adjacent corners.
     * <p>If this returns {@code true}, the protection will
     * <ol>
     * <li> get a higher z-order than the ones which returns {@code false}, and
     * <li> prevent adjacent protections with lower z-orders from drawing into the sharing corners.
     * </ol>
     * If this returns {@code false}, the protection can still draw into a sharing corner if no one
     * occupies it.
     *
     * @return {@code true}, if the protection occupies the corners; {@code false}, otherwise.
     */
    boolean occupiesCorners() {
        return false;
    }

    Insets dispatchInsets(Insets insets, Insets insetsIgnoringVisibility, Insets consumed) {
        mInsets = insets;
        mInsetsIgnoringVisibility = insetsIgnoringVisibility;
        mAttributes.setMargin(consumed);
        return updateLayout();
    }

    @NonNull Insets updateLayout() {
        Insets consumed = Insets.NONE;
        final int inset;
        switch (mSide) {
            case WindowInsetsCompat.Side.LEFT:
                inset = mInsets.left;
                mAttributes.setWidth(getThickness(mInsetsIgnoringVisibility.left));
                if (occupiesCorners()) {
                    consumed = Insets.of(getThickness(inset), 0, 0, 0);
                }
                break;
            case WindowInsetsCompat.Side.TOP:
                inset = mInsets.top;
                mAttributes.setHeight(getThickness(mInsetsIgnoringVisibility.top));
                if (occupiesCorners()) {
                    consumed = Insets.of(0, getThickness(inset), 0, 0);
                }
                break;
            case WindowInsetsCompat.Side.RIGHT:
                inset = mInsets.right;
                mAttributes.setWidth(getThickness(mInsetsIgnoringVisibility.right));
                if (occupiesCorners()) {
                    consumed = Insets.of(0, 0, getThickness(inset), 0);
                }
                break;
            case WindowInsetsCompat.Side.BOTTOM:
                inset = mInsets.bottom;
                mAttributes.setHeight(getThickness(mInsetsIgnoringVisibility.bottom));
                if (occupiesCorners()) {
                    consumed = Insets.of(0, 0, 0, getThickness(inset));
                }
                break;
            default:
                inset = 0;
        }
        setSystemVisible(inset > 0);
        setSystemAlpha(inset > 0 ? 1f : 0);
        setSystemInsetAmount(inset > 0 ? 1f : 0);
        return consumed;
    }

    void dispatchColorHint(int color) {
    }

    Object getController() {
        return mController;
    }

    void setController(Object controller) {
        mController = controller;
    }

    void setSystemVisible(boolean visible) {
        mAttributes.setVisible(visible);
    }

    void setSystemAlpha(@FloatRange(from = 0.0, to = 1.0) float alpha) {
        mSystemAlpha = alpha;
        updateAlpha();
    }

    /**
     * Sets the opacity of the protection to a value from 0 to 1, where 0 means the protection is
     * completely transparent and 1 means the protection is completely opaque.
     *
     * @param alpha The opacity of the protection.
     * @throws IllegalArgumentException if the given alpha is not in a range of [0, 1].
     */
    public void setAlpha(@FloatRange(from = 0.0, to = 1.0) float alpha) {
        if (alpha < 0 || alpha > 1f) {
            throw new IllegalArgumentException("Alpha must in a range of [0, 1]. Got: " + alpha);
        }
        cancelUserAlphaAnimation();
        setAlphaInternal(alpha);
    }

    // Sets the alpha without cancelling the user animation.
    private void setAlphaInternal(float alpha) {
        mUserAlpha = alpha;
        updateAlpha();
    }

    /**
     * Gets the opacity of the protection. This is a value from 0 to 1, where 0 means the protection
     * is completely transparent and 1 means the protection is completely opaque.
     *
     * @return The opacity of the protection.
     */
    @FloatRange(from = 0.0, to = 1.0)
    public float getAlpha() {
        return mUserAlpha;
    }

    private void updateAlpha() {
        mAttributes.setAlpha(mSystemAlpha * mUserAlpha);
    }

    private void cancelUserAlphaAnimation() {
        if (mUserAlphaAnimator != null) {
            mUserAlphaAnimator.cancel();
            mUserAlphaAnimator = null;
        }
    }

    /**
     * Animates the alpha from the current value to the specified one.
     *
     * <p>Calling {@link #setAlpha(float)} during the animation will cancel the existing alpha
     * animation.
     *
     * @param toAlpha The alpha that will be animated to. The range is [0, 1].
     */
    public void animateAlpha(float toAlpha) {
        cancelUserAlphaAnimation();
        if (toAlpha == mUserAlpha) {
            return;
        }
        mUserAlphaAnimator = ValueAnimator.ofFloat(mUserAlpha, toAlpha);
        if (mUserAlpha < toAlpha) {
            mUserAlphaAnimator.setDuration(DEFAULT_DURATION_IN);
            mUserAlphaAnimator.setInterpolator(DEFAULT_INTERPOLATOR_FADE_IN);
        } else {
            mUserAlphaAnimator.setDuration(DEFAULT_DURATION_OUT);
            mUserAlphaAnimator.setInterpolator(DEFAULT_INTERPOLATOR_FADE_OUT);
        }
        mUserAlphaAnimator.addUpdateListener(
                animation -> setAlphaInternal((float) animation.getAnimatedValue()));
        mUserAlphaAnimator.start();
    }

    void setSystemInsetAmount(@FloatRange(from = 0.0, to = 1.0) float insetAmount) {
        mSystemInsetAmount = insetAmount;
        updateInsetAmount();
    }

    /**
     * Sets the depth of the protection to a value from 0 to 1, where 0 means the protection is
     * completely outside the window and 1 means the protection is completely inside the window.
     *
     * @param insetAmount The depth of the protection.
     * @throws IllegalArgumentException if the given inset amount is not in a range of [0, 1].
     */
    public void setInsetAmount(@FloatRange(from = 0.0, to = 1.0) float insetAmount) {
        if (insetAmount < 0 || insetAmount > 1f) {
            throw new IllegalArgumentException("Inset amount must in a range of [0, 1]. Got: "
                    + insetAmount);
        }
        cancelUserInsetsAmountAnimation();
        setInsetAmountInternal(insetAmount);
    }

    // Sets the insets amount without cancelling the user animation.
    private void setInsetAmountInternal(float insetAmount) {
        mUserInsetAmount = insetAmount;
        updateInsetAmount();
    }

    /**
     * Gets the depth of the protection. This is a value from 0 to 1, where 0 means the protection
     * completely inside the window and 1 means the protection is completely outside the window.
     *
     * @return The depth of the protection.
     */
    public float getInsetAmount() {
        return mUserInsetAmount;
    }

    private void updateInsetAmount() {
        final float finalInsetAmount = mUserInsetAmount * mSystemInsetAmount;
        switch (mSide) {
            case WindowInsetsCompat.Side.LEFT:
                mAttributes.setTranslationX(-(1f - finalInsetAmount) * mAttributes.mWidth);
                break;
            case WindowInsetsCompat.Side.TOP:
                mAttributes.setTranslationY(-(1f - finalInsetAmount) * mAttributes.mHeight);
                break;
            case WindowInsetsCompat.Side.RIGHT:
                mAttributes.setTranslationX((1f - finalInsetAmount) * mAttributes.mWidth);
                break;
            case WindowInsetsCompat.Side.BOTTOM:
                mAttributes.setTranslationY((1f - finalInsetAmount) * mAttributes.mHeight);
                break;
        }
    }

    private void cancelUserInsetsAmountAnimation() {
        if (mUserInsetAmountAnimator != null) {
            mUserInsetAmountAnimator.cancel();
            mUserInsetAmountAnimator = null;
        }
    }

    /**
     * Animates the insets amount from the current value to the specified one.
     *
     * <p>Calling {@link #setInsetAmount(float)} during the animation will cancel the existing
     * animation of insets amount.
     *
     * @param toInsetsAmount The insets amount that will be animated to. The range is [0, 1].
     */
    public void animateInsetsAmount(float toInsetsAmount) {
        cancelUserInsetsAmountAnimation();
        if (toInsetsAmount == mUserInsetAmount) {
            return;
        }
        mUserInsetAmountAnimator = ValueAnimator.ofFloat(mUserInsetAmount, toInsetsAmount);
        if (mUserInsetAmount < toInsetsAmount) {
            mUserInsetAmountAnimator.setDuration(DEFAULT_DURATION_IN);
            mUserInsetAmountAnimator.setInterpolator(DEFAULT_INTERPOLATOR_MOVE_IN);
        } else {
            mUserInsetAmountAnimator.setDuration(DEFAULT_DURATION_OUT);
            mUserInsetAmountAnimator.setInterpolator(DEFAULT_INTERPOLATOR_MOVE_OUT);
        }
        mUserInsetAmountAnimator.addUpdateListener(
                animation -> setAlphaInternal((float) animation.getAnimatedValue()));
        mUserInsetAmountAnimator.start();
    }

    void setDrawable(@Nullable Drawable drawable) {
        mAttributes.setDrawable(drawable);
    }

    /**
     * Describes the final appearance of the protection.
     */
    static class Attributes {

        private static final int UNSPECIFIED = -1;

        private int mWidth = UNSPECIFIED;
        private int mHeight = UNSPECIFIED;
        private Insets mMargin = Insets.NONE;
        private boolean mVisible = false;
        private Drawable mDrawable = null;
        private float mTranslationX = 0;
        private float mTranslationY = 0;
        private float mAlpha = 1f;
        private @Nullable Callback mCallback;

        /**
         * Returns the width of the protection in pixels.
         *
         * @return the width of the protection in pixels.
         */
        int getWidth() {
            return mWidth;
        }

        /**
         * Returns the height of the protection in pixels.
         *
         * @return the height of the protection in pixels.
         */
        int getHeight() {
            return mHeight;
        }

        /**
         * Returns the margin of the protection in pixels.
         *
         * @return the margin of the protection in pixels.
         */
        @NonNull Insets getMargin() {
            return mMargin;
        }

        /**
         * Returns {@code true} if the protection is visible. {@code false} otherwise.
         *
         * @return {@code true} if the protection is visible. {@code false} otherwise.
         */
        boolean isVisible() {
            return mVisible;
        }

        /**
         * Returns the {@link Drawable} that fills the protection.
         *
         * @return the {@link Drawable} that fills the protection.
         */
        @Nullable Drawable getDrawable() {
            return mDrawable;
        }

        /**
         * Returns the translation of the protection along the x-axis.
         *
         * @return the translation of the protection along the x-axis.
         */
        float getTranslationX() {
            return mTranslationX;
        }

        /**
         * Returns the translation of the protection along the y-axis in pixels.
         *
         * @return the translation of the protection along the y-axis in pixels.
         */
        float getTranslationY() {
            return mTranslationY;
        }

        /**
         * Returns the transparency of the protection.
         *
         * @return the transparency of the protection.
         */
        float getAlpha() {
            return mAlpha;
        }

        private void setWidth(int width) {
            if (mWidth != width) {
                mWidth = width;
                if (mCallback != null) {
                    mCallback.onWidthChanged(width);
                }
            }
        }

        private void setHeight(int height) {
            if (mHeight != height) {
                mHeight = height;
                if (mCallback != null) {
                    mCallback.onHeightChanged(height);
                }
            }
        }

        private void setMargin(Insets margin) {
            if (!mMargin.equals(margin)) {
                mMargin = margin;
                if (mCallback != null) {
                    mCallback.onMarginChanged(margin);
                }
            }
        }

        private void setVisible(boolean visible) {
            if (mVisible != visible) {
                mVisible = visible;
                if (mCallback != null) {
                    mCallback.onVisibilityChanged(visible);
                }
            }
        }

        private void setDrawable(@Nullable Drawable drawable) {
            mDrawable = drawable;
            if (mCallback != null) {
                mCallback.onDrawableChanged(drawable);
            }
        }

        private void setTranslationX(float translationX) {
            if (mTranslationX != translationX) {
                mTranslationX = translationX;
                if (mCallback != null) {
                    mCallback.onTranslationXChanged(translationX);
                }
            }
        }

        private void setTranslationY(float translationY) {
            if (mTranslationY != translationY) {
                mTranslationY = translationY;
                if (mCallback != null) {
                    mCallback.onTranslationYChanged(translationY);
                }
            }
        }

        private void setAlpha(float alpha) {
            if (mAlpha != alpha) {
                mAlpha = alpha;
                if (mCallback != null) {
                    mCallback.onAlphaChanged(alpha);
                }
            }
        }

        /**
         * Callbacks for monitoring the attribute change.
         */
        interface Callback {

            /** Called when the width is changed */
            default void onWidthChanged(int width) {}

            /** Called when the height is changed */
            default void onHeightChanged(int height) {}

            /** Called when the margin is changed */
            default void onMarginChanged(@NonNull Insets margin) {}

            /** Called when the visibility is changed */
            default void onVisibilityChanged(boolean visible) {}

            /** Called when the drawable is changed */
            default void onDrawableChanged(@NonNull Drawable drawable) {}

            /** Called when the translation along the x-axis is changed */
            default void onTranslationXChanged(float translationX) {}

            /** Called when the translation along the y-axis is changed */
            default void onTranslationYChanged(float translationY) {}

            /** Called when the transparency is changed */
            default void onAlphaChanged(float alpha) {}
        }

        /**
         * Sets a {@link Callback} to monitor attribute change.
         *
         * @param callback the given callback.
         */
        void setCallback(@Nullable Callback callback) {
            if (mCallback != null && callback != null) {
                throw new IllegalStateException("Trying to overwrite the existing callback."
                        + " Did you send one protection to multiple ProtectionLayouts?");
            }
            mCallback = callback;
        }
    }
}
