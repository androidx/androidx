/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.pdf.models.Dimensions;

/**
 * Utilities related to {@link Rect}s.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class RectUtils {

    /** Scales the given Rect by the given scaling factor. Modifies (and returns) the given rect. */
    @NonNull
    public static Rect scale(@NonNull Rect rect, float scale) {
        return scale(rect, scale, scale);
    }

    /**
     *
     */
    @NonNull
    public static Rect scale(@NonNull Rect rect, float scaleX, float scaleY) {
        rect.set(
                floor(rect.left * scaleX),
                floor(rect.top * scaleY),
                ceil(rect.right * scaleX),
                ceil(rect.bottom * scaleY));
        return rect;
    }

    /**
     *
     */
    public static int area(@NonNull Rect rect) {
        return rect.width() * rect.height();
    }

    /**
     *
     */
    @NonNull
    public static Rect fromDimensions(@NonNull Dimensions dimensions) {
        return new Rect(0, 0, dimensions.getWidth(), dimensions.getHeight());
    }

    /**
     * Returns the contained, inner {@link Rect}, or the intersection of the two {@link Rect}s.
     *
     * <ul>
     *   <li>If one Rect contains the other, return the inner Rect.
     *   <li>If the two {@link Rect}s intersect, return the intersection.
     *   <li>Otherwise, return an empty (0x0) {@link Rect}.
     * </ul>
     */
    @NonNull
    public static Rect getInnerIntersection(@NonNull Rect rect1, @NonNull Rect rect2) {
        if (rect1.contains(rect2)) {
            return rect2;
        }
        if (rect2.contains(rect1)) {
            return rect1;
        }
        if (rect1.equals(rect2)) {
            return rect1;
        }
        Rect intersection = new Rect();
        if (intersection.setIntersect(rect1, rect2)) {
            return intersection;
        }
        return new Rect();
    }

    /**
     * Returns True if the width of an inner rectangle would equal the width of the outer rectangle
     * before the heights would equal, if the inner rectangle were to be zoomed in while maintaining
     * it's aspect ratio.
     */
    public static boolean widthIsLimitingDimension(
            float outerWidth, float outerHeight, float innerWidth, float innerHeight) {
        return (innerWidth / innerHeight) > (outerWidth / outerHeight);
    }

    private static int floor(float val) {
        return (int) Math.floor(val);
    }

    private static int ceil(float val) {
        return (int) Math.ceil(val);
    }

    private RectUtils() {
        // static utility.
    }
}
