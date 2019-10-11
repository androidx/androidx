/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.text.style;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.graphics.Paint;

import androidx.annotation.NonNull;
import androidx.annotation.Px;
import androidx.annotation.RestrictTo;

/**
 * The span which modifies the height of the covered paragraphs. A paragraph is defined as a
 * segment of string divided by '\n' character. To make sure the span work as expected, the
 * boundary of this span should align with paragraph boundary.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class LineHeightSpan implements android.text.style.LineHeightSpan {

    @Px
    private final int mLineHeight;

    /**
     * Create a LineHeightSpan which sets the line height to <code>height</code> physical pixels.
     * @param lineHeight The specified line height in pixel unit, which is the space between the
     *                   baseline of adjacent lines.
     */
    public LineHeightSpan(@Px int lineHeight) {
        mLineHeight = lineHeight;
    }

    @Override
    public void chooseHeight(@NonNull CharSequence text, int start, int end, int spanstartv,
            int lineHeight, @NonNull Paint.FontMetricsInt fontMetricsInt) {

        // In StaticLayout, line height is computed with descent - ascent
        final int currentHeight = fontMetricsInt.descent - fontMetricsInt.ascent;
        // If current height is not positive, do nothing.
        if (currentHeight <= 0) {
            return;
        }
        final float ratio = mLineHeight * 1.0f / currentHeight;
        fontMetricsInt.descent = (int) Math.ceil(fontMetricsInt.descent * ratio);
        fontMetricsInt.ascent = fontMetricsInt.descent - mLineHeight;
    }
}
