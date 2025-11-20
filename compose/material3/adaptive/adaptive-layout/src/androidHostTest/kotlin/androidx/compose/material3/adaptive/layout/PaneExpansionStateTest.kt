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

package androidx.compose.material3.adaptive.layout

import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PaneExpansionStateTest {
    @Test
    fun draggingPositively_withinBounds() {
        val mockState = PaneExpansionState(PaneExpansionStateData())
        mockState.onMeasured(MockScaffoldWidth, MockDensity)
        mockState.onExpansionOffsetMeasured(1000)
        mockState.draggableState.dispatchRawDelta(500f)

        assertThat(mockState.currentDraggingOffset).isEqualTo(1500)
    }

    @Test
    fun draggingNegatively_withinBounds() {
        val mockState = PaneExpansionState(PaneExpansionStateData())
        mockState.onMeasured(MockScaffoldWidth, MockDensity)
        mockState.onExpansionOffsetMeasured(1000)
        mockState.draggableState.dispatchRawDelta(-500f)

        assertThat(mockState.currentDraggingOffset).isEqualTo(500)
    }

    @Test
    fun draggingPositively_beyondBounds_shouldBeBounded() {
        val mockState = PaneExpansionState(PaneExpansionStateData())
        mockState.onMeasured(MockScaffoldWidth, MockDensity)
        mockState.onExpansionOffsetMeasured(1000)
        mockState.draggableState.dispatchRawDelta(1500f)

        assertThat(mockState.currentDraggingOffset).isEqualTo(MockScaffoldWidth)
    }

    @Test
    fun draggingNegatively_beyondBounds_shouldBeBounded() {
        val mockState = PaneExpansionState(PaneExpansionStateData())
        mockState.onMeasured(MockScaffoldWidth, MockDensity)
        mockState.onExpansionOffsetMeasured(1000)
        mockState.draggableState.dispatchRawDelta(-1500f)

        assertThat(mockState.currentDraggingOffset).isEqualTo(0)
    }

    @Test
    fun noOpPreDrag_shouldDoNothing() {
        val mockState = PaneExpansionState(PaneExpansionStateData()) { delta -> delta }
        mockState.onMeasured(MockScaffoldWidth, MockDensity)
        mockState.onExpansionOffsetMeasured(1000)
        mockState.draggableState.dispatchRawDelta(500f)

        assertThat(mockState.currentDraggingOffset).isEqualTo(1500)
    }

    @Test
    fun preDrag_shouldConsumeDelta() {
        val mockState = PaneExpansionState(PaneExpansionStateData()) { delta -> delta - 200 }
        mockState.onMeasured(MockScaffoldWidth, MockDensity)
        mockState.onExpansionOffsetMeasured(1000)
        mockState.draggableState.dispatchRawDelta(500f)

        assertThat(mockState.currentDraggingOffset).isEqualTo(1300)
    }

    @Test
    fun preDrag_alterDeltaBeyondBounds_shouldBeBounded() {
        val mockState = PaneExpansionState(PaneExpansionStateData()) { delta -> -1200f }
        mockState.onMeasured(MockScaffoldWidth, MockDensity)
        mockState.onExpansionOffsetMeasured(1000)
        mockState.draggableState.dispatchRawDelta(500f)

        assertThat(mockState.currentDraggingOffset).isEqualTo(0)
    }

    @Test
    fun preDrag_consumeAllDelta_shouldKeepTheSameOffset() {
        val mockState = PaneExpansionState(PaneExpansionStateData()) { delta -> 0f }
        mockState.onMeasured(MockScaffoldWidth, MockDensity)
        mockState.onExpansionOffsetMeasured(1000)
        mockState.draggableState.dispatchRawDelta(500f)

        assertThat(mockState.currentDraggingOffset).isEqualTo(1000)
    }
}

private const val MockScaffoldWidth = 2000
private val MockDensity = Density(1f)
