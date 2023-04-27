/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.view;

import static android.os.Build.VERSION.SDK_INT;

import android.graphics.Rect;
import android.os.Build;
import android.view.DisplayCutout;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.Insets;
import androidx.core.util.ObjectsCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Represents the area of the display that is not functional for displaying content.
 *
 * <p>{@code DisplayCutoutCompat} instances are immutable.
 */
public final class DisplayCutoutCompat {

    private final DisplayCutout mDisplayCutout;

    /**
     * Creates a DisplayCutout instance.
     *
     * @param safeInsets the insets from each edge which avoid the display cutout as returned by
     *                   {@link #getSafeInsetTop()} etc.
     * @param boundingRects the bounding rects of the display cutouts as returned by
     *               {@link #getBoundingRects()} ()}.
     */
    public DisplayCutoutCompat(@Nullable Rect safeInsets, @Nullable List<Rect> boundingRects) {
        this(SDK_INT >= 28 ? Api28Impl.createDisplayCutout(safeInsets, boundingRects) : null);
    }

    /**
     * Creates a DisplayCutout instance.
     *
     * @param safeInsets the insets from each edge which avoid the display cutout as returned by
     *                   {@link #getSafeInsetTop()} etc.
     * @param boundLeft the left bounding rect of the display cutout in pixels. If null is passed,
     *                  it's treated as an empty rectangle (0,0)-(0,0).
     * @param boundTop the top bounding rect of the display cutout in pixels. If null is passed,
     *                  it's treated as an empty rectangle (0,0)-(0,0).
     * @param boundRight the right bounding rect of the display cutout in pixels. If null is
     *                  passed, it's treated as an empty rectangle (0,0)-(0,0).
     * @param boundBottom the bottom bounding rect of the display cutout in pixels. If null is
     *                   passed, it's treated as an empty rectangle (0,0)-(0,0).
     * @param waterfallInsets the insets for the curved areas in waterfall display.
     */
    public DisplayCutoutCompat(@NonNull Insets safeInsets, @Nullable Rect boundLeft,
            @Nullable Rect boundTop, @Nullable Rect boundRight, @Nullable Rect boundBottom,
            @NonNull Insets waterfallInsets) {
        this(constructDisplayCutout(safeInsets, boundLeft, boundTop, boundRight, boundBottom,
                waterfallInsets));
    }

    private static DisplayCutout constructDisplayCutout(@NonNull Insets safeInsets,
            @Nullable Rect boundLeft, @Nullable Rect boundTop, @Nullable Rect boundRight,
            @Nullable Rect boundBottom, @NonNull Insets waterfallInsets) {
        if (SDK_INT >= 30) {
            return Api30Impl.createDisplayCutout(safeInsets.toPlatformInsets(), boundLeft, boundTop,
                    boundRight, boundBottom, waterfallInsets.toPlatformInsets());
        } else if (SDK_INT >= Build.VERSION_CODES.Q) {
            return Api29Impl.createDisplayCutout(safeInsets.toPlatformInsets(), boundLeft, boundTop,
                    boundRight, boundBottom);
        } else if (SDK_INT >= Build.VERSION_CODES.P) {
            final Rect safeInsetRect = new Rect(safeInsets.left, safeInsets.top, safeInsets.right,
                    safeInsets.bottom);
            final ArrayList<Rect> boundingRects = new ArrayList<>();
            if (boundLeft != null) {
                boundingRects.add(boundLeft);
            }
            if (boundTop != null) {
                boundingRects.add(boundTop);
            }
            if (boundRight != null) {
                boundingRects.add(boundRight);
            }
            if (boundBottom != null) {
                boundingRects.add(boundBottom);
            }
            return Api28Impl.createDisplayCutout(safeInsetRect, boundingRects);
        } else {
            return null;
        }
    }

    private DisplayCutoutCompat(DisplayCutout displayCutout) {
        mDisplayCutout = displayCutout;
    }

    /** Returns the inset from the top which avoids the display cutout in pixels. */
    public int getSafeInsetTop() {
        if (SDK_INT >= 28) {
            return Api28Impl.getSafeInsetTop(mDisplayCutout);
        } else {
            return 0;
        }
    }

    /** Returns the inset from the bottom which avoids the display cutout in pixels. */
    public int getSafeInsetBottom() {
        if (SDK_INT >= 28) {
            return Api28Impl.getSafeInsetBottom(mDisplayCutout);
        } else {
            return 0;
        }
    }

