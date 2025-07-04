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

package androidx.camera.camera2.pipe.compat

import android.os.Build
import androidx.camera.camera2.pipe.AudioRestrictionMode.Companion.AUDIO_RESTRICTION_VIBRATION
import androidx.camera.camera2.pipe.AudioRestrictionMode.Companion.AUDIO_RESTRICTION_VIBRATION_SOUND
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.internal.CameraPipeLifetime
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
class AudioRestrictionControllerImplTest {
    private val testScope = TestScope()
    private val threads = FakeThreads.fromTestScope(testScope)
    private val cameraGraph1: CameraGraph = mock()
    private val cameraGraph2: CameraGraph = mock()
    private val listener1: AudioRestrictionController.Listener = mock()
    private val listener2: AudioRestrictionController.Listener = mock()
    private val cameraPipeLifetime = CameraPipeLifetime()

    @Test
    fun setAudioRestrictionMode_ListenerUpdatedToHighestMode() =
        testScope.runTest {
            val audioRestrictionController =
                AudioRestrictionControllerImpl(threads, cameraPipeLifetime)
            audioRestrictionController.addListener(listener1)
            audioRestrictionController.addListener(listener2)

            audioRestrictionController.updateCameraGraphAudioRestrictionMode(
                cameraGraph1,
                AUDIO_RESTRICTION_VIBRATION,
            )
            advanceUntilIdle()

            verify(listener1, times(1)).onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION)
            verify(listener2, times(1)).onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION)

            audioRestrictionController.updateCameraGraphAudioRestrictionMode(
                cameraGraph2,
                AUDIO_RESTRICTION_VIBRATION_SOUND,
            )
            advanceUntilIdle()

            verify(listener1, times(1))
                .onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION_SOUND)
            verify(listener2, times(1))
                .onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION_SOUND)
        }

    @Test
    fun setGlobalAudioRestrictionMode_ListenerUpdatedToHighestMode() =
        testScope.runTest {
            val audioRestrictionController =
                AudioRestrictionControllerImpl(threads, cameraPipeLifetime)
            audioRestrictionController.addListener(listener1)
            audioRestrictionController.addListener(listener2)

            audioRestrictionController.updateCameraGraphAudioRestrictionMode(
                cameraGraph1,
                AUDIO_RESTRICTION_VIBRATION,
            )
            advanceUntilIdle()

            verify(listener1, times(1)).onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION)
            verify(listener2, times(1)).onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION)

            audioRestrictionController.globalAudioRestrictionMode =
                AUDIO_RESTRICTION_VIBRATION_SOUND
            advanceUntilIdle()

            verify(listener1, times(1))
                .onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION_SOUND)
            verify(listener2, times(1))
                .onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION_SOUND)
        }

    @Test
    fun setAudioRestrictionMode_lowerModeNotOverrideHigherMode() =
        testScope.runTest {
            val audioRestrictionController =
                AudioRestrictionControllerImpl(threads, cameraPipeLifetime)
            audioRestrictionController.addListener(listener1)

            audioRestrictionController.updateCameraGraphAudioRestrictionMode(
                cameraGraph1,
                AUDIO_RESTRICTION_VIBRATION_SOUND,
            )
            audioRestrictionController.updateCameraGraphAudioRestrictionMode(
                cameraGraph2,
                AUDIO_RESTRICTION_VIBRATION,
            )
            advanceUntilIdle()

            // If the mode hasn't changed, there shouldn't be a second update call
            verify(listener1, times(1))
                .onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION_SOUND)
            verify(listener1, never()).onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION)
        }

    @Test
    fun setGlobalAudioRestrictionMode_lowerModeNotOverrideHigherMode() =
        testScope.runTest {
            val audioRestrictionController =
                AudioRestrictionControllerImpl(threads, cameraPipeLifetime)
            audioRestrictionController.addListener(listener1)

            audioRestrictionController.updateCameraGraphAudioRestrictionMode(
                cameraGraph1,
                AUDIO_RESTRICTION_VIBRATION_SOUND,
            )
            audioRestrictionController.globalAudioRestrictionMode = AUDIO_RESTRICTION_VIBRATION
            advanceUntilIdle()

            // If the mode hasn't changed, there shouldn't be a second update call
            verify(listener1, times(1))
                .onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION_SOUND)
            verify(listener1, never()).onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION)
        }

    @Test
    fun removeCameraGraphAudioRestriction_associatedModeUpdated() =
        testScope.runTest {
            val audioRestrictionController =
                AudioRestrictionControllerImpl(threads, cameraPipeLifetime)
            audioRestrictionController.addListener(listener1)

            audioRestrictionController.updateCameraGraphAudioRestrictionMode(
                cameraGraph1,
                AUDIO_RESTRICTION_VIBRATION_SOUND,
            )
            audioRestrictionController.updateCameraGraphAudioRestrictionMode(
                cameraGraph2,
                AUDIO_RESTRICTION_VIBRATION,
            )
            advanceUntilIdle()

            verify(listener1, times(1))
                .onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION_SOUND)

            audioRestrictionController.removeCameraGraph(cameraGraph1)
            advanceUntilIdle()

            verify(listener1, times(1)).onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION)
        }

    @Test
    fun addListenerAfterUpdateMode_newListenerUpdated() =
        testScope.runTest {
            val mode = AUDIO_RESTRICTION_VIBRATION
            val audioRestrictionController =
                AudioRestrictionControllerImpl(threads, cameraPipeLifetime)
            audioRestrictionController.addListener(listener1)

            audioRestrictionController.updateCameraGraphAudioRestrictionMode(cameraGraph1, mode)
            advanceUntilIdle()
            audioRestrictionController.addListener(listener2)
            advanceUntilIdle()

            verify(listener1, times(1)).onCameraAudioRestrictionUpdated(mode)
            verify(listener2, times(1)).onCameraAudioRestrictionUpdated(mode)
        }

    @Test
    fun setRestrictionBeforeAddingListener_listenerSetToUpdatedMode() =
        testScope.runTest {
            val mode = AUDIO_RESTRICTION_VIBRATION
            val audioRestrictionController =
                AudioRestrictionControllerImpl(threads, cameraPipeLifetime)

            audioRestrictionController.globalAudioRestrictionMode = mode
            advanceUntilIdle()
            audioRestrictionController.addListener(listener1)
            audioRestrictionController.addListener(listener2)
            advanceUntilIdle()

            verify(listener1, times(1)).onCameraAudioRestrictionUpdated(mode)
            verify(listener2, times(1)).onCameraAudioRestrictionUpdated(mode)
        }

    @Test
    fun removedListener_noLongerUpdated() =
        testScope.runTest {
            val mode = AUDIO_RESTRICTION_VIBRATION
            val audioRestrictionController =
                AudioRestrictionControllerImpl(threads, cameraPipeLifetime)
            audioRestrictionController.addListener(listener1)
            audioRestrictionController.addListener(listener2)
            audioRestrictionController.removeListener(listener1)
            advanceUntilIdle()

            audioRestrictionController.updateCameraGraphAudioRestrictionMode(cameraGraph1, mode)
            advanceUntilIdle()

            verify(listener1, times(0)).onCameraAudioRestrictionUpdated(mode)
            verify(listener2, times(1)).onCameraAudioRestrictionUpdated(mode)
        }

    @Test
    @Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
    fun belowRBuild_addListenerNoOp() =
        testScope.runTest {
            val mode = AUDIO_RESTRICTION_VIBRATION
            val audioRestrictionController =
                AudioRestrictionControllerImpl(threads, cameraPipeLifetime)
            audioRestrictionController.addListener(listener1)

            audioRestrictionController.updateCameraGraphAudioRestrictionMode(cameraGraph1, mode)
            advanceUntilIdle()

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                verify(listener1, times(0)).onCameraAudioRestrictionUpdated(mode)
            } else {
                verify(listener1, times(1)).onCameraAudioRestrictionUpdated(mode)
            }
        }
}
