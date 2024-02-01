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

import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import kotlin.math.ceil

/**
 * Span which shifts the vertical position of baseline.
 */
internal open class BaselineShiftSpan(val multiplier: Float) : MetricAffectingSpan() {

    override fun updateMeasureState(textPaint: TextPaint) {
        textPaint.baselineShift += ceil(textPaint.ascent() * multiplier).toInt()
    }

    override fun updateDrawState(textPaint: TextPaint) {
        textPaint.baselineShift += ceil(textPaint.ascent() * multiplier).toInt()
    }
}
