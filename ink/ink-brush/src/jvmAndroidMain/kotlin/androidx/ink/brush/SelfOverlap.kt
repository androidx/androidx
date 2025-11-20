/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.brush

import androidx.collection.MutableIntObjectMap

/**
 * Specifies how parts of the stroke that intersect itself should be treated during the rendering
 * process. The simplest example of this is with translucent, solid-color strokes - such as
 * [StockBrushes.highlighter] or [StockBrushes.emojiHighlighter].
 */
public class SelfOverlap
private constructor(@JvmField internal val value: Int, private val name: String) {
    init {
        check(value !in VALUE_TO_INSTANCE) { "Duplicate SelfOverlap value: $value." }
        VALUE_TO_INSTANCE[value] = this
    }

    override fun toString(): String = "SelfOverlap.$name"

    public companion object {
        private val VALUE_TO_INSTANCE = MutableIntObjectMap<SelfOverlap>()

        internal fun fromInt(value: Int): SelfOverlap =
            checkNotNull(VALUE_TO_INSTANCE[value]) { "Invalid SelfOverlap value: $value" }

        /**
         * Any of the options listed below may be used, depending on what would be most efficient
         * and feature-complete for the brush and the device.
         */
        @JvmField public val ANY: SelfOverlap = SelfOverlap(0, "ANY")

        /**
         * Self overlap will be accumulated, meaning that both the overlapped content and the
         * overlapping content will be drawn. For a translucent color stroke, this typically means
         * that the overlapping portion will appear with double the opacity of the non-overlapping
         * portions. This option is most analogous to physical writing and drawing, and it is the
         * option that best matches the appearance as if the stroke were drawn as separate, shorter
         * strokes. This is the default behavior for Android U and above when [ANY] is used because
         * it leads to the most performant rendering. This cannot be rendered on Android devices
         * before API level U.
         */
        @JvmField public val ACCUMULATE: SelfOverlap = SelfOverlap(1, "ACCUMULATE")

        /**
         * Self overlap will be drawn in a way that discards the overlapping content. This can be
         * used to make the stroke appear as if it's drawn as a PDF page object or annotation, where
         * a stroke can be filled only with a solid color or textures using
         * [BrushPaint.TextureMapping.Companion.TILING]. This is the default behavior for Android T
         * and below when [ANY] is used, and can also be used on Android U and above if desired.
         */
        @JvmField public val DISCARD: SelfOverlap = SelfOverlap(2, "DISCARD")
    }
}
