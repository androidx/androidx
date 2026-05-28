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

package androidx.appstate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.serialization.Serializable

class AppStateTest {

    @Serializable object StringKey : AppStateKey<String>()

    @Serializable object IntKey : AppStateKey<Int>()

    @Test
    fun testGetStateReturnsDefaultValue() {
        val appState = AppState()

        val state = appState.getState(StringKey, "default")
        assertEquals("default", state.value)
    }

    @Test
    fun testSetStateUpdatesValue() {
        val appState = AppState()

        appState.setState(StringKey, "new value")

        val state = appState.getState(StringKey, "default")
        assertEquals("new value", state.value)
    }

    @Test
    fun testUpdateState() {
        val appState = AppState()

        appState.setState(IntKey, 5)
        appState.updateState(IntKey, 0) { it + 5 }

        val state = appState.getState(IntKey, 0)
        assertEquals(10, state.value)
    }

    @Test
    fun testTokenEquality() {
        val token1 = AppStateToken()
        val token2 = AppStateToken()

        assertEquals(token1, token1)
        assertNotEquals(token1, token2)
    }
}
