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

package androidx.savedstate

import androidx.kruth.assertThat
import kotlin.test.Test

@IgnoreWebTarget
internal class SavedStateContainerTest : RobolectricTest() {

    private class TestSavedStateValue(override var value: String = "") : SavedStateValue<String> {
        var restoredState: SavedState? = null
        var saveStateCalled: Boolean = false

        override fun saveState(): SavedState {
            saveStateCalled = true
            return savedState { putString("key", value) }
        }

        override fun restoreState(savedState: SavedState?) {
            restoredState = savedState
            if (savedState != null) {
                value = savedState.read { getStringOrNull("key") } ?: ""
            }
        }
    }

    private class ProviderOnlyValue(override val value: Int = 42) : SavedStateValue<Int> {
        override fun saveState(): SavedState = savedState { putInt("intKey", value) }

        override fun restoreState(savedState: SavedState?) {}
    }

    private class RestorerOnlyValue(override var value: String = "") : SavedStateValue<String> {
        var restored: Boolean = false

        override fun saveState(): SavedState = savedState()

        override fun restoreState(savedState: SavedState?) {
            restored = true
        }
    }

    @Test
    fun addAndGetSavedStateValue() {
        val container = SavedStateContainer()
        val testValue = TestSavedStateValue("hello")

        container.putSavedStateValue("testKey", testValue)

        val retrieved: TestSavedStateValue? =
            container.getSavedStateValue<String, TestSavedStateValue>("testKey")
        assertThat(retrieved).isNotNull()
        assertThat(retrieved).isSameInstanceAs(testValue)
        assertThat(retrieved?.value).isEqualTo("hello")
    }

    @Test
    fun getSavedStateValue_returnsNullWhenKeyNotFound() {
        val container = SavedStateContainer()
        val retrieved: TestSavedStateValue? =
            container.getSavedStateValue<String, TestSavedStateValue>("nonExistent")
        assertThat(retrieved).isNull()
    }

    @Test
    fun addSavedStateValue_overwritesExistingValue() {
        val container = SavedStateContainer()
        val value1 = TestSavedStateValue("first")
        val value2 = TestSavedStateValue("second")

        container.putSavedStateValue("key", value1)
        container.putSavedStateValue("key", value2)

        val retrieved: TestSavedStateValue? =
            container.getSavedStateValue<String, TestSavedStateValue>("key")
        assertThat(retrieved).isSameInstanceAs(value2)
        assertThat(retrieved?.value).isEqualTo("second")
    }

    @Test
    fun getOrPutSavedStateValue_returnsExistingValueWhenPresent() {
        val container = SavedStateContainer()
        val existing = TestSavedStateValue("existing")
        container.putSavedStateValue("key", existing)

        var factoryCalled = false
        val result =
            container.getOrPutSavedStateValue("key") {
                factoryCalled = true
                TestSavedStateValue("new")
            }

        assertThat(factoryCalled).isFalse()
        assertThat(result).isSameInstanceAs(existing)
        assertThat(result.value).isEqualTo("existing")
    }

    @Test
    fun getOrPutSavedStateValue_evaluatesDefaultAndRegistersWhenAbsent() {
        val container = SavedStateContainer()
        val created = TestSavedStateValue("created")

        var factoryCalled = false
        val result =
            container.getOrPutSavedStateValue("key") {
                factoryCalled = true
                created
            }

        assertThat(factoryCalled).isTrue()
        assertThat(result).isSameInstanceAs(created)
        assertThat(container.getSavedStateValue<String, TestSavedStateValue>("key"))
            .isSameInstanceAs(created)
    }

    @Test
    fun removeSavedStateValue_removesAndReturnsValue() {
        val container = SavedStateContainer()
        val testValue = TestSavedStateValue("removeMe")
        container.putSavedStateValue("key", testValue)

        val removed: TestSavedStateValue? =
            container.removeSavedStateValue<String, TestSavedStateValue>("key")

        assertThat(removed).isSameInstanceAs(testValue)
        assertThat(container.getSavedStateValue<String, TestSavedStateValue>("key")).isNull()
    }

    @Test
    fun createOrGetContainer_createsNewChildContainer() {
        val parent = SavedStateContainer()
        val child = parent.createOrGetContainer("child")

        assertThat(child).isNotNull()

        // Same call returns the exact same child container
        val sameChild = parent.createOrGetContainer("child")
        assertThat(sameChild).isSameInstanceAs(child)
    }

