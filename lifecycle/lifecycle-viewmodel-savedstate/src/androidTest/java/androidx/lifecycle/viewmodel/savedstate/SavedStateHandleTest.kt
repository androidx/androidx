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

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.test.annotation.UiThreadTest
import androidx.test.espresso.base.MainThread
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
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
    fun testRemoveWithLD() {
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
        accessor.getLiveData<String>("ld").value = "a"
        assertThat(accessor.keys().size).isEqualTo(2)
        assertThat(accessor.keys()).containsExactly("s", "ld")
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
        observeForever {
            received = true
        }
        assertThat(received).isFalse()
    }
}
