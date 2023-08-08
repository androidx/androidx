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

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@Suppress("deprecation", "unused_parameter")
@RunWith(JUnit4::class)
class ReflectiveGenericLifecycleObserverTest {
    private lateinit var owner: LifecycleOwner
    private lateinit var lifecycle: Lifecycle

    @Before
    fun initMocks() {
        owner = mock(LifecycleOwner::class.java)
        lifecycle = mock(Lifecycle::class.java)
        `when`(owner.lifecycle).thenReturn(lifecycle)
    }

    @Test
    fun anyState() {
        val obj = mock(AnyStateListener::class.java)
        val observer = ReflectiveGenericLifecycleObserver(obj)
        `when`(lifecycle.currentState).thenReturn(Lifecycle.State.STARTED)
        observer.onStateChanged(owner, Lifecycle.Event.ON_CREATE)
        verify(obj).onAnyState(owner, Lifecycle.Event.ON_CREATE)
        reset(obj)
        observer.onStateChanged(owner, Lifecycle.Event.ON_START)
        verify(obj).onAnyState(owner, Lifecycle.Event.ON_START)
        reset(obj)
        observer.onStateChanged(owner, Lifecycle.Event.ON_RESUME)
        verify(obj).onAnyState(owner, Lifecycle.Event.ON_RESUME)
        reset(obj)
        observer.onStateChanged(owner, Lifecycle.Event.ON_PAUSE)
        verify(obj).onAnyState(owner, Lifecycle.Event.ON_PAUSE)
        reset(obj)
        observer.onStateChanged(owner, Lifecycle.Event.ON_STOP)
        verify(obj).onAnyState(owner, Lifecycle.Event.ON_STOP)
        reset(obj)
        observer.onStateChanged(owner, Lifecycle.Event.ON_DESTROY)
        verify(obj).onAnyState(owner, Lifecycle.Event.ON_DESTROY)
        reset(obj)
    }

    private open class AnyStateListener : LifecycleObserver {
        @Suppress("deprecation")
        @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
        open fun onAnyState(owner: LifecycleOwner?, event: Lifecycle.Event?) {}
    }

    @Test
    fun singleMethod() {
        val obj = mock(CreatedStateListener::class.java)
        val observer = ReflectiveGenericLifecycleObserver(obj)
        `when`(lifecycle.currentState).thenReturn(Lifecycle.State.CREATED)
        observer.onStateChanged(owner, Lifecycle.Event.ON_CREATE)
        verify(obj).onCreated()
        verify(obj).onCreated(owner)
    }

