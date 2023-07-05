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

import androidx.arch.core.executor.ArchTaskExecutor.getInstance
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.lifecycle.util.InstantTaskExecutor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.only
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify

@Suppress("unchecked_cast")
@RunWith(JUnit4::class)
class TransformationsTest {
    private lateinit var owner: TestLifecycleOwner

    @Before
    fun swapExecutorDelegate() {
        getInstance().setDelegate(InstantTaskExecutor())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        owner = TestLifecycleOwner(
            Lifecycle.State.STARTED,
            UnconfinedTestDispatcher(null, null)
        )
    }

    @Test
    fun testMap() {
        val source: LiveData<String> = MutableLiveData()
        val mapped = source.map(String::length)
        val observer = mock(Observer::class.java) as Observer<Int>
        mapped.observe(owner, observer)
        source.value = "four"
        verify(observer).onChanged(4)
    }

    @Test
    fun testSwitchMap() {
        val trigger: LiveData<Int> = MutableLiveData()
        val first: LiveData<String> = MutableLiveData()
        val second: LiveData<String> = MutableLiveData()
        val result = trigger.switchMap { input ->
            if (input == 1) {
                first
            } else {
                second
            }
        }
        val observer = mock(Observer::class.java) as Observer<String>
        result.observe(owner, observer)
        verify(observer, never()).onChanged(anyString())
        first.value = "first"
        trigger.value = 1
        verify(observer).onChanged("first")
        second.value = "second"
        reset(observer)
        verify(observer, never()).onChanged(anyString())
        trigger.value = 2
        verify(observer).onChanged("second")
        reset(observer)
        first.value = "failure"
        verify(observer, never()).onChanged(anyString())
    }

    @Test
    fun testSwitchMap2() {
        val trigger: LiveData<Int> = MutableLiveData()
        val first: LiveData<String> = MutableLiveData()
        val second: LiveData<String> = MutableLiveData()
        val result = trigger.switchMap { input: Int ->
            if (input == 1) {
                first
            } else {
                second
            }
        }
        val observer = mock(Observer::class.java) as Observer<String>
        result.observe(owner, observer)
        verify(observer, never()).onChanged(anyString())
        trigger.value = 1
        verify(observer, never()).onChanged(anyString())
        first.value = "fi"
        verify(observer).onChanged("fi")
        first.value = "rst"
        verify(observer).onChanged("rst")
        second.value = "second"
        reset(observer)
        verify(observer, never()).onChanged(anyString())
        trigger.value = 2
        verify(observer).onChanged("second")
        reset(observer)
        first.value = "failure"
        verify(observer, never()).onChanged(anyString())
    }

    @Test
    fun testNoRedispatchSwitchMap() {
        val trigger: LiveData<Int> = MutableLiveData()
        val first: LiveData<String> = MutableLiveData()
        val result = trigger.switchMap { first }
        val observer = mock(Observer::class.java) as Observer<String>
        result.observe(owner, observer)
        verify(observer, never()).onChanged(anyString())
        first.value = "first"
        trigger.value = 1
        verify(observer).onChanged("first")
        reset(observer)
        trigger.value = 2
        verify(observer, never()).onChanged(anyString())
    }

    @Test
    fun testSwitchMapToNull() {
        val trigger: LiveData<Int> = MutableLiveData()
        val first: LiveData<String> = MutableLiveData()
        val result = trigger.switchMap { input: Int ->
            if (input == 1) {
                first
            } else {
                null
            }
        }
        val observer = mock(Observer::class.java) as Observer<String>
        result.observe(owner, observer)
        verify(observer, never()).onChanged(anyString())
        first.value = "first"
        trigger.value = 1
        verify(observer).onChanged("first")
        reset(observer)
        trigger.value = 2
        verify(observer, never()).onChanged(anyString())
        assertThat(first.hasObservers(), `is`(false))
    }

    @Test
    fun noObsoleteValueTest() {
        val numbers = MutableLiveData<Int>()
        val squared = numbers.map { input: Int -> input * input }
        val observer = mock(Observer::class.java) as Observer<Int>
        squared.value = 1
        squared.observeForever(observer)
        verify(observer).onChanged(1)
        squared.removeObserver(observer)
        reset(observer)
        numbers.value = 2
        squared.observeForever(observer)
        verify(observer, only()).onChanged(4)
    }

    @Test
    fun testDistinctUntilChanged_initialValueIsSet() {
        val originalLiveData = MutableLiveData("value")
        val newLiveData = originalLiveData.distinctUntilChanged()
        assertThat(newLiveData.value, `is`("value"))
        val observer = CountingObserver<String>()
        newLiveData.observe(owner, observer)
        assertThat(observer.timesUpdated, `is`(1))
        assertThat(newLiveData.value, `is`("value"))
    }

    @Test
    fun testDistinctUntilChanged_triggersOnInitialNullValue() {
        val originalLiveData = MutableLiveData<String?>()
        originalLiveData.value = null
        val newLiveData = originalLiveData.distinctUntilChanged()
        assertThat(newLiveData.value, `is`(nullValue()))
        val observer = CountingObserver<String?>()
        newLiveData.observe(owner, observer)
        assertThat(observer.timesUpdated, `is`(1))
        assertThat(newLiveData.value, `is`(nullValue()))
    }

    @Test
    fun testDistinctUntilChanged_copiesValues() {
        val originalLiveData = MutableLiveData<String>()
        val newLiveData = originalLiveData.distinctUntilChanged()
        assertThat(newLiveData.value, `is`(nullValue()))
        val observer = CountingObserver<String>()
        newLiveData.observe(owner, observer)
        assertThat(observer.timesUpdated, `is`(0))
        val value = "new value"
        originalLiveData.value = value
        assertThat(newLiveData.value, `is`(value))
        assertThat(observer.timesUpdated, `is`(1))
        originalLiveData.value = value
        assertThat(newLiveData.value, `is`(value))
        assertThat(observer.timesUpdated, `is`(1))
        val newerValue = "newer value"
        originalLiveData.value = newerValue
        assertThat(newLiveData.value, `is`(newerValue))
        assertThat(observer.timesUpdated, `is`(2))
        newLiveData.removeObservers(owner)
    }

    private class CountingObserver<T> : Observer<T> {
        var timesUpdated = 0
        override fun onChanged(value: T) {
            ++timesUpdated
        }
    }
}