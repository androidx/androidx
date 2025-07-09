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

package androidx.camera.viewfinder.compose

import androidx.camera.viewfinder.core.ImplementationMode
import androidx.camera.viewfinder.core.TransformationInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [Viewfinder] with various source transforms via [TransformationInfo] */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@LargeTest
@RunWith(AndroidJUnit4::class)
class ViewfinderScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_CAMERA_VIEWFINDER_COMPOSE)

    @Test
    fun embeddedImplementationDrawsUpright_from0DegreeSource() = runBlocking {
        val testParams =
            ViewfinderTestParams(
                sourceRotation = 0,
                implementationMode = ImplementationMode.EMBEDDED,
            )

        drawUprightFaceAndAssert(testParams)
    }

    @Test
    fun embeddedImplementationDrawsUpright_from180DegreeSource() = runBlocking {
        val testParams =
            ViewfinderTestParams(
                sourceRotation = 180,
                implementationMode = ImplementationMode.EMBEDDED,
            )

        drawUprightFaceAndAssert(testParams)
    }

    @Test
    fun embeddedImplementationDrawsUpright_from90DegreeSource() = runBlocking {
        val testParams =
            ViewfinderTestParams(
                sourceRotation = 90,
                implementationMode = ImplementationMode.EMBEDDED,
            )

        drawUprightFaceAndAssert(testParams)
    }

    @Test
    fun embeddedImplementationDrawsUpright_from270DegreeSource() = runBlocking {
        val testParams =
            ViewfinderTestParams(
                sourceRotation = 270,
                implementationMode = ImplementationMode.EMBEDDED,
            )

        drawUprightFaceAndAssert(testParams)
    }

    @Test
    fun embeddedImplementationDrawsUpright_fromHorizontallyMirroredSource() = runBlocking {
        val testParams =
            ViewfinderTestParams(
                sourceRotation = 0,
                implementationMode = ImplementationMode.EMBEDDED,
                isMirroredHorizontally = true,
            )

        drawUprightFaceAndAssert(testParams)
    }

    @Test
    fun embeddedImplementationDrawsUpright_from90Degree_HorizontallyMirroredSource() = runBlocking {
        val testParams =
            ViewfinderTestParams(
                sourceRotation = 90,
                implementationMode = ImplementationMode.EMBEDDED,
                isMirroredHorizontally = true,
            )

        drawUprightFaceAndAssert(testParams)
    }

    @Test
    fun embeddedImplementationDrawsUpright_from180Degree_HorizontallyMirroredSource() =
        runBlocking {
            val testParams =
                ViewfinderTestParams(
                    sourceRotation = 180,
                    implementationMode = ImplementationMode.EMBEDDED,
                    isMirroredHorizontally = true,
                )

            drawUprightFaceAndAssert(testParams)
        }

    @Test
    fun embeddedImplementationDrawsUpright_from270Degree_HorizontallyMirroredSource() =
        runBlocking {
            val testParams =
                ViewfinderTestParams(
                    sourceRotation = 270,
                    implementationMode = ImplementationMode.EMBEDDED,
                    isMirroredHorizontally = true,
                )

            drawUprightFaceAndAssert(testParams)
        }

    @Test
    fun embeddedImplementationDrawsUpright_fromVerticallyMirroredSource() = runBlocking {
        val testParams =
            ViewfinderTestParams(
                sourceRotation = 0,
                implementationMode = ImplementationMode.EMBEDDED,
                isMirroredVertically = true,
            )

        drawUprightFaceAndAssert(testParams)
    }

    @Test
    fun embeddedImplementationDrawsUpright_from90Degree_VerticallyMirroredSource() = runBlocking {
        val testParams =
            ViewfinderTestParams(
                sourceRotation = 90,
                implementationMode = ImplementationMode.EMBEDDED,
                isMirroredVertically = true,
            )

        drawUprightFaceAndAssert(testParams)
    }

    @Test
    fun embeddedImplementationDrawsUpright_from180Degree_VerticallyMirroredSource() = runBlocking {
        val testParams =
            ViewfinderTestParams(
                sourceRotation = 180,
                implementationMode = ImplementationMode.EMBEDDED,
                isMirroredVertically = true,
            )

        drawUprightFaceAndAssert(testParams)
    }

    @Test
    fun embeddedImplementationDrawsUpright_from270Degree_VerticallyMirroredSource() = runBlocking {
        val testParams =
            ViewfinderTestParams(
                sourceRotation = 270,
                implementationMode = ImplementationMode.EMBEDDED,
                isMirroredVertically = true,
            )

        drawUprightFaceAndAssert(testParams)
    }

    @Test
    fun embeddedImplementationDrawsUpright_fromVerticallyAndHorizontallyMirroredSource() =
        runBlocking {
            val testParams =
                ViewfinderTestParams(
                    sourceRotation = 0,
                    implementationMode = ImplementationMode.EMBEDDED,
                    isMirroredHorizontally = true,
                    isMirroredVertically = true,
                )

            drawUprightFaceAndAssert(testParams)
        }

    @Test
    fun embeddedImplementationDrawsUpright_from90Degree_VerticallyAndHorizontallyMirroredSource() =
        runBlocking {
            val testParams =
                ViewfinderTestParams(
                    sourceRotation = 90,
                    implementationMode = ImplementationMode.EMBEDDED,
                    isMirroredHorizontally = true,
                    isMirroredVertically = true,
                )

            drawUprightFaceAndAssert(testParams)
        }

    @Test
    fun embeddedImplementationDrawsUpright_from180Degree_VerticallyAndHorizontallyMirroredSource() =
        runBlocking {
            val testParams =
                ViewfinderTestParams(
                    sourceRotation = 180,
                    implementationMode = ImplementationMode.EMBEDDED,
                    isMirroredHorizontally = true,
                    isMirroredVertically = true,
                )

            drawUprightFaceAndAssert(testParams)
        }

    @Test
    fun embeddedImplementationDrawsUpright_from270Degree_VerticallyAndHorizontallyMirroredSource() =
        runBlocking {
            val testParams =
                ViewfinderTestParams(
                    sourceRotation = 270,
                    implementationMode = ImplementationMode.EMBEDDED,
                    isMirroredHorizontally = true,
                    isMirroredVertically = true,
                )

            drawUprightFaceAndAssert(testParams)
        }

    @Ignore("b/338466761")
    @Test
    fun externalImplementationDrawsUpright_from0DegreeSource() = runBlocking {
        val testParams =
            ViewfinderTestParams(
                sourceRotation = 0,
                implementationMode = ImplementationMode.EXTERNAL,
            )

        drawUprightFaceAndAssert(testParams)
    }

    @Ignore("b/338466761")
    @Test
    fun externalImplementationDrawsUpright_from180DegreeSource() = runBlocking {
        val testParams =
            ViewfinderTestParams(
                sourceRotation = 180,
                implementationMode = ImplementationMode.EXTERNAL,
            )

        drawUprightFaceAndAssert(testParams)
    }

    @Ignore("Currently cannot draw rotated buffers to SurfaceView")
    @Test
    fun externalImplementationDrawsUpright_from90DegreeSource() = runBlocking {
        val testParams =
            ViewfinderTestParams(
                sourceRotation = 90,
                implementationMode = ImplementationMode.EXTERNAL,
            )

        drawUprightFaceAndAssert(testParams)
    }

    @Ignore("Currently cannot draw rotated buffers to SurfaceView")
    @Test
    fun externalImplementationDrawsUpright_from270DegreeSource() = runBlocking {
        val testParams =
            ViewfinderTestParams(
                sourceRotation = 270,
                implementationMode = ImplementationMode.EXTERNAL,
            )

        drawUprightFaceAndAssert(testParams)
    }

    fun drawUprightFaceAndAssert(testParams: ViewfinderTestParams) {
        drawAndAssertAgainstGolden(
            composeTestRule = composeTestRule,
            screenshotRule = screenshotRule,
            testParams = testParams,
            goldenIdentifier = "upright_face_with_mapped_touch_point",
        )
    }
}
