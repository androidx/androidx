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

package androidx.text

import android.text.BoringLayout
import android.text.Layout
import android.text.TextPaint
import androidx.annotation.RestrictTo

/**
 * Computes and caches the text layout intrinsic values such as min/max width.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class LayoutIntrinsics(
    charSequence: CharSequence,
    textPaint: TextPaint,
    @LayoutCompat.TextDirection textDirectionHeuristic: Int
) {
    /**
     * Compute Android platform BoringLayout metrics. A null value means the provided CharSequence
     * cannot be laid out using a BoringLayout.
     */
    internal val boringMetrics: BoringLayout.Metrics? by lazy {
        val frameworkTextDir = getTextDirectionHeuristic(textDirectionHeuristic)
        BoringLayoutCompat.isBoring(charSequence, textPaint, frameworkTextDir)
    }

    /**
     * Calculate minimum intrinsic width of the CharSequence.
     *
     * @see androidx.text.minIntrinsicWidth
     */
    val minIntrinsicWidth: Float by lazy {
        minIntrinsicWidth(charSequence, textPaint)
    }

    /**
     * Calculate maximum intrinsic width for the CharSequence. Maximum intrinsic width is the width
     * of text where no soft line breaks are applied.
     */
    val maxIntrinsicWidth: Float by lazy {
        // TODO(haoyuchang): we didn't pass the TextDirection to Layout.getDesiredWidth(), check if
        //  there is any behavior difference from
        //  Layout.getWidthWithLimits(charSequence, start, end, paint, dir)
        boringMetrics?.width?.toFloat()
            ?: Layout.getDesiredWidth(charSequence, 0, charSequence.length, textPaint)
    }
}