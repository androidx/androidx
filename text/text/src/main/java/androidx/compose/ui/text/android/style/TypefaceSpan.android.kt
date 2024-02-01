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
package androidx.compose.ui.text.android.style

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.MetricAffectingSpan

/**
 * Span that displays text in the given Typeface. In Android Framework, TypefaceSpan that accepts
 * a Typeface as constructor argument was added in API 28, therefore was not usable before 28.
 *
 * @constructor Constructs a [android.text.style.TypefaceSpan] from a [Typeface]. The previous
 * style of the TextPaint is overridden and the style of the typeface is used.
 * @param typeface Typeface to render the text with.
 */
internal class TypefaceSpan(val typeface: Typeface) : MetricAffectingSpan() {
    override fun updateDrawState(ds: TextPaint) {
        updateTypeface(ds)
    }

    override fun updateMeasureState(paint: TextPaint) {
        updateTypeface(paint)
    }

    private fun updateTypeface(paint: Paint) {
        paint.typeface = typeface
    }
}
