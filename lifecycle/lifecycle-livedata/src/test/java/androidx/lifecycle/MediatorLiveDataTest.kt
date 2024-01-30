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
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.lifecycle.util.InstantTaskExecutor
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify

@RunWith(JUnit4::class)
class MediatorLiveDataTest {
    @JvmField
    @Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @JvmField
    @Rule
    @Suppress("deprecation")
    var exception: ExpectedException = ExpectedException.none()

    private lateinit var owner: TestLifecycleOwner
    private lateinit var mediator: MediatorLiveData<String>
    private lateinit var source: LiveData<String?>
    private var sourceActive = false

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        owner = TestLifecycleOwner(
            Lifecycle.State.STARTED,
            UnconfinedTestDispatcher(null, null)
        )
        mediator = MediatorLiveData()
        source = object : LiveData<String?>() {
            override fun onActive() {
                sourceActive = true
            }

            override fun onInactive() {
                sourceActive = false
            }
        }
        sourceActive = false
        @Suppress("unchecked_cast")
        mediator.observe(owner, mock(Observer::class.java) as Observer<in String>)
    }

    @Before
    fun swapExecutorDelegate() {
        getInstance().setDelegate(InstantTaskExecutor())
    }

    @Test
    fun testHasInitialValue() {
        val mediator = MediatorLiveData("value")
        assertThat(mediator.value).isEqualTo("value")
    }

    @Test
    fun testSingleDelivery() {
        @Suppress("unchecked_cast")
        val observer = mock(Observer::class.java) as Observer<in String?>
        mediator.addSource(source, observer)
        source.setValue("flatfoot")
        verify(observer).onChanged("flatfoot")
        owner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        reset(observer)
        verify(observer, never()).onChanged(any())
    }

    @Suppress("unchecked_cast")
    @Test
    fun testChangeWhileInactive() {
        val observer = mock(Observer::class.java) as Observer<in String?>
        mediator.addSource(source, observer)
        mediator.observe(owner, mock(Observer::class.java) as Observer<in String>)
        source.value = "one"
        verify(observer).onChanged("one")
        owner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        reset(observer)
        source.value = "flatfoot"
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        verify(observer).onChanged("flatfoot")
    }

    @Test
    fun testAddSourceToActive() {
        source.value = "flatfoot"
        @Suppress("unchecked_cast")
        val observer = mock(Observer::class.java) as Observer<in String?>
        mediator.addSource(source, observer)
        verify(observer).onChanged("flatfoot")
    }

    @Test
    fun testAddSourceToInActive() {
        source.value = "flatfoot"
        owner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        @Suppress("unchecked_cast")
        val observer = mock(Observer::class.java) as Observer<in String?>
        mediator.addSource(source, observer)
        verify(observer, never()).onChanged(any())
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        verify(observer).onChanged("flatfoot")
    }

    @Test
    fun testRemoveSource() {
        source.value = "flatfoot"
        @Suppress("unchecked_cast")
        val observer = mock(Observer::class.java) as Observer<in String?>
        mediator.addSource(source, observer)
        verify(observer).onChanged("flatfoot")
        mediator.removeSource(source)
        reset(observer)
        source.value = "failure"
        verify(observer, never()).onChanged(any())
    }

    @Test
    fun testSourceInactive() {
        @Suppress("unchecked_cast")
        val observer = mock(Observer::class.java) as Observer<in String?>
        mediator.addSource(source, observer)
        assertThat(sourceActive, `is`(true))
        owner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertThat(sourceActive, `is`(false))
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertThat(sourceActive, `is`(true))
    }

    @Test
    fun testNoLeakObserver() {
        // Imitates a destruction of a ViewModel: a listener of LiveData is destroyed,
        // a reference to MediatorLiveData is cleaned up. In this case we shouldn't leak
        // MediatorLiveData as an observer of mSource.
        assertThat(source.hasObservers(), `is`(false))
        @Suppress("unchecked_cast")
        val observer = mock(Observer::class.java) as Observer<in String?>
        mediator.addSource(source, observer)
        assertThat(source.hasObservers(), `is`(true))
        owner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        mediator.removeObserver(observer)
        assertThat(source.hasObservers(), `is`(false))
    }

    @Suppress("unchecked_cast")
    @Test
    fun testMultipleSources() {
        val observer1 = mock(Observer::class.java) as Observer<in String?>
        mediator.addSource(source, observer1)
        val source2 = MutableLiveData<Int>()
        val observer2 = mock(Observer::class.java) as Observer<in Int?>
        mediator.addSource(source2, observer2)
        source.value = "flatfoot"
        verify(observer1).onChanged("flatfoot")
        verify(observer2, never()).onChanged(any())
        reset(observer1, observer2)
        source2.value = 1703
        verify(observer1, never()).onChanged(any())
        verify(observer2).onChanged(1703)
        reset(observer1, observer2)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        source.value = "failure"
        source2.value = 0
        verify(observer1, never()).onChanged(any())
        verify(observer2, never()).onChanged(any())
    }

    @Test
    fun removeSourceDuringOnActive() {
        // to trigger ConcurrentModificationException,
        // we have to call remove from a collection during "for" loop.
        // ConcurrentModificationException is thrown from next() method of an iterator
        // so this modification shouldn't be at the last iteration,
        // because if it is a last iteration, then next() wouldn't be called.
        // And the last: an order of an iteration over sources is not defined,
        // so I have to call it remove operation  from all observers.
        owner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        val removingObserver: Observer<String?> =
            Observer { mediator.removeSource(source) }
        mediator.addSource(source, removingObserver)
        val source2 = MutableLiveData<String>()
        source2.setValue("nana")
        mediator.addSource(source2, removingObserver)
        source.setValue("pet-jack")
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    @Suppress("unchecked_cast")
    @Test(expected = IllegalArgumentException::class)
    fun reAddSameSourceWithDifferentObserver() {
        mediator.addSource(
            source, mock(Observer::class.java) as Observer<in String?>
        )
        mediator.addSource(
            source, mock(Observer::class.java) as Observer<in String?>
        )
    }

    @Test
    fun addSameSourceWithSameObserver() {
        @Suppress("unchecked_cast")
        val observer = mock(Observer::class.java) as Observer<in String?>
        mediator.addSource(source, observer)
        mediator.addSource(source, observer)
        // no exception was thrown
    }

    @Test
    fun addSourceDuringOnActive() {
        owner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        source.value = "a"
        mediator.addSource(source, Observer {
            val source = MutableLiveData<String>()
            source.value = "b"
            mediator.addSource(source) {
                mediator.value = "c"
            }
        })
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertThat(mediator.value, `is`("c"))
    }
}
