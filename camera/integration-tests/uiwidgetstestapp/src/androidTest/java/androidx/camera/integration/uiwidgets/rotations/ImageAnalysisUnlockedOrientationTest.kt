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
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.integration.uiwidgets.rotations.RotationUnlocked.Left
import androidx.camera.integration.uiwidgets.rotations.RotationUnlocked.Natural
import androidx.camera.integration.uiwidgets.rotations.RotationUnlocked.Right
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
class ImageAnalysisUnlockedOrientationTest(
    private val lensFacing: Int,
    private val cameraXConfig: String,
    private val rotation: RotationUnlocked,
    private val testName: String
) : ImageAnalysisBaseTest<UnlockedOrientationActivity>(cameraXConfig) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "cameraXConfig={1}, {3}")
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                cameraXConfigList.forEach { config ->
                    add(arrayOf(LENS_FACING_BACK, config, Natural, "Back lens - Natural"))
                    add(arrayOf(LENS_FACING_BACK, config, Left, "Back lens - Left"))
                    add(arrayOf(LENS_FACING_BACK, config, Right, "Back lens - Right"))
                    add(arrayOf(LENS_FACING_FRONT, config, Natural, "Front lens - Natural"))
                    add(arrayOf(LENS_FACING_FRONT, config, Left, "Front lens - Left"))
                    add(arrayOf(LENS_FACING_FRONT, config, Right, "Front lens - Right"))
                }
            }
    }

    @Before
    fun before() {
        setUp(lensFacing)
    }

    @After
    fun after() {
        tearDown()
    }

    @Test
    fun verifyRotation() {
        verifyRotation<UnlockedOrientationActivity>(lensFacing, cameraXConfig) {
            if (rotation.shouldRotate) {
                rotateDeviceAndWait()
            }
        }
    }

    private fun rotateDeviceAndWait() {
        val monitor =
            Instrumentation.ActivityMonitor(
                UnlockedOrientationActivity::class.java.name,
                null,
                false
            )
        InstrumentationRegistry.getInstrumentation().addMonitor(monitor)

        // Rotate
        rotation.rotate(device)

        // Wait for the activity to be recreated after rotation
        InstrumentationRegistry.getInstrumentation().waitForMonitorWithTimeout(monitor, 2000L)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
}
