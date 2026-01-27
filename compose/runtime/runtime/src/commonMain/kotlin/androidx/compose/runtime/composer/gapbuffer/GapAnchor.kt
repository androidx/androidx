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

package androidx.compose.runtime.composer.gapbuffer

import androidx.compose.runtime.Anchor
import androidx.compose.runtime.composeRuntimeError

/**
 * An [Anchor] tracks a groups as its index changes due to other groups being inserted and removed
 * before it. If the group the [Anchor] is tracking is removed, directly or indirectly, [valid] will
 * return false. The current index of the group can be determined by passing either the [SlotTable]
 * or [] to [toIndexFor]. If a [SlotWriter] is active, it must be used instead of the [SlotTable] as
 * the anchor index could have shifted due to operations performed on the writer.
 */
internal class GapAnchor(loc: Int) : Anchor {
    internal var location: Int = loc

    override val valid
        get() = location != Int.MIN_VALUE

    fun toIndexFor(slots: SlotTable) = slots.anchorIndex(this)

    fun toIndexFor(writer: SlotWriter) = writer.anchorIndex(this)

    override fun toString(): String {
        return "${super.toString()}{ location = $location }"
    }
}

internal fun Anchor.asGapAnchor(): GapAnchor =
    this as? GapAnchor ?: composeRuntimeError("Inconsistent composition")