    @Suppress("deprecation")
    private open class CreatedStateListener : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        open fun onCreated() {}

        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        open fun onCreated(provider: LifecycleOwner?) {}
    }

    @Suppress("deprecation")
    @Test
    fun eachEvent() {
        val obj = mock(AllMethodsListener::class.java)
        val observer = ReflectiveGenericLifecycleObserver(obj)
        `when`(lifecycle.currentState).thenReturn(Lifecycle.State.CREATED)
        observer.onStateChanged(owner, Lifecycle.Event.ON_CREATE)
        verify(obj).created()
        reset(obj)
        `when`(lifecycle.currentState).thenReturn(Lifecycle.State.STARTED)
        observer.onStateChanged(owner, Lifecycle.Event.ON_START)
        verify(obj).started()
        reset(obj)
        `when`(lifecycle.currentState).thenReturn(Lifecycle.State.RESUMED)
        observer.onStateChanged(owner, Lifecycle.Event.ON_RESUME)
        verify(obj).resumed()
        reset(obj)
        `when`(lifecycle.currentState).thenReturn(Lifecycle.State.STARTED)
        observer.onStateChanged(owner, Lifecycle.Event.ON_PAUSE)
        verify(obj).paused()
        reset(obj)
        `when`(lifecycle.currentState).thenReturn(Lifecycle.State.CREATED)
        observer.onStateChanged(owner, Lifecycle.Event.ON_STOP)
        verify(obj).stopped()
        reset(obj)
        `when`(lifecycle.currentState).thenReturn(Lifecycle.State.INITIALIZED)
        observer.onStateChanged(owner, Lifecycle.Event.ON_DESTROY)
        verify(obj).destroyed()
        reset(obj)
    }

    @Suppress("deprecation")
    private open class AllMethodsListener : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        open fun created() {}
        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        open fun started() {}
        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        open fun resumed() {}
        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        open fun paused() {}
        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        open fun stopped() {}
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        open fun destroyed() {}
    }

    @Suppress("deprecation")
    @Test
    fun testFailingObserver() {
        class UnprecedentedError : Error()

        val obj: LifecycleObserver = object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            fun started() {
                throw UnprecedentedError()
            }
        }
        val observer = ReflectiveGenericLifecycleObserver(obj)
        try {
            observer.onStateChanged(owner, Lifecycle.Event.ON_START)
            fail()
        } catch (e: Exception) {
            assertThat(
                "exception cause is wrong",
                e.cause is UnprecedentedError
            )
        }
    }

    @Test
    fun testPrivateObserverMethods() {
        open class ObserverWithPrivateMethod : LifecycleObserver {
            var called = false
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            private fun started() {
                called = true
            }
        }

        val obj = mock(ObserverWithPrivateMethod::class.java)
        val observer = ReflectiveGenericLifecycleObserver(obj)
        observer.onStateChanged(owner, Lifecycle.Event.ON_START)
        assertThat(obj.called, `is`(true))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testWrongFirstParam1() {
        val observer: LifecycleObserver = object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            private fun started(e: Lifecycle.Event) {}
        }
        ReflectiveGenericLifecycleObserver(observer)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testWrongFirstParam2() {
        val observer: LifecycleObserver = object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
            private fun started(l: Lifecycle, e: Lifecycle.Event) {}
        }
        ReflectiveGenericLifecycleObserver(observer)
    }

    @Test
    fun testLifecycleOwnerSubclassFirstParam() {
        val observer: LifecycleObserver = object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
            private fun started() {}
        }
        ReflectiveGenericLifecycleObserver(observer)
    }

    @Test
    fun testLifecycleEventSecondParam() {
        val observer: LifecycleObserver = object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
            private fun started(owner: LifecycleOwner, l: Lifecycle) {}
        }
        val expectedException = assertThrows(
            IllegalArgumentException::class.java
        ) { ReflectiveGenericLifecycleObserver(observer) }
        assertEquals(
            "invalid parameter type. second arg must be an event",
            expectedException.message
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testThreeParams() {
        val observer: LifecycleObserver = object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
            private fun started(owner: LifecycleOwner, e: Lifecycle.Event, i: Int) {}
        }
        ReflectiveGenericLifecycleObserver(observer)
    }

    @Test
    fun testOwnerMethodWithSecondParam_eventMustBeOnAny() {
        val observer: LifecycleObserver = object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            private fun started(owner: LifecycleOwner, e: Lifecycle.Event) {}
        }
        val expectedException = assertThrows(
            IllegalArgumentException::class.java
        ) { ReflectiveGenericLifecycleObserver(observer) }
        assertEquals(
            "Second arg is supported only for ON_ANY value",
            expectedException.message
        )
    }

    internal open class BaseClass1 : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        open fun foo(owner: LifecycleOwner?) {}
    }

    internal class DerivedClass1 : BaseClass1() {
        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        override fun foo(owner: LifecycleOwner?) {}
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidSuper1() {
        ReflectiveGenericLifecycleObserver(DerivedClass1())
    }

    internal class BaseClass2 : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun foo() {}
    }

    internal open class DerivedClass2 : BaseClass1() {
        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        open fun foo() {}
    }

    @Test
    fun testValidSuper1() {
        val obj = mock(DerivedClass2::class.java)
        val observer = ReflectiveGenericLifecycleObserver(obj)
        observer.onStateChanged(mock(LifecycleOwner::class.java), Lifecycle.Event.ON_START)
        verify(obj).foo(any())
        verify(obj, never()).foo()
        reset(obj)
        observer.onStateChanged(mock(LifecycleOwner::class.java), Lifecycle.Event.ON_STOP)
        verify(obj).foo()
        verify(obj, never()).foo(any())
    }

    internal open class BaseClass3 : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        open fun foo(owner: LifecycleOwner?) {}
    }

    internal interface Interface3 : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun foo(owner: LifecycleOwner?)
    }

    internal class DerivedClass3 : BaseClass3(), Interface3 {
        override fun foo(owner: LifecycleOwner?) {}
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidSuper2() {
        ReflectiveGenericLifecycleObserver(DerivedClass3())
    }

    internal open class BaseClass4 : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        open fun foo(owner: LifecycleOwner?) {}
    }

    internal interface Interface4 : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun foo(owner: LifecycleOwner?)
    }

    internal open class DerivedClass4 : BaseClass4(), Interface4 {
        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        override fun foo(owner: LifecycleOwner?) {}

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun foo() {}
    }

    @Test
    fun testValidSuper2() {
        val obj = mock(DerivedClass4::class.java)
        val observer = ReflectiveGenericLifecycleObserver(obj)
        observer.onStateChanged(mock(LifecycleOwner::class.java), Lifecycle.Event.ON_START)
        verify(obj).foo(any())
        verify(obj).foo()
    }

    internal interface InterfaceStart : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun foo(owner: LifecycleOwner?)
    }

    internal interface InterfaceStop : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun foo(owner: LifecycleOwner?)
    }

    internal class DerivedClass5 : InterfaceStart, InterfaceStop {
        override fun foo(owner: LifecycleOwner?) {}
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidSuper3() {
        ReflectiveGenericLifecycleObserver(DerivedClass5())
    }
}
