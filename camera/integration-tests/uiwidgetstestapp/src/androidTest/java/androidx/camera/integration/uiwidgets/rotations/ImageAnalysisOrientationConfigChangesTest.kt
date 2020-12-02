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

import android.view.Surface
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
@LargeTest
class ImageAnalysisOrientationConfigChangesTest(
    private val lensFacing: Int,
    private val rotation: Int
) : ImageAnalysisBaseTest<OrientationConfigChangesOverriddenActivity>() {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "lensFacing={0}, rotation={1}")
        fun data() = mutableListOf<Array<Any?>>().apply {
            add(arrayOf(CameraSelector.LENS_FACING_BACK, Surface.ROTATION_0))
            add(arrayOf(CameraSelector.LENS_FACING_BACK, Surface.ROTATION_90))
            add(arrayOf(CameraSelector.LENS_FACING_BACK, Surface.ROTATION_180))
            add(arrayOf(CameraSelector.LENS_FACING_BACK, Surface.ROTATION_270))
            add(arrayOf(CameraSelector.LENS_FACING_FRONT, Surface.ROTATION_0))
            add(arrayOf(CameraSelector.LENS_FACING_FRONT, Surface.ROTATION_90))
            add(arrayOf(CameraSelector.LENS_FACING_FRONT, Surface.ROTATION_180))
            add(arrayOf(CameraSelector.LENS_FACING_FRONT, Surface.ROTATION_270))
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
        verifyRotation<OrientationConfigChangesOverriddenActivity>(lensFacing) {
            if (rotate(rotation)) {

                // Wait for the rotation to occur
                waitForRotation()
            }
        }
    }

    private fun ActivityScenario<OrientationConfigChangesOverriddenActivity>.rotate(rotation: Int):
        Boolean {
            val currentRotation = withActivity {
                val root = findViewById<View>(android.R.id.content)
                root.display.rotation
            }
            InstrumentationRegistry.getInstrumentation().uiAutomation.setRotation(rotation)
            return currentRotation != rotation
        }

    private fun ActivityScenario<OrientationConfigChangesOverriddenActivity>.waitForRotation() {
        val displayChanged = withActivity { mDisplayChanged }
        assertThat(displayChanged.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
    }
}
