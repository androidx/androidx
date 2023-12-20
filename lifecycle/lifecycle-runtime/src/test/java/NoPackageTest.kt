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

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LifecycleRegistry.Companion.createUnsafe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@RunWith(JUnit4::class)
class NoPackageTest {
    private lateinit var lifecycleOwner: LifecycleOwner
    private lateinit var lifecycle: Lifecycle
    private lateinit var registry: LifecycleRegistry

    @Before
    fun init() {
        lifecycleOwner = mock(LifecycleOwner::class.java)
        lifecycle = mock(Lifecycle::class.java)
        `when`(lifecycleOwner.lifecycle).thenReturn(lifecycle)
        registry = createUnsafe(lifecycleOwner)
    }

    @Test
    fun testNoPackage() {
        val observer = mock(NoPackageObserver::class.java)
        registry.addObserver(observer)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        verify(observer).onCreate()
    }
}
