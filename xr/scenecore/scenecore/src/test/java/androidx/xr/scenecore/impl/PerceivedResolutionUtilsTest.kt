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

package androidx.xr.scenecore.impl

import androidx.xr.runtime.internal.CameraViewActivityPose
import androidx.xr.runtime.internal.Dimensions // runtime.internal.Dimensions
import androidx.xr.runtime.internal.PerceivedResolutionResult
import androidx.xr.runtime.internal.PixelDimensions // runtime.internal.PixelDimensions
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlin.math.atan
import kotlin.math.roundToInt
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PerceivedResolutionUtilsTest {

    private lateinit var mockCameraView: CameraViewActivityPose

    // Default camera properties
    private lateinit var cameraPose: Pose
    private lateinit var cameraFov: CameraViewActivityPose.Fov
    private lateinit var cameraDisplayResolution: PixelDimensions

    @Before
    fun setUp() {
        mockCameraView = mock()

        // Default camera setup: at origin, looking along -Z, 90deg HFOV, 90deg VFOV
        cameraPose = Pose(Vector3(0f, 0f, 0f), Quaternion.Identity)
        cameraFov =
            CameraViewActivityPose.Fov(
                atan(1.0f),
                atan(1.0f),
                atan(1.0f),
                atan(1.0f),
            ) // tan(angle) = 1 => 45 deg
        cameraDisplayResolution = PixelDimensions(1000, 1000) // 1000x1000 display

        whenever(mockCameraView.activitySpacePose).thenReturn(cameraPose)
        whenever(mockCameraView.fov).thenReturn(cameraFov)
        whenever(mockCameraView.displayResolutionInPixels).thenReturn(cameraDisplayResolution)
    }

    // --- Tests for getPerceivedResolutionCameraView ---

    @Test
    fun getPerceivedResolutionCameraView_leftEyeExists_returnsLeftEye() {
        val leftEyeCamera: CameraViewActivityPose = mock {
            on { cameraType } doReturn CameraViewActivityPose.CameraType.CAMERA_TYPE_LEFT_EYE
        }
        val rightEyeCamera: CameraViewActivityPose = mock {
            on { cameraType } doReturn CameraViewActivityPose.CameraType.CAMERA_TYPE_RIGHT_EYE
        }
        val entityManager = EntityManager()
        entityManager.addSystemSpaceActivityPose(leftEyeCamera)
        entityManager.addSystemSpaceActivityPose(rightEyeCamera)

        val result = getPerceivedResolutionCameraView(entityManager)
        assertThat(result).isEqualTo(leftEyeCamera)
    }

    @Test
    fun getPerceivedResolutionCameraView_onlyRightEyeExists_returnsNull() {
        val rightEyeCamera: CameraViewActivityPose = mock {
            on { cameraType } doReturn CameraViewActivityPose.CameraType.CAMERA_TYPE_RIGHT_EYE
        }
        val entityManager = EntityManager()
        entityManager.addSystemSpaceActivityPose(rightEyeCamera)

        val result = getPerceivedResolutionCameraView(entityManager)
        assertThat(result).isNull()
    }

    @Test
    fun getPerceivedResolutionCameraView_noCameraViews_returnsNull() {
        val entityManager = EntityManager()
        val result = getPerceivedResolutionCameraView(entityManager)
        assertThat(result).isNull()
    }

    @Test
    fun getPerceivedResolutionCameraView_noLeftEyeAmongOthers_returnsNull() {
        val rightEyeCamera: CameraViewActivityPose = mock {
            on { cameraType } doReturn CameraViewActivityPose.CameraType.CAMERA_TYPE_RIGHT_EYE
        }
        val unknownCamera: CameraViewActivityPose = mock {
            on { cameraType } doReturn CameraViewActivityPose.CameraType.CAMERA_TYPE_UNKNOWN
        }
        val entityManager = EntityManager()
        entityManager.addSystemSpaceActivityPose(rightEyeCamera)
        entityManager.addSystemSpaceActivityPose(unknownCamera)

        val result = getPerceivedResolutionCameraView(entityManager)
        assertThat(result).isNull()
    }

    // --- Tests for getDimensionsAndDistanceOfLargest3dBoxSurface ---

    @Test
    fun getDimensionsAndDistanceOfLargest3dBoxSurface_calculatesCorrectly() {
        val boxDimensions = Dimensions(width = 2f, height = 3f, depth = 1f) // Smallest is depth
        val boxPosition = Vector3(0f, 0f, -5f) // Box 5 units away along -Z
        // Camera is at (0,0,0)
        // Expected largest face: 3x2 (height x width)
        // Distance to center of box = 5f
        // Smallest dimension = 1f (depth)
        // Distance to largest face = 5f - (1f / 2) = 4.5f

        val result =
            getDimensionsAndDistanceOfLargest3dBoxSurface(
                mockCameraView,
                boxDimensions,
                boxPosition,
            )

        assertThat(result.width).isEqualTo(3f) // Largest dimension
        assertThat(result.height).isEqualTo(2f) // Second largest
        assertThat(result.depth)
            .isWithin(PERCEIVED_RESOLUTION_EPSILON)
            .of(4.5f) // Calculated distance
    }

    @Test
    fun getDimensionsAndDistanceOfLargest3dBoxSurface_allDimensionsEqual() {
        val boxDimensions = Dimensions(width = 2f, height = 2f, depth = 2f)
        val boxPosition = Vector3(0f, 0f, -10f)
        // Distance to center = 10f
        // Smallest dimension = 2f
        // Distance to largest face = 10f - (2f / 2) = 9f

        val result =
            getDimensionsAndDistanceOfLargest3dBoxSurface(
                mockCameraView,
                boxDimensions,
                boxPosition,
            )

        assertThat(result.width).isEqualTo(2f)
        assertThat(result.height).isEqualTo(2f)
        assertThat(result.depth).isWithin(PERCEIVED_RESOLUTION_EPSILON).of(9f)
    }

    @Test
    fun getDimensionsAndDistanceOfLargest3dBoxSurface_cameraNotAtOrigin() {
        whenever(mockCameraView.activitySpacePose)
            .thenReturn(Pose(Vector3(1f, 1f, 1f), Quaternion.Identity))
        val boxDimensions = Dimensions(width = 1f, height = 2f, depth = 3f) // Smallest is width
        val boxPosition =
            Vector3(1f, 1f, -2f) // Box is 3 units away from camera along -Z relative to camera
        // Distance to center of box = sqrt((1-1)^2 + (1-1)^2 + (1 - (-2))^2) = sqrt(0+0+9) = 3f
        // Smallest dimension = 1f
        // Distance to largest face = 3f - (1f / 2) = 2.5f

        val result =
            getDimensionsAndDistanceOfLargest3dBoxSurface(
                mockCameraView,
                boxDimensions,
                boxPosition,
            )

        assertThat(result.width).isEqualTo(3f) // Largest
        assertThat(result.height).isEqualTo(2f) // Second largest
        assertThat(result.depth).isWithin(PERCEIVED_RESOLUTION_EPSILON).of(2.5f)
    }

    // --- Tests for getPerceivedResolutionOfPanel ---

    @Test
    fun getPerceivedResolutionOfPanel_panelInFront_returnsSuccess() {
        val panelWidth = 1f
        val panelHeight = 1f
        val panelDistance = 1f // At 1m distance

        // With 90deg HFOV & VFOV (tan(angle)=1 for half-angles):
        // viewPlaneWidth = 1f * (1f + 1f) = 2f
        // viewPlaneHeight = 1f * (1f + 1f) = 2f
        // panelWidthRatio = 1f / 2f = 0.5f
        // panelHeightRatio = 1f / 2f = 0.5f
        // pixelWidth = 0.5f * 1000 = 500
        // pixelHeight = 0.5f * 1000 = 500

        val result =
            getPerceivedResolutionOfPanel(mockCameraView, panelWidth, panelHeight, panelDistance)

        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success::class.java)
        val successResult = result as PerceivedResolutionResult.Success
        assertThat(successResult.perceivedResolution.width).isEqualTo(500)
        assertThat(successResult.perceivedResolution.height).isEqualTo(500)
    }

    @Test
    fun getPerceivedResolutionOfPanel_panelTooClose_returnsEntityTooClose() {
        val panelWidth = 1f
        val panelHeight = 1f
        val panelDistance = PERCEIVED_RESOLUTION_EPSILON / 2f // Closer than epsilon

        val result =
            getPerceivedResolutionOfPanel(mockCameraView, panelWidth, panelHeight, panelDistance)
        assertThat(result).isInstanceOf(PerceivedResolutionResult.EntityTooClose::class.java)
    }

    @Test
    fun getPerceivedResolutionOfPanel_panelAtEpsilonDistance_returnsEntityTooClose() {
        val panelWidth = 1f
        val panelHeight = 1f
        val panelDistance = PERCEIVED_RESOLUTION_EPSILON

        val result =
            getPerceivedResolutionOfPanel(mockCameraView, panelWidth, panelHeight, panelDistance)
        assertThat(result).isInstanceOf(PerceivedResolutionResult.EntityTooClose::class.java)
    }

    @Test
    fun getPerceivedResolutionOfPanel_panelSlightlyBeyondEpsilon_calculates() {
        val panelWidth = 1f
        val panelHeight = 1f
        val panelDistance = PERCEIVED_RESOLUTION_EPSILON * 2f

        // viewPlaneWidth = (2*epsilon) * (1+1) = 4*epsilon
        // viewPlaneHeight = (2*epsilon) * (1+1) = 4*epsilon
        // panelWidthRatio = 1 / (4*epsilon)
        // panelHeightRatio = 1 / (4*epsilon)
        // pixelWidth = (1 / (4*epsilon)) * 1000
        // pixelHeight = (1 / (4*epsilon)) * 1000
        // This will be very large, but should calculate.
        val expectedPixelDim = ((1f / (4f * PERCEIVED_RESOLUTION_EPSILON)) * 1000f).roundToInt()

        val result =
            getPerceivedResolutionOfPanel(mockCameraView, panelWidth, panelHeight, panelDistance)

        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success::class.java)
        val successResult = result as PerceivedResolutionResult.Success
        assertThat(successResult.perceivedResolution.width).isEqualTo(expectedPixelDim)
        assertThat(successResult.perceivedResolution.height).isEqualTo(expectedPixelDim)
    }

    @Test
    fun getPerceivedResolutionOfPanel_zeroFov_returnsZeroPixels() {
        cameraFov = CameraViewActivityPose.Fov(0f, 0f, 0f, 0f)
        whenever(mockCameraView.fov).thenReturn(cameraFov)

        val panelWidth = 1f
        val panelHeight = 1f
        val panelDistance = 1f

        // viewPlaneWidth/Height will be 0, so ratios will be 0
        val result =
            getPerceivedResolutionOfPanel(mockCameraView, panelWidth, panelHeight, panelDistance)

        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success::class.java)
        val successResult = result as PerceivedResolutionResult.Success
        assertThat(successResult.perceivedResolution.width).isEqualTo(0)
        assertThat(successResult.perceivedResolution.height).isEqualTo(0)
    }

    @Test
    fun getPerceivedResolutionOfPanel_panelFillsExactlyViewFrustum() {
        val panelDistance = 2f
        // At 2m, viewPlaneWidth = 2*2 = 4m, viewPlaneHeight = 2*2 = 4m
        val panelWidth = 4f
        val panelHeight = 4f

        // Ratios should be 1.0
        // pixelWidth = 1.0 * 1000 = 1000
        // pixelHeight = 1.0 * 1000 = 1000

        val result =
            getPerceivedResolutionOfPanel(mockCameraView, panelWidth, panelHeight, panelDistance)

        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success::class.java)
        val successResult = result as PerceivedResolutionResult.Success
        assertThat(successResult.perceivedResolution.width).isEqualTo(1000)
        assertThat(successResult.perceivedResolution.height).isEqualTo(1000)
    }

    @Test
    fun getPerceivedResolutionOfPanel_panelExceedsViewFrustum_calculatesCorrectly() {
        val panelDistance = 2f
        // At 2m, with 90deg HFOV & VFOV (tan(half-angle)=1):
        // viewPlaneWidth = 2f * (tan(fov.angleLeft) + tan(fov.angleRight))
        //                = 2f * (1f + 1f) = 4f
        // viewPlaneHeight = 2f * (tan(fov.angleDown) + tan(fov.angleUp))
        //                 = 2f * (1f + 1f) = 4f

        // Panel dimensions larger than the view frustum at this distance
        val panelWidth = 8f // Twice the view plane width
        val panelHeight = 6f // 1.5 times the view plane height

        // Expected Ratios:
        // panelWidthRatio = 8f / 4f = 2.0f
        // panelHeightRatio = 6f / 4f = 1.5f

        // Expected Pixel Dimensions (cameraDisplayResolution is 1000x1000):
        // pixelWidth = 2.0f * 1000 = 2000
        // pixelHeight = 1.5f * 1000 = 1500
        val expectedPixelWidth = 2000
        val expectedPixelHeight = 1500

        val result =
            getPerceivedResolutionOfPanel(mockCameraView, panelWidth, panelHeight, panelDistance)

        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success::class.java)
        val successResult = result as PerceivedResolutionResult.Success
        assertThat(successResult.perceivedResolution.width).isEqualTo(expectedPixelWidth)
        assertThat(successResult.perceivedResolution.height).isEqualTo(expectedPixelHeight)
    }

    @Test
    fun getPerceivedResolutionOfPanel_nonFiniteFov_returnsInvalidCameraView() {
        // Test with one non-finite angle (Positive Infinity)
        cameraFov =
            CameraViewActivityPose.Fov(
                atan(1.0f),
                Float.POSITIVE_INFINITY, // Non-finite angle
                atan(1.0f),
                atan(1.0f),
            )
        whenever(mockCameraView.fov).thenReturn(cameraFov)

        val panelWidth = 1f
        val panelHeight = 1f
        val panelDistance = 1f

        var result =
            getPerceivedResolutionOfPanel(mockCameraView, panelWidth, panelHeight, panelDistance)
        assertThat(result).isInstanceOf(PerceivedResolutionResult.InvalidCameraView::class.java)

        // Test with another non-finite angle (NaN)
        cameraFov =
            CameraViewActivityPose.Fov(
                atan(1.0f),
                atan(1.0f),
                Float.NaN, // Non-finite angle
                atan(1.0f),
            )
        whenever(mockCameraView.fov).thenReturn(cameraFov)

        result =
            getPerceivedResolutionOfPanel(mockCameraView, panelWidth, panelHeight, panelDistance)
        assertThat(result).isInstanceOf(PerceivedResolutionResult.InvalidCameraView::class.java)
    }

    @Test
    fun getPerceivedResolutionOfPanel_nonFiniteDistance_returnsInvalidCameraView() {
        val panelWidth = 1f
        val panelHeight = 1f
        val panelDistance = Float.POSITIVE_INFINITY // Non-finite distance

        val result =
            getPerceivedResolutionOfPanel(mockCameraView, panelWidth, panelHeight, panelDistance)
        assertThat(result).isInstanceOf(PerceivedResolutionResult.InvalidCameraView::class.java)
    }

    @Test
    fun getPerceivedResolutionOfPanel_zeroDisplayResolution_returnsInvalidCameraView() {
        val panelWidth = 1f
        val panelHeight = 1f
        val panelDistance = 1f

        // Test with zero width in display resolution
        var zeroDisplayResolution = PixelDimensions(0, 1000)
        whenever(mockCameraView.displayResolutionInPixels).thenReturn(zeroDisplayResolution)

        var result =
            getPerceivedResolutionOfPanel(mockCameraView, panelWidth, panelHeight, panelDistance)
        assertThat(result).isInstanceOf(PerceivedResolutionResult.InvalidCameraView::class.java)

        // Test with zero height in display resolution
        zeroDisplayResolution = PixelDimensions(1000, 0)
        whenever(mockCameraView.displayResolutionInPixels).thenReturn(zeroDisplayResolution)

        result =
            getPerceivedResolutionOfPanel(mockCameraView, panelWidth, panelHeight, panelDistance)
        assertThat(result).isInstanceOf(PerceivedResolutionResult.InvalidCameraView::class.java)

        // Test with zero width and zero height in display resolution
        zeroDisplayResolution = PixelDimensions(0, 0)
        whenever(mockCameraView.displayResolutionInPixels).thenReturn(zeroDisplayResolution)

        result =
            getPerceivedResolutionOfPanel(mockCameraView, panelWidth, panelHeight, panelDistance)
        assertThat(result).isInstanceOf(PerceivedResolutionResult.InvalidCameraView::class.java)
    }

    // --- Tests for getPerceivedResolutionOf3DBox ---

    @Test
    fun getPerceivedResolutionOf3DBox_boxInFront_returnsSuccess() {
        // Box 2x3x1, at (0,0,-5). Largest face 3x2. Distance to face = 4.5f
        // This reuses the setup from
        // getDimensionsAndDistanceOfLargest3dBoxSurface_calculatesCorrectly
        val boxDimensions = Dimensions(width = 2f, height = 3f, depth = 1f)
        val boxPosition = Vector3(0f, 0f, -5f)

        // Largest face width = 3f, height = 2f, distance = 4.5f
        // viewPlaneWidth = 4.5f * (1+1) = 9f
        // viewPlaneHeight = 4.5f * (1+1) = 9f
        // panelWidthRatio = 3f / 9f = 1/3
        // panelHeightRatio = 2f / 9f
        // pixelWidth = (1/3) * 1000 = 333
        // pixelHeight = (2/9) * 1000 = 222
        val expectedPixelWidth = ((3f / (4.5f * 2f)) * 1000f).roundToInt() // 333
        val expectedPixelHeight = ((2f / (4.5f * 2f)) * 1000f).roundToInt() // 222

        val result = getPerceivedResolutionOf3DBox(mockCameraView, boxDimensions, boxPosition)

        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success::class.java)
        val successResult = result as PerceivedResolutionResult.Success
        assertThat(successResult.perceivedResolution.width).isEqualTo(expectedPixelWidth)
        assertThat(successResult.perceivedResolution.height).isEqualTo(expectedPixelHeight)
    }

    @Test
    fun getPerceivedResolutionOf3DBox_boxTooClose_returnsEntityTooClose() {
        // Box 2x3x1, at (0,0,-0.1f). Smallest dim = 1. dist to center = 0.1
        // Distance to largest face = 0.1 - (1/2) = -0.4f
        // This distance will be <= EPSILON for the panel calculation.
        val boxDimensions = Dimensions(width = 2f, height = 3f, depth = 1f)
        val boxPosition = Vector3(0f, 0f, -0.1f)

        val result = getPerceivedResolutionOf3DBox(mockCameraView, boxDimensions, boxPosition)
        assertThat(result).isInstanceOf(PerceivedResolutionResult.EntityTooClose::class.java)
    }

    @Test
    fun getPerceivedResolutionOf3DBox_boxFaceAtEpsilon_returnsEntityTooClose() {
        // Smallest dim = 1.0f. Half smallest = 0.5f
        // We want distanceToLargestFace = EPSILON
        // distanceToCenter - 0.5f = EPSILON  => distanceToCenter = EPSILON + 0.5f
        val distanceToCenter = PERCEIVED_RESOLUTION_EPSILON + 0.5f
        val boxDimensions =
            Dimensions(width = 2f, height = 3f, depth = 1f) // smallest is depth = 1f
        val boxPosition = Vector3(0f, 0f, -distanceToCenter)

        val result = getPerceivedResolutionOf3DBox(mockCameraView, boxDimensions, boxPosition)
        assertThat(result).isInstanceOf(PerceivedResolutionResult.EntityTooClose::class.java)
    }
}
