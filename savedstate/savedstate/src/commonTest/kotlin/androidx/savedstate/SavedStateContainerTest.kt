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
import androidx.kruth.assertThrows
import kotlin.test.Test

@IgnoreWebTarget
internal class SavedStateContainerTest : RobolectricTest() {

    private class TestSavedStateValue(override var value: String? = "") : SavedStateValue<String?> {
        var restoredState: SavedState? = null
        var saveStateCalled: Boolean = false

        override fun saveState(): SavedState {
            saveStateCalled = true
            return if (value != null) savedState { putString("key", value!!) } else savedState()
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
    fun addAndGet() {
        val container = SavedStateContainer()
        val testValue = TestSavedStateValue("hello")

        container["testKey"] = testValue

        val retrieved = container.get<String?>("testKey")
        assertThat(retrieved).isNotNull()
        assertThat(retrieved).isSameInstanceAs(testValue)
        assertThat(retrieved?.value).isEqualTo("hello")
    }

    @Test
    fun get_returnsNullWhenKeyNotFound() {
        val container = SavedStateContainer()
        val retrieved = container.get<String?>("nonExistent")
        assertThat(retrieved).isNull()
    }

    @Test
    fun addSavedStateValue_overwritesExistingValue() {
        val container = SavedStateContainer()
        val value1 = TestSavedStateValue("first")
        val value2 = TestSavedStateValue("second")

        container["key"] = value1
        container["key"] = value2

        val retrieved = container.get<String?>("key")
        assertThat(retrieved).isSameInstanceAs(value2)
        assertThat(retrieved?.value).isEqualTo("second")
    }

    @Test
    fun put_returnsNullWhenKeyAbsent() {
        val container = SavedStateContainer()
        val value = TestSavedStateValue("first")

        val previous = container.put("key", value)

        assertThat(previous).isNull()
        assertThat(container.get<String?>("key")).isSameInstanceAs(value)
    }

    @Test
    fun put_returnsPreviousValueWhenOverwriting() {
        val container = SavedStateContainer()
        val value1 = TestSavedStateValue("first")
        val value2 = TestSavedStateValue("second")

        container.put("key", value1)
        val previous = container.put("key", value2)

        assertThat(previous).isSameInstanceAs(value1)
        assertThat(container.get<String?>("key")).isSameInstanceAs(value2)
        assertThat(container.get<String?>("key")?.value).isEqualTo("second")
    }

    @Test
    fun restoreState_thenPut_restoresValueAndReturnsNull() {
        val container = SavedStateContainer()
        val key1Bundle = savedState { putString("key", "restoredVal") }
        val bundle = savedState { putSavedState("key1", key1Bundle) }

        container.restoreState(bundle)

        val testValue = TestSavedStateValue("initial")
        val previous = container.put("key1", testValue)

        assertThat(previous).isNull()
        val retrieved = container.get<String?>("key1")
        assertThat(retrieved).isSameInstanceAs(testValue)
        assertThat(retrieved?.value).isEqualTo("restoredVal")
        assertThat(testValue.restoredState).isEqualTo(key1Bundle)
    }

    @Test
    fun getOrSet_returnsExistingValueWhenPresent() {
        val container = SavedStateContainer()
        val existing = TestSavedStateValue("existing")
        container["key"] = existing

        var factoryCalled = false
        val result =
            container.getOrPut("key") {
                factoryCalled = true
                TestSavedStateValue("new")
            }

        assertThat(factoryCalled).isFalse()
        assertThat(result).isSameInstanceAs(existing)
        assertThat(result.value).isEqualTo("existing")
    }

    @Test
    fun getOrSet_evaluatesDefaultAndRegistersWhenAbsent() {
        val container = SavedStateContainer()
        val created = TestSavedStateValue("created")

        var factoryCalled = false
        val result =
            container.getOrPut("key") {
                factoryCalled = true
                created
            }

        assertThat(factoryCalled).isTrue()
        assertThat(result).isSameInstanceAs(created)
        assertThat(container.get<String?>("key")).isSameInstanceAs(created)
    }

    @Test
    fun getOrSet_throwsClassCastExceptionWhenExistingTypeDiffers() {
        val container = SavedStateContainer()
        val existing = TestSavedStateValue("existing")
        container["key"] = existing

        var factoryCalled = false
        assertThrows<ClassCastException> {
            val result: ProviderOnlyValue =
                container.getOrPut("key") {
                    factoryCalled = true
                    ProviderOnlyValue(42)
                }
            assertThat(result).isNotNull()
        }

        assertThat(factoryCalled).isFalse()
        assertThat(container.get<String?>("key")).isSameInstanceAs(existing)
    }

    @Test
    fun removeSavedStateValue_removesAndReturnsValue() {
        val container = SavedStateContainer()
        val testValue = TestSavedStateValue("removeMe")
        container["key"] = testValue

        val removed = container.remove<String?>("key")

        assertThat(removed).isSameInstanceAs(testValue)
        assertThat(container.get<String?>("key")).isNull()
    }

    @Test
    fun createOrGetContainer_createsNewChildContainer() {
        val parent = SavedStateContainer()
        val child = parent.getOrCreateContainer("child")

        assertThat(child).isNotNull()

        // Same call returns the exact same child container
        val sameChild = parent.getOrCreateContainer("child")
        assertThat(sameChild).isSameInstanceAs(child)
    }

    @Test
    fun nestedContainers_saveAndRestoreState() {
        val parent = SavedStateContainer()
        val child = parent.getOrCreateContainer("child")
        val childValue = TestSavedStateValue("childState")
        child["leafKey"] = childValue

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
        val restoredChild = restoredParent.getOrCreateContainer("child")
        val restoredChildValue = TestSavedStateValue()
        restoredChild["leafKey"] = restoredChildValue

        // When restoring root with the bundle
        restoredParent.restoreState(savedState)

        // The nested child should have received its restoration state
        // Note: SavedStateContainerValue restores the bundle into child container
        assertThat(restoredChildValue.restoredState).isNotNull()
        assertThat(restoredChildValue.value).isEqualTo("childState")
    }

    @Test
    fun nestedContainers_restoreStateBeforeGetOrCreateContainer() {
        val parent = SavedStateContainer()
        val child = parent.getOrCreateContainer("child")
        val childValue = TestSavedStateValue("childState")
        child["leafKey"] = childValue

        val savedState = parent.saveState()

        // Restore into a fresh parent BEFORE getOrCreateContainer is called
        val restoredParent = SavedStateContainer()
        restoredParent.restoreState(savedState)

        val restoredChild = restoredParent.getOrCreateContainer("child")
        val restoredChildValue = TestSavedStateValue()
        restoredChild["leafKey"] = restoredChildValue

        assertThat(restoredChildValue.restoredState).isNotNull()
        assertThat(restoredChildValue.value).isEqualTo("childState")
    }

    @Test
    fun savedState_collectsStateFromAllProviders() {
        val container = SavedStateContainer()
        val value1 = TestSavedStateValue("val1")
        val value2 = ProviderOnlyValue(100)

        container["key1"] = value1
        container["key2"] = value2

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

        container["key1"] = value1
        container["key2"] = value2

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

        container["key1"] = value1
        container.restoreState(null)

        assertThat(value1.restoredState).isNull()
    }

    @Test
    fun set_thenRestoreState_restoresValue() {
        val container = SavedStateContainer()
        val testValue = TestSavedStateValue("initial")

        container["key1"] = testValue

        val key1Bundle = savedState { putString("key", "restoredVal") }
        val bundle = savedState { putSavedState("key1", key1Bundle) }

        container.restoreState(bundle)

        val retrieved = container.get<String?>("key1")
        assertThat(retrieved).isSameInstanceAs(testValue)
        assertThat(retrieved?.value).isEqualTo("restoredVal")
        assertThat(testValue.restoredState).isEqualTo(key1Bundle)
    }

    @Test
    fun restoreState_thenSet_restoresValue() {
        val container = SavedStateContainer()
        val key1Bundle = savedState { putString("key", "restoredVal") }
        val bundle = savedState { putSavedState("key1", key1Bundle) }

        container.restoreState(bundle)

        val testValue = TestSavedStateValue("initial")
        container["key1"] = testValue

        val retrieved = container.get<String?>("key1")
        assertThat(retrieved).isSameInstanceAs(testValue)
        assertThat(retrieved?.value).isEqualTo("restoredVal")
        assertThat(testValue.restoredState).isEqualTo(key1Bundle)
    }

    @Test
    fun contains_returnsTrueWhenKeyExistsAndFalseWhenNot() {
        val container = SavedStateContainer()
        val value = TestSavedStateValue("initial")

        assertThat("key" in container).isFalse()

        container["key"] = value
        assertThat("key" in container).isTrue()

        container.remove<String?>("key")
        assertThat("key" in container).isFalse()
    }

    @Test
    fun contains_returnsTrueWhenKeyExistsAndValueIsNull() {
        val container = SavedStateContainer()
        val value = TestSavedStateValue(null)

        assertThat("key" in container).isFalse()

        container["key"] = value
        assertThat("key" in container).isTrue()

        container.remove<String?>("key")
        assertThat("key" in container).isFalse()
    }

    @Test
    fun keys_returnsRegisteredKeys() {
        val container = SavedStateContainer()
        assertThat(container.keys()).isEmpty()

        container["key1"] = TestSavedStateValue("1")
        container["key2"] = TestSavedStateValue("2")

        assertThat(container.keys()).containsExactly("key1", "key2")
    }
}
