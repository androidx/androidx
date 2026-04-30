/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.text.input

import androidx.compose.foundation.text.input.internal.IntervalHandle
import kotlin.jvm.JvmInline

/**
 * Public API candidate.
 *
 * A reference to a style in the [TextFieldBuffer]. It allows you to update the style's range,
 * expand policy, or the style object itself.
 *
 * The `range` of this [LiveRange] will automatically update when the text is edited. If the style's
 * range collapses to zero length due to text edits, the style will be removed and `isRemoved` will
 * return true.
 *
 * This object's lifecycle is bound to the [TextFieldBuffer] which is returned by
 * [TextFieldState.edit], [InputTransformation.transformInput] and
 * [OutputTransformation.transformOutput]. Do not keep a reference of it.
 *
 * @see TextFieldBuffer
 */
internal class LiveRange<T> internal constructor(internal var intervalHandle: IntervalHandle)

/**
 * Public API candidate.
 *
 * Defines how a [LiveRange] expands when text is inserted exactly at its boundaries.
 *
 * In [TextFieldBuffer.addStyle] methods without an [ExpandPolicy] parameter, the default policy is
 * [AtEnd]. By providing an [ExpandPolicy], you can configure the range to expand and include text
 * inserted at the start, end, or both boundaries of the range.
 */
@JvmInline
internal value class ExpandPolicy private constructor(val flag: Int) {

    internal constructor(
        startExpands: Boolean,
        endExpands: Boolean,
    ) : this(
        (if (startExpands) FLAG_EXPAND_START else 0) or (if (endExpands) FLAG_EXPAND_END else 0)
    )

    internal val startExpands: Boolean
        get() = (flag and FLAG_EXPAND_START) != 0

    internal val endExpands: Boolean
        get() = (flag and FLAG_EXPAND_END) != 0

    companion object {
        private const val FLAG_EXPAND_START = 0b0001
        private const val FLAG_EXPAND_END = 0b0010

        /**
         * The range will not expand when text is inserted at its boundaries. Text inserted exactly
         * at the start or end will be placed outside the range.
         */
        val InsideOnly = ExpandPolicy(0b0000)

        /**
         * The range will expand when text is inserted at its start boundary. Text inserted exactly
         * at the start will be included in the range.
         */
        val AtStart = ExpandPolicy(FLAG_EXPAND_START)

        /**
         * The range will expand when text is inserted at its end boundary. Text inserted exactly at
         * the end will be included in the range.
         */
        val AtEnd = ExpandPolicy(FLAG_EXPAND_END)

        /** The range will expand when text is inserted at either of its boundaries. */
        val AtBoth = ExpandPolicy(FLAG_EXPAND_START or FLAG_EXPAND_END)
    }
}
