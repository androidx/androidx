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

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import androidx.camera.view.transform.TransformTestUtils.createFakeImageProxy
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private const val FLOAT_ERROR = 1E-4F

/**
 * Unit tests for [CoordinateTransform].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CoordinateTransformTest {

    @Test
    public fun mapPointF() {
        // Arrange: the target is source with a 90° rotation.
        val imageProxyTransformFactory = ImageProxyTransformFactory()
        imageProxyTransformFactory.isUsingRotationDegrees = true
        val source = imageProxyTransformFactory.getOutputTransform(
            createFakeImageProxy(3, 4, 0, Rect(0, 0, 3, 4))
        )
        val target = imageProxyTransformFactory.getOutputTransform(
            createFakeImageProxy(3, 4, 90, Rect(0, 0, 3, 4))
        )

        // Act.
        val transform = CoordinateTransform(source, target)

        // Assert: top-left corner is mapped to top-right.
        val point = PointF(0F, 0F)
        transform.mapPoint(point)
        assertThat(point.x).isWithin(FLOAT_ERROR).of(4F)
        assertThat(point.y).isWithin(FLOAT_ERROR).of(0F)
    }

    @Test
    public fun mapRect() {
        // Arrange: the target is source with a 90° rotation.
        val imageProxyTransformFactory = ImageProxyTransformFactory()
        imageProxyTransformFactory.isUsingRotationDegrees = true
        val source = imageProxyTransformFactory.getOutputTransform(
            createFakeImageProxy(3, 4, 0, Rect(0, 0, 3, 4))
        )
        val target = imageProxyTransformFactory.getOutputTransform(
            createFakeImageProxy(3, 4, 90, Rect(0, 0, 3, 4))
        )

        // Act.
        val transform = CoordinateTransform(source, target)

        // Assert: the 3x4 rect is mapped to a 4x3 rect.
        val rect = RectF(0F, 0F, 3F, 4F)
        transform.mapRect(rect)
        assertThat(rect.left).isWithin(FLOAT_ERROR).of(0F)
        assertThat(rect.top).isWithin(FLOAT_ERROR).of(0F)
        assertThat(rect.right).isWithin(FLOAT_ERROR).of(4F)
        assertThat(rect.bottom).isWithin(FLOAT_ERROR).of(3F)
    }

    @Test
    public fun sameSourceAndTarget_getsIdentityMatrix() {
        // Arrange.
        val imageProxyTransformFactory = ImageProxyTransformFactory()
        val imageProxy = imageProxyTransformFactory.getOutputTransform(
            createFakeImageProxy(3, 4, 0, Rect(0, 0, 3, 4))
        )

        // Act: create a transform with the same source and target.
        val transform = CoordinateTransform(imageProxy, imageProxy)

        // Assert: the result transform is a no-op.
        val matrix = Matrix()
        // Set an arbitrary transform so it's no longer identity matrix.
        matrix.setRotate(90f)
        assertThat(matrix.isIdentity).isFalse()
        transform.transform(matrix)
        assertThat(matrix.isIdentity).isTrue()
    }

    @Test
    public fun scaleImageProxy() {
        // Arrange: create 2 ImageProxy with the only difference being 10x scale.
        val imageProxyTransformFactory = ImageProxyTransformFactory()
        val source = imageProxyTransformFactory.getOutputTransform(
            createFakeImageProxy(3, 4, 0, Rect(0, 0, 3, 4))
        )
        val target = imageProxyTransformFactory.getOutputTransform(
            createFakeImageProxy(30, 40, 0, Rect(0, 0, 30, 40))
        )

        // Act.
        val coordinateTransform = CoordinateTransform(source, target)

        // Assert: the mapping is scaled.
        val point = floatArrayOf(3f, 4f)
        coordinateTransform.mapPoints(point)
        assertThat(point).isEqualTo(floatArrayOf(30f, 40f))
    }

    @Test
    public fun scaleAndRotateImageProxy() {
        // Arrange: create 2 ImageProxy with different scale and rotation.
        val imageProxyTransformFactory = ImageProxyTransformFactory()
        imageProxyTransformFactory.isUsingRotationDegrees = true
        val source = imageProxyTransformFactory.getOutputTransform(
            createFakeImageProxy(3, 4, 270, Rect(0, 0, 3, 4))
        )
        val target = imageProxyTransformFactory.getOutputTransform(
            createFakeImageProxy(30, 40, 90, Rect(0, 0, 30, 40))
        )

        // Act.
        val coordinateTransform = CoordinateTransform(source, target)

        // Assert.
        val point = floatArrayOf(0f, 0f)
        coordinateTransform.mapPoints(point)
        assertThat(point).isEqualTo(floatArrayOf(40f, 30f))
    }

    @Test
    public fun withViewPortWithoutCropRect() {
        // Arrange: create 2 ImageProxy that have crop rect, but the coordinates do not respect the
        // crop rect. (MLKit scenario).
        val imageProxyTransformFactory = ImageProxyTransformFactory()
        imageProxyTransformFactory.isUsingRotationDegrees = true
        val source = imageProxyTransformFactory.getOutputTransform(
            createFakeImageProxy(16, 12, 0, Rect(2, 2, 10, 8))
        )
        val target = imageProxyTransformFactory.getOutputTransform(
            createFakeImageProxy(16, 12, 0, Rect(8, 6, 16, 12))
        )

        // Act.
        val coordinateTransform = CoordinateTransform(source, target)

        // Assert.
        val point = floatArrayOf(10f, 8f)
        coordinateTransform.mapPoints(point)
        assertThat(point).isEqualTo(floatArrayOf(16f, 12f))
    }

    @Test
    public fun withViewPortAndCropRect() {
        // Arrange: create 2 ImageProxy that have crop rect, and the coordinates respect the crop
        // rect.
        val imageProxyTransformFactory = ImageProxyTransformFactory()
        imageProxyTransformFactory.isUsingCropRect = true
        val source = imageProxyTransformFactory.getOutputTransform(
            createFakeImageProxy(
                16, 12, 0, Rect(2, 2, 10, 8)
            )
        )
        val target = imageProxyTransformFactory.getOutputTransform(
            createFakeImageProxy(
                16, 12, 90, Rect(8, 6, 16, 12)
            )
        )

        // Act.
        val coordinateTransform = CoordinateTransform(source, target)

        // Assert.
        val point = floatArrayOf(8f, 6f)
        coordinateTransform.mapPoints(point)
        assertThat(point).isEqualTo(floatArrayOf(8f, 6f))
    }
}