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

import android.app.Instrumentation;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Rational;

import androidx.camera.core.AppConfig;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.SensorOrientedMeteringPointFactory;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.lifecycle.LifecycleOwner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

public final class SensorOrientedMeteringPointFactoryTest {
    private static final float WIDTH = 480;
    private static final float HEIGHT = 640;
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private LifecycleOwner mLifecycle;
    SensorOrientedMeteringPointFactory mPointFactory;
    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        AppConfig config = Camera2AppConfig.create(context);

        CameraX.init(context, config);
        mLifecycle = new FakeLifecycleOwner();
        mPointFactory = new SensorOrientedMeteringPointFactory(WIDTH, HEIGHT);
    }

    @Test
    public void defaultWeightAndAreaSize() {
        MeteringPoint point = mPointFactory.createPoint(0, 0);
        assertThat(point.getSize()).isEqualTo(MeteringPointFactory.DEFAULT_AREASIZE);
        assertThat(point.getWeight()).isEqualTo(MeteringPointFactory.DEFAULT_WEIGHT);
        assertThat(point.getFOVAspectRatio()).isNull();
    }

    @Test
    public void createPointWithValidWeightAndAreaSize() {
        final float areaSize = 0.2f;
        final float weight = 0.5f;
        MeteringPoint point = mPointFactory.createPoint(0, 0, areaSize, weight);
        assertThat(point.getSize()).isEqualTo(areaSize);
        assertThat(point.getWeight()).isEqualTo(weight);
        assertThat(point.getFOVAspectRatio()).isNull();
    }

    @Test
    public void createPointLeftTop_correctValueSet() {
        MeteringPoint meteringPoint = mPointFactory.createPoint(0f, 0f);
        assertThat(meteringPoint.getNormalizedCropRegionX()).isEqualTo(0f);
        assertThat(meteringPoint.getNormalizedCropRegionY()).isEqualTo(0f);
    }

    @Test
    public void createPointLeftBottom_correctValueSet() {
        MeteringPoint meteringPoint2 = mPointFactory.createPoint(0f, HEIGHT);
        assertThat(meteringPoint2.getNormalizedCropRegionX()).isEqualTo(0f);
        assertThat(meteringPoint2.getNormalizedCropRegionY()).isEqualTo(1f);
    }

    @Test
    public void createPointRightTop_correctValueSet() {
        MeteringPoint meteringPoint3 = mPointFactory.createPoint(WIDTH, 0f);
        assertThat(meteringPoint3.getNormalizedCropRegionX()).isEqualTo(1f);
        assertThat(meteringPoint3.getNormalizedCropRegionY()).isEqualTo(0f);
    }

    @Test
    public void createPointRightBottom_correctValueSet() {
        MeteringPoint meteringPoint4 = mPointFactory.createPoint(WIDTH, HEIGHT);
        assertThat(meteringPoint4.getNormalizedCropRegionX()).isEqualTo(1f);
        assertThat(meteringPoint4.getNormalizedCropRegionY()).isEqualTo(1f);
    }

    @Test
    public void createPointWithFoVUseCase_success() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraX.LensFacing.BACK));

        ImageAnalysisConfig imageAnalysisConfig =
                new ImageAnalysisConfig.Builder()
                        .setLensFacing(CameraX.LensFacing.BACK)
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setTargetName("ImageAnalysis")
                        .setCallbackHandler(new Handler(Looper.getMainLooper()))
                        .build();
        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                CameraX.bindToLifecycle(mLifecycle, imageAnalysis);
            }
        });

        SensorOrientedMeteringPointFactory factory = new SensorOrientedMeteringPointFactory(
                WIDTH, HEIGHT, imageAnalysis);
        MeteringPoint point = factory.createPoint(0f, 0f);
        assertThat(point.getFOVAspectRatio()).isEqualTo(new Rational(4, 3));
    }

    @Test(expected = IllegalStateException.class)
    public void createPointWithFoVUseCase_FailedNotBound() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraX.LensFacing.BACK));

        ImageAnalysisConfig imageAnalysisConfig =
                new ImageAnalysisConfig.Builder()
                        .setLensFacing(CameraX.LensFacing.BACK)
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setTargetName("ImageAnalysis")
                        .setCallbackHandler(new Handler(Looper.getMainLooper()))
                        .build();
        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);

        // This will throw IllegalStateException.
        SensorOrientedMeteringPointFactory factory = new SensorOrientedMeteringPointFactory(
                WIDTH, HEIGHT, imageAnalysis);
    }
}
