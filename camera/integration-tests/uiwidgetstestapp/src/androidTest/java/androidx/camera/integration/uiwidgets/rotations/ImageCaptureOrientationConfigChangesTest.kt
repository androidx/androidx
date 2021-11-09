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
class ImageCaptureOrientationConfigChangesTest(
    private val lensFacing: Int,
    private val rotation: Int,
    private val captureMode: Int
) : ImageCaptureBaseTest<OrientationConfigChangesOverriddenActivity>() {

    companion object {
        private val rotations = arrayOf(
            Surface.ROTATION_0,
            Surface.ROTATION_90,
            Surface.ROTATION_180,
            Surface.ROTATION_270
        )

        @JvmStatic
        @Parameterized.Parameters(name = "lensFacing={0}, rotation={1}, captureMode={2}")
        fun data() = mutableListOf<Array<Any?>>().apply {
            lensFacing.forEach { lens ->
                rotations.forEach { rotation ->
                    captureModes.forEach { mode ->
                        add(arrayOf(lens, rotation, mode))
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
    fun verifyRotation() {
        verifyRotation<OrientationConfigChangesOverriddenActivity>(
            lensFacing,
            captureMode
        ) {
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
