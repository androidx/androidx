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

import androidx.camera.core.BaseCamera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.UseCase;

import java.util.Collection;

/** A fake camera which will not produce any data. */
public class FakeCamera implements BaseCamera {
    private final CameraControl mCameraControl;

    private final CameraInfo mCameraInfo;

    public FakeCamera() {
        this(new FakeCameraInfo(), CameraControl.DEFAULT_EMPTY_INSTANCE);
    }

    public FakeCamera(CameraInfo cameraInfo, CameraControl cameraControl) {
        mCameraInfo = cameraInfo;
        mCameraControl = cameraControl;
    }

    @Override
    public void open() {
    }

    @Override
    public void close() {
    }

    @Override
    public void release() {
    }

    @Override
    public void addOnlineUseCase(Collection<UseCase> useCases) {
    }

    @Override
    public void removeOnlineUseCase(Collection<UseCase> useCases) {
    }

    @Override
    public void onUseCaseActive(UseCase useCase) {
    }

    @Override
    public void onUseCaseInactive(UseCase useCase) {
    }

    @Override
    public void onUseCaseUpdated(UseCase useCase) {
    }

    @Override
    public void onUseCaseReset(UseCase useCase) {
    }

    // Returns fixed CameraControl instance in order to verify the instance is correctly attached.
    @Override
    public CameraControl getCameraControl() {
        return mCameraControl;
    }

    @Override
    public CameraInfo getCameraInfo() {
        return mCameraInfo;
    }

    @Override
    public void onCameraControlUpdateSessionConfig(SessionConfig sessionConfig) {
    }

    @Override
    public void onCameraControlSingleRequest(CaptureConfig captureConfig) {
    }
}
