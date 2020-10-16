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

package androidx.window;

import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * A utility class to provide bounds for a display feature
 */
class TestBoundUtil {

    public static Rect validFoldBound(Rect windowBounds) {
        return new Rect(windowBounds.left, windowBounds.top, windowBounds.right, 0);
    }

    public static Rect invalidZeroBound() {
        return new Rect();
    }

    public static Rect invalidBoundShortWidth(Rect windowBounds) {
        return new Rect(windowBounds.left, windowBounds.top, windowBounds.right / 2, 2);
    }

    public static Rect invalidBoundShortHeightHeight(Rect windowBounds) {
        return new Rect(windowBounds.left, windowBounds.top, 2, windowBounds.bottom / 2);
    }

    private static List<Rect> coreInvalidBounds(Rect windowBounds) {
        List<Rect> badBounds = new ArrayList<>();

        badBounds.add(invalidZeroBound());
        badBounds.add(invalidBoundShortWidth(windowBounds));
        badBounds.add(invalidBoundShortHeightHeight(windowBounds));

        return badBounds;
    }

    public static List<Rect> invalidFoldBounds(Rect windowBounds) {
        List<Rect> badBounds = coreInvalidBounds(windowBounds);
        Rect nonEmptySmallRect = new Rect(0, 1, 1, 1);
        badBounds.add(nonEmptySmallRect);
        return badBounds;
    }

    public static List<Rect> invalidHingeBounds(Rect windowBounds) {
        return coreInvalidBounds(windowBounds);
    }
}
