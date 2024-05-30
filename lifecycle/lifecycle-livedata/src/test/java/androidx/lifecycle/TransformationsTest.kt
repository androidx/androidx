/*
 * Copyright (C) 2017 The Android Open Source Project
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

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.lifecycle.util.InstantTaskExecutor
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("unchecked_cast")
@RunWith(JUnit4::class)
class TransformationsTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val owner = TestLifecycleOwner(coroutineDispatcher = UnconfinedTestDispatcher())

    // region map
    @Test
    fun map() {
        ArchTaskExecutor.getInstance().setDelegate(InstantTaskExecutor())

        val sourceLiveData = MutableLiveData<String>()
        val observer = TestObserver<Int>()

        val mapLiveData = sourceLiveData.map { it.length }
        mapLiveData.observe(owner, observer)
        sourceLiveData.value = "four"

        assertThat(observer.values).containsExactly(4)
    }

    @Test
    fun map_initialValueIsSet() {
        ArchTaskExecutor.getInstance().setDelegate(InstantTaskExecutor())

        val initialValue = "initialValue"
        val sourceLiveData = MutableLiveData(initialValue)

        val mapLiveData = sourceLiveData.map { it }

        assertThat(mapLiveData.isInitialized).isTrue()
        assertThat(mapLiveData.value).isEqualTo(initialValue)
        assertThat(sourceLiveData.value).isEqualTo(initialValue)
    }

    @Test
    fun map_initialValueNull() {
        ArchTaskExecutor.getInstance().setDelegate(InstantTaskExecutor())

        val sourceLiveData = MutableLiveData<String?>(null)
        val output = "testOutput"

        val mapLiveData = sourceLiveData.map { output }

        assertThat(mapLiveData.isInitialized).isTrue()
        assertThat(mapLiveData.value).isEqualTo(output)
        assertThat(sourceLiveData.value).isNull()
    }

    @Test
    fun map_createsOnBackgroundThread() {
        ArchTaskExecutor.getInstance().setDelegate(InstantTaskOnBackgroundTaskExecutor())

        val initialValue = "value"
        val sourceLiveData = MutableLiveData(initialValue)

        val mapLiveData = sourceLiveData.map { "mapped $it" }

        assertThat(mapLiveData.isInitialized).isTrue()
        assertThat(mapLiveData.value).isEqualTo("mapped $initialValue")
        assertThat(sourceLiveData.value).isEqualTo(initialValue)
    }

    @Test
    fun map_observesOnBackgroundThread_throwsException() {
        ArchTaskExecutor.getInstance().setDelegate(InstantTaskOnBackgroundTaskExecutor())

        val sourceLiveData = MutableLiveData("value")
        val observer = TestObserver<String>()

        val mapLiveData = sourceLiveData.map { "mapped $it" }
        val error = runCatching { mapLiveData.observe(owner, observer) }.exceptionOrNull()

        with(assertThat(error)) {
            isInstanceOf(IllegalStateException::class.java)
            hasMessageThat().isEqualTo("Cannot invoke observe on a background thread")
        }
    }

    @Test
    fun map_noObsoleteValue() {
        ArchTaskExecutor.getInstance().setDelegate(InstantTaskExecutor())

        val sourceLiveData = MutableLiveData<Int>()
        val mapLiveData = sourceLiveData.map { value: Int -> value * value }
        val observer = TestObserver<Int>()

        mapLiveData.value = 1
        mapLiveData.observeForever(observer)
        mapLiveData.removeObserver(observer)
        sourceLiveData.value = 2
        mapLiveData.observeForever(observer)

        assertThat(observer.values).containsExactly(1, 4)
    }

    // endregion

    // region switchMap
    @Test
    fun switchMap() {
        ArchTaskExecutor.getInstance().setDelegate(InstantTaskExecutor())

        val sourceLiveData = MutableLiveData<Int>()
        val firstLiveData = MutableLiveData<String>()
        val secondLiveData = MutableLiveData<String>()
        val observer = TestObserver<String>()

        val switchLiveData =
            sourceLiveData.switchMap { input -> if (input == 1) firstLiveData else secondLiveData }
        switchLiveData.observe(owner, observer)

        firstLiveData.value = "first"
        sourceLiveData.value = 1
        secondLiveData.value = "second"
        sourceLiveData.value = 2
        firstLiveData.value = "failure"

        assertThat(observer.values).containsExactly("first", "second")
    }

    @Test
    fun switchMap_initialValueSet() {
        ArchTaskExecutor.getInstance().setDelegate(InstantTaskExecutor())

        val initialValue = "initialValue"
        val sourceLiveData = MutableLiveData(true)
        val anotherLiveData = MutableLiveData(initialValue)

        val switchMapLiveData = sourceLiveData.switchMap { anotherLiveData }

        assertThat(switchMapLiveData.isInitialized).isTrue()
        assertThat(switchMapLiveData.value).isEqualTo(initialValue)
        assertThat(anotherLiveData.value).isEqualTo(initialValue)
    }

    @Test
    fun switchMap_noInitialValue_notInitialized() {
        ArchTaskExecutor.getInstance().setDelegate(InstantTaskExecutor())

        val sourceLiveData = MutableLiveData(true)
        val anotherLiveData = MutableLiveData<String>()

        val switchMapLiveData = sourceLiveData.switchMap { anotherLiveData }

        assertThat(switchMapLiveData.isInitialized).isFalse()
    }

    @Test
    fun switchMap_initialValueNull() {
        ArchTaskExecutor.getInstance().setDelegate(InstantTaskExecutor())

        val sourceLiveData = MutableLiveData<String?>(null)
        val anotherLiveData = MutableLiveData<String?>()

        val switchMapLiveData = sourceLiveData.switchMap { anotherLiveData }

        assertThat(switchMapLiveData.isInitialized).isFalse()
    }

    @Test
    fun switchMap_sameLiveData() {
        ArchTaskExecutor.getInstance().setDelegate(InstantTaskExecutor())

        val initialValue = "initialValue"
        val modifiedValue = "modifiedValue"
        val observer = TestObserver<String?>()
        val sourceLiveData = MutableLiveData(true)
        val anotherLiveData = MutableLiveData(initialValue)

        val switchMapLiveData = sourceLiveData.switchMap { anotherLiveData }
        switchMapLiveData.observe(owner, observer)

        anotherLiveData.value = modifiedValue

        assertThat(switchMapLiveData.value).isEqualTo(modifiedValue)
        assertThat(observer.values).containsExactly(initialValue, modifiedValue)
    }

    @Test
    fun switchMap_noRedispatch() {
        ArchTaskExecutor.getInstance().setDelegate(InstantTaskExecutor())

        val sourceLiveData = MutableLiveData<Int>()
        val anotherLiveData = MutableLiveData<String>()
        val observer = TestObserver<String>()

        val switchMapLiveData = sourceLiveData.switchMap { anotherLiveData }
        switchMapLiveData.observe(owner, observer)

        anotherLiveData.value = "first"
        sourceLiveData.value = 1
        sourceLiveData.value = 2

        assertThat(observer.values).containsExactly("first")
    }

    @Test
    fun switchMap_toNull() {
        ArchTaskExecutor.getInstance().setDelegate(InstantTaskExecutor())

        val sourceLiveData = MutableLiveData<Int>()
        val anotherLiveData = MutableLiveData<String>()
        val observer = TestObserver<String>()

        val switchMapLiveData =
            sourceLiveData.switchMap { input: Int -> if (input == 1) anotherLiveData else null }
        switchMapLiveData.observe(owner, observer)

        anotherLiveData.value = "first"
        sourceLiveData.value = 1
        sourceLiveData.value = 2

        assertThat(anotherLiveData.hasObservers()).isFalse()
        assertThat(observer.values).containsExactly("first")
    }

    @Test
    fun switchMap_createsOnBackgroundThread() {
        ArchTaskExecutor.getInstance().setDelegate(InstantTaskOnBackgroundTaskExecutor())

        val initialValue = "initialValue"
        val sourceLiveData = MutableLiveData(true)
        val anotherLiveData = MutableLiveData(initialValue)

        val switchMapLiveData = sourceLiveData.switchMap { anotherLiveData }

        assertThat(switchMapLiveData.isInitialized).isTrue()
        assertThat(switchMapLiveData.value).isEqualTo(initialValue)
    }

    @Test
    fun switchMap_observesOnBackgroundThread_throwsException() {
        ArchTaskExecutor.getInstance().setDelegate(InstantTaskOnBackgroundTaskExecutor())

        val initialValue = "initialValue"
        val sourceLiveData = MutableLiveData(true)
        val anotherLiveData = MutableLiveData(initialValue)
        val observer = TestObserver<String>()

        val mapLiveData = sourceLiveData.switchMap { anotherLiveData }
        val error = runCatching { mapLiveData.observe(owner, observer) }.exceptionOrNull()

        with(assertThat(error)) {
            isInstanceOf(IllegalStateException::class.java)
            hasMessageThat().isEqualTo("Cannot invoke observe on a background thread")
        }
    }

    // endregion

    // region distinctUntilChanged
    @Test
    fun distinctUntilChanged_initialValueIsSet() {
        ArchTaskExecutor.getInstance().setDelegate(InstantTaskExecutor())

        val sourceLiveData = MutableLiveData("value")
        val observer = TestObserver<String>()

        val distinctLiveData = sourceLiveData.distinctUntilChanged()
        distinctLiveData.observe(owner, observer)

        assertThat(observer.values).containsExactly("value")
        assertThat(distinctLiveData.value).isEqualTo("value")
    }

    @Test
    fun distinctUntilChanged_onInitialNullValue_triggersObserver() {
        ArchTaskExecutor.getInstance().setDelegate(InstantTaskExecutor())

        val sourceLiveData = MutableLiveData<String?>(null)
        val observer = TestObserver<String?>()

        val distinctLiveData = sourceLiveData.distinctUntilChanged()
        distinctLiveData.observe(owner, observer)

        assertThat(observer.values).containsExactly(null)
        assertThat(distinctLiveData.value).isNull()
    }

    @Test
    fun distinctUntilChanged_initialNullValue() {
        ArchTaskExecutor.getInstance().setDelegate(InstantTaskExecutor())

        val sourceLiveData = MutableLiveData<String>()
        val observer = TestObserver<String>()

        val distinctLiveData = sourceLiveData.distinctUntilChanged()
        distinctLiveData.observe(owner, observer)

        assertThat(distinctLiveData.value).isNull()
    }

    @Test
    fun distinctUntilChanged_filtersValueRepetitions() {
        ArchTaskExecutor.getInstance().setDelegate(InstantTaskExecutor())

        val sourceLiveData = MutableLiveData<String>()
        val observer = TestObserver<String>()

        val distinctLiveData = sourceLiveData.distinctUntilChanged()
        distinctLiveData.observe(owner, observer)

        sourceLiveData.value = "new value"
        sourceLiveData.value = "new value"
        sourceLiveData.value = "newer value"

        assertThat(observer.values).containsExactly("new value", "newer value")
    }

    @Test
    fun distinctUntilChanged_createsOnBackgroundThread() {
        ArchTaskExecutor.getInstance().setDelegate(InstantTaskOnBackgroundTaskExecutor())

        val sourceLiveData = MutableLiveData("value")
        val distinctLiveData = sourceLiveData.distinctUntilChanged()

        assertThat(distinctLiveData.value).isEqualTo("value")
    }

    @Test
    fun distinctUntilChanged_observesOnMainThread() {
        ArchTaskExecutor.getInstance().setDelegate(InstantTaskExecutor())

        val sourceLiveData = MutableLiveData("value")
        val observer = TestObserver<String>()

        val distinctLiveData = sourceLiveData.distinctUntilChanged()
        distinctLiveData.observe(owner, observer)

        assertThat(observer.values.size).isEqualTo(1)
        assertThat(distinctLiveData.value).isEqualTo("value")
    }

    @Test
    fun distinctUntilChanged_observesOnBackgroundThread_throwsException() {
        ArchTaskExecutor.getInstance().setDelegate(InstantTaskOnBackgroundTaskExecutor())

        val sourceLiveData = MutableLiveData("value")
        val observer = TestObserver<String>()

        val distinctLiveData = sourceLiveData.distinctUntilChanged()
        val error = runCatching { distinctLiveData.observe(owner, observer) }.exceptionOrNull()

        with(assertThat(error)) {
            isInstanceOf(IllegalStateException::class.java)
            hasMessageThat().isEqualTo("Cannot invoke observe on a background thread")
        }
    }

    // endregion

    private class TestObserver<T>(val values: MutableList<T> = mutableListOf()) : Observer<T> {
        override fun onChanged(value: T) {
            values += value
        }
    }

    private class InstantTaskOnBackgroundTaskExecutor : InstantTaskExecutor() {
        override fun isMainThread(): Boolean = false
    }
}
