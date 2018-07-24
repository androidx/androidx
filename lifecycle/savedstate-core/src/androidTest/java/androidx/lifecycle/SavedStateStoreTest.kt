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

package androidx.lifecycle

import android.os.Bundle
import androidx.test.filters.SmallTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class SavedStateStoreTest {

    class TestSavedStateStore : SavedStateStore() {
        override fun getArguments(): Bundle? = null
    }

    @Test
    fun registerWithSameKey() {
        val store = TestSavedStateStore()
        store.registerSavedStateCallback("key") { Bundle.EMPTY }
        try {
            store.registerSavedStateCallback("key") { Bundle.EMPTY }
            Assert.fail("can't register with the same key")
        } catch (e: IllegalArgumentException) {
            // fail as expected
        }
    }

    @Test
    fun saveRestoreFlow() {
        val store = TestSavedStateStore()
        store.registerSavedStateCallback("a") { bundleOf("foo", 1) }
        store.registerSavedStateCallback("b") { bundleOf("foo", 2) }
        val state = Bundle()
        store.performSaveState(state)

        val newStore = TestSavedStateStore()
        newStore.performRestoreState(state)

        assertThat(newStore.isRestored, `is`(true))
        val bundleForA = newStore.consumeRestoredStateForKey("a")
        val bundleForB = newStore.consumeRestoredStateForKey("b")

        assertThat(bundleForA.isSame(bundleOf("foo", 1)), `is`(true))
        assertThat(bundleForB.isSame(bundleOf("foo", 2)), `is`(true))
    }

    @Test
    fun consumeSameTwice() {
        val store = TestSavedStateStore()
        store.registerSavedStateCallback("a") { bundleOf("foo", 1) }
        val state = Bundle()
        store.performSaveState(state)

        val newStore = TestSavedStateStore()
        newStore.performRestoreState(state)

        assertThat(newStore.isRestored, `is`(true))
        val bundleForA = newStore.consumeRestoredStateForKey("a")
        assertThat(bundleForA.isSame(bundleOf("foo", 1)), `is`(true))
        assertThat(newStore.consumeRestoredStateForKey("a"), nullValue())
    }

    @Test
    fun unregister() {
        val store = TestSavedStateStore()
        store.registerSavedStateCallback("a") { bundleOf("foo", 1) }
        store.unregisterSaveStateCallback("a")
        // this call should succeed
        store.registerSavedStateCallback("a") { bundleOf("foo", 2) }
        store.unregisterSaveStateCallback("a")
        val state = Bundle()
        store.performRestoreState(state)
        assertThat(state.isEmpty, `is`(true))
    }

    @Test
    fun unconsumedSavedState() {
        val store = TestSavedStateStore()
        store.registerSavedStateCallback("a") { bundleOf("foo", 1) }
        val savedState1 = Bundle()
        store.performSaveState(savedState1)
        val intermediateStore = TestSavedStateStore()
        intermediateStore.performRestoreState(savedState1)
        val savedState2 = Bundle()
        intermediateStore.performSaveState(savedState2)
        val newStore = TestSavedStateStore()
        newStore.performRestoreState(savedState2)
        val bundleForA = newStore.consumeRestoredStateForKey("a")
        assertThat(bundleForA.isSame(bundleOf("foo", 1)), `is`(true))
    }

    @Test
    fun unconsumedSavedStateClashWithCallback() {
        val store = TestSavedStateStore()
        store.registerSavedStateCallback("a") { bundleOf("foo", 1) }
        val savedState1 = Bundle()
        store.performSaveState(savedState1)
        val intermediateStore = TestSavedStateStore()
        // there is unconsumed value for "a"
        intermediateStore.performRestoreState(savedState1)
        intermediateStore.registerSavedStateCallback("a") { bundleOf("foo", 2) }
        val savedState2 = Bundle()
        intermediateStore.performSaveState(savedState2)
        val newStore = TestSavedStateStore()
        newStore.performRestoreState(savedState2)
        val bundleForA = newStore.consumeRestoredStateForKey("a")
        assertThat(bundleForA.isSame(bundleOf("foo", 2)), `is`(true))
    }
}

private fun bundleOf(key: String, value: Int): Bundle {
    val result = Bundle()
    result.putInt(key, value)
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