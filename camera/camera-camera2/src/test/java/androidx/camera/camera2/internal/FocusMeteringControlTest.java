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

package androidx.camera.camera2.internal;

import static androidx.camera.core.FocusMeteringAction.FLAG_AE;
import static androidx.camera.core.FocusMeteringAction.FLAG_AF;
import static androidx.camera.core.FocusMeteringAction.FLAG_AWB;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.TestCase.fail;

import static org.mockito.ArgumentMatchers.any;
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
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Build;
import android.util.Pair;
import android.util.Rational;
import android.util.Size;

import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.Camera2CameraControlImpl.CaptureResultListener;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.CameraControl;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.TagBundle;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.shadows.ShadowCameraManager;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@DoNotInstrument
public class FocusMeteringControlTest {
    @ParameterizedRobolectricTestRunner.Parameters
    public static Collection<Object[]> data() {
        final List<Object[]> data = new ArrayList<>();
        data.add(new Object[]{CameraDevice.TEMPLATE_PREVIEW});
        data.add(new Object[]{CameraDevice.TEMPLATE_RECORD});
        return data;
    }

    private final int mTemplate;

    public FocusMeteringControlTest(int template) {
        mTemplate = template;
    }

    private static final String CAMERA0_ID = "0"; // 640x480 sensor size
    private static final String CAMERA1_ID = "1"; // 1920x1080 sensor size
    private static final String CAMERA2_ID = "2"; // 640x480 sensor size, not support AF_AUTO.
    private static final String CAMERA3_ID = "3"; // camera that does not support 3A regions.

    private static final int SENSOR_WIDTH = 640;
    private static final int SENSOR_HEIGHT = 480;
    private static final int SENSOR_WIDTH2 = 1920;
    private static final int SENSOR_HEIGHT2 = 1080;

    private static final int AREA_WIDTH =
            (int) (MeteringPointFactory.getDefaultPointSize() * SENSOR_WIDTH);
    private static final int AREA_HEIGHT =
            (int) (MeteringPointFactory.getDefaultPointSize() * SENSOR_HEIGHT);
    private static final int AREA_WIDTH2 =
            (int) (MeteringPointFactory.getDefaultPointSize() * SENSOR_WIDTH2);
    private static final int AREA_HEIGHT2 =
            (int) (MeteringPointFactory.getDefaultPointSize() * SENSOR_HEIGHT2);

    private final SurfaceOrientedMeteringPointFactory mPointFactory =
            new SurfaceOrientedMeteringPointFactory(1, 1);
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

    private static final MeteringRectangle METERING_RECTANGLE_1 = new MeteringRectangle(M_RECT_1,
            MeteringRectangle.METERING_WEIGHT_MAX);
    private static final MeteringRectangle METERING_RECTANGLE_2 = new MeteringRectangle(M_RECT_2,
            MeteringRectangle.METERING_WEIGHT_MAX);

    private static final Rational PREVIEW_ASPECT_RATIO_4_X_3 = new Rational(4, 3);
    private Camera2CameraControlImpl mCamera2CameraControlImpl;

    @Before
    public void setUp() throws CameraAccessException {
        initCameras();
        mFocusMeteringControl = spy(initFocusMeteringControl(CAMERA0_ID));
        mFocusMeteringControl.setActive(true);
        mFocusMeteringControl.setTemplate(mTemplate);
    }

    private FocusMeteringControl initFocusMeteringControl(String cameraID) {
        CameraManager cameraManager =
                (CameraManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE);
        try {
            CameraCharacteristicsCompat cameraCharacteristics =
                    CameraCharacteristicsCompat.toCameraCharacteristicsCompat(cameraManager
                            .getCameraCharacteristics(cameraID));

            CameraControlInternal.ControlUpdateCallback updateCallback = mock(
                    CameraControlInternal.ControlUpdateCallback.class);

            mCamera2CameraControlImpl = spy(new Camera2CameraControlImpl(
                    cameraCharacteristics,
                    CameraXExecutors.mainThreadExecutor(),
                    CameraXExecutors.directExecutor(),
                    updateCallback));

            FocusMeteringControl focusMeteringControl = new FocusMeteringControl(
                    mCamera2CameraControlImpl,
                    CameraXExecutors.mainThreadExecutor(), CameraXExecutors.directExecutor());
            focusMeteringControl.setActive(true);
            return focusMeteringControl;
        } catch (CameraAccessException e) {
            fail("Cannot access camera.");
            return null;
        }
    }

