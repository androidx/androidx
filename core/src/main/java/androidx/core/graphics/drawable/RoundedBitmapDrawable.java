/*
 * Copyright (C) 2014 The Android Open Source Project
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
package androidx.core.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A Drawable that wraps a bitmap and can be drawn with rounded corners. You can create a
 * RoundedBitmapDrawable from a file path, an input stream, or from a
 * {@link android.graphics.Bitmap} object.
 * <p>
 * Also see the {@link android.graphics.Bitmap} class, which handles the management and
 * transformation of raw bitmap graphics, and should be used when drawing to a
 * {@link android.graphics.Canvas}.
 * </p>
 */
public abstract class RoundedBitmapDrawable extends Drawable {
    private static final int DEFAULT_PAINT_FLAGS =
            Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG;
    final Bitmap mBitmap;
    private int mTargetDensity = DisplayMetrics.DENSITY_DEFAULT;
    private int mGravity = Gravity.FILL;
    private final Paint mPaint = new Paint(DEFAULT_PAINT_FLAGS);
    private final BitmapShader mBitmapShader;
    private final Matrix mShaderMatrix = new Matrix();
    private float mCornerRadius;

    final Rect mDstRect = new Rect();   // Gravity.apply() sets this
    private final RectF mDstRectF = new RectF();

    private boolean mApplyGravity = true;
    private boolean mIsCircular;

    // These are scaled to match the target density.
    private int mBitmapWidth;
    private int mBitmapHeight;

    /**
     * Returns the paint used to render this drawable.
     */
    @NonNull
    public final Paint getPaint() {
        return mPaint;
    }

    /**
     * Returns the bitmap used by this drawable to render. May be null.
     */
    @Nullable
    public final Bitmap getBitmap() {
        return mBitmap;
    }

    private void computeBitmapSize() {
        mBitmapWidth = mBitmap.getScaledWidth(mTargetDensity);
        mBitmapHeight = mBitmap.getScaledHeight(mTargetDensity);
    }

    /**
     * Set the density scale at which this drawable will be rendered. This
     * method assumes the drawable will be rendered at the same density as the
     * specified canvas.
     *
     * @param canvas The Canvas from which the density scale must be obtained.
     *
     * @see android.graphics.Bitmap#setDensity(int)
     * @see android.graphics.Bitmap#getDensity()
     */
    public void setTargetDensity(@NonNull Canvas canvas) {
        setTargetDensity(canvas.getDensity());
    }

    /**
     * Set the density scale at which this drawable will be rendered.
     *
     * @param metrics The DisplayMetrics indicating the density scale for this drawable.
     *
     * @see android.graphics.Bitmap#setDensity(int)
     * @see android.graphics.Bitmap#getDensity()
     */
    public void setTargetDensity(@NonNull DisplayMetrics metrics) {
        setTargetDensity(metrics.densityDpi);
    }

    /**
     * Set the density at which this drawable will be rendered.
     *
     * @param density The density scale for this drawable.
     *
     * @see android.graphics.Bitmap#setDensity(int)
     * @see android.graphics.Bitmap#getDensity()
     */
    public void setTargetDensity(int density) {
        if (mTargetDensity != density) {
            mTargetDensity = density == 0 ? DisplayMetrics.DENSITY_DEFAULT : density;
            if (mBitmap != null) {
                computeBitmapSize();
            }
            invalidateSelf();
        }
    }

    /**
     * Get the gravity used to position/stretch the bitmap within its bounds.
     *
     * @return the gravity applied to the bitmap
     *
     * @see android.view.Gravity
     */
    public int getGravity() {
        return mGravity;
    }

    /**
     * Set the gravity used to position/stretch the bitmap within its bounds.
     *
     * @param gravity the gravity
     *
     * @see android.view.Gravity
     */
    public void setGravity(int gravity) {
        if (mGravity != gravity) {
            mGravity = gravity;
            mApplyGravity = true;
            invalidateSelf();
        }
    }

    /**
     * Enables or disables the mipmap hint for this drawable's bitmap.
     * See {@link Bitmap#setHasMipMap(boolean)} for more information.
     *
     * If the bitmap is null, or the current API version does not support setting a mipmap hint,
     * calling this method has no effect.
     *
     * @param mipMap True if the bitmap should use mipmaps, false otherwise.
     *
     * @see #hasMipMap()
     */
    public void setMipMap(boolean mipMap) {
        throw new UnsupportedOperationException(); // must be overridden in subclasses
    }

    /**
     * Indicates whether the mipmap hint is enabled on this drawable's bitmap.
     *
     * @return True if the mipmap hint is set, false otherwise. If the bitmap
     *         is null, this method always returns false.
     *
     * @see #setMipMap(boolean)
     */
    public boolean hasMipMap() {
        throw new UnsupportedOperationException(); // must be overridden in subclasses
    }

    /**
     * Enables or disables anti-aliasing for this drawable. Anti-aliasing affects
     * the edges of the bitmap only so it applies only when the drawable is rotated.
     *
     * @param aa True if the bitmap should be anti-aliased, false otherwise.
     *
     * @see #hasAntiAlias()
     */
    public void setAntiAlias(boolean aa) {
        mPaint.setAntiAlias(aa);
        invalidateSelf();
    }

