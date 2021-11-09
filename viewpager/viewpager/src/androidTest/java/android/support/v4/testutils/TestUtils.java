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


package android.support.v4.testutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public class TestUtils {
    /**
     * Checks whether all the pixels in the specified drawable are of the same specified color.
     * If the passed <code>Drawable</code> does not have positive intrinsic dimensions set, this
     * method will throw an <code>IllegalArgumentException</code>. If there is a color mismatch,
     * this method will call <code>Assert.fail</code> with detailed description of the mismatch.
     */
    public static void assertAllPixelsOfColor(String failMessagePrefix, @NonNull Drawable drawable,
            @ColorInt int color) {
        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();

        if ((drawableWidth <= 0) || (drawableHeight <= 0)) {
            throw new IllegalArgumentException("Drawable must be configured to have non-zero size");
        }

        assertAllPixelsOfColor(failMessagePrefix, drawable, drawableWidth, drawableHeight, color,
                false);
    }

    /**
     * Checks whether all the pixels in the specified drawable are of the same specified color.
     *
     * In case there is a color mismatch, the behavior of this method depends on the
     * <code>throwExceptionIfFails</code> parameter. If it is <code>true</code>, this method will
     * throw an <code>Exception</code> describing the mismatch. Otherwise this method will call
     * <code>Assert.fail</code> with detailed description of the mismatch.
     */
    public static void assertAllPixelsOfColor(String failMessagePrefix, @NonNull Drawable drawable,
            int drawableWidth, int drawableHeight, @ColorInt int color,
            boolean throwExceptionIfFails) {
        // Create a bitmap
        Bitmap bitmap = Bitmap.createBitmap(drawableWidth, drawableHeight, Bitmap.Config.ARGB_8888);
        // Create a canvas that wraps the bitmap
        Canvas canvas = new Canvas(bitmap);
        // Configure the drawable to have bounds that match its intrinsic size
        drawable.setBounds(0, 0, drawableWidth, drawableHeight);
        // And ask the drawable to draw itself to the canvas / bitmap
        drawable.draw(canvas);

        try {
            int[] rowPixels = new int[drawableWidth];
            for (int row = 0; row < drawableHeight; row++) {
                bitmap.getPixels(rowPixels, 0, drawableWidth, 0, row, drawableWidth, 1);
                for (int column = 0; column < drawableWidth; column++) {
                    if (rowPixels[column] != color) {
                        String mismatchDescription = failMessagePrefix
                                + ": expected all drawable colors to be ["
                                + Color.red(color) + "," + Color.green(color) + ","
                                + Color.blue(color)
                                + "] but at position (" + row + "," + column + ") found ["
                                + Color.red(rowPixels[column]) + ","
                                + Color.green(rowPixels[column]) + ","
                                + Color.blue(rowPixels[column]) + "]";
                        if (throwExceptionIfFails) {
                            throw new RuntimeException(mismatchDescription);
                        } else {
                            fail(mismatchDescription);
                        }
                    }
                }
            }
        } finally {
            bitmap.recycle();
        }
    }

    /**
     * Checks whether the specified rectangle matches the specified left / top / right /
     * bottom bounds.
     */
    public static void assertRectangleBounds(String failMessagePrefix, @NonNull Rect rectangle,
            int left, int top, int right, int bottom) {
        assertEquals(failMessagePrefix + " left", rectangle.left, left);
        assertEquals(failMessagePrefix + " top", rectangle.top, top);
        assertEquals(failMessagePrefix + " right", rectangle.right, right);
        assertEquals(failMessagePrefix + " bottom", rectangle.bottom, bottom);
    }
}