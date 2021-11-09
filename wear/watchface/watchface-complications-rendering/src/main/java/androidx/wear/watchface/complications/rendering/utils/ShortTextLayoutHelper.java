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

import static androidx.wear.watchface.complications.rendering.utils.LayoutUtils.getBottomHalf;
import static androidx.wear.watchface.complications.rendering.utils.LayoutUtils.getCentralSquare;
import static androidx.wear.watchface.complications.rendering.utils.LayoutUtils.getLeftPart;
import static androidx.wear.watchface.complications.rendering.utils.LayoutUtils.getRightPart;
import static androidx.wear.watchface.complications.rendering.utils.LayoutUtils.getTopHalf;
import static androidx.wear.watchface.complications.rendering.utils.LayoutUtils.isWideRectangle;

import android.graphics.Rect;
import android.support.wearable.complications.ComplicationData;
import android.text.Layout;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Layout helper for {@link ComplicationData#TYPE_SHORT_TEXT}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ShortTextLayoutHelper extends LayoutHelper {

    /** Used to avoid allocating a Rect object whenever needed. */
    private final Rect mBounds = new Rect();

    @Override
    public void getIconBounds(@NonNull Rect outRect) {
        ComplicationData data = getComplicationData();
        if (data.getIcon() == null) {
            outRect.setEmpty();
        } else {
            getBounds(outRect);
            if (isWideRectangle(outRect)) {
                // Left square part of the inner bounds
                getLeftPart(outRect, outRect);
            } else {
                // Use top half of the central square
                getCentralSquare(outRect, outRect);
                getTopHalf(outRect, outRect);
                getCentralSquare(outRect, outRect);
            }
        }
    }

    @NonNull
    @Override
    public Layout.Alignment getShortTextAlignment() {
        ComplicationData data = getComplicationData();
        getBounds(mBounds);
        if (isWideRectangle(mBounds) && data.getIcon() != null) {
            // Wide rectangle with an icon available, align normal
            return Layout.Alignment.ALIGN_NORMAL;
        } else {
            // Otherwise, align center
            return Layout.Alignment.ALIGN_CENTER;
        }
    }

    @Override
    public int getShortTextGravity() {
        ComplicationData data = getComplicationData();
        if (data.getShortTitle() != null && data.getIcon() == null) {
            // If title is shown, align to bottom.
            return Gravity.BOTTOM;
        } else {
            // Otherwise, center text vertically
            return Gravity.CENTER_VERTICAL;
        }
    }

    @Override
    public void getShortTextBounds(@NonNull Rect outRect) {
        ComplicationData data = getComplicationData();
        getBounds(outRect);
        if (data.getIcon() != null) { // If there is an icon
            if (isWideRectangle(outRect)) {
                // Text to the right of icon
                getRightPart(outRect, outRect);
            } else {
                // Text on bottom half of central square
                getCentralSquare(outRect, outRect);
                getBottomHalf(outRect, outRect);
            }
        } else if (data.getShortTitle() != null) {
            // Text above title
            getTopHalf(outRect, outRect);
        }
        // Text only, no-op here.
    }

    @NonNull
    @Override
    public Layout.Alignment getShortTitleAlignment() {
        return getShortTextAlignment();
    }

    @Override
    public int getShortTitleGravity() {
        return Gravity.TOP;
    }

    @Override
    public void getShortTitleBounds(@NonNull Rect outRect) {
        ComplicationData data = getComplicationData();
        if (data.getIcon() != null || data.getShortTitle() == null) {
            outRect.setEmpty();
        } else {
            // Title is always on bottom half
            getBounds(outRect);
            getBottomHalf(outRect, outRect);
        }
    }
}
