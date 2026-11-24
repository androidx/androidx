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

package androidx.camera.testing.impl

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import androidx.camera.core.CameraSelector
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class CameraUtilTest {

    @Test
    fun postTestCameraAvailability_continuesToNextLensFacingWhenPreviousMissing() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val frontCharacteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        shadowOf(frontCharacteristics).apply {
            set(CameraCharacteristics.LENS_FACING, CameraCharacteristics.LENS_FACING_FRONT)
            set(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES,
                intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE),
            )
        }
        // Register only a front camera; LENS_FACING_BACK is absent.
        shadowOf(cameraManager).addCamera("1", frontCharacteristics)
        ShadowLog.clear()

        val events = mutableListOf<String>()
        val rule =
            CameraUtil.PostTestCameraAvailability(
                listOf(CameraSelector.LENS_FACING_BACK, CameraSelector.LENS_FACING_FRONT),
                /* timeoutPerCameraMillis= */ 50,
            )

        rule
            .apply(
                object : Statement() {
                    override fun evaluate() {
                        events.add("testBody")
                    }
                },
                Description.EMPTY,
            )
            .evaluate()

        assertThat(events).containsExactly("testBody")
        val logs = ShadowLog.getLogsForTag("CameraUtil").map { it.msg }
        assertThat(logs.any { it.contains("Waiting for camera[1] to be available!") }).isTrue()
    }
}
