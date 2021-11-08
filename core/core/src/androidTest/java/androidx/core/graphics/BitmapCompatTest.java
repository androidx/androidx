/*
 * Copyright 2021 The Android Open Source Project
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@RequiresApi(api = Build.VERSION_CODES.KITKAT)
@SmallTest
public class BitmapCompatTest {

    @Test
    public void testSizeAtStepUpscale() {
        assertEquals(240, BitmapCompat.sizeAtStep(240, 481, 2, 2));
        assertEquals(480, BitmapCompat.sizeAtStep(240, 481, 1, 2));
        assertEquals(481, BitmapCompat.sizeAtStep(240, 481, 0, 2));
    }

    public void testSizeAtStepDownscale() {
        assertEquals(320, BitmapCompat.sizeAtStep(320, 155, -2, -2));
        assertEquals(160, BitmapCompat.sizeAtStep(320, 155, -1, -2));
        assertEquals(155, BitmapCompat.sizeAtStep(320, 155, 0, -2));
        assertEquals(500, BitmapCompat.sizeAtStep(500, 19, -4, -4));
    }

    @Test
    public void testCreateScaledBitmap() {
        Bitmap input = Bitmap.createBitmap(2000, 1200, Bitmap.Config.ARGB_8888);
        Bitmap output = BitmapCompat.createScaledBitmap(input, 20, 12, null, false);
        assertEquals(20, output.getWidth());
        assertEquals(12, output.getHeight());
        assertEquals(input.getConfig(), output.getConfig());

        input.recycle();
        output.recycle();
    }

    // confirm a no-op on a non-hardware mutable bitmap gives a mutable copy with the same config
    @Test
    public void testCreateScaledBitmapNop() {
        Bitmap input = Bitmap.createBitmap(20, 21, Bitmap.Config.ARGB_8888);
        Bitmap output = BitmapCompat.createScaledBitmap(input, 20, 21, null, false);
        assertEquals(20, output.getWidth());
        assertEquals(21, output.getHeight());
        assertEquals(input.getConfig(), output.getConfig());
        assertTrue(input.isMutable());
        assertFalse(input.equals(output));
        assertTrue(output.isMutable());
        assertTrue(output.sameAs(input));

        input.recycle();
        output.recycle();
    }

    // confirm a no-op on a non-hardware immutable bitmap returns the same bitmap
    @Test
    public void testCreateScaledBitmapNopImmutable() {
        int[] colors = new int[20 * 21];
        // this method creates immutable bitmaps.
        Bitmap input = Bitmap.createBitmap(colors, 20, 21, Bitmap.Config.ARGB_8888);
        assertFalse(input.isMutable());
        Bitmap output = BitmapCompat.createScaledBitmap(input, 20, 21, null, false);
        assertTrue(input.equals(output));

        input.recycle();
    }

    // confirm a no-op on an immutable hardware bitmap gives a mutable copy with the same config
    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void testCreateScaledBitmapNopHardware() {
        HardwareBuffer hwbuffer = HardwareBuffer.create(50, 50, HardwareBuffer.RGBA_8888, 1,
                HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE);
        Bitmap input = Bitmap.wrapHardwareBuffer(hwbuffer,
                ColorSpace.get(ColorSpace.Named.DISPLAY_P3));
        Bitmap output = BitmapCompat.createScaledBitmap(input, 20, 21, null, false);
        assertEquals(20, output.getWidth());
        assertEquals(21, output.getHeight());
        assertEquals(Bitmap.Config.ARGB_8888, output.getConfig());
        assertTrue(output.isMutable());
        assertFalse(input.equals(output));

        input.recycle();
        output.recycle();
        hwbuffer.close();
    }

    // confirm degenerative case in which the user requests a sub rect of their input that is not
    // scaled, returns a new bitmap with that content.
    @Test
    public void testCreateScaledBitmapSubrect() {
        Bitmap input = Bitmap.createBitmap(20, 21, Bitmap.Config.ARGB_8888);
        Bitmap output = BitmapCompat.createScaledBitmap(input, 10, 10, new Rect(0, 0, 10, 10),
                false);
        assertEquals(10, output.getWidth());
        assertEquals(10, output.getHeight());
        assertEquals(input.getConfig(), output.getConfig());
        assertTrue(input.isMutable());
        assertFalse(input.equals(output));
        assertTrue(output.isMutable());

        input.recycle();
        output.recycle();
    }

    @Test
    public void testExceptionOutOfBoundsRect() {

        Bitmap input = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        assertThrows(IllegalArgumentException.class, () -> {
            Bitmap output = BitmapCompat.createScaledBitmap(input, 20, 20, new Rect(-2, 0, 32, 32),
                    false);
        });

        input.recycle();
    }

    @Test
    public void testExceptionZeroScaledWidth() {

        Bitmap input = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        assertThrows(IllegalArgumentException.class, () -> {
            Bitmap output = BitmapCompat.createScaledBitmap(input, 0, 1, null,
                    false);
        });

        input.recycle();
    }

    // fills bitmap with alternating rows of black and white
    private void fillStripes(Bitmap bm) {
        Canvas canvas = new Canvas(bm);
        Paint paint = new Paint();
        paint.setAntiAlias(false);
        paint.setColor(0xFF000000);
        canvas.drawPaint(paint);
        paint.setColor(0xFFFFFFFF);
        int width = bm.getWidth();
        int height = bm.getHeight();
        for (float y = 0.5f; y < height; y += 2) {
            canvas.drawLine(-1, y, width + 1, y, paint);
        }
    }

    // Confirm that createScaledBitmap accepts a hardware buffer input,
    // but in this case, produces an output bitmap that is not a hardware buffer.
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testHardwareBufferInput() {
        HardwareBuffer hwbuffer = HardwareBuffer.create(50, 50, HardwareBuffer.RGBA_8888, 1,
                HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE);
        Bitmap input = Bitmap.wrapHardwareBuffer(hwbuffer,
                ColorSpace.get(ColorSpace.Named.DISPLAY_P3));
        Bitmap output = BitmapCompat.createScaledBitmap(input, 20, 12, null, false);
        assertEquals(20, output.getWidth());
        assertEquals(12, output.getHeight());
        assertEquals(Bitmap.Config.HARDWARE, input.getConfig());
        assertEquals(Bitmap.Config.ARGB_8888, output.getConfig());

        input.recycle();
        output.recycle();
        hwbuffer.close();
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testHardwareBufferInputLinear() {
        HardwareBuffer hwbuffer = HardwareBuffer.create(50, 50, HardwareBuffer.RGBA_8888, 1,
                HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE);
        Bitmap input = Bitmap.wrapHardwareBuffer(hwbuffer,
                ColorSpace.get(ColorSpace.Named.DISPLAY_P3));
        Bitmap output = BitmapCompat.createScaledBitmap(input, 20, 12, null, true);
        assertEquals(20, output.getWidth());
        assertEquals(12, output.getHeight());
        assertEquals(Bitmap.Config.HARDWARE, input.getConfig());
        assertEquals(Bitmap.Config.ARGB_8888, output.getConfig());

        input.recycle();
        output.recycle();
        hwbuffer.close();
    }

    // Confirm a hardware buffer in the f16 pixel format comes out as a buffer in that format.
    @Test
    @SdkSuppress(minSdkVersion = 31)
    public void testHardwareBufferInputF16Linear() {
        HardwareBuffer hwbuffer = HardwareBuffer.create(50, 50, HardwareBuffer.RGBA_FP16, 1,
                HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE);
        Bitmap input = Bitmap.wrapHardwareBuffer(hwbuffer,
                ColorSpace.get(ColorSpace.Named.DISPLAY_P3));
        Bitmap output = BitmapCompat.createScaledBitmap(input, 20, 12, null, true);
        assertEquals(20, output.getWidth());
        assertEquals(12, output.getHeight());
        assertEquals(Bitmap.Config.HARDWARE, input.getConfig());
        assertEquals(Bitmap.Config.RGBA_F16, output.getConfig());

        input.recycle();
        output.recycle();
        hwbuffer.close();
    }

    // confirm that createScaledBitmap does not alter its input
    @Test
    public void testCreateScaledBitmapNonDestructive() {
        Bitmap input = Bitmap.createBitmap(2000, 1200, Bitmap.Config.ARGB_8888);
        fillStripes(input);
        Bitmap copy = input.copy(input.getConfig(), true);

        Bitmap output = BitmapCompat.createScaledBitmap(input, 20, 12, null, false);
        assertEquals(2000, input.getWidth());
        assertEquals(1200, input.getHeight());
        assertEquals(input.getConfig(), output.getConfig());
        assertTrue(copy.sameAs(input));

        input.recycle();
        output.recycle();
    }

    // A test where the size change is small enough that it will occur in a single step
    // thereby testing that the special behavior for a single step resize is also non-destructive.
    @Test
    public void testCreateScaledBitmapNonDestructiveSingleStep() {
        Bitmap input = Bitmap.createBitmap(2000, 1200, Bitmap.Config.ARGB_8888);
        fillStripes(input);
        Bitmap copy = input.copy(input.getConfig(), true);
        Bitmap output = BitmapCompat.createScaledBitmap(input, 1800, 1000, null, false);
        assertEquals(2000, input.getWidth());
        assertEquals(1200, input.getHeight());
        assertEquals(input.getConfig(), output.getConfig());
        assertTrue(copy.sameAs(input));

        input.recycle();
        output.recycle();
    }

    @Test
    public void testCreateScaledBitmapNonDestructiveLinear() {
        Bitmap input = Bitmap.createBitmap(2000, 1200, Bitmap.Config.ARGB_8888);
        fillStripes(input);
        Bitmap copy = input.copy(input.getConfig(), true);
        Bitmap output = BitmapCompat.createScaledBitmap(input, 20, 12, null, true);
        assertEquals(2000, input.getWidth());
        assertEquals(1200, input.getHeight());
        assertEquals(input.getConfig(), output.getConfig());
        assertTrue(copy.sameAs(input));

        input.recycle();
        output.recycle();
    }

    // confirm that createScaledBitmap does not alter its input when input has a non-default
    // color space
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    public void testCreateScaledBitmapNonDestructiveColor() {
        Bitmap input = Bitmap.createBitmap(2000, 1200, Bitmap.Config.RGBA_F16);
        fillStripes(input);
        Bitmap copy = input.copy(input.getConfig(), true);
        Bitmap output = BitmapCompat.createScaledBitmap(input, 20, 12, null, false);
        assertEquals(2000, input.getWidth());
        assertEquals(1200, input.getHeight());
        assertEquals(input.getConfig(), output.getConfig());
        assertTrue(copy.sameAs(input));

        assertEquals(20, output.getWidth());
        assertEquals(12, output.getHeight());

        input.recycle();
        output.recycle();
    }

    // confirm via colored rectangles in the corners of an image that the resulting image hasn't
    // been accidentally cropped.
    // may not work on input dimensions less than about 60 pixels
    @SdkSuppress(minSdkVersion = 29)
    private void scaledBitmapAndCheckCorners(int srcW, int srcH, int dstW, int dstH,
            boolean linear) {
        Bitmap bm = Bitmap.createBitmap(srcW, srcH, Bitmap.Config.ARGB_8888);

        // four percent of input width and height;
        float pW = srcW * 0.04f;
        float pH = srcH * 0.04f;

        // draw a colored rect in each corner.
        Canvas canvas = new Canvas(bm);
        Paint paint = new Paint();
        paint.setAntiAlias(false);
        paint.setColor(Color.rgb(0, 0, 0));
        canvas.drawPaint(paint);

        paint.setColor(Color.rgb(255, 0, 0)); // red, top left
        canvas.drawRect(0, 0, pW, pH, paint);

        paint.setColor(Color.rgb(0, 255, 0));// green, top right
        canvas.drawRect(srcW - pW, 0, srcW, pH, paint);

        paint.setColor(Color.rgb(0, 0, 255));// blue, bottom right
        canvas.drawRect(srcW - pW, srcH - pH, srcW, srcH, paint);

        paint.setColor(Color.rgb(0, 255, 255));// cyan, bottom left
        canvas.drawRect(0, srcH - pH, pW, srcH, paint);

        Bitmap output = BitmapCompat.createScaledBitmap(bm, dstW, dstH, null,
                linear);

        // two percent of output width and height;
        int qW = (int) (dstW * 0.02f);
        int qH = (int) (dstH * 0.02f);

        // confirm centers of each corner have correct color.
        assertEquals(Color.valueOf(1f, 0f, 0f, 1f), output.getColor(qW, qH));
        assertEquals(Color.valueOf(0f, 1f, 0f, 1f), output.getColor(dstW - qW, qH));
        assertEquals(Color.valueOf(0f, 0f, 1f, 1f), output.getColor(dstW - qW, dstH - qH));
        assertEquals(Color.valueOf(0f, 1f, 1f, 1f), output.getColor(qW, dstH - qH));
    }


    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void testCropDotNotOccurUpscaleLinear() {
        scaledBitmapAndCheckCorners(100, 100, 805, 805, true);
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void testCropDotNotOccurUpscaleNonLinear() {
        scaledBitmapAndCheckCorners(100, 100, 805, 805, false);
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void testCropDotNotOccurDownscaleLinear() {
        scaledBitmapAndCheckCorners(805, 805, 100, 100, true);
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void testCropDotNotOccurDownscaleNonLinear() {
        scaledBitmapAndCheckCorners(805, 805, 100, 100, false);
    }

    // confirm that createScaledBitmap scaling algorithm produces high quality images.
    // When performing a large down scale of an image, it is important the each pixel in the result
    // is a good average of the pixels in the corresponding region from the source, as opposed to
    // being an average of a small non-random selection of them.
    // To confirm this, we resize an image consisting of alternating 1px rows of black and white.
    // For a resize smaller than 1/2 of the original dimensions, the result should basically be
    // uniform grey, so the higher quality the resize, the lower the variance in grey values is
    // expected to be.
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    private void scaledBitmapAndAssertQuality(int scaledWidth, int scaledHeight,
            boolean scaleInLinearSpace,
            float expectedMeanValue, float expectedVariance, Bitmap.Config inputConfig) {
        int width = 500;
        int height = 500;
        Bitmap bm = Bitmap.createBitmap(width, height, inputConfig);
        fillStripes(bm);

        int numOut = scaledWidth * scaledHeight;
        Bitmap output = BitmapCompat.createScaledBitmap(bm, scaledWidth, scaledHeight, null,
                scaleInLinearSpace);

        // assert correct metadata
        assertEquals(scaledWidth, output.getWidth());
        assertEquals(scaledHeight, output.getHeight());
        assertEquals(bm.getConfig(), output.getConfig());

        // measure the variance of the red channel in the result
        int[] pixelsOut = new int[numOut];
        output.getPixels(pixelsOut, 0, scaledWidth, 0, 0, scaledWidth, scaledHeight);
        float totalRed = 0;
        for (int i = 0; i < numOut; i++) {
            totalRed += Color.red(pixelsOut[i]);
        }
        totalRed /= numOut;
        assertTrue(Math.abs(totalRed - expectedMeanValue) < Math.sqrt(expectedVariance));
        float variance = 0;
        for (int i = 0; i < numOut; i++) {
            variance += Math.pow(Color.red(pixelsOut[i]) - totalRed, 2);
        }
        variance /= numOut;
        assertTrue(variance < expectedVariance);

        bm.recycle();
        output.recycle();
    }

    private static final float DOWNSCALE_VARIANCE = 60f;
    private static final float UPSCALE_VARIANCE = 40000f;

    // Unlike downscale, We really aren't verifying the quality of the upscale, because it's
    // subjective. The variance limit is set so high, the image could be anything. So all we're
    // doing with  the upscale tests effectively is verifying that they don't throw exceptions, and
    // return images with the right metadata.

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testQualityDownscaleLinear() {
        scaledBitmapAndAssertQuality(19, 19, true, 187.5f, DOWNSCALE_VARIANCE,
                Bitmap.Config.ARGB_8888);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testQualityDownscaleNonLinear() {
        scaledBitmapAndAssertQuality(19, 19, false, 127.0f, DOWNSCALE_VARIANCE,
                Bitmap.Config.ARGB_8888);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testQualityUpscaleLinear() {
        scaledBitmapAndAssertQuality(1213, 4213, true, 187.5f, UPSCALE_VARIANCE,
                Bitmap.Config.ARGB_8888);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void testQualityUpscaleNonLinear() {
        scaledBitmapAndAssertQuality(1213, 1213, false, 127.0f, UPSCALE_VARIANCE,
                Bitmap.Config.ARGB_8888);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    public void testQualityDownscaleLinearF16() {
        scaledBitmapAndAssertQuality(19, 19, true, 187.5f, DOWNSCALE_VARIANCE,
                Bitmap.Config.RGBA_F16);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testQualityDownscaleNonLinearF16() {
        scaledBitmapAndAssertQuality(19, 19, false, 127.0f, DOWNSCALE_VARIANCE,
                Bitmap.Config.RGBA_F16);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    public void testQualityUpscaleLinearF16() {
        scaledBitmapAndAssertQuality(1213, 1213, true, 187.5f, UPSCALE_VARIANCE,
                Bitmap.Config.RGBA_F16);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testQualityUpscaleNonLinearF16() {
        scaledBitmapAndAssertQuality(1213, 1213, false, 127.0f, UPSCALE_VARIANCE,
                Bitmap.Config.RGBA_F16);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O_MR1)
    public void testQualityScaleYDownAndXUp() {
        scaledBitmapAndAssertQuality(1000, 20, true, 187.5f, UPSCALE_VARIANCE,
                Bitmap.Config.ARGB_8888);
    }
}
