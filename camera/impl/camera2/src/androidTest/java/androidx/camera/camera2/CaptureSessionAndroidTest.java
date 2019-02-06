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
import static org.junit.Assert.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import androidx.camera.camera2.CaptureSession.State;
import androidx.camera.core.CameraCaptureCallback;
import androidx.camera.core.CameraCaptureCallbacks;
import androidx.camera.core.CameraCaptureResult;
import androidx.camera.core.CameraUtil;
import androidx.camera.core.CaptureRequestConfiguration;
import androidx.camera.core.ImmediateSurface;
import androidx.camera.core.SessionConfiguration;
import androidx.test.runner.AndroidJUnit4;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 * Tests for {@link CaptureSession}. This requires an environment where a valid {@link
 * android.hardware.camera2.CameraDevice} can be opened since it is used to open a {@link
 * android.hardware.camera2.CaptureRequest}.
 */
@RunWith(AndroidJUnit4.class)
public class CaptureSessionAndroidTest {
  private CaptureSessionTestParameters testParameters0;
  private CaptureSessionTestParameters testParameters1;

  private CameraDevice cameraDevice;

  /**
   * Collection of parameters required for setting a {@link CaptureSession} and wait for it to
   * produce data.
   */
  private static class CaptureSessionTestParameters {
    /** Thread for all asynchronous calls. */
    private final HandlerThread handlerThread;

    /** Handler for all asynchronous calls. */
    private final Handler handler;

    private static final int TIME_TO_WAIT_FOR_DATA_SECONDS = 3;

    /** Latch to wait for first image data to appear. */
    private final CountDownLatch dataLatch = new CountDownLatch(1);

    /** Latch to wait for camera capture callback to be invoked. */
    private final CountDownLatch cameraCaptureCallbackLatch = new CountDownLatch(1);

    /** Image reader that unlocks the latch waiting for the first image data to appear. */
    private final OnImageAvailableListener onImageAvailableListener =
        reader -> {
          Image image = reader.acquireNextImage();
          if (image != null) {
            image.close();
            dataLatch.countDown();
          }
        };

    private final ImageReader imageReader;
    private final SessionConfiguration sessionConfiguration;
    private final CaptureRequestConfiguration captureRequestConfiguration;

    private final CameraCaptureSession.StateCallback sessionStateCallback =
        Mockito.mock(CameraCaptureSession.StateCallback.class);
    private final CameraCaptureCallback sessionCameraCaptureCallback =
        Mockito.mock(CameraCaptureCallback.class);
    private final CameraCaptureCallback cameraCaptureCallback =
        Mockito.mock(CameraCaptureCallback.class);

    /**
     * A composite capture callback that dispatches callbacks to both mock and real callbacks. The
     * mock callback is used to verify the callback result. The real callback is used to unlock the
     * latch waiting.
     */
    private final CameraCaptureCallback comboCameraCaptureCallback =
        CameraCaptureCallbacks.createComboCallback(
            cameraCaptureCallback,
            new CameraCaptureCallback() {
              @Override
              public void onCaptureCompleted(@NonNull CameraCaptureResult result) {
                cameraCaptureCallbackLatch.countDown();
              }
            });

    CaptureSessionTestParameters(String name) {
      handlerThread = new HandlerThread(name);
      handlerThread.start();
      handler = new Handler(handlerThread.getLooper());

      imageReader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, /*maxImages*/ 2);
      imageReader.setOnImageAvailableListener(onImageAvailableListener, handler);

      SessionConfiguration.Builder builder = new SessionConfiguration.Builder();
      builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
      builder.addSurface(new ImmediateSurface(imageReader.getSurface()));
      builder.setSessionStateCallback(sessionStateCallback);
      builder.setCameraCaptureCallback(sessionCameraCaptureCallback);

      sessionConfiguration = builder.build();

      CaptureRequestConfiguration.Builder captureRequestConfigBuilder =
          new CaptureRequestConfiguration.Builder();
      captureRequestConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
      captureRequestConfigBuilder.addSurface(new ImmediateSurface(imageReader.getSurface()));
      captureRequestConfigBuilder.setCameraCaptureCallback(comboCameraCaptureCallback);

      captureRequestConfiguration = captureRequestConfigBuilder.build();
    }

    /**
     * Wait for data to get produced by the session.
     *
     * @throws InterruptedException if data is not produced after a set amount of time
     */
    void waitForData() throws InterruptedException {
      dataLatch.await(TIME_TO_WAIT_FOR_DATA_SECONDS, TimeUnit.SECONDS);
    }

    void waitForCameraCaptureCallback() throws InterruptedException {
      cameraCaptureCallbackLatch.await(TIME_TO_WAIT_FOR_DATA_SECONDS, TimeUnit.SECONDS);
    }

    /** Clean up resources. */
    void tearDown() {
      imageReader.close();
      handlerThread.quitSafely();
    }
  }

  @Before
  public void setup() throws CameraAccessException, InterruptedException {
    testParameters0 = new CaptureSessionTestParameters("testParameters0");
    testParameters1 = new CaptureSessionTestParameters("testParameters1");
    cameraDevice = CameraUtil.getCameraDevice();
  }

  @After
  public void tearDown() {
    testParameters0.tearDown();
    testParameters1.tearDown();
    CameraUtil.releaseCameraDevice(cameraDevice);
  }

  @Test
  public void setCaptureSessionSucceed() {
    CaptureSession captureSession = new CaptureSession(testParameters0.handler);

    captureSession.setSessionConfiguration(testParameters0.sessionConfiguration);

    assertThat(captureSession.getSessionConfiguration())
        .isEqualTo(testParameters0.sessionConfiguration);
  }

  @Test
  public void setCaptureSessionOnClosedSession_throwsException() {
    CaptureSession captureSession = new CaptureSession(testParameters0.handler);
    SessionConfiguration newSessionConfiguration = testParameters0.sessionConfiguration;

    captureSession.close();

    assertThrows(
        IllegalStateException.class,
        () -> captureSession.setSessionConfiguration(newSessionConfiguration));
  }

  @Test
  public void openCaptureSessionSucceed() throws CameraAccessException, InterruptedException {
    CaptureSession captureSession = new CaptureSession(testParameters0.handler);
    captureSession.setSessionConfiguration(testParameters0.sessionConfiguration);

    captureSession.open(testParameters0.sessionConfiguration, cameraDevice);

    testParameters0.waitForData();

    assertThat(captureSession.getState()).isEqualTo(State.OPENED);

    // StateCallback.onConfigured() should be called to signal the session is configured.
    verify(testParameters0.sessionStateCallback, times(1))
        .onConfigured(any(CameraCaptureSession.class));

    // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
    verify(testParameters0.sessionCameraCaptureCallback, timeout(3000).atLeast(1))
        .onCaptureCompleted(any());
  }

  @Test
  public void closeUnopenedSession() {
    CaptureSession captureSession = new CaptureSession(testParameters0.handler);
    captureSession.setSessionConfiguration(testParameters0.sessionConfiguration);

    captureSession.close();

    assertThat(captureSession.getState()).isEqualTo(State.RELEASED);
  }

  @Test
  public void releaseUnopenedSession() {
    CaptureSession captureSession = new CaptureSession(testParameters0.handler);
    captureSession.setSessionConfiguration(testParameters0.sessionConfiguration);

    captureSession.release();

    assertThat(captureSession.getState()).isEqualTo(State.RELEASED);
  }

  @Test
  public void closeOpenedSession() throws CameraAccessException, InterruptedException {
    CaptureSession captureSession = new CaptureSession(testParameters0.handler);
    captureSession.setSessionConfiguration(testParameters0.sessionConfiguration);
    captureSession.open(testParameters0.sessionConfiguration, cameraDevice);

    captureSession.close();

    Thread.sleep(3000);
    // Session should not get released until triggered by another session opening
    assertThat(captureSession.getState()).isEqualTo(State.CLOSED);
  }

  @Test
  public void releaseOpenedSession() throws CameraAccessException, InterruptedException {
    CaptureSession captureSession = new CaptureSession(testParameters0.handler);
    captureSession.setSessionConfiguration(testParameters0.sessionConfiguration);
    captureSession.open(testParameters0.sessionConfiguration, cameraDevice);
    captureSession.release();

    Thread.sleep(3000);
    assertThat(captureSession.getState()).isEqualTo(State.RELEASED);

    // StateCallback.onClosed() should be called to signal the session is closed.
    verify(testParameters0.sessionStateCallback, times(1))
        .onClosed(any(CameraCaptureSession.class));
  }

  @Test
  public void openSecondSession() throws CameraAccessException, InterruptedException {
    CaptureSession captureSession = new CaptureSession(testParameters0.handler);
    captureSession.setSessionConfiguration(testParameters0.sessionConfiguration);

    // First session is opened
    captureSession.open(testParameters0.sessionConfiguration, cameraDevice);
    captureSession.close();

    // Open second session, which should cause first one to be released
    CaptureSession captureSession1 = new CaptureSession(testParameters1.handler);
    captureSession1.setSessionConfiguration(testParameters1.sessionConfiguration);
    captureSession1.open(testParameters1.sessionConfiguration, cameraDevice);

    testParameters1.waitForData();

    assertThat(captureSession1.getState()).isEqualTo(State.OPENED);
    assertThat(captureSession.getState()).isEqualTo(State.RELEASED);

    // First session should have StateCallback.onConfigured(), onClosed() calls.
    verify(testParameters0.sessionStateCallback, times(1))
        .onConfigured(any(CameraCaptureSession.class));
    verify(testParameters0.sessionStateCallback, times(1))
        .onClosed(any(CameraCaptureSession.class));

    // Second session should have StateCallback.onConfigured() call.
    verify(testParameters1.sessionStateCallback, times(1))
        .onConfigured(any(CameraCaptureSession.class));

    // Second session should have CameraCaptureCallback.onCaptureCompleted() call.
    verify(testParameters1.sessionCameraCaptureCallback, timeout(3000).atLeast(1))
        .onCaptureCompleted(any());
  }

  @Test
  public void issueSingleCaptureRequest() throws CameraAccessException, InterruptedException {
    CaptureSession captureSession = new CaptureSession(testParameters0.handler);
    captureSession.setSessionConfiguration(testParameters0.sessionConfiguration);
    captureSession.open(testParameters0.sessionConfiguration, cameraDevice);

    testParameters0.waitForData();

    assertThat(captureSession.getState()).isEqualTo(State.OPENED);

    captureSession.issueSingleCaptureRequest(testParameters0.captureRequestConfiguration);

    testParameters0.waitForCameraCaptureCallback();

    // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
    verify(testParameters0.cameraCaptureCallback, timeout(3000).times(1)).onCaptureCompleted(any());
  }

  @Test
  public void issueSingleCaptureRequestBeforeCaptureSessionOpened()
      throws CameraAccessException, InterruptedException {
    CaptureSession captureSession = new CaptureSession(testParameters0.handler);
    captureSession.setSessionConfiguration(testParameters0.sessionConfiguration);

    captureSession.issueSingleCaptureRequest(testParameters0.captureRequestConfiguration);
    captureSession.open(testParameters0.sessionConfiguration, cameraDevice);

    testParameters0.waitForCameraCaptureCallback();

    // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
    verify(testParameters0.cameraCaptureCallback, timeout(3000).times(1)).onCaptureCompleted(any());
  }

  @Test
  public void issueSingleCaptureRequestOnClosedSession_throwsException() {
    CaptureSession captureSession = new CaptureSession(testParameters0.handler);

    captureSession.close();

    assertThrows(
        IllegalStateException.class,
        () ->
            captureSession.issueSingleCaptureRequest(testParameters0.captureRequestConfiguration));
  }
}
