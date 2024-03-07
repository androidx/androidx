/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3.adaptive.layout

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.coroutineScope

@ExperimentalMaterial3AdaptiveApi
@Stable
internal class PaneExpansionState : DraggableState {
    private var firstPaneWidthState by mutableIntStateOf(UnspecifiedWidth)
    private var firstPanePercentageState by mutableFloatStateOf(Float.NaN)
    private var currentDraggingOffsetState by mutableIntStateOf(UnspecifiedWidth)

    var firstPaneWidth: Int
        set(value) {
            firstPanePercentageState = Float.NaN
            currentDraggingOffsetState = UnspecifiedWidth
            firstPaneWidthState = value.coerceIn(0, maxExpansionWidth)
        }
        get() = firstPaneWidthState

    var firstPanePercentage: Float
        set(value) {
            require(value in 0f..1f) { "Percentage value needs to be in [0, 1]" }
            firstPaneWidthState = UnspecifiedWidth
            currentDraggingOffsetState = UnspecifiedWidth
            firstPanePercentageState = value
        }
        get() = firstPanePercentageState

    internal var currentDraggingOffset
        get() = currentDraggingOffsetState
        private set(value) {
            if (value == currentDraggingOffsetState) {
                return
            }
            currentDraggingOffsetState = value
        }

    // Use this field to store the dragging offset decided by measuring instead of dragging to
    // prevent redundant re-composition.
    internal var currentMeasuredDraggingOffset = UnspecifiedWidth

    internal var isDragging by mutableStateOf(false)
        private set

    internal var maxExpansionWidth = 0
        set(value) {
            if (field == value) {
                return
            }
            field = value
            if (firstPaneWidth != UnspecifiedWidth) {
                firstPaneWidth = firstPaneWidth
            }
        }

    private val dragScope: DragScope = object : DragScope {
        override fun dragBy(pixels: Float): Unit = dispatchRawDelta(pixels)
    }

    private val dragMutex = MutatorMutex()

    fun isUnspecified(): Boolean =
        firstPaneWidthState == UnspecifiedWidth &&
            firstPanePercentage.isNaN() &&
            currentDraggingOffset == UnspecifiedWidth

    override fun dispatchRawDelta(delta: Float) {
        if (currentMeasuredDraggingOffset == UnspecifiedWidth) {
            return
        }
        currentDraggingOffset =
            (currentMeasuredDraggingOffset + delta).toInt().coerceIn(0, maxExpansionWidth)
        currentMeasuredDraggingOffset = currentDraggingOffset
    }

    override suspend fun drag(
        dragPriority: MutatePriority,
        block: suspend DragScope.() -> Unit
    ) = coroutineScope {
        isDragging = true
        dragMutex.mutateWith(dragScope, dragPriority, block)
        isDragging = false
    }

    companion object {
        const val UnspecifiedWidth = -1
    }
}
