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

@file:OptIn(ExperimentalFoundationStyleApi::class)

package androidx.compose.foundation.style

import androidx.collection.ScatterSet
import androidx.collection.mutableScatterSetOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.state.ToggleableState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StyleStateTest {
    @Test
    fun testPredefinedDefaults() {
        val state = MutableStyleState(null)
        assertFalse(state.isHovered)
        assertFalse(state.isPressed)
        assertFalse(state.isFocused)
        assertFalse(state.isSelected)
        assertFalse(state.isChecked)
        assertTrue(state.isEnabled)
        assertEquals(ToggleableState.Off, state.triStateToggle)
    }

    @Test
    fun testPredefinedStates_set() {
        val state = MutableStyleState(null)
        state.isHovered = true
        state.isPressed = true
        state.isFocused = true
        state.isSelected = true
        state.isChecked = true

        assertTrue(state.isHovered)
        assertTrue(state.isPressed)
        assertTrue(state.isFocused)
        assertTrue(state.isSelected)
        assertTrue(state.isChecked)
    }

    @Test
    fun testPredefinedStates_partial_set() {
        val state = MutableStyleState(null)
        state.isPressed = true
        state.isSelected = true

        assertFalse(state.isHovered)
        assertTrue(state.isPressed)
        assertFalse(state.isFocused)
        assertTrue(state.isSelected)
        assertFalse(state.isChecked)
    }

    @Test
    fun testObservePredefined_read() {
        val state = MutableStyleState(null)
        observe({ read, _, _ -> assertTrue(read.size > 0) }) { assertFalse(state.isPressed) }
    }

    @Test
    fun testObservePredefined_written() {
        val state = MutableStyleState(null)
        observe({ _, written, _ -> assertTrue(written.size > 0) }) { state.isPressed = true }
    }

    @Test
    fun testObservePredefined_changed() {
        val state = MutableStyleState(null)
        observe({ _, _, changed -> assertTrue(changed.size > 0) }) { state.isPressed = true }
    }

    @Test
    fun testObservePredefined_read_and_write_matches_changed() {
        val state = MutableStyleState(null)
        observe({ read, written, changed ->
            assertTrue(read.size > 0)
            assertTrue(changed.all { it in read })
            assertTrue(written.all { it in changed })
        }) {
            assertFalse(state.isPressed)
            state.isPressed = true
            assertTrue(state.isPressed)
        }
    }

    @Test
    fun testCustomState_set() {
        val state = MutableStyleState(null)
        state.customState = 10
        assertEquals(10, state.customState)
    }

    @Test
    fun testObserveCustomState_read() {
        val state = MutableStyleState(null)
        observe({ read, _, _ -> assertTrue(read.size > 0) }) { assertEquals(0, state.customState) }
    }

    @Test
    fun testObserveCustomState_written() {
        val state = MutableStyleState(null)
        observe({ _, written, _ -> assertTrue(written.size > 0) }) { state.customState++ }
    }

    @Test
    fun testObserveCustomState_changed() {
        val state = MutableStyleState(null)
        observe({ _, _, changed -> assertTrue(changed.size > 0) }) { state.customState++ }
    }

    @Test
    fun testObserveCustom_read_and_write_matches_changed() {
        val state = MutableStyleState(null)
        observe({ read, written, changed ->
            assertTrue(read.size > 0)
            assertTrue(changed.all { it in read })
            assertTrue(written.all { it in changed })
        }) {
            assertEquals(0, state.customState)
            state.customState = 10
            assertEquals(10, state.customState)
        }
    }
}

private fun observe(
    validate: (ScatterSet<Any>, ScatterSet<Any>, ScatterSet<Any>) -> Unit,
    block: () -> Unit,
) {
    val readStates = mutableScatterSetOf<Any>()
    val writtenStates = mutableScatterSetOf<Any>()
    val changedStates = mutableScatterSetOf<Any>()

    val handle = Snapshot.registerApplyObserver { set, _ -> changedStates += set }
    try {
        Snapshot.observe(
            readObserver = { readStates += it },
            writeObserver = { writtenStates += it },
            block = block,
        )
        Snapshot.sendApplyNotifications()
    } finally {
        handle.dispose()
    }

    validate(readStates, writtenStates, changedStates)
}

private val customStateKey = StyleStateKey(0)
private var MutableStyleState.customState
    get() = this[customStateKey]
    set(value) {
        this[customStateKey] = value
    }
