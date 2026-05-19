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

package androidx.xr.scenecore.testrule

import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.InputEvent
import androidx.xr.scenecore.PointerCaptureComponent
import androidx.xr.scenecore.testing.PointerCaptureComponentTester
import androidx.xr.scenecore.testing.SceneCoreTestRule
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.util.function.Consumer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class TestRulePointerCaptureComponentTest {

    @get:Rule val scenecoreTestRule = SceneCoreTestRule()
    private lateinit var session: Session
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity

    private val stateListener =
        object : Consumer<PointerCaptureComponent.PointerCaptureState> {
            var lastState: PointerCaptureComponent.PointerCaptureState? = null

            override fun accept(newState: PointerCaptureComponent.PointerCaptureState) {
                lastState = newState
            }
        }

    private val inputListener =
        object : Consumer<InputEvent> {
            var lastEvent: InputEvent? = null

            override fun accept(inputEvent: InputEvent) {
                lastEvent = inputEvent
            }
        }

    @Before
    fun setUp() {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.create().start().get()
        val result =
            Session.create(activity, testDispatcher, lifecycleOwner = activity as LifecycleOwner)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
    }

    @After
    fun tearDown() {
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
    }

    @Test
    fun addComponent_addsRuntimeComponent() =
        runTest(testDispatcher) {
            val entity = Entity.create(session, "test")
            val pointerCaptureComponent =
                PointerCaptureComponent.create(
                    session,
                    directExecutor(),
                    stateListener,
                    inputListener,
                )

            assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()

            // Verifies addition by ensuring a tester can be successfully created for the component.
            assertThat(
                    scenecoreTestRule.createTester<PointerCaptureComponentTester>(
                        pointerCaptureComponent
                    )
                )
                .isNotNull()
            assertThat(entity.getComponents()).contains(pointerCaptureComponent)
        }

    @Test
    fun addComponent_failsIfAlreadyAttached() =
        runTest(testDispatcher) {
            val entity = Entity.create(session, "test")
            val pointerCaptureComponent =
                PointerCaptureComponent.create(
                    session,
                    directExecutor(),
                    stateListener,
                    inputListener,
                )

            assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()
            assertThat(entity.addComponent(pointerCaptureComponent)).isFalse()
        }

    @Test
    fun stateListener_propagatesCorrectlyFromRuntime() =
        runTest(testDispatcher) {
            val entity = Entity.create(session, "test")
            val pointerCaptureComponent =
                PointerCaptureComponent.create(
                    session,
                    directExecutor(),
                    stateListener,
                    inputListener,
                )

            assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()
            val tester =
                scenecoreTestRule.createTester<PointerCaptureComponentTester>(
                    pointerCaptureComponent
                )

            // Verify all states are properly converted and propagated.
            tester.triggerOnStateChanged(PointerCaptureComponent.PointerCaptureState.ACTIVE)
            advanceUntilIdle()
            assertThat(stateListener.lastState)
                .isEqualTo(PointerCaptureComponent.PointerCaptureState.ACTIVE)

            tester.triggerOnStateChanged(PointerCaptureComponent.PointerCaptureState.PAUSED)
            advanceUntilIdle()
            assertThat(stateListener.lastState)
                .isEqualTo(PointerCaptureComponent.PointerCaptureState.PAUSED)

            tester.triggerOnStateChanged(PointerCaptureComponent.PointerCaptureState.STOPPED)
            advanceUntilIdle()
            assertThat(stateListener.lastState)
                .isEqualTo(PointerCaptureComponent.PointerCaptureState.STOPPED)
        }

    @Test
    fun inputEventListener_propagatesFromRuntime() =
        runTest(testDispatcher) {
            val entity = Entity.create(session, "test")
            val pointerCaptureComponent =
                PointerCaptureComponent.create(
                    session,
                    directExecutor(),
                    stateListener,
                    inputListener,
                )

            assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()
            val tester =
                scenecoreTestRule.createTester<PointerCaptureComponentTester>(
                    pointerCaptureComponent
                )

            val inputEvent =
                InputEvent(
                    InputEvent.Source.HANDS,
                    InputEvent.Pointer.LEFT,
                    100,
                    Vector3(),
                    Vector3(0f, 0f, 1f),
                    InputEvent.Action.DOWN,
                    listOf(InputEvent.HitInfo(entity, Vector3.One, Matrix4.Identity)),
                )

            tester.triggerOnInputEvent(inputEvent)
            advanceUntilIdle()

            val receivedEvent = inputListener.lastEvent
            assertThat(receivedEvent).isNotNull()
            // Only compare non-floating point values for stability
            assertThat(receivedEvent!!.source).isEqualTo(InputEvent.Source.HANDS)
            assertThat(receivedEvent.pointerType).isEqualTo(InputEvent.Pointer.LEFT)
            assertThat(receivedEvent.timestamp).isEqualTo(inputEvent.timestamp)
            assertThat(receivedEvent.action).isEqualTo(InputEvent.Action.DOWN)
            assertThat(receivedEvent.hitInfoList).hasSize(1)

            val hitInfo = receivedEvent.hitInfoList[0]
            assertThat(hitInfo.inputEntity).isEqualTo(entity)
            assertVector3(hitInfo.hitPosition!!, Vector3.One)
            assertMatrix4(hitInfo.transform, Matrix4.Identity)
        }

    @Test
    fun removeComponent_removesRuntimeComponent() =
        runTest(testDispatcher) {
            val entity = Entity.create(session, "test")
            val pointerCaptureComponent =
                PointerCaptureComponent.create(
                    session,
                    directExecutor(),
                    stateListener,
                    inputListener,
                )

            assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()

            entity.removeComponent(pointerCaptureComponent)

            assertThat(entity.getComponents()).doesNotContain(pointerCaptureComponent)

            // Verifies removal by ensuring the component can be added back to the same entity.
            // This indirectly confirms it's no longer attached.
            assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()
        }

    private fun assertVector3(actual: Vector3, expected: Vector3, epsilon: Float = 1e-5f) {
        assertThat(actual.x).isWithin(epsilon).of(expected.x)
        assertThat(actual.y).isWithin(epsilon).of(expected.y)
        assertThat(actual.z).isWithin(epsilon).of(expected.z)
    }

    private fun assertMatrix4(actual: Matrix4, expected: Matrix4, epsilon: Float = 1e-5f) {
        for (i in 0 until 16) {
            assertThat(actual.data[i]).isWithin(epsilon).of(expected.data[i])
        }
    }
}
