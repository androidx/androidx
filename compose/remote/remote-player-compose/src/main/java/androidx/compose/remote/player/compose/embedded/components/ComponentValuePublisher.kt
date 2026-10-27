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

// The state map is the one shared with LocalComponentValueStateMap and GraphContext.
@file:Suppress("PrimitiveInCollection")

package androidx.compose.remote.player.compose.embedded.components

import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.ComponentValue
import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs

/**
 * How far a root-relative position ([ComponentValue.POS_ROOT_X] / [ComponentValue.POS_ROOT_Y]) must
 * move before it is published. These follow every pixel of an in-document scroll, so they are
 * throttled to avoid a snapshot write (and recomposition / redraw of their readers) per pixel.
 */
private const val ROOT_POSITION_THRESHOLD_PX = 2f

/**
 * Publishes a component's measured values to the [ComponentValue] ops in [ops]: both the backing
 * store ([store]) and the reactive per-value state ([states]) that graph reads resolve first.
 *
 * The two are kept in lockstep, so every reader sees the same value, and only written when the
 * value changed: exactly for sizes and positions in the parent, and by more than
 * [ROOT_POSITION_THRESHOLD_PX] for root-relative positions. The [initial] publish of a component
 * writes every value, so the store doesn't keep a stale value that happens to match the reactive
 * state's seed.
 */
internal fun publishComponentValues(
    ops: List<ComponentValue>,
    width: Float,
    height: Float,
    contentWidth: Float,
    contentHeight: Float,
    posInParent: Offset,
    rootOffset: Offset,
    store: RemoteContext,
    states: Map<Int, MutableState<Float>>,
    initial: Boolean,
) {
    ops.fastForEach { op ->
        val newValue =
            when (op.type) {
                ComponentValue.WIDTH -> width
                ComponentValue.CONTENT_WIDTH -> contentWidth
                ComponentValue.HEIGHT -> height
                ComponentValue.CONTENT_HEIGHT -> contentHeight
                ComponentValue.POS_X -> posInParent.x
                ComponentValue.POS_Y -> posInParent.y
                ComponentValue.POS_ROOT_X -> rootOffset.x
                ComponentValue.POS_ROOT_Y -> rootOffset.y
                else -> return@fastForEach
            }
        val state = states[op.valueId]
        if (state != null) {
            val current = state.value
            val isRootPosition =
                op.type == ComponentValue.POS_ROOT_X || op.type == ComponentValue.POS_ROOT_Y
            val changed =
                if (isRootPosition) {
                    abs(newValue - current) > ROOT_POSITION_THRESHOLD_PX
                } else {
                    newValue != current
                }
            if (!changed && !initial) return@fastForEach
            state.value = newValue
        }
        store.loadFloat(op.valueId, newValue)
    }
}
