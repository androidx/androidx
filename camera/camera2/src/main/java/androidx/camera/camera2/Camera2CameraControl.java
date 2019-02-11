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

import android.graphics.Rect;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import android.util.Log;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CaptureRequestConfiguration;
import androidx.camera.core.Configuration;
import androidx.camera.core.FlashMode;
import androidx.camera.core.OnFocusCompletedListener;
import androidx.camera.core.SessionConfiguration;
import java.util.HashSet;
import java.util.Set;

/**
 * A Camera2 implementation for CameraControl interface
 *
 * <p>It takes a {@link Camera2RequestRunner} for executing capture request and a {@link Handler} in
 * which methods run.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class Camera2CameraControl implements CameraControl {
  private static final String TAG = "Camera2CameraControl";

  private final Camera2RequestRunner camera2RequestRunner;
  private final Handler handler;

  // use volatile modifier to make these variables in sync in all threads.
  private volatile boolean isTorchOn = false;
  private volatile boolean isFocusLocked = false;
  private volatile FlashMode flashMode = FlashMode.OFF;
  private volatile Rect cropRect = null;
  private volatile MeteringRectangle afRect;
  private volatile MeteringRectangle aeRect;
  private volatile MeteringRectangle awbRect;

  private final CameraControlSessionCallback sessionCallback = new CameraControlSessionCallback();

  @VisibleForTesting static final long FOCUS_TIMEOUT = 5000;

  public Camera2CameraControl(Camera2RequestRunner camera2RequestRunner, Handler handler) {
    this.camera2RequestRunner = camera2RequestRunner;
    this.handler = handler;
  }

  /** {@inheritDoc} */
  @Override
  public void setCropRegion(Rect crop) {
    if (Looper.myLooper() != handler.getLooper()) {
      handler.post(() -> setCropRegion(crop));
      return;
    }

    cropRect = crop;
    camera2RequestRunner.updateRepeatingRequest();
  }

  private volatile Integer currentAfState = CaptureResult.CONTROL_AF_STATE_INACTIVE;
  private volatile OnFocusCompletedListener focusListener = null;
  private volatile Handler focusListenerHandler = null;
  private volatile CaptureResultListener sessionListenerForFocus = null;

  /** {@inheritDoc} */
  @Override
  public void focus(
      Rect focus,
      Rect metering,
      @Nullable OnFocusCompletedListener listener,
      @Nullable Handler listenerHandler) {
    if (Looper.myLooper() != handler.getLooper()) {
      handler.post(() -> focus(focus, metering, listener, listenerHandler));
      return;
    }

    sessionCallback.removeListener(sessionListenerForFocus);

    handler.removeCallbacks(handleFocusTimeoutRunnable);

    afRect = new MeteringRectangle(focus, MeteringRectangle.METERING_WEIGHT_MAX);
    aeRect = new MeteringRectangle(metering, MeteringRectangle.METERING_WEIGHT_MAX);
    awbRect = new MeteringRectangle(metering, MeteringRectangle.METERING_WEIGHT_MAX);
    Log.d(TAG, "Setting new AF rectangle: " + afRect);
    Log.d(TAG, "Setting new AE rectangle: " + aeRect);
    Log.d(TAG, "Setting new AWB rectangle: " + awbRect);

    focusListener = listener;
    focusListenerHandler =
        (listenerHandler != null ? listenerHandler : new Handler(Looper.getMainLooper()));
    currentAfState = CaptureResult.CONTROL_AF_STATE_INACTIVE;
    isFocusLocked = true;

    if (listener != null) {

      sessionListenerForFocus =
          (result) -> {
            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
            if (afState == null) {
              return false;
            }

            if (currentAfState == CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN) {
              if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED) {
                runInFocusListenerHandler(() -> focusListener.onFocusLocked(afRect.getRect()));
                return true; // finished
              } else if (afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                runInFocusListenerHandler(
                    () -> focusListener.onFocusUnableToLock(afRect.getRect()));
                return true; // finished
              }
            }
            if (!currentAfState.equals(afState)) {
              currentAfState = afState;
            }
            return false; // continue checking
          };

      sessionCallback.addListener(sessionListenerForFocus);
    }
    camera2RequestRunner.updateRepeatingRequest();

    triggerAf();
    if (FOCUS_TIMEOUT != 0) {
      handler.postDelayed(handleFocusTimeoutRunnable, FOCUS_TIMEOUT);
    }
  }

  private void runInFocusListenerHandler(Runnable runnable) {
    if (focusListenerHandler != null) {
      focusListenerHandler.post(runnable);
    }
  }

  private final Runnable handleFocusTimeoutRunnable =
      () -> {
        cancelFocus();

        sessionCallback.removeListener(sessionListenerForFocus);

        if (focusListener != null && currentAfState == CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN) {
          runInFocusListenerHandler(() -> focusListener.onFocusTimedOut(afRect.getRect()));
        }
      };

  /** Cancels the focus operation. */
  @VisibleForTesting
  void cancelFocus() {
    if (Looper.myLooper() != handler.getLooper()) {
      handler.post(() -> cancelFocus());
      return;
    }

    handler.removeCallbacks(handleFocusTimeoutRunnable);

    MeteringRectangle zeroRegion =
        new MeteringRectangle(new Rect(), MeteringRectangle.METERING_WEIGHT_DONT_CARE);
    afRect = zeroRegion;
    aeRect = zeroRegion;
    awbRect = zeroRegion;

    // Send a single request to cancel af process
    CaptureRequestConfiguration.Builder singleRequestBuilder =
        new CaptureRequestConfiguration.Builder();
    singleRequestBuilder.setTemplateType(getDefaultTemplate());
    singleRequestBuilder.setUseRepeatingSurface(true);
    singleRequestBuilder.addCharacteristic(
        CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
    camera2RequestRunner.submitSingleRequest(singleRequestBuilder.build());

    isFocusLocked = false;
    camera2RequestRunner.updateRepeatingRequest();
  }

  /** {@inheritDoc} */
  @Override
  public void setFlashMode(FlashMode flashMode) {
    // update flashMode immediately so that following getFlashMode() returns correct value.
    this.flashMode = flashMode;

    updateRepeatingRequest();
  }

  private void updateRepeatingRequest() {
    if (Looper.myLooper() != handler.getLooper()) {
      handler.post(() -> updateRepeatingRequest());
      return;
    }

    camera2RequestRunner.updateRepeatingRequest();
  }

  @Override
  public FlashMode getFlashMode() {
    return flashMode;
  }

  /** {@inheritDoc} */
  @Override
  public void enableTorch(boolean torch) {
    // update isTorchOn immediately so that following isTorchOn() returns correct value.
    isTorchOn = torch;
    enableTorchInternal(torch);
  }

  private void enableTorchInternal(boolean torch) {
    if (Looper.myLooper() != handler.getLooper()) {
      handler.post(() -> enableTorchInternal(torch));
      return;
    }

    if (!torch) {
      CaptureRequestConfiguration.Builder singleRequestBuilder =
          new CaptureRequestConfiguration.Builder();
      singleRequestBuilder.setTemplateType(getDefaultTemplate());
      singleRequestBuilder.addCharacteristic(
          CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
      singleRequestBuilder.setUseRepeatingSurface(true);

      camera2RequestRunner.submitSingleRequest(singleRequestBuilder.build());
    }
    camera2RequestRunner.updateRepeatingRequest();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isTorchOn() {
    return isTorchOn;
  }

  @Override
  public boolean isFocusLocked() {
    return isFocusLocked;
  }

  /** Issues a {@link CaptureRequest#CONTROL_AF_TRIGGER_START} request to start auto focus scan. */
  @Override
  public void triggerAf() {
    if (Looper.myLooper() != handler.getLooper()) {
      handler.post(() -> triggerAf());
      return;
    }

    CaptureRequestConfiguration.Builder builder = new CaptureRequestConfiguration.Builder();

    builder.setTemplateType(getDefaultTemplate());
    builder.setUseRepeatingSurface(true);
    builder.addCharacteristic(
        CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
    camera2RequestRunner.submitSingleRequest(builder.build());
  }

  /**
   * Issues a {@link CaptureRequest#CONTROL_AE_PRECAPTURE_TRIGGER_START} request to start auto
   * exposure scan.
   */
  @Override
  public void triggerAePrecapture() {
    if (Looper.myLooper() != handler.getLooper()) {
      handler.post(() -> triggerAePrecapture());
      return;
    }

    CaptureRequestConfiguration.Builder builder = new CaptureRequestConfiguration.Builder();
    builder.setTemplateType(getDefaultTemplate());
    builder.setUseRepeatingSurface(true);
    builder.addCharacteristic(
        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
    camera2RequestRunner.submitSingleRequest(builder.build());
  }

  /**
   * Issues {@link CaptureRequest#CONTROL_AF_TRIGGER_CANCEL} or {@link
   * CaptureRequest#CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL} request to cancel auto focus or auto
   * exposure scan.
   */
  @Override
  public void cancelAfAeTrigger(boolean cancelAfTrigger, boolean cancelAePrecaptureTrigger) {
    if (Looper.myLooper() != handler.getLooper()) {
      handler.post(() -> cancelAfAeTrigger(cancelAfTrigger, cancelAePrecaptureTrigger));
      return;
    }
    CaptureRequestConfiguration.Builder builder = new CaptureRequestConfiguration.Builder();
    builder.setUseRepeatingSurface(true);
    builder.setTemplateType(getDefaultTemplate());
    if (cancelAfTrigger) {
      builder.addCharacteristic(
          CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
    }
    if (cancelAePrecaptureTrigger) {
      builder.addCharacteristic(
          CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
          CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL);
    }
    camera2RequestRunner.submitSingleRequest(builder.build());
  }

  private int getDefaultTemplate() {
    return CameraDevice.TEMPLATE_PREVIEW;
  }

  /** {@inheritDoc} */
  @Override
  public SessionConfiguration getControlSessionConfiguration() {
    SessionConfiguration.Builder builder = new SessionConfiguration.Builder();

    builder.setTemplateType(getDefaultTemplate());
    builder.setCameraCaptureCallback(CaptureCallbackContainer.create(sessionCallback));

    Camera2Configuration.Builder requestOptionBuilder = new Camera2Configuration.Builder();

    if (isTorchOn) {
      requestOptionBuilder.setCaptureRequestOption(
          CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
      requestOptionBuilder.setCaptureRequestOption(
          CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
    } else {
      int aeMode = CaptureRequest.CONTROL_AE_MODE_ON;
      switch (flashMode) {
        case OFF:
          aeMode = CaptureRequest.CONTROL_AE_MODE_ON;
          break;
        case ON:
          aeMode = CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH;
          break;
        case AUTO:
          aeMode = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH;
          break;
      }
      requestOptionBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, aeMode);
    }

    // also apply the common option for single requests.
    Configuration singleRequestImpOptions = getSingleRequestImplOptions();
    requestOptionBuilder.insertAllOptions(singleRequestImpOptions);
    builder.setImplementationOptions(requestOptionBuilder.build());

    return builder.build();
  }

  /** {@inheritDoc} */
  @Override
  public Configuration getSingleRequestImplOptions() {
    Camera2Configuration.Builder builder = new Camera2Configuration.Builder();

    builder.setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

    builder.setCaptureRequestOption(
        CaptureRequest.CONTROL_AF_MODE,
        isFocusLocked()
            ? CaptureRequest.CONTROL_AF_MODE_AUTO
            : CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

    if (isTorchOn) {
      // In case some random single request turns off the torch by accident, attach FLASH_MODE and
      // CONTROL_AE_MODE_ON to all single requests.
      builder.setCaptureRequestOption(
          CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
      builder.setCaptureRequestOption(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
    }
    // Turning off Flash requires a single request of AE mode set to CONTROL_AE_MODE_ON. This is
    // the reason why we do not specify AE mode by default for single request.

    builder.setCaptureRequestOption(
        CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);

    if (afRect != null) {
      builder.setCaptureRequestOption(
          CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[] {afRect});
    }
    if (aeRect != null) {
      builder.setCaptureRequestOption(
          CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[] {aeRect});
    }
    if (awbRect != null) {
      builder.setCaptureRequestOption(
          CaptureRequest.CONTROL_AWB_REGIONS, new MeteringRectangle[] {awbRect});
    }

    if (cropRect != null) {
      builder.setCaptureRequestOption(CaptureRequest.SCALER_CROP_REGION, cropRect);
    }

    return builder.build();
  }

  private static final class CameraControlSessionCallback extends CaptureCallback {

    private final Set<CaptureResultListener> resultListeners = new HashSet<>();

    public void addListener(CaptureResultListener listener) {
      synchronized (resultListeners) {
        resultListeners.add(listener);
      }
    }

    public void removeListener(CaptureResultListener listener) {
      if (listener == null) {
        return;
      }
      synchronized (resultListeners) {
        resultListeners.remove(listener);
      }
    }

    @Override
    public void onCaptureCompleted(
        @NonNull CameraCaptureSession session,
        @NonNull CaptureRequest request,
        @NonNull TotalCaptureResult result) {
      Set<CaptureResultListener> listeners;
      synchronized (resultListeners) {
        if (resultListeners.isEmpty()) {
          return;
        }
        listeners = new HashSet<>(resultListeners);
      }

      Set<CaptureResultListener> removeSet = new HashSet<>();
      for (CaptureResultListener listener : listeners) {
        boolean isFinished = listener.onCaptureResult(result);
        if (isFinished) {
          removeSet.add(listener);
        }
      }

      if (!removeSet.isEmpty()) {
        synchronized (resultListeners) {
          resultListeners.removeAll(removeSet);
        }
      }
    }
  }

  /** An interface to listen to camera capture results. */
  private interface CaptureResultListener {
    /**
     * Callback to handle camera capture results.
     *
     * @param captureResult camera capture result.
     * @return true to finish listening, false to continue listening.
     */
    boolean onCaptureResult(TotalCaptureResult captureResult);
  }
}
