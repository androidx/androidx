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


package android.support.v7.testutils;

import android.support.v4.util.Pair;
import android.view.View;
import android.view.ViewParent;
import junit.framework.Assert;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class TestUtils {
    /**
     * This method takes a view and returns a single bitmap that is the layered combination
     * of background drawables of this view and all its ancestors. It can be used to abstract
     * away the specific implementation of a view hierarchy that is not exposed via class APIs
     * or a view hierarchy that depends on the platform version. Instead of hard-coded lookups
     * of particular inner implementations of such a view hierarchy that can break during
     * refactoring or on newer platform versions, calling this API returns a "combined" background
     * of the view.
     *
     * For example, it is useful to get the combined background of a popup / dropdown without
     * delving into the inner implementation details of how that popup is implemented on a
     * particular platform version.
     */
    public static Bitmap getCombinedBackgroundBitmap(View view) {
        final int bitmapWidth = view.getWidth();
        final int bitmapHeight = view.getHeight();

        // Create a bitmap
        final Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight,
                Bitmap.Config.ARGB_8888);
        // Create a canvas that wraps the bitmap
        final Canvas canvas = new Canvas(bitmap);

        // As the draw pass starts at the top of view hierarchy, our first step is to traverse
        // the ancestor hierarchy of our view and collect a list of all ancestors with non-null
        // and visible backgrounds. At each step we're keeping track of the combined offsets
        // so that we can properly combine all of the visuals together in the next pass.
        List<View> ancestorsWithBackgrounds = new ArrayList<>();
        List<Pair<Integer, Integer>> ancestorOffsets = new ArrayList<>();
        int offsetX = 0;
        int offsetY = 0;
        while (true) {
            final Drawable backgroundDrawable = view.getBackground();
            if ((backgroundDrawable != null) && backgroundDrawable.isVisible()) {
                ancestorsWithBackgrounds.add(view);
                ancestorOffsets.add(Pair.create(offsetX, offsetY));
            }
            // Go to the parent
            ViewParent parent = view.getParent();
            if (!(parent instanceof View)) {
                // We're done traversing the ancestor chain
                break;
            }

            // Update the offsets based on the location of current view in its parent's bounds
            offsetX += view.getLeft();
            offsetY += view.getTop();

            view = (View) parent;
        }

        // Now we're going to iterate over the collected ancestors in reverse order (starting from
        // the topmost ancestor) and draw their backgrounds into our combined bitmap. At each step
        // we are respecting the offsets of our original view in the coordinate system of the
        // currently drawn ancestor.
        final int layerCount = ancestorsWithBackgrounds.size();
        for (int i = layerCount - 1; i >= 0; i--) {
            View ancestor = ancestorsWithBackgrounds.get(i);
            Pair<Integer, Integer> offsets = ancestorOffsets.get(i);

            canvas.translate(offsets.first, offsets.second);
            ancestor.getBackground().draw(canvas);
            canvas.translate(-offsets.first, -offsets.second);
        }

        return bitmap;
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
            int drawableWidth, int drawableHeight, boolean callSetBounds, @ColorInt int color,
            int allowedComponentVariance, boolean throwExceptionIfFails) {
            // Create a bitmap
            Bitmap bitmap = Bitmap.createBitmap(drawableWidth, drawableHeight,
                    Bitmap.Config.ARGB_8888);
            // Create a canvas that wraps the bitmap
            Canvas canvas = new Canvas(bitmap);
            if (callSetBounds) {
                // Configure the drawable to have bounds that match the passed size
                drawable.setBounds(0, 0, drawableWidth, drawableHeight);
            }
            // And ask the drawable to draw itself to the canvas / bitmap
            drawable.draw(canvas);

        try {
            assertAllPixelsOfColor(failMessagePrefix, bitmap, drawableWidth, drawableHeight, color,
                    allowedComponentVariance, throwExceptionIfFails);
        } finally {
            bitmap.recycle();
        }
    }

    /**
     * Checks whether all the pixels in the specified bitmap are of the same specified color.
     *
     * In case there is a color mismatch, the behavior of this method depends on the
     * <code>throwExceptionIfFails</code> parameter. If it is <code>true</code>, this method will
     * throw an <code>Exception</code> describing the mismatch. Otherwise this method will call
     * <code>Assert.fail</code> with detailed description of the mismatch.
     */
    public static void assertAllPixelsOfColor(String failMessagePrefix, @NonNull Bitmap bitmap,
            int bitmapWidth, int bitmapHeight, @ColorInt int color,
            int allowedComponentVariance, boolean throwExceptionIfFails) {
            int[] rowPixels = new int[bitmapWidth];
        for (int row = 0; row < bitmapHeight; row++) {
            bitmap.getPixels(rowPixels, 0, bitmapWidth, 0, row, bitmapWidth, 1);
            for (int column = 0; column < bitmapWidth; column++) {
                int sourceAlpha = Color.alpha(rowPixels[column]);
                int sourceRed = Color.red(rowPixels[column]);
                int sourceGreen = Color.green(rowPixels[column]);
                int sourceBlue = Color.blue(rowPixels[column]);

                int expectedAlpha = Color.alpha(color);
                int expectedRed = Color.red(color);
                int expectedGreen = Color.green(color);
                int expectedBlue = Color.blue(color);

                int varianceAlpha = Math.abs(sourceAlpha - expectedAlpha);
                int varianceRed = Math.abs(sourceRed - expectedRed);
                int varianceGreen = Math.abs(sourceGreen - expectedGreen);
                int varianceBlue = Math.abs(sourceBlue - expectedBlue);

                boolean isColorMatch = (varianceAlpha <= allowedComponentVariance)
                        && (varianceRed <= allowedComponentVariance)
                        && (varianceGreen <= allowedComponentVariance)
                        && (varianceBlue <= allowedComponentVariance);

                if (!isColorMatch) {
                    String mismatchDescription = failMessagePrefix
                            + ": expected all drawable colors to be ["
                            + expectedAlpha + "," + expectedRed + ","
                            + expectedGreen + "," + expectedBlue
                            + "] but at position (" + row + "," + column + ") out of ("
                            + bitmapWidth + "," + bitmapHeight + ") found ["
                            + sourceAlpha + "," + sourceRed + ","
                            + sourceGreen + "," + sourceBlue + "]";
                    if (throwExceptionIfFails) {
                        throw new RuntimeException(mismatchDescription);
                    } else {
                        Assert.fail(mismatchDescription);
                    }
                }
            }
        }
    }

    public static void waitForActivityDestroyed(BaseTestActivity activity) {
        while (!activity.isDestroyed()) {
            SystemClock.sleep(30);
        }
    }
}