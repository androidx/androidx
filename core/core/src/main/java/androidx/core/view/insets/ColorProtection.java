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
import android.graphics.drawable.ColorDrawable;

import androidx.annotation.ColorInt;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsCompat.Side.InsetsSide;

/**
 * A type of protection which draws the plain color.
 */
public class ColorProtection extends Protection {

    private final ColorDrawable mDrawable = new ColorDrawable();
    private boolean mHasColor;

    @ColorInt
    private int mColor = Color.TRANSPARENT;

    /**
     * Creates an instance associated with a {@link WindowInsetsCompat.Side}.
     *
     * @param side the given {@link WindowInsetsCompat.Side}.
     */
    public ColorProtection(@InsetsSide int side) {
        super(side);
    }

    /**
     * Creates an instance associated with a {@link WindowInsetsCompat.Side}.
     *
     * @param side the given {@link WindowInsetsCompat.Side}.
     * @param color The color to draw.
     */
    public ColorProtection(@InsetsSide int side, @ColorInt int color) {
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
            mDrawable.setColor(color);
            setDrawable(mDrawable);
        }
    }

    /**
     * Sets the color to the protection.
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

    @Override
    boolean occupiesCorners() {
        return true;
    }
}
