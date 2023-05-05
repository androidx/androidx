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

package androidx.camera.integration.uiwidgets.rotations

import android.app.Instrumentation
import androidx.camera.core.CameraSelector
import androidx.camera.integration.uiwidgets.rotations.CameraActivity.Companion.IMAGE_CAPTURE_MODE_FILE
import androidx.camera.integration.uiwidgets.rotations.CameraActivity.Companion.IMAGE_CAPTURE_MODE_IN_MEMORY
import androidx.camera.integration.uiwidgets.rotations.CameraActivity.Companion.IMAGE_CAPTURE_MODE_MEDIA_STORE
import androidx.camera.integration.uiwidgets.rotations.CameraActivity.Companion.IMAGE_CAPTURE_MODE_OUTPUT_STREAM
import androidx.camera.testing.CoreAppTestUtil
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
class ImageCaptureUnlockedOrientationTest(
    private val lensFacing: Int,
    private val captureMode: Int,
    private val cameraXConfig: String,
    private val rotation: RotationUnlocked,
    private val testName: String
) : ImageCaptureBaseTest<UnlockedOrientationActivity>(cameraXConfig) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "cameraXConfig={2}, {4}")
        fun data() = mutableListOf<Array<Any?>>().apply {
            lensFacingList.forEach { lens ->
                captureModes.forEach { mode ->
                    cameraXConfigList.forEach { cameraXConfig ->
                        val lensName = if (lens == CameraSelector.LENS_FACING_BACK) {
                            "Back lens"
                        } else {
                            "Front lens"
                        }

                        val captureModeName = when (mode) {
                            IMAGE_CAPTURE_MODE_IN_MEMORY -> "In memory"
                            IMAGE_CAPTURE_MODE_FILE -> "File"
                            IMAGE_CAPTURE_MODE_OUTPUT_STREAM -> "Output stream"
                            IMAGE_CAPTURE_MODE_MEDIA_STORE -> "Media store"
                            else -> "Invalid capture mode"
                        }

                        add(
                            arrayOf(
                                lens, mode, cameraXConfig, RotationUnlocked.Natural,
                                "$lensName - $captureModeName - Natural"
                            )
                        )
                        add(
                            arrayOf(
                                lens, mode, cameraXConfig, RotationUnlocked.Left,
                                "$lensName - $captureModeName - Left"
                            )
                        )
                        add(
                            arrayOf(
                                lens, mode, cameraXConfig, RotationUnlocked.Right,
                                "$lensName - $captureModeName - Right"
                            )
                        )
                    }
                }
            }
        }
    }

    @Before
    fun before() {
        CoreAppTestUtil.assumeCompatibleDevice()
        setUp(lensFacing)
    }

    @After
    fun after() {
        tearDown()
    }

    @Test
    fun verifyRotation() {
        verifyRotation<UnlockedOrientationActivity>(lensFacing, captureMode, cameraXConfig) {
            if (rotation.shouldRotate) {
                rotateDeviceAndWait()
            }
        }
    }

    private fun rotateDeviceAndWait() {
        val monitor = Instrumentation.ActivityMonitor(
            UnlockedOrientationActivity::class.java.name,
            null,
            false
        )
        InstrumentationRegistry.getInstrumentation().addMonitor(monitor)

        // Rotate
        rotation.rotate(mDevice)

        // Wait for the activity to be recreated after rotation
        InstrumentationRegistry.getInstrumentation().waitForMonitorWithTimeout(
            monitor,
            2000L
        )
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
}
