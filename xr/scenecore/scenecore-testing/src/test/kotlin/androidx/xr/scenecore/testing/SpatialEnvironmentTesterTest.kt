/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore.testing

import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.SpatialEnvironment
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.util.function.Consumer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SpatialEnvironmentTesterTest {

    @get:Rule val testRule = SceneCoreTestRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var session: Session
    private lateinit var spatialEnvironment: SpatialEnvironment
    private lateinit var tester: SpatialEnvironmentTester

    @Before
    fun setUp() = runBlocking {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.create().start().get()
        val result =
            Session.create(activity, testDispatcher, lifecycleOwner = activity as LifecycleOwner)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
        spatialEnvironment = session.scene.spatialEnvironment
        tester = testRule.spatialEnvironmentTester
    }

    @After
    fun tearDown() {
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
    }

    @Test
    fun onPassthroughOpacityChanged_currentPassthroughOpacity_getsRuntimePassthroughOpacity() {
        val rtOpacity = 0.3f

        assertThat(spatialEnvironment.currentPassthroughOpacity).isEqualTo(0.0f)

        tester.triggerPassthroughOpacityChanged(rtOpacity)

        assertThat(spatialEnvironment.currentPassthroughOpacity).isEqualTo(rtOpacity)
    }

    @Test
    fun onPassthroughOpacityChanged_addPassthroughOpacityChangedListener_invokesListenerCorrectly() {
        var listenerCalledWithValue = 0.0f
        val listener = Consumer<Float> { floatValue: Float -> listenerCalledWithValue = floatValue }
        spatialEnvironment.addPassthroughOpacityChangedListener(listener)

        tester.triggerPassthroughOpacityChanged(0.3f)
        ShadowLooper.idleMainLooper()

        assertThat(listenerCalledWithValue).isEqualTo(0.3f)
    }

    @Test
    fun onPassthroughOpacityChanged_addPassthroughOpacityChangedListenerWithExecutor_invokesListenerCorrectly() {
        var listenerCalledWithValue = 0.0f
        var listenerThread: Thread? = null
        val executor = directExecutor()

        val listener =
            Consumer<Float> { floatValue: Float ->
                listenerCalledWithValue = floatValue
                listenerThread = Thread.currentThread()
            }
        spatialEnvironment.addPassthroughOpacityChangedListener(executor, listener)

        val eventValue = 0.3f
        tester.triggerPassthroughOpacityChanged(eventValue)

        assertThat(listenerCalledWithValue).isEqualTo(eventValue)
        assertThat(listenerThread).isNotNull()
    }

    @Test
    fun onPassthroughOpacityChanged_removePassthroughOpacityChangedListener_notInvokeListener() {
        var listenerCalledCount = 0
        val listener = Consumer<Float> { listenerCalledCount++ }
        spatialEnvironment.addPassthroughOpacityChangedListener(listener)

        spatialEnvironment.removePassthroughOpacityChangedListener(listener)
        tester.triggerPassthroughOpacityChanged(0.3f)
        ShadowLooper.idleMainLooper()

        assertThat(listenerCalledCount).isEqualTo(0)
    }

    @Test
    fun onSpatialEnvironmentChanged_isPreferredSpatialEnvironmentActive_returnsCorrectly() {
        tester.triggerSpatialEnvironmentChanged(true)

        assertThat(spatialEnvironment.isPreferredSpatialEnvironmentActive).isTrue()
    }

    @Test
    fun onSpatialEnvironmentChanged_addSpatialEnvironmentChangedListener_invokesListenerCorrectly() {
        var listenerCalled = false
        val listener = Consumer<Boolean> { called: Boolean -> listenerCalled = called }
        spatialEnvironment.addSpatialEnvironmentChangedListener(listener)
        tester.triggerSpatialEnvironmentChanged(true)
        ShadowLooper.idleMainLooper()

        assertThat(listenerCalled).isTrue()
    }

    @Test
    fun onSpatialEnvironmentChanged_addSpatialEnvironmentChangedListenerWithExecutor_invokesListenerCorrectly() {
        var listenerCalledWithValue = false
        var listenerThread: Thread? = null
        val executor = directExecutor()

        val listener =
            Consumer<Boolean> { boolValue: Boolean ->
                listenerCalledWithValue = boolValue
                listenerThread = Thread.currentThread()
            }
        spatialEnvironment.addSpatialEnvironmentChangedListener(executor, listener)

        val eventValue = true
        tester.triggerSpatialEnvironmentChanged(eventValue)

        assertThat(listenerCalledWithValue).isEqualTo(eventValue)
        assertThat(listenerThread).isNotNull()
    }

    @Test
    fun onSpatialEnvironmentChanged_removeSpatialEnvironmentChangedListener_notInvokeListener() {
        var listenerCalledCount = 0
        val listener = Consumer<Boolean> { listenerCalledCount++ }
        spatialEnvironment.addSpatialEnvironmentChangedListener(listener)

        tester.triggerSpatialEnvironmentChanged(true)
        ShadowLooper.idleMainLooper()

        assertThat(listenerCalledCount).isEqualTo(1)

        spatialEnvironment.removeSpatialEnvironmentChangedListener(listener)
        tester.triggerSpatialEnvironmentChanged(true)
        ShadowLooper.idleMainLooper()

        assertThat(listenerCalledCount).isEqualTo(1)
    }
}
