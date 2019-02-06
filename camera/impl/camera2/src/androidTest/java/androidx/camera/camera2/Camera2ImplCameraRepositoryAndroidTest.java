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

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraDevice;
import android.util.Size;
import android.view.Surface;
import androidx.camera.camera2.SemaphoreReleasingCamera2Callbacks.DeviceStateCallback;
import androidx.camera.camera2.SemaphoreReleasingCamera2Callbacks.SessionStateCallback;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraRepository;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.FakeUseCase;
import androidx.camera.core.FakeUseCaseConfiguration;
import androidx.camera.core.ImmediateSurface;
import androidx.camera.core.SessionConfiguration;
import androidx.camera.core.UseCaseGroup;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Contains tests for {@link androidx.camera.core.CameraRepository} which require an actual
 * implementation to run.
 */
@RunWith(AndroidJUnit4.class)
public final class Camera2ImplCameraRepositoryAndroidTest {
  private CameraRepository cameraRepository;
  private UseCaseGroup useCaseGroup;
  private FakeUseCaseConfiguration configuration;
  private CallbackAttachingFakeUseCase useCase;
  private CameraFactory cameraFactory;

  private String getCameraIdForLensFacingUnchecked(LensFacing lensFacing) {
    try {
      return cameraFactory.cameraIdForLensFacing(lensFacing);
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Unable to attach to camera with LensFacing " + lensFacing, e);
    }
  }

  @Before
  public void setUp() {
    cameraRepository = new CameraRepository();
    cameraFactory = new Camera2CameraFactory(ApplicationProvider.getApplicationContext());
    cameraRepository.init(cameraFactory);
    useCaseGroup = new UseCaseGroup();
    configuration = new FakeUseCaseConfiguration.Builder().setLensFacing(LensFacing.BACK).build();
    String cameraId = getCameraIdForLensFacingUnchecked(configuration.getLensFacing());
    useCase = new CallbackAttachingFakeUseCase(configuration, cameraId);
    useCaseGroup.addUseCase(useCase);
  }

  @Test(timeout = 5000)
  public void cameraDeviceCallsAreForwardedToCallback() throws InterruptedException {
    cameraRepository.onGroupActive(useCaseGroup);

    // Wait for the CameraDevice.onOpened callback.
    useCase.deviceStateCallback.waitForOnOpened(1);

    cameraRepository.onGroupInactive(useCaseGroup);

    // Wait for the CameraDevice.onClosed callback.
    useCase.deviceStateCallback.waitForOnClosed(1);
  }

  @Test(timeout = 5000)
  public void cameraSessionCallsAreForwardedToCallback() throws InterruptedException {
    useCase.addStateChangeListener(
        cameraRepository.getCamera(
            getCameraIdForLensFacingUnchecked(configuration.getLensFacing())));
    useCase.doNotifyActive();
    cameraRepository.onGroupActive(useCaseGroup);

    // Wait for the CameraCaptureSession.onConfigured callback.
    useCase.sessionStateCallback.waitForOnConfigured(1);

    // Camera doesn't currently call CaptureSession.release(), because it is recommended that
    // we don't explicitly call CameraCaptureSession.close(). Rather, we rely on another
    // CameraCaptureSession to get opened. See
    // https://developer.android.com/reference/android/hardware/camera2/CameraCaptureSession.html#close()
  }

  /** A fake use case which attaches to a camera with various callbacks. */
  private static class CallbackAttachingFakeUseCase extends FakeUseCase {
    private final DeviceStateCallback deviceStateCallback = new DeviceStateCallback();
    private final SessionStateCallback sessionStateCallback = new SessionStateCallback();
    private final SurfaceTexture surfaceTexture = new SurfaceTexture(0);

    CallbackAttachingFakeUseCase(FakeUseCaseConfiguration configuration, String cameraId) {
      super(configuration);

      SessionConfiguration.Builder builder = new SessionConfiguration.Builder();
      builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
      builder.addSurface(new ImmediateSurface(new Surface(surfaceTexture)));
      builder.setDeviceStateCallback(deviceStateCallback);
      builder.setSessionStateCallback(sessionStateCallback);

      attachToCamera(cameraId, builder.build());
    }

    @Override
    protected Map<String, Size> onSuggestedResolutionUpdated(
        Map<String, Size> suggestedResolutionMap) {
      return suggestedResolutionMap;
    }

    void doNotifyActive() {
      super.notifyActive();
    }
  }
}
