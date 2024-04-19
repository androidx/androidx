/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.kruth.assertThat
import kotlin.native.internal.GC
import kotlin.test.BeforeTest
import kotlin.test.Test

class NativeLifecycleRegistryTest {
    private var mLifecycleOwner: LifecycleOwner? = null
    private lateinit var mRegistry: LifecycleRegistry

    @BeforeTest
    fun init() {
        mLifecycleOwner = object : LifecycleOwner {
            override val lifecycle get() = mRegistry
        }
        mRegistry = LifecycleRegistry.createUnsafe(mLifecycleOwner!!)
    }

    @Suppress("DEPRECATION")
    private fun forceGc() {
        GC.collect()
        GC.collect()
    }

    @Test
    fun goneLifecycleOwner() {
        fullyInitializeRegistry()
        mLifecycleOwner = null
        forceGc()
        val observer = TestObserver()
        mRegistry.addObserver(observer)
        assertThat(observer.onCreateCallCount).isEqualTo(0)
        assertThat(observer.onStartCallCount).isEqualTo(0)
        assertThat(observer.onResumeCallCount).isEqualTo(0)
    }

    private fun dispatchEvent(event: Lifecycle.Event) {
        mRegistry.handleLifecycleEvent(event)
    }

    private fun fullyInitializeRegistry() {
        dispatchEvent(Lifecycle.Event.ON_CREATE)
        dispatchEvent(Lifecycle.Event.ON_START)
        dispatchEvent(Lifecycle.Event.ON_RESUME)
    }
}
