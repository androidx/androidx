/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.support.wear.widget;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.util.Objects;

/**
 * Maintains and draws a drawable inside rounded rectangular bounds.
 *
 * <p>The drawable set by the {@link #setDrawable(Drawable)} method will be drawn within the rounded
 * bounds specified by {@link #setBounds(Rect)} and {@link #setRadius(int)} when the
 * {@link #draw(Canvas)} method is called.
 *
 * <p>By default, RoundedDrawable will apply padding to the drawable inside to fit the drawable into
 * the rounded rectangle. If clipping is enabled by the {@link #setClipEnabled(boolean)} method, it
 * will clip the drawable to a rounded rectangle instead of resizing it.
 *
 * <p>The {@link #setRadius(int)} method is used to specify the amount of border radius applied to
 * the corners of inner drawable, regardless of whether or not the clipping is enabled, border
 * radius will be applied to prevent overflowing of the drawable from specified rounded rectangular
 * area.
 */
@TargetApi(Build.VERSION_CODES.N)
public class RoundedDrawable extends Drawable {

    @VisibleForTesting
    final Paint mPaint;
    final Paint mBackgroundPaint;

    @Nullable
    private Drawable mDrawable;
    private int mRadius; // Radius applied to corners in pixels
    private boolean mIsClipEnabled;

    // Used to avoid creating new Rect objects every time draw() is called
    private final Rect mTmpBounds = new Rect();
    private final RectF mTmpBoundsF = new RectF();

    public RoundedDrawable() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setAntiAlias(true);
        mBackgroundPaint.setColor(Color.TRANSPARENT);
    }

    /**
     * Sets the drawable to be rendered.
     *
     * @param drawable {@link Drawable} to be rendered
     */
    public void setDrawable(@Nullable Drawable drawable) {
        if (Objects.equals(mDrawable, drawable)) {
            return;
        }
        mDrawable = drawable;
        mPaint.setShader(null); // Clear the shader so it can be reinitialized
        invalidateSelf();
    }

    /**
     * Returns the drawable that will be rendered.
     *
     * @return {@link Drawable} that will be rendered.
     */
    @Nullable
    public Drawable getDrawable() {
        return mDrawable;
    }

    /**
     * Sets the background color of the rounded drawable.
     *
     * @param color an ARGB color
     */
    public void setBackgroundColor(@ColorInt int color) {
        mBackgroundPaint.setColor(color);
        invalidateSelf();
    }

    /**
     * Returns the background color.
     *
     * @return an ARGB color
     */
    @ColorInt
    public int getBackgroundColor() {
        return mBackgroundPaint.getColor();
    }

    /**
     * Sets whether the drawable inside should be clipped or resized to fit the rounded bounds. If
     * the drawable is animated, don't set clipping to {@code true} as clipping on animated
     * drawables is not supported.
     *
     * @param clipEnabled {@code true} if the drawable should be clipped, {@code false} if it
     *                    should be resized.
     */
    public void setClipEnabled(boolean clipEnabled) {
        mIsClipEnabled = clipEnabled;
        if (!clipEnabled) {
            mPaint.setShader(null); // Clear the shader so it's garbage collected
        }
        invalidateSelf();
    }

    /**
     * Returns whether the drawable inside is clipped or resized to fit the rounded bounds.
     *
     * @return {@code true} if the drawable is clipped, {@code false} if it's resized.
     */
    public boolean isClipEnabled() {
        return mIsClipEnabled;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        mTmpBounds.right = bounds.width();
        mTmpBounds.bottom = bounds.height();
        mTmpBoundsF.right = bounds.width();
        mTmpBoundsF.bottom = bounds.height();
        mPaint.setShader(null); // Clear the shader so it can be reinitialized
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (mDrawable == null || bounds.isEmpty()) {
            return;
        }
        canvas.save();
        canvas.translate(bounds.left, bounds.top);
        // mTmpBoundsF is bounds translated to (0,0) and converted to RectF as drawRoundRect
        // requires.
        canvas.drawRoundRect(mTmpBoundsF, (float) mRadius, (float) mRadius,
                mBackgroundPaint);
        if (mIsClipEnabled) {
            // Update the shader if it's not present.
            if (mPaint.getShader() == null) {
                updateBitmapShader();
            }
            // Clip to a rounded rectangle
            canvas.drawRoundRect(mTmpBoundsF, (float) mRadius, (float) mRadius, mPaint);
        } else {
            // Scale to fit the rounded rectangle
            int minEdge = Math.min(bounds.width(), bounds.height());
            int padding = (int) Math.ceil(
                    Math.min(mRadius, minEdge / 2) * (1 - 1 / (float) Math.sqrt(2.0)));
            mTmpBounds.inset(padding, padding);
            mDrawable.setBounds(mTmpBounds);
            mDrawable.draw(canvas);
            mTmpBounds.inset(-padding, -padding);
        }
        canvas.restore();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
        mBackgroundPaint.setAlpha(alpha);
    }

    @Override
    public int getAlpha() {
        return mPaint.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    /**
     * Sets the border radius to be applied when rendering the drawable in pixels.
     *
     * @param radius radius in pixels
     */
    public void setRadius(int radius) {
        mRadius = radius;
    }

    /**
     * Returns the border radius applied when rendering the drawable in pixels.
     *
     * @return radius in pixels
     */
    public int getRadius() {
        return mRadius;
    }

    /**
     * Updates the shader of the paint. To avoid scaling and creation of a BitmapShader every time,
     * this method should be called only if the drawable or the bounds has changed.
     */
    private void updateBitmapShader() {
        if (mDrawable == null) {
            return;
        }
        Rect bounds = getBounds();
        if (!bounds.isEmpty()) {
            Bitmap bitmap = drawableToBitmap(mDrawable, bounds.width(), bounds.height());

            Shader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            mPaint.setShader(shader);
        }
    }

    /** Converts a drawable to a bitmap of specified width and height. */
    private Bitmap drawableToBitmap(Drawable drawable, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }
}
