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
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
class AudioRestrictionControllerImplTest {
    private val cameraGraph1: CameraGraph = mock()
    private val cameraGraph2: CameraGraph = mock()
    private val listener1: AudioRestrictionController.Listener = mock()
    private val listener2: AudioRestrictionController.Listener = mock()

    @Test
    fun setAudioRestrictionMode_ListenerUpdatedToHighestMode() {
        val audioRestrictionController = AudioRestrictionControllerImpl()
        audioRestrictionController.addListener(listener1)
        audioRestrictionController.addListener(listener2)

        audioRestrictionController.updateCameraGraphAudioRestrictionMode(
            cameraGraph1,
            AUDIO_RESTRICTION_VIBRATION
        )

        verify(listener1, times(1)).onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION)
        verify(listener2, times(1)).onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION)

        audioRestrictionController.updateCameraGraphAudioRestrictionMode(
            cameraGraph2,
            AUDIO_RESTRICTION_VIBRATION_SOUND
        )

        verify(listener1, times(1))
            .onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION_SOUND)
        verify(listener2, times(1))
            .onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION_SOUND)
    }

    @Test
    fun setGlobalAudioRestrictionMode_ListenerUpdatedToHighestMode() {
        val audioRestrictionController = AudioRestrictionControllerImpl()
        audioRestrictionController.addListener(listener1)
        audioRestrictionController.addListener(listener2)

        audioRestrictionController.updateCameraGraphAudioRestrictionMode(
            cameraGraph1,
            AUDIO_RESTRICTION_VIBRATION
        )

        verify(listener1, times(1)).onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION)
        verify(listener2, times(1)).onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION)

        audioRestrictionController.globalAudioRestrictionMode = AUDIO_RESTRICTION_VIBRATION_SOUND

        verify(listener1, times(1))
            .onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION_SOUND)
        verify(listener2, times(1))
            .onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION_SOUND)
    }

    @Test
    fun setAudioRestrictionMode_lowerModeNotOverrideHigherMode() {
        val audioRestrictionController = AudioRestrictionControllerImpl()
        audioRestrictionController.addListener(listener1)

        audioRestrictionController.updateCameraGraphAudioRestrictionMode(
            cameraGraph1,
            AUDIO_RESTRICTION_VIBRATION_SOUND
        )
        audioRestrictionController.updateCameraGraphAudioRestrictionMode(
            cameraGraph2,
            AUDIO_RESTRICTION_VIBRATION
        )

        // Whenever a setter method is called, an update should be called on the listener
        verify(listener1, times(2))
            .onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION_SOUND)
        verify(listener1, never()).onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION)
    }

    @Test
    fun setGlobalAudioRestrictionMode_lowerModeNotOverrideHigherMode() {
        val audioRestrictionController = AudioRestrictionControllerImpl()
        audioRestrictionController.addListener(listener1)

        audioRestrictionController.updateCameraGraphAudioRestrictionMode(
            cameraGraph1,
            AUDIO_RESTRICTION_VIBRATION_SOUND
        )
        audioRestrictionController.globalAudioRestrictionMode = AUDIO_RESTRICTION_VIBRATION

        // Whenever a setter method is called, an update should be called on the listener
        verify(listener1, times(2))
            .onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION_SOUND)
        verify(listener1, never()).onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION)
    }

    @Test
    fun removeCameraGraphAudioRestriction_associatedModeUpdated() {
        val audioRestrictionController = AudioRestrictionControllerImpl()
        audioRestrictionController.addListener(listener1)

        audioRestrictionController.updateCameraGraphAudioRestrictionMode(
            cameraGraph1,
            AUDIO_RESTRICTION_VIBRATION_SOUND
        )
        audioRestrictionController.updateCameraGraphAudioRestrictionMode(
            cameraGraph2,
            AUDIO_RESTRICTION_VIBRATION
        )

        verify(listener1, times(2))
            .onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION_SOUND)

        audioRestrictionController.removeCameraGraph(cameraGraph1)

        verify(listener1, times(1)).onCameraAudioRestrictionUpdated(AUDIO_RESTRICTION_VIBRATION)
    }

    @Test
    fun addListenerAfterUpdateMode_newListenerUpdated() {
        val mode = AUDIO_RESTRICTION_VIBRATION
        val audioRestrictionController = AudioRestrictionControllerImpl()
        audioRestrictionController.addListener(listener1)

        audioRestrictionController.updateCameraGraphAudioRestrictionMode(cameraGraph1, mode)
        audioRestrictionController.addListener(listener2)

        verify(listener1, times(1)).onCameraAudioRestrictionUpdated(mode)
        verify(listener2, times(1)).onCameraAudioRestrictionUpdated(mode)
    }

    @Test
    fun setRestrictionBeforeAddingListener_listenerSetToUpdatedMode() {
        val mode = AUDIO_RESTRICTION_VIBRATION
        val audioRestrictionController = AudioRestrictionControllerImpl()

        audioRestrictionController.globalAudioRestrictionMode = mode
        audioRestrictionController.addListener(listener1)
        audioRestrictionController.addListener(listener2)

        verify(listener1, times(1)).onCameraAudioRestrictionUpdated(mode)
        verify(listener2, times(1)).onCameraAudioRestrictionUpdated(mode)
    }

    @Test
    fun removedListener_noLongerUpdated() {
        val mode = AUDIO_RESTRICTION_VIBRATION
        val audioRestrictionController = AudioRestrictionControllerImpl()
        audioRestrictionController.addListener(listener1)
        audioRestrictionController.addListener(listener2)
        audioRestrictionController.removeListener(listener1)

        audioRestrictionController.updateCameraGraphAudioRestrictionMode(cameraGraph1, mode)

        verify(listener1, times(0)).onCameraAudioRestrictionUpdated(mode)
        verify(listener2, times(1)).onCameraAudioRestrictionUpdated(mode)
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
    fun belowRBuild_addListenerNoOp() {
        val mode = AUDIO_RESTRICTION_VIBRATION
        val audioRestrictionController = AudioRestrictionControllerImpl()
        audioRestrictionController.addListener(listener1)

        audioRestrictionController.updateCameraGraphAudioRestrictionMode(cameraGraph1, mode)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            verify(listener1, times(0)).onCameraAudioRestrictionUpdated(mode)
        } else {
            verify(listener1, times(1)).onCameraAudioRestrictionUpdated(mode)
        }
    }
}
