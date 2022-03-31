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
package androidx.compose.ui.text.android.style

import android.graphics.Paint.FontMetricsInt
import androidx.compose.ui.text.android.InternalPlatformTextApi
import kotlin.math.ceil

/**
 * The span which modifies the height of the covered paragraphs. A paragraph is defined as a
 * segment of string divided by '\n' character. To make sure the span work as expected, the
 * boundary of this span should align with paragraph boundary.
 * @constructor Create a LineHeightSpan which sets the line height to `height` physical pixels.
 * @param lineHeight The specified line height in pixel unit, which is the space between the
 * baseline of adjacent lines.
 * @param applyToFirstLine whether to apply the line height to the first line or not. false by
 * default.
 *
 * @suppress
 */
@InternalPlatformTextApi
class LineHeightSpan(
    val lineHeight: Float,
    val applyToFirstLine: Boolean = false
) : android.text.style.LineHeightSpan {

    override fun chooseHeight(
        text: CharSequence,
        start: Int,
        end: Int,
        spanstartVertical: Int,
        lineHeight: Int,
        fontMetricsInt: FontMetricsInt
    ) {
        // start == 0 assumes that the string always start from 0
        // when we implement b/139320242 this assumption will become wrong.
        if (start == 0 && !applyToFirstLine) return
        // In StaticLayout, line height is computed with descent - ascent
        val currentHeight = fontMetricsInt.descent - fontMetricsInt.ascent
        // If current height is not positive, do nothing.
        if (currentHeight <= 0) {
            return
        }
        // TODO changes here might be wrong: ceiling line height before ratio would cause
        //  discrepancies because of ~roundings in between.
        val ceiledLineHeight = ceil(this.lineHeight).toInt()
        val ratio = ceiledLineHeight * 1.0f / currentHeight
        fontMetricsInt.descent = ceil(fontMetricsInt.descent * ratio.toDouble()).toInt()
        fontMetricsInt.ascent = fontMetricsInt.descent - ceiledLineHeight
    }
}