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

package androidx.wear.watchface.complications.rendering.utils;

import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Utilities for calculations related to bounds.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class LayoutUtils {

    private LayoutUtils() {}

    /** Minimum aspect ratio for a rectangle to be qualified as wide rectangle. */
    public static final float WIDE_RECTANGLE_MINIMUM_ASPECT_RATIO = 2.0f;

    /**
     * Returns true if the aspect ratio of bounds is greater than {@link
     * #WIDE_RECTANGLE_MINIMUM_ASPECT_RATIO}.
     */
    public static boolean isWideRectangle(@NonNull Rect bounds) {
        return bounds.width() > bounds.height() * WIDE_RECTANGLE_MINIMUM_ASPECT_RATIO;
    }

    /**
     * Sets the output to the square that has the same left edge as input. Sets to empty if input
     * does not contain the square. This function is used to find icon / image position on wide
     * rectangles.
     */
    public static void getLeftPart(@NonNull Rect outRect, @NonNull Rect inRect) {
        if (inRect.width() < inRect.height()) {
            outRect.setEmpty(); // There is no left square
        } else {
            outRect.set(inRect.left, inRect.top, inRect.left + inRect.height(), inRect.bottom);
        }
    }

    /** Sets the output to the remaining part from left part of the input. */
    public static void getRightPart(@NonNull Rect outRect, @NonNull Rect inRect) {
        if (inRect.width() < inRect.height()) {
            outRect.set(inRect);
        } else {
            outRect.set(inRect.left + inRect.height(), inRect.top, inRect.right, inRect.bottom);
        }
    }

    /** Sets the output to the top half of the input. */
    public static void getTopHalf(@NonNull Rect outRect, @NonNull Rect inRect) {
        outRect.set(inRect.left, inRect.top, inRect.right, (inRect.top + inRect.bottom) / 2);
    }

    /** Sets the output to the bottom half of the input. */
    public static void getBottomHalf(@NonNull Rect outRect, @NonNull Rect inRect) {
        outRect.set(inRect.left, (inRect.top + inRect.bottom) / 2, inRect.right, inRect.bottom);
    }

    /**
     * Sets the output to the biggest square that fits in input rectangle and has the same center,
     * works for all aspect ratios.
     */
    public static void getCentralSquare(@NonNull Rect outRect, @NonNull Rect inRect) {
        int edge = Math.min(inRect.width(), inRect.height());
        outRect.set(
                inRect.centerX() - edge / 2,
                inRect.centerY() - edge / 2,
                inRect.centerX() + edge / 2,
                inRect.centerY() + edge / 2);
    }

    /**
     * Sets the output to the rectangle obtained by scaling input's edges by a given fraction but
     * keeping its center the same.
     */
    public static void scaledAroundCenter(
            @NonNull Rect outRect, @NonNull Rect inRect, float sizeFraction) {
        outRect.set(inRect);
        float paddingFraction = 0.5f - sizeFraction / 2;
        outRect.inset(
                (int) (outRect.width() * paddingFraction),
                (int) (outRect.height() * paddingFraction));
    }

    /** Fits square bounds inside a container and tries to keep its center the same. */
    public static void fitSquareToBounds(@NonNull Rect squareBounds, @NonNull Rect container) {
        if (squareBounds.isEmpty()) {
            return;
        }
        int originalCenterX = squareBounds.centerX();
        int originalCenterY = squareBounds.centerY();
        if (!squareBounds.intersect(container)) {
            squareBounds.setEmpty();
            return;
        }
        LayoutUtils.getCentralSquare(squareBounds, squareBounds);
        // Try to move to original center
        int dx = originalCenterX - squareBounds.centerX();
        int dy = originalCenterY - squareBounds.centerY();
        squareBounds.offset(dx, dy);
        // If it causes an overflow, move it back
        if (!container.contains(squareBounds)) {
            squareBounds.offset(-dx, -dy);
        }
    }

    /**
     * Sets the output to inscribed rectangle of the rounded rectangle specified by input rectangle
     * and radius.
     */
    public static void getInnerBounds(@NonNull Rect outRect, @NonNull Rect inRect, float radius) {
        outRect.set(inRect);
        int padding = (int) Math.ceil((Math.sqrt(2.0f) - 1.0f) * radius);
        outRect.inset(padding, padding);
    }
}
