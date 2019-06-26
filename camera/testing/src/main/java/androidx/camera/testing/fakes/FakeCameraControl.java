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

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraControlInternal;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.FlashMode;
import androidx.camera.core.OnFocusListener;
import androidx.camera.core.SessionConfig;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * A fake implementation for the CameraControlInternal interface.
 */
public final class FakeCameraControl implements CameraControlInternal {
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
    public void setCropRegion(@Nullable final Rect crop) {
        Log.d(TAG, "setCropRegion(" + crop + ")");
    }

    @SuppressLint("LambdaLast") // Remove after https://issuetracker.google.com/135275901
    @Override
    public void focus(
            @NonNull final Rect focus,
            @NonNull final Rect metering,
            @NonNull final Executor listenerExecutor,
            @NonNull final OnFocusListener listener) {
        focus(focus, metering);
    }

    @Override
    public void focus(@NonNull Rect focus, @NonNull Rect metering) {
        Log.d(TAG, "focus(\n    " + focus + ",\n    " + metering + ")");
    }

    @NonNull
    @Override
    public FlashMode getFlashMode() {
        return mFlashMode;
    }

    @Override
    public void setFlashMode(@NonNull FlashMode flashMode) {
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
    public void submitCaptureRequests(@NonNull List<CaptureConfig> captureConfigs) {
        mControlUpdateListener.onCameraControlCaptureRequests(captureConfigs);
    }

    private void updateSessionConfig() {
        mControlUpdateListener.onCameraControlUpdateSessionConfig(mSessionConfigBuilder.build());
    }
}
