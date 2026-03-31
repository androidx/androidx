/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.text.vertical

import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.text.vertical.FontShearSpan.Companion.DEFAULT_FONT_SHEAR

/**
 * A span that applies a shear (skew) transformation to vertical writing text.
 *
 * This span inherits from [MetricAffectingSpan], but it is specifically designed for use within a
 * [VerticalTextLayout] and will not have an effect in other contexts.
 *
 * It is used to achieve an italic-like effect for vertical text layout, where traditional italic
 * fonts may not render correctly.
 *
 * The shear value represents the horizontal skew factor for rotated or tate-chu-yoko text. For
 * upright text, this value is used as the vertical skew factor.
 *
 * NOTE: This span only works with [VerticalTextLayout].
 *
 * @property fontShear The shear factor to apply to the text. This is the tangent of the shear
 *   angle.
 */
public class FontShearSpan
@JvmOverloads
constructor(public val fontShear: Float = DEFAULT_FONT_SHEAR) : MetricAffectingSpan() {

    override fun updateMeasureState(textPaint: TextPaint) {
        /* No-op: Handled internally by VerticalTextLayout */
    }

    override fun updateDrawState(textPaint: TextPaint?) {
        /* No-op: Handled internally by VerticalTextLayout */
    }

    public companion object {
        /**
         * Default constant for fontShear.
         *
         * This value represents a shear angle of 15 degrees (tan(15 deg)).
         */
        // This value is derived from Chrome's vertical writing implementation.
        // https://source.chromium.org/chromium/chromium/src/+/main:third_party/blink/renderer/platform/fonts/shaping/shape_result_bloberizer.cc;drc=c58cc9d7cce70b7f52b985e48aa126a4ba705cf6;l=676
        public const val DEFAULT_FONT_SHEAR: Float = 0.2679492f
    }
}
