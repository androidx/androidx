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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.Insets;
import androidx.core.os.BuildCompat;
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

    private final Object mDisplayCutout;

    /**
     * Creates a DisplayCutout instance.
     *
     * @param safeInsets the insets from each edge which avoid the display cutout as returned by
     *                   {@link #getSafeInsetTop()} etc.
     * @param boundingRects the bounding rects of the display cutouts as returned by
     *               {@link #getBoundingRects()} ()}.
     */
    // TODO(b/73953958): @VisibleForTesting(visibility = PRIVATE)
    public DisplayCutoutCompat(Rect safeInsets, List<Rect> boundingRects) {
        this(SDK_INT >= 28 ? new DisplayCutout(safeInsets, boundingRects) : null);
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
        if (BuildCompat.isAtLeastR()) {
            return new DisplayCutout(safeInsets.toPlatformInsets(), boundLeft,
                    boundTop, boundRight, boundBottom, waterfallInsets.toPlatformInsets());
        } else if (SDK_INT >= Build.VERSION_CODES.Q) {
            return new DisplayCutout(safeInsets.toPlatformInsets(), boundLeft,
                    boundTop, boundRight, boundBottom);
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
            return new DisplayCutout(safeInsetRect, boundingRects);
        } else {
            return null;
        }
    }

    private DisplayCutoutCompat(Object displayCutout) {
        mDisplayCutout = displayCutout;
    }

    /** Returns the inset from the top which avoids the display cutout in pixels. */
    public int getSafeInsetTop() {
        if (SDK_INT >= 28) {
            return ((DisplayCutout) mDisplayCutout).getSafeInsetTop();
        } else {
            return 0;
        }
    }

    /** Returns the inset from the bottom which avoids the display cutout in pixels. */
    public int getSafeInsetBottom() {
        if (SDK_INT >= 28) {
            return ((DisplayCutout) mDisplayCutout).getSafeInsetBottom();
        } else {
            return 0;
        }
    }

    /** Returns the inset from the left which avoids the display cutout in pixels. */
    public int getSafeInsetLeft() {
        if (SDK_INT >= 28) {
            return ((DisplayCutout) mDisplayCutout).getSafeInsetLeft();
        } else {
            return 0;
        }
    }

    /** Returns the inset from the right which avoids the display cutout in pixels. */
    public int getSafeInsetRight() {
        if (SDK_INT >= 28) {
            return ((DisplayCutout) mDisplayCutout).getSafeInsetRight();
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
            return ((DisplayCutout) mDisplayCutout).getBoundingRects();
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
        if (BuildCompat.isAtLeastR()) {
            return Insets.toCompatInsets(((DisplayCutout) mDisplayCutout).getWaterfallInsets());
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

    @Override
    public String toString() {
        return "DisplayCutoutCompat{" + mDisplayCutout + "}";
    }

    static DisplayCutoutCompat wrap(Object displayCutout) {
        return displayCutout == null ? null : new DisplayCutoutCompat(displayCutout);
    }

    @RequiresApi(api = 28)
    DisplayCutout unwrap() {
        return (DisplayCutout) mDisplayCutout;
    }
}
