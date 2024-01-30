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

import androidx.annotation.UiThread
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TestLifecycleOwnerAndroidTest {

    @UiThread
    @Test
    fun uiThreadTest() {
        val owner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        owner.currentState = Lifecycle.State.RESUMED
        assertThat(owner.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    @Test
    fun testThreadTest() {
        val owner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        owner.currentState = Lifecycle.State.RESUMED
        assertThat(owner.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    @Test
    fun testHandleLifecycleEvent() {
        val owner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        assertThat(owner.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
    }

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
    @Test
    fun testSetCurrentStateInRunTest() = runTest(timeout = 5000.milliseconds) {
        Dispatchers.setMain(coroutineContext[CoroutineDispatcher]!!)
        val owner = TestLifecycleOwner()
        owner.setCurrentState(Lifecycle.State.RESUMED)
        assertThat(owner.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        Dispatchers.resetMain()
    }
}
