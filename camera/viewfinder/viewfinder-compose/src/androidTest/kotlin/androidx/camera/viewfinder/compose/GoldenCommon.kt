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

import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.testing.impl.SurfaceUtil
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Face
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.MSSIMMatcher
import kotlin.math.abs

const val GOLDEN_CAMERA_VIEWFINDER_COMPOSE = "camera/viewfinder/viewfinder-compose"
private const val VIEWFINDER_TAG = "Viewfinder"

@RequiresApi(26)
fun drawAndAssertAgainstGolden(
    composeTestRule: ComposeContentTestRule,
    screenshotRule: AndroidXScreenshotTestRule,
    testParams: ViewfinderTestParams,
    goldenIdentifier: String,
) {
    val surfaceRequest =
        ViewfinderSurfaceRequest(
            width = testParams.sourceResolution.width,
            height = testParams.sourceResolution.height,
            implementationMode = testParams.implementationMode,
        )
    val coordinateTransformer = MutableCoordinateTransformer()
    composeTestRule.setContent {
        val faceIcon = Icons.Outlined.Face
        val facePainter = rememberVectorPainter(image = faceIcon)
        val density = LocalDensity.current
        val touchCoordinates = Offset(200f, 200f)
        Viewfinder(
            modifier = Modifier.size(testParams.viewfinderSize).testTag(VIEWFINDER_TAG),
            surfaceRequest = surfaceRequest,
            transformationInfo = testParams.transformationInfo,
            coordinateTransformer = coordinateTransformer,
            alignment = testParams.alignment,
            contentScale = testParams.contentScale,
        ) {
            onSurfaceSession {
                // Fill Viewfinder buffer with content
                drawFaceToSurface(
                    testParams = testParams,
                    surface = surface,
                    painter = facePainter,
                    density = density,
                    coordinateTransformer = coordinateTransformer,
                    touchCoordinates = touchCoordinates,
                )
            }
        }

        // Draw touch coordinate on top of Viewfinder
        val touchCoordIcon = Icons.Filled.Add
        val touchCoordPainter = rememberVectorPainter(image = touchCoordIcon)
        Canvas(modifier = Modifier.size(testParams.viewfinderSize)) {
            val imageSize =
                with(density) {
                    Size(touchCoordIcon.defaultWidth.toPx(), touchCoordIcon.defaultHeight.toPx())
                }
            withTransform({
                translate(
                    left = touchCoordinates.x - imageSize.width / 2f,
                    top = touchCoordinates.y - imageSize.height / 2f,
                )
            }) {
                with(touchCoordPainter) {
                    draw(size = imageSize, colorFilter = ColorFilter.tint(Color.Green))
                }
            }
        }
    }

    composeTestRule
        .onNodeWithTag(VIEWFINDER_TAG)
        .captureToImage()
        .assertAgainstGolden(
            rule = screenshotRule,
            goldenIdentifier = goldenIdentifier,
            // Tuned to find a 1px difference in mapped touch coordinates.
            // May need to split out touch coordinate mapping into its own
            // screenshot test if this becomes flaky.
            matcher = MSSIMMatcher(threshold = 0.9995),
        )
}

/** This emulates the camera sensor. */
@RequiresApi(26)
fun drawFaceToSurface(
    testParams: ViewfinderTestParams,
    surface: Surface,
    painter: VectorPainter,
    density: Density,
    coordinateTransformer: CoordinateTransformer,
    touchCoordinates: Offset?,
) {
    SurfaceUtil.setBuffersTransform(
        surface,
        toTransformEnum(
            sourceRotation = testParams.sourceRotation,
            horizontalMirror = testParams.isMirroredHorizontally,
            verticalMirror = testParams.isMirroredVertically,
        ),
    )
    val resolution = testParams.sourceResolution
    val canvas = ComposeCanvas(surface.lockHardwareCanvas())
    try {
        CanvasDrawScope().draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(resolution.width.toFloat(), resolution.height.toFloat()),
        ) {
            val rotation = testParams.sourceRotation
            val iconSize = painter.calcFitSize(size, rotation)
            val mirrorX =
                when (testParams.isMirroredHorizontally) {
                    true -> -1.0f
                    false -> 1.0f
                }
            val flipY =
                when (testParams.isMirroredVertically) {
                    true -> -1.0f
                    false -> 1.0f
                }

            drawRect(Color.Gray)

            // For drawing the face, we need to emulate how the real world
            // would project onto the sensor. So we must apply the reverse rotation
            // and mirroring.
            withTransform({
                scale(mirrorX, flipY)
                rotate(degrees = -rotation.toFloat())
                translate(
                    left = (size.width - iconSize.width) / 2f,
                    top = (size.height - iconSize.height) / 2f,
                )
            }) {
                with(painter) { draw(iconSize) }
            }

            // For drawing the touch coordinates, we are already in the "sensor"
            // coordinates. No need to apply any transformations.
            touchCoordinates?.let {
                with(coordinateTransformer) {
                    drawCircle(
                        radius = 25f,
                        color = Color.Red,
                        center = touchCoordinates.transform(),
                    )
                }
            }
        }
    } finally {
        surface.unlockCanvasAndPost(canvas.nativeCanvas)
    }
}

private fun VectorPainter.calcFitSize(boundSize: Size, rotation: Int): Size {
    val rotatedBoundSize =
        when (abs(rotation)) {
            90,
            270 -> boundSize.swapDimens()
            else -> boundSize
        }

    val defaultSize = intrinsicSize

    val scale = ContentScale.Fit.computeScaleFactor(defaultSize, rotatedBoundSize)

    return Size(defaultSize.width * scale.scaleX, defaultSize.height * scale.scaleY)
}

private fun Size.swapDimens(): Size = Size(height, width)

private fun toTransformEnum(
    sourceRotation: Int,
    horizontalMirror: Boolean,
    verticalMirror: Boolean,
): Int {
    val rotationTransform =
        when (sourceRotation) {
            0 -> SurfaceUtil.TRANSFORM_IDENTITY
            90 -> SurfaceUtil.TRANSFORM_ROTATE_90
            180 -> SurfaceUtil.TRANSFORM_ROTATE_180
            270 -> SurfaceUtil.TRANSFORM_ROTATE_270
            else ->
                throw IllegalArgumentException(
                    "Rotation value $sourceRotation does not correspond to valid transform"
                )
        }

    val horizontalMirrorTransform =
        when (horizontalMirror) {
            true -> SurfaceUtil.TRANSFORM_MIRROR_HORIZONTAL
            false -> SurfaceUtil.TRANSFORM_IDENTITY
        }

    val verticalMirrorTransform =
        when (verticalMirror) {
            true -> SurfaceUtil.TRANSFORM_MIRROR_VERTICAL
            false -> SurfaceUtil.TRANSFORM_IDENTITY
        }

    return (horizontalMirrorTransform or verticalMirrorTransform) xor rotationTransform
}
