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

import static android.os.Looper.getMainLooper;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.CameraControl;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.test.core.app.ApplicationProvider;

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

import java.util.Objects;
import java.util.concurrent.ExecutionException;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
// Not able to write test for Robolectric API 30 because it is not added yet.
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP, maxSdk = Build.VERSION_CODES.Q)
public class ZoomControlTest {
    private static final String CAMERA0_ID = "0";
    private static final String CAMERA1_ID = "1";
    private static final int SENSOR_WIDTH = 640;
    private static final int SENSOR_HEIGHT = 480;
    private static final Rect SENSOR_RECT = new Rect(0, 0, SENSOR_WIDTH, SENSOR_HEIGHT);

    private Camera2CameraControlImpl mCamera2CameraControlImpl;
    private ZoomControl mZoomControl;
    private Camera2CameraControlImpl.CaptureResultListener mCaptureResultListener;

    private static Rect getCropRectByRatio(float ratio) {
        float cropWidth = (SENSOR_RECT.width() / ratio);
        float cropHeight = (SENSOR_RECT.height() / ratio);
        float left = ((SENSOR_RECT.width() - cropWidth) / 2.0f);
        float top = ((SENSOR_RECT.height() - cropHeight) / 2.0f);
        return new Rect((int) left, (int) top, (int) (left + cropWidth),
                (int) (top + cropHeight));
    }

