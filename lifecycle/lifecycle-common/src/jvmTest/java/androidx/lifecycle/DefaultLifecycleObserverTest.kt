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

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.`when`

@RunWith(JUnit4::class)
class DefaultLifecycleObserverTest {
    private lateinit var owner: LifecycleOwner
    private lateinit var lifecycle: Lifecycle

    @Before
    fun initMocks() {
        owner = mock(LifecycleOwner::class.java)
        lifecycle = mock(Lifecycle::class.java)
        `when`(owner.lifecycle).thenReturn(lifecycle)
    }

    @Test
    fun eachEvent() {
        val obj = mock(DefaultLifecycleObserver::class.java)
        val observer = DefaultLifecycleObserverAdapter(obj, null)
        `when`(lifecycle.currentState).thenReturn(Lifecycle.State.CREATED)
        observer.onStateChanged(owner, Lifecycle.Event.ON_CREATE)
        val inOrder = inOrder(obj)
        inOrder.verify(obj).onCreate(owner)
        reset(obj)
        `when`(lifecycle.currentState).thenReturn(Lifecycle.State.STARTED)
        observer.onStateChanged(owner, Lifecycle.Event.ON_START)
        inOrder.verify(obj).onStart(owner)
        reset(obj)
        `when`(lifecycle.currentState).thenReturn(Lifecycle.State.RESUMED)
        observer.onStateChanged(owner, Lifecycle.Event.ON_RESUME)
        inOrder.verify(obj).onResume(owner)
        reset(obj)
        `when`(lifecycle.currentState).thenReturn(Lifecycle.State.STARTED)
        observer.onStateChanged(owner, Lifecycle.Event.ON_PAUSE)
        inOrder.verify(obj).onPause(owner)
        reset(obj)
        `when`(lifecycle.currentState).thenReturn(Lifecycle.State.CREATED)
        observer.onStateChanged(owner, Lifecycle.Event.ON_STOP)
        inOrder.verify(obj).onStop(owner)
        reset(obj)
        `when`(lifecycle.currentState).thenReturn(Lifecycle.State.INITIALIZED)
        observer.onStateChanged(owner, Lifecycle.Event.ON_DESTROY)
        inOrder.verify(obj).onDestroy(owner)
        reset(obj)
    }

    @Test
    fun defaultLifecycleObserverAndLifecycleEventObserver() {
        open class AllObservers : DefaultLifecycleObserver, LifecycleEventObserver {
            override fun onCreate(owner: LifecycleOwner) {}
            override fun onStart(owner: LifecycleOwner) {}
            override fun onResume(owner: LifecycleOwner) {}
            override fun onPause(owner: LifecycleOwner) {}
            override fun onStop(owner: LifecycleOwner) {}
            override fun onDestroy(owner: LifecycleOwner) {}
            override fun onStateChanged(
                source: LifecycleOwner,
                event: Lifecycle.Event
            ) {
            }
        }

        val obj = mock(AllObservers::class.java)
        val observer = DefaultLifecycleObserverAdapter(obj, obj)
        `when`(lifecycle.currentState).thenReturn(Lifecycle.State.CREATED)
        observer.onStateChanged(owner, Lifecycle.Event.ON_CREATE)
        val inOrder = inOrder(obj)
        inOrder.verify(obj).onCreate(owner)
        inOrder.verify(obj).onStateChanged(owner, Lifecycle.Event.ON_CREATE)
        reset(obj)
        `when`(lifecycle.currentState).thenReturn(Lifecycle.State.STARTED)
        observer.onStateChanged(owner, Lifecycle.Event.ON_START)
        inOrder.verify(obj).onStart(owner)
        inOrder.verify(obj).onStateChanged(owner, Lifecycle.Event.ON_START)
        reset(obj)
        `when`(lifecycle.currentState).thenReturn(Lifecycle.State.RESUMED)
        observer.onStateChanged(owner, Lifecycle.Event.ON_RESUME)
        inOrder.verify(obj).onResume(owner)
        inOrder.verify(obj).onStateChanged(owner, Lifecycle.Event.ON_RESUME)
        reset(obj)
        `when`(lifecycle.currentState).thenReturn(Lifecycle.State.STARTED)
        observer.onStateChanged(owner, Lifecycle.Event.ON_PAUSE)
        inOrder.verify(obj).onPause(owner)
        inOrder.verify(obj).onStateChanged(owner, Lifecycle.Event.ON_PAUSE)
        reset(obj)
        `when`(lifecycle.currentState).thenReturn(Lifecycle.State.CREATED)
        observer.onStateChanged(owner, Lifecycle.Event.ON_STOP)
        inOrder.verify(obj).onStop(owner)
        inOrder.verify(obj).onStateChanged(owner, Lifecycle.Event.ON_STOP)
        reset(obj)
        `when`(lifecycle.currentState).thenReturn(Lifecycle.State.INITIALIZED)
        observer.onStateChanged(owner, Lifecycle.Event.ON_DESTROY)
        inOrder.verify(obj).onDestroy(owner)
        inOrder.verify(obj).onStateChanged(owner, Lifecycle.Event.ON_DESTROY)
        reset(obj)
    }

    fun fullLifecycleObserverAndAnnotations() {
        @Suppress("deprecation")
        class AnnotatedFullLifecycleObserver : DefaultLifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
            fun onAny() {
                throw IllegalStateException(
                    "Annotations in FullLifecycleObserver must not be called"
                )
            }

            override fun onCreate(owner: LifecycleOwner) {}
            override fun onStart(owner: LifecycleOwner) {}
            override fun onResume(owner: LifecycleOwner) {}
            override fun onPause(owner: LifecycleOwner) {}
            override fun onStop(owner: LifecycleOwner) {}
            override fun onDestroy(owner: LifecycleOwner) {}
        }
    }
}