    private void initCameras() {
        // **** Camera 0 characteristics (640X480 sensor size)****//
        CameraCharacteristics characteristics0 =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics0 = Shadow.extract(characteristics0);

        shadowCharacteristics0.set(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE,
                new Rect(0, 0, SENSOR_WIDTH, SENSOR_HEIGHT));
        shadowCharacteristics0.set(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES, new int[]{
                CaptureResult.CONTROL_AF_MODE_CONTINUOUS_VIDEO,
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

        shadowCharacteristics0.set(CameraCharacteristics.CONTROL_MAX_REGIONS_AF, 3);
        shadowCharacteristics0.set(CameraCharacteristics.CONTROL_MAX_REGIONS_AE, 3);
        shadowCharacteristics0.set(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB, 1);

        // Add the camera to the camera service
        ((ShadowCameraManager)
                Shadow.extract(
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CAMERA_SERVICE)))
                .addCamera(CAMERA0_ID, characteristics0);

        // **** Camera 1 characteristics (1920x1080 sensor size) ****//
        CameraCharacteristics characteristics1 =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics1 = Shadow.extract(characteristics1);

        shadowCharacteristics1.set(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE,
                new Rect(0, 0, SENSOR_WIDTH2, SENSOR_HEIGHT2));
        shadowCharacteristics1.set(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3);
        shadowCharacteristics1.set(CameraCharacteristics.CONTROL_MAX_REGIONS_AF, 1);
        shadowCharacteristics1.set(CameraCharacteristics.CONTROL_MAX_REGIONS_AE, 1);
        shadowCharacteristics1.set(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB, 1);

        // Add the camera to the camera service
        ((ShadowCameraManager)
                Shadow.extract(
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CAMERA_SERVICE)))
                .addCamera(CAMERA1_ID, characteristics1);


        // **** Camera 2 characteristics (640x480 sensor size, does not support AF_AUTO ****//
        CameraCharacteristics characteristics2 =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics2 = Shadow.extract(characteristics2);

        shadowCharacteristics2.set(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE,
                new Rect(0, 0, SENSOR_WIDTH, SENSOR_HEIGHT));
        shadowCharacteristics2.set(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3);
        shadowCharacteristics2.set(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES, new int[]{
                CaptureResult.CONTROL_AF_MODE_OFF
        });
        shadowCharacteristics2.set(CameraCharacteristics.CONTROL_MAX_REGIONS_AF, 1);
        shadowCharacteristics2.set(CameraCharacteristics.CONTROL_MAX_REGIONS_AE, 1);
        shadowCharacteristics2.set(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB, 1);

        // Add the camera to the camera service
        ((ShadowCameraManager)
                Shadow.extract(
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CAMERA_SERVICE)))
                .addCamera(CAMERA2_ID, characteristics2);

        // ** Camera 3 characteristics (640x480 sensor size, does not support any 3A regions //
        CameraCharacteristics characteristics3 =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics3 = Shadow.extract(characteristics3);

        shadowCharacteristics3.set(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE,
                new Rect(0, 0, SENSOR_WIDTH, SENSOR_HEIGHT));
        shadowCharacteristics3.set(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3);

        shadowCharacteristics3.set(CameraCharacteristics.CONTROL_MAX_REGIONS_AF, 0);
        shadowCharacteristics3.set(CameraCharacteristics.CONTROL_MAX_REGIONS_AE, 0);
        shadowCharacteristics3.set(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB, 0);

