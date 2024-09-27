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

package androidx.camera.view;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.util.LayoutDirection;
import android.util.Size;
import android.view.Surface;

import androidx.camera.core.SurfaceRequest;
import androidx.test.annotation.UiThreadTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/**
 * Unit test for {@link PreviewViewMeteringPointFactory}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class PreviewViewMeteringPointFactoryTest {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final Rect SENSOR_RECT = new Rect(0, 0, 4000, 3000);

    @UiThreadTest
    @Test
    public void transformationInfoNotSet_createsInvalidMeteringPoint() {
        // Arrange.
        PreviewViewMeteringPointFactory previewViewMeteringPointFactory =
                new PreviewViewMeteringPointFactory(new PreviewTransformation());
        previewViewMeteringPointFactory.setSensorRect(SENSOR_RECT);
        previewViewMeteringPointFactory.recalculate(new Size(WIDTH, HEIGHT), LayoutDirection.LTR);

        // Act.
        PointF meteringPoint = previewViewMeteringPointFactory.convertPoint(0F, 0F);

        // Assume.
        assertThat(meteringPoint).isEqualTo(PreviewViewMeteringPointFactory.INVALID_POINT);
    }

    @UiThreadTest
    @Test
    public void previewViewSizeIs0_createsInvalidMeteringPoint() {
        // Arrange.
        PreviewTransformation previewTransformation = new PreviewTransformation();
        previewTransformation.setTransformationInfo(
                SurfaceRequest.TransformationInfo.of(
                        new Rect(0, 0, WIDTH, HEIGHT),
                        0,
                        Surface.ROTATION_0,
                        /*hasCameraTransform=*/true,
                        /*sensorToBufferTransform=*/new Matrix(),
                        /*mirroring=*/false),
                new Size(WIDTH, HEIGHT),
                /*isFrontCamera=*/false);
        PreviewViewMeteringPointFactory previewViewMeteringPointFactory =
                new PreviewViewMeteringPointFactory(previewTransformation);
        previewViewMeteringPointFactory.setSensorRect(SENSOR_RECT);

        // Act.
        previewViewMeteringPointFactory.recalculate(new Size(0, 0), LayoutDirection.LTR);
        PointF meteringPoint = previewViewMeteringPointFactory.convertPoint(0F, 0F);

        // Assume.
        assertThat(meteringPoint).isEqualTo(PreviewViewMeteringPointFactory.INVALID_POINT);
    }

    @UiThreadTest
    @Test
    public void sensorRectNotSet_createsInvalidMeteringPoint() {
        // Arrange.
        PreviewTransformation previewTransformation = new PreviewTransformation();
        previewTransformation.setTransformationInfo(
                SurfaceRequest.TransformationInfo.of(
                        new Rect(0, 0, WIDTH, HEIGHT),
                        0,
                        Surface.ROTATION_0,
                        /*hasCameraTransform=*/true,
                        /*sensorToBufferTransform=*/new Matrix(),
                        /*mirroring=*/false),
                new Size(WIDTH, HEIGHT),
                /*isFrontCamera=*/false);
        PreviewViewMeteringPointFactory previewViewMeteringPointFactory =
                new PreviewViewMeteringPointFactory(previewTransformation);

        // Act.
        previewViewMeteringPointFactory.recalculate(new Size(WIDTH, WIDTH), LayoutDirection.LTR);
        PointF meteringPoint = previewViewMeteringPointFactory.convertPoint(0F, 0F);

        // Assume.
        assertThat(meteringPoint).isEqualTo(PreviewViewMeteringPointFactory.INVALID_POINT);
    }

    @UiThreadTest
    @Test
    public void canCreateValidMeteringPoint() {
        // Arrange.
        PreviewTransformation previewTransformation = new PreviewTransformation();
        previewTransformation.setTransformationInfo(
                SurfaceRequest.TransformationInfo.of(
                        new Rect(0, 0, WIDTH, HEIGHT),
                        /*rotationDegrees=*/0,
                        Surface.ROTATION_0,
                        /*hasCameraTransform=*/true,
                        /*sensorToBufferTransform=*/new Matrix(),
                        /*mirroring=*/false),
                new Size(WIDTH, HEIGHT),
                /*isFrontCamera=*/false);
        PreviewViewMeteringPointFactory previewViewMeteringPointFactory =
                new PreviewViewMeteringPointFactory(previewTransformation);
        previewViewMeteringPointFactory.setSensorRect(SENSOR_RECT);

        // Act.
        previewViewMeteringPointFactory.recalculate(new Size(WIDTH, HEIGHT), LayoutDirection.LTR);
        PointF meteringPoint = previewViewMeteringPointFactory.convertPoint(0F, 0F);

        // Assume not invalid.
        // The result can't be correct for unit test because Matrix is native code, but at least
        // it should not be INVALID_POINT.
        assertThat(meteringPoint).isNotEqualTo(PreviewViewMeteringPointFactory.INVALID_POINT);
    }
}
