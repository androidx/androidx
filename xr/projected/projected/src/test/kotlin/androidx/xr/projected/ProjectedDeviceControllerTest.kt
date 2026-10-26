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
import android.os.DeadObjectException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.xr.projected.ProjectedDeviceController.Capability.Companion.CAPABILITY_VISUAL_UI
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.platform.BatteryState as AidlBatteryState
import androidx.xr.projected.platform.IBatteryStateListener
import androidx.xr.projected.platform.IProjectedService
import androidx.xr.projected.testing.ProjectedTestRule
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(ExperimentalProjectedApi::class, ExperimentalCoroutinesApi::class)
class ProjectedDeviceControllerTest {

    @get:Rule() val projectedTestRule = ProjectedTestRule()

    private lateinit var testScope: TestScope

    private val mockProjectedService: IProjectedService
        get() = ReflectionHelpers.getField(projectedTestRule, "mockProjectedService")

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

    @Suppress("DEPRECATION") // Tests the deprecated CoroutineContext-based listener APIs.
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

    @Suppress("DEPRECATION") // Tests the deprecated CoroutineContext-based listener APIs.
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

    @Suppress("DEPRECATION") // Tests the deprecated CoroutineContext-based listener APIs.
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

    @Suppress("DEPRECATION") // Tests the deprecated CoroutineContext-based listener APIs.
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

