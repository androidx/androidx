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
class AbstractSavedStateRegistryTest {

    @Test
    fun registerWithSameKey() {
        val registry = TestSavedStateRegistry()
        registry.registerSavedStateProvider("key") { "a" }
        try {
            registry.registerSavedStateProvider("key") { "b" }
            Assert.fail("can't register with the same key")
        } catch (e: IllegalArgumentException) {
            // fail as expected
        }
    }

    @Test
    fun saveRestoreFlow() {
        val registry = TestSavedStateRegistry()
        registry.registerSavedStateProvider("a") { "bla" }
        registry.registerSavedStateProvider("b") { "boo" }
        val savedState = registry.savedState()

        val newRegistry = TestSavedStateRegistry()
        newRegistry.restoreState(savedState)

        assertThat(newRegistry.isRestored, `is`(true))
        assertThat(newRegistry.consumeRestoredStateForKey("a"), `is`("bla"))
        assertThat(newRegistry.consumeRestoredStateForKey("b"), `is`("boo"))
    }

    @Test
    fun consumeSameTwice() {
        val registry = TestSavedStateRegistry()
        registry.registerSavedStateProvider("a") { "foo" }
        val state = registry.savedState()

        val newStore = TestSavedStateRegistry()
        newStore.restoreState(state)

        assertThat(newStore.isRestored, `is`(true))
        assertThat(newStore.consumeRestoredStateForKey("a"), `is`("foo"))
        assertThat(newStore.consumeRestoredStateForKey("a"), nullValue())
    }

    @Test
    fun unregister() {
        val registry = TestSavedStateRegistry()
        registry.registerSavedStateProvider("a") { "foo" }
        registry.unregisterSavedStateProvider("a")
        // this call should succeed
        registry.registerSavedStateProvider("a") { "foo" }
        registry.unregisterSavedStateProvider("a")

        assertThat(registry.savedState().isEmpty(), `is`(true))
    }

    @Test
    fun unconsumedSavedState() {
        val registry = TestSavedStateRegistry()
        registry.registerSavedStateProvider("a") { "foo" }
        val savedState1 = registry.savedState()
        val intermediateStore = TestSavedStateRegistry()
        intermediateStore.restoreState(savedState1)
        val savedState2 = intermediateStore.savedState()
        val newRegistry = TestSavedStateRegistry()
        newRegistry.restoreState(savedState2)
        assertThat(newRegistry.consumeRestoredStateForKey("a"), `is`("foo"))
    }

    @Test
    fun unconsumedSavedStateClashWithCallback() {
        val registry = TestSavedStateRegistry()
        registry.registerSavedStateProvider("a") { "foo" }
        val savedState1 = registry.savedState()
        val intermediateStore = TestSavedStateRegistry()
        intermediateStore.restoreState(savedState1)
        // there is unconsumed value for "a"
        intermediateStore.registerSavedStateProvider("a") { "bar" }
        val savedState2 = intermediateStore.savedState()
        val newStore = TestSavedStateRegistry()
        newStore.restoreState(savedState2)
        assertThat(newStore.consumeRestoredStateForKey("a"), `is`("bar"))
    }

    class TestSavedStateRegistry : AbstractSavedStateRegistry<String>() {
        fun restoreState(state: Map<String, String>) {
            restoreSavedState(state)
        }
        fun savedState() = saveState()
    }
}
