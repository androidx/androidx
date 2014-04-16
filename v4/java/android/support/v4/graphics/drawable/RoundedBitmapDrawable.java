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
package android.support.v4.graphics.drawable;

import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.BitmapCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

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
public class RoundedBitmapDrawable extends Drawable {

    private static final String TAG = "RoundedBitmapDrawable";
    private static final int DEFAULT_PAINT_FLAGS =
            Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG;
    private Bitmap mBitmap;
    private int mTargetDensity = DisplayMetrics.DENSITY_DEFAULT;
    private int mGravity = Gravity.FILL;
    private Paint mPaint = new Paint(DEFAULT_PAINT_FLAGS);
    private BitmapShader mBitmapShader;
    private float mCornerRadius;

    private final Rect mDstRect = new Rect();   // Gravity.apply() sets this
    private final RectF mDstRectF = new RectF();

    private boolean mApplyGravity = true;

     // These are scaled to match the target density.
    private int mBitmapWidth;
    private int mBitmapHeight;


    /**
     * Returns a new drawable by creating it from a bitmap, setting initial target density based on
     * the display metrics of the resources.
     */
    public static RoundedBitmapDrawable createRoundedBitmapDrawable(Resources res, Bitmap bitmap) {
        return new RoundedBitmapDrawable(res, bitmap);
    }

    /**
     * Returns a new drawable, creating it by opening a given file path and decoding the bitmap.
     */
    public static RoundedBitmapDrawable createRoundedBitmapDrawable(Resources res,
            String filepath) {
        final RoundedBitmapDrawable drawable =
                createRoundedBitmapDrawable(res, BitmapFactory.decodeFile(filepath));
        if (drawable.mBitmap == null) {
            Log.w(TAG, "BitmapDrawable cannot decode " + filepath);
        }
        return drawable;
    }


    /**
     * Returns a new drawable, creating it by decoding a bitmap from the given input stream.
     */
    public static RoundedBitmapDrawable createRoundedBitmapDrawable(Resources res,
            java.io.InputStream is) {
        final RoundedBitmapDrawable drawable =
                createRoundedBitmapDrawable(res, BitmapFactory.decodeStream(is));
        if (drawable.mBitmap == null) {
            Log.w(TAG, "BitmapDrawable cannot decode " + is);
        }
        return drawable;
    }

    /**
     * Returns the paint used to render this drawable.
     */
    public final Paint getPaint() {
        return mPaint;
    }

    /**
     * Returns the bitmap used by this drawable to render. May be null.
     */
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
    public void setTargetDensity(Canvas canvas) {
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
    public void setTargetDensity(DisplayMetrics metrics) {
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
        if (mBitmap != null) {
            BitmapCompat.setHasMipMap(mBitmap, mipMap);
            invalidateSelf();
        }
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
        return mBitmap != null && BitmapCompat.hasMipMap(mBitmap);
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

    @Override
    public void draw(Canvas canvas) {
        final Bitmap bitmap = mBitmap;
        if (bitmap == null) {
            return;
        }

        final Paint paint = mPaint;

        final Shader shader = paint.getShader();
        if (shader == null) {
            if (mApplyGravity) {
                GravityCompat.apply(mGravity, mBitmapWidth, mBitmapHeight,
                        getBounds(), mDstRect, ViewCompat.LAYOUT_DIRECTION_LTR);
                mDstRectF.set(mDstRect);
                mApplyGravity = false;
            }

            canvas.drawBitmap(bitmap, null, mDstRect, paint);

        } else {
            if (mApplyGravity) {
                copyBounds(mDstRect);
                mDstRectF.set(mDstRect);
                mApplyGravity = false;
            }

            canvas.drawRoundRect(mDstRectF, mCornerRadius, mCornerRadius, paint);
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

    public int getAlpha() {
        return mPaint.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
        invalidateSelf();
    }

    public ColorFilter getColorFilter() {
        return mPaint.getColorFilter();
    }

    /**
     * Sets the corner radius to be applied when drawing the bitmap.
     */
    public void setCornerRadius(float cornerRadius) {
        if (isGreaterThanZero(cornerRadius)) {
            mPaint.setShader(mBitmapShader);
        } else {
            mPaint.setShader(null);
        }
        mCornerRadius = cornerRadius;
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
        if (mGravity != Gravity.FILL) {
            return PixelFormat.TRANSLUCENT;
        }
        Bitmap bm = mBitmap;
        return (bm == null
                || bm.hasAlpha()
                || mPaint.getAlpha() < 255
                || isGreaterThanZero(mCornerRadius))
                ? PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;
    }

    private RoundedBitmapDrawable(Resources res, Bitmap bitmap) {
        if (res != null) {
            mTargetDensity = res.getDisplayMetrics().densityDpi;
        }

        mBitmap = bitmap;
        if (mBitmap != null) {
            computeBitmapSize();
            mBitmapShader = new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        } else {
            mBitmapWidth = mBitmapHeight = -1;
        }
    }

    private boolean isGreaterThanZero(float toCompare) {
        return Float.compare(toCompare, +0.0f) > 0;
    }
}
