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

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.camera.integration.uiwidgets.rotations.UnlockedOrientationActivity.Companion.mCreated
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
@LargeTest
class ImageCaptureUnlockedOrientationTest(
    private val lensFacing: Int,
    private val orientation: Int,
    private val captureMode: Int
) : ImageCaptureBaseTest<UnlockedOrientationActivity>() {

    companion object {
        val ORIENTATION_MAP = hashMapOf(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT to Configuration.ORIENTATION_PORTRAIT,
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT to Configuration.ORIENTATION_PORTRAIT,
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE to Configuration.ORIENTATION_LANDSCAPE,
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE to Configuration.ORIENTATION_LANDSCAPE
        )

        @JvmStatic
        @Parameterized.Parameters(name = "lensFacing={0}, orientation={1}, captureMode={2}")
        fun data() = mutableListOf<Array<Any?>>().apply {
            lensFacing.forEach { lens ->
                ORIENTATION_MAP.keys.forEach { orientation ->
                    captureModes.forEach { mode ->
                        add(arrayOf(lens, orientation, mode))
                    }
                }
            }
        }
    }

    @get:Rule
    val mCameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(*CameraActivity.PERMISSIONS)

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
        verifyRotation<UnlockedOrientationActivity>(lensFacing, captureMode) {
            if (rotate(orientation)) {

                // Wait for the rotation to occur
                waitForRotation()
            }
        }
    }

    private fun ActivityScenario<UnlockedOrientationActivity>.rotate(orientation: Int): Boolean {
        val currentOrientation = withActivity { resources.configuration.orientation }
        val didRotate = ORIENTATION_MAP[orientation] != currentOrientation
        mCreated = Semaphore(0)
        onActivity { activity ->
            activity.requestedOrientation = orientation
        }
        return didRotate
    }

    private fun waitForRotation() {
        assertThat(mCreated.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
    }
}
