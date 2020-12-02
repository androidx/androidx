/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface.complications.rendering;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import java.util.Objects;

/**
 * Class used to maintain and draw a rounded rectangular image. The given drawable will be drawn in
 * a rounded rectangle within the specified bounds. The image will be cropped.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class RoundedDrawable extends Drawable {

    @VisibleForTesting final Paint mPaint;

    private Drawable mDrawable;
    private int mRadius; // Radius in pixels

    // Used to avoid creating new RectF object every time draw() is called
    private final RectF mTmpBounds = new RectF();

    RoundedDrawable() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
    }

    /**
     * Sets the drawable to be rendered.
     *
     * @param drawable {@link Drawable} to be rendered
     */
    public void setDrawable(@NonNull Drawable drawable) {
        if (Objects.equals(mDrawable, drawable)) {
            return;
        }
        mDrawable = drawable;
        updateBitmapShader();
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        mTmpBounds.right = bounds.width();
        mTmpBounds.bottom = bounds.height();
        updateBitmapShader();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (mDrawable == null || bounds.isEmpty()) {
            return;
        }
        canvas.save();
        canvas.translate(bounds.left, bounds.top);
        // mTmpBounds is bounds translated to (0,0) and converted to RectF as drawRoundRect
        // requires.
        canvas.drawRoundRect(mTmpBounds, (float) mRadius, (float) mRadius, mPaint);
        canvas.restore();
    }

    /** @deprecated This method is no longer used in graphics optimizations */
    @Override
    @Deprecated
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    /**
     * Sets the border radius to be applied when rendering drawable.
     *
     * @param radius border radius in pixels
     */
    public void setRadius(int radius) {
        mRadius = radius;
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
    @NonNull
    private Bitmap drawableToBitmap(@NonNull Drawable drawable, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();
        if (intrinsicWidth > intrinsicHeight) {
            // Center crop the Drawable width wise
            float aspectRatio = (float) intrinsicWidth / intrinsicHeight;
            int scaledWidth = (int) (width * aspectRatio);
            int offset = (scaledWidth - width) / 2;
            drawable.setBounds(-offset, 0, width + offset, height);
        } else {
            // Center crop the Drawable height wise
            float aspectRatio = (float) intrinsicHeight / intrinsicWidth;
            int scaledHeight = (int) (height * aspectRatio);
            int offset = (scaledHeight - height) / 2;
            drawable.setBounds(0, -offset, width, height + offset);
        }
        drawable.draw(canvas);
        return bitmap;
    }

    @Nullable
    @VisibleForTesting
    Drawable getDrawable() {
        return mDrawable;
    }
}
