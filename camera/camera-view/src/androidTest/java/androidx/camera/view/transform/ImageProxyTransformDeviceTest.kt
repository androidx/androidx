/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.view.transform

import android.graphics.Rect
import androidx.camera.view.transform.TransformTestUtils.createFakeImageProxy
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrument tests for [ImageProxyTransform]
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
public class ImageProxyTransformDeviceTest {

    @Test
    public fun rotateVerticesAndAlignToOrigin() {
        // Arrange: a 3x4 rect: (1,2) - (4,6)
        val vertices = floatArrayOf(1f, 2f, 4f, 2f, 4f, 6f, 1f, 6f)

        // Act.
        val rotatedVertices = ImageProxyTransform.Builder.getRotatedVertices(vertices, 90)

        // Assert: the rotated rect becomes 4x3 and aligned to the origin: (0,0) - (4,3)
        assertThat(rotatedVertices).isEqualTo(floatArrayOf(4f, 0f, 4f, 3f, 0f, 3f, 0f, 0f))
    }

    @Test
    public fun withoutRotationOrCropRect_scaled() {
        // Arrange: a 3x4 rect.
        val imageProxy = createFakeImageProxy(3, 4, Rect(0, 0, 3, 4))
        val transform = ImageProxyTransform.Builder(imageProxy).build()

        // Assert: The bottom-right of the normalized space (1, 1) mapped to (3, 4)
        val point = floatArrayOf(1f, 1f)
        transform.matrix.mapPoints(point)
        assertThat(point).isEqualTo(floatArrayOf(3f, 4f))
    }

    @Test
    public fun withRotation_scaledAndRotated() {
        // Arrange: a 3x4 rect with 90° rotation.
        val imageProxy = createFakeImageProxy(3, 4, Rect(0, 0, 3, 4))
        val transform = ImageProxyTransform.Builder(imageProxy).setRotationDegrees(90).build()

        // Assert: The bottom-right of the normalized space (1, 1) mapped to (0, 3)
        val point = floatArrayOf(1f, 1f)
        transform.matrix.mapPoints(point)
        assertThat(point).isEqualTo(floatArrayOf(0f, 3f))
    }

    @Test
    public fun withCropRect_cropped() {
        // Arrange: a 16x12 rect with a 8x12 crop rect (8,0)-(16,12).
        val cropRect = Rect(8, 0, 16, 12)
        val imageProxy = createFakeImageProxy(16, 12, cropRect)
        val transform = ImageProxyTransform.Builder(imageProxy).setCropRect(cropRect).build()

        // Assert: the center of the normalized space (0.5, 0.5) mapped to the center of the crop
        // rect (4,6).
        val point = floatArrayOf(0.5f, 0.5f)
        transform.matrix.mapPoints(point)
        assertThat(point).isEqualTo(floatArrayOf(4f, 6f))
    }

    @Test
    public fun withIgnoredCropRect() {
        // The ImageProxy has crop rect which is the viewport, but the user chooses to ignore it
        // (the MLKit scenario).
        // Arrange: a 16x12 rect with a 8x12 crop rect (8,0)-(16,12).
        val imageProxy = createFakeImageProxy(16, 12, Rect(8, 0, 16, 12))
        val transform = ImageProxyTransform.Builder(imageProxy).build()

        // Assert: the center of the normalized space (0.5, 0.5) mapped to the center of the crop
        // rect, but the coordinate system is based on the full buffer.
        val point = floatArrayOf(.5f, .5f)
        transform.matrix.mapPoints(point)
        assertThat(point).isEqualTo(floatArrayOf(12f, 6f))
    }

    @Test
    public fun rotationAndCrop() {
        // Arrange: crop rect with rotation.
        val cropRect = Rect(8, 0, 16, 12)
        val imageProxy = createFakeImageProxy(16, 12, cropRect)
        val transform = ImageProxyTransform.Builder(imageProxy)
            .setCropRect(cropRect)
            .setRotationDegrees(90)
            .build()

        // Assert: the center of the normalized space (0.5, 0.5) mapped to the center of the
        // rotated crop rect (4,6).
        val point = floatArrayOf(0.5f, 0.5f)
        transform.matrix.mapPoints(point)
        assertThat(point).isEqualTo(floatArrayOf(6f, 4f))
    }
}