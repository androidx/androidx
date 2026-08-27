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

import androidx.kruth.assertThat
import kotlin.test.Test
import kotlinx.serialization.Serializable

class StateStoreTest {

    @Serializable object StringKey : StateStoreKey<String>()

    @Serializable object IntKey : StateStoreKey<Int>()

    @Test
    fun testGetStateReturnsDefaultValue() {
        val stateStore = StateStore()

        val state = stateStore.getState(StringKey, "default")
        assertThat(state.value).isEqualTo("default")
    }

    @Test
    fun testSetStateUpdatesValue() {
        val stateStore = StateStore()

        stateStore.setState(StringKey, "new value")

        val state = stateStore.getState(StringKey, "default")
        assertThat(state.value).isEqualTo("new value")
    }

    @Test
    fun testUpdateState() {
        val stateStore = StateStore()

        stateStore.setState(IntKey, 5)
        stateStore.updateState(IntKey, 0) { it + 5 }

        val state = stateStore.getState(IntKey, 0)
        assertThat(state.value).isEqualTo(10)
    }
}