    @Test
    fun addBatteryStateChangedListener_invokesListenerOnCoroutineContextDispatcher() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                ProjectedDeviceController.create(
                        projectedDeviceActivity,
                        testScope.coroutineContext,
                    )
                    .use { controller ->
                        var receivedState: BatteryState? = null

                        controller.addBatteryStateChangedListener { state -> receivedState = state }
                        val testState = BatteryState(isCharging = true, batteryLevel = 80)
                        projectedTestRule.setBatteryState(testState)
                        check(receivedState == null)
                        testScope.advanceUntilIdle()

                        assertThat(receivedState).isEqualTo(testState)
                    }
            }
        }

    @Test
    fun addBatteryStateChangedListener_noDispatcher_invokesListenerDirectly() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                ProjectedDeviceController.create(projectedDeviceActivity).use { controller ->
                    var receivedState: BatteryState? = null

                    controller.addBatteryStateChangedListener { state -> receivedState = state }
                    val testState = BatteryState(isCharging = true, batteryLevel = 80)
                    projectedTestRule.setBatteryState(testState)

                    assertThat(receivedState).isEqualTo(testState)
                }
            }
        }

    @Test
    fun addBatteryStateChangedListener_contextJobCancelled_stillInvokesListener() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                val cancelledJob = Job().apply { cancel() }
                ProjectedDeviceController.create(projectedDeviceActivity, cancelledJob).use {
                    controller ->
                    var receivedState: BatteryState? = null

                    controller.addBatteryStateChangedListener { state -> receivedState = state }
                    val testState = BatteryState(isCharging = true, batteryLevel = 80)
                    projectedTestRule.setBatteryState(testState)

                    assertThat(receivedState).isEqualTo(testState)
                }
            }
        }

    @Test
    fun addBatteryStateChangedListener_listenerThrows_stillReceivesUpdates() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                val exceptions = mutableListOf<Throwable>()
                val exceptionHandler = CoroutineExceptionHandler { _, e -> exceptions.add(e) }
                ProjectedDeviceController.create(projectedDeviceActivity, exceptionHandler).use {
                    controller ->
                    var callCount = 0
                    controller.addBatteryStateChangedListener {
                        callCount++
                        throw IllegalStateException("Listener failed")
                    }

                    projectedTestRule.setBatteryState(BatteryState(true, 90))
                    projectedTestRule.setBatteryState(BatteryState(false, 70))

                    assertThat(callCount).isEqualTo(2)
                    assertThat(exceptions).hasSize(2)
                }
            }
        }

    @Test
    fun addBatteryStateChangedListener_sameListenerAddedTwice_receivesUpdatesOnce() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                ProjectedDeviceController.create(projectedDeviceActivity).use { controller ->
                    var callCount = 0
                    val listener: (BatteryState) -> Unit = { callCount++ }

                    controller.addBatteryStateChangedListener(listener)
                    controller.addBatteryStateChangedListener(listener)
                    projectedTestRule.setBatteryState(
                        BatteryState(isCharging = true, batteryLevel = 90)
                    )

                    assertThat(callCount).isEqualTo(1)
                }
            }
        }

    @Test
    fun addBatteryStateChangedListener_serviceNotRunning_throwsIllegalStateException() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                ProjectedDeviceController.create(projectedDeviceActivity).use { controller ->
                    val listener: (BatteryState) -> Unit = {}
                    doThrow(DeadObjectException())
                        .whenever(mockProjectedService)
                        .registerBatteryStateListener(any())

                    assertFailsWith<IllegalStateException> {
                        controller.addBatteryStateChangedListener(listener)
                    }

                    // The listener wasn't added, so removing it doesn't unregister anything.
                    controller.removeBatteryStateChangedListener(listener)
                    verify(mockProjectedService, never()).unregisterBatteryStateListener(any())
                }
            }
        }

    @Test
    fun addBatteryStateChangedListener_registrationThrows_canBeAddedAgain() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                ProjectedDeviceController.create(projectedDeviceActivity).use { controller ->
                    val listener: (BatteryState) -> Unit = {}
                    doThrow(SecurityException())
                        .doNothing()
                        .whenever(mockProjectedService)
                        .registerBatteryStateListener(any())
                    assertFailsWith<SecurityException> {
                        controller.addBatteryStateChangedListener(listener)
                    }

                    controller.addBatteryStateChangedListener(listener)

                    verify(mockProjectedService, times(2)).registerBatteryStateListener(any())
                }
            }
        }

    @Test
    fun removeBatteryStateChangedListener_addedWithoutContext_stopsUpdates() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                ProjectedDeviceController.create(projectedDeviceActivity).use { controller ->
                    var callCount = 0
                    val listener: (BatteryState) -> Unit = { callCount++ }
                    controller.addBatteryStateChangedListener(listener)
                    projectedTestRule.setBatteryState(
                        BatteryState(isCharging = true, batteryLevel = 90)
                    )
                    check(callCount == 1)

                    controller.removeBatteryStateChangedListener(listener)
                    projectedTestRule.setBatteryState(
                        BatteryState(isCharging = false, batteryLevel = 70)
                    )

                    assertThat(callCount).isEqualTo(1)
                }
            }
        }

    @Test
    fun removeBatteryStateChangedListener_updateAlreadyQueued_doesNotInvokeListener() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                ProjectedDeviceController.create(
                        projectedDeviceActivity,
                        testScope.coroutineContext,
                    )
                    .use { controller ->
                        var callCount = 0
                        val listener: (BatteryState) -> Unit = { callCount++ }
                        controller.addBatteryStateChangedListener(listener)
                        projectedTestRule.setBatteryState(
                            BatteryState(isCharging = true, batteryLevel = 90)
                        )

                        controller.removeBatteryStateChangedListener(listener)
                        testScope.advanceUntilIdle()

                        assertThat(callCount).isEqualTo(0)
                    }
            }
        }

    @Test
    fun addBatteryStateChangedListener_listenerRemovesItselfOnCurrentState_staysRemoved() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                ProjectedDeviceController.create(projectedDeviceActivity).use { controller ->
                    // Like the real service, send the current state during registration.
                    val registeredListeners = mutableListOf<IBatteryStateListener>()
                    doAnswer { invocation ->
                            val aidlListener = invocation.getArgument<IBatteryStateListener>(0)
                            registeredListeners.add(aidlListener)
                            aidlListener.onBatteryStateChanged(
                                AidlBatteryState().apply {
                                    isCharging = true
                                    batteryLevel = 50
                                }
                            )
                            null
                        }
                        .whenever(mockProjectedService)
                        .registerBatteryStateListener(any())
                    var callCount = 0
                    lateinit var listener: (BatteryState) -> Unit
                    listener = {
                        callCount++
                        controller.removeBatteryStateChangedListener(listener)
                    }

                    controller.addBatteryStateChangedListener(listener)
                    // A later update from the service doesn't reach the removed listener.
                    registeredListeners
                        .single()
                        .onBatteryStateChanged(
                            AidlBatteryState().apply {
                                isCharging = false
                                batteryLevel = 40
                            }
                        )

                    assertThat(callCount).isEqualTo(1)
                    verify(mockProjectedService, atLeastOnce())
                        .unregisterBatteryStateListener(registeredListeners.single())
                }
            }
        }

    @Test
    fun close_unregistersListenersAddedWithoutContext() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                val controller = ProjectedDeviceController.create(projectedDeviceActivity)
                var stateChanges = 0
                controller.addBatteryStateChangedListener { stateChanges++ }
                projectedTestRule.setBatteryState(BatteryState(true, 50))
                check(stateChanges == 1)

                controller.close()
                projectedTestRule.setBatteryState(BatteryState(false, 40))

                assertThat(stateChanges).isEqualTo(1)
            }
        }
}
