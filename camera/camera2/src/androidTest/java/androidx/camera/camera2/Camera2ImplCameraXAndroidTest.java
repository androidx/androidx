/*
 * Copyright (C) 2019 The Android Open Source Project
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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.hardware.camera2.CameraDevice;
import android.os.HandlerThread;
import androidx.camera.camera2.SemaphoreReleasingCamera2Callbacks.DeviceStateCallback;
import androidx.camera.camera2.SemaphoreReleasingCamera2Callbacks.SessionCaptureCallback;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImageAnalysisUseCase;
import androidx.camera.core.ImageAnalysisUseCaseConfiguration;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 * Contains tests for {@link androidx.camera.core.CameraX} which require an actual implementation to
 * run.
 */
@RunWith(AndroidJUnit4.class)
public final class Camera2ImplCameraXAndroidTest {
  private static final LensFacing DEFAULT_LENS_FACING = LensFacing.BACK;

  private FakeLifecycleOwner lifecycle;
  private final MutableLiveData<Long> analysisResult = new MutableLiveData<>();
  private final ImageAnalysisUseCase.Analyzer imageAnalyzer =
      (image, rotationDegrees) -> {
        analysisResult.postValue(image.getTimestamp());
      };

  private HandlerThread handlerThread;

  private CameraDevice.StateCallback mockStateCallback;

  @Before
  public void setUp() {
    Context context = ApplicationProvider.getApplicationContext();
    CameraX.init(context, Camera2AppConfiguration.create(context));
    lifecycle = new FakeLifecycleOwner();
    handlerThread = new HandlerThread("ErrorHandlerThread");
    handlerThread.start();
    mockStateCallback = Mockito.mock(CameraDevice.StateCallback.class);
  }

  @After
  public void tearDown() throws InterruptedException {
    CameraX.unbindAll();
    handlerThread.quitSafely();

    // Wait some time for the cameras to close. We need the cameras to close to bring CameraX back
    // to the initial state.
    Thread.sleep(3000);
  }

  @Test
  public void lifecycleResume_opensCameraAndStreamsFrames() throws InterruptedException {
    ImageAnalysisUseCaseConfiguration configuration =
        new ImageAnalysisUseCaseConfiguration.Builder().setLensFacing(DEFAULT_LENS_FACING).build();
    ImageAnalysisUseCase useCase = new ImageAnalysisUseCase(configuration);
    CameraX.bindToLifecycle(lifecycle, useCase);
    final AtomicLong observedCount = new AtomicLong(0);
    useCase.setAnalyzer(imageAnalyzer);
    analysisResult.observe(lifecycle, createCountIncrementingObserver(observedCount));

    lifecycle.startAndResume();

    // Wait a little bit for the camera to open and stream frames.
    Thread.sleep(5000);

    // Some frames should have been observed.
    assertThat(observedCount.get()).isAtLeast(10L);
  }

  @Test
  public void removedUseCase_doesNotStreamWhenLifecycleResumes() throws InterruptedException {
    ImageAnalysisUseCaseConfiguration configuration =
        new ImageAnalysisUseCaseConfiguration.Builder().setLensFacing(DEFAULT_LENS_FACING).build();
    ImageAnalysisUseCase useCase = new ImageAnalysisUseCase(configuration);
    CameraX.bindToLifecycle(lifecycle, useCase);
    final AtomicLong observedCount = new AtomicLong(0);
    useCase.setAnalyzer(imageAnalyzer);
    analysisResult.observe(lifecycle, createCountIncrementingObserver(observedCount));
    assertThat(observedCount.get()).isEqualTo(0);

    CameraX.unbind(useCase);

    lifecycle.startAndResume();

    // Wait a little bit for the camera to open and stream frames.
    Thread.sleep(5000);

    // No frames should have been observed.
    assertThat(observedCount.get()).isEqualTo(0);
  }

  @Test
  public void lifecyclePause_closesCameraAndStopsStreamingFrames() throws InterruptedException {
    ImageAnalysisUseCaseConfiguration.Builder configurationBuilder =
        new ImageAnalysisUseCaseConfiguration.Builder().setLensFacing(DEFAULT_LENS_FACING);
    DeviceStateCallback deviceStateCallback = new DeviceStateCallback();
    SessionCaptureCallback sessionCaptureCallback = new SessionCaptureCallback();
    new Camera2Configuration.Extender(configurationBuilder)
        .setDeviceStateCallback(deviceStateCallback)
        .setSessionCaptureCallback(sessionCaptureCallback);
    ImageAnalysisUseCase useCase = new ImageAnalysisUseCase(configurationBuilder.build());
    CameraX.bindToLifecycle(lifecycle, useCase);
    final AtomicLong observedCount = new AtomicLong(0);
    useCase.setAnalyzer(imageAnalyzer);
    analysisResult.observe(lifecycle, createCountIncrementingObserver(observedCount));

    lifecycle.startAndResume();

    // Wait a little bit for the camera to open and stream frames.
    sessionCaptureCallback.waitForOnCaptureCompleted(5);

    lifecycle.pauseAndStop();

    // Wait a little bit for the camera to close.
    deviceStateCallback.waitForOnClosed(1);

    final Long firstObservedCount = observedCount.get();
    assertThat(firstObservedCount).isGreaterThan(1L);

    // Stay in idle state for a while.
    Thread.sleep(5000);

    // Additional frames should not be observed.
    final Long secondObservedCount = observedCount.get();
    assertThat(secondObservedCount).isEqualTo(firstObservedCount);
  }