        // Add the camera to the camera service
        ((ShadowCameraManager)
                Shadow.extract(
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CAMERA_SERVICE)))
                .addCamera(CAMERA3_ID, characteristics3);
    }

    private MeteringRectangle[] getAfRects(FocusMeteringControl control) {
        Camera2ImplConfig.Builder configBuilder = new Camera2ImplConfig.Builder();
        control.addFocusMeteringOptions(configBuilder);
        Camera2ImplConfig config = configBuilder.build();

        return config.getCaptureRequestOption(CaptureRequest.CONTROL_AF_REGIONS,
                new MeteringRectangle[]{});
    }

    private MeteringRectangle[] getAeRects(FocusMeteringControl control) {
        Camera2ImplConfig.Builder configBuilder = new Camera2ImplConfig.Builder();
        control.addFocusMeteringOptions(configBuilder);
        Camera2ImplConfig config = configBuilder.build();

        return config.getCaptureRequestOption(CaptureRequest.CONTROL_AE_REGIONS,
                new MeteringRectangle[]{});
    }

    private MeteringRectangle[] getAwbRects(FocusMeteringControl control) {
        Camera2ImplConfig.Builder configBuilder = new Camera2ImplConfig.Builder();
        control.addFocusMeteringOptions(configBuilder);
        Camera2ImplConfig config = configBuilder.build();

        return config.getCaptureRequestOption(CaptureRequest.CONTROL_AWB_REGIONS,
                new MeteringRectangle[]{});
    }

    @Test
    public void addFocusMeteringOptions_hasCorrectAfMode() {
        verifyAfMode(mFocusMeteringControl.getDefaultAfMode());
    }

    @Test
    public void triggerAfWithTemplate() {
        mFocusMeteringControl.triggerAf(null);

        verifyTemplate(mTemplate);
    }

    @Test
    public void triggerAePrecaptureWithTemplate() {
        mFocusMeteringControl.triggerAePrecapture(null);

        verifyTemplate(mTemplate);
    }

    @Test
    public void cancelAfAeTriggerWithTemplate() {
        mFocusMeteringControl.cancelAfAeTrigger(true, true);

        verifyTemplate(mTemplate);
    }

    @Test
    public void startFocusAndMetering_invalidPoint() {
        final MeteringPoint invalidPoint = mPointFactory.createPoint(1F, 1.1F);
        mFocusMeteringControl.startFocusAndMetering(
                new FocusMeteringAction.Builder(invalidPoint).build(), new Rational(16, 9));

        final MeteringRectangle[] afRects = getAfRects(mFocusMeteringControl);
        final MeteringRectangle[] aeRects = getAeRects(mFocusMeteringControl);
        final MeteringRectangle[] awbRects = getAwbRects(mFocusMeteringControl);

        assertThat(afRects.length).isEqualTo(0);
        assertThat(aeRects.length).isEqualTo(0);
        assertThat(awbRects.length).isEqualTo(0);
    }

    @Test
    public void startFocusAndMetering_defaultPoint_3ARectssAreCorrect() {
        mFocusMeteringControl.startFocusAndMetering(
                new FocusMeteringAction.Builder(mPoint1).build(),
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
        // Max AF count = 3, Max AE count = 3, Max AWB count = 1
        mFocusMeteringControl.startFocusAndMetering(
                new FocusMeteringAction.Builder(mPoint1)
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

        assertThat(awbRects.length).isEqualTo(1);
        assertThat(awbRects[0].getRect()).isEqualTo(M_RECT_1);
    }

    @Test
    public void startFocusAndMetering_multiplePointVariousModes() {
        // Max AF count = 3, Max AE count = 3, Max AWB count = 1
        mFocusMeteringControl.startFocusAndMetering(
                new FocusMeteringAction.Builder(mPoint1, FLAG_AWB)
                        .addPoint(mPoint2, FLAG_AF | FLAG_AE)
                        .addPoint(mPoint3, FLAG_AF | FLAG_AE | FLAG_AWB)
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

        assertThat(awbRects.length).isEqualTo(1);
        assertThat(awbRects[0].getRect()).isEqualTo(M_RECT_1);
    }

    @Test
    public void startFocusAndMetering_multiplePointVariousModes2() {
        // Max AF count = 3, Max AE count = 3, Max AWB count = 1
        mFocusMeteringControl.startFocusAndMetering(
                new FocusMeteringAction.Builder(mPoint1, FLAG_AF)
                        .addPoint(mPoint2, FLAG_AWB)
                        .addPoint(mPoint3, FLAG_AE)
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
        when(mCamera2CameraControlImpl.getCropSensorRegion()).thenReturn(cropRect);

        MeteringPoint centorPt = mPointFactory.createPoint(0.5f, 0.5f);
        mFocusMeteringControl.startFocusAndMetering(
                new FocusMeteringAction.Builder(centorPt).build(),
                PREVIEW_ASPECT_RATIO_4_X_3);
        MeteringRectangle[] afRects = getAfRects(mFocusMeteringControl);

        final int areaWidth = (int) (MeteringPointFactory.getDefaultPointSize() * cropRect.width());
        final int areaHeight =
                (int) (MeteringPointFactory.getDefaultPointSize() * cropRect.height());
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
                new FocusMeteringAction.Builder(mPoint1).build(),
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
                new FocusMeteringAction.Builder(mPoint1).build(),
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
        when(imageAnalysis.getAttachedSurfaceResolution()).thenReturn(
                new Size(1920, 1080));

        SurfaceOrientedMeteringPointFactory factory =
                new SurfaceOrientedMeteringPointFactory(1.0f, 1.0f, imageAnalysis);

        MeteringPoint point = factory.createPoint(0, 0);
        mFocusMeteringControl.startFocusAndMetering(new FocusMeteringAction.Builder(point).build(),
                PREVIEW_ASPECT_RATIO_4_X_3);
        MeteringRectangle[] afRects = getAfRects(mFocusMeteringControl);


        Rect adjustedRect = new Rect(0, 60 - AREA_HEIGHT / 2, AREA_WIDTH / 2, 60 + AREA_HEIGHT / 2);
        assertThat(afRects[0].getRect()).isEqualTo(adjustedRect);
    }

    @Test
    public void pointSize_ConvertedCorrect() {
        MeteringPoint point1 = mPointFactory.createPoint(0.5f, 0.5f, 1.0f);
        MeteringPoint point2 = mPointFactory.createPoint(0.5f, 0.5f, 0.5f);
        MeteringPoint point3 = mPointFactory.createPoint(0.5f, 0.5f, 0.1f);

        mFocusMeteringControl.startFocusAndMetering(new FocusMeteringAction.Builder(point1)
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
        mFocusMeteringControl.startFocusAndMetering(new FocusMeteringAction.Builder(mPoint1,
                FLAG_AF | FLAG_AE | FLAG_AWB).build(), PREVIEW_ASPECT_RATIO_4_X_3);

        verify(mFocusMeteringControl).triggerAf(any());
        Mockito.reset(mFocusMeteringControl);

        mFocusMeteringControl.startFocusAndMetering(
                new FocusMeteringAction.Builder(mPoint1, FLAG_AF).build(),
                PREVIEW_ASPECT_RATIO_4_X_3);
        verify(mFocusMeteringControl).triggerAf(any());
        Mockito.reset(mFocusMeteringControl);

        mFocusMeteringControl.startFocusAndMetering(
                new FocusMeteringAction.Builder(mPoint1, FLAG_AF | FLAG_AE).build(),
                PREVIEW_ASPECT_RATIO_4_X_3);
        verify(mFocusMeteringControl).triggerAf(any());
        Mockito.reset(mFocusMeteringControl);

        mFocusMeteringControl.startFocusAndMetering(
                new FocusMeteringAction.Builder(mPoint1, FLAG_AF | FLAG_AWB).build(),
                PREVIEW_ASPECT_RATIO_4_X_3);
        verify(mFocusMeteringControl).triggerAf(any());
        Mockito.reset(mFocusMeteringControl);
    }

    @Test
    public void withoutAFPoints_AFIsNotTriggered() {
        mFocusMeteringControl.startFocusAndMetering(
                new FocusMeteringAction.Builder(mPoint1, FLAG_AE).build(),
                PREVIEW_ASPECT_RATIO_4_X_3);
        verify(mFocusMeteringControl, never()).triggerAf(any());
        Mockito.reset(mFocusMeteringControl);

        mFocusMeteringControl.startFocusAndMetering(
                new FocusMeteringAction.Builder(mPoint1, FLAG_AWB).build(),
                PREVIEW_ASPECT_RATIO_4_X_3);
        verify(mFocusMeteringControl, never()).triggerAf(any());
        Mockito.reset(mFocusMeteringControl);

        mFocusMeteringControl.startFocusAndMetering(
                new FocusMeteringAction.Builder(mPoint1, FLAG_AE).build(),
                PREVIEW_ASPECT_RATIO_4_X_3);
        verify(mFocusMeteringControl, never()).triggerAf(any());
        Mockito.reset(mFocusMeteringControl);

        mFocusMeteringControl.startFocusAndMetering(
                new FocusMeteringAction.Builder(mPoint1,
                        FLAG_AE | FLAG_AWB).build(),
                PREVIEW_ASPECT_RATIO_4_X_3);
        verify(mFocusMeteringControl, never()).triggerAf(any());
        Mockito.reset(mFocusMeteringControl);
    }

    @Test
    public void updateSessionConfigIsCalled() {
        mFocusMeteringControl.startFocusAndMetering(
                new FocusMeteringAction.Builder(mPoint1).build(),
                PREVIEW_ASPECT_RATIO_4_X_3);

        verify(mCamera2CameraControlImpl, times(1))
                .updateSessionConfigSynchronous();
    }

    @Test
    public void autoCancelDuration_cancelIsCalled() throws InterruptedException {
        mFocusMeteringControl = spy(mFocusMeteringControl);
        final long autocancelDuration = 500;
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1)
                .setAutoCancelDuration(autocancelDuration, TimeUnit.MILLISECONDS)
                .build();

        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);

        // This is necessary for running delayed task in robolectric.
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        verify(mFocusMeteringControl, timeout(action.getAutoCancelDurationInMillis()))
                .cancelFocusAndMeteringWithoutAsyncResult();
    }

    @Test
    public void autoCancelDurationDisabled_cancelIsNotCalled() throws InterruptedException {
        mFocusMeteringControl = spy(mFocusMeteringControl);
        final long autocancelDuration = 500;

        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1)
                .setAutoCancelDuration(autocancelDuration, TimeUnit.MILLISECONDS)
                .disableAutoCancel()
                .build();

        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);

        // This is necessary for running delayed task in robolectric.
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        Thread.sleep(autocancelDuration);
        verify(mFocusMeteringControl, never()).cancelFocusAndMetering();
    }

    private void assertFutureFocusCompleted(ListenableFuture<FocusMeteringResult> future,
            boolean isFocused) throws ExecutionException, InterruptedException, TimeoutException {
        FocusMeteringResult focusMeteringResult = future.get(3, TimeUnit.SECONDS);
        assertThat(focusMeteringResult.isFocusSuccessful()).isEqualTo(isFocused);
    }

    private void assertFutureComplete(ListenableFuture<Void> future) {
        try {
            future.get();
        } catch (Exception e) {
            fail("Future fails to complete");
        }
    }

    private CaptureResultListener retrieveCaptureResultListener() {
        ArgumentCaptor<CaptureResultListener> argumentCaptor =
                ArgumentCaptor.forClass(Camera2CameraControlImpl.CaptureResultListener.class);
        verify(mCamera2CameraControlImpl).addCaptureResultListener(argumentCaptor.capture());
        CaptureResultListener listener = argumentCaptor.getValue();
        Mockito.reset(mCamera2CameraControlImpl);
        return listener;
    }

    private static void updateCaptureResultWithSessionUpdateId(
            CaptureResultListener captureResultListener, long sessionUpdateId) {
        TotalCaptureResult result = mock(TotalCaptureResult.class);
        CaptureRequest captureRequest = mock(CaptureRequest.class);
        when(result.getRequest()).thenReturn(captureRequest);
        TagBundle tagBundle =
                TagBundle.create(new Pair<>(Camera2CameraControlImpl.TAG_SESSION_UPDATE_ID,
                        sessionUpdateId));
        when(captureRequest.getTag()).thenReturn(tagBundle);

        captureResultListener.onCaptureResult(result);
    }

    private static void updateCaptureResultWithAfState(
            CaptureResultListener captureResultListener,
            Integer afState) {
        TotalCaptureResult result1 = mock(TotalCaptureResult.class);
        when(result1.get(CaptureResult.CONTROL_AF_STATE)).thenReturn(afState);
        captureResultListener.onCaptureResult(result1);
    }

    private static void updateCaptureResultWithAfStateAndSessionUpdateId(
            CaptureResultListener captureResultListener, Integer afState, long sessionUpdateId) {
        TotalCaptureResult result = mock(TotalCaptureResult.class);
        CaptureRequest captureRequest = mock(CaptureRequest.class);
        when(result.get(CaptureResult.CONTROL_AF_STATE)).thenReturn(afState);
        when(result.getRequest()).thenReturn(captureRequest);

        TagBundle tagBundle =
                TagBundle.create(new Pair<>(Camera2CameraControlImpl.TAG_SESSION_UPDATE_ID,
                        sessionUpdateId));
        when(captureRequest.getTag()).thenReturn(tagBundle);

        captureResultListener.onCaptureResult(result);
    }

    private static void updateCaptureResultWithAfModeAndSessionUpdateId(
            CaptureResultListener captureResultListener, int afMode, long sessionUpdateId) {
        TotalCaptureResult result = mock(TotalCaptureResult.class);
        CaptureRequest captureRequest = mock(CaptureRequest.class);
        when(result.get(CaptureResult.CONTROL_AF_MODE)).thenReturn(afMode);
        when(result.getRequest()).thenReturn(captureRequest);

        TagBundle tagBundle =
                TagBundle.create(new Pair<>(Camera2CameraControlImpl.TAG_SESSION_UPDATE_ID,
                        sessionUpdateId));
        when(captureRequest.getTag()).thenReturn(tagBundle);

        captureResultListener.onCaptureResult(result);
    }

    private static <T> void assertFutureFailedWithOperationCancelation(ListenableFuture<T> future)
            throws Exception {
        try {
            future.get();
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(CameraControl.OperationCanceledException.class);
            return;
        }
        fail("Should fail with CameraControl.OperationCanceledException.");
    }

    @Test
    public void startFocusMeteringAEAWB_sessionUpdated_completesWithFocusFalse()
            throws ExecutionException, InterruptedException, TimeoutException {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FLAG_AE | FLAG_AWB)
                .build();
        ListenableFuture<FocusMeteringResult> future =
                mFocusMeteringControl.startFocusAndMetering(action,
                        PREVIEW_ASPECT_RATIO_4_X_3);
        CaptureResultListener captureResultListener = retrieveCaptureResultListener();
        updateCaptureResultWithSessionUpdateId(captureResultListener,
                mCamera2CameraControlImpl.getCurrentSessionUpdateId());

        assertFutureFocusCompleted(future, false);
    }


    @Test
    public void startFocusMeteringAE_sessionUpdated_completesWithFocusFalse()
            throws ExecutionException, InterruptedException, TimeoutException {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FLAG_AE).build();

        ListenableFuture<FocusMeteringResult> future2 =
                mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        CaptureResultListener captureResultListener = retrieveCaptureResultListener();
        updateCaptureResultWithSessionUpdateId(captureResultListener,
                mCamera2CameraControlImpl.getCurrentSessionUpdateId());

        assertFutureFocusCompleted(future2, false);
    }

    @Test
    public void startFocusMeteringAWB_sessionUpdated_completesWithFocusFalse()
            throws ExecutionException, InterruptedException, TimeoutException {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FLAG_AWB)
                .build();

        ListenableFuture<FocusMeteringResult> future3 =
                mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        CaptureResultListener captureResultListener = retrieveCaptureResultListener();
        updateCaptureResultWithSessionUpdateId(captureResultListener,
                mCamera2CameraControlImpl.getCurrentSessionUpdateId());

        assertFutureFocusCompleted(future3, false);
    }

    @Test
    public void startFocusMetering_sessionUpdateIdIncreaseBy1_completesWithFocusFalse()
            throws ExecutionException, InterruptedException, TimeoutException {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FLAG_AE | FLAG_AWB)
                .build();
        ListenableFuture<FocusMeteringResult> future =
                mFocusMeteringControl.startFocusAndMetering(action,
                        PREVIEW_ASPECT_RATIO_4_X_3);
        CaptureResultListener captureResultListener = retrieveCaptureResultListener();
        updateCaptureResultWithSessionUpdateId(captureResultListener,
                mCamera2CameraControlImpl.getCurrentSessionUpdateId() + 1);

        assertFutureFocusCompleted(future, false);
    }

    @Test
    public void startFocusMetering_AFLockedWithSessionUpdated_completesWithfocusTrue()
            throws ExecutionException, InterruptedException, TimeoutException {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1).build();

        ListenableFuture<FocusMeteringResult> future =
                mFocusMeteringControl.startFocusAndMetering(action,
                        PREVIEW_ASPECT_RATIO_4_X_3);

        CaptureResultListener captureResultListener = retrieveCaptureResultListener();

        updateCaptureResultWithAfState(captureResultListener,
                CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN);

        updateCaptureResultWithAfStateAndSessionUpdateId(captureResultListener,
                CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                mCamera2CameraControlImpl.getCurrentSessionUpdateId());

        assertFutureFocusCompleted(future, true);
    }

    @Test
    public void startFocusMetering_AFLockedThenSessionUpdated_completesWithFocusTrue()
            throws ExecutionException, InterruptedException, TimeoutException {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1).build();

        ListenableFuture<FocusMeteringResult> future =
                mFocusMeteringControl.startFocusAndMetering(action,
                        PREVIEW_ASPECT_RATIO_4_X_3);

        CaptureResultListener captureResultListener = retrieveCaptureResultListener();

        updateCaptureResultWithAfState(captureResultListener,
                CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN);

        updateCaptureResultWithAfState(captureResultListener,
                CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED);

        updateCaptureResultWithSessionUpdateId(captureResultListener,
                mCamera2CameraControlImpl.getCurrentSessionUpdateId());

        assertFutureFocusCompleted(future, true);
    }

    @Test
    public void startFocusMetering_NotAFLocked_completesWithFocusFalse()
            throws ExecutionException, InterruptedException, TimeoutException {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1).build();

        ListenableFuture<FocusMeteringResult> future =
                mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);

        CaptureResultListener captureResultListener = retrieveCaptureResultListener();

        updateCaptureResultWithAfState(captureResultListener,
                CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN);

        updateCaptureResultWithAfStateAndSessionUpdateId(captureResultListener,
                CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED,
                mCamera2CameraControlImpl.getCurrentSessionUpdateId());

        assertFutureFocusCompleted(future, false);
    }

    // When AfState is null, it means it does not support AF.
    @Test
    public void startFocusMetering_AfStateIsNull_completesWithFocusTrue()
            throws ExecutionException, InterruptedException, TimeoutException {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1).build();

        ListenableFuture<FocusMeteringResult> future =
                mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);

        CaptureResultListener captureResultListener = retrieveCaptureResultListener();

        updateCaptureResultWithAfState(captureResultListener, null);

        updateCaptureResultWithAfStateAndSessionUpdateId(captureResultListener,
                null,
                mCamera2CameraControlImpl.getCurrentSessionUpdateId());

        assertFutureFocusCompleted(future, true);
    }

    @Test
    public void startFocusMeteringAFOnly_sessionUpdated_completesWithFocusTrue()
            throws ExecutionException, InterruptedException, TimeoutException {
        FocusMeteringAction action =
                new FocusMeteringAction.Builder(mPoint1, FLAG_AF).build();

        ListenableFuture<FocusMeteringResult> future =
                mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);

        CaptureResultListener captureResultListener = retrieveCaptureResultListener();

        updateCaptureResultWithAfState(captureResultListener,
                CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN);

        updateCaptureResultWithAfStateAndSessionUpdateId(captureResultListener,
                CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                mCamera2CameraControlImpl.getCurrentSessionUpdateId());

        assertFutureFocusCompleted(future, true);
    }

    @Test
    public void startFocusMeteringAfRequested_CameraNotSupportAfAuto_CompletesWithTrue()
            throws ExecutionException, InterruptedException, TimeoutException {
        // Use camera which does not support AF_AUTO
        FocusMeteringControl focusMeteringControl = initFocusMeteringControl(CAMERA2_ID);
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1)
                .build();

        ListenableFuture<FocusMeteringResult> result = focusMeteringControl.startFocusAndMetering(
                action, PREVIEW_ASPECT_RATIO_4_X_3);

        CaptureResultListener captureResultListener = retrieveCaptureResultListener();

        updateCaptureResultWithSessionUpdateId(captureResultListener,
                mCamera2CameraControlImpl.getCurrentSessionUpdateId());

        assertFutureFocusCompleted(result, true);
    }

    @Test
    public void startFocusMetering_cancelBeforeCompleted_failWithOperationCancelledOperation() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1).build();

        ListenableFuture<FocusMeteringResult> future = mFocusMeteringControl.startFocusAndMetering(
                action, PREVIEW_ASPECT_RATIO_4_X_3);
        mFocusMeteringControl.cancelFocusAndMetering();

        try {
            future.get();
            fail("The future should fail.");
        } catch (ExecutionException | InterruptedException e) {
            assertThat(e.getCause()).isInstanceOf(CameraControl.OperationCanceledException.class);
        }
    }

    @Test
    public void startThenCancelThenStart_previous2FuturesFailsWithOperationCancelled()
            throws Exception {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1)
                .build();

        ListenableFuture<FocusMeteringResult> result1 =
                mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);

        ListenableFuture<Void> result2 =
                mFocusMeteringControl.cancelFocusAndMetering();

        Mockito.reset(mCamera2CameraControlImpl);
        ListenableFuture<FocusMeteringResult> result3 =
                mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        CaptureResultListener captureResultListener = retrieveCaptureResultListener();

        assertFutureFailedWithOperationCancelation(result1);
        assertFutureFailedWithOperationCancelation(result2);

        updateCaptureResultWithAfState(captureResultListener,
                CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN);
        updateCaptureResultWithAfStateAndSessionUpdateId(captureResultListener,
                CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                mCamera2CameraControlImpl.getCurrentSessionUpdateId());

        assertFutureFocusCompleted(result3, true);
    }

    @Test
    public void startMultipleActions_cancelNonLatest() throws Exception {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1)
                .build();

        ListenableFuture<FocusMeteringResult> result1 =
                mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);

        ListenableFuture<FocusMeteringResult> result2 =
                mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);

        Mockito.reset(mCamera2CameraControlImpl);
        ListenableFuture<FocusMeteringResult> result3 =
                mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        CaptureResultListener captureResultListener = retrieveCaptureResultListener();

        assertFutureFailedWithOperationCancelation(result1);
        assertFutureFailedWithOperationCancelation(result2);

        updateCaptureResultWithAfState(captureResultListener,
                CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN);
        updateCaptureResultWithAfStateAndSessionUpdateId(captureResultListener,
                CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                mCamera2CameraControlImpl.getCurrentSessionUpdateId());

        assertFutureFocusCompleted(result3, true);
    }

    @Test
    public void startFocusMetering_focusedThenCancel_futureStillCompletes()
            throws ExecutionException, InterruptedException, TimeoutException {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1)
                .build();

        ListenableFuture<FocusMeteringResult> result =
                mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        CaptureResultListener captureResultListener = retrieveCaptureResultListener();

        updateCaptureResultWithAfState(captureResultListener,
                CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN);

        updateCaptureResultWithAfStateAndSessionUpdateId(captureResultListener,
                CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                mCamera2CameraControlImpl.getCurrentSessionUpdateId());

        // cancel it and then ensure the returned ListenableFuture still completes;
        mFocusMeteringControl.cancelFocusAndMetering();

        assertFutureFocusCompleted(result, true);
    }

    @Test
    public void cancelFocusAndMetering_regionIsReset() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FLAG_AF | FLAG_AE | FLAG_AWB)
                .addPoint(mPoint2, FLAG_AF | FLAG_AE | FLAG_AWB)
                .build();

        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        MeteringRectangle[] afRects = getAfRects(mFocusMeteringControl);
        MeteringRectangle[] aeRects = getAeRects(mFocusMeteringControl);
        MeteringRectangle[] awbRects = getAwbRects(mFocusMeteringControl);

        // Max AF count = 3, Max AE count = 3, Max AWB count = 1
        assertThat(afRects).hasLength(2);
        assertThat(aeRects).hasLength(2);
        assertThat(awbRects).hasLength(1);

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
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FLAG_AF | FLAG_AE | FLAG_AWB)
                .addPoint(mPoint2, FLAG_AF | FLAG_AE | FLAG_AWB)
                .build();

        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        Mockito.reset(mCamera2CameraControlImpl);

        mFocusMeteringControl.cancelFocusAndMetering();
        verify(mCamera2CameraControlImpl, times(1))
                .updateSessionConfigSynchronous();
    }

    @Test
    public void cancelFocusAndMetering_triggerCancelAfProperly() {
        // If AF is enabled, cancel operation needs to call cancelAfAeTriggerInternal(true, false)
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FLAG_AF | FLAG_AE | FLAG_AWB)
                .build();

        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        Mockito.reset(mFocusMeteringControl);
        mFocusMeteringControl.cancelFocusAndMetering();
        verify(mFocusMeteringControl, times(1)).cancelAfAeTrigger(true, false);

        action = new FocusMeteringAction.Builder(mPoint1, FLAG_AF | FLAG_AE).build();
        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        Mockito.reset(mFocusMeteringControl);
        mFocusMeteringControl.cancelFocusAndMetering();
        verify(mFocusMeteringControl, times(1)).cancelAfAeTrigger(true, false);

        action = new FocusMeteringAction.Builder(mPoint1, FLAG_AF | FLAG_AWB).build();
        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        Mockito.reset(mFocusMeteringControl);
        mFocusMeteringControl.cancelFocusAndMetering();
        verify(mFocusMeteringControl, times(1)).cancelAfAeTrigger(true, false);

        action = new FocusMeteringAction.Builder(mPoint1, FLAG_AF)
                .build();
        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        Mockito.reset(mFocusMeteringControl);
        mFocusMeteringControl.cancelFocusAndMetering();
        verify(mFocusMeteringControl, times(1)).cancelAfAeTrigger(true, false);
    }

    @Test
    public void cancelFocusAndMetering_AFNotInvolved_cancelAfNotTriggered() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1, FLAG_AE).build();

        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        Mockito.reset(mFocusMeteringControl);
        mFocusMeteringControl.cancelFocusAndMetering();
        verify(mFocusMeteringControl, never()).cancelAfAeTrigger(true, false);

        action = new FocusMeteringAction.Builder(mPoint1, FLAG_AWB).build();
        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        Mockito.reset(mFocusMeteringControl);
        mFocusMeteringControl.cancelFocusAndMetering();
        verify(mFocusMeteringControl, never()).cancelAfAeTrigger(true, false);

        action = new FocusMeteringAction.Builder(mPoint1, FLAG_AE | FLAG_AWB).build();
        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        Mockito.reset(mFocusMeteringControl);
        mFocusMeteringControl.cancelFocusAndMetering();
        verify(mFocusMeteringControl, never()).cancelAfAeTrigger(true, false);
    }

    @Test
    public void cancelFocusMetering_actionIsCancelledAndfutureCompletes() throws Exception {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1).build();
        ListenableFuture<FocusMeteringResult> actionResult =
                mFocusMeteringControl.startFocusAndMetering(action,
                        PREVIEW_ASPECT_RATIO_4_X_3);

        // reset mock so that we can capture the listener added by cancelFocusAndMetering.
        Mockito.reset(mCamera2CameraControlImpl);
        ListenableFuture<Void> cancelResult =
                mFocusMeteringControl.cancelFocusAndMetering();
        CaptureResultListener captureResultListener = retrieveCaptureResultListener();

        updateCaptureResultWithAfModeAndSessionUpdateId(captureResultListener,
                mFocusMeteringControl.getDefaultAfMode(),
                mCamera2CameraControlImpl.getCurrentSessionUpdateId());

        assertFutureFailedWithOperationCancelation(actionResult);
        assertFutureComplete(cancelResult);
    }

    @Test
    public void cancelFocusAndMetering_autoCancelIsDisabled() throws InterruptedException {
        mFocusMeteringControl = spy(mFocusMeteringControl);
        final long autocancelDuration = 500;

        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1)
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
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1).build();

        verifyAfMode(mFocusMeteringControl.getDefaultAfMode());

        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);

        verifyAfMode(CaptureResult.CONTROL_AF_MODE_AUTO);
    }

    private void verifyAfMode(int expectAfMode) {
        Camera2ImplConfig.Builder builder1 = new Camera2ImplConfig.Builder();
        mFocusMeteringControl.addFocusMeteringOptions(builder1);
        assertThat(builder1.build().getCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE, null))
                .isEqualTo(expectAfMode);
    }

    @SuppressWarnings("unchecked")
    private void verifyTemplate(int expectTemplate) {
        ArgumentCaptor<List<CaptureConfig>> captor = ArgumentCaptor.forClass(List.class);
        verify(mCamera2CameraControlImpl).submitCaptureRequestsInternal(captor.capture());

        List<CaptureConfig> captureConfigList = captor.getValue();
        assertThat(captureConfigList.get(0).getTemplateType()).isEqualTo(expectTemplate);
    }

    @Test
    public void startFocusMetering_AfNotInvolved_isAfAutoModeIsSet() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FLAG_AE | FLAG_AWB).build();

        int defaultAfMode = mFocusMeteringControl.getDefaultAfMode();
        verifyAfMode(defaultAfMode);
        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        verifyAfMode(defaultAfMode);
    }

    @Test
    public void startAndThenCancel_isAfAutoModeIsFalse() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1).build();
        mFocusMeteringControl.startFocusAndMetering(action, PREVIEW_ASPECT_RATIO_4_X_3);
        mFocusMeteringControl.cancelFocusAndMetering();
        verifyAfMode(mFocusMeteringControl.getDefaultAfMode());
    }

    @Test
    public void startFocusMeteringAFAEAWB_noPointsAreSupported_failFuture() throws Exception {
        FocusMeteringControl focusMeteringControl = initFocusMeteringControl(CAMERA3_ID);
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FLAG_AF | FLAG_AE | FLAG_AWB).build();

        ListenableFuture<FocusMeteringResult> future =
                focusMeteringControl.startFocusAndMetering(action,
                        PREVIEW_ASPECT_RATIO_4_X_3);

        try {
            future.get(500, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
            return;
        }

        fail("Future should fail with IllegalArgumentException.");
    }

    @Test
    public void startFocusMeteringAEAWB_noPointsAreSupported_failFuture() throws Exception {
        FocusMeteringControl focusMeteringControl = initFocusMeteringControl(CAMERA3_ID);
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FLAG_AE | FLAG_AWB).build();

        ListenableFuture<FocusMeteringResult> future =
                focusMeteringControl.startFocusAndMetering(action,
                        PREVIEW_ASPECT_RATIO_4_X_3);

        try {
            future.get(500, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
            return;
        }

        fail("Future should fail with IllegalArgumentException.");
    }

    @Test
    public void startFocusMeteringAFAWB_noPointsAreSupported_failFuture() throws Exception {
        FocusMeteringControl focusMeteringControl = initFocusMeteringControl(CAMERA3_ID);
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FLAG_AF | FLAG_AWB).build();

        ListenableFuture<FocusMeteringResult> future =
                focusMeteringControl.startFocusAndMetering(action,
                        PREVIEW_ASPECT_RATIO_4_X_3);
        try {
            future.get(500, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
            return;
        }

        fail("Future should fail with IllegalArgumentException.");
    }

    @Test
    public void startFocusMetering_morePointsThanSupported_futureCompletes()
            throws ExecutionException, InterruptedException, TimeoutException {
        // Camera0 only support 3 AF, 3 AE, 1 AWB regions, here we try to have 1 AE region, 2 AWB
        // regions.  it should still complete the future.
        FocusMeteringAction action =
                new FocusMeteringAction.Builder(mPoint1, FLAG_AE | FLAG_AWB)
                        .addPoint(mPoint2, FLAG_AWB)
                        .build();

        ListenableFuture<FocusMeteringResult> future =
                mFocusMeteringControl.startFocusAndMetering(action,
                        PREVIEW_ASPECT_RATIO_4_X_3);

        CaptureResultListener captureResultListener = retrieveCaptureResultListener();

        updateCaptureResultWithAfState(captureResultListener,
                CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN);

        updateCaptureResultWithAfStateAndSessionUpdateId(captureResultListener,
                CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                mCamera2CameraControlImpl.getCurrentSessionUpdateId());

        assertFutureFocusCompleted(future, false);
    }

    @Test
    public void startFocusMetering_noPointsAreValid_failFuture() throws Exception {
        FocusMeteringControl focusMeteringControl = initFocusMeteringControl(CAMERA0_ID);

        // These will generate MeteringRectangles (width == 0 or height ==0)
        final MeteringPoint invalidPt1 = mPointFactory.createPoint(2.0f, 2.0f);
        final MeteringPoint invalidPt2 = mPointFactory.createPoint(2.0f, 0.5f);
        final MeteringPoint invalidPt3 = mPointFactory.createPoint(-1.0f, -1.0f);

        FocusMeteringAction action =
                new FocusMeteringAction.Builder(invalidPt1, FLAG_AF)
                        .addPoint(invalidPt2, FLAG_AE)
                        .addPoint(invalidPt3, FLAG_AWB).build();

        ListenableFuture<FocusMeteringResult> future =
                focusMeteringControl.startFocusAndMetering(action,
                        PREVIEW_ASPECT_RATIO_4_X_3);
        try {
            future.get(500, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
            return;
        }

        fail("Future should fail with IllegalArgumentException.");
    }

}
