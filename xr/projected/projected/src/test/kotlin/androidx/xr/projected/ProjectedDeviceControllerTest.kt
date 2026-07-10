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

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.xr.projected.ProjectedDeviceController.Capability.Companion.CAPABILITY_VISUAL_UI
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.testing.ProjectedTestRule
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(ExperimentalProjectedApi::class, ExperimentalCoroutinesApi::class)
class ProjectedDeviceControllerTest {

    @get:Rule() val projectedTestRule = ProjectedTestRule()

    private lateinit var testScope: TestScope

    private lateinit var projectedDeviceController: ProjectedDeviceController

    @Before
    fun setUp() {
        testScope = TestScope()
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun create_throwsIllegalStateException() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            projectedTestRule.shouldThrowIllegalStateExceptionWhenCreatingControllers = true

            assertFailsWith<IllegalStateException> {
                runBlocking {
                    projectedDeviceController =
                        ProjectedDeviceController.create(projectedDeviceActivity)
                }
            }
        }

    @Test
    fun capabilities_capabilitiesIncludeVisualUi_returnsCapabilityVisualUi() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                projectedDeviceController =
                    ProjectedDeviceController.create(projectedDeviceActivity)
            }

            assertThat(projectedDeviceController.capabilities).contains(CAPABILITY_VISUAL_UI)
        }

    @Test
    fun capabilities_emptyCapabilities_doesNotReturnCapabilityVisualUi() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            projectedTestRule.capabilities = setOf()
            runBlocking {
                projectedDeviceController =
                    ProjectedDeviceController.create(projectedDeviceActivity)
            }

            assertThat(projectedDeviceController.capabilities).doesNotContain(CAPABILITY_VISUAL_UI)
        }

    @Test
    fun audioDevices_twoDevicesSetByDefault_returnsTwoAudioDevices() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                projectedDeviceController =
                    ProjectedDeviceController.create(projectedDeviceActivity)
            }

            assertThat(projectedDeviceController.audioDevices).hasSize(2)
        }

    @Test
    fun audioDevices_emptyProjectedAudioDevices_returnsEmptyList() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            projectedTestRule.audioDevices = listOf()
            runBlocking {
                projectedDeviceController =
                    ProjectedDeviceController.create(projectedDeviceActivity)
            }

            assertThat(projectedDeviceController.audioDevices).isEmpty()
        }

    @Test
    fun addBatteryStateChangedListener_receivesUpdates() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                ProjectedDeviceController.create(projectedDeviceActivity).use { controller ->
                    var receivedState: BatteryState? = null
                    controller.addBatteryStateChangedListener(testScope.coroutineContext) { state ->
                        receivedState = state
                    }

                    val testState1 = BatteryState(isCharging = true, batteryLevel = 80)
                    projectedTestRule.setBatteryState(testState1)
                    testScope.advanceUntilIdle()
                    assertThat(receivedState).isEqualTo(testState1)

                    val testState2 = BatteryState(isCharging = false, batteryLevel = 20)
                    projectedTestRule.setBatteryState(testState2)
                    testScope.advanceUntilIdle()
                    assertThat(receivedState).isEqualTo(testState2)
                }
            }
        }

    @Test
    fun removeBatteryStateChangedListener_stopsUpdates() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                ProjectedDeviceController.create(projectedDeviceActivity).use { controller ->
                    var callCount = 0
                    val listener: (BatteryState) -> Unit = { _ -> callCount++ }

                    controller.addBatteryStateChangedListener(testScope.coroutineContext, listener)

                    projectedTestRule.setBatteryState(
                        BatteryState(isCharging = true, batteryLevel = 90)
                    )
                    testScope.advanceUntilIdle()
                    assertThat(callCount).isEqualTo(1)

                    controller.removeBatteryStateChangedListener(listener)

                    projectedTestRule.setBatteryState(
                        BatteryState(isCharging = false, batteryLevel = 70)
                    )
                    testScope.advanceUntilIdle()
                    assertThat(callCount).isEqualTo(1)
                }
            }
        }

    @Test
    fun batteryStateListener_scopeCancelled_stopsUpdates() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                ProjectedDeviceController.create(projectedDeviceActivity).use { controller ->
                    var callCount = 0
                    val listener: (BatteryState) -> Unit = { _ -> callCount++ }

                    controller.addBatteryStateChangedListener(testScope.coroutineContext, listener)

                    projectedTestRule.setBatteryState(
                        BatteryState(isCharging = true, batteryLevel = 90)
                    )
                    testScope.advanceUntilIdle()
                    assertThat(callCount).isEqualTo(1)

                    testScope.cancel()

                    projectedTestRule.setBatteryState(
                        BatteryState(isCharging = false, batteryLevel = 70)
                    )
                    assertThat(callCount).isEqualTo(1)
                }
            }
        }

    @Test
    fun close_unregistersListeners() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                val controller = ProjectedDeviceController.create(projectedDeviceActivity)
                var stateChanges = 0

                controller.addBatteryStateChangedListener(testScope.coroutineContext) { _ ->
                    stateChanges++
                }

                projectedTestRule.setBatteryState(BatteryState(true, 50))
                testScope.advanceUntilIdle()
                assertThat(stateChanges).isEqualTo(1)

                controller.close()

                projectedTestRule.setBatteryState(BatteryState(false, 40))
                testScope.advanceUntilIdle()

                assertThat(stateChanges).isEqualTo(1)
            }
        }
}
