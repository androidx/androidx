/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.support.v17.leanback.graphics;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;

/**
 * Subclass of {@link RegionDrawable} used to draw a solid color.
 */
public class ColorRegionDrawable extends RegionDrawable {
    private int mColor;
    private int mUseColor;

    public ColorRegionDrawable(int color) {
        this.mColor = color;
        mPaint.setColor(mColor);
    }

    /**
     * Sets the color attribute.
     */
    public void setColor(int color) {
        this.mColor = color;
    }

    /**
     * Return the current color.
     */
    public int getColor() {
        return this.mColor;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRect(getBounds(), mPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        alpha += alpha >> 7;   // make it 0..256
        final int baseAlpha = mColor >>> 24;
        final int useAlpha = baseAlpha * alpha >> 8;
        mUseColor = (mColor << 8 >>> 8) | (useAlpha << 24);
        mPaint.setColor(mUseColor);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        if (mPaint.getColorFilter() != null) {
            return PixelFormat.TRANSLUCENT;
        }

        switch (mUseColor >>> 24) {
            case 255:
                return PixelFormat.OPAQUE;
            case 0:
                return PixelFormat.TRANSPARENT;
        }
        return PixelFormat.TRANSLUCENT;
    }
}
