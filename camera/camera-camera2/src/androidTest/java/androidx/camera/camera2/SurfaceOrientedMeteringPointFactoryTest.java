/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.camera2;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.util.Rational;

import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.testing.CameraUtil;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SdkSuppress(minSdkVersion = 21)
public final class SurfaceOrientedMeteringPointFactoryTest {
    private static final float WIDTH = 480;
    private static final float HEIGHT = 640;
    SurfaceOrientedMeteringPointFactory mPointFactory;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        CameraXConfig config = Camera2Config.defaultConfig();

        CameraX.initialize(mContext, config);
        mPointFactory = new SurfaceOrientedMeteringPointFactory(WIDTH, HEIGHT);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        CameraX.shutdown().get(10000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void defaultAreaSize() {
        MeteringPoint point = mPointFactory.createPoint(0, 0);
        assertThat(point.getSize()).isEqualTo(MeteringPointFactory.getDefaultPointSize());
        assertThat(point.getSurfaceAspectRatio()).isNull();
    }

    @Test
    public void createPointWithValidAreaSize() {
        final float areaSize = 0.2f;
        MeteringPoint point = mPointFactory.createPoint(0, 0, areaSize);
        assertThat(point.getSize()).isEqualTo(areaSize);
        assertThat(point.getSurfaceAspectRatio()).isNull();
    }

    @Test
    public void createPointLeftTop_correctValueSet() {
        MeteringPoint meteringPoint = mPointFactory.createPoint(0f, 0f);
        assertThat(meteringPoint.getX()).isEqualTo(0f);
        assertThat(meteringPoint.getY()).isEqualTo(0f);
    }

    @Test
    public void createPointLeftBottom_correctValueSet() {
        MeteringPoint meteringPoint2 = mPointFactory.createPoint(0f, HEIGHT);
        assertThat(meteringPoint2.getX()).isEqualTo(0f);
        assertThat(meteringPoint2.getY()).isEqualTo(1f);
    }

    @Test
    public void createPointRightTop_correctValueSet() {
        MeteringPoint meteringPoint3 = mPointFactory.createPoint(WIDTH, 0f);
        assertThat(meteringPoint3.getX()).isEqualTo(1f);
        assertThat(meteringPoint3.getY()).isEqualTo(0f);
    }

    @Test
    public void createPointRightBottom_correctValueSet() {
        MeteringPoint meteringPoint4 = mPointFactory.createPoint(WIDTH, HEIGHT);
        assertThat(meteringPoint4.getX()).isEqualTo(1f);
        assertThat(meteringPoint4.getY()).isEqualTo(1f);
    }

    @Test
    public void createPointWithFoVUseCase_success() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK));

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetName("ImageAnalysis")
                .build();
        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(
                        CameraSelector.LENS_FACING_BACK).build();
        CameraUseCaseAdapter camera = CameraUtil.createCameraAndAttachUseCase(mContext,
                cameraSelector, imageAnalysis);

        SurfaceOrientedMeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(
                WIDTH, HEIGHT, imageAnalysis);
        MeteringPoint point = factory.createPoint(0f, 0f);
        assertThat(point.getSurfaceAspectRatio()).isEqualTo(new Rational(4, 3));

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                //TODO: The removeUseCases() call might be removed after clarifying the
                // abortCaptures() issue in b/162314023.
                camera.removeUseCases(camera.getUseCases())
        );
    }

    @Test(expected = IllegalStateException.class)
    public void createPointWithFoVUseCase_FailedNotBound() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK));

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetName("ImageAnalysis")
                .build();

        // This will throw IllegalStateException.
        SurfaceOrientedMeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(
                WIDTH, HEIGHT, imageAnalysis);
    }
}