    /**
     * Indicates whether anti-aliasing is enabled for this drawable.
     *
     * @return True if anti-aliasing is enabled, false otherwise.
     *
     * @see #setAntiAlias(boolean)
     */
    public boolean hasAntiAlias() {
        return mPaint.isAntiAlias();
    }

    @Override
    public void setFilterBitmap(boolean filter) {
        mPaint.setFilterBitmap(filter);
        invalidateSelf();
    }

    @Override
    public void setDither(boolean dither) {
        mPaint.setDither(dither);
        invalidateSelf();
    }

    void gravityCompatApply(int gravity, int bitmapWidth, int bitmapHeight,
            Rect bounds, Rect outRect) {
        throw new UnsupportedOperationException();
    }

    void updateDstRect() {
        if (mApplyGravity) {
            if (mIsCircular) {
                final int minDimen = Math.min(mBitmapWidth, mBitmapHeight);
                gravityCompatApply(mGravity, minDimen, minDimen, getBounds(), mDstRect);

                // inset the drawing rectangle to the largest contained square,
                // so that a circle will be drawn
                final int minDrawDimen = Math.min(mDstRect.width(), mDstRect.height());
                final int insetX = Math.max(0, (mDstRect.width() - minDrawDimen) / 2);
                final int insetY = Math.max(0, (mDstRect.height() - minDrawDimen) / 2);
                mDstRect.inset(insetX, insetY);
                mCornerRadius = 0.5f * minDrawDimen;
            } else {
                gravityCompatApply(mGravity, mBitmapWidth, mBitmapHeight, getBounds(), mDstRect);
            }
            mDstRectF.set(mDstRect);

            if (mBitmapShader != null) {
                // setup shader matrix
                mShaderMatrix.setTranslate(mDstRectF.left,mDstRectF.top);
                mShaderMatrix.preScale(
                        mDstRectF.width() / mBitmap.getWidth(),
                        mDstRectF.height() / mBitmap.getHeight());
                mBitmapShader.setLocalMatrix(mShaderMatrix);
                mPaint.setShader(mBitmapShader);
            }

            mApplyGravity = false;
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        final Bitmap bitmap = mBitmap;
        if (bitmap == null) {
            return;
        }

        updateDstRect();
        if (mPaint.getShader() == null) {
            canvas.drawBitmap(bitmap, null, mDstRect, mPaint);
        } else {
            canvas.drawRoundRect(mDstRectF, mCornerRadius, mCornerRadius, mPaint);
        }
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
    public int getAlpha() {
        return mPaint.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
        invalidateSelf();
    }

    @Override
    public ColorFilter getColorFilter() {
        return mPaint.getColorFilter();
    }

    /**
     * Sets the image shape to circular.
     * <p>This overwrites any calls made to {@link #setCornerRadius(float)} so far.</p>
     */
    public void setCircular(boolean circular) {
        mIsCircular = circular;
        mApplyGravity = true;
        if (circular) {
            updateCircularCornerRadius();
            mPaint.setShader(mBitmapShader);
            invalidateSelf();
        } else {
            setCornerRadius(0);
        }
    }

    private void updateCircularCornerRadius() {
        final int minCircularSize = Math.min(mBitmapHeight, mBitmapWidth);
        mCornerRadius = minCircularSize / 2;
    }

    /**
     * @return <code>true</code> if the image is circular, else <code>false</code>.
     */
    public boolean isCircular() {
        return mIsCircular;
    }

    /**
     * Sets the corner radius to be applied when drawing the bitmap.
     */
    public void setCornerRadius(float cornerRadius) {
        if (mCornerRadius == cornerRadius) return;

        mIsCircular = false;
        if (isGreaterThanZero(cornerRadius)) {
            mPaint.setShader(mBitmapShader);
        } else {
            mPaint.setShader(null);
        }

        mCornerRadius = cornerRadius;
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        if (mIsCircular) {
            updateCircularCornerRadius();
        }
        mApplyGravity = true;
    }

    /**
     * @return The corner radius applied when drawing the bitmap.
     */
    public float getCornerRadius() {
        return mCornerRadius;
    }

    @Override
    public int getIntrinsicWidth() {
        return mBitmapWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mBitmapHeight;
    }

    @Override
    public int getOpacity() {
        if (mGravity != Gravity.FILL || mIsCircular) {
            return PixelFormat.TRANSLUCENT;
        }
        Bitmap bm = mBitmap;
        return (bm == null
                || bm.hasAlpha()
                || mPaint.getAlpha() < 255
                || isGreaterThanZero(mCornerRadius))
                ? PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;
    }

    RoundedBitmapDrawable(Resources res, Bitmap bitmap) {
        if (res != null) {
            mTargetDensity = res.getDisplayMetrics().densityDpi;
        }

        mBitmap = bitmap;
        if (mBitmap != null) {
            computeBitmapSize();
            mBitmapShader = new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        } else {
            mBitmapWidth = mBitmapHeight = -1;
            mBitmapShader = null;
        }
    }

    private static boolean isGreaterThanZero(float toCompare) {
        return toCompare > 0.05f;
    }
}
