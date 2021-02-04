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

package androidx.window;

import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * A class containing static methods for creating different window bound types. Test methods are
 * shared between the unit tests and the instrumentation tests.
 */
final class TestBoundsUtil {

    private TestBoundsUtil() { }

    /**
     * @param windowBounds the bounds for a window contain a valid fold.
     * @return {@link Rect} that is a valid fold bound within the given window.
     */
    public static Rect validFoldBound(Rect windowBounds) {
        int verticalMid = windowBounds.height() / 2;
        return new Rect(0, verticalMid, windowBounds.width(), verticalMid);
    }

    /**
     * @return {@link Rect} containing the invalid zero bounds.
     */
    static Rect invalidZeroBound() {
        return new Rect();
    }

    /**
     * @param windowBounds the bounds for a window contain an invalid fold.
     * @return {@link Rect} for bounds where the width is shorter than the window width.
     */
    static Rect invalidBoundShortWidth(Rect windowBounds) {
        return new Rect(0, 0, windowBounds.width() / 2, 0);
    }

    /**
     * @param windowBounds the bounds for a window contain an invalid fold.
     * @return {@link Rect} for bounds where the height is shorter than the window height.
     */
    static Rect invalidBoundShortHeight(Rect windowBounds) {
        return new Rect(0, 0, 0, windowBounds.height() / 2);
    }

    /**
     * @param windowBounds the bounds for a window contain an invalid fold.
     * @return a {@link List} of {@link Rect} of invalid bounds for fold features
     */
    static List<Rect> invalidFoldBounds(Rect windowBounds) {
        List<Rect> badBounds = invalidHingeBounds(windowBounds);
        Rect nonEmptySmallRect = new Rect(0, 0, 1, 1);
        badBounds.add(nonEmptySmallRect);
        return badBounds;
    }

    /**
     * @param windowBounds the bounds for a window contain an invalid fold.
     * @return a {@link List} of {@link Rect} of invalid bounds for hinge features
     */
    static List<Rect> invalidHingeBounds(Rect windowBounds) {
        List<Rect> badBounds = new ArrayList<>();

        badBounds.add(invalidZeroBound());
        badBounds.add(invalidBoundShortWidth(windowBounds));
        badBounds.add(invalidBoundShortHeight(windowBounds));

        return badBounds;
    }
}