    @Before
    public void setUp() throws CameraAccessException {
        initShadowCameraManager();
        CameraManager cameraManager =
                (CameraManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE);

        CameraCharacteristics cameraCharacteristics =
                cameraManager.getCameraCharacteristics(CAMERA0_ID);
        CameraCharacteristicsCompat characteristicsCompat =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(cameraCharacteristics);

        mCamera2CameraControlImpl = spy(new Camera2CameraControlImpl(characteristicsCompat,
                CameraXExecutors.mainThreadExecutor(), CameraXExecutors.mainThreadExecutor(),
                mock(CameraControlInternal.ControlUpdateCallback.class)));
        mZoomControl = new ZoomControl(mCamera2CameraControlImpl, characteristicsCompat,
                CameraXExecutors.mainThreadExecutor());
        mZoomControl.setActive(true);

        ArgumentCaptor<Camera2CameraControlImpl.CaptureResultListener> argumentCaptor =
                ArgumentCaptor.forClass(Camera2CameraControlImpl.CaptureResultListener.class);
        verify(mCamera2CameraControlImpl).addCaptureResultListener(argumentCaptor.capture());
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

    @Test
    public void setZoomRatio1_whenResultCropRegionIsAlive_ListenableFutureSucceeded()
            throws InterruptedException, ExecutionException {
        ListenableFuture<Void> listenableFuture = mZoomControl.setZoomRatio(1.0f);
        // setZoomRatio can be called from any thread and posts to executor, so idle our executor.
        shadowOf(getMainLooper()).idle();

        TotalCaptureResult result = mockCaptureResult(getCropRectByRatio(1.0f));
        // Calling onCaptureResult directly from executor thread (main thread). No need to idle.
        mCaptureResultListener.onCaptureResult(result);

        assertThat(listenableFuture.isDone()).isTrue();
        // Future should have succeeded. Should not throw.
        listenableFuture.get();
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
            throws InterruptedException, ExecutionException {
        ListenableFuture<Void> listenableFuture = mZoomControl.setZoomRatio(2.0f);
        // setZoomRatio can be called from any thread and posts to executor, so idle our executor.
        shadowOf(getMainLooper()).idle();

        TotalCaptureResult result2 = mockCaptureResult(getCropRectByRatio(2.0f));
        // Calling onCaptureResult directly from executor thread (main thread). No need to idle.
        mCaptureResultListener.onCaptureResult(result2);

        assertThat(listenableFuture.isDone()).isTrue();
        // Future should have succeeded. Should not throw.
        listenableFuture.get();
    }

    @Test
    public void setLinearZoom_valueIsAlive_ListenableFutureSucceeded()
            throws ExecutionException, InterruptedException {
        ListenableFuture<Void> listenableFuture = mZoomControl.setLinearZoom(0.1f);
        // setLinearZoom can be called from any thread and posts to executor, so idle our executor.
        shadowOf(getMainLooper()).idle();

        float targetRatio = Objects.requireNonNull(
                mZoomControl.getZoomState().getValue()).getZoomRatio();
        TotalCaptureResult result = mockCaptureResult(getCropRectByRatio(targetRatio));
        // Calling onCaptureResult directly from executor thread (main thread). No need to idle.
        mCaptureResultListener.onCaptureResult(result);

        assertThat(listenableFuture.isDone()).isTrue();
        // Future should have succeeded. Should not throw.
        listenableFuture.get();
    }

    @Test
    public void setZoomRatio_newRatioIsSet_operationCanceled()
            throws InterruptedException, ExecutionException {
        ListenableFuture<Void> listenableFuture = mZoomControl.setZoomRatio(2.0f);
        ListenableFuture<Void> listenableFuture2 = mZoomControl.setZoomRatio(3.0f);
        ListenableFuture<Void> listenableFuture3 = mZoomControl.setZoomRatio(4.0f);
        // setZoomRatio can be called from any thread and posts to executor, so idle our executor.
        // Since multiple calls to setZoomRatio are posted in order, we only need to idle once to
        // run all of them.
        shadowOf(getMainLooper()).idle();

        TotalCaptureResult result = mockCaptureResult(getCropRectByRatio(4.0f));
        // Calling onCaptureResult directly from executor thread (main thread). No need to idle.
        mCaptureResultListener.onCaptureResult(result);

        assertThat(listenableFuture.isDone()).isTrue();
        assertThat(listenableFuture2.isDone()).isTrue();
        assertThat(listenableFuture3.isDone()).isTrue();
        // Futures 1 and 2 should have failed.
        Throwable t = null;
        try {
            listenableFuture.get();
        } catch (ExecutionException e) {
            t = e.getCause();
        }
        assertThat(t).isInstanceOf(CameraControl.OperationCanceledException.class);

        t = null;
        try {
            listenableFuture2.get();
        } catch (ExecutionException e) {
            t = e.getCause();
        }
        assertThat(t).isInstanceOf(CameraControl.OperationCanceledException.class);

        // Future 3 should have succeeded. Should not throw.
        listenableFuture3.get();
    }

    @Test
    public void setLinearZoom_newPercentageIsSet_operationCanceled()
            throws InterruptedException, ExecutionException {
        ListenableFuture<Void> listenableFuture = mZoomControl.setLinearZoom(0.1f);
        ListenableFuture<Void> listenableFuture2 = mZoomControl.setLinearZoom(0.2f);
        ListenableFuture<Void> listenableFuture3 = mZoomControl.setLinearZoom(0.3f);
        // setZoomRatio can be called from any thread and posts to executor, so idle our executor.
        // Since multiple calls to setZoomRatio are posted in order, we only need to idle once to
        // run all of them.
        shadowOf(getMainLooper()).idle();
        float ratioForPercentage = Objects.requireNonNull(
                mZoomControl.getZoomState().getValue()).getZoomRatio();

        TotalCaptureResult result = mockCaptureResult(getCropRectByRatio(ratioForPercentage));
        // Calling onCaptureResult directly from executor thread (main thread). No need to idle.
        mCaptureResultListener.onCaptureResult(result);

        assertThat(listenableFuture.isDone()).isTrue();
        assertThat(listenableFuture2.isDone()).isTrue();
        assertThat(listenableFuture3.isDone()).isTrue();
        // Futures 1 and 2 should have failed.
        Throwable t = null;
        try {
            listenableFuture.get();
        } catch (ExecutionException e) {
            t = e.getCause();
        }
        assertThat(t).isInstanceOf(CameraControl.OperationCanceledException.class);

        t = null;
        try {
            listenableFuture2.get();
        } catch (ExecutionException e) {
            t = e.getCause();
        }
        assertThat(t).isInstanceOf(CameraControl.OperationCanceledException.class);

        // Future 3 should have succeeded. Should not throw.
        listenableFuture3.get();
    }

    @Test
    public void setZoomRatioAndPercentage_mixedOperation()
            throws InterruptedException, ExecutionException {
        ListenableFuture<Void> listenableFuture = mZoomControl.setZoomRatio(2f);
        ListenableFuture<Void> listenableFuture2 = mZoomControl.setLinearZoom(0.1f);
        ListenableFuture<Void> listenableFuture3 = mZoomControl.setZoomRatio(4f);
        // setZoomRatio/setLinearZoom can be called from any thread and posts to executor, so idle
        // our executor. Since multiple calls to setZoomRatio/setLinearZoom  are posted in order,
        // we only need to idle once to run all of them.
        shadowOf(getMainLooper()).idle();

        TotalCaptureResult result = mockCaptureResult(getCropRectByRatio(4.0f));
        // Calling onCaptureResult directly from executor thread (main thread). No need to idle.
        mCaptureResultListener.onCaptureResult(result);

        assertThat(listenableFuture.isDone()).isTrue();
        assertThat(listenableFuture2.isDone()).isTrue();
        assertThat(listenableFuture3.isDone()).isTrue();
        // Futures 1 and 2 should have failed.
        Throwable t = null;
        try {
            listenableFuture.get();
        } catch (ExecutionException e) {
            t = e.getCause();
        }
        assertThat(t).isInstanceOf(CameraControl.OperationCanceledException.class);

        t = null;
        try {
            listenableFuture2.get();
        } catch (ExecutionException e) {
            t = e.getCause();
        }
        assertThat(t).isInstanceOf(CameraControl.OperationCanceledException.class);

        // Future 3 should have succeeded. Should not throw.
        listenableFuture3.get();
    }

    @Test
    public void setZoomRatio_whenInActive_operationCanceled() throws InterruptedException {
        // setActive() is called from executor thread (main thread in this case). No need to idle.
        mZoomControl.setActive(false);
        ListenableFuture<Void> listenableFuture = mZoomControl.setZoomRatio(1.0f);
        // setZoomRatio can be called from any thread and posts to executor, so idle our executor.
        shadowOf(getMainLooper()).idle();

        assertThat(listenableFuture.isDone()).isTrue();
        Throwable t = null;
        try {
            listenableFuture.get();
        } catch (ExecutionException e) {
            t = e.getCause();
        }
        assertThat(t).isInstanceOf(CameraControl.OperationCanceledException.class);
    }

    @Test
    public void setLinearZoom_whenInActive_operationCanceled() throws InterruptedException {
        // setActive() is called from executor thread (main thread in this case). No need to idle.
        mZoomControl.setActive(false);
        ListenableFuture<Void> listenableFuture = mZoomControl.setLinearZoom(0f);
        // setZoomRatio can be called from any thread and posts to executor, so idle our executor.
        shadowOf(getMainLooper()).idle();

        assertThat(listenableFuture.isDone()).isTrue();
        Throwable t = null;
        try {
            listenableFuture.get();
        } catch (ExecutionException e) {
            t = e.getCause();
        }
        assertThat(t).isInstanceOf(CameraControl.OperationCanceledException.class);
    }

    @Test
    public void setZoomRatio_afterInActive_operationCanceled() throws InterruptedException {
        ListenableFuture<Void> listenableFuture = mZoomControl.setZoomRatio(2.0f);
        // setZoomRatio can be called from any thread and posts to executor, so idle our executor.
        shadowOf(getMainLooper()).idle();
        // setActive() is called from executor thread (main thread in this case). No need to idle.
        mZoomControl.setActive(false);

        assertThat(listenableFuture.isDone()).isTrue();
        Throwable t = null;
        try {
            listenableFuture.get();
        } catch (ExecutionException e) {
            t = e.getCause();
        }
        assertThat(t).isInstanceOf(CameraControl.OperationCanceledException.class);
    }

    @Test
    public void setLinearZoom_afterInActive_operationCanceled() throws InterruptedException {
        ListenableFuture<Void> listenableFuture = mZoomControl.setLinearZoom(0.3f);
        // setZoomRatio can be called from any thread and posts to executor, so idle our executor.
        shadowOf(getMainLooper()).idle();
        // setActive() is called from executor thread (main thread in this case). No need to idle.
        mZoomControl.setActive(false);

        assertThat(listenableFuture.isDone()).isTrue();
        Throwable t = null;
        try {
            listenableFuture.get();
        } catch (ExecutionException e) {
            t = e.getCause();
        }
        assertThat(t).isInstanceOf(CameraControl.OperationCanceledException.class);
    }

    private CameraCharacteristicsCompat getCameraCharacteristicsCompat(String cameraId)
            throws CameraAccessException {
        CameraManager cameraManager =
                (CameraManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE);

        return CameraCharacteristicsCompat.toCameraCharacteristicsCompat(
                cameraManager.getCameraCharacteristics(cameraId));
    }

    @Test
    public void setZoomRatioLargerThan1_WhenZoomNotSupported_zoomIsMin()
            throws CameraAccessException {
        CameraCharacteristicsCompat characteristicsCompat =
                getCameraCharacteristicsCompat(CAMERA1_ID);

        mCamera2CameraControlImpl = new Camera2CameraControlImpl(characteristicsCompat,
                CameraXExecutors.mainThreadExecutor(), CameraXExecutors.mainThreadExecutor(),
                mock(CameraControlInternal.ControlUpdateCallback.class));
        ZoomControl zoomControl = new ZoomControl(mCamera2CameraControlImpl, characteristicsCompat,
                CameraXExecutors.mainThreadExecutor());
        // setActive() is called from executor thread (main thread in this case). No need to idle.
        zoomControl.setActive(true);

        // LiveData is updated synchronously. No need to idle.
        zoomControl.setZoomRatio(3.0f);
        assertThat(Objects.requireNonNull(
                zoomControl.getZoomState().getValue()).getZoomRatio()).isEqualTo(1.0f);
        assertThat(zoomControl.getZoomState().getValue().getLinearZoom()).isEqualTo(0.0f);
    }

    @Test
    public void setZoomRatioSmallerThan1_WhenZoomNotSupported_zoomIsMin()
            throws CameraAccessException {
        CameraCharacteristicsCompat characteristicsCompat =
                getCameraCharacteristicsCompat(CAMERA1_ID);

        mCamera2CameraControlImpl = new Camera2CameraControlImpl(characteristicsCompat,
                CameraXExecutors.mainThreadExecutor(), CameraXExecutors.mainThreadExecutor(),
                mock(CameraControlInternal.ControlUpdateCallback.class));
        ZoomControl zoomControl = new ZoomControl(mCamera2CameraControlImpl, characteristicsCompat,
                CameraXExecutors.mainThreadExecutor());
        // setActive() is called from executor thread (main thread in this case). No need to idle.
        zoomControl.setActive(true);

        // LiveData is updated synchronously. No need to idle.
        zoomControl.setZoomRatio(0.2f);
        assertThat(Objects.requireNonNull(
                zoomControl.getZoomState().getValue()).getZoomRatio()).isEqualTo(1.0f);
        assertThat(zoomControl.getZoomState().getValue().getLinearZoom()).isEqualTo(0.0f);
    }


    @Test
    public void setLinearZoomValidValue_WhenZoomNotSupported_zoomIsMin()
            throws CameraAccessException {
        CameraCharacteristicsCompat characteristicsCompat =
                getCameraCharacteristicsCompat(CAMERA1_ID);

        mCamera2CameraControlImpl = new Camera2CameraControlImpl(characteristicsCompat,
                CameraXExecutors.mainThreadExecutor(), CameraXExecutors.mainThreadExecutor(),
                mock(CameraControlInternal.ControlUpdateCallback.class));
        ZoomControl zoomControl = new ZoomControl(mCamera2CameraControlImpl, characteristicsCompat,
                CameraXExecutors.mainThreadExecutor());
        // setActive() is called from executor thread (main thread in this case). No need to idle.
        zoomControl.setActive(true);

        // LiveData is updated synchronously. No need to idle.
        zoomControl.setLinearZoom(0.4f);
        assertThat(Objects.requireNonNull(
                zoomControl.getZoomState().getValue()).getZoomRatio()).isEqualTo(1.0f);
        // percentage is updated correctly but the zoomRatio is always 1.0f if zoom not supported.
        assertThat(zoomControl.getZoomState().getValue().getLinearZoom()).isEqualTo(0.4f);
    }

    @Test
    public void setLinearZoomSmallerThan0_WhenZoomNotSupported_zoomIsMin()
            throws CameraAccessException {
        CameraCharacteristicsCompat characteristicsCompat =
                getCameraCharacteristicsCompat(CAMERA1_ID);

        mCamera2CameraControlImpl = new Camera2CameraControlImpl(characteristicsCompat,
                CameraXExecutors.mainThreadExecutor(), CameraXExecutors.mainThreadExecutor(),
                mock(CameraControlInternal.ControlUpdateCallback.class));
        ZoomControl zoomControl = new ZoomControl(mCamera2CameraControlImpl, characteristicsCompat,
                CameraXExecutors.mainThreadExecutor());
        // setActive() is called from executor thread (main thread in this case). No need to idle.
        zoomControl.setActive(true);

        // LiveData is updated synchronously. No need to idle.
        zoomControl.setLinearZoom(0.3f);
        zoomControl.setLinearZoom(-0.2f);
        assertThat(Objects.requireNonNull(
                zoomControl.getZoomState().getValue()).getZoomRatio()).isEqualTo(1.0f);
        // percentage not changed but the zoomRatio is always 1.0f if zoom not supported.
        assertThat(zoomControl.getZoomState().getValue().getLinearZoom()).isEqualTo(0.3f);
    }

    @Test
    public void setLinearZoomLargerThan1_WhenZoomNotSupported_zoomIsMin()
            throws CameraAccessException {
        CameraCharacteristicsCompat characteristicsCompat =
                getCameraCharacteristicsCompat(CAMERA1_ID);

        mCamera2CameraControlImpl = new Camera2CameraControlImpl(characteristicsCompat,
                CameraXExecutors.mainThreadExecutor(), CameraXExecutors.mainThreadExecutor(),
                mock(CameraControlInternal.ControlUpdateCallback.class));
        ZoomControl zoomControl = new ZoomControl(mCamera2CameraControlImpl, characteristicsCompat,
                CameraXExecutors.mainThreadExecutor());
        // setActive() is called from executor thread (main thread in this case). No need to idle.
        zoomControl.setActive(true);

        // LiveData is updated synchronously. No need to idle.
        zoomControl.setLinearZoom(0.3f);
        zoomControl.setLinearZoom(1.2f);
        assertThat(Objects.requireNonNull(
                zoomControl.getZoomState().getValue()).getZoomRatio()).isEqualTo(1.0f);
        // percentage not changed but the zoomRatio is always 1.0f if zoom not supported.
        assertThat(zoomControl.getZoomState().getValue().getLinearZoom()).isEqualTo(0.3f);
    }
}
