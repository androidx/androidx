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
package androidx.core.graphics;

import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.ColorSpace;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

/**
 * Helper for accessing features in {@link Bitmap}.
 */
public final class BitmapCompat {

    /**
     * Indicates whether the renderer responsible for drawing this
     * bitmap should attempt to use mipmaps when this bitmap is drawn
     * scaled down.
     * <p>
     * If you know that you are going to draw this bitmap at less than
     * 50% of its original size, you may be able to obtain a higher
     * quality
     * <p>
     * This property is only a suggestion that can be ignored by the
     * renderer. It is not guaranteed to have any effect.
     *
     * @return true if the renderer should attempt to use mipmaps,
     * false otherwise
     * @see Bitmap#hasMipMap()
     */
    public static boolean hasMipMap(@NonNull Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= 17) {
            return Api17Impl.hasMipMap(bitmap);
        }
        return false;
    }

    /**
     * Set a hint for the renderer responsible for drawing this bitmap
     * indicating that it should attempt to use mipmaps when this bitmap
     * is drawn scaled down.
     * <p>
     * If you know that you are going to draw this bitmap at less than
     * 50% of its original size, you may be able to obtain a higher
     * quality by turning this property on.
     * <p>
     * Note that if the renderer respects this hint it might have to
     * allocate extra memory to hold the mipmap levels for this bitmap.
     * <p>
     * This property is only a suggestion that can be ignored by the
     * renderer. It is not guaranteed to have any effect.
     *
     * @param bitmap bitmap for which to set the state.
     * @param hasMipMap indicates whether the renderer should attempt
     *                  to use mipmaps
     * @see Bitmap#setHasMipMap(boolean)
     */
    public static void setHasMipMap(@NonNull Bitmap bitmap, boolean hasMipMap) {
        if (Build.VERSION.SDK_INT >= 17) {
            Api17Impl.setHasMipMap(bitmap, hasMipMap);
        }
    }

    /**
     * Returns the size of the allocated memory used to store this bitmap's pixels.
     * <p>
     * This value will not change over the lifetime of a Bitmap.
     *
     * @see Bitmap#getAllocationByteCount()
     */
    public static int getAllocationByteCount(@NonNull Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= 19) {
            return Api19Impl.getAllocationByteCount(bitmap);
        }
        return bitmap.getByteCount();
    }

    /**
     * <p>Return a scaled bitmap.</p>
     * <p>This algorithm is intended for downscaling by large ratios when high quality is desired.
     * It is similar to the creation of mipmaps, but stops at the desired size.
     * Visually, the result is smoother and softer than {@link Bitmap#createScaledBitmap}</p>
     *
     * <p>
     * The returned bitmap will always be a mutable copy with a config matching the input except in
     * the following scenarios:
     * <ol>
     * <li> The source bitmap is returned and the source bitmap is immutable.</li>
     * <li> The source bitmap is a {@code HARDWARE} bitmap. For this input, a mutable
     * non-{@code HARDWARE} Bitmap
     * is returned. On API 31 and up, the internal format of the HardwareBuffer is read to
     * determine the underlying format, and the returned Bitmap will use a Config to match.
     * Pre-31, the returned Bitmap will be {@code ARGB_8888}.
     * </li></ol></p>
     *
     * @param srcBm              A source bitmap. It will not be altered.
     * @param dstW               The output width
     * @param dstH               The output height
     * @param srcRect            Uses a region of the input bitmap as the source.
     * @param scaleInLinearSpace When true, uses {@code LINEAR_EXTENDED_SRGB} as a color space
     *                           when scaling.
     *                           Otherwise, uses the color space of the input bitmap. (On API
     *                           level 26 and earlier, this parameter has no effect).
     * @return A new bitmap in the requested size.
     */
    public static @NonNull
    Bitmap createScaledBitmap(@NonNull Bitmap srcBm, int dstW,
            int dstH, @Nullable Rect srcRect, boolean scaleInLinearSpace) {
        if (dstW <= 0 || dstH <= 0) {
            throw new IllegalArgumentException("dstW and dstH must be > 0!");
        }

        if (srcRect != null) {
            if (srcRect.isEmpty() || srcRect.left < 0 || srcRect.right > srcBm.getWidth()
                    || srcRect.top < 0 || srcRect.bottom > srcBm.getHeight()) {
                throw new IllegalArgumentException("srcRect must be contained by srcBm!");
            }
        }

        Bitmap src = srcBm;
        if (Build.VERSION.SDK_INT >= 27) {
            // Note that since this uses Bitmap.copy, not canvas.drawBitmap, it cannot be eliminated
            // by combining it with the first drawBitmap that occurs.
            src = Api27Impl.copyBitmapIfHardware(srcBm);
        }

        int srcW = srcRect != null ? srcRect.width() : srcBm.getWidth();
        int srcH = srcRect != null ? srcRect.height() : srcBm.getHeight();

        float sx = dstW / (float) srcW;
        float sy = dstH / (float) srcH;

        int srcX = srcRect != null ? srcRect.left : 0;
        int srcY = srcRect != null ? srcRect.top : 0;

        // Early return for no-ops
        if (srcX == 0 && srcY == 0 && dstW == srcBm.getWidth() && dstH == srcBm.getHeight()) {
            // Don't return inputs if they are mutable.
            if (srcBm.isMutable() && srcBm == src) {
                return srcBm.copy(srcBm.getConfig(), true);
            } else {
                // this may be the original, or it may be a copy of a hardware bitmap
                return src;
            }
        }

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);
        if (Build.VERSION.SDK_INT >= 29) {
            Api29Impl.setPaintBlendMode(paint);
        } else {
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        }

        // Special case for copying from sub-rects without scaling
        if (srcW == dstW && srcH == dstH) {
            Bitmap out = Bitmap.createBitmap(dstW, dstH, src.getConfig());
            Canvas canvasForCopy = new Canvas(out);
            canvasForCopy.drawBitmap(src, -srcX, -srcY, paint);
            return out;
        }

        // How many filtering steps to do in X and Y. + means upscaling, - means downscaling.
        double log2 = Math.log(2);
        int stepsX = (sx > 1.0f) ? (int) Math.ceil(Math.log(sx) / log2) :
                (int) Math.floor(Math.log(sx) / log2);
        int stepsY = (sy > 1.0f) ? (int) Math.ceil(Math.log(sy) / log2) :
                (int) Math.floor(Math.log(sy) / log2);
        final int totalStepsX = stepsX;
        final int totalStepsY = stepsY;

        // Bitmaps are re-used in order to minimize allocations.
        // One is a source and one is a destination, and at each step they switch roles.
        // On the first pass however, srcBm may take the place of src if no linear color space
        // transformation is being performed.
        Bitmap dst = null;
        // A flag indicating the scratch bitmaps will be in a different color space than the
        // intended output color space and a conversion on the final iteration will be necessary.
        boolean needFinalConversion = false;
        if (scaleInLinearSpace) {
            if (Build.VERSION.SDK_INT >= 27 && !Api27Impl.isAlreadyF16AndLinear(srcBm)) {
                int allocW = stepsX > 0 ? sizeAtStep(srcW, dstW, 1, totalStepsX) : srcW;
                int allocH = stepsY > 0 ? sizeAtStep(srcH, dstH, 1, totalStepsY) : srcH;
                dst = Api27Impl.createBitmapWithSourceColorspace(
                        allocW, allocH, srcBm, true);
                Canvas canvasForCopy = new Canvas(dst);
                canvasForCopy.drawBitmap(src, -srcX, -srcY, paint);
                srcX = 0;
                srcY = 0;
                Bitmap swap = dst;
                dst = src;
                src = swap;
                needFinalConversion = true;
            }
        }

        Rect currRect = new Rect(srcX, srcY, srcW, srcH);
        Rect nextRect = new Rect();

        while (stepsX != 0 || stepsY != 0) {
            if (stepsX < 0) {
                stepsX++;
            } else if (stepsX > 0) {
                --stepsX;
            }
            if (stepsY < 0) {
                stepsY++;
            } else if (stepsY > 0) {
                --stepsY;
            }
            int nextW = sizeAtStep(srcW, dstW, stepsX, totalStepsX);
            int nextH = sizeAtStep(srcH, dstH, stepsY, totalStepsY);
            nextRect.set(0, 0, nextW, nextH);

            // The purpose of following block is to make dst a suitable size, configuration, and
            // color space for the next iteration in the loop, while minimizing allocation.
            // The following constraints/needs are addressed:
            // * On the first pass, allocate dst for the first time.
            // * On the second pass, once the scratch bitmaps have been swapped, allocate the
            //      other bitmap.
            // * Either of them could have already been allocated for the first time due
            //      to scaleInLinearSpace or copying out of a hardware buffer.
            // * On the last pass, convert back to the original config and color space.
            // * recycle() any bitmap that will no longer be used.
            // * re-use a region within a bitmap instead of allocating wherever possible.
            // * If scaling down, it may be a waste of memory to return the user a bitmap with a
            //      larger footprint than necessary as the costs of using over its lifetime may
            //      exceed the savings of re-using the allocation here.
            // * Color spaces are only supported on O or later.
            // * This function may not alter srcBm.
            boolean lastStep = (stepsX == 0 && stepsY == 0);
            boolean dstSizeIsFinal =
                    dst != null && dst.getWidth() == dstW && dst.getHeight() == dstH;
            if (
                // On first and second passes, scratch bitmaps may not have been allocated yet.
                dst == null
                // The previous step may have read directly from srcBm then swapped
                // it with dst.
                || dst == srcBm
                // dst may have been allocated by the hardware copy step, but linear is
                // requested and dst is not linear yet.
                || (scaleInLinearSpace && (Build.VERSION.SDK_INT >= 27
                && !Api27Impl.isAlreadyF16AndLinear(dst)))
                // If this is the last step and the scratch bitmap cannot be returned,
                // because in the wrong color space, allocate a new bitmap that will
                // be returned.
                || (lastStep && (!dstSizeIsFinal || needFinalConversion))
            ) {
                // Recycle the old one if necessary
                if (dst != srcBm && dst != null) {
                    dst.recycle();
                }

                // The scratch bitmap may be reused multiple times. Choose a size large enough for
                // the largest draw that will be made to them. Each dimension can be considered
                // independently. When a dimension is being scaled up, take the size of the
                // last step. When a dimension is being scaled down, take the size of the current
                // step.
                int lastScratchStep = needFinalConversion ? 1 : 0;
                int allocW = sizeAtStep(srcW, dstW, stepsX > 0 ? lastScratchStep : stepsX,
                        totalStepsX);
                int allocH = sizeAtStep(srcH, dstH, stepsY > 0 ? lastScratchStep : stepsY,
                        totalStepsY);

                // Create a new bitmap. If possible, use the correct color space.
                if (Build.VERSION.SDK_INT >= 27) {
                    boolean linear = scaleInLinearSpace && !lastStep;
                    dst = Api27Impl.createBitmapWithSourceColorspace(
                            allocW, allocH, srcBm, linear);
                } else {
                    dst = Bitmap.createBitmap(allocW, allocH, src.getConfig());
                }
            }

            // On any iteration where dst did not need to be created anew, it is suitable to draw
            // into the region of it indicated by nextRect.
            Canvas canvas = new Canvas(dst);
            canvas.drawBitmap(src, currRect, nextRect, paint);

            // swap the two bitmaps
            Bitmap swap = src;
            src = dst;
            dst = swap;
            currRect.set(nextRect);
        }
        if (dst != srcBm && dst != null) {
            dst.recycle();
        }
        return src; // remember they were just swapped
    }

    /**
     * Return the size that a scratch bitmap dimension (x or y) should be at a given step.
     * When scaling up step counts down to zero from positive numbers.
     * When scaling down, step counts up to zero from negative numbers.
     */
    @VisibleForTesting
    static int sizeAtStep(int srcSize, int dstSize, int step, int totalSteps) {
        if (step == 0) {
            return dstSize;
        } else if (step > 0) { // upscale
            return srcSize * (1 << (totalSteps - step));
        } else { // downscale
            return dstSize << (-step - 1);
        }
    }

    private BitmapCompat() {
        // This class is not instantiable.
    }

    @RequiresApi(17)
    static class Api17Impl {
        private Api17Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static boolean hasMipMap(Bitmap bitmap) {
            return bitmap.hasMipMap();
        }

        @DoNotInline
        static void setHasMipMap(Bitmap bitmap, boolean hasMipMap) {
            bitmap.setHasMipMap(hasMipMap);
        }
    }

    @RequiresApi(19)
    static class Api19Impl {
        private Api19Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static int getAllocationByteCount(Bitmap bitmap) {
            return bitmap.getAllocationByteCount();
        }
    }

    @RequiresApi(27)
    static class Api27Impl {
        private Api27Impl() {
        }

        @DoNotInline
        static Bitmap createBitmapWithSourceColorspace(int w, int h, Bitmap src, boolean linear) {
            Bitmap.Config config = src.getConfig();
            ColorSpace colorSpace = src.getColorSpace();
            ColorSpace linearCs = ColorSpace.get(ColorSpace.Named.LINEAR_EXTENDED_SRGB);
            if (linear && !src.getColorSpace().equals(linearCs)) {
                // Promote to F16 to preserve precision.
                config = Bitmap.Config.RGBA_F16;
                colorSpace = linearCs;
            } else if (src.getConfig() == Bitmap.Config.HARDWARE) {
                config = Bitmap.Config.ARGB_8888;
                if (Build.VERSION.SDK_INT >= 31) {
                    config = Api31Impl.getHardwareBitmapConfig(src);
                }
            }
            return Bitmap.createBitmap(w, h, config, src.hasAlpha(), colorSpace);
        }

        @DoNotInline
        static boolean isAlreadyF16AndLinear(Bitmap b) {
            ColorSpace linearCs = ColorSpace.get(ColorSpace.Named.LINEAR_EXTENDED_SRGB);
            return b.getConfig() == Bitmap.Config.RGBA_F16 && b.getColorSpace().equals(linearCs);
        }

        @DoNotInline
        static Bitmap copyBitmapIfHardware(Bitmap bm) {
            if (bm.getConfig() == Bitmap.Config.HARDWARE) {
                Bitmap.Config newConfig = Bitmap.Config.ARGB_8888;
                if (Build.VERSION.SDK_INT >= 31) {
                    newConfig = Api31Impl.getHardwareBitmapConfig(bm);
                }
                return bm.copy(newConfig, true);
            } else {
                return bm;
            }
        }
    }

    @RequiresApi(29)
    static class Api29Impl {
        private Api29Impl() {
        }

        @DoNotInline
        static void setPaintBlendMode(Paint paint) {
            paint.setBlendMode(BlendMode.SRC);
        }
    }

    @RequiresApi(31)
    static class Api31Impl {
        private Api31Impl() {
        }

        @DoNotInline
        static Bitmap.Config getHardwareBitmapConfig(Bitmap bm) {
            if (bm.getHardwareBuffer().getFormat() == HardwareBuffer.RGBA_FP16) {
                return Bitmap.Config.RGBA_F16;
            } else {
                return Bitmap.Config.ARGB_8888;
            }
        }
    }
}
