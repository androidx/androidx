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

package androidx.appcompat.testutils;

import static org.junit.Assert.fail;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.TintTypedArray;
import androidx.core.util.Pair;

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
                @ColorInt int colorAtCurrPixel = rowPixels[column];
                if (!areColorsTheSameWithTolerance(color, colorAtCurrPixel,
                        allowedComponentVariance)) {
                    String mismatchDescription = failMessagePrefix
                            + ": expected all drawable colors to be "
                            + formatColorToHex(color)
                            + " but at position (" + row + "," + column + ") out of ("
                            + bitmapWidth + "," + bitmapHeight + ") found "
                            + formatColorToHex(colorAtCurrPixel);
                    if (throwExceptionIfFails) {
                        throw new RuntimeException(mismatchDescription);
                    } else {
                        fail(mismatchDescription);
                    }
                }
            }
        }
    }

    /**
     * Checks whether the center pixel in the specified drawable is of the same specified color.
     *
     * In case there is a color mismatch, the behavior of this method depends on the
     * <code>throwExceptionIfFails</code> parameter. If it is <code>true</code>, this method will
     * throw an <code>Exception</code> describing the mismatch. Otherwise this method will call
     * <code>Assert.fail</code> with detailed description of the mismatch.
     */
    public static void assertCenterPixelOfColor(String failMessagePrefix, @NonNull Drawable drawable,
            int drawableWidth, int drawableHeight, boolean callSetBounds, @ColorInt int color,
            int allowedComponentVariance, boolean throwExceptionIfFails) {
        // Create a bitmap
        Bitmap bitmap = Bitmap.createBitmap(drawableWidth, drawableHeight, Bitmap.Config.ARGB_8888);
        // Create a canvas that wraps the bitmap
        Canvas canvas = new Canvas(bitmap);
        if (callSetBounds) {
            // Configure the drawable to have bounds that match the passed size
            drawable.setBounds(0, 0, drawableWidth, drawableHeight);
        }
        // And ask the drawable to draw itself to the canvas / bitmap
        drawable.draw(canvas);

        try {
            assertCenterPixelOfColor(failMessagePrefix, bitmap, color, allowedComponentVariance,
                    throwExceptionIfFails);
        } finally {
            bitmap.recycle();
        }
    }

    /**
     * Checks whether the center pixel in the specified bitmap is of the same specified color.
     *
     * In case there is a color mismatch, the behavior of this method depends on the
     * <code>throwExceptionIfFails</code> parameter. If it is <code>true</code>, this method will
     * throw an <code>Exception</code> describing the mismatch. Otherwise this method will call
     * <code>Assert.fail</code> with detailed description of the mismatch.
     */
    public static void assertCenterPixelOfColor(String failMessagePrefix, @NonNull Bitmap bitmap,
            @ColorInt int color, int allowedComponentVariance, boolean throwExceptionIfFails) {
        final int centerX = bitmap.getWidth() / 2;
        final int centerY = bitmap.getHeight() / 2;
        final @ColorInt int colorAtCenterPixel = bitmap.getPixel(centerX, centerY);
        if (!areColorsTheSameWithTolerance(color, colorAtCenterPixel,
                allowedComponentVariance)) {
            String mismatchDescription = failMessagePrefix
                    + ": expected all drawable colors to be "
                    + formatColorToHex(color)
                    + " but at position (" + centerX + "," + centerY + ") out of ("
                    + bitmap.getWidth() + "," + bitmap.getHeight() + ") found "
                    + formatColorToHex(colorAtCenterPixel);
            if (throwExceptionIfFails) {
                throw new RuntimeException(mismatchDescription);
            } else {
                fail(mismatchDescription);
            }
        }
    }

    /**
     * Formats the passed integer-packed color into the #AARRGGBB format.
     */
    private static String formatColorToHex(@ColorInt int color) {
        return String.format("#%08X", (0xFFFFFFFF & color));
    }

    /**
     * Compares two integer-packed colors to be equal, each component within the specified
     * allowed variance. Returns <code>true</code> if the two colors are sufficiently equal
     * and <code>false</code> otherwise.
     */
    private static boolean areColorsTheSameWithTolerance(@ColorInt int expectedColor,
            @ColorInt int actualColor, int allowedComponentVariance) {
        int sourceAlpha = Color.alpha(actualColor);
        int sourceRed = Color.red(actualColor);
        int sourceGreen = Color.green(actualColor);
        int sourceBlue = Color.blue(actualColor);

        int expectedAlpha = Color.alpha(expectedColor);
        int expectedRed = Color.red(expectedColor);
        int expectedGreen = Color.green(expectedColor);
        int expectedBlue = Color.blue(expectedColor);

        int varianceAlpha = Math.abs(sourceAlpha - expectedAlpha);
        int varianceRed = Math.abs(sourceRed - expectedRed);
        int varianceGreen = Math.abs(sourceGreen - expectedGreen);
        int varianceBlue = Math.abs(sourceBlue - expectedBlue);

        boolean isColorMatch = (varianceAlpha <= allowedComponentVariance)
                && (varianceRed <= allowedComponentVariance)
                && (varianceGreen <= allowedComponentVariance)
                && (varianceBlue <= allowedComponentVariance);

        return isColorMatch;
    }

    public static void waitForActivityDestroyed(BaseTestActivity activity) {
        while (!activity.isDestroyed()) {
            SystemClock.sleep(30);
        }
    }

    public static int getThemeAttrColor(Context context, int attr) {
        final int[] attrs = { attr };
        TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, null, attrs);
        try {
            return a.getColor(0, 0);
        } finally {
            a.recycle();
        }
    }

    /**
     * Emulates a tap on a point relative to the top-left corner of the passed {@link View}. Offset
     * parameters are used to compute the final screen coordinates of the tap point.
     *
     * @param instrumentation the instrumentation used to run the test
     * @param anchorView the anchor view to determine the tap location on the screen
     * @param offsetX extra X offset for the tap
     * @param offsetY extra Y offset for the tap
     */
    public static void emulateTapOnView(Instrumentation instrumentation, View anchorView,
            int offsetX, int offsetY) {
        final int touchSlop = ViewConfiguration.get(anchorView.getContext()).getScaledTouchSlop();
        // Get anchor coordinates on the screen
        final int[] viewOnScreenXY = new int[2];
        anchorView.getLocationOnScreen(viewOnScreenXY);
        int xOnScreen = viewOnScreenXY[0] + offsetX;
        int yOnScreen = viewOnScreenXY[1] + offsetY;
        final long downTime = SystemClock.uptimeMillis();

        injectDownEvent(instrumentation, downTime, xOnScreen, yOnScreen);
        injectMoveEventForTap(instrumentation, downTime, touchSlop, xOnScreen, yOnScreen);
        injectUpEvent(instrumentation, downTime, false, xOnScreen, yOnScreen);

        // Wait for the system to process all events in the queue
        instrumentation.waitForIdleSync();
    }

    private static long injectDownEvent(Instrumentation instrumentation, long downTime,
            int xOnScreen, int yOnScreen) {
        MotionEvent eventDown = MotionEvent.obtain(
                downTime, downTime, MotionEvent.ACTION_DOWN, xOnScreen, yOnScreen, 1);
        eventDown.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        instrumentation.sendPointerSync(eventDown);
        eventDown.recycle();
        return downTime;
    }

    private static void injectMoveEventForTap(Instrumentation instrumentation, long downTime,
            int touchSlop, int xOnScreen, int yOnScreen) {
        MotionEvent eventMove = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_MOVE,
                xOnScreen + (touchSlop / 2.0f), yOnScreen + (touchSlop / 2.0f), 1);
        eventMove.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        instrumentation.sendPointerSync(eventMove);
        eventMove.recycle();
    }


    private static void injectUpEvent(Instrumentation instrumentation, long downTime,
            boolean useCurrentEventTime, int xOnScreen, int yOnScreen) {
        long eventTime = useCurrentEventTime ? SystemClock.uptimeMillis() : downTime;
        MotionEvent eventUp = MotionEvent.obtain(
                downTime, eventTime, MotionEvent.ACTION_UP, xOnScreen, yOnScreen, 1);
        eventUp.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        instrumentation.sendPointerSync(eventUp);
        eventUp.recycle();
    }
}