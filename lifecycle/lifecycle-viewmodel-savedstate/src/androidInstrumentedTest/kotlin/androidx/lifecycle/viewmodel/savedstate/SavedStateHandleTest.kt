/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.lifecycle.viewmodel.savedstate

import android.os.Bundle
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class SavedStateHandleTest {

    @Test
    @UiThreadTest
    fun testSetGet() {
        val handle = SavedStateHandle()
        handle.set("foo", "trololo")
        assertThat(handle.get<String?>("foo")).isEqualTo("trololo")
        val fooLd = handle.getLiveData<String>("foo")
        assertThat(fooLd.value).isEqualTo("trololo")
        fooLd.value = "another"
        assertThat(handle.get<String?>("foo")).isEqualTo("another")
    }

    @Test
    @UiThreadTest
    fun testSetNullGet() {
        val handle = SavedStateHandle()
        handle.set("foo", null)
        assertThat(handle.get<String?>("foo")).isEqualTo(null)
        val fooLd = handle.getLiveData<String>("foo")
        assertThat(fooLd.value).isEqualTo(null)
        fooLd.value = "another"
        assertThat(handle.get<String?>("foo")).isEqualTo("another")
        fooLd.value = null
        assertThat(handle.get<String?>("foo")).isEqualTo(null)
    }

    @Test
    @UiThreadTest
    fun testSetObserve() {
        val handle = SavedStateHandle()
        val liveData = handle.getLiveData<Int>("a")
        var lastValue = 0
        liveData.observeForever { newValue -> lastValue = newValue }
        handle.set("a", 261)
        assertThat(lastValue).isEqualTo(261)
    }

    @Test
    @UiThreadTest
    fun testContains() {
        val handle = SavedStateHandle()
        val foo = handle.getLiveData<Int>("foo")
        assertThat(handle.contains("foo")).isFalse()
        foo.value = 712
        assertThat(handle.contains("foo")).isTrue()
        handle.get<String>("foo2")
        assertThat(handle.contains("foo2")).isFalse()
        handle.set("foo2", "spb")
        assertThat(handle.contains("foo2")).isTrue()
    }

    @Test
    @UiThreadTest
    fun testRemove() {
        val handle = SavedStateHandle()
        handle.set("s", "pb")
        assertThat(handle.contains("s")).isTrue()

        assertThat(handle.remove<String?>("s")).isEqualTo("pb")
        assertThat(handle.contains("s")).isFalse()
        assertThat(handle.remove<String?>("don't exist")).isNull()
    }

    @Test
    @UiThreadTest
    fun testRemoveWithLiveData() {
        val handle = SavedStateHandle()
        handle.set("spb", 1703)
        val ld = handle.getLiveData<Int>("spb")
        assertThat(handle.contains("spb")).isTrue()
        var invalidUpdate = false
        ld.observeForever {
            if (invalidUpdate) throw AssertionError("LiveData must be already detached")
        }
        invalidUpdate = true // next removes key
        assertThat(handle.remove<Int>("spb")).isEqualTo(1703)
        assertThat(handle.contains("spb")).isFalse()
        assertThat(handle.remove<Int?>("spb")).isNull()
        handle.set("spb", 1914)
        invalidUpdate = false
        ld.value = 1924
        assertThat(handle.get<Int?>("spb")).isEqualTo(1914)
    }

    @Test
    @UiThreadTest
    fun testRemoveWithStateFlow() = runTest {
        val handle = SavedStateHandle()
        handle.set("spb", 1703)
        val flow = handle.getStateFlow("spb", 1309)
        assertThat(handle.contains("spb")).isTrue()
        var invalidUpdate = false
        backgroundScope.launch {
            flow.collect {
                if (invalidUpdate) throw AssertionError("Flow must be already detached")
            }
        }
        invalidUpdate = true // next removes key
        assertThat(handle.remove<Int>("spb")).isEqualTo(1703)
        assertThat(handle.contains("spb")).isFalse()
        assertThat(handle.remove<Int?>("spb")).isNull()
        handle.set("spb", 1914)
        invalidUpdate = false
    }

    @Test
    @UiThreadTest
    fun testRemoveWithMutableStateFlow() = runTest {
        val handle = SavedStateHandle()
        handle.set("spb", 1703)
        val flow = handle.getMutableStateFlow("spb", 1309)
        assertThat(handle.contains("spb")).isTrue()
        var invalidUpdate = false
        backgroundScope.launch {
            flow.collect {
                if (invalidUpdate) throw AssertionError("Flow must be already detached")
            }
        }
        invalidUpdate = true // next removes key
        assertThat(handle.remove<Int>("spb")).isEqualTo(1703)
        assertThat(handle.contains("spb")).isFalse()
        assertThat(handle.remove<Int?>("spb")).isNull()
        handle.set("spb", 1914)
        invalidUpdate = false
        flow.value = 1924
        assertThat(handle.get<Int?>("spb")).isEqualTo(1914)
    }

    @Test
    @UiThreadTest
    fun newLiveData_noDefault() {
        val handle = SavedStateHandle()
        val ld: LiveData<String?> = handle.getLiveData("aa")
        ld.assertNoValue()
    }

    @Test
    @UiThreadTest
    fun newLiveData_nullInitial() {
        val handle = SavedStateHandle()
        val ld: LiveData<String?> = handle.getLiveData("aa", null)
        ld.assertValue(null)
    }

    @Test
    @UiThreadTest
    fun newliveData_withInitial() {
        val handle = SavedStateHandle()
        val ld: LiveData<String?> = handle.getLiveData("aa", "xx")
        ld.assertValue("xx")
    }

    @Test
    @UiThreadTest
    fun newliveData_withInitialGet() {
        val handle = SavedStateHandle()
        val ld: LiveData<String?> = handle.getLiveData("aa", "xx")
        ld.assertValue("xx")
        assertThat(handle.get<String?>("aa")).isEqualTo("xx")
    }

    @Test
    @UiThreadTest
    fun newLiveData_existingValue_withInitial() {
        val handle = SavedStateHandle()
        handle["aa"] = "existing"
        val ld: LiveData<String?> = handle.getLiveData("aa", "xx")
        ld.assertValue("existing")
    }

    @Test
    @UiThreadTest
    fun newLiveData_existingValue_withNullInitial() {
        val handle = SavedStateHandle()
        handle["aa"] = "existing"
        val ld: LiveData<String?> = handle.getLiveData("aa", null)
        ld.assertValue("existing")
    }

    @Test
    @UiThreadTest
    fun newLiveData_existingNullValue_withInitial() {
        val handle = SavedStateHandle()
        handle["aa"] = null
        val ld: LiveData<String?> = handle.getLiveData("aa", "xx")
        ld.assertValue(null)
    }

    @Test
    @UiThreadTest
    fun testKeySet() {
        val accessor = SavedStateHandle()
        accessor.set("s", "pb")
        accessor.getLiveData<String>("no value ld")
        accessor.getLiveData<String>("ld").value = "a"
        accessor.setSavedStateProvider("provider") { Bundle() }
        assertThat(accessor.keys().size).isEqualTo(4)
        assertThat(accessor.keys()).containsExactly("s", "ld", "provider", "no value ld")
    }

    @Test
    fun savedStateValueFlow() = runTest {
        val handle = SavedStateHandle()

        handle
            .getStateFlow("test", 1)
            .take(3)
            .withIndex()
            .onEach { (index, value) ->
                val expectedValue = index + 1
                assertWithMessage("Flow emitted unexpected value")
                    .that(value)
                    .isEqualTo(expectedValue)

                if (expectedValue < 3) {
                    handle["test"] = expectedValue + 1
                }
            }
            .collect()
    }

    @Test
    @UiThreadTest
    fun newFlow_nullInitial() = runTest {
        val handle = SavedStateHandle()
        handle
            .getStateFlow<String?>("aa", null)
            .take(1)
            .onEach { assertWithMessage("Flow should emit a null value").that(it).isNull() }
            .collect()
    }

    @Test
    @UiThreadTest
    fun newFlow_withInitialGet() = runTest {
        val handle = SavedStateHandle()
        val flow = handle.getStateFlow("aa", "xx")

        flow
            .take(1)
            .onEach {
                assertWithMessage("Flow should emit the initial value").that(it).isEqualTo("xx")
            }
            .collect()

        assertThat(flow.value).isEqualTo("xx")
    }

    @Test
    @UiThreadTest
    fun newFlow_existingValue_withInitial() = runTest {
        val handle = SavedStateHandle()
        handle["aa"] = "existing"

        handle
            .getStateFlow("aa", "xx")
            .take(1)
            .onEach {
                assertWithMessage("Flow should emit a null value").that(it).isEqualTo("existing")
            }
            .collect()
    }

    @Test
    @UiThreadTest
    fun newFlow_existingValue_withNullInitial() = runTest {
        val handle = SavedStateHandle()
        handle["aa"] = "existing"

        handle
            .getStateFlow<String?>("aa", null)
            .take(1)
            .onEach {
                assertWithMessage("Flow should emit the set value").that(it).isEqualTo("existing")
            }
            .collect()
    }

    @Test
    @UiThreadTest
    fun newFlow_existingNullValue_withInitial() = runTest {
        val handle = SavedStateHandle()
        handle["aa"] = null

        handle
            .getStateFlow<String?>("aa", "xx")
            .take(1)
            .onEach { assertWithMessage("Flow should emit a null value").that(it).isNull() }
            .collect()
    }

    @Test
    @UiThreadTest
    fun newFlow_setNullValue_nonNullFlow() = runTest {
        val handle = SavedStateHandle()
        val flow = handle.getStateFlow("aa", "xx")

        flow
            .take(1)
            .onEach {
                assertWithMessage("Flow should emit the initial value").that(it).isEqualTo("xx")
            }
            .collect()

        handle["aa"] = null
        assertThat(flow.value).isNull()
    }

    @Test
    @UiThreadTest
    fun flow_setByLiveDataSetValue() = runTest {
        val handle = SavedStateHandle()
        val flow = handle.getStateFlow("aa", "xx")

        flow
            .take(1)
            .onEach {
                assertWithMessage("Flow should emit the initial value").that(it).isEqualTo("xx")
            }
            .collect()

        assertThat(flow.value).isEqualTo("xx")

        val ld = handle.getLiveData<String>("aa")
        ld.value = "yy"
        ld.assertValue("yy")
        assertThat(flow.value).isEqualTo("yy")
    }

    @Test
    fun getMutableStateFlow_savedStateValueFlow() = runTest {
        val handle = SavedStateHandle()

        handle
            .getMutableStateFlow("test", 1)
            .take(3)
            .withIndex()
            .onEach { (index, value) ->
                val expectedValue = index + 1
                assertWithMessage("Flow emitted unexpected value")
                    .that(value)
                    .isEqualTo(expectedValue)

                if (expectedValue < 3) {
                    handle["test"] = expectedValue + 1
                }
            }
            .collect()
    }

    @Test
    @UiThreadTest
    fun getMutableStateFlow_nullInitial() = runTest {
        val handle = SavedStateHandle()
        handle
            .getMutableStateFlow<String?>("aa", null)
            .take(1)
            .onEach { assertWithMessage("Flow should emit a null value").that(it).isNull() }
            .collect()
    }

    @Test
    @UiThreadTest
    fun getMutableStateFlow_withInitialGet() = runTest {
        val handle = SavedStateHandle()
        val flow = handle.getMutableStateFlow("aa", "xx")

        flow
            .take(1)
            .onEach {
                assertWithMessage("Flow should emit the initial value").that(it).isEqualTo("xx")
            }
            .collect()

        assertThat(flow.value).isEqualTo("xx")
    }

    @Test
    @UiThreadTest
    fun getMutableStateFlow_existingValue_withInitial() = runTest {
        val handle = SavedStateHandle()
        handle["aa"] = "existing"

        handle
            .getMutableStateFlow("aa", "xx")
            .take(1)
            .onEach {
                assertWithMessage("Flow should emit a null value").that(it).isEqualTo("existing")
            }
            .collect()
    }

    @Test
    @UiThreadTest
    fun getMutableStateFlow_existingValue_withNullInitial() = runTest {
        val handle = SavedStateHandle()
        handle["aa"] = "existing"

        handle
            .getMutableStateFlow<String?>("aa", null)
            .take(1)
            .onEach {
                assertWithMessage("Flow should emit the set value").that(it).isEqualTo("existing")
            }
            .collect()
    }

    @Test
    @UiThreadTest
    fun getMutableStateFlow_existingNullValue_withInitial() = runTest {
        val handle = SavedStateHandle()
        handle["aa"] = null

        handle
            .getMutableStateFlow<String?>("aa", "xx")
            .take(1)
            .onEach { assertWithMessage("Flow should emit a null value").that(it).isNull() }
            .collect()
    }

    @Test
    @UiThreadTest
    fun getMutableStateFlow_setNullValue_nonNullFlow() = runTest {
        val handle = SavedStateHandle()
        val flow = handle.getMutableStateFlow("aa", "xx")

        flow
            .take(1)
            .onEach {
                assertWithMessage("Flow should emit the initial value").that(it).isEqualTo("xx")
            }
            .collect()

        handle["aa"] = null
        assertThat(flow.value).isNull()
    }

    @Test
    fun getMutableStateFlow_keyUsedByLiveData() = runTest {
        val handle = SavedStateHandle()
        handle.getLiveData(key = "key", initialValue = "test")

        assertThrows(IllegalArgumentException::class.java) {
            handle.getMutableStateFlow(key = "key", initialValue = "test")
        }
    }

    @Test
    fun getLiveData_keyUsedByMutableStateFlow() = runTest {
        val handle = SavedStateHandle()
        handle.getMutableStateFlow(key = "key", initialValue = "test")

        assertThrows(IllegalArgumentException::class.java) {
            handle.getLiveData(key = "key", initialValue = "test")
        }
    }

    @Test
    fun getRegular_keyUsedByMutableStateFlow() = runTest {
        val handle = SavedStateHandle()
        val mutableFlow = handle.getMutableStateFlow(key = "key", initialValue = 1)

        assertThat(mutableFlow.value).isEqualTo(1)
        assertThat(handle.get<Int>("key")).isEqualTo(1)

        mutableFlow.value = 3
        assertThat(mutableFlow.value).isEqualTo(3)
        assertThat(handle.get<Int>("key")).isEqualTo(3)

        handle["key"] = 4
        assertThat(mutableFlow.value).isEqualTo(4)
        assertThat(handle.get<Int>("key")).isEqualTo(4)
    }

    @Test
    fun getStateFlow_keyUsedByMutableStateFlow() = runTest {
        val handle = SavedStateHandle()
        val mutableFlow = handle.getMutableStateFlow(key = "key", initialValue = 1)
        val flow = handle.getStateFlow(key = "key", initialValue = 2)

        assertThat(mutableFlow.value).isEqualTo(1)
        assertThat(flow.value).isEqualTo(1)

        mutableFlow.value = 3
        assertThat(mutableFlow.value).isEqualTo(3)
        assertThat(flow.value).isEqualTo(3)

        handle["key"] = 4
        assertThat(mutableFlow.value).isEqualTo(4)
        assertThat(flow.value).isEqualTo(4)
    }

    @Test
    fun savedStateProvider() = runTest {
        val handle = SavedStateHandle()
        for (i in 1..10) {
            handle["Regular$i"] = i
            handle.getLiveData(key = "MutableLiveData$i", initialValue = i)
            handle.getStateFlow(key = "StateFlow$i", initialValue = i)
            handle.getMutableStateFlow(key = "MutableStateFlow$i", initialValue = i)
            handle.setSavedStateProvider(
                key = "SavedStateProvider$i",
                provider = { savedState(mapOf("SavedState$i" to i)) },
            )
        }

        val savedState = handle.savedStateProvider().saveState()

        assertThat(savedState.size()).isEqualTo(50)
        for (i in 1..10) {
            val regularValue = savedState.read { getInt("Regular$i") }
            assertThat(regularValue).isEqualTo(i)

            val mutableLiveDataValue = savedState.read { getInt("MutableLiveData$i") }
            assertThat(mutableLiveDataValue).isEqualTo(i)

            val stateFlowValue = savedState.read { getInt("StateFlow$i") }
            assertThat(stateFlowValue).isEqualTo(i)

            val mutableStateFlowValue = savedState.read { getInt("MutableStateFlow$i") }
            assertThat(mutableStateFlowValue).isEqualTo(i)

            val actualSavedState = savedState.read { getSavedState("SavedStateProvider$i") }
            val expectedSavedState = savedState(mapOf("SavedState$i" to i))
            val isDeepEquals = actualSavedState.read { contentDeepEquals(expectedSavedState) }
            assertThat(isDeepEquals).isTrue()
        }
    }

    @MainThread
    private fun <T : Any?> LiveData<T>.assertValue(expected: T?) {
        var received = false
        observeForever {
            received = true
            assertThat(it).isEqualTo(expected)
        }
        assertThat(received).isTrue()
    }

    private fun <T> LiveData<T>.assertNoValue() {
        var received = false
        observeForever { received = true }
        assertThat(received).isFalse()
    }
}
