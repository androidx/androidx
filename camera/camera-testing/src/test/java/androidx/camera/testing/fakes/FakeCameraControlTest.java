/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.fakes;

import static androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.Rect;
import android.os.Build;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureFailure;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.impl.fakes.FakeCameraCaptureResult;
import androidx.camera.testing.impl.mocks.MockScreenFlash;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@org.robolectric.annotation.Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class FakeCameraControlTest {
    private FakeCameraControl mCameraControl;

    @Before
    public void setUp() {
        mCameraControl = new FakeCameraControl();
    }

    @Test
    public void notifiesAllRequestOnCaptureCancelled() {
        CountDownLatch latch = new CountDownLatch(3);
        CaptureConfig captureConfig1 = createCaptureConfig(new CameraCaptureCallback() {
            @Override
            public void onCaptureCancelled(int captureConfigId) {
                latch.countDown();
            }
        }, new CameraCaptureCallback() {
            @Override
            public void onCaptureCancelled(int captureConfigId) {
                latch.countDown();
            }
        });
        CaptureConfig captureConfig2 = createCaptureConfig(new CameraCaptureCallback() {
            @Override
            public void onCaptureCancelled(int captureConfigId) {
                latch.countDown();
            }
        });

        mCameraControl.submitStillCaptureRequests(Arrays.asList(captureConfig1, captureConfig2),
                ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY, ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH);
        mCameraControl.notifyAllRequestsOnCaptureCancelled();

        awaitLatch(latch);
    }

    @Test
    public void notifiesAllRequestOnCaptureFailed() {
        CountDownLatch latch = new CountDownLatch(3);
        List<CameraCaptureFailure> failureList = new ArrayList<>();
        CaptureConfig captureConfig1 = createCaptureConfig(new CameraCaptureCallback() {
            @Override
            public void onCaptureFailed(int captureConfigId,
                    @NonNull CameraCaptureFailure failure) {
                failureList.add(failure);
                latch.countDown();
            }
        }, new CameraCaptureCallback() {
            @Override
            public void onCaptureFailed(int captureConfigId,
                    @NonNull CameraCaptureFailure failure) {
                failureList.add(failure);
                latch.countDown();
            }
        });
        CaptureConfig captureConfig2 = createCaptureConfig(new CameraCaptureCallback() {
            @Override
            public void onCaptureFailed(int captureConfigId,
                    @NonNull CameraCaptureFailure failure) {
                failureList.add(failure);
                latch.countDown();
            }
        });

        mCameraControl.submitStillCaptureRequests(Arrays.asList(captureConfig1, captureConfig2),
                ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY, ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH);
        mCameraControl.notifyAllRequestsOnCaptureFailed();

        awaitLatch(latch);
    }

    @Test
    public void notifiesAllRequestOnCaptureCompleted() {
        CameraCaptureResult captureResult = new FakeCameraCaptureResult();

        CountDownLatch latch = new CountDownLatch(3);
        List<CameraCaptureResult> resultList = new ArrayList<>();
        CaptureConfig captureConfig1 = createCaptureConfig(new CameraCaptureCallback() {
            @Override
            public void onCaptureCompleted(int captureConfigId,
                    @NonNull CameraCaptureResult cameraCaptureResult) {
                resultList.add(cameraCaptureResult);
                latch.countDown();
            }
        }, new CameraCaptureCallback() {
            @Override
            public void onCaptureCompleted(int captureConfigId,
                    @NonNull CameraCaptureResult cameraCaptureResult) {
                resultList.add(cameraCaptureResult);
                latch.countDown();
            }
        });
        CaptureConfig captureConfig2 = createCaptureConfig(new CameraCaptureCallback() {
            @Override
            public void onCaptureCompleted(int captureConfigId,
                    @NonNull CameraCaptureResult cameraCaptureResult) {
                resultList.add(cameraCaptureResult);
                latch.countDown();
            }
        });

        mCameraControl.submitStillCaptureRequests(Arrays.asList(captureConfig1, captureConfig2),
                ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY, ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH);
        mCameraControl.notifyAllRequestsOnCaptureCompleted(captureResult);

        awaitLatch(latch);
        assertThat(resultList).containsExactlyElementsIn(Arrays.asList(captureResult, captureResult,
                captureResult));
    }

    @Test
    public void notifiesLastRequestOnCaptureCompleted() {
        CameraCaptureResult captureResult = new FakeCameraCaptureResult();

        CountDownLatch latch = new CountDownLatch(1);
        List<CameraCaptureResult> resultList = new ArrayList<>();
        CaptureConfig captureConfig1 = createCaptureConfig(new CameraCaptureCallback() {
            @Override
            public void onCaptureCompleted(int captureConfigId,
                    @NonNull CameraCaptureResult cameraCaptureResult) {
                resultList.add(cameraCaptureResult);
            }
        }, new CameraCaptureCallback() {
            @Override
            public void onCaptureCompleted(int captureConfigId,
                    @NonNull CameraCaptureResult cameraCaptureResult) {
                resultList.add(cameraCaptureResult);
            }
        });
        CaptureConfig captureConfig2 = createCaptureConfig(new CameraCaptureCallback() {
            @Override
            public void onCaptureCompleted(int captureConfigId,
                    @NonNull CameraCaptureResult cameraCaptureResult) {
                resultList.add(cameraCaptureResult);
                latch.countDown();
            }
        });

        mCameraControl.submitStillCaptureRequests(Arrays.asList(captureConfig1, captureConfig2),
                ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY, ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH);
        mCameraControl.notifyLastRequestOnCaptureCompleted(captureResult);

        awaitLatch(latch);
        assertThat(resultList).containsExactlyElementsIn(Collections.singletonList(captureResult));
    }

    @Test
    public void canUpdateFlashModeToOff() {
        mCameraControl.setFlashMode(ImageCapture.FLASH_MODE_OFF);
        assertThat(mCameraControl.getFlashMode()).isEqualTo(ImageCapture.FLASH_MODE_OFF);
    }

    @Test
    public void canUpdateFlashModeToOn() {
        mCameraControl.setFlashMode(ImageCapture.FLASH_MODE_ON);
        assertThat(mCameraControl.getFlashMode()).isEqualTo(ImageCapture.FLASH_MODE_ON);
    }

    @Test
    public void canUpdateFlashModeToAuto() {
        mCameraControl.setFlashMode(ImageCapture.FLASH_MODE_AUTO);
        assertThat(mCameraControl.getFlashMode()).isEqualTo(ImageCapture.FLASH_MODE_AUTO);
    }

    @Test
    public void canSetZslDisabledByUserCaseConfig() {
        mCameraControl.setZslDisabledByUserCaseConfig(true);
        assertThat(mCameraControl.isZslDisabledByByUserCaseConfig()).isEqualTo(true);
    }

    @Test
    public void canUnsetZslDisabledByUserCaseConfig_afterSet() {
        mCameraControl.setZslDisabledByUserCaseConfig(true);
        mCameraControl.setZslDisabledByUserCaseConfig(false);
        assertThat(mCameraControl.isZslDisabledByByUserCaseConfig()).isEqualTo(false);
    }

    @Test
    public void canCheckIfZslConfigAdded() {
        mCameraControl.addZslConfig(new SessionConfig.Builder());
        assertThat(mCameraControl.isZslConfigAdded()).isEqualTo(true);
    }

    @Test
    public void canEnableTorch() {
        mCameraControl.enableTorch(true);
        assertThat(mCameraControl.getTorchEnabled()).isEqualTo(true);
    }

    @Test
    public void canDisableTorch_afterEnable() {
        mCameraControl.enableTorch(true);
        mCameraControl.enableTorch(false);
        assertThat(mCameraControl.getTorchEnabled()).isEqualTo(false);
    }

    @Test
    public void canSetScreenFlash() {
        ImageCapture.ScreenFlash screenFlash = new MockScreenFlash();
        mCameraControl.setScreenFlash(screenFlash);
        assertThat(mCameraControl.getScreenFlash()).isEqualTo(screenFlash);
    }

    @Test
    public void canClearScreenFlash_afterEnable() {
        ImageCapture.ScreenFlash screenFlash = new MockScreenFlash();
        mCameraControl.setScreenFlash(screenFlash);
        mCameraControl.setScreenFlash(null);
        assertThat(mCameraControl.getScreenFlash()).isEqualTo(null);
    }

    @Test
    public void futureCompletesImmediately_whenTorchEnabled() {
        ListenableFuture<?> future = mCameraControl.enableTorch(true);
        assertThat(future.isDone()).isTrue();
    }

    @Test
    public void canSetExposureIndex() {
        mCameraControl.setExposureCompensationIndex(25);
        assertThat(mCameraControl.getExposureCompensationIndex()).isEqualTo(25);
    }

    @Test
    public void futureCompletesImmediately_whenExposureCompensationIndexSet() {
        ListenableFuture<?> future = mCameraControl.setExposureCompensationIndex(45);
        assertThat(future.isDone()).isTrue();
    }

    @Test
    public void futureNotImmediatelyCompleted_whenStillCaptureRequestsSubmitted() {
        ListenableFuture<?> future = submitStillCaptureRequests();
        assertThat(future.isDone()).isFalse();
    }

    @Test
    public void futureCompletes_whenStillCaptureRequestsSubmittedAndSuccessNotified() {
        ListenableFuture<?> future = submitStillCaptureRequests();
        mCameraControl.notifyAllRequestsOnCaptureCompleted(new FakeCameraCaptureResult());

        try {
            future.get(3, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void canGetSessionConfig() {
        assertThat(mCameraControl.getSessionConfig()).isInstanceOf(SessionConfig.class);
    }

    @Test
    public void providesSensorRectAccordingToMaxOutputSize() {
        assertThat(mCameraControl.getSensorRect()).isEqualTo(
                new Rect(0, 0, MAX_OUTPUT_SIZE.getWidth(), MAX_OUTPUT_SIZE.getHeight()));
    }

    @Test
    public void canProvideLastFocusMeteringAction() {
        FocusMeteringAction focusMeteringAction = new FocusMeteringAction.Builder(
                new SurfaceOrientedMeteringPointFactory(1f, 1f).createPoint(0f, 0f)).build();
        mCameraControl.startFocusAndMetering(focusMeteringAction);

        assertThat(mCameraControl.getLastSubmittedFocusMeteringAction()).isEqualTo(
                focusMeteringAction);
    }

    @Test
    public void futureCompletesImmediately_forStartFocusAndMetering() {
        ListenableFuture<?> future = mCameraControl.startFocusAndMetering(
                new FocusMeteringAction.Builder(
                        new SurfaceOrientedMeteringPointFactory(1f, 1f).createPoint(0f,
                                0f)).build());

        assertThat(future.isDone()).isTrue();
    }

    @Test
    public void futureCompletesImmediately_forCancelFocusAndMetering() {
        ListenableFuture<?> future = mCameraControl.cancelFocusAndMetering();
        assertThat(future.isDone()).isTrue();
    }

    @Test
    public void captureRequestListenerNotified_whenStillCaptureRequestSubmitted() {
        List<CaptureConfig> notifiedCaptureConfigs = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        mCameraControl.addOnNewCaptureRequestListener(captureConfigs -> {
            notifiedCaptureConfigs.addAll(captureConfigs);
            latch.countDown();
        });
        CaptureConfig captureConfig = createCaptureConfig();
        submitStillCaptureRequests(captureConfig);

        awaitLatch(latch);
        assertThat(notifiedCaptureConfigs).containsExactly(captureConfig);
    }

    @Test
    public void captureRequestListenerNotifiedInCurrentThread_whenExecutorNotSet() {
        AtomicReference<Thread> listenerThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        mCameraControl.addOnNewCaptureRequestListener(captureConfigs -> {
            listenerThread.set(Thread.currentThread());
            latch.countDown();
        });
        submitStillCaptureRequests();

        awaitLatch(latch);
        assertThat(listenerThread.get()).isEqualTo(Thread.currentThread());
    }

    @Test
    public void captureRequestListenerNotifiedInMainThread_whenExecutorSetToMainThread() {
        AtomicReference<Thread> listenerThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        mCameraControl.addOnNewCaptureRequestListener(CameraXExecutors.mainThreadExecutor(),
                captureConfigs -> {
                    listenerThread.set(Thread.currentThread());
                    latch.countDown();
                });
        submitStillCaptureRequests();

        ShadowLooper.runUiThreadTasks();
        awaitLatch(latch);
        assertThat(listenerThread.get()).isEqualTo(Looper.getMainLooper().getThread());
    }

    @Test
    public void controlUpdateCallbackNotifiedInCurrentThread_whenExecutorNotSet() {
        AtomicReference<Thread> callbackThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        mCameraControl = new FakeCameraControl(
                new CameraControlInternal.ControlUpdateCallback() {
                    @Override
                    public void onCameraControlUpdateSessionConfig() {

                    }

                    @Override
                    public void onCameraControlCaptureRequests(
                            @NonNull List<CaptureConfig> captureConfigs) {
                        callbackThread.set(Thread.currentThread());
                        latch.countDown();
                    }
                });
        submitStillCaptureRequests();

        awaitLatch(latch);
        assertThat(callbackThread.get()).isEqualTo(Thread.currentThread());
    }

    @Test
    public void controlUpdateCallbackNotifiedInMainThread_whenExecutorSetToMainThread() {
        AtomicReference<Thread> callbackThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        mCameraControl = new FakeCameraControl(CameraXExecutors.mainThreadExecutor(),
                new CameraControlInternal.ControlUpdateCallback() {
                    @Override
                    public void onCameraControlUpdateSessionConfig() {

                    }

                    @Override
                    public void onCameraControlCaptureRequests(
                            @NonNull List<CaptureConfig> captureConfigs) {
                        callbackThread.set(Thread.currentThread());
                        latch.countDown();
                    }
                });
        submitStillCaptureRequests();

        ShadowLooper.runUiThreadTasks();
        awaitLatch(latch);
        assertThat(callbackThread.get()).isEqualTo(Looper.getMainLooper().getThread());
    }

    @Test
    public void addInteropConfigOverridesSameOption() {
        Config.Option<Integer> option1 = Config.Option.create("OPTION_ID_1", Integer.class);

        MutableOptionsBundle config1 = MutableOptionsBundle.create();
        config1.insertOption(option1, 1);
        mCameraControl.addInteropConfig(config1);

        MutableOptionsBundle config2 = MutableOptionsBundle.create();
        config2.insertOption(option1, 2);
        mCameraControl.addInteropConfig(config2);

        Config finalConfig = mCameraControl.getInteropConfig();
        assertThat(finalConfig.retrieveOption(option1)).isEqualTo(2);
    }

    @Test
    public void addInteropConfigKeepsDifferentOptions() {
        Config.Option<Integer> option1 = Config.Option.create("OPTION_ID_1", Integer.class);
        Config.Option<Integer> option2 = Config.Option.create("OPTION_ID_2", Integer.class);

        MutableOptionsBundle config1 = MutableOptionsBundle.create();
        config1.insertOption(option1, 1);
        mCameraControl.addInteropConfig(config1);

        MutableOptionsBundle config2 = MutableOptionsBundle.create();
        config2.insertOption(option2, 2);
        mCameraControl.addInteropConfig(config2);

        Config finalConfig = mCameraControl.getInteropConfig();
        assertThat(finalConfig.retrieveOption(option1)).isEqualTo(1);
        assertThat(finalConfig.retrieveOption(option2)).isEqualTo(2);
    }

    @Test
    public void canClearInteropConfig() {
        Config.Option<Integer> option1 = Config.Option.create("OPTION_ID_1", Integer.class);

        MutableOptionsBundle config1 = MutableOptionsBundle.create();
        config1.insertOption(option1, 1);
        mCameraControl.addInteropConfig(config1);
        mCameraControl.clearInteropConfig();

        Config finalConfig = mCameraControl.getInteropConfig();
        assertThat(finalConfig.containsOption(option1)).isFalse();
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private ListenableFuture<List<Void>> submitStillCaptureRequests() {
        return mCameraControl.submitStillCaptureRequests(
                Collections.singletonList(createCaptureConfig()),
                ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY,
                ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH);
    }

    private ListenableFuture<List<Void>> submitStillCaptureRequests(CaptureConfig captureConfig) {
        return mCameraControl.submitStillCaptureRequests(
                Collections.singletonList(captureConfig),
                ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY,
                ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH);
    }

    private CaptureConfig createCaptureConfig(CameraCaptureCallback... cameraCaptureCallback) {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();
        builder.addAllCameraCaptureCallbacks(Arrays.asList(cameraCaptureCallback));
        return builder.build();
    }
}
