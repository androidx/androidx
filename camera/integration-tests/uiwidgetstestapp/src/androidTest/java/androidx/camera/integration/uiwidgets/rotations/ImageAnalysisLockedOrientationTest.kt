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

import androidx.test.core.app.ActivityScenario
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
class ImageAnalysisLockedOrientationTest(
    private val lensFacing: Int,
    private val rotationDegrees: Int,
    private val cameraXConfig: String
) : ImageAnalysisBaseTest<LockedOrientationActivity>(cameraXConfig) {

    companion object {
        @JvmStatic private val rotationDegrees = arrayOf(0, 90, 180, 270)

        @JvmStatic
        @Parameterized.Parameters(name = "lensFacing={0}, rotationDegrees={1}, cameraXConfig={2}")
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                lensFacingList.forEach { lens ->
                    rotationDegrees.forEach { rotation ->
                        cameraXConfigList.forEach { cameraXConfig ->
                            add(arrayOf(lens, rotation, cameraXConfig))
                        }
                    }
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
    @SdkSuppress(maxSdkVersion = 33) // b/360867144: Module crashes on API34
    fun verifyRotation() {
        verifyRotation<LockedOrientationActivity>(lensFacing, cameraXConfig) {
            rotate(rotationDegrees)
        }
    }

    private fun ActivityScenario<LockedOrientationActivity>.rotate(rotationDegrees: Int) {
        onActivity { activity ->
            // OrientationEventListener is called whenever the sensor detects the slightest change
            // in the device's orientation. This causes flakiness in the test, since a new
            // orientation can be set between the moment `onOrientationChanged(rotationDegrees)` is
            // called, and the moment the final test assertion is done, which would change the
            // value of the imageCapture use case's target rotation. To work around this, the
            // orientationEventListener is disabled right before the rotation is performed.
            activity.mOrientationEventListener.disable()
            activity.mOrientationEventListener.onOrientationChanged(rotationDegrees)
        }
    }
}
