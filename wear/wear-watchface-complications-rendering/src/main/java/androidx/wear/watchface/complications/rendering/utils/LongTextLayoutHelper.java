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
 * Layout helper for {@link ComplicationData#TYPE_LONG_TEXT}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class LongTextLayoutHelper extends LayoutHelper {

    /** Used to avoid allocating a Rect object whenever needed. */
    private final Rect mBounds = new Rect();

    private boolean shouldShowTextOnly(@NonNull Rect bounds) {
        // Should show only text if there's no visual available or rectangle is not wide enough
        ComplicationData data = getComplicationData();
        return (data.getIcon() == null && data.getSmallImage() == null) || !isWideRectangle(bounds);
    }

    @Override
    public void getIconBounds(@NonNull Rect outRect) {
        ComplicationData data = getComplicationData();
        getBounds(outRect);
        if (data.getIcon() == null || data.getSmallImage() != null || shouldShowTextOnly(outRect)) {
            outRect.setEmpty();
        } else {
            // Show icon on the left part of bounds
            getLeftPart(outRect, outRect);
        }
    }

    @Override
    public void getSmallImageBounds(@NonNull Rect outRect) {
        ComplicationData data = getComplicationData();
        getBounds(outRect);
        if (data.getSmallImage() == null || shouldShowTextOnly(outRect)) {
            outRect.setEmpty();
        } else {
            // Show image on the left part of bounds
            getLeftPart(outRect, outRect);
        }
    }

    @Override
    public void getLongTextBounds(@NonNull Rect outRect) {
        ComplicationData data = getComplicationData();
        getBounds(outRect);
        if (shouldShowTextOnly(outRect)) {
            if (data.getLongTitle() != null) {
                // Title is available, use top half
                getTopHalf(outRect, outRect);
            }
            // Title is not available, fill the area
            // No-op here.
        } else {
            // To the right of the icon/small image
            if (data.getLongTitle() == null) {
                // Full height if there's no title
                getRightPart(outRect, outRect);
            } else {
                // Only top half if there's title
                getRightPart(outRect, outRect);
                getTopHalf(outRect, outRect);
            }
        }
    }

    @NonNull
    @Override
    public Layout.Alignment getLongTextAlignment() {
        getBounds(mBounds);
        if (shouldShowTextOnly(mBounds)) {
            return Layout.Alignment.ALIGN_CENTER;
        } else {
            return Layout.Alignment.ALIGN_NORMAL;
        }
    }

    @Override
    public int getLongTextGravity() {
        ComplicationData data = getComplicationData();
        // Title is always shown if available
        if (data.getLongTitle() == null) {
            return Gravity.CENTER_VERTICAL;
        } else {
            return Gravity.BOTTOM;
        }
    }

    @Override
    public void getLongTitleBounds(@NonNull Rect outRect) {
        ComplicationData data = getComplicationData();
        getBounds(outRect);
        if (data.getLongTitle() == null) {
            outRect.setEmpty();
        } else {
            if (shouldShowTextOnly(outRect)) {
                getBottomHalf(outRect, outRect);
            } else {
                getRightPart(outRect, outRect);
                getBottomHalf(outRect, outRect);
            }
        }
    }

    @NonNull
    @Override
    public Layout.Alignment getLongTitleAlignment() {
        return getLongTextAlignment();
    }

    @Override
    public int getLongTitleGravity() {
        return Gravity.TOP;
    }
}
