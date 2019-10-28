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

package androidx.camera.camera2.impl;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Build;
import android.util.Rational;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.impl.Camera2CameraControl.CaptureResultListener;
import androidx.camera.camera2.impl.annotation.CameraExecutor;
import androidx.camera.core.CameraControlInternal;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.SensorOrientedMeteringPointFactory;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import com.google.common.collect.Sets;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.shadows.ShadowCameraManager;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@DoNotInstrument
public class FocusMeteringControlTest {
    private static final String CAMERA0_ID = "0";
    private static final String CAMERA1_ID = "1";
    private static final int SENSOR_WIDTH = 640;
    private static final int SENSOR_HEIGHT = 480;
    private static final int SENSOR_WIDTH2 = 1920;
    private static final int SENSOR_HEIGHT2 = 1080;

    private static final int AREA_WIDTH =
            (int) (MeteringPointFactory.DEFAULT_AREASIZE * SENSOR_WIDTH);
    private static final int AREA_HEIGHT =
            (int) (MeteringPointFactory.DEFAULT_AREASIZE * SENSOR_HEIGHT);
    private static final int AREA_WIDTH2 =
            (int) (MeteringPointFactory.DEFAULT_AREASIZE * SENSOR_WIDTH2);
    private static final int AREA_HEIGHT2 =
            (int) (MeteringPointFactory.DEFAULT_AREASIZE * SENSOR_HEIGHT2);

    private final SensorOrientedMeteringPointFactory mPointFactory =
            new SensorOrientedMeteringPointFactory(1, 1);
    private FocusMeteringControl mFocusMeteringControl;

    private final MeteringPoint mPoint1 = mPointFactory.createPoint(0, 0);
    private final MeteringPoint mPoint2 = mPointFactory.createPoint(0.0f, 1.0f);
    private final MeteringPoint mPoint3 = mPointFactory.createPoint(1.0f, 1.0f);

    private static final Rect M_RECT_1 = new Rect(0, 0, AREA_WIDTH / 2, AREA_HEIGHT / 2);
    private static final Rect M_RECT_2 = new Rect(0, SENSOR_HEIGHT - AREA_HEIGHT / 2,
            AREA_WIDTH / 2,
            SENSOR_HEIGHT);
    private static final Rect M_RECT_3 = new Rect(SENSOR_WIDTH - AREA_WIDTH / 2,
            SENSOR_HEIGHT - AREA_HEIGHT / 2,
            SENSOR_WIDTH, SENSOR_HEIGHT);

    private static final Rational PREVIEW_ASPECT_RATIO_4_X_3 = new Rational(4, 3);
    private Camera2CameraControl mCamera2CameraControl;
    @CameraExecutor
    private ExecutorService mCameraExecutor;

    @Before
    public void setUp() throws CameraAccessException {
        initCameras();
        mFocusMeteringControl = spy(initFocusMeteringControl(CAMERA0_ID));
        mFocusMeteringControl.setActive(true);
    }

    @After
    public void tearDown() {
        mCameraExecutor.shutdown();
    }

    private FocusMeteringControl initFocusMeteringControl(String cameraID) throws
            CameraAccessException {
        CameraManager cameraManager =
                (CameraManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE);

        CameraCharacteristics cameraCharacteristics =
                cameraManager.getCameraCharacteristics(
                        cameraID);

        CameraControlInternal.ControlUpdateCallback updateCallback = mock(
                CameraControlInternal.ControlUpdateCallback.class);

        mCameraExecutor = Executors.newSingleThreadExecutor();

        mCamera2CameraControl = spy(new Camera2CameraControl(
                cameraCharacteristics,
                CameraXExecutors.mainThreadExecutor(),
                mCameraExecutor,
                updateCallback));

        FocusMeteringControl focusMeteringControl = new FocusMeteringControl(mCamera2CameraControl,
                CameraXExecutors.mainThreadExecutor(), mCameraExecutor);
        focusMeteringControl.setActive(true);
        return focusMeteringControl;
    }

    private void initCameras() {
        // **** Camera 0 characteristics ****//
        CameraCharacteristics characteristics0 =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics0 = Shadow.extract(characteristics0);

        shadowCharacteristics0.set(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE,
                new Rect(0, 0, SENSOR_WIDTH, SENSOR_HEIGHT));
        shadowCharacteristics0.set(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES, new int[]{
                CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
                CaptureResult.CONTROL_AF_MODE_AUTO,
                CaptureResult.CONTROL_AF_MODE_OFF
        });
        shadowCharacteristics0.set(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES, new int[]{
                CaptureResult.CONTROL_AE_MODE_ON,
                CaptureResult.CONTROL_AE_MODE_ON_ALWAYS_FLASH,
                CaptureResult.CONTROL_AE_MODE_ON_AUTO_FLASH,
                CaptureResult.CONTROL_AE_MODE_OFF
        });
        shadowCharacteristics0.set(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES, new int[]{
                CaptureResult.CONTROL_AWB_MODE_AUTO,
                CaptureResult.CONTROL_AWB_MODE_OFF,
        });
        shadowCharacteristics0.set(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);

        // Add the camera to the camera service
        ((ShadowCameraManager)
                Shadow.extract(
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CAMERA_SERVICE)))
                .addCamera(CAMERA0_ID, characteristics0);

