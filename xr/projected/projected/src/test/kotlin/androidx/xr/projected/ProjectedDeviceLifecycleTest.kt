/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.projected

import android.app.Application
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.testing.ProjectedTestRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalProjectedApi::class)
@RunWith(RobolectricTestRunner::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class ProjectedDeviceLifecycleTest {
    @get:Rule() val projectedTestRule = ProjectedTestRule()
    private val lifecycleOwner = mock<LifecycleOwner>()
    private val context: Application = ApplicationProvider.getApplicationContext()

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    private val lifecycleObserver = mock<LifecycleEventObserver>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun create_returnsProjectedDeviceLifecycleInstance_stateChangesToInitialized() = runBlocking {
        val projectedDeviceLifecycle =
            ProjectedDeviceLifecycle(lifecycleOwner, context, testDispatcher)
        testScheduler.advanceUntilIdle()

        assertThat(projectedDeviceLifecycle.currentState).isEqualTo(Lifecycle.State.INITIALIZED)
    }

    @Test
    fun addObserver_stateChangesToCreated() = runBlocking {
        val projectedDeviceLifecycle =
            ProjectedDeviceLifecycle(lifecycleOwner, context, testDispatcher)
        projectedDeviceLifecycle.addObserver(lifecycleObserver)
        testScheduler.advanceUntilIdle()
        assertThat(projectedDeviceLifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
    }

    @Test
    fun removeObserver_stateChangesToInitialized() = runBlocking {
        val projectedDeviceLifecycle =
            ProjectedDeviceLifecycle(lifecycleOwner, context, testDispatcher)
        projectedDeviceLifecycle.addObserver(lifecycleObserver)
        testScheduler.advanceUntilIdle()
        check(projectedDeviceLifecycle.currentState == Lifecycle.State.CREATED)

        projectedDeviceLifecycle.removeObserver(lifecycleObserver)
        testScheduler.advanceUntilIdle()
        assertThat(projectedDeviceLifecycle.currentState).isEqualTo(Lifecycle.State.INITIALIZED)
    }

    @Test
    fun removeObserver_multipleObservers_removesOneObserver_doesNotChangeStateToIntialized() =
        runBlocking {
            val secondLifecycleObserver = mock<LifecycleEventObserver>()
            val projectedDeviceLifecycle =
                ProjectedDeviceLifecycle(lifecycleOwner, context, testDispatcher)
            projectedDeviceLifecycle.addObserver(lifecycleObserver)
            projectedDeviceLifecycle.addObserver(secondLifecycleObserver)
            testScheduler.advanceUntilIdle()
            check(projectedDeviceLifecycle.currentState == Lifecycle.State.CREATED)

            projectedDeviceLifecycle.removeObserver(secondLifecycleObserver)
            testScheduler.advanceUntilIdle()
            assertThat(projectedDeviceLifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        }

    @Test
    fun onProjectedDeviceStateChanged_onStartEventReceived_stateChangesToStarted() = runBlocking {
        val projectedDeviceLifecycle =
            ProjectedDeviceLifecycle(lifecycleOwner, context, testDispatcher)
        projectedDeviceLifecycle.addObserver(lifecycleObserver)
        testScheduler.advanceUntilIdle()

        projectedTestRule.lifecycleState = Lifecycle.State.STARTED
        testScheduler.advanceUntilIdle()
        verify(lifecycleObserver).onStateChanged(eq(lifecycleOwner), eq(Lifecycle.Event.ON_START))
        assertThat(projectedDeviceLifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
    }

    @Test
    fun onProjectedDeviceStateChanged_onStopEventReceived_stateChangesToCreated() = runBlocking {
        val projectedDeviceLifecycle =
            ProjectedDeviceLifecycle(lifecycleOwner, context, testDispatcher)
        projectedDeviceLifecycle.addObserver(lifecycleObserver)
        testScheduler.advanceUntilIdle()

        projectedTestRule.lifecycleState = Lifecycle.State.STARTED
        testScheduler.advanceUntilIdle()
        assertThat(projectedDeviceLifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)

        clearInvocations(lifecycleObserver)

        projectedTestRule.lifecycleState = Lifecycle.State.CREATED
        testScheduler.advanceUntilIdle()
        verify(lifecycleObserver).onStateChanged(eq(lifecycleOwner), eq(Lifecycle.Event.ON_STOP))
        assertThat(projectedDeviceLifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
    }

    @Test
    fun onProjectedDeviceStateChanged_onDestroyedEventReceived_stateChangesToDestroyed() =
        runBlocking {
            val projectedDeviceLifecycle =
                ProjectedDeviceLifecycle(lifecycleOwner, context, testDispatcher)
            projectedDeviceLifecycle.addObserver(lifecycleObserver)
            testScheduler.advanceUntilIdle()

            projectedTestRule.lifecycleState = Lifecycle.State.DESTROYED
            testScheduler.advanceUntilIdle()
            verify(lifecycleObserver)
                .onStateChanged(eq(lifecycleOwner), eq(Lifecycle.Event.ON_DESTROY))
            assertThat(projectedDeviceLifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        }
}
