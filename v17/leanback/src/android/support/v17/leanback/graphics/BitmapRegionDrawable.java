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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;

/**
 * Subclass of {@link RegionDrawable} that can be used to draw a bitmap into a region. Bitmap
 * will be scaled to fit the full width of the region and will be aligned to the top left corner.
 * Any region outside the bounds will be clipped during {@link #draw(Canvas)} call. Top
 * position of the bitmap can be controlled by {@link #setVerticalOffset(int)} call.
 */
public class BitmapRegionDrawable extends RegionDrawable {
    private final Rect mDest = new Rect();
    private Bitmap mBitmap;
    private Rect mSource;
    protected int mOffset;

    /**
     * Constructor.
     *
     * @param bitmap Bitmap to be drawn in the region.
     * @param source Rectangle used to extract a rectangular region of the provided bitmap.
     *               When source is null, we use the full bitmap.
     */
    public BitmapRegionDrawable(Bitmap bitmap, Rect source) {
        this.mBitmap = bitmap;
        this.mSource = validateSource(source);
    }

    /**
     * Sets the bitmap.
     */
    public void setBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
    }

    /**
     * Returns the bitmap.
     */
    public Bitmap getBitmap() {
        return mBitmap;
    }

    /**
     * Sets the {@link Rect} used for extracting the bitmap.
     */
    public void setSource(Rect source) {
        this.mSource = validateSource(source);
    }

    /**
     * Returns the {@link Rect} used for extracting the bitmap.
     */
    public Rect getSource() {
        return mSource;
    }

    /**
     * Sets the vertical offset which will be used for drawing the bitmap. The bitmap drawing
     * will start the provided vertical offset.
     */
    public void setVerticalOffset(int offset) {
        this.mOffset = offset;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        mDest.left = 0;
        mDest.top = mOffset;
        mDest.right = bounds.width();
        float scale = (float) bounds.width() / mSource.width();
        mDest.bottom = mDest.top + (int) (mSource.height() * scale);
        canvas.clipRect(bounds);
        canvas.drawBitmap(mBitmap, mSource, mDest, mPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        final int oldAlpha = mPaint.getAlpha();
        if (alpha != oldAlpha) {
            mPaint.setAlpha(alpha);
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return (mBitmap == null || mBitmap.hasAlpha() || mPaint.getAlpha() < 255) ?
                PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;
    }


    private Rect validateSource(Rect source) {
        if (source == null) {
            return new Rect(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        }
        return source;
    }
}
