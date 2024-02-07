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

package androidx.wear.protolayout.renderer.inflater;

import static java.lang.Math.abs;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.style.LeadingMarginSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Helper class fixing the indentation for the last broken line by translating the canvas in the
 * opposite direction.
 *
 * <p>Applying letter spacing, center alignment and ellipsis to a text causes incorrect indentation
 * of the truncated line. For example, the last line is indented in a way where the start of the
 * line is outside of the boundaries of text.
 *
 * <p>It should be applied to a text only when those three attributes are set.
 */
// Branched from androidx.compose.ui.text.android.style.IndentationFixSpan
class IndentationFixSpan implements LeadingMarginSpan {
    @VisibleForTesting static final String ELLIPSIS_CHAR = "…";
    @Nullable private Layout mOverrideLayoutForMeasuring = null;

    @Override
    public int getLeadingMargin(boolean first) {
        return 0;
    }

    /**
     * Creates an instance of {@link IndentationFixSpan} used for fixing the text in {@link
     * android.widget.TextView} when ellipsize, letter spacing and alignment are set.
     */
    IndentationFixSpan() {}

    /**
     * Creates an instance of {@link IndentationFixSpan} used for fixing the text in {@link
     * android.widget.TextView} when ellipsize, letter spacing and alignment are set.
     *
     * @param layout The {@link StaticLayout} used for measuring how much Canvas should be rotated
     *               in {@link #drawLeadingMargin}.
     */
    IndentationFixSpan(@NonNull StaticLayout layout) {
        this.mOverrideLayoutForMeasuring = layout;
    }

    /**
     * See {@link LeadingMarginSpan#drawLeadingMargin}.
     *
     * <p>If {@code IndentationFixSpan(StaticLayout)} has been used, the given {@code layout} would
     * be ignored when doing measurements.
     */
    @Override
    public void drawLeadingMargin(
            @NonNull Canvas canvas,
            @Nullable Paint paint,
            int x,
            int dir,
            int top,
            int baseline,
            int bottom,
            @Nullable CharSequence text,
            int start,
            int end,
            boolean first,
            @Nullable Layout layout) {
        // If StaticLayout has been provided, we should use that one for measuring instead of the
        // passed in one.
        if (mOverrideLayoutForMeasuring != null) {
            layout = mOverrideLayoutForMeasuring;
        }

        if (layout == null || paint == null) {
            return;
        }

        float padding = calculatePadding(paint, start, layout);

        if (padding != 0f) {
            canvas.translate(padding, 0f);
        }
    }

    /** Calculates the extra padding on ellipsized last line. Otherwise, returns 0. */
    @VisibleForTesting
    static float calculatePadding(@NonNull Paint paint, int start, @NonNull Layout layout) {
        int lineIndex = layout.getLineForOffset(start);

        // No action needed if line is not ellipsized and that is not the last line.
        if (lineIndex != layout.getLineCount() - 1 || !isLineEllipsized(layout, lineIndex)) {
            return 0f;
        }

        return layout.getParagraphDirection(lineIndex) == Layout.DIR_LEFT_TO_RIGHT
                ? getEllipsizedPaddingForLtr(layout, lineIndex, paint)
                : getEllipsizedPaddingForRtl(layout, lineIndex, paint);
    }

    /** Returns whether the given line is ellipsized. */
    private static boolean isLineEllipsized(@NonNull Layout layout, int lineIndex) {
        return layout.getEllipsisCount(lineIndex) > 0;
    }

    /**
     * Gets the extra padding that is on the left when line is ellipsized on left-to-right layout
     * direction. Otherwise, returns 0.
     */
    private static float getEllipsizedPaddingForLtr(
            @NonNull Layout layout, int lineIndex, @NonNull Paint paint) {
        float lineLeft = layout.getLineLeft(lineIndex);

        if (lineLeft >= 0) {
            return 0;
        }

        int ellipsisIndex = getEllipsisIndex(layout, lineIndex);
        float horizontal = getHorizontalPosition(layout, ellipsisIndex);
        float length = (horizontal - lineLeft) + paint.measureText(ELLIPSIS_CHAR);
        float divideFactor = getDivideFactor(layout, lineIndex);

        return abs(lineLeft) + ((layout.getWidth() - length) / divideFactor);
    }

    /**
     * Gets the extra padding that is on the right when line is ellipsized on right-to-left layout
     * direction. Otherwise, returns 0.
     */
    // TODO: b/323180070 - Investigate how to improve this so that text doesn't get clipped on large
    // sizes as there is a bug in platform with letter spacing on formatting characters.
    private static float getEllipsizedPaddingForRtl(
            @NonNull Layout layout, int lineIndex, @NonNull Paint paint) {
        float width = layout.getWidth();

        if (width >= layout.getLineRight(lineIndex)) {
            return 0;
        }

        int ellipsisIndex = getEllipsisIndex(layout, lineIndex);
        float horizontal = getHorizontalPosition(layout, ellipsisIndex);
        float length = (layout.getLineRight(lineIndex) - horizontal)
                + paint.measureText(ELLIPSIS_CHAR);
        float divideFactor = getDivideFactor(layout, lineIndex);

        return width - layout.getLineRight(lineIndex) - ((width - length) / divideFactor);
    }

    private static float getHorizontalPosition(@NonNull Layout layout, int ellipsisIndex) {
        return layout.getPrimaryHorizontal(ellipsisIndex);
    }

    private static int getEllipsisIndex(@NonNull Layout layout, int lineIndex) {
        return layout.getLineStart(lineIndex) + layout.getEllipsisStart(lineIndex);
    }

    private static float getDivideFactor(@NonNull Layout layout, int lineIndex) {
        return layout.getParagraphAlignment(lineIndex) == Alignment.ALIGN_CENTER ? 2f : 1f;
    }
}
