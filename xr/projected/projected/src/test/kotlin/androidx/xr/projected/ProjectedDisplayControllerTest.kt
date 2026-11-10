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

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.xr.projected.ProjectedDisplayController.PresentationMode
import androidx.xr.projected.ProjectedDisplayController.PresentationModeFlags
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.testing.ProjectedTestRule
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.function.Consumer
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(ExperimentalProjectedApi::class)
class ProjectedDisplayControllerTest {

    @get:Rule() val projectedTestRule = ProjectedTestRule()
    private val context: Application = ApplicationProvider.getApplicationContext()

    private lateinit var projectedDisplayController: ProjectedDisplayController

    @Test
    fun create_returnsProjectedDisplayControllerInstance() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                projectedDisplayController =
                    ProjectedDisplayController.create(projectedDeviceActivity)
            }

            assertThat(projectedDisplayController).isNotNull()
        }

    @Test
    fun create_throwsIllegalStateException() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            projectedTestRule.shouldThrowIllegalStateExceptionWhenCreatingControllers = true

            assertFailsWith<IllegalStateException> {
                runBlocking {
                    projectedDisplayController =
                        ProjectedDisplayController.create(projectedDeviceActivity)
                }
            }
        }

    @Test
    fun create_nonProjectedDeviceActivity_throwsException() =
        launchTestActivity { nonProjectedDeviceActivity ->
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { ProjectedDisplayController.create(nonProjectedDeviceActivity) }
            }
        }

    @Test
    fun addFlags_callsService() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                projectedDisplayController =
                    ProjectedDisplayController.create(projectedDeviceActivity)
            }
            val flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

            projectedDisplayController.addLayoutParamsFlags(flags)

            assertThat(projectedTestRule.projectedLayoutParamFlags.and(flags) == flags).isTrue()
        }

    @Test
    fun removeFlags_callsService() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                projectedDisplayController =
                    ProjectedDisplayController.create(projectedDeviceActivity)
            }
            val flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

            projectedDisplayController.addLayoutParamsFlags(flags)
            check(projectedTestRule.projectedLayoutParamFlags.and(flags) == flags)

            projectedDisplayController.removeLayoutParamsFlags(flags)
            assertThat(projectedTestRule.projectedLayoutParamFlags.and(flags) == flags).isFalse()
        }

    @Test
    fun close_disconnectsConnection() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                projectedDisplayController =
                    ProjectedDisplayController.create(projectedDeviceActivity)
            }
            check(shadowOf(context).boundServiceConnections.size == 1)

            projectedDisplayController.close()

            assertThat(shadowOf(context).boundServiceConnections).isEmpty()
        }

    @Test
    fun addPresentationModeChangedListener_receivesUpdates() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                projectedDisplayController =
                    ProjectedDisplayController.create(projectedDeviceActivity)

                var presentationModes: PresentationModeFlags? = null

                projectedDisplayController.addPresentationModeChangedListener { updatedModes ->
                    presentationModes = updatedModes
                }

                // Trigger the callback.
                projectedTestRule.presentationModeFlags =
                    setOf(PresentationMode.VISUALS_ON, PresentationMode.AUDIO_ON)

                assertThat(presentationModes).isNotNull()
                assertThat(presentationModes?.hasPresentationMode(PresentationMode.VISUALS_ON))
                    .isTrue()
                assertThat(presentationModes?.hasPresentationMode(PresentationMode.AUDIO_ON))
                    .isTrue()
            }
        }

    @Test
    fun addPresentationModeChangedListener_invokesListenerOnCoroutineContextDispatcher() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            val dispatcher = StandardTestDispatcher()
            runBlocking {
                projectedDisplayController =
                    ProjectedDisplayController.create(projectedDeviceActivity, dispatcher)
                var presentationModes: PresentationModeFlags? = null

                projectedDisplayController.addPresentationModeChangedListener { updatedModes ->
                    presentationModes = updatedModes
                }
                projectedTestRule.presentationModeFlags = setOf(PresentationMode.VISUALS_ON)
                check(presentationModes == null)
                dispatcher.scheduler.advanceUntilIdle()

                assertThat(presentationModes?.hasPresentationMode(PresentationMode.VISUALS_ON))
                    .isTrue()
            }
        }

    @Test
    fun addMultipleEngagementModeChangedListener_receivesUpdates() {
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                projectedDisplayController =
                    ProjectedDisplayController.create(projectedDeviceActivity)

                var presentationModes1: PresentationModeFlags? = null
                var callCount1 = 0
                val listener1: (PresentationModeFlags) -> Unit = { updatedModes ->
                    presentationModes1 = updatedModes
                    ++callCount1
                }
                var presentationModes2: PresentationModeFlags? = null
                var callCount2 = 0
                val listener2: (PresentationModeFlags) -> Unit = { updatedModes ->
                    presentationModes2 = updatedModes
                    ++callCount2
                }
                projectedDisplayController.addPresentationModeChangedListener(listener = listener1)
                projectedDisplayController.addPresentationModeChangedListener(listener = listener2)

                // Trigger the callback.
                projectedTestRule.presentationModeFlags =
                    setOf(PresentationMode.VISUALS_ON, PresentationMode.AUDIO_ON)

                // Verify that both listeners were called once.
                assertThat(callCount1).isEqualTo(1)
                assertThat(presentationModes1).isNotNull()
                assertThat(presentationModes1?.hasPresentationMode(PresentationMode.VISUALS_ON))
                    .isTrue()
                assertThat(presentationModes1?.hasPresentationMode(PresentationMode.AUDIO_ON))
                    .isTrue()
                assertThat(callCount2).isEqualTo(1)
                assertThat(presentationModes2).isNotNull()
                assertThat(presentationModes2?.hasPresentationMode(PresentationMode.VISUALS_ON))
                    .isTrue()
                assertThat(presentationModes1?.hasPresentationMode(PresentationMode.AUDIO_ON))
                    .isTrue()

                // Remove the second callback.
                projectedDisplayController.removePresentationModeChangedListener(listener2)

                // Trigger another callback.
                projectedTestRule.presentationModeFlags = setOf(PresentationMode.AUDIO_ON)

                // Verify that only the first listener was called again
                assertThat(callCount1).isEqualTo(2)
                assertThat(presentationModes1).isNotNull()
                assertThat(presentationModes1?.hasPresentationMode(PresentationMode.VISUALS_ON))
                    .isFalse()
                assertThat(presentationModes1?.hasPresentationMode(PresentationMode.AUDIO_ON))
                    .isTrue()
                assertThat(callCount2).isEqualTo(1)
                assertThat(presentationModes2).isNotNull()
                assertThat(presentationModes2?.hasPresentationMode(PresentationMode.VISUALS_ON))
                    .isTrue()
                assertThat(presentationModes1?.hasPresentationMode(PresentationMode.AUDIO_ON))
                    .isTrue()
            }
        }
    }

    @Test
    fun addPresentationModeChangedListener_sameListenerAddedTwice_receivesUpdatesOnce() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                projectedDisplayController =
                    ProjectedDisplayController.create(projectedDeviceActivity)
                var callCount = 0
                val listener: (PresentationModeFlags) -> Unit = { callCount++ }
                projectedDisplayController.addPresentationModeChangedListener(listener)
                projectedDisplayController.addPresentationModeChangedListener(listener)

                projectedTestRule.presentationModeFlags = setOf(PresentationMode.VISUALS_ON)
                projectedDisplayController.removePresentationModeChangedListener(listener)
                projectedTestRule.presentationModeFlags = setOf(PresentationMode.AUDIO_ON)

                assertThat(callCount).isEqualTo(1)
            }
        }

    @Test
    fun removePresentationModeChangedListener_updateAlreadyQueued_doesNotInvokeListener() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            val dispatcher = StandardTestDispatcher()
            runBlocking {
                projectedDisplayController =
                    ProjectedDisplayController.create(projectedDeviceActivity, dispatcher)
                var callCount = 0
                val listener: (PresentationModeFlags) -> Unit = { callCount++ }
                projectedDisplayController.addPresentationModeChangedListener(listener)
                projectedTestRule.presentationModeFlags = setOf(PresentationMode.VISUALS_ON)

                projectedDisplayController.removePresentationModeChangedListener(listener)
                dispatcher.scheduler.advanceUntilIdle()

                assertThat(callCount).isEqualTo(0)
            }
        }

    @Test
    fun close_removesPresentationModeListenersAndUnbindsService() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                projectedDisplayController =
                    ProjectedDisplayController.create(projectedDeviceActivity)
                var callCount = 0
                projectedDisplayController.addPresentationModeChangedListener { callCount++ }
                projectedTestRule.presentationModeFlags = setOf(PresentationMode.VISUALS_ON)
                check(callCount == 1)

                projectedDisplayController.close()
                projectedTestRule.presentationModeFlags = setOf(PresentationMode.AUDIO_ON)

                assertThat(callCount).isEqualTo(1)
                assertThat(shadowOf(context).boundServiceConnections).isEmpty()
            }
        }

    @Test
    fun addPresentationModeChangedListener_contextJobCancelled_stillInvokesListener() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                val cancelledJob = Job().apply { cancel() }
                projectedDisplayController =
                    ProjectedDisplayController.create(projectedDeviceActivity, cancelledJob)
                var presentationModes: PresentationModeFlags? = null

                projectedDisplayController.addPresentationModeChangedListener { updatedModes ->
                    presentationModes = updatedModes
                }
                projectedTestRule.presentationModeFlags = setOf(PresentationMode.VISUALS_ON)

                assertThat(presentationModes?.hasPresentationMode(PresentationMode.VISUALS_ON))
                    .isTrue()
            }
        }

    @Test
    fun addPresentationModeChangedListener_listenerThrows_stillReceivesUpdates() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                val exceptions = mutableListOf<Throwable>()
                val exceptionHandler = CoroutineExceptionHandler { _, e -> exceptions.add(e) }
                projectedDisplayController =
                    ProjectedDisplayController.create(projectedDeviceActivity, exceptionHandler)
                var callCount = 0
                projectedDisplayController.addPresentationModeChangedListener {
                    callCount++
                    throw IllegalStateException("Listener failed")
                }

                projectedTestRule.presentationModeFlags = setOf(PresentationMode.VISUALS_ON)
                projectedTestRule.presentationModeFlags = setOf(PresentationMode.AUDIO_ON)

                assertThat(callCount).isEqualTo(2)
                assertThat(exceptions).hasSize(2)
            }
        }

    @Suppress("DEPRECATION") // Tests the deprecated Consumer-based listener APIs.
    @Test
    fun addPresentationModeChangedListener_consumerThrowsOnCurrentState_canBeAddedAgain() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                projectedDisplayController =
                    ProjectedDisplayController.create(projectedDeviceActivity)
                // Once there's a current state, a newly added listener receives it right away.
                projectedDisplayController.addPresentationModeChangedListener {}
                projectedTestRule.presentationModeFlags = setOf(PresentationMode.VISUALS_ON)
                var shouldThrow = true
                var callCount = 0
                val listener =
                    Consumer<PresentationModeFlags> {
                        if (shouldThrow) throw IllegalStateException("Listener failed")
                        callCount++
                    }
                val directExecutor = Executor(Runnable::run)
                assertFailsWith<IllegalStateException> {
                    projectedDisplayController.addPresentationModeChangedListener(
                        directExecutor,
                        listener,
                    )
                }
                shouldThrow = false

                projectedDisplayController.addPresentationModeChangedListener(
                    directExecutor,
                    listener,
                )

                assertThat(callCount).isEqualTo(1)
            }
        }

    @Suppress("DEPRECATION") // Tests the deprecated Consumer-based listener APIs.
    @Test
    fun addPresentationModeChangedListener_withConsumer_receivesUpdatesUntilRemoved() =
        projectedTestRule.launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                projectedDisplayController =
                    ProjectedDisplayController.create(projectedDeviceActivity)
                var callCount = 0
                val listener = Consumer<PresentationModeFlags> { callCount++ }
                projectedDisplayController.addPresentationModeChangedListener(
                    Executor(Runnable::run),
                    listener,
                )
                projectedTestRule.presentationModeFlags = setOf(PresentationMode.VISUALS_ON)
                check(callCount == 1)

                projectedDisplayController.removePresentationModeChangedListener(listener)
                projectedTestRule.presentationModeFlags = setOf(PresentationMode.AUDIO_ON)

                assertThat(callCount).isEqualTo(1)
            }
        }

    private fun launchTestActivity(block: (Activity) -> Unit) {
        shadowOf(context.packageManager)
            .addOrUpdateActivity(
                ActivityInfo().apply {
                    name = TestActivity::class.java.name
                    packageName = context.packageName
                }
            )
        ActivityScenario.launch<TestActivity>(Intent(context, TestActivity::class.java)).use {
            activityScenario ->
            activityScenario.onActivity { activity -> block(activity) }
        }
    }
}

private class TestActivity : Activity()
