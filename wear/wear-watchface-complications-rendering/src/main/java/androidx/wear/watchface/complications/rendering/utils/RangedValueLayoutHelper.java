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
import static androidx.wear.watchface.complications.rendering.utils.LayoutUtils.scaledAroundCenter;

import android.graphics.Rect;
import android.support.wearable.complications.ComplicationData;
import android.text.Layout;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Layout helper for {@link ComplicationData#TYPE_RANGED_VALUE}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class RangedValueLayoutHelper extends LayoutHelper {

    /** As ranged value indicator is circle, this is used to calculate the inner square of it. */
    private static final float INNER_SQUARE_SIZE_FRACTION = (float) (1.0f / Math.sqrt(2.0f));

    /** Padding applied to inner square of the ranged value indicator. */
    private static final float INNER_SQUARE_PADDING_FRACTION = 0.15f;

    /** Padding applied to icon inside the ranged value indicator. */
    private static final float ICON_PADDING_FRACTION = 0.15f;

    /** Used to apply padding to ranged value indicator. */
    private static final float RANGED_VALUE_SIZE_FRACTION = 0.95f;

    /** Used to draw a short text complication inside ranged value for non-wide rectangles. */
    private final ShortTextLayoutHelper mShortTextLayoutHelper = new ShortTextLayoutHelper();

    /** Used to avoid calculating inner square of the ranged value every time it's needed. */
    private final Rect mRangedValueInnerSquare = new Rect();

    /** Used to avoid allocating a Rect object whenever needed. */
    private final Rect mBounds = new Rect();

    private void updateShortTextLayoutHelper() {
        if (getComplicationData() != null) {
            getRangedValueBounds(mRangedValueInnerSquare);
            scaledAroundCenter(
                    mRangedValueInnerSquare,
                    mRangedValueInnerSquare,
                    (1 - INNER_SQUARE_PADDING_FRACTION * 2) * INNER_SQUARE_SIZE_FRACTION);
            mShortTextLayoutHelper.update(
                    mRangedValueInnerSquare.width(),
                    mRangedValueInnerSquare.height(),
                    getComplicationData());
        }
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        updateShortTextLayoutHelper();
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        updateShortTextLayoutHelper();
    }

    @Override
    public void setComplicationData(@Nullable ComplicationData data) {
        super.setComplicationData(data);
        updateShortTextLayoutHelper();
    }

    @Override
    public void getRangedValueBounds(@NonNull Rect outRect) {
        getBounds(outRect);
        ComplicationData data = getComplicationData();
        if (data.getShortText() == null || !isWideRectangle(outRect)) {
            getCentralSquare(outRect, outRect);
            scaledAroundCenter(outRect, outRect, RANGED_VALUE_SIZE_FRACTION);
        } else {
            getLeftPart(outRect, outRect);
            scaledAroundCenter(outRect, outRect, RANGED_VALUE_SIZE_FRACTION);
        }
    }

    @Override
    public void getIconBounds(@NonNull Rect outRect) {
        ComplicationData data = getComplicationData();
        if (data.getIcon() == null) {
            outRect.setEmpty();
        } else {
            getBounds(outRect);
            if (data.getShortText() == null || isWideRectangle(outRect)) {
                // Show only an icon inside ranged value indicator
                scaledAroundCenter(outRect, mRangedValueInnerSquare, 1 - ICON_PADDING_FRACTION * 2);
            } else {
                // Draw a short text complication inside ranged value bounds
                mShortTextLayoutHelper.getIconBounds(outRect);
                outRect.offset(mRangedValueInnerSquare.left, mRangedValueInnerSquare.top);
            }
        }
    }

    @NonNull
    @Override
    public Layout.Alignment getShortTextAlignment() {
        getBounds(mBounds);
        if (isWideRectangle(mBounds)) {
            return Layout.Alignment.ALIGN_NORMAL;
        } else {
            // Draw a short text complication inside ranged value bounds
            return mShortTextLayoutHelper.getShortTextAlignment();
        }
    }

    @Override
    public int getShortTextGravity() {
        ComplicationData data = getComplicationData();
        getBounds(mBounds);
        if (isWideRectangle(mBounds)) {
            if (data.getShortTitle() != null) {
                return Gravity.BOTTOM;
            } else {
                return Gravity.CENTER_VERTICAL;
            }
        } else {
            // Draw as a square short text complication inside ranged value bounds
            return mShortTextLayoutHelper.getShortTextGravity();
        }
    }

    @Override
    public void getShortTextBounds(@NonNull Rect outRect) {
        ComplicationData data = getComplicationData();
        if (data.getShortText() == null) {
            outRect.setEmpty();
        } else {
            getBounds(outRect);
            if (isWideRectangle(outRect)) {
                if (data.getShortTitle() == null || data.getIcon() != null) {
                    getRightPart(outRect, outRect);
                } else {
                    getRightPart(outRect, outRect);
                    getTopHalf(outRect, outRect);
                }
            } else {
                // Draw a short text complication inside ranged value bounds
                mShortTextLayoutHelper.getShortTextBounds(outRect);
                outRect.offset(mRangedValueInnerSquare.left, mRangedValueInnerSquare.top);
            }
        }
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
        // As title is meaningless without text, return empty rectangle in that case too.
        if (data.getShortTitle() == null || data.getShortText() == null) {
            outRect.setEmpty();
        } else {
            getBounds(outRect);
            if (isWideRectangle(outRect)) {
                getRightPart(outRect, outRect);
                getBottomHalf(outRect, outRect);
            } else {
                // Draw a short text complication inside ranged value bounds
                mShortTextLayoutHelper.getShortTitleBounds(outRect);
                outRect.offset(mRangedValueInnerSquare.left, mRangedValueInnerSquare.top);
            }
        }
    }
}
