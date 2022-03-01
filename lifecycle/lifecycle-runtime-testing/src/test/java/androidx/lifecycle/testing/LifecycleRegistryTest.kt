/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.lifecycle.testing

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LifecycleRegistryTest {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val lifecycleOwner = TestLifecycleOwner(
        Lifecycle.State.INITIALIZED,
        UnconfinedTestDispatcher()
    )

    @Test
    fun getCurrentState() {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        assertThat(lifecycleOwner.currentState).isEqualTo(Lifecycle.State.RESUMED)

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(lifecycleOwner.currentState).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun observerCount() {
        lifecycleOwner.currentState = Lifecycle.State.STARTED
        assertThat(lifecycleOwner.observerCount).isEqualTo(0)
        lifecycleOwner.lifecycle.addObserver(
            LifecycleEventObserver { _, _ ->
            }
        )
        assertThat(lifecycleOwner.observerCount).isEqualTo(1)
    }
}
