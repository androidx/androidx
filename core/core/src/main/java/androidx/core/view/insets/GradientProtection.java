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

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.animation.PathInterpolator;

import androidx.annotation.ColorInt;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsCompat.Side.InsetsSide;

/**
 * A type of protection which draws the gradient color.
 */
public class GradientProtection extends Protection {

    private static final float[] ALPHAS = new float[100];
    static {
        final PathInterpolator interpolator = new PathInterpolator(0.42f, 0f, 0.58f, 1f);
        final int steps = ALPHAS.length - 1;
        for (int i = steps; i >= 0; i--) {
            ALPHAS[i] = interpolator.getInterpolation((steps - i)  / (float) steps);
        }
    }

    private final GradientDrawable mDrawable = new GradientDrawable();
    private final int[] mColors = new int[ALPHAS.length];
    private boolean mHasColor;

    @ColorInt
    private int mColor = Color.TRANSPARENT;

    private float mScale = 1.2f;

    /**
     * Creates an instance associated with a {@link WindowInsetsCompat.Side}.
     *
     * @param side the given {@link WindowInsetsCompat.Side}.
     */
    public GradientProtection(@InsetsSide int side) {
        super(side);
        switch (side) {
            case WindowInsetsCompat.Side.LEFT:
                mDrawable.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
                break;
            case WindowInsetsCompat.Side.TOP:
                mDrawable.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
                break;
            case WindowInsetsCompat.Side.RIGHT:
                mDrawable.setOrientation(GradientDrawable.Orientation.RIGHT_LEFT);
                break;
            case WindowInsetsCompat.Side.BOTTOM:
                mDrawable.setOrientation(GradientDrawable.Orientation.BOTTOM_TOP);
                break;
        }
    }

    /**
     * Creates an instance associated with a {@link WindowInsetsCompat.Side}.
     *
     * @param side the given {@link WindowInsetsCompat.Side}.
     * @param color The color to draw.
     */
    public GradientProtection(@InsetsSide int side, @ColorInt int color) {
        this(side);
        setColor(color);
    }

    @Override
    void dispatchColorHint(@ColorInt int color) {
        if (!mHasColor) {
            setColorInner(color);
        }
    }

    private void setColorInner(@ColorInt int color) {
        if (mColor != color) {
            mColor = color;
            toColors(mColor, mColors);
            mDrawable.setColors(mColors);
            setDrawable(mDrawable);
        }
    }

    /**
     * Sets the color to the protection. The pixels farther away from the edge get more transparent.
     *
     * @param color The color to draw.
     */
    public void setColor(@ColorInt int color) {
        mHasColor = true;
        setColorInner(color);
    }

    /**
     * Gets the color associated with this protection.
     *
     * @return The color associated with this protection.
     */
    @ColorInt
    public int getColor() {
        return mColor;
    }

    private static void toColors(int color, int[] colors) {
        for (int i = colors.length - 1; i >= 0; i--) {
            colors[i] = Color.argb((int) (ALPHAS[i] * Color.alpha(color)),
                    Color.red(color),
                    Color.green(color),
                    Color.blue(color));
        }
    }

    @Override
    int getThickness(int inset) {
        return (int) (mScale * inset);
    }

    /**
     * Sets the scale of the thickness to the protection.
     *
     * @param scale The scale of the thickness.
     */
    public void setScale(float scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("Scale must not be negative.");
        }
        mScale = scale;
        updateLayout();
    }

    /**
     * Gets the scale of the thickness of the protection.
     *
     * @return The scale of the thickness.
     */
    public float getScale() {
        return mScale;
    }
}
