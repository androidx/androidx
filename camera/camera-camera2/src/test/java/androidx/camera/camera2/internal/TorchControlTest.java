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
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.robolectric.Shadows.shadowOf;

import static kotlinx.coroutines.test.TestCoroutineDispatchersKt.UnconfinedTestDispatcher;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.CameraControl;
import androidx.camera.core.TorchState;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.testing.TestLifecycleOwner;
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

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class TorchControlTest {
    private static final String CAMERA0_ID = "0";
    private static final String CAMERA1_ID = "1";

    private TorchControl mNoFlashUnitTorchControl;
    private TorchControl mTorchControl;
    private Camera2CameraControlImpl.CaptureResultListener mCaptureResultListener;
    private TestLifecycleOwner mLifecycleOwner;

    @Before
    public void setUp() throws CameraAccessException {
        initShadowCameraManager();
        CameraManager cameraManager =
                (CameraManager) ApplicationProvider.getApplicationContext()
                        .getSystemService(Context.CAMERA_SERVICE);

        /* Prepare CameraControl 0 which flash is unavailable */
        CameraCharacteristics cameraCharacteristics0 =
                cameraManager.getCameraCharacteristics(CAMERA0_ID);

        CameraCharacteristicsCompat characteristicsCompat0 =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(cameraCharacteristics0);
        Camera2CameraControlImpl camera2CameraControl0 =
                spy(new Camera2CameraControlImpl(characteristicsCompat0,
                        CameraXExecutors.mainThreadExecutor(),
                        CameraXExecutors.mainThreadExecutor(),
                        mock(CameraControlInternal.ControlUpdateCallback.class)));
        mNoFlashUnitTorchControl = new TorchControl(camera2CameraControl0, characteristicsCompat0,
                CameraXExecutors.mainThreadExecutor());
        mNoFlashUnitTorchControl.setActive(true);

        /* Prepare CameraControl 1 which flash is available */
        CameraCharacteristics cameraCharacteristics1 =
                cameraManager.getCameraCharacteristics(CAMERA1_ID);
        CameraCharacteristicsCompat characteristicsCompat1 =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(cameraCharacteristics1);

        Camera2CameraControlImpl camera2CameraControlImpl1 =
                spy(new Camera2CameraControlImpl(characteristicsCompat1,
                        CameraXExecutors.mainThreadExecutor(),
                        CameraXExecutors.mainThreadExecutor(),
                        mock(CameraControlInternal.ControlUpdateCallback.class)));
        mTorchControl = new TorchControl(camera2CameraControlImpl1, characteristicsCompat1,
                CameraXExecutors.mainThreadExecutor());
        mTorchControl.setActive(true);

        ArgumentCaptor<Camera2CameraControlImpl.CaptureResultListener> argumentCaptor =
                ArgumentCaptor.forClass(Camera2CameraControlImpl.CaptureResultListener.class);
        verify(camera2CameraControlImpl1).addCaptureResultListener(argumentCaptor.capture());
        mCaptureResultListener = argumentCaptor.getValue();

        /* Prepare Lifecycle for test LiveData */
        mLifecycleOwner = new TestLifecycleOwner(Lifecycle.State.STARTED,
                UnconfinedTestDispatcher(null, null));
    }

    @Test
    public void enableTorch_whenNoFlashUnit() throws InterruptedException {
        Throwable cause = null;
        try {
            // Without a flash unit, this future will complete immediately. No need to idle.
            mNoFlashUnitTorchControl.enableTorch(true).get();
        } catch (ExecutionException e) {
            // The real cause is wrapped in ExecutionException, retrieve it and check.
            cause = e.getCause();
        }
        assertThat(cause).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void getTorchState_whenNoFlashUnit() {
        int torchState =
                Objects.requireNonNull(mNoFlashUnitTorchControl.getTorchState().getValue());
        assertThat(torchState).isEqualTo(TorchState.OFF);
    }

    @Test
    public void enableTorch_whenInactive() throws InterruptedException {
        mTorchControl.setActive(false);
        ListenableFuture<Void> listenableFuture = mTorchControl.enableTorch(true);
        // enableTorch can be called from any thread and posts to executor, so idle our executor.
        shadowOf(getMainLooper()).idle();

        assertThat(listenableFuture.isDone()).isTrue();
        Throwable cause = null;
        try {
            listenableFuture.get();
        } catch (ExecutionException e) {
            // The real cause is wrapped in ExecutionException, retrieve it and check.
            cause = e.getCause();
        }

        assertThat(cause).isInstanceOf(CameraControl.OperationCanceledException.class);
    }

    @Test
    public void getTorchState_whenInactive() {
        mTorchControl.setActive(false);
        // LiveData is updated synchronously. No need to idle.
        int torchState = Objects.requireNonNull(mTorchControl.getTorchState().getValue());

        assertThat(torchState).isEqualTo(TorchState.OFF);
    }

    @Test
    public void enableTorch_torchStateOn() {
        mTorchControl.enableTorch(true);
        // LiveData is updated synchronously. No need to idle.
        int torchState = Objects.requireNonNull(mTorchControl.getTorchState().getValue());

        assertThat(torchState).isEqualTo(TorchState.ON);
    }

    @Test
    public void disableTorch_TorchStateOff() {
        mTorchControl.enableTorch(true);
        // LiveData is updated synchronously. No need to idle.
        int firstTorchState = Objects.requireNonNull(mTorchControl.getTorchState().getValue());

        mTorchControl.enableTorch(false);
        // LiveData is updated synchronously. No need to idle.
        int secondTorchState = mTorchControl.getTorchState().getValue();

        assertThat(firstTorchState).isEqualTo(TorchState.ON);
        assertThat(secondTorchState).isEqualTo(TorchState.OFF);
    }

    @Test
    public void enableDisableTorch_futureWillCompleteSuccessfully()
            throws ExecutionException, InterruptedException {
        ListenableFuture<Void> future = mTorchControl.enableTorch(true);
        // enableTorch can be called from any thread and posts to executor, so idle our executor.
        shadowOf(getMainLooper()).idle();

        // Calling onCaptureResult directly from executor thread (main thread). No need to idle.
        mCaptureResultListener.onCaptureResult(
                mockFlashCaptureResult(CaptureResult.FLASH_MODE_TORCH));
        // Future should return with no exception
        future.get();

        future = mTorchControl.enableTorch(false);
        // enableTorch can be called from any thread and posts to executor, so idle our executor.
        shadowOf(getMainLooper()).idle();
        // Calling onCaptureResult directly from executor thread (main thread). No need to idle.
        mCaptureResultListener.onCaptureResult(
                mockFlashCaptureResult(CaptureResult.FLASH_MODE_OFF));
        // Future should return with no exception
        future.get();
    }

    @Test
    public void enableTorchTwice_cancelPreviousFuture() throws InterruptedException {
        ListenableFuture<Void> future = mTorchControl.enableTorch(true);
        mTorchControl.enableTorch(true);
        // enableTorch can be called from any thread and posts to executor, so idle our executor.
        shadowOf(getMainLooper()).idle();
        Throwable cause = null;
        try {
            future.get();
        } catch (ExecutionException e) {
            // The real cause is wrapped in ExecutionException, retrieve it and check.
            cause = e.getCause();
        }

        assertThat(cause).isInstanceOf(CameraControl.OperationCanceledException.class);
    }

    @Test
    public void setInActive_cancelPreviousFuture() throws InterruptedException {
        ListenableFuture<Void> future = mTorchControl.enableTorch(true);
        // enableTorch can be called from any thread and posts to executor, so idle our executor.
        shadowOf(getMainLooper()).idle();
        // setActive() is called from executor thread (main thread in this case). No need to idle.
        mTorchControl.setActive(false);
        Throwable cause = null;
        try {
            future.get();
        } catch (ExecutionException e) {
            // The real cause is wrapped in ExecutionException, retrieve it and check.
            cause = e.getCause();
        }

        assertThat(cause).isInstanceOf(CameraControl.OperationCanceledException.class);
    }

    @Test
    public void setInActiveWhenTorchOn_changeToTorchOff() {
        mTorchControl.enableTorch(true);
        // enableTorch can be called from any thread and posts to executor, so idle our executor.
        shadowOf(getMainLooper()).idle();
        int initialTorchState = Objects.requireNonNull(mTorchControl.getTorchState().getValue());

        // setActive() is called from executor thread (main thread in this case). No need to idle.
        mTorchControl.setActive(false);
        int torchStateAfterInactive = mTorchControl.getTorchState().getValue();

        assertThat(initialTorchState).isEqualTo(TorchState.ON);
        assertThat(torchStateAfterInactive).isEqualTo(TorchState.OFF);
    }

    @Test
    public void enableDisableTorch_observeTorchStateLiveData() {
        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        LiveData<Integer> torchStateLiveData = mTorchControl.getTorchState();
        // Adding observer from main thread should synchronously be notified of initial state
        torchStateLiveData.observe(mLifecycleOwner, new Observer<Integer>() {
            private Integer mValue;
            @Override
            public void onChanged(Integer value) {
                if (!Objects.equals(mValue, value)) {
                    observer.onChanged(mValue = value);
                }
            }
        });

        mTorchControl.enableTorch(true);
        // Idle the main thread to receive first update
        shadowOf(getMainLooper()).idle();

        mTorchControl.enableTorch(false);
        // Idle the main thread to receive second update
        shadowOf(getMainLooper()).idle();

        ArgumentCaptor<Integer> torchStateCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(observer, times(3)).onChanged(torchStateCaptor.capture());

        List<Integer> torchStates = torchStateCaptor.getAllValues();

        assertThat(torchStates.get(0)).isEqualTo(TorchState.OFF); // initial state
        assertThat(torchStates.get(1)).isEqualTo(TorchState.ON);  // by enableTorch(true)
        assertThat(torchStates.get(2)).isEqualTo(TorchState.OFF); // by enableTorch(false)
    }

    private void initShadowCameraManager() {
        // **** Camera 0 characteristics ****//
        CameraCharacteristics characteristics0 =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics0 = Shadow.extract(characteristics0);

        shadowCharacteristics0.set(CameraCharacteristics.FLASH_INFO_AVAILABLE, false);

        ((ShadowCameraManager)
                Shadow.extract(
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CAMERA_SERVICE)))
                .addCamera(CAMERA0_ID, characteristics0);

        // **** Camera 1 characteristics ****//
        CameraCharacteristics characteristics1 =
                ShadowCameraCharacteristics.newCameraCharacteristics();
        ShadowCameraCharacteristics shadowCharacteristics1 = Shadow.extract(characteristics1);

        shadowCharacteristics1.set(CameraCharacteristics.FLASH_INFO_AVAILABLE, true);

        ((ShadowCameraManager)
                Shadow.extract(
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CAMERA_SERVICE)))
                .addCamera(CAMERA1_ID, characteristics1);
    }

    private TotalCaptureResult mockFlashCaptureResult(int flashMode) {
        TotalCaptureResult result = mock(TotalCaptureResult.class);
        CaptureRequest captureRequest = mock(CaptureRequest.class);
        when(result.getRequest()).thenReturn(captureRequest);
        when(captureRequest.get(CaptureRequest.FLASH_MODE)).thenReturn(flashMode);
        return result;
    }
}