    /** Returns the inset from the left which avoids the display cutout in pixels. */
    public int getSafeInsetLeft() {
        if (SDK_INT >= 28) {
            return Api28Impl.getSafeInsetLeft(mDisplayCutout);
        } else {
            return 0;
        }
    }

    /** Returns the inset from the right which avoids the display cutout in pixels. */
    public int getSafeInsetRight() {
        if (SDK_INT >= 28) {
            return Api28Impl.getSafeInsetRight(mDisplayCutout);
        } else {
            return 0;
        }
    }

    /**
     * Returns a list of {@code Rect}s, each of which is the bounding rectangle for a non-functional
     * area on the display.
     *
     * There will be at most one non-functional area per short edge of the device, and none on
     * the long edges.
     *
     * @return a list of bounding {@code Rect}s, one for each display cutout area.
     */
    @NonNull
    public List<Rect> getBoundingRects() {
        if (SDK_INT >= 28) {
            return Api28Impl.getBoundingRects(mDisplayCutout);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Returns the insets representing the curved areas of a waterfall display.
     *
     * A waterfall display has curved areas along the edges of the screen. Apps should be careful
     * when showing UI and handling touch input in those insets because the curve may impair
     * legibility and can frequently lead to unintended touch inputs.
     *
     * @return the insets for the curved areas of a waterfall display in pixels or {@code
     * Insets.NONE} if there are no curved areas or they don't overlap with the window.
     */
    @NonNull
    public Insets getWaterfallInsets() {
        if (SDK_INT >= 30) {
            return Insets.toCompatInsets(Api30Impl.getWaterfallInsets(mDisplayCutout));
        } else {
            return Insets.NONE;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DisplayCutoutCompat other = (DisplayCutoutCompat) o;
        return ObjectsCompat.equals(mDisplayCutout, other.mDisplayCutout);
    }

    @Override
    public int hashCode() {
        return mDisplayCutout == null ? 0 : mDisplayCutout.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return "DisplayCutoutCompat{" + mDisplayCutout + "}";
    }

    static DisplayCutoutCompat wrap(DisplayCutout displayCutout) {
        return displayCutout == null ? null : new DisplayCutoutCompat(displayCutout);
    }

    @RequiresApi(28)
    DisplayCutout unwrap() {
        return mDisplayCutout;
    }

    @RequiresApi(28)
    static class Api28Impl {
        private Api28Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static DisplayCutout createDisplayCutout(Rect safeInsets, List<Rect> boundingRects) {
            return new DisplayCutout(safeInsets, boundingRects);
        }

        @DoNotInline
        static int getSafeInsetTop(DisplayCutout displayCutout) {
            return displayCutout.getSafeInsetTop();
        }

        @DoNotInline
        static int getSafeInsetBottom(DisplayCutout displayCutout) {
            return displayCutout.getSafeInsetBottom();
        }

        @DoNotInline
        static int getSafeInsetLeft(DisplayCutout displayCutout) {
            return displayCutout.getSafeInsetLeft();
        }

        @DoNotInline
        static int getSafeInsetRight(DisplayCutout displayCutout) {
            return displayCutout.getSafeInsetRight();
        }

        @DoNotInline
        static List<Rect> getBoundingRects(DisplayCutout displayCutout) {
            return displayCutout.getBoundingRects();
        }
    }

    @RequiresApi(30)
    static class Api30Impl {
        private Api30Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static DisplayCutout createDisplayCutout(android.graphics.Insets safeInsets, Rect boundLeft,
                Rect boundTop, Rect boundRight, Rect boundBottom,
                android.graphics.Insets waterfallInsets) {
            return new DisplayCutout(safeInsets, boundLeft, boundTop, boundRight, boundBottom,
                    waterfallInsets);
        }

        @DoNotInline
        static android.graphics.Insets getWaterfallInsets(DisplayCutout displayCutout) {
            return displayCutout.getWaterfallInsets();
        }
    }

    @RequiresApi(29)
    static class Api29Impl {
        private Api29Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static DisplayCutout createDisplayCutout(android.graphics.Insets safeInsets, Rect boundLeft,
                Rect boundTop, Rect boundRight, Rect boundBottom) {
            return new DisplayCutout(safeInsets, boundLeft, boundTop, boundRight, boundBottom);
        }
    }
}
