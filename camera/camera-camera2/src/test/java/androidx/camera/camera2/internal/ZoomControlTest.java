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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraControl;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.shadows.ShadowCameraManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class ZoomControlTest {
    private static final String CAMERA0_ID = "0";
    private static final String CAMERA1_ID = "1";
    private static final int SENSOR_WIDTH = 640;
    private static final int SENSOR_HEIGHT = 480;
    private static final Rect SENSOR_RECT = new Rect(0, 0, SENSOR_WIDTH, SENSOR_HEIGHT);

    private Camera2CameraControl mCamera2CameraControl;
    private ZoomControl mZoomControl;
    private Camera2CameraControl.CaptureResultListener mCaptureResultListener;

    @Before
    public void setUp() throws CameraAccessException {
        initShadowCameraManager();
        CameraManager cameraManager =
                (CameraManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE);

        CameraCharacteristics cameraCharacteristics =
                cameraManager.getCameraCharacteristics(
                        CAMERA0_ID);

        mCamera2CameraControl = spy(new Camera2CameraControl(cameraCharacteristics,
                CameraXExecutors.mainThreadExecutor(), CameraXExecutors.mainThreadExecutor(),
                mock(CameraControlInternal.ControlUpdateCallback.class)));
        mZoomControl = new ZoomControl(mCamera2CameraControl, cameraCharacteristics);
        mZoomControl.setActive(true);

        ArgumentCaptor<Camera2CameraControl.CaptureResultListener> argumentCaptor =
                ArgumentCaptor.forClass(Camera2CameraControl.CaptureResultListener.class);
        verify(mCamera2CameraControl).addCaptureResultListener(argumentCaptor.capture());
        mCaptureResultListener = argumentCaptor.getValue();
    }

    private void initShadowCameraManager() {
        // **** Camera 0 characteristics ****//
        CameraCharacteristics characteristics0 =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics0 = Shadow.extract(characteristics0);

        shadowCharacteristics0.set(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE,
                SENSOR_RECT);

        shadowCharacteristics0.set(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM, 7.0f);

        ((ShadowCameraManager)
                Shadow.extract(
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CAMERA_SERVICE)))
                .addCamera(CAMERA0_ID, characteristics0);

        CameraCharacteristics characteristics1 =
                ShadowCameraCharacteristics.newCameraCharacteristics();
        ShadowCameraCharacteristics shadowCharacteristics1 = Shadow.extract(characteristics1);

        shadowCharacteristics1.set(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE,
                new Rect(0, 0, SENSOR_WIDTH, SENSOR_HEIGHT));

        shadowCharacteristics1.set(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM, 1.0f);
        ((ShadowCameraManager)
                Shadow.extract(
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CAMERA_SERVICE)))
                .addCamera(CAMERA1_ID, characteristics1);
    }

    private static Rect getCropRectByRatio(float ratio) {
        return ZoomControl.getCropRectByRatio(SENSOR_RECT, ratio);
    }

    @Test
    public void setZoomRatio1_whenResultCropRegionIsAlive_ListenableFutureSucceeded()
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ListenableFuture<Void> listenableFuture = mZoomControl.setZoomRatio(1.0f);

        TotalCaptureResult result = mockCaptureResult(getCropRectByRatio(1.0f));
        mCaptureResultListener.onCaptureResult(result);

        Futures.addCallback(listenableFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable t) {

            }
        }, CameraXExecutors.mainThreadExecutor());

        assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
    }

    @NonNull
    private TotalCaptureResult mockCaptureResult(Rect cropRectByRatio) {
        TotalCaptureResult result = mock(TotalCaptureResult.class);
        CaptureRequest captureRequest = mock(CaptureRequest.class);
        when(result.getRequest()).thenReturn(captureRequest);
        when(captureRequest.get(CaptureRequest.SCALER_CROP_REGION)).thenReturn(cropRectByRatio);
        return result;
    }

    @Test
    public void setZoomRatioOtherThan1_whenResultCropRegionIsAlive_ListenableFutureSucceeded()
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ListenableFuture<Void> listenableFuture = mZoomControl.setZoomRatio(2.0f);

        TotalCaptureResult result2 = mockCaptureResult(getCropRectByRatio(2.0f));
        mCaptureResultListener.onCaptureResult(result2);

        Futures.addCallback(listenableFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable t) {

            }
        }, CameraXExecutors.mainThreadExecutor());

        assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    public void setLinearZoom_valueIsAlive_ListenableFutureSucceeded()
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ListenableFuture<Void> listenableFuture = mZoomControl.setLinearZoom(0.1f);

        float targetRatio = mZoomControl.getZoomState().getValue().getZoomRatio();
        TotalCaptureResult result = mockCaptureResult(getCropRectByRatio(targetRatio));
        mCaptureResultListener.onCaptureResult(result);

        Futures.addCallback(listenableFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable t) {

            }
        }, CameraXExecutors.mainThreadExecutor());

        assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    public void setZoomRatio_newRatioIsSet_operationCanceled() throws InterruptedException {
        CountDownLatch latchForOp1Canceled = new CountDownLatch(1);
        CountDownLatch latchForOp2Canceled = new CountDownLatch(1);
        CountDownLatch latchForOp3Succeeded = new CountDownLatch(1);
        ListenableFuture<Void> listenableFuture = mZoomControl.setZoomRatio(2.0f);
        ListenableFuture<Void> listenableFuture2 = mZoomControl.setZoomRatio(3.0f);
        ListenableFuture<Void> listenableFuture3 = mZoomControl.setZoomRatio(4.0f);

        TotalCaptureResult result = mockCaptureResult(getCropRectByRatio(4.0f));
        mCaptureResultListener.onCaptureResult(result);

        Futures.addCallback(listenableFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
            }

            @Override
            public void onFailure(Throwable t) {
                if (t instanceof CameraControl.OperationCanceledException) {
                    latchForOp1Canceled.countDown();
                }
            }
        }, CameraXExecutors.mainThreadExecutor());

        Futures.addCallback(listenableFuture2, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
            }

            @Override
            public void onFailure(Throwable t) {
                if (t instanceof CameraControl.OperationCanceledException) {
                    latchForOp2Canceled.countDown();
                }
            }
        }, CameraXExecutors.mainThreadExecutor());

        Futures.addCallback(listenableFuture3, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                latchForOp3Succeeded.countDown();
            }

            @Override
            public void onFailure(Throwable t) {
            }
        }, CameraXExecutors.mainThreadExecutor());

        assertTrue(latchForOp1Canceled.await(500, TimeUnit.MILLISECONDS));
        assertTrue(latchForOp2Canceled.await(500, TimeUnit.MILLISECONDS));
        assertTrue(latchForOp3Succeeded.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    public void setLinearZoom_newPercentageIsSet_operationCanceled()
            throws InterruptedException {
        CountDownLatch latchForOp1Canceled = new CountDownLatch(1);
        CountDownLatch latchForOp2Canceled = new CountDownLatch(1);
        CountDownLatch latchForOp3Succeeded = new CountDownLatch(1);
        ListenableFuture<Void> listenableFuture = mZoomControl.setLinearZoom(0.1f);
        ListenableFuture<Void> listenableFuture2 = mZoomControl.setLinearZoom(0.2f);
        ListenableFuture<Void> listenableFuture3 = mZoomControl.setLinearZoom(0.3f);
        float ratioForPercentage = mZoomControl.getZoomState().getValue().getZoomRatio();

        TotalCaptureResult result = mockCaptureResult(getCropRectByRatio(ratioForPercentage));
        mCaptureResultListener.onCaptureResult(result);

        Futures.addCallback(listenableFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
            }

            @Override
            public void onFailure(Throwable t) {
                if (t instanceof CameraControl.OperationCanceledException) {
                    latchForOp1Canceled.countDown();
                }
            }
        }, CameraXExecutors.mainThreadExecutor());

        Futures.addCallback(listenableFuture2, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
            }

            @Override
            public void onFailure(Throwable t) {
                if (t instanceof CameraControl.OperationCanceledException) {
                    latchForOp2Canceled.countDown();
                }
            }
        }, CameraXExecutors.mainThreadExecutor());

        Futures.addCallback(listenableFuture3, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                latchForOp3Succeeded.countDown();
            }

            @Override
            public void onFailure(Throwable t) {
            }
        }, CameraXExecutors.mainThreadExecutor());

        assertTrue(latchForOp1Canceled.await(500, TimeUnit.MILLISECONDS));
        assertTrue(latchForOp2Canceled.await(500, TimeUnit.MILLISECONDS));
        assertTrue(latchForOp3Succeeded.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    public void setZoomRatioAndPercentage_mixedOperation() throws InterruptedException {
        CountDownLatch latchForOp1Canceled = new CountDownLatch(1);
        CountDownLatch latchForOp2Canceled = new CountDownLatch(1);
        CountDownLatch latchForOp3Succeeded = new CountDownLatch(1);
        ListenableFuture<Void> listenableFuture = mZoomControl.setZoomRatio(2f);
        ListenableFuture<Void> listenableFuture2 = mZoomControl.setLinearZoom(0.1f);
        ListenableFuture<Void> listenableFuture3 = mZoomControl.setZoomRatio(4f);

        TotalCaptureResult result = mockCaptureResult(getCropRectByRatio(4.0f));
        mCaptureResultListener.onCaptureResult(result);

        Futures.addCallback(listenableFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
            }

            @Override
            public void onFailure(Throwable t) {
                if (t instanceof CameraControl.OperationCanceledException) {
                    latchForOp1Canceled.countDown();
                }
            }
        }, CameraXExecutors.mainThreadExecutor());

        Futures.addCallback(listenableFuture2, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
            }

            @Override
            public void onFailure(Throwable t) {
                if (t instanceof CameraControl.OperationCanceledException) {
                    latchForOp2Canceled.countDown();
                }
            }
        }, CameraXExecutors.mainThreadExecutor());

        Futures.addCallback(listenableFuture3, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                latchForOp3Succeeded.countDown();
            }

            @Override
            public void onFailure(Throwable t) {
            }
        }, CameraXExecutors.mainThreadExecutor());

        assertTrue(latchForOp1Canceled.await(500, TimeUnit.MILLISECONDS));
        assertTrue(latchForOp2Canceled.await(500, TimeUnit.MILLISECONDS));
        assertTrue(latchForOp3Succeeded.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    public void setZoomRatio_whenInActive_operationCanceled() {
        mZoomControl.setActive(false);
        ListenableFuture<Void> listenableFuture = mZoomControl.setZoomRatio(0.5f);

        try {
            listenableFuture.get(1000, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof CameraControl.OperationCanceledException) {
                assertTrue(true);
                return;
            }
        } catch (Exception e) {
        }

        fail();
    }

    @Test
    public void setLinearZoom_whenInActive_operationCanceled() {
        mZoomControl.setActive(false);
        ListenableFuture<Void> listenableFuture = mZoomControl.setLinearZoom(0f);

        try {
            listenableFuture.get(1000, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof CameraControl.OperationCanceledException) {
                assertTrue(true);
                return;
            }
        } catch (Exception e) {
        }

        fail();
    }

    @Test
    public void setZoomRatio_afterInActive_operationCanceled() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ListenableFuture<Void> listenableFuture = mZoomControl.setZoomRatio(2.0f);
        Futures.addCallback(listenableFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
            }

            @Override
            public void onFailure(Throwable t) {
                if (t instanceof CameraControl.OperationCanceledException) {
                    latch.countDown();
                }
            }
        }, CameraXExecutors.mainThreadExecutor());

        mZoomControl.setActive(false);

        assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    public void setLinearZoom_afterInActive_operationCanceled() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ListenableFuture<Void> listenableFuture = mZoomControl.setLinearZoom(0.3f);
        Futures.addCallback(listenableFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
            }

            @Override
            public void onFailure(Throwable t) {
                if (t instanceof CameraControl.OperationCanceledException) {
                    latch.countDown();
                }
            }
        }, CameraXExecutors.mainThreadExecutor());

        mZoomControl.setActive(false);

        assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    public void setZoomRatioLargerThan1_WhenZoomNotSupported_zoomIsMin()
            throws CameraAccessException {
        CameraManager cameraManager =
                (CameraManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE);

        CameraCharacteristics cameraCharacteristics =
                cameraManager.getCameraCharacteristics(CAMERA1_ID);

        mCamera2CameraControl = new Camera2CameraControl(cameraCharacteristics,
                CameraXExecutors.mainThreadExecutor(), CameraXExecutors.mainThreadExecutor(),
                mock(CameraControlInternal.ControlUpdateCallback.class));
        ZoomControl zoomControl = new ZoomControl(mCamera2CameraControl, cameraCharacteristics);
        zoomControl.setActive(true);

        zoomControl.setZoomRatio(3.0f);
        assertThat(zoomControl.getZoomState().getValue().getZoomRatio()).isEqualTo(1.0f);
        assertThat(zoomControl.getZoomState().getValue().getLinearZoom()).isEqualTo(0.0f);
    }

    @Test
    public void setZoomRatioSmallerThan1_WhenZoomNotSupported_zoomIsMin()
            throws CameraAccessException {
        CameraManager cameraManager =
                (CameraManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE);

        CameraCharacteristics cameraCharacteristics =
                cameraManager.getCameraCharacteristics(CAMERA1_ID);

        mCamera2CameraControl = new Camera2CameraControl(cameraCharacteristics,
                CameraXExecutors.mainThreadExecutor(), CameraXExecutors.mainThreadExecutor(),
                mock(CameraControlInternal.ControlUpdateCallback.class));
        ZoomControl zoomControl = new ZoomControl(mCamera2CameraControl, cameraCharacteristics);
        zoomControl.setActive(true);

        zoomControl.setZoomRatio(0.2f);
        assertThat(zoomControl.getZoomState().getValue().getZoomRatio()).isEqualTo(1.0f);
        assertThat(zoomControl.getZoomState().getValue().getLinearZoom()).isEqualTo(0.0f);
    }


    @Test
    public void setLinearZoomValidValue_WhenZoomNotSupported_zoomIsMin()
            throws CameraAccessException {
        CameraManager cameraManager =
                (CameraManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE);

        CameraCharacteristics cameraCharacteristics =
                cameraManager.getCameraCharacteristics(CAMERA1_ID);

        mCamera2CameraControl = new Camera2CameraControl(cameraCharacteristics,
                CameraXExecutors.mainThreadExecutor(), CameraXExecutors.mainThreadExecutor(),
                mock(CameraControlInternal.ControlUpdateCallback.class));
        ZoomControl zoomControl = new ZoomControl(mCamera2CameraControl, cameraCharacteristics);
        zoomControl.setActive(true);

        zoomControl.setLinearZoom(0.4f);
        assertThat(zoomControl.getZoomState().getValue().getZoomRatio()).isEqualTo(1.0f);
        // percentage is updated correctly but the zoomRatio is always 1.0f if zoom not supported.
        assertThat(zoomControl.getZoomState().getValue().getLinearZoom()).isEqualTo(0.4f);
    }

    @Test
    public void setLinearZoomSmallerThan0_WhenZoomNotSupported_zoomIsMin()
            throws CameraAccessException {
        CameraManager cameraManager =
                (CameraManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE);

        CameraCharacteristics cameraCharacteristics =
                cameraManager.getCameraCharacteristics(CAMERA1_ID);

        mCamera2CameraControl = new Camera2CameraControl(cameraCharacteristics,
                CameraXExecutors.mainThreadExecutor(), CameraXExecutors.mainThreadExecutor(),
                mock(CameraControlInternal.ControlUpdateCallback.class));
        ZoomControl zoomControl = new ZoomControl(mCamera2CameraControl, cameraCharacteristics);
        zoomControl.setActive(true);

        zoomControl.setLinearZoom(0.3f);
        zoomControl.setLinearZoom(-0.2f);
        assertThat(zoomControl.getZoomState().getValue().getZoomRatio()).isEqualTo(1.0f);
        // percentage not changed but the zoomRatio is always 1.0f if zoom not supported.
        assertThat(zoomControl.getZoomState().getValue().getLinearZoom()).isEqualTo(0.3f);
    }

    @Test
    public void setLinearZoomLargerThan1_WhenZoomNotSupported_zoomIsMin()
            throws CameraAccessException {
        CameraManager cameraManager =
                (CameraManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE);

        CameraCharacteristics cameraCharacteristics =
                cameraManager.getCameraCharacteristics(CAMERA1_ID);

        mCamera2CameraControl = new Camera2CameraControl(cameraCharacteristics,
                CameraXExecutors.mainThreadExecutor(), CameraXExecutors.mainThreadExecutor(),
                mock(CameraControlInternal.ControlUpdateCallback.class));
        ZoomControl zoomControl = new ZoomControl(mCamera2CameraControl, cameraCharacteristics);
        zoomControl.setActive(true);

        zoomControl.setLinearZoom(0.3f);
        zoomControl.setLinearZoom(1.2f);
        assertThat(zoomControl.getZoomState().getValue().getZoomRatio()).isEqualTo(1.0f);
        // percentage not changed but the zoomRatio is always 1.0f if zoom not supported.
        assertThat(zoomControl.getZoomState().getValue().getLinearZoom()).isEqualTo(0.3f);
    }
}
