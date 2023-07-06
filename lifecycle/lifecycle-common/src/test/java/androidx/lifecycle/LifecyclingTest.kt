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

import androidx.lifecycle.Lifecycling.lifecycleEventObserver
import androidx.lifecycle.observers.DerivedSequence1
import androidx.lifecycle.observers.DerivedSequence2
import androidx.lifecycle.observers.DerivedWithNewMethods
import androidx.lifecycle.observers.DerivedWithNoNewMethods
import androidx.lifecycle.observers.DerivedWithOverriddenMethodsWithLfAnnotation
import androidx.lifecycle.observers.InterfaceImpl1
import androidx.lifecycle.observers.InterfaceImpl2
import androidx.lifecycle.observers.InterfaceImpl3
import androidx.lifecycle.observers.NoOpLifecycle
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("deprecation")
@RunWith(JUnit4::class)
class LifecyclingTest {
    @Test
    fun testDerivedWithNewLfMethodsNoGeneratedAdapter() {
        val callback = lifecycleEventObserver(DerivedWithNewMethods())
        assertThat(
            callback, instanceOf(
                ReflectiveGenericLifecycleObserver::class.java
            )
        )
    }

    @Test
    fun testDerivedWithNoNewLfMethodsNoGeneratedAdapter() {
        val callback = lifecycleEventObserver(DerivedWithNoNewMethods())
        assertThat(
            callback, instanceOf(
                SingleGeneratedAdapterObserver::class.java
            )
        )
    }

    @Test
    fun testDerivedWithOverriddenMethodsNoGeneratedAdapter() {
        val callback = lifecycleEventObserver(
            DerivedWithOverriddenMethodsWithLfAnnotation()
        )
        // that is not effective but...
        assertThat(
            callback, instanceOf(
                ReflectiveGenericLifecycleObserver::class.java
            )
        )
    }

    @Test
    fun testInterfaceImpl1NoGeneratedAdapter() {
        val callback = lifecycleEventObserver(InterfaceImpl1())
        assertThat(
            callback, instanceOf(
                SingleGeneratedAdapterObserver::class.java
            )
        )
    }

    @Test
    fun testInterfaceImpl2NoGeneratedAdapter() {
        val callback = lifecycleEventObserver(InterfaceImpl2())
        assertThat(
            callback, instanceOf(
                CompositeGeneratedAdaptersObserver::class.java
            )
        )
    }

    @Test
    fun testInterfaceImpl3NoGeneratedAdapter() {
        val callback = lifecycleEventObserver(InterfaceImpl3())
        assertThat(
            callback, instanceOf(
                CompositeGeneratedAdaptersObserver::class.java
            )
        )
    }

    @Test
    fun testDerivedSequence() {
        val callback2 = lifecycleEventObserver(DerivedSequence2())
        assertThat(
            callback2, instanceOf(
                ReflectiveGenericLifecycleObserver::class.java
            )
        )
        val callback1 = lifecycleEventObserver(DerivedSequence1())
        assertThat(
            callback1, instanceOf(
                SingleGeneratedAdapterObserver::class.java
            )
        )
    }

    // MUST BE HERE TILL Lifecycle 3.0.0 release for back-compatibility with other modules
    @Suppress("deprecation")
    @Test
    fun testDeprecatedGenericLifecycleObserver() {
        val genericLifecycleObserver = GenericLifecycleObserver { _, _ -> }
        val observer = lifecycleEventObserver(genericLifecycleObserver)
        assertThat(observer, `is`(observer))
    }

    @Test
    fun defaultLifecycleObserverAndAnnotations() {
        class AnnotatedFullLifecycleObserver : DefaultLifecycleObserver {
            @Suppress("deprecation")
            @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
            fun onAny() {
                throw IllegalStateException(
                    "Annotations in FullLifecycleObserver must not be called"
                )
            }
        }

        val callback = lifecycleEventObserver(
            AnnotatedFullLifecycleObserver()
        )
        // check that neither of these calls fail
        callback.onStateChanged(DefaultLifecycleOwner(), Lifecycle.Event.ON_CREATE)
        callback.onStateChanged(DefaultLifecycleOwner(), Lifecycle.Event.ON_START)
        callback.onStateChanged(DefaultLifecycleOwner(), Lifecycle.Event.ON_RESUME)
        callback.onStateChanged(DefaultLifecycleOwner(), Lifecycle.Event.ON_PAUSE)
        callback.onStateChanged(DefaultLifecycleOwner(), Lifecycle.Event.ON_STOP)
        callback.onStateChanged(DefaultLifecycleOwner(), Lifecycle.Event.ON_DESTROY)
    }

    @Test
    fun lifecycleEventObserverAndAnnotations() {
        class AnnotatedLifecycleEventObserver : LifecycleEventObserver {
            @Suppress("deprecation")
            @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
            fun onAny() {
                throw IllegalStateException(
                    "Annotations in FullLifecycleObserver must not be called"
                )
            }

            override fun onStateChanged(
                source: LifecycleOwner,
                event: Lifecycle.Event
            ) {}
        }

        val callback = lifecycleEventObserver(
            AnnotatedLifecycleEventObserver()
        )

        // check that neither of these calls fail
        callback.onStateChanged(DefaultLifecycleOwner(), Lifecycle.Event.ON_CREATE)
        callback.onStateChanged(DefaultLifecycleOwner(), Lifecycle.Event.ON_START)
        callback.onStateChanged(DefaultLifecycleOwner(), Lifecycle.Event.ON_RESUME)
        callback.onStateChanged(DefaultLifecycleOwner(), Lifecycle.Event.ON_PAUSE)
        callback.onStateChanged(DefaultLifecycleOwner(), Lifecycle.Event.ON_STOP)
        callback.onStateChanged(DefaultLifecycleOwner(), Lifecycle.Event.ON_DESTROY)
    }

    internal class DefaultLifecycleOwner : LifecycleOwner {
        override val lifecycle = NoOpLifecycle()
    }
}
