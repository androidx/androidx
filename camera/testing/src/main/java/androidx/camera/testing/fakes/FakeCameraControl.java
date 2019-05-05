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

package androidx.camera.testing.fakes;

import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.FlashMode;
import androidx.camera.core.OnFocusListener;
import androidx.camera.core.SessionConfig;

import java.util.List;

/**
 * A fake implementation for the CameraControl interface.
 */
public final class FakeCameraControl implements CameraControl {
    private static final String TAG = "FakeCameraControl";
    private final ControlUpdateListener mControlUpdateListener;
    private final SessionConfig.Builder mSessionConfigBuilder = new SessionConfig.Builder();
    private boolean mIsTorchOn = false;
    private FlashMode mFlashMode = FlashMode.OFF;

    public FakeCameraControl(ControlUpdateListener controlUpdateListener) {
        mControlUpdateListener = controlUpdateListener;
        updateSessionConfig();
    }

    @Override
    public void setCropRegion(final Rect crop) {
        Log.d(TAG, "setCropRegion(" + crop + ")");
    }

    @Override
    public void focus(
            final Rect focus,
            final Rect metering,
            @Nullable final OnFocusListener listener,
            @Nullable final Handler listenerHandler) {
        Log.d(TAG, "focus(\n    " + focus + ",\n    " + metering + ")");
    }

    @Override
    public void focus(Rect focus, Rect metering) {
        focus(focus, metering, null, null);
    }

    @Override
    public FlashMode getFlashMode() {
        return mFlashMode;
    }

    @Override
    public void setFlashMode(FlashMode flashMode) {
        mFlashMode = flashMode;
        Log.d(TAG, "setFlashMode(" + mFlashMode + ")");
    }

    @Override
    public void enableTorch(boolean torch) {
        mIsTorchOn = torch;
        Log.d(TAG, "enableTorch(" + torch + ")");
    }

    @Override
    public boolean isTorchOn() {
        return mIsTorchOn;
    }

    @Override
    public boolean isFocusLocked() {
        return false;
    }

    @Override
    public void triggerAf() {
        Log.d(TAG, "triggerAf()");
    }

    @Override
    public void triggerAePrecapture() {
        Log.d(TAG, "triggerAePrecapture()");
    }

    @Override
    public void cancelAfAeTrigger(final boolean cancelAfTrigger,
            final boolean cancelAePrecaptureTrigger) {
        Log.d(TAG, "cancelAfAeTrigger(" + cancelAfTrigger + ", "
                + cancelAePrecaptureTrigger + ")");
    }

    @Override
    public void submitCaptureRequests(List<CaptureConfig> captureConfigs) {
        mControlUpdateListener.onCameraControlCaptureRequests(captureConfigs);
    }

    private void updateSessionConfig() {
        mControlUpdateListener.onCameraControlUpdateSessionConfig(mSessionConfigBuilder.build());
    }
}
