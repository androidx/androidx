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
import android.os.Build
import androidx.camera.view.transform.TransformTestUtils.createFakeImageProxy
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [ImageProxyTransformFactory]
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class ImageProxyTransformFactoryTest {

    @Test
    public fun setUseRotationDegrees_getterReturnsTrue() {
        val factory = ImageProxyTransformFactory()
        assertThat(factory.isUsingRotationDegrees).isFalse()
        factory.isUsingRotationDegrees = true
        assertThat(factory.isUsingRotationDegrees).isTrue()
    }

    @Test
    public fun setUseCropRect_getterReturnsTrue() {
        val factory = ImageProxyTransformFactory()
        assertThat(factory.isUsingCropRect).isFalse()
        factory.isUsingCropRect = true
        assertThat(factory.isUsingCropRect).isTrue()
    }

    @Test
    public fun withoutRotationOrCropRect_scaled() {
        // Arrange: a 3x4 rect.
        val imageProxy = createFakeImageProxy(3, 4, 90, Rect(0, 0, 3, 4))
        val imageProxyTransformFactory = ImageProxyTransformFactory()
        val transform = imageProxyTransformFactory.getOutputTransform(imageProxy)

        // Assert: The bottom-right of the normalized space (1, 1) mapped to (3, 4)
        val point = floatArrayOf(1f, 1f)
        transform.matrix.mapPoints(point)
        assertThat(point).isEqualTo(floatArrayOf(3f, 4f))
    }

    @Test
    public fun withRotation_scaledAndRotated() {
        // Arrange: a 3x4 rect with 90° rotation.
        // (the MLKit scenario).
        val imageProxyTransformFactory = ImageProxyTransformFactory()
        imageProxyTransformFactory.isUsingRotationDegrees = true
        val imageProxy = createFakeImageProxy(3, 4, 90, Rect(0, 0, 3, 4))
        val transform = imageProxyTransformFactory.getOutputTransform(imageProxy)

        // Assert: The bottom-right of the normalized space (1, 1) mapped to (0, 3)
        val point = floatArrayOf(1f, 1f)
        transform.matrix.mapPoints(point)
        assertThat(point).isEqualTo(floatArrayOf(0f, 3f))
    }

    @Test
    public fun withCropRect_cropped() {
        // Arrange: a 16x12 rect with a 8x12 crop rect (8,0)-(16,12).
        val imageProxyTransformFactory = ImageProxyTransformFactory()
        imageProxyTransformFactory.isUsingCropRect = true

        val imageProxy = createFakeImageProxy(16, 12, 90, Rect(8, 0, 16, 12))
        val transform = imageProxyTransformFactory.getOutputTransform(imageProxy)

        // Assert: the center of the normalized space (0, 0) mapped to the center of the crop
        // rect (4,6).
        val point = floatArrayOf(0f, 0f)
        transform.matrix.mapPoints(point)
        assertThat(point).isEqualTo(floatArrayOf(4f, 6f))
    }

    @Test
    public fun rotationAndCrop() {
        // Arrange: crop rect with rotation.
        val imageProxyTransformFactory = ImageProxyTransformFactory()
        imageProxyTransformFactory.isUsingRotationDegrees = true
        imageProxyTransformFactory.isUsingCropRect = true
        val imageProxy = createFakeImageProxy(16, 12, 90, Rect(8, 0, 16, 12))
        val transform = imageProxyTransformFactory.getOutputTransform(imageProxy)

        // Assert: the center of the normalized space (0, 0) mapped to the center of the
        // rotated crop rect (4,6).
        val point = floatArrayOf(0f, 0f)
        transform.matrix.mapPoints(point)
        assertThat(point).isEqualTo(floatArrayOf(6f, 4f))
    }
}