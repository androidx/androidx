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

import androidx.annotation.RequiresApi
import androidx.camera.testing.impl.SurfaceUtil
import androidx.camera.viewfinder.surface.ImplementationMode
import androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Face
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 33) // Required for screenshot tests
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
                implementationMode = ImplementationMode.EMBEDDED
            )

        assertImplementationDrawsUpright(testParams)
    }

    @Test
    fun embeddedImplementationDrawsUpright_from180DegreeSource() = runBlocking {
        val testParams =
            ViewfinderTestParams(
                sourceRotation = 180,
                implementationMode = ImplementationMode.EMBEDDED
            )

        assertImplementationDrawsUpright(testParams)
    }

    @Test
    fun embeddedImplementationDrawsUpright_from90DegreeSource() = runBlocking {
        val testParams =
            ViewfinderTestParams(
                sourceRotation = 90,
                implementationMode = ImplementationMode.EMBEDDED
            )

        assertImplementationDrawsUpright(testParams)
    }

    @Test
    fun embeddedImplementationDrawsUpright_from270DegreeSource() = runBlocking {
        val testParams =
            ViewfinderTestParams(
                sourceRotation = 270,
                implementationMode = ImplementationMode.EMBEDDED
            )

        assertImplementationDrawsUpright(testParams)
    }

    @Ignore("b/338466761")
    @Test
    fun externalImplementationDrawsUpright_from0DegreeSource() = runBlocking {
        val testParams =
            ViewfinderTestParams(
                sourceRotation = 0,
                implementationMode = ImplementationMode.EXTERNAL
            )

        assertImplementationDrawsUpright(testParams)
    }

    @Ignore("b/338466761")
    @Test
    fun externalImplementationDrawsUpright_from180DegreeSource() = runBlocking {
        val testParams =
            ViewfinderTestParams(
                sourceRotation = 180,
                implementationMode = ImplementationMode.EXTERNAL
            )

        assertImplementationDrawsUpright(testParams)
    }

    @Ignore("Currently cannot draw rotated buffers to SurfaceView")
    @Test
    fun externalImplementationDrawsUpright_from90DegreeSource() = runBlocking {
        val testParams =
            ViewfinderTestParams(
                sourceRotation = 90,
                implementationMode = ImplementationMode.EXTERNAL
            )

        assertImplementationDrawsUpright(testParams)
    }

    @Ignore("Currently cannot draw rotated buffers to SurfaceView")
    @Test
    fun externalImplementationDrawsUpright_from270DegreeSource() = runBlocking {
        val testParams =
            ViewfinderTestParams(
                sourceRotation = 270,
                implementationMode = ImplementationMode.EXTERNAL
            )

        assertImplementationDrawsUpright(testParams)
    }

    private fun assertImplementationDrawsUpright(testParams: ViewfinderTestParams) {
        val surfaceRequest = ViewfinderSurfaceRequest.Builder(testParams.sourceResolution).build()
        composeTestRule.setContent {
            Viewfinder(
                modifier = Modifier.size(testParams.viewfinderSize).testTag(VIEWFINDER_TAG),
                surfaceRequest = surfaceRequest,
                transformationInfo = testParams.transformationInfo,
                implementationMode = testParams.implementationMode
            )

            DrawFaceToSurface(testParams = testParams, surfaceRequest = surfaceRequest)
        }

        composeTestRule
            .onNodeWithTag(VIEWFINDER_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "upright_face")
    }

    @RequiresApi(26)
    @Composable
    private fun DrawFaceToSurface(
        testParams: ViewfinderTestParams,
        surfaceRequest: ViewfinderSurfaceRequest
    ) {
        val imageVec = Icons.Outlined.Face
        val painter = rememberVectorPainter(image = imageVec)
        val density = LocalDensity.current
        LaunchedEffect(Unit) {
            val surface = surfaceRequest.getSurface()
            SurfaceUtil.setBuffersTransform(surface, testParams.sourceRotation.toTransformEnum())
            val resolution = testParams.sourceResolution
            val canvas = ComposeCanvas(surface.lockHardwareCanvas())
            try {
                CanvasDrawScope().draw(
                    density = density,
                    layoutDirection = LayoutDirection.Ltr,
                    canvas = canvas,
                    size = Size(resolution.width.toFloat(), resolution.height.toFloat())
                ) {
                    val rotation = testParams.sourceRotation
                    val iconSize = imageVec.calcFitSize(size, rotation, density)
                    drawRect(Color.Gray)
                    withTransform({
                        rotate(degrees = -rotation.toFloat())
                        translate(
                            left = (size.width - iconSize.width) / 2f,
                            top = (size.height - iconSize.height) / 2f
                        )
                    }) {
                        with(painter) { draw(iconSize) }
                    }
                }
            } finally {
                surface.unlockCanvasAndPost(canvas.nativeCanvas)
            }
        }
    }

    private fun ImageVector.calcFitSize(boundSize: Size, rotation: Int, density: Density): Size {
        val rotatedBoundSize =
            when (abs(rotation)) {
                90,
                270 -> boundSize.swapDimens()
                else -> boundSize
            }

        val defaultSize = with(density) { Size(defaultWidth.toPx(), defaultHeight.toPx()) }

        val scale = ContentScale.Fit.computeScaleFactor(defaultSize, rotatedBoundSize)

        return Size(defaultSize.width * scale.scaleX, defaultSize.height * scale.scaleY)
    }

    private fun Size.swapDimens(): Size = Size(height, width)

    private fun Int.toTransformEnum(): Int {
        return when (this) {
            0 -> SurfaceUtil.TRANSFORM_IDENTITY
            90 -> SurfaceUtil.TRANSFORM_ROTATE_90
            180 -> SurfaceUtil.TRANSFORM_ROTATE_180
            270 -> SurfaceUtil.TRANSFORM_ROTATE_270
            else ->
                throw IllegalArgumentException(
                    "Rotation value $this does not correspond to valid transform"
                )
        }
    }
}

private const val VIEWFINDER_TAG = "Viewfinder"
