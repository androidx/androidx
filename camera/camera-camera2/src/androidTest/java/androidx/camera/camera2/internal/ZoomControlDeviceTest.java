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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ZoomState;
import androidx.camera.core.impl.CameraControlInternal.ControlUpdateCallback;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.HandlerUtil;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.core.os.HandlerCompat;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 21)
public final class ZoomControlDeviceTest {
    private static final int TOLERANCE = 5;
    private ZoomControl mZoomControl;
    private Camera2CameraControlImpl mCamera2CameraControlImpl;
    private HandlerThread mHandlerThread;
    private ControlUpdateCallback mControlUpdateCallback;
    private CameraCharacteristics mCameraCharacteristics;
    private Handler mHandler;

    @Before
    public void setUp()
            throws CameraInfoUnavailableException, CameraAccessException, InterruptedException {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK));

        // Init CameraX
        Context context = ApplicationProvider.getApplicationContext();
        CameraXConfig config = Camera2Config.defaultConfig();
        CameraX.initialize(context, config);

        mCameraCharacteristics =
                CameraUtil.getCameraCharacteristics(CameraSelector.LENS_FACING_BACK);

        assumeTrue(getMaxDigitalZoom() >= 2.0);

        mControlUpdateCallback = mock(ControlUpdateCallback.class);
        mHandlerThread = new HandlerThread("ControlThread");
        mHandlerThread.start();
        mHandler = HandlerCompat.createAsync(mHandlerThread.getLooper());

        ScheduledExecutorService executorService = CameraXExecutors.newHandlerExecutor(mHandler);
        CameraCharacteristicsCompat cameraCharacteristicsCompat =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(mCameraCharacteristics);
        mCamera2CameraControlImpl = new Camera2CameraControlImpl(cameraCharacteristicsCompat,
                executorService, executorService, mControlUpdateCallback);

        mZoomControl = mCamera2CameraControlImpl.getZoomControl();
        mZoomControl.setActive(true);

        // Await Camera2CameraControlImpl updateSessionConfig to complete.
        HandlerUtil.waitForLooperToIdle(mHandler);
        Mockito.reset(mControlUpdateCallback);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        CameraX.shutdown().get(10000, TimeUnit.MILLISECONDS);
        if (mHandlerThread != null) {
            mHandlerThread.quit();
        }
    }

    private boolean isAndroidRZoomEnabled() {
        return (Build.VERSION.SDK_INT >= 30
                && mCameraCharacteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
                != null);
    }

    @Test
    @UiThreadTest
    public void setZoomRatio_getValueIsCorrect_InUIThread() {
        final float newZoomRatio = 2.0f;
        assumeTrue(newZoomRatio <= mZoomControl.getZoomState().getValue().getMaxZoomRatio());

        // We can only ensure new value is reflected immediately on getZoomRatio on UI thread
        // because of the nature of LiveData.
        mZoomControl.setZoomRatio(newZoomRatio);
        assertThat(mZoomControl.getZoomState().getValue().getZoomRatio()).isEqualTo(newZoomRatio);
    }

    @Test
    @UiThreadTest
    public void setZoomRatio_largerThanMax_zoomUnmodified() {
        assumeTrue(2.0f <= mZoomControl.getZoomState().getValue().getMaxZoomRatio());
        mZoomControl.setZoomRatio(2.0f);
        float maxZoomRatio = mZoomControl.getZoomState().getValue().getMaxZoomRatio();
        mZoomControl.setZoomRatio(maxZoomRatio + 1.0f);
        assertThat(mZoomControl.getZoomState().getValue().getZoomRatio()).isEqualTo(2.0f);
    }

    @Test
    public void setZoomRatio_largerThanMax_OutOfRangeException() {
        float maxZoomRatio = mZoomControl.getZoomState().getValue().getMaxZoomRatio();
        ListenableFuture<Void> result = mZoomControl.setZoomRatio(maxZoomRatio + 1.0f);

        assertThrowOutOfRangeExceptionOnListenableFuture(result);
    }

    private void assertThrowOutOfRangeExceptionOnListenableFuture(ListenableFuture<Void> result) {
        try {
            result.get(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | TimeoutException e) {
        } catch (ExecutionException ee) {
            assertThat(ee.getCause()).isInstanceOf(IllegalArgumentException.class);
            return;
        }

        fail();
    }

    @Test
    @UiThreadTest
    public void setZoomRatio_smallerThanMin_zoomUnmodified() {
        assumeTrue(2.0f <= mZoomControl.getZoomState().getValue().getMaxZoomRatio());
        mZoomControl.setZoomRatio(2.0f);
        float minZoomRatio = mZoomControl.getZoomState().getValue().getMinZoomRatio();
        mZoomControl.setZoomRatio(minZoomRatio - 1.0f);
        assertThat(mZoomControl.getZoomState().getValue().getZoomRatio()).isEqualTo(2.0f);
    }

    @Test
    public void setZoomRatio_smallerThanMin_OutOfRangeException() {
        float minZoomRatio = mZoomControl.getZoomState().getValue().getMinZoomRatio();
        ListenableFuture<Void> result = mZoomControl.setZoomRatio(minZoomRatio - 1.0f);
        assertThrowOutOfRangeExceptionOnListenableFuture(result);
    }

    private Rect getSensorRect() {
        Rect rect = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        // Some device like pixel 2 will have (0, 8) as the left-top corner.
        return new Rect(0, 0, rect.width(), rect.height());
    }

    @Test
    public void setZoomRatioBy1_0_isEqualToSensorRect() throws InterruptedException {
        assumeFalse(isAndroidRZoomEnabled());
        mZoomControl.setZoomRatio(1.0f);
        HandlerUtil.waitForLooperToIdle(mHandler);
        Rect sessionCropRegion = getSessionCropRegion(mControlUpdateCallback);
        assertThat(sessionCropRegion).isEqualTo(getSensorRect());
    }

    @Test
    @RequiresApi(30)
    public void setZoomRatioBy1_0_androidRZoomRatioIsUpdated() throws InterruptedException {
        assumeTrue(isAndroidRZoomEnabled());
        mZoomControl.setZoomRatio(1.0f);
        HandlerUtil.waitForLooperToIdle(mHandler);
        float zoomRatio = getAndroidRZoomRatio(mControlUpdateCallback);
        assertThat(zoomRatio).isEqualTo(1.0f);
    }

    @Test
    public void setZoomRatioBy2_0_cropRegionIsSetCorrectly() throws InterruptedException {
        assumeFalse(isAndroidRZoomEnabled());
        mZoomControl.setZoomRatio(2.0f);
        HandlerUtil.waitForLooperToIdle(mHandler);

        Rect sessionCropRegion = getSessionCropRegion(mControlUpdateCallback);

        Rect sensorRect = getSensorRect();
        int cropX = (sensorRect.width() / 4);
        int cropY = (sensorRect.height() / 4);
        Rect cropRect = new Rect(cropX, cropY, cropX + sensorRect.width() / 2,
                cropY + sensorRect.height() / 2);
        assertThat(sessionCropRegion).isEqualTo(cropRect);
    }

    @Test
    @RequiresApi(30)
    public void setZoomRatioBy2_0_androidRZoomRatioIsUpdated() throws InterruptedException {
        assumeTrue(isAndroidRZoomEnabled());
        mZoomControl.setZoomRatio(2.0f);
        HandlerUtil.waitForLooperToIdle(mHandler);

        float zoomRatio = getAndroidRZoomRatio(mControlUpdateCallback);
        assertThat(zoomRatio).isEqualTo(2.0f);
    }

    @NonNull
    private Rect getSessionCropRegion(ControlUpdateCallback controlUpdateCallback) {
        verify(controlUpdateCallback, times(1)).onCameraControlUpdateSessionConfig();
        SessionConfig sessionConfig = mCamera2CameraControlImpl.getSessionConfig();
        Camera2ImplConfig camera2Config = new Camera2ImplConfig(
                sessionConfig.getImplementationOptions());

        reset(controlUpdateCallback);
        return camera2Config.getCaptureRequestOption(
                CaptureRequest.SCALER_CROP_REGION, null);
    }

    @NonNull
    private Float getAndroidRZoomRatio(ControlUpdateCallback controlUpdateCallback) {
        verify(controlUpdateCallback, times(1)).onCameraControlUpdateSessionConfig();
        SessionConfig sessionConfig = mCamera2CameraControlImpl.getSessionConfig();
        Camera2ImplConfig camera2Config = new Camera2ImplConfig(
                sessionConfig.getImplementationOptions());

        reset(controlUpdateCallback);
        assertThat(camera2Config.getCaptureRequestOption(CaptureRequest.SCALER_CROP_REGION, null))
                .isNull();
        return camera2Config.getCaptureRequestOption(
                CaptureRequest.CONTROL_ZOOM_RATIO, null);
    }

    @UiThreadTest
    @Test
    public void setLinearZoomBy0_isSameAsMinRatio() {
        mZoomControl.setLinearZoom(0);
        float ratioAtPercentage0 = mZoomControl.getZoomState().getValue().getZoomRatio();

        mZoomControl.setZoomRatio(mZoomControl.getZoomState().getValue().getMinZoomRatio());
        float ratioAtMinZoomRatio = mZoomControl.getZoomState().getValue().getZoomRatio();

        assertThat(ratioAtPercentage0).isEqualTo(ratioAtMinZoomRatio);
    }

    @UiThreadTest
    @Test
    public void setLinearZoomBy1_isSameAsMaxRatio() {
        mZoomControl.setLinearZoom(1);
        float ratioAtPercentage1 = mZoomControl.getZoomState().getValue().getZoomRatio();

        mZoomControl.setZoomRatio(mZoomControl.getZoomState().getValue().getMaxZoomRatio());
        float ratioAtMaxZoomRatio = mZoomControl.getZoomState().getValue().getZoomRatio();

        assertThat(ratioAtPercentage1).isEqualTo(ratioAtMaxZoomRatio);
    }

    @UiThreadTest
    @Test
    public void setLinearZoomBy0_5_isHalfCropWidth() throws InterruptedException {
        assumeFalse(isAndroidRZoomEnabled());

        mZoomControl.setLinearZoom(1f);
        HandlerUtil.waitForLooperToIdle(mHandler);
        Rect cropRegionMaxZoom = getSessionCropRegion(mControlUpdateCallback);

        Rect cropRegionMinZoom = getSensorRect();

        mZoomControl.setLinearZoom(0.5f);
        HandlerUtil.waitForLooperToIdle(mHandler);
        Rect cropRegionHalfZoom = getSessionCropRegion(mControlUpdateCallback);

        Assert.assertEquals(cropRegionHalfZoom.width(),
                (cropRegionMinZoom.width() + cropRegionMaxZoom.width()) / 2.0f, TOLERANCE);
    }

    @UiThreadTest
    @RequiresApi(30)
    @Test
    public void setLinearZoomBy0_5_androidRZoomRatioUpdatedCorrectly() throws InterruptedException {
        assumeTrue(isAndroidRZoomEnabled());

        mZoomControl.setLinearZoom(1f);
        HandlerUtil.waitForLooperToIdle(mHandler);
        float zoomRatioForLinearMax = getAndroidRZoomRatio(mControlUpdateCallback);
        final float cropWidth = 10000f;
        float cropWidthForLinearMax = cropWidth / zoomRatioForLinearMax;

        mZoomControl.setLinearZoom(0f);
        HandlerUtil.waitForLooperToIdle(mHandler);
        float zoomRatioForLinearMin = getAndroidRZoomRatio(mControlUpdateCallback);
        float cropWidthForLinearMin = cropWidth / zoomRatioForLinearMin;

        mZoomControl.setLinearZoom(0.5f);
        HandlerUtil.waitForLooperToIdle(mHandler);
        float zoomRatioForLinearHalf = getAndroidRZoomRatio(mControlUpdateCallback);
        float cropWidthForLinearHalf = cropWidth / zoomRatioForLinearHalf;

        Assert.assertEquals(cropWidthForLinearHalf,
                (cropWidthForLinearMin + cropWidthForLinearMax) / 2.0f, TOLERANCE);
    }

    @UiThreadTest
    @Test
    public void setLinearZoom_cropWidthChangedLinearly() throws InterruptedException {
        assumeFalse(isAndroidRZoomEnabled());

        // crop region in percentage == 0 is null, need to use sensor rect instead.
        Rect prevCropRegion = getSensorRect();

        float prevWidthDelta = 0;
        for (float percentage = 0.1f; percentage < 1.0f; percentage += 0.1f) {

            mZoomControl.setLinearZoom(percentage);
            HandlerUtil.waitForLooperToIdle(mHandler);
            Rect cropRegion = getSessionCropRegion(mControlUpdateCallback);

            if (prevWidthDelta == 0) {
                prevWidthDelta = prevCropRegion.width() - cropRegion.width();
            } else {
                float widthDelta = prevCropRegion.width() - cropRegion.width();
                Assert.assertEquals(prevWidthDelta, widthDelta, TOLERANCE);
            }

            prevCropRegion = cropRegion;
        }
    }

    @UiThreadTest
    @RequiresApi(30)
    @Test
    public void setLinearZoom_androidRZoomRatio_cropWidthChangedLinearly()
            throws InterruptedException {
        assumeTrue(isAndroidRZoomEnabled());
        final float cropWidth = 10000;

        mZoomControl.setLinearZoom(0f);
        HandlerUtil.waitForLooperToIdle(mHandler);
        float zoomRatioForLinearMin = getAndroidRZoomRatio(mControlUpdateCallback);

        float prevCropWidth = cropWidth / zoomRatioForLinearMin;

        float prevWidthDelta = 0;
        for (float percentage = 0.1f; percentage < 1.0f; percentage += 0.1f) {

            mZoomControl.setLinearZoom(percentage);
            HandlerUtil.waitForLooperToIdle(mHandler);
            float zoomRatio = getAndroidRZoomRatio(mControlUpdateCallback);
            float cropWidthForTheRatio = cropWidth / zoomRatio;

            if (prevWidthDelta == 0) {
                prevWidthDelta = prevCropWidth - cropWidthForTheRatio;
            } else {
                float widthDelta = prevCropWidth - cropWidthForTheRatio;
                Assert.assertEquals(prevWidthDelta, widthDelta, TOLERANCE);
            }

            prevCropWidth = cropWidthForTheRatio;
        }
    }

    @UiThreadTest
    @Test
    public void setLinearZoom_largerThan1_zoomUnmodified() {
        mZoomControl.setLinearZoom(0.5f);
        mZoomControl.setLinearZoom(1.1f);
        assertThat(mZoomControl.getZoomState().getValue().getLinearZoom()).isEqualTo(0.5f);
    }

    @Test
    public void setLinearZoom_largerThan1_outOfRangeExeception() {
        ListenableFuture<Void> result = mZoomControl.setLinearZoom(1.1f);
        assertThrowOutOfRangeExceptionOnListenableFuture(result);
    }

    @UiThreadTest
    @Test
    public void setLinearZoom_smallerThan0_zoomUnmodified() {
        mZoomControl.setLinearZoom(0.5f);
        mZoomControl.setLinearZoom(-0.1f);
        assertThat(mZoomControl.getZoomState().getValue().getLinearZoom()).isEqualTo(0.5f);
    }

    @Test
    public void setLinearZoom_smallerThan0_outOfRangeExeception() {
        ListenableFuture<Void> result = mZoomControl.setLinearZoom(-0.1f);
        assertThrowOutOfRangeExceptionOnListenableFuture(result);
    }

    @UiThreadTest
    @Test
    public void getterLiveData_defaultValueIsNonNull() {
        assertThat(mZoomControl.getZoomState().getValue()).isNotNull();
    }

    @UiThreadTest
    @Test
    public void getZoomRatioLiveData_observerIsCalledWhenZoomRatioIsSet()
            throws InterruptedException {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        CountDownLatch latch3 = new CountDownLatch(1);
        FakeLifecycleOwner lifecycleOwner = new FakeLifecycleOwner();
        lifecycleOwner.startAndResume();

        mZoomControl.getZoomState().observe(lifecycleOwner, (value) -> {
            if (value.getZoomRatio() == 1.2f) {
                latch1.countDown();
            } else if (value.getZoomRatio() == 1.5f) {
                latch2.countDown();
            } else if (value.getZoomRatio() == 2.0f) {
                latch3.countDown();
            }
        });

        mZoomControl.setZoomRatio(1.2f);
        mZoomControl.setZoomRatio(1.5f);
        mZoomControl.setZoomRatio(2.0f);

        assertTrue(latch1.await(500, TimeUnit.MILLISECONDS));
        assertTrue(latch2.await(500, TimeUnit.MILLISECONDS));
        assertTrue(latch3.await(500, TimeUnit.MILLISECONDS));
    }

    @UiThreadTest
    @Test
    public void getZoomRatioLiveData_observerIsCalledWhenZoomPercentageIsSet()
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        FakeLifecycleOwner lifecycleOwner = new FakeLifecycleOwner();
        lifecycleOwner.startAndResume();

        mZoomControl.getZoomState().observe(lifecycleOwner, (value) -> {
            if (value.getZoomRatio() != 1.0f) {
                latch.countDown();
            }
        });

        mZoomControl.setLinearZoom(0.1f);
        mZoomControl.setLinearZoom(0.2f);
        mZoomControl.setLinearZoom(0.3f);

        assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
    }

    @UiThreadTest
    @Test
    public void getZoomPercentageLiveData_observerIsCalledWhenZoomPercentageIsSet()
            throws InterruptedException {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        CountDownLatch latch3 = new CountDownLatch(1);
        FakeLifecycleOwner lifecycleOwner = new FakeLifecycleOwner();
        lifecycleOwner.startAndResume();

        mZoomControl.getZoomState().observe(lifecycleOwner, (value) -> {
            if (value.getLinearZoom() == 0.1f) {
                latch1.countDown();
            } else if (value.getLinearZoom() == 0.2f) {
                latch2.countDown();
            } else if (value.getLinearZoom() == 0.3f) {
                latch3.countDown();
            }
        });

        mZoomControl.setLinearZoom(0.1f);
        mZoomControl.setLinearZoom(0.2f);
        mZoomControl.setLinearZoom(0.3f);

        assertTrue(latch1.await(500, TimeUnit.MILLISECONDS));
        assertTrue(latch2.await(500, TimeUnit.MILLISECONDS));
        assertTrue(latch3.await(500, TimeUnit.MILLISECONDS));
    }

    @UiThreadTest
    @Test
    public void getZoomPercentageLiveData_observerIsCalledWhenZoomRatioIsSet()
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        FakeLifecycleOwner lifecycleOwner = new FakeLifecycleOwner();
        lifecycleOwner.startAndResume();

        mZoomControl.getZoomState().observe(lifecycleOwner, (value) -> {
            if (value.getLinearZoom() != 0f) {
                latch.countDown();
            }
        });

        mZoomControl.setZoomRatio(1.2f);
        mZoomControl.setZoomRatio(1.5f);
        mZoomControl.setZoomRatio(2.0f);

        assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
    }

    @UiThreadTest
    @Test
    public void getZoomRatioDefaultValue() {
        assertThat(mZoomControl.getZoomState().getValue().getZoomRatio()).isEqualTo(
                ZoomControl.DEFAULT_ZOOM_RATIO);
    }

    @UiThreadTest
    @Test
    public void getZoomPercentageDefaultValue() {
        assumeFalse(isAndroidRZoomEnabled());
        assertThat(mZoomControl.getZoomState().getValue().getLinearZoom()).isEqualTo(0);
    }

    @UiThreadTest
    @Test
    public void getMaxZoomRatio_isMaxDigitalZoom() {
        float maxZoom = mZoomControl.getZoomState().getValue().getMaxZoomRatio();
        assertThat(maxZoom).isEqualTo(getMaxDigitalZoom());
    }

    @UiThreadTest
    @Test
    public void getMinZoomRatio_isOne() {
        assumeFalse(isAndroidRZoomEnabled());
        float minZoom = mZoomControl.getZoomState().getValue().getMinZoomRatio();
        assertThat(minZoom).isEqualTo(1f);
    }

    private float getMaxDigitalZoom() {
        return mCameraCharacteristics.get(
                CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
    }

    @Test
    public void getMaxZoomRatio_isEqualToMaxDigitalZoom() {
        float maxZoom = mZoomControl.getZoomState().getValue().getMaxZoomRatio();

        assertThat(maxZoom).isEqualTo(getMaxDigitalZoom());
    }

    @UiThreadTest
    @Test
    public void valueIsResetAfterInactive() {
        mZoomControl.setActive(true);
        mZoomControl.setLinearZoom(0.2f); // this will change ratio and percentage.

        mZoomControl.setActive(false);

        assertThat(mZoomControl.getZoomState().getValue().getZoomRatio()).isEqualTo(
                ZoomControl.DEFAULT_ZOOM_RATIO);
    }

    @Test
    public void maxZoomShouldBeLargerThanOrEqualToMinZoom() {
        ZoomState zoomState = mZoomControl.getZoomState().getValue();
        assertThat(zoomState.getMaxZoomRatio()).isAtLeast(zoomState.getMinZoomRatio());
    }
}