        // **** Camera 1 characteristics ****//
        CameraCharacteristics characteristics1 =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics1 = Shadow.extract(characteristics1);

        shadowCharacteristics1.set(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE,
                new Rect(0, 0, SENSOR_WIDTH2, SENSOR_HEIGHT2));
        shadowCharacteristics1.set(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3);

        // Add the camera to the camera service
        ((ShadowCameraManager)
                Shadow.extract(
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CAMERA_SERVICE)))
                .addCamera(CAMERA1_ID, characteristics1);
    }

    private MeteringRectangle[] getAfRects(FocusMeteringControl control) {
        Camera2Config.Builder configBuilder = new Camera2Config.Builder();
        control.addFocusMeteringOptions(configBuilder);
        Camera2Config config = configBuilder.build();

        return config.getCaptureRequestOption(CaptureRequest.CONTROL_AF_REGIONS,
                new MeteringRectangle[]{});
    }

    private MeteringRectangle[] getAeRects(FocusMeteringControl control) {
        Camera2Config.Builder configBuilder = new Camera2Config.Builder();
        control.addFocusMeteringOptions(configBuilder);
        Camera2Config config = configBuilder.build();

        return config.getCaptureRequestOption(CaptureRequest.CONTROL_AE_REGIONS,
                new MeteringRectangle[]{});
    }

    private MeteringRectangle[] getAwbRects(FocusMeteringControl control) {
        Camera2Config.Builder configBuilder = new Camera2Config.Builder();
        control.addFocusMeteringOptions(configBuilder);
        Camera2Config config = configBuilder.build();

        return config.getCaptureRequestOption(CaptureRequest.CONTROL_AWB_REGIONS,
                new MeteringRectangle[]{});
    }

    @Test
    public void startFocusAndMetering_defaultPoint_3ARectssAreCorrect() {
        mFocusMeteringControl.startFocusAndMetering(
                FocusMeteringAction.Builder.from(mPoint1).build(),
                PREVIEW_ASPECT_RATIO_4_X_3);

        MeteringRectangle[] afRects = getAfRects(mFocusMeteringControl);
        MeteringRectangle[] aeRects = getAeRects(mFocusMeteringControl);
        MeteringRectangle[] awbRects = getAwbRects(mFocusMeteringControl);

        assertThat(afRects.length).isEqualTo(1);
        assertThat(afRects[0].getRect()).isEqualTo(M_RECT_1);
        assertThat(aeRects.length).isEqualTo(1);
        assertThat(aeRects[0].getRect()).isEqualTo(M_RECT_1);
        assertThat(awbRects.length).isEqualTo(1);
        assertThat(awbRects[0].getRect()).isEqualTo(M_RECT_1);
    }

    @Test
    public void startFocusAndMetering_multiplePoint_3ARectsAreCorrect() {
        mFocusMeteringControl.startFocusAndMetering(
                FocusMeteringAction.Builder.from(mPoint1)
                        .addPoint(mPoint2)
                        .addPoint(mPoint3)
                        .build(), PREVIEW_ASPECT_RATIO_4_X_3);

        MeteringRectangle[] afRects = getAfRects(mFocusMeteringControl);
        MeteringRectangle[] aeRects = getAeRects(mFocusMeteringControl);
        MeteringRectangle[] awbRects = getAwbRects(mFocusMeteringControl);

        assertThat(afRects.length).isEqualTo(3);
        assertThat(afRects[0].getRect()).isEqualTo(M_RECT_1);
        assertThat(afRects[1].getRect()).isEqualTo(M_RECT_2);
        assertThat(afRects[2].getRect()).isEqualTo(M_RECT_3);

        assertThat(aeRects.length).isEqualTo(3);
        assertThat(aeRects[0].getRect()).isEqualTo(M_RECT_1);
        assertThat(aeRects[1].getRect()).isEqualTo(M_RECT_2);
        assertThat(aeRects[2].getRect()).isEqualTo(M_RECT_3);

        assertThat(awbRects.length).isEqualTo(3);
        assertThat(awbRects[0].getRect()).isEqualTo(M_RECT_1);
        assertThat(awbRects[1].getRect()).isEqualTo(M_RECT_2);
        assertThat(awbRects[2].getRect()).isEqualTo(M_RECT_3);
    }

    @Test
    public void startFocusAndMetering_multiplePointVariousModes() {
        mFocusMeteringControl.startFocusAndMetering(
                FocusMeteringAction.Builder.from(mPoint1, FocusMeteringAction.MeteringMode.AWB_ONLY)
                        .addPoint(mPoint2, FocusMeteringAction.MeteringMode.AF_AE)
                        .addPoint(mPoint3, FocusMeteringAction.MeteringMode.AF_AE_AWB)
                        .build(), PREVIEW_ASPECT_RATIO_4_X_3);

        MeteringRectangle[] afRects = getAfRects(mFocusMeteringControl);
        MeteringRectangle[] aeRects = getAeRects(mFocusMeteringControl);
        MeteringRectangle[] awbRects = getAwbRects(mFocusMeteringControl);

        assertThat(afRects.length).isEqualTo(2);
        assertThat(afRects[0].getRect()).isEqualTo(M_RECT_2);
        assertThat(afRects[1].getRect()).isEqualTo(M_RECT_3);

        assertThat(aeRects.length).isEqualTo(2);
        assertThat(aeRects[0].getRect()).isEqualTo(M_RECT_2);
        assertThat(aeRects[1].getRect()).isEqualTo(M_RECT_3);

        assertThat(awbRects.length).isEqualTo(2);
        assertThat(awbRects[0].getRect()).isEqualTo(M_RECT_1);
        assertThat(awbRects[1].getRect()).isEqualTo(M_RECT_3);
    }

    @Test
    public void startFocusAndMetering_multiplePointVariousModes2() {
        mFocusMeteringControl.startFocusAndMetering(
                FocusMeteringAction.Builder.from(mPoint1, FocusMeteringAction.MeteringMode.AF_ONLY)
                        .addPoint(mPoint2, FocusMeteringAction.MeteringMode.AWB_ONLY)
                        .addPoint(mPoint3, FocusMeteringAction.MeteringMode.AE_ONLY)
                        .build(), PREVIEW_ASPECT_RATIO_4_X_3);
        MeteringRectangle[] afRects = getAfRects(mFocusMeteringControl);
        MeteringRectangle[] aeRects = getAeRects(mFocusMeteringControl);
        MeteringRectangle[] awbRects = getAwbRects(mFocusMeteringControl);

        assertThat(afRects.length).isEqualTo(1);
        assertThat(afRects[0].getRect()).isEqualTo(M_RECT_1);

        assertThat(aeRects.length).isEqualTo(1);
        assertThat(aeRects[0].getRect()).isEqualTo(M_RECT_3);

        assertThat(awbRects.length).isEqualTo(1);
        assertThat(awbRects[0].getRect()).isEqualTo(M_RECT_2);
    }

    @Test
    public void cropRegionIsSet_resultBasedOnCropRegion() {
        final int cropWidth = 480;
        final int cropHeight = 360;
        Rect cropRect = new Rect(SENSOR_WIDTH / 2 - cropWidth / 2,
                SENSOR_HEIGHT / 2 - cropHeight / 2,
                SENSOR_WIDTH / 2 + cropWidth / 2, SENSOR_HEIGHT / 2 + cropHeight / 2);
        when(mCamera2CameraControl.getCropSensorRegion()).thenReturn(cropRect);

        MeteringPoint centorPt = mPointFactory.createPoint(0.5f, 0.5f);
        mFocusMeteringControl.startFocusAndMetering(
                FocusMeteringAction.Builder.from(centorPt).build(),
                PREVIEW_ASPECT_RATIO_4_X_3);
        MeteringRectangle[] afRects = getAfRects(mFocusMeteringControl);

        final int areaWidth = (int) (MeteringPointFactory.DEFAULT_AREASIZE * cropRect.width());
        final int areaHeight = (int) (MeteringPointFactory.DEFAULT_AREASIZE * cropRect.height());
        Rect adjustedRect = new Rect(cropRect.centerX() - areaWidth / 2,
                cropRect.centerY() - areaHeight / 2,
                cropRect.centerX() + areaWidth / 2,
                cropRect.centerY() + areaHeight / 2);

        assertThat(afRects[0].getRect()).isEqualTo(adjustedRect);
    }

    @Test
    public void previewFovAdjusted_16by9_to_4by3() {
        // use 16:9 preview aspect ratio
        Rational previewAspectRatio = new Rational(16, 9);
        mFocusMeteringControl.startFocusAndMetering(
                FocusMeteringAction.Builder.from(mPoint1).build(),
                previewAspectRatio);
        MeteringRectangle[] afRects = getAfRects(mFocusMeteringControl);


        Rect adjustedRect = new Rect(0, 60 - AREA_HEIGHT / 2, AREA_WIDTH / 2, 60 + AREA_HEIGHT / 2);
        assertThat(afRects[0].getRect()).isEqualTo(adjustedRect);
    }

    @Test
    public void previewFovAdjusted_4by3_to_16by9()
            throws CameraAccessException {
        //Camera1 sensor region is 16:9
        mFocusMeteringControl = initFocusMeteringControl(CAMERA1_ID);

        mFocusMeteringControl.startFocusAndMetering(
                FocusMeteringAction.Builder.from(mPoint1).build(),
                PREVIEW_ASPECT_RATIO_4_X_3);
        MeteringRectangle[] afRects = getAfRects(mFocusMeteringControl);

        Rect adjustedRect = new Rect(240 - AREA_WIDTH2 / 2, 0, 240 + AREA_WIDTH2 / 2,
                AREA_HEIGHT2 / 2);
        assertThat(afRects[0].getRect()).isEqualTo(adjustedRect);
    }

    @Test
    public void customFovAdjusted() {
        // 16:9 to 4:3
        ImageAnalysis imageAnalysis = Mockito.mock(ImageAnalysis.class);
        when(imageAnalysis.getAttachedSurfaceResolution(any())).thenReturn(
                new Size(1920, 1080));
        when(imageAnalysis.getAttachedCameraIds()).thenReturn(Sets.newHashSet(CAMERA0_ID));

        SensorOrientedMeteringPointFactory factory =
                new SensorOrientedMeteringPointFactory(1.0f, 1.0f, imageAnalysis);

        MeteringPoint point = factory.createPoint(0, 0);
        mFocusMeteringControl.startFocusAndMetering(FocusMeteringAction.Builder.from(point).build(),
                PREVIEW_ASPECT_RATIO_4_X_3);
        MeteringRectangle[] afRects = getAfRects(mFocusMeteringControl);


        Rect adjustedRect = new Rect(0, 60 - AREA_HEIGHT / 2, AREA_WIDTH / 2, 60 + AREA_HEIGHT / 2);
        assertThat(afRects[0].getRect()).isEqualTo(adjustedRect);
    }

    @Test
    public void weight_ConvertedCorrect() {
        MeteringPoint point1 = mPointFactory.createPoint(0, 0, 0.2f, 1.0f);
        MeteringPoint point2 = mPointFactory.createPoint(0, 0, 0.2f, 0.5f);
        MeteringPoint point3 = mPointFactory.createPoint(0, 0, 0.2f, 0.1f);

        mFocusMeteringControl.startFocusAndMetering(FocusMeteringAction.Builder.from(point1)
                .addPoint(point2)
                .addPoint(point3).build(), PREVIEW_ASPECT_RATIO_4_X_3);
        MeteringRectangle[] afRects = getAfRects(mFocusMeteringControl);

        assertThat(afRects.length).isEqualTo(3);
        assertThat(afRects[0].getMeteringWeight()).isEqualTo(
                (int) (MeteringRectangle.METERING_WEIGHT_MAX * 1.0f));
        assertThat(afRects[1].getMeteringWeight()).isEqualTo(
                (int) (MeteringRectangle.METERING_WEIGHT_MAX * 0.5f));
        assertThat(afRects[2].getMeteringWeight()).isEqualTo(
                (int) (MeteringRectangle.METERING_WEIGHT_MAX * 0.1f));
    }

    @Test
    public void invalidWeight_ConvertedCorrect() {
        MeteringPoint point1 = mPointFactory.createPoint(0, 0, 0.2f, 1.1f);
        MeteringPoint point2 = mPointFactory.createPoint(0, 0, 0.2f, -0.3f);
        MeteringPoint point3 = mPointFactory.createPoint(0, 0, 0.2f, 90000f);

        mFocusMeteringControl.startFocusAndMetering(FocusMeteringAction.Builder.from(point1)
                .addPoint(point2)
                .addPoint(point3).build(), PREVIEW_ASPECT_RATIO_4_X_3);
        MeteringRectangle[] afRects = getAfRects(mFocusMeteringControl);

        assertThat(afRects.length).isEqualTo(3);
        assertThat(afRects[0].getMeteringWeight()).isEqualTo(
                MeteringRectangle.METERING_WEIGHT_MAX);
        assertThat(afRects[1].getMeteringWeight()).isEqualTo(
                MeteringRectangle.METERING_WEIGHT_MIN);
        assertThat(afRects[2].getMeteringWeight()).isEqualTo(
                MeteringRectangle.METERING_WEIGHT_MAX);
    }

    @Test
    public void pointSize_ConvertedCorrect() {
        MeteringPoint point1 = mPointFactory.createPoint(0.5f, 0.5f, 1.0f, 1.0f);
        MeteringPoint point2 = mPointFactory.createPoint(0.5f, 0.5f, 0.5f, 1.0f);
        MeteringPoint point3 = mPointFactory.createPoint(0.5f, 0.5f, 0.1f, 1.0f);

        mFocusMeteringControl.startFocusAndMetering(FocusMeteringAction.Builder.from(point1)
                .addPoint(point2)
                .addPoint(point3).build(), PREVIEW_ASPECT_RATIO_4_X_3);
        MeteringRectangle[] afRects = getAfRects(mFocusMeteringControl);

        assertThat(afRects.length).isEqualTo(3);
        assertThat(afRects[0].getWidth()).isEqualTo(
                (int) (SENSOR_WIDTH * 1.0f));
        assertThat(afRects[0].getHeight()).isEqualTo(
                (int) (SENSOR_HEIGHT * 1.0f));

        assertThat(afRects[1].getWidth()).isEqualTo(
                (int) (SENSOR_WIDTH * 0.5f));
        assertThat(afRects[1].getHeight()).isEqualTo(
                (int) (SENSOR_HEIGHT * 0.5f));

        assertThat(afRects[2].getWidth()).isEqualTo(
                (int) (SENSOR_WIDTH * 0.1f));
        assertThat(afRects[2].getHeight()).isEqualTo(
                (int) (SENSOR_HEIGHT * 0.1f));
    }

    @Test
    public void withAFPoints_AFIsTriggered() {
        mFocusMeteringControl.startFocusAndMetering(FocusMeteringAction.Builder.from(mPoint1,
                FocusMeteringAction.MeteringMode.AF_AE_AWB).build(), PREVIEW_ASPECT_RATIO_4_X_3);

        verify(mFocusMeteringControl).triggerAf();
        Mockito.reset(mFocusMeteringControl);

        mFocusMeteringControl.startFocusAndMetering(
                FocusMeteringAction.Builder.from(mPoint1,
                        FocusMeteringAction.MeteringMode.AF_ONLY).build(),
                PREVIEW_ASPECT_RATIO_4_X_3);
        verify(mFocusMeteringControl).triggerAf();
        Mockito.reset(mFocusMeteringControl);

        mFocusMeteringControl.startFocusAndMetering(
                FocusMeteringAction.Builder.from(mPoint1,
                        FocusMeteringAction.MeteringMode.AF_AE).build(),
                PREVIEW_ASPECT_RATIO_4_X_3);
        verify(mFocusMeteringControl).triggerAf();
        Mockito.reset(mFocusMeteringControl);

        mFocusMeteringControl.startFocusAndMetering(
                FocusMeteringAction.Builder.from(mPoint1,
                        FocusMeteringAction.MeteringMode.AF_AWB).build(),
                PREVIEW_ASPECT_RATIO_4_X_3);
        verify(mFocusMeteringControl).triggerAf();
        Mockito.reset(mFocusMeteringControl);
    }

    @Test
    public void withoutAFPoints_AFIsNotTriggered() {
        mFocusMeteringControl.startFocusAndMetering(FocusMeteringAction.Builder.from(mPoint1,
                FocusMeteringAction.MeteringMode.AE_ONLY).build(), PREVIEW_ASPECT_RATIO_4_X_3);
        verify(mFocusMeteringControl, never()).triggerAf();
        Mockito.reset(mFocusMeteringControl);

        mFocusMeteringControl.startFocusAndMetering(
                FocusMeteringAction.Builder.from(mPoint1,
                        FocusMeteringAction.MeteringMode.AWB_ONLY).build(),
                PREVIEW_ASPECT_RATIO_4_X_3);
        verify(mFocusMeteringControl, never()).triggerAf();
        Mockito.reset(mFocusMeteringControl);

        mFocusMeteringControl.startFocusAndMetering(
                FocusMeteringAction.Builder.from(mPoint1,
                        FocusMeteringAction.MeteringMode.AE_ONLY).build(),
                PREVIEW_ASPECT_RATIO_4_X_3);
        verify(mFocusMeteringControl, never()).triggerAf();
        Mockito.reset(mFocusMeteringControl);

        mFocusMeteringControl.startFocusAndMetering(
                FocusMeteringAction.Builder.from(mPoint1,
                        FocusMeteringAction.MeteringMode.AE_AWB).build(),
                PREVIEW_ASPECT_RATIO_4_X_3);
        verify(mFocusMeteringControl, never()).triggerAf();
        Mockito.reset(mFocusMeteringControl);
    }

    @Test
    public void updateSessionConfigIsCalled() {
        mFocusMeteringControl.startFocusAndMetering(
                FocusMeteringAction.Builder.from(mPoint1).build(),
                PREVIEW_ASPECT_RATIO_4_X_3);

        verify(mCamera2CameraControl, times(1)).updateSessionConfig();
    }

    @Test
    public void autoCancelDuration_cancelIsCalled() throws InterruptedException {
        mFocusMeteringControl = spy(mFocusMeteringControl);
        final long autocancelDuration = 500;
        FocusMeteringAction action = FocusMeteringAction.Builder.from(mPoint1)
                .setAutoCancelDuration(autocancelDuration, TimeUnit.MILLISECONDS)
                .build();

        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);

        // This is necessary for running delayed task in robolectric.
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        verify(mFocusMeteringControl, timeout(action.getAutoCancelDurationInMillis()))
                .cancelFocusAndMetering();
    }

    @Test
    public void autoCancelDurationDisabled_cancelIsNotCalled() throws InterruptedException {
        mFocusMeteringControl = spy(mFocusMeteringControl);
        final long autocancelDuration = 500;

        FocusMeteringAction action = FocusMeteringAction.Builder.from(mPoint1)
                .setAutoCancelDuration(autocancelDuration, TimeUnit.MILLISECONDS)
                .disableAutoCancel()
                .build();

        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);

        // This is necessary for running delayed task in robolectric.
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        Thread.sleep(autocancelDuration);
        verify(mFocusMeteringControl, never()).cancelFocusAndMetering();
    }

    @Test
    public void onAutoFocusListener_cancelImmediately_calledWithFalse() {
        FocusMeteringAction.OnAutoFocusListener onAutoFocusListener = mock(
                FocusMeteringAction.OnAutoFocusListener.class);

        FocusMeteringAction action = FocusMeteringAction.Builder.from(mPoint1)
                .setAutoFocusCallback(onAutoFocusListener)
                .build();
        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        mFocusMeteringControl.cancelFocusAndMetering();

        verify(onAutoFocusListener).onFocusCompleted(false);
    }

    @Test
    public void onAutoFocusListener_AFNotTriggered_calledWithFalse() {
        FocusMeteringAction.OnAutoFocusListener onAutoFocusListener = mock(
                FocusMeteringAction.OnAutoFocusListener.class);

        FocusMeteringAction action = FocusMeteringAction.Builder.from(mPoint1,
                FocusMeteringAction.MeteringMode.AE_AWB)
                .setAutoFocusCallback(onAutoFocusListener)
                .build();
        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);

        verify(onAutoFocusListener).onFocusCompleted(false);
        Mockito.reset(onAutoFocusListener);

        action = FocusMeteringAction.Builder.from(mPoint1,
                FocusMeteringAction.MeteringMode.AE_ONLY)
                .setAutoFocusCallback(onAutoFocusListener)
                .build();
        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        verify(onAutoFocusListener).onFocusCompleted(false);
        Mockito.reset(onAutoFocusListener);

        action = FocusMeteringAction.Builder.from(mPoint1,
                FocusMeteringAction.MeteringMode.AWB_ONLY)
                .setAutoFocusCallback(onAutoFocusListener)
                .build();
        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        verify(onAutoFocusListener).onFocusCompleted(false);
        Mockito.reset(onAutoFocusListener);
    }

    @Test
    public void onAutoFocusListener_AFLocked_calledWithTrue() {
        FocusMeteringAction.OnAutoFocusListener onAutoFocusListener = mock(
                FocusMeteringAction.OnAutoFocusListener.class);

        FocusMeteringAction action = FocusMeteringAction.Builder.from(mPoint1)
                .setAutoFocusCallback(onAutoFocusListener)
                .build();

        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);

        for (CaptureResultListener listener :
                mCamera2CameraControl.mSessionCallback.mResultListeners) {
            TotalCaptureResult result1 = mock(TotalCaptureResult.class);
            when(result1.get(CaptureResult.CONTROL_AF_STATE)).thenReturn(
                    CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN);
            listener.onCaptureResult(result1);

            TotalCaptureResult result2 = mock(TotalCaptureResult.class);
            when(result2.get(CaptureResult.CONTROL_AF_STATE)).thenReturn(
                    CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED);
            listener.onCaptureResult(result2);

        }
        verify(onAutoFocusListener).onFocusCompleted(true);
    }

    @Test
    public void onAutoFocusListener_executorSpecified_calledWithTheExecutor() {
        FocusMeteringAction.OnAutoFocusListener onAutoFocusListener = mock(
                FocusMeteringAction.OnAutoFocusListener.class);

        Executor executor = spy(new Executor() {
            @Override
            public void execute(@NonNull Runnable runnable) {
                runnable.run();
            }
        });

        FocusMeteringAction action = FocusMeteringAction.Builder.from(mPoint1)
                .setAutoFocusCallback(executor, onAutoFocusListener)
                .build();

        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        for (CaptureResultListener listener :
                mCamera2CameraControl.mSessionCallback.mResultListeners) {
            TotalCaptureResult result1 = mock(TotalCaptureResult.class);
            when(result1.get(CaptureResult.CONTROL_AF_STATE)).thenReturn(
                    CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN);
            listener.onCaptureResult(result1);

            TotalCaptureResult result2 = mock(TotalCaptureResult.class);
            when(result2.get(CaptureResult.CONTROL_AF_STATE)).thenReturn(
                    CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED);
            listener.onCaptureResult(result2);

        }

        verify(onAutoFocusListener).onFocusCompleted(true);
        verify(executor).execute(any());
    }

    @Test
    public void onAutoFocusListener_NotAFLocked_calledWithFalse() {
        FocusMeteringAction.OnAutoFocusListener onAutoFocusListener = mock(
                FocusMeteringAction.OnAutoFocusListener.class);

        FocusMeteringAction action = FocusMeteringAction.Builder.from(mPoint1)
                .setAutoFocusCallback(onAutoFocusListener)
                .build();

        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);

        for (CaptureResultListener listener :
                mCamera2CameraControl.mSessionCallback.mResultListeners) {
            TotalCaptureResult result1 = mock(TotalCaptureResult.class);
            when(result1.get(CaptureResult.CONTROL_AF_STATE)).thenReturn(
                    CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN);
            listener.onCaptureResult(result1);

            TotalCaptureResult result2 = mock(TotalCaptureResult.class);
            when(result2.get(CaptureResult.CONTROL_AF_STATE)).thenReturn(
                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED);
            listener.onCaptureResult(result2);

        }
        verify(onAutoFocusListener).onFocusCompleted(false);
    }

    @Test
    public void onAutoFocusListener_isCalledOnce() {
        FocusMeteringAction.OnAutoFocusListener onAutoFocusListener = mock(
                FocusMeteringAction.OnAutoFocusListener.class);

        FocusMeteringAction action = FocusMeteringAction.Builder.from(mPoint1)
                .setAutoFocusCallback(onAutoFocusListener)
                .build();

        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);

        for (CaptureResultListener listener :
                mCamera2CameraControl.mSessionCallback.mResultListeners) {
            TotalCaptureResult result1 = mock(TotalCaptureResult.class);
            when(result1.get(CaptureResult.CONTROL_AF_STATE)).thenReturn(
                    CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN);
            listener.onCaptureResult(result1);

            TotalCaptureResult result2 = mock(TotalCaptureResult.class);
            when(result2.get(CaptureResult.CONTROL_AF_STATE)).thenReturn(
                    CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED);
            listener.onCaptureResult(result2);

        }

        // cancel it and then ensure the OnAutoFocusListener is still called once.
        mFocusMeteringControl.cancelFocusAndMetering();

        verify(onAutoFocusListener, times(1)).onFocusCompleted(anyBoolean());
    }


    @Test
    public void cancelFocusAndMetering_regionIsReset() {
        FocusMeteringAction action = FocusMeteringAction.Builder.from(mPoint1,
                FocusMeteringAction.MeteringMode.AF_AE_AWB)
                .addPoint(mPoint2, FocusMeteringAction.MeteringMode.AF_AE_AWB)
                .build();

        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        MeteringRectangle[] afRects = getAfRects(mFocusMeteringControl);
        MeteringRectangle[] aeRects = getAeRects(mFocusMeteringControl);
        MeteringRectangle[] awbRects = getAwbRects(mFocusMeteringControl);

        assertThat(afRects).hasLength(2);
        assertThat(aeRects).hasLength(2);
        assertThat(awbRects).hasLength(2);

        mFocusMeteringControl.cancelFocusAndMetering();
        afRects = getAfRects(mFocusMeteringControl);
        aeRects = getAeRects(mFocusMeteringControl);
        awbRects = getAwbRects(mFocusMeteringControl);

        assertThat(afRects).hasLength(0);
        assertThat(aeRects).hasLength(0);
        assertThat(awbRects).hasLength(0);
    }


    @Test
    public void cancelFocusAndMetering_updateSessionIsCalled() {
        FocusMeteringAction action = FocusMeteringAction.Builder.from(mPoint1,
                FocusMeteringAction.MeteringMode.AF_AE_AWB)
                .addPoint(mPoint2, FocusMeteringAction.MeteringMode.AF_AE_AWB)
                .build();

        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        Mockito.reset(mCamera2CameraControl);

        mFocusMeteringControl.cancelFocusAndMetering();
        verify(mCamera2CameraControl, times(1)).updateSessionConfig();
    }


    @Test
    public void cancelFocusAndMetering_triggerCancelAfProperly() {
        // If AF is enabled, cancel operation needs to call cancelAfAeTriggerInternal(true, false)
        FocusMeteringAction action = FocusMeteringAction.Builder.from(mPoint1,
                FocusMeteringAction.MeteringMode.AF_AE_AWB)
                .build();

        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        Mockito.reset(mFocusMeteringControl);
        mFocusMeteringControl.cancelFocusAndMetering();
        verify(mFocusMeteringControl, times(1)).cancelAfAeTrigger(true, false);

        action = FocusMeteringAction.Builder.from(mPoint1, FocusMeteringAction.MeteringMode.AF_AE)
                .build();
        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        Mockito.reset(mFocusMeteringControl);
        mFocusMeteringControl.cancelFocusAndMetering();
        verify(mFocusMeteringControl, times(1)).cancelAfAeTrigger(true, false);

        action = FocusMeteringAction.Builder.from(mPoint1, FocusMeteringAction.MeteringMode.AF_AWB)
                .build();
        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        Mockito.reset(mFocusMeteringControl);
        mFocusMeteringControl.cancelFocusAndMetering();
        verify(mFocusMeteringControl, times(1)).cancelAfAeTrigger(true, false);

        action = FocusMeteringAction.Builder.from(mPoint1, FocusMeteringAction.MeteringMode.AF_ONLY)
                .build();
        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        Mockito.reset(mFocusMeteringControl);
        mFocusMeteringControl.cancelFocusAndMetering();
        verify(mFocusMeteringControl, times(1)).cancelAfAeTrigger(true, false);
    }

    @Test
    public void cancelFocusAndMetering_AFNotInvolved_cancelAfNotTriggered() {
        FocusMeteringAction action = FocusMeteringAction.Builder.from(mPoint1,
                FocusMeteringAction.MeteringMode.AE_ONLY)
                .build();

        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        Mockito.reset(mFocusMeteringControl);
        mFocusMeteringControl.cancelFocusAndMetering();
        verify(mFocusMeteringControl, never()).cancelAfAeTrigger(true, false);

        action = FocusMeteringAction.Builder.from(mPoint1,
                FocusMeteringAction.MeteringMode.AWB_ONLY)
                .build();
        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        Mockito.reset(mFocusMeteringControl);
        mFocusMeteringControl.cancelFocusAndMetering();
        verify(mFocusMeteringControl, never()).cancelAfAeTrigger(true, false);

        action = FocusMeteringAction.Builder.from(mPoint1, FocusMeteringAction.MeteringMode.AE_AWB)
                .build();
        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        Mockito.reset(mFocusMeteringControl);
        mFocusMeteringControl.cancelFocusAndMetering();
        verify(mFocusMeteringControl, never()).cancelAfAeTrigger(true, false);
    }

    @Test
    public void cancelFocusAndMetering_autoCancelIsDisabled() throws InterruptedException {
        mFocusMeteringControl = spy(mFocusMeteringControl);
        final long autocancelDuration = 500;

        FocusMeteringAction action = FocusMeteringAction.Builder.from(mPoint1)
                .setAutoCancelDuration(autocancelDuration, TimeUnit.MILLISECONDS)
                .build();

        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        mFocusMeteringControl.cancelFocusAndMetering();
        Mockito.reset(mFocusMeteringControl);

        // This is necessary for running delayed task in robolectric.
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        Thread.sleep(autocancelDuration);

        verify(mFocusMeteringControl, never()).cancelFocusAndMetering();
    }

    @Test
    public void startFocusMetering_isAfAutoModeIsTrue() {
        FocusMeteringAction action = FocusMeteringAction.Builder.from(mPoint1).build();

        verifyAfMode(CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);

        verifyAfMode(CaptureResult.CONTROL_AF_MODE_AUTO);
    }

    private void verifyAfMode(int expectAfMode) {
        Camera2Config.Builder builder1 = new Camera2Config.Builder();
        mFocusMeteringControl.addFocusMeteringOptions(builder1);
        assertThat(builder1.build().getCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, null))
                .isEqualTo(expectAfMode);
    }

    @Test
    public void startFocusMetering_AfNotInvolved_isAfAutoModeIsSet() {
        FocusMeteringAction action = FocusMeteringAction.Builder.from(mPoint1,
                FocusMeteringAction.MeteringMode.AE_AWB).build();

        verifyAfMode(CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        verifyAfMode(CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
    }

    @Test
    public void startAndThenCancel_isAfAutoModeIsFalse() {
        FocusMeteringAction action = FocusMeteringAction.Builder.from(mPoint1).build();
        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        mFocusMeteringControl.cancelFocusAndMetering();
        verifyAfMode(CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
    }
}