  @Test
  public void bind_opensCamera() {
    ImageAnalysisUseCaseConfiguration.Builder builder =
        new ImageAnalysisUseCaseConfiguration.Builder().setLensFacing(DEFAULT_LENS_FACING);
    new Camera2Configuration.Extender(builder).setDeviceStateCallback(mockStateCallback);
    ImageAnalysisUseCaseConfiguration configuration = builder.build();
    ImageAnalysisUseCase useCase = new ImageAnalysisUseCase(configuration);
    CameraX.bindToLifecycle(lifecycle, useCase);
    lifecycle.startAndResume();

    verify(mockStateCallback, timeout(3000)).onOpened(any(CameraDevice.class));
  }

  @Test
  public void unbindAll_closesAllCameras() {
    ImageAnalysisUseCaseConfiguration.Builder builder =
        new ImageAnalysisUseCaseConfiguration.Builder().setLensFacing(DEFAULT_LENS_FACING);
    new Camera2Configuration.Extender(builder).setDeviceStateCallback(mockStateCallback);
    ImageAnalysisUseCaseConfiguration configuration = builder.build();
    ImageAnalysisUseCase useCase = new ImageAnalysisUseCase(configuration);
    CameraX.bindToLifecycle(lifecycle, useCase);
    lifecycle.startAndResume();

    CameraX.unbindAll();

    verify(mockStateCallback, timeout(3000)).onClosed(any(CameraDevice.class));
  }

  @Test
  public void unbindAllAssociatedUseCase_closesCamera() {
    ImageAnalysisUseCaseConfiguration.Builder builder =
        new ImageAnalysisUseCaseConfiguration.Builder().setLensFacing(DEFAULT_LENS_FACING);
    new Camera2Configuration.Extender(builder).setDeviceStateCallback(mockStateCallback);
    ImageAnalysisUseCaseConfiguration configuration = builder.build();
    ImageAnalysisUseCase useCase = new ImageAnalysisUseCase(configuration);
    CameraX.bindToLifecycle(lifecycle, useCase);
    lifecycle.startAndResume();

    CameraX.unbind(useCase);

    verify(mockStateCallback, timeout(3000)).onClosed(any(CameraDevice.class));
  }

  @Test
  public void unbindPartialAssociatedUseCase_doesNotCloseCamera() throws InterruptedException {
    ImageAnalysisUseCaseConfiguration.Builder builder =
        new ImageAnalysisUseCaseConfiguration.Builder().setLensFacing(DEFAULT_LENS_FACING);
    new Camera2Configuration.Extender(builder).setDeviceStateCallback(mockStateCallback);
    ImageAnalysisUseCaseConfiguration configuration0 = builder.build();
    ImageAnalysisUseCase useCase0 = new ImageAnalysisUseCase(configuration0);

    ImageAnalysisUseCaseConfiguration configuration1 =
        new ImageAnalysisUseCaseConfiguration.Builder().setLensFacing(DEFAULT_LENS_FACING).build();
    ImageAnalysisUseCase useCase1 = new ImageAnalysisUseCase(configuration1);

    CameraX.bindToLifecycle(lifecycle, useCase0, useCase1);
    lifecycle.startAndResume();

    CameraX.unbind(useCase1);

    Thread.sleep(3000);

    verify(mockStateCallback, never()).onClosed(any(CameraDevice.class));
  }

  @Test
  public void unbindAllAssociatedUseCaseInParts_ClosesCamera() {
    ImageAnalysisUseCaseConfiguration.Builder builder =
        new ImageAnalysisUseCaseConfiguration.Builder().setLensFacing(DEFAULT_LENS_FACING);
    new Camera2Configuration.Extender(builder).setDeviceStateCallback(mockStateCallback);
    ImageAnalysisUseCaseConfiguration configuration0 = builder.build();
    ImageAnalysisUseCase useCase0 = new ImageAnalysisUseCase(configuration0);

    ImageAnalysisUseCaseConfiguration configuration1 =
        new ImageAnalysisUseCaseConfiguration.Builder().setLensFacing(DEFAULT_LENS_FACING).build();
    ImageAnalysisUseCase useCase1 = new ImageAnalysisUseCase(configuration1);

    CameraX.bindToLifecycle(lifecycle, useCase0, useCase1);
    lifecycle.startAndResume();

    CameraX.unbind(useCase0);
    CameraX.unbind(useCase1);

    verify(mockStateCallback, timeout(3000).times(1)).onClosed(any(CameraDevice.class));
  }

  private static Observer<Long> createCountIncrementingObserver(final AtomicLong counter) {
    return value -> {
      counter.incrementAndGet();
    };
  }
}