    @Test
    fun nestedContainers_saveAndRestoreState() {
        val parent = SavedStateContainer()
        val child = parent.createOrGetContainer("child")
        val childValue = TestSavedStateValue("childState")
        child.putSavedStateValue("leafKey", childValue)

        // Save parent state
        val savedState = parent.saveState()

        // Verify structure of saved bundle
        val childSavedState = savedState.read { getSavedStateOrNull("child") }
        assertThat(childSavedState).isNotNull()
        val leafSavedState = childSavedState!!.read { getSavedStateOrNull("leafKey") }
        assertThat(leafSavedState).isNotNull()
        assertThat(leafSavedState!!.read { getStringOrNull("key") }).isEqualTo("childState")

        // Restore into a fresh parent hierarchy
        val restoredParent = SavedStateContainer()
        val restoredChild = restoredParent.createOrGetContainer("child")
        val restoredChildValue = TestSavedStateValue()
        restoredChild.putSavedStateValue("leafKey", restoredChildValue)

        // When restoring root with the bundle
        restoredParent.restoreState(savedState)

        // The nested child should have received its restoration state
        // Note: SavedStateContainerValue restores the bundle into child container
        assertThat(restoredChildValue.restoredState).isNotNull()
        assertThat(restoredChildValue.value).isEqualTo("childState")
    }

    @Test
    fun savedState_collectsStateFromAllProviders() {
        val container = SavedStateContainer()
        val value1 = TestSavedStateValue("val1")
        val value2 = ProviderOnlyValue(100)

        container.putSavedStateValue("key1", value1)
        container.putSavedStateValue("key2", value2)

        val saved = container.saveState()

        assertThat(value1.saveStateCalled).isTrue()
        saved.read {
            val key1State = getSavedStateOrNull("key1")
            assertThat(key1State).isNotNull()
            assertThat(key1State!!.read { getStringOrNull("key") }).isEqualTo("val1")

            val key2State = getSavedStateOrNull("key2")
            assertThat(key2State).isNotNull()
            assertThat(key2State!!.read { getIntOrNull("intKey") }).isEqualTo(100)
        }
    }

    @Test
    fun restoreState_delegatesToAllRestorers() {
        val container = SavedStateContainer()
        val value1 = TestSavedStateValue("initial")
        val value2 = RestorerOnlyValue()

        container.putSavedStateValue("key1", value1)
        container.putSavedStateValue("key2", value2)

        val key1Bundle = savedState { putString("key", "restoredVal") }
        val bundle = savedState {
            putSavedState("key1", key1Bundle)
        }

        container.restoreState(bundle)

        assertThat(value1.restoredState).isEqualTo(key1Bundle)
        assertThat(value1.value).isEqualTo("restoredVal")
        assertThat(value2.restored).isTrue()
    }

    @Test
    fun restoreState_withNullState_delegatesNullToRestorers() {
        val container = SavedStateContainer()
        val value1 = TestSavedStateValue("initial")

        container.putSavedStateValue("key1", value1)
        container.restoreState(null)

        assertThat(value1.restoredState).isNull()
    }

    @Test
    fun putSavedStateValue_thenRestoreState_restoresValue() {
        val container = SavedStateContainer()
        val testValue = TestSavedStateValue("initial")

        container.putSavedStateValue("key1", testValue)

        val key1Bundle = savedState { putString("key", "restoredVal") }
        val bundle = savedState { putSavedState("key1", key1Bundle) }

        container.restoreState(bundle)

        val retrieved = container.getSavedStateValue<String, TestSavedStateValue>("key1")
        assertThat(retrieved).isSameInstanceAs(testValue)
        assertThat(retrieved?.value).isEqualTo("restoredVal")
        assertThat(testValue.restoredState).isEqualTo(key1Bundle)
    }

    @Test
    fun restoreState_thenPutSavedStateValue_restoresValue() {
        val container = SavedStateContainer()
        val key1Bundle = savedState { putString("key", "restoredVal") }
        val bundle = savedState { putSavedState("key1", key1Bundle) }

        container.restoreState(bundle)

        val testValue = TestSavedStateValue("initial")
        container.putSavedStateValue("key1", testValue)

        val retrieved = container.getSavedStateValue<String, TestSavedStateValue>("key1")
        assertThat(retrieved).isSameInstanceAs(testValue)
        assertThat(retrieved?.value).isEqualTo("restoredVal")
        assertThat(testValue.restoredState).isEqualTo(key1Bundle)
    }

    @Test
    fun contains_returnsTrueWhenKeyExistsAndFalseWhenNot() {
        val container = SavedStateContainer()
        val value = TestSavedStateValue("initial")

        assertThat("key" in container).isFalse()

        container.putSavedStateValue("key", value)
        assertThat("key" in container).isTrue()

        container.removeSavedStateValue<String, TestSavedStateValue>("key")
        assertThat("key" in container).isFalse()
    }

    @Test
    fun keys_returnsRegisteredKeys() {
        val container = SavedStateContainer()
        assertThat(container.keys()).isEmpty()

        container.putSavedStateValue("key1", TestSavedStateValue("1"))
        container.putSavedStateValue("key2", TestSavedStateValue("2"))

        assertThat(container.keys()).containsExactly("key1", "key2")
    }
}
