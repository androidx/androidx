/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.savedstate

import android.os.Bundle
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class SavedStateRegistryTest {

    @Test
    fun saveRestoreFlow() {
        val registry = SavedStateRegistry()
        registry.registerSavedStateProvider("a") { bundleOf("foo", 1) }
        registry.registerSavedStateProvider("b") { bundleOf("foo", 2) }
        val state = Bundle()
        registry.performSave(state)

        val newRegistry = SavedStateRegistry()
        newRegistry.performRestore(state)

        assertThat(newRegistry.isRestored).isTrue()
        val bundleForA = newRegistry.consumeRestoredStateForKey("a")
        val bundleForB = newRegistry.consumeRestoredStateForKey("b")
        assertThat(bundleForA.isSame(bundleOf("foo", 1))).isTrue()
        assertThat(bundleForA.isSame(bundleOf("foo", 1))).isTrue()
        assertThat(bundleForB.isSame(bundleOf("foo", 2))).isTrue()
    }

    @Test
    fun registerWithSameKey() {
        val registry = SavedStateRegistry()
        registry.registerSavedStateProvider("key") { bundleOf("foo", "a") }
        try {
            registry.registerSavedStateProvider("key") { bundleOf("foo", "b") }
            Assert.fail("can't register with the same key")
        } catch (e: IllegalArgumentException) {
            // fail as expected
        }
    }

    @Test
    fun consumeSameTwice() {
        val registry = SavedStateRegistry()
        registry.registerSavedStateProvider("a") { bundleOf("key", "fo") }

        val state = Bundle()
        registry.performSave(state)

        val newStore = SavedStateRegistry()
        newStore.performRestore(state)

        assertThat(newStore.isRestored).isTrue()
        assertThat(newStore.consumeRestoredStateForKey("a").isSame(bundleOf("key", "fo"))).isTrue()
        assertThat(newStore.consumeRestoredStateForKey("a")).isNull()
    }

    @Test
    fun unregister() {
        val registry = SavedStateRegistry()
        registry.registerSavedStateProvider("a") { bundleOf("key", "fo") }
        registry.unregisterSavedStateProvider("a")
        // this call should succeed
        registry.registerSavedStateProvider("a") { bundleOf("key", "fo") }
        registry.unregisterSavedStateProvider("a")
        val state = Bundle()
        registry.performSave(state)

        val newStore = SavedStateRegistry()
        newStore.performRestore(state)
        assertThat(newStore.consumeRestoredStateForKey("a")).isNull()
    }

    @Test
    fun unconsumedSavedState() {
        val registry = SavedStateRegistry()
        registry.registerSavedStateProvider("a") { bundleOf("key", "fo") }
        val savedState1 = Bundle()
        registry.performSave(savedState1)

        val intermediateStore = SavedStateRegistry()
        intermediateStore.performRestore(savedState1)

        val savedState2 = Bundle()
        intermediateStore.performSave(savedState2)

        val newRegistry = SavedStateRegistry()
        newRegistry.performRestore(savedState2)
        assertThat(
            newRegistry.consumeRestoredStateForKey("a")
                .isSame(bundleOf("key", "fo"))
        ).isTrue()
    }

    @Test
    fun unconsumedSavedStateClashWithCallback() {
        val registry = SavedStateRegistry()
        registry.registerSavedStateProvider("a") { bundleOf("key", "fo") }
        val savedState1 = Bundle()
        registry.performSave(savedState1)

        val intermediateStore = SavedStateRegistry()
        intermediateStore.performRestore(savedState1)
        // there is unconsumed value for "a"
        intermediateStore.registerSavedStateProvider("a") { bundleOf("key", "ba") }
        val savedState2 = Bundle()
        intermediateStore.performSave(savedState2)

        val newStore = SavedStateRegistry()
        newStore.performRestore(savedState2)
        assertThat(newStore.consumeRestoredStateForKey("a").isSame(bundleOf("key", "ba"))).isTrue()
    }
}

private fun bundleOf(key: String, value: Int): Bundle {
    val result = Bundle()
    result.putInt(key, value)
    return result
}

private fun bundleOf(key: String, value: String): Bundle {
    val result = Bundle()
    result.putString(key, value)
    return result
}

private fun Bundle?.isSame(other: Bundle): Boolean {
    if (this == null) {
        return false
    }
    if (keySet() != other.keySet()) {
        return false
    }
    for (key in keySet()) {
        if (get(key) != other.get(key)) {
            return false
        }
    }
    return true
}