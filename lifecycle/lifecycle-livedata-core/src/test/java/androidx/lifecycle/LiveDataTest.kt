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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.`when`
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.only
import org.mockito.kotlin.reset
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@Suppress("unchecked_cast")
@RunWith(JUnit4::class)
class LiveDataTest {
    @JvmField
    @Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var liveData: PublicLiveData<String>
    private lateinit var activeObserversChanged: MethodExec
    private lateinit var owner: TestLifecycleOwner
    private lateinit var owner2: TestLifecycleOwner
    private lateinit var owner3: LifecycleOwner
    private lateinit var lifecycle3: Lifecycle
    private lateinit var observer3: Observer<String>
    private lateinit var owner4: LifecycleOwner
    private lateinit var lifecycle4: Lifecycle
    private lateinit var observer4: Observer<String>
    private var inObserver = false

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun init() {
        liveData = PublicLiveData()
        activeObserversChanged = mock()
        liveData.activeObserversChanged = activeObserversChanged
        owner = TestLifecycleOwner(
            Lifecycle.State.INITIALIZED,
            UnconfinedTestDispatcher(null, null)
        )
        owner2 = TestLifecycleOwner(
            Lifecycle.State.INITIALIZED,
            UnconfinedTestDispatcher(null, null)
        )
        inObserver = false
    }

    @Before
    fun initNonLifecycleRegistry() {
        owner3 = mock()
        lifecycle3 = mock()
        observer3 = mock()
        `when`(owner3.lifecycle).thenReturn(lifecycle3)
        owner4 = mock()
        lifecycle4 = mock()
        observer4 = mock()
        `when`(owner4.lifecycle).thenReturn(lifecycle4)
    }

    @After
    fun removeExecutorDelegate() {
        getInstance().setDelegate(null)
    }

    @Test
    fun observe() {
        @OptIn(ExperimentalCoroutinesApi::class)
        val lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = UnconfinedTestDispatcher())

        val liveData = MutableLiveData<String>()
        var value = ""
        liveData.observe(lifecycleOwner) { newValue ->
            value = newValue
        }

        liveData.value = "261"
        assertThat(value, `is`("261"))
    }

    @Test
    fun testIsInitialized() {
        assertThat(liveData.isInitialized, `is`(false))
        assertThat(liveData.value, `is`(nullValue()))
        liveData.value = "a"
        assertThat(liveData.value, `is`("a"))
        assertThat(liveData.isInitialized, `is`(true))
    }

    @Test
    fun testIsInitializedNullValue() {
        assertThat(liveData.isInitialized, `is`(false))
        assertThat(liveData.value, `is`(nullValue()))
        liveData.value = null
        assertThat(liveData.isInitialized, `is`(true))
        assertThat(liveData.value, `is`(nullValue()))
    }

    @Test
    fun testObserverToggle() {
        val observer = mock() as Observer<String>
        liveData.observe(owner, observer)
        verify(activeObserversChanged, never()).onCall(anyBoolean())
        assertThat(liveData.hasObservers(), `is`(true))
        assertThat(liveData.hasActiveObservers(), `is`(false))
        liveData.removeObserver(observer)
        verify(activeObserversChanged, never()).onCall(anyBoolean())
        assertThat(liveData.hasObservers(), `is`(false))
        assertThat(liveData.hasActiveObservers(), `is`(false))
    }

    @Test
    fun testActiveObserverToggle() {
        val observer = mock() as Observer<String>
        liveData.observe(owner, observer)
        verify(activeObserversChanged, never()).onCall(anyBoolean())
        assertThat(liveData.hasObservers(), `is`(true))
        assertThat(liveData.hasActiveObservers(), `is`(false))
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        verify(activeObserversChanged).onCall(true)
        assertThat(liveData.hasActiveObservers(), `is`(true))
        reset(activeObserversChanged)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        verify(activeObserversChanged).onCall(false)
        assertThat(liveData.hasActiveObservers(), `is`(false))
        assertThat(liveData.hasObservers(), `is`(true))
        reset(activeObserversChanged)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        verify(activeObserversChanged).onCall(true)
        assertThat(liveData.hasActiveObservers(), `is`(true))
        assertThat(liveData.hasObservers(), `is`(true))
        reset(activeObserversChanged)
        liveData.removeObserver(observer)
        verify(activeObserversChanged).onCall(false)
        assertThat(liveData.hasActiveObservers(), `is`(false))
        assertThat(liveData.hasObservers(), `is`(false))
        verifyNoMoreInteractions(activeObserversChanged)
    }

    @Test
    fun testReAddSameObserverTuple() {
        val observer = mock() as Observer<String>
        liveData.observe(owner, observer)
        liveData.observe(owner, observer)
        assertThat(liveData.hasObservers(), `is`(true))
    }

    @Test
    fun testAdd2ObserversWithSameOwnerAndRemove() {
        val o1 = mock() as Observer<String>
        val o2 = mock() as Observer<String>
        liveData.observe(owner, o1)
        liveData.observe(owner, o2)
        assertThat(liveData.hasObservers(), `is`(true))
        verify(activeObserversChanged, never()).onCall(anyBoolean())
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        verify(activeObserversChanged).onCall(true)
        liveData.value = "a"
        verify(o1).onChanged("a")
        verify(o2).onChanged("a")
        liveData.removeObservers(owner)
        assertThat(liveData.hasObservers(), `is`(false))
        assertThat(owner.observerCount, `is`(0))
    }

    @Test
    fun testAddSameObserverIn2LifecycleOwners() {
        val observer = mock() as Observer<String>
        liveData.observe(owner, observer)
        lateinit var throwable: Throwable
        try {
            liveData.observe(owner2, observer)
        } catch (t: Throwable) {
            throwable = t
        }
        assertThat(
            throwable,
            instanceOf(IllegalArgumentException::class.java)
        )
        assertThat(
            throwable.message,
            `is`("Cannot add the same observer with different lifecycles")
        )
    }

    @Test
    fun testRemoveDestroyedObserver() {
        val observer = mock() as Observer<String>
        liveData.observe(owner, observer)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        verify(activeObserversChanged).onCall(true)
        assertThat(liveData.hasObservers(), `is`(true))
        assertThat(liveData.hasActiveObservers(), `is`(true))
        reset(activeObserversChanged)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(liveData.hasObservers(), `is`(false))
        assertThat(liveData.hasActiveObservers(), `is`(false))
        verify(activeObserversChanged).onCall(false)
    }

    @Test
    fun testInactiveRegistry() {
        val observer = mock() as Observer<String>
        owner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        liveData.observe(owner, observer)
        assertThat(liveData.hasObservers(), `is`(false))
    }

    @Test
    fun testNotifyActiveInactive() {
        val observer = mock() as Observer<String>
        owner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        liveData.observe(owner, observer)
        liveData.value = "a"
        verify(observer, never()).onChanged(anyString())
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        verify(observer).onChanged("a")
        liveData.value = "b"
        verify(observer).onChanged("b")
        owner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        liveData.value = "c"
        verify(observer, never()).onChanged("c")
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        verify(observer).onChanged("c")
        reset(observer)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        verify(observer, never()).onChanged(anyString())
    }

    @Test
    fun testStopObservingOwner_onDestroy() {
        val observer = mock() as Observer<String>
        owner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        liveData.observe(owner, observer)
        assertThat(owner.observerCount, `is`(1))
        owner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(owner.observerCount, `is`(0))
    }

    @Test
    fun testStopObservingOwner_onStopObserving() {
        val observer = mock() as Observer<String>
        owner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        liveData.observe(owner, observer)
        assertThat(owner.observerCount, `is`(1))
        liveData.removeObserver(observer)
        assertThat(owner.observerCount, `is`(0))
    }

    @Test
    fun testActiveChangeInCallback() {
        open class TestObserver : Observer<String> {
            override fun onChanged(value: String) {
                owner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                assertThat(liveData.hasObservers(), `is`(true))
                assertThat(liveData.hasActiveObservers(), `is`(false))
            }
        }

        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        val observer1 = spy(TestObserver())
        val observer2 = mock() as Observer<String>
        liveData.observe(owner, observer1)
        liveData.observe(owner, observer2)
        liveData.value = "bla"
        verify(observer1).onChanged(anyString())
        verify(observer2, never()).onChanged(anyString())
        assertThat(liveData.hasObservers(), `is`(true))
        assertThat(liveData.hasActiveObservers(), `is`(false))
    }

    @Test
    fun testActiveChangeInCallback2() {
        open class TestObserver : Observer<String> {
            override fun onChanged(value: String) {
                assertThat(inObserver, `is`(false))
                inObserver = true
                owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
                assertThat(liveData.hasActiveObservers(), `is`(true))
                inObserver = false
            }
        }

        val observer1 = spy(TestObserver())
        val observer2 =
            spy<FailReentrantObserver<*>>(FailReentrantObserver<Any?>()) as Observer<in String>
        liveData.observeForever(observer1)
        liveData.observe(owner, observer2)
        liveData.value = "bla"
        verify(observer1).onChanged(anyString())
        verify(observer2).onChanged(anyString())
        assertThat(liveData.hasObservers(), `is`(true))
        assertThat(liveData.hasActiveObservers(), `is`(true))
    }

    @Test
    fun testObserverRemovalInCallback() {
        open class TestObserver : Observer<String> {
            override fun onChanged(value: String) {
                assertThat(liveData.hasObservers(), `is`(true))
                liveData.removeObserver(this)
                assertThat(liveData.hasObservers(), `is`(false))
            }
        }

        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        val observer = spy(TestObserver())
        liveData.observe(owner, observer)
        liveData.value = "bla"
        verify(observer).onChanged(anyString())
        assertThat(liveData.hasObservers(), `is`(false))
    }

    @Test
    fun testObserverAdditionInCallback() {
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        val observer2 =
            spy<FailReentrantObserver<*>>(FailReentrantObserver<Any?>()) as Observer<in String>

        open class TestObserver : Observer<String> {
            override fun onChanged(value: String) {
                assertThat(inObserver, `is`(false))
                inObserver = true
                liveData.observe(owner, observer2)
                assertThat(liveData.hasObservers(), `is`(true))
                assertThat(liveData.hasActiveObservers(), `is`(true))
                inObserver = false
            }
        }

        val observer1 = spy(TestObserver())
        liveData.observe(owner, observer1)
        liveData.value = "bla"
        verify(observer1).onChanged(anyString())
        verify(observer2).onChanged(anyString())
        assertThat(liveData.hasObservers(), `is`(true))
        assertThat(liveData.hasActiveObservers(), `is`(true))
    }

    @Test
    fun testObserverWithoutLifecycleOwner() {
        val observer = mock() as Observer<String>
        liveData.value = "boring"
        liveData.observeForever(observer)
        verify(activeObserversChanged).onCall(true)
        verify(observer).onChanged("boring")
        liveData.value = "this"
        verify(observer).onChanged("this")
        liveData.removeObserver(observer)
        verify(activeObserversChanged).onCall(false)
        liveData.value = "boring"
        reset(observer)
        verify(observer, never()).onChanged(anyString())
    }

    @Test
    fun testSetValueDuringSetValue() {
        open class TestObserver : Observer<String> {
            override fun onChanged(value: String) {
                assertThat(inObserver, `is`(false))
                inObserver = true
                if (value == "bla") {
                    liveData.value = "gt"
                }
                inObserver = false
            }
        }

        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        val observer1 = spy(TestObserver())
        val observer2 =
            spy<FailReentrantObserver<*>>(FailReentrantObserver<Any?>()) as Observer<in String>
        liveData.observe(owner, observer1)
        liveData.observe(owner, observer2)
        liveData.value = "bla"
        verify(observer1, times(1)).onChanged("gt")
        verify(observer2, times(1)).onChanged("gt")
    }

    @Test
    fun testRemoveDuringSetValue() {
        open class TestObserver : Observer<String> {
            override fun onChanged(value: String) {
                liveData.removeObserver(this)
            }
        }

        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        val observer1 = spy(TestObserver())
        val observer2 = mock() as Observer<String>
        liveData.observeForever(observer1)
        liveData.observe(owner, observer2)
        liveData.value = "gt"
        verify(observer2).onChanged("gt")
    }

    @Test
    fun testDataChangeDuringStateChange() {
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                // change data in onStop, observer should not be called!
                liveData.value = "b"
            }
        })
        val observer = mock() as Observer<String>
        liveData.value = "a"
        liveData.observe(owner, observer)
        verify(observer).onChanged("a")
        owner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        verify(observer, never()).onChanged("b")
        owner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        verify(observer).onChanged("b")
    }

    @Test
    fun testNotCallInactiveWithObserveForever() {
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        val observer = mock() as Observer<String>
        val observer2 = mock() as Observer<String>
        liveData.observe(owner, observer)
        liveData.observeForever(observer2)
        verify(activeObserversChanged).onCall(true)
        reset(activeObserversChanged)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        verify(activeObserversChanged, never()).onCall(anyBoolean())
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        verify(activeObserversChanged, never()).onCall(anyBoolean())
    }

    @Test
    fun testRemoveDuringAddition() {
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        liveData.value = "bla"
        liveData.observeForever(object : Observer<String> {
            override fun onChanged(value: String) {
                liveData.removeObserver(this)
            }
        })
        assertThat(liveData.hasActiveObservers(), `is`(false))
        val inOrder = inOrder(activeObserversChanged)
        inOrder.verify(activeObserversChanged).onCall(true)
        inOrder.verify(activeObserversChanged).onCall(false)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun testRemoveDuringBringingUpToState() {
        liveData.value = "bla"
        liveData.observeForever(object : Observer<String> {
            override fun onChanged(value: String) {
                liveData.removeObserver(this)
            }
        })
        owner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        assertThat(liveData.hasActiveObservers(), `is`(false))
        val inOrder = inOrder(activeObserversChanged)
        inOrder.verify(activeObserversChanged).onCall(true)
        inOrder.verify(activeObserversChanged).onCall(false)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun setValue_neverActive_observerOnChangedNotCalled() {
        val observer = mock() as Observer<String>
        liveData.observe(owner, observer)
        liveData.value = "1"
        verify(observer, never()).onChanged(anyString())
    }

    @Test
    fun setValue_twoObserversTwoStartedOwners_onChangedCalledOnBoth() {
        val observer1 = mock() as Observer<in String>
        val observer2 = mock() as Observer<in String>
        liveData.observe(owner, observer1)
        liveData.observe(owner2, observer2)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        owner2.handleLifecycleEvent(Lifecycle.Event.ON_START)
        liveData.value = "1"
        verify(observer1).onChanged("1")
        verify(observer2).onChanged("1")
    }

    @Test
    fun setValue_twoObserversOneStartedOwner_onChangedCalledOnOneCorrectObserver() {
        val observer1 = mock() as Observer<in String>
        val observer2 = mock() as Observer<in String>
        liveData.observe(owner, observer1)
        liveData.observe(owner2, observer2)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        liveData.value = "1"
        verify(observer1).onChanged("1")
        verify(observer2, never()).onChanged(anyString())
    }

    @Test
    fun setValue_twoObserversBothStartedAfterSetValue_onChangedCalledOnBoth() {
        val observer1 = mock() as Observer<in String>
        val observer2 = mock() as Observer<in String>
        liveData.observe(owner, observer1)
        liveData.observe(owner2, observer2)
        liveData.value = "1"
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        owner2.handleLifecycleEvent(Lifecycle.Event.ON_START)
        verify(observer1).onChanged("1")
        verify(observer2).onChanged("1")
    }

    @Test
    fun setValue_twoObserversOneStartedAfterSetValue_onChangedCalledOnCorrectObserver() {
        val observer1 = mock() as Observer<in String>
        val observer2 = mock() as Observer<in String>
        liveData.observe(owner, observer1)
        liveData.observe(owner2, observer2)
        liveData.value = "1"
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        verify(observer1).onChanged("1")
        verify(observer2, never()).onChanged(anyString())
    }

    @Test
    fun setValue_twoObserversOneStarted_liveDataBecomesActive() {
        val observer1 = mock() as Observer<in String>
        val observer2 = mock() as Observer<in String>
        liveData.observe(owner, observer1)
        liveData.observe(owner2, observer2)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        verify(activeObserversChanged).onCall(true)
    }

    @Test
    fun setValue_twoObserversOneStopped_liveDataStaysActive() {
        val observer1 = mock() as Observer<in String>
        val observer2 = mock() as Observer<in String>
        liveData.observe(owner, observer1)
        liveData.observe(owner2, observer2)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        owner2.handleLifecycleEvent(Lifecycle.Event.ON_START)
        verify(activeObserversChanged).onCall(true)
        reset(activeObserversChanged)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        verify(activeObserversChanged, never()).onCall(anyBoolean())
    }

    @Test
    fun setValue_lifecycleIsCreatedNoEvent_liveDataBecomesInactiveAndObserverNotCalled() {
        // Arrange.
        liveData.observe(owner3, observer3)
        val lifecycleObserver = getLiveDataInternalObserver(lifecycle3)
        `when`(lifecycle3.currentState).thenReturn(Lifecycle.State.STARTED)
        lifecycleObserver.onStateChanged(owner3, Lifecycle.Event.ON_START)
        `when`(lifecycle3.currentState).thenReturn(Lifecycle.State.CREATED)
        reset(activeObserversChanged)
        reset(observer3)

        // Act.
        liveData.value = "1"

        // Assert.
        verify(activeObserversChanged).onCall(false)
        verify(observer3, never()).onChanged(anyString())
    }

    /*
     * Arrange: LiveData was made inactive via SetValue (because the Lifecycle it was
     * observing was in the CREATED state and no event was dispatched).
     * Act: Lifecycle enters Started state and dispatches event.
     * Assert: LiveData becomes active and dispatches new value to observer.
     */
    @Test
    fun test_liveDataInactiveViaSetValueThenLifecycleResumes() {
        // Arrange.
        liveData.observe(owner3, observer3)
        val lifecycleObserver = getLiveDataInternalObserver(lifecycle3)
        `when`(lifecycle3.currentState).thenReturn(Lifecycle.State.STARTED)
        lifecycleObserver.onStateChanged(owner3, Lifecycle.Event.ON_START)
        `when`(lifecycle3.currentState).thenReturn(Lifecycle.State.CREATED)
        liveData.value = "1"
        reset(activeObserversChanged)
        reset(observer3)

        // Act.
        `when`(lifecycle3.currentState).thenReturn(Lifecycle.State.STARTED)
        lifecycleObserver.onStateChanged(owner3, Lifecycle.Event.ON_START)

        // Assert.
        verify(activeObserversChanged).onCall(true)
        verify(observer3).onChanged("1")
    }

    /*
     * Arrange: One of two Lifecycles enter the CREATED state without sending an event.
     * Act: Lifecycle's setValue method is called with new value.
     * Assert: LiveData stays active and new value is dispatched to Lifecycle that is still at least
     * STARTED.
     */
    @Test
    fun setValue_oneOfTwoLifecyclesAreCreatedNoEvent() {
        // Arrange.
        liveData.observe(owner3, observer3)
        liveData.observe(owner4, observer4)
        val lifecycleObserver3 = getLiveDataInternalObserver(lifecycle3)
        val lifecycleObserver4 = getLiveDataInternalObserver(lifecycle4)
        `when`(lifecycle3.currentState).thenReturn(Lifecycle.State.STARTED)
        `when`(lifecycle4.currentState).thenReturn(Lifecycle.State.STARTED)
        lifecycleObserver3.onStateChanged(owner3, Lifecycle.Event.ON_START)
        lifecycleObserver4.onStateChanged(owner4, Lifecycle.Event.ON_START)
        `when`(lifecycle3.currentState).thenReturn(Lifecycle.State.CREATED)
        reset(activeObserversChanged)
        reset(observer3)
        reset(observer4)

        // Act.
        liveData.value = "1"

        // Assert.
        verify(activeObserversChanged, never()).onCall(anyBoolean())
        verify(observer3, never()).onChanged(anyString())
        verify(observer4).onChanged("1")
    }

    /*
     * Arrange: Two observed Lifecycles enter the CREATED state without sending an event.
     * Act: Lifecycle's setValue method is called with new value.
     * Assert: LiveData becomes inactive and nothing is dispatched to either observer.
     */
    @Test
    fun setValue_twoLifecyclesAreCreatedNoEvent() {
        // Arrange.
        liveData.observe(owner3, observer3)
        liveData.observe(owner4, observer4)
        val lifecycleObserver3 = getLiveDataInternalObserver(lifecycle3)
        val lifecycleObserver4 = getLiveDataInternalObserver(lifecycle4)
        `when`(lifecycle3.currentState).thenReturn(Lifecycle.State.STARTED)
        `when`(lifecycle4.currentState).thenReturn(Lifecycle.State.STARTED)
        lifecycleObserver3.onStateChanged(owner3, Lifecycle.Event.ON_START)
        lifecycleObserver4.onStateChanged(owner4, Lifecycle.Event.ON_START)
        `when`(lifecycle3.currentState).thenReturn(Lifecycle.State.CREATED)
        `when`(lifecycle4.currentState).thenReturn(Lifecycle.State.CREATED)
        reset(activeObserversChanged)
        reset(observer3)
        reset(observer4)

        // Act.
        liveData.value = "1"

        // Assert.
        verify(activeObserversChanged).onCall(false)
        verify(observer3, never()).onChanged(anyString())
        verify(observer3, never()).onChanged(anyString())
    }

    /*
     * Arrange: LiveData was made inactive via SetValue (because both Lifecycles it was
     * observing were in the CREATED state and no event was dispatched).
     * Act: One Lifecycle enters STARTED state and dispatches lifecycle event.
     * Assert: LiveData becomes active and dispatches new value to observer associated with started
     * Lifecycle.
     */
    @Test
    fun test_liveDataInactiveViaSetValueThenOneLifecycleResumes() {
        // Arrange.
        liveData.observe(owner3, observer3)
        liveData.observe(owner4, observer4)
        val lifecycleObserver3 = getLiveDataInternalObserver(lifecycle3)
        val lifecycleObserver4 = getLiveDataInternalObserver(lifecycle4)
        `when`(lifecycle3.currentState).thenReturn(Lifecycle.State.STARTED)
        `when`(lifecycle4.currentState).thenReturn(Lifecycle.State.STARTED)
        lifecycleObserver3.onStateChanged(owner3, Lifecycle.Event.ON_START)
        lifecycleObserver4.onStateChanged(owner4, Lifecycle.Event.ON_START)
        `when`(lifecycle3.currentState).thenReturn(Lifecycle.State.CREATED)
        `when`(lifecycle4.currentState).thenReturn(Lifecycle.State.CREATED)
        liveData.value = "1"
        reset(activeObserversChanged)
        reset(observer3)
        reset(observer4)

        // Act.
        `when`(lifecycle3.currentState).thenReturn(Lifecycle.State.STARTED)
        lifecycleObserver3.onStateChanged(owner3, Lifecycle.Event.ON_START)

        // Assert.
        verify(activeObserversChanged).onCall(true)
        verify(observer3).onChanged("1")
        verify(observer4, never()).onChanged(anyString())
    }

    @Test
    fun nestedForeverObserver() {
        liveData.value = "."
        liveData.observeForever(object : Observer<String> {
            override fun onChanged(value: String) {
                liveData.observeForever(
                    mock() as Observer<String>
                )
                liveData.removeObserver(this)
            }
        })
        verify(activeObserversChanged, only()).onCall(true)
    }

    @Test
    fun readForeverObserver() {
        val observer = mock() as Observer<String>
        liveData.observeForever(observer)
        liveData.observeForever(observer)
        liveData.removeObserver(observer)
        assertThat(liveData.hasObservers(), `is`(false))
    }

    @Test
    fun initialValue() {
        val mutableLiveData = MutableLiveData("foo")
        val observer = mock() as Observer<String>
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        mutableLiveData.observe(owner, observer)
        verify(observer).onChanged("foo")
    }

    @Test
    fun activeReentry_removeOnActive() {
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        val observer: Observer<String> = Observer { }
        val activeCalls: MutableList<Boolean> = ArrayList()
        val liveData = object : MutableLiveData<String>("foo") {
            override fun onActive() {
                activeCalls.add(true)
                super.onActive()
                removeObserver(observer)
            }

            override fun onInactive() {
                activeCalls.add(false)
                super.onInactive()
            }
        }
        liveData.observe(owner, observer)
        assertThat<List<Boolean>>(
            activeCalls,
            equalTo(mutableListOf(true, false))
        )
    }

    @Test
    fun activeReentry_addOnInactive() {
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        val observer1 = mock() as Observer<String>
        val observer2 = mock() as Observer<String>
        val activeCalls: MutableList<Boolean> = ArrayList()
        val liveData = object : MutableLiveData<String>("foo") {
            override fun onActive() {
                activeCalls.add(true)
                super.onActive()
            }

            override fun onInactive() {
                activeCalls.add(false)
                observe(owner, observer2)
                super.onInactive()
            }
        }
        liveData.observe(owner, observer1)
        liveData.removeObserver(observer1)
        liveData.removeObserver(observer2)
        assertThat<List<Boolean>>(
            activeCalls,
            equalTo(mutableListOf(true, false, true, false, true))
        )
    }

    @Test
    fun activeReentry_lifecycleChangesActive() {
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        val observer: Observer<String> =
            Observer { owner.handleLifecycleEvent(Lifecycle.Event.ON_STOP) }
        val activeCalls: MutableList<Boolean> = ArrayList()
        val liveData = object : MutableLiveData<String>("foo") {
            override fun onActive() {
                activeCalls.add(true)
                super.onActive()
            }

            override fun onInactive() {
                activeCalls.add(false)
                super.onInactive()
            }
        }
        liveData.observe(owner, observer)
        assertThat(owner.currentState, `is`(Lifecycle.State.CREATED))
        assertThat<List<Boolean>>(
            activeCalls,
            `is`(mutableListOf(true, false))
        )
    }

    private fun getLiveDataInternalObserver(lifecycle: Lifecycle?): LifecycleEventObserver {
        val captor: KArgumentCaptor<LifecycleEventObserver> = argumentCaptor()
        verify(lifecycle)?.addObserver(captor.capture())
        return captor.firstValue
    }

    internal class PublicLiveData<T> : LiveData<T>() {
        // cannot spy due to internal calls
        var activeObserversChanged: MethodExec? = null
        override fun onActive() {
            if (activeObserversChanged != null) {
                activeObserversChanged!!.onCall(true)
            }
        }

        override fun onInactive() {
            if (activeObserversChanged != null) {
                activeObserversChanged!!.onCall(false)
            }
        }
    }

    private open inner class FailReentrantObserver<T> : Observer<T> {
        override fun onChanged(value: T) {
            assertThat(inObserver, `is`(false))
        }
    }

    internal interface MethodExec {
        fun onCall(value: Boolean)
    }
}
