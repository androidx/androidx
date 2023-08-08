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
package androidx.camera.core.streamsharing;

import static androidx.core.util.Preconditions.checkArgument;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.ForwardingCameraControl;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

/**
 * A {@link CameraControlInternal} that is used to control the virtual camera.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class VirtualCameraControl extends ForwardingCameraControl {

    private static final int DEFAULT_JPEG_QUALITY = 100;
    private static final int DEFAULT_ROTATION_DEGREES = 0;

    private final StreamSharing.Control mStreamSharingControl;

    VirtualCameraControl(@NonNull CameraControlInternal parent,
            @NonNull StreamSharing.Control streamSharingControl) {
        super(parent);
        mStreamSharingControl = streamSharingControl;
    }

    @NonNull
    @Override
    public ListenableFuture<List<Void>> submitStillCaptureRequests(
            @NonNull List<CaptureConfig> captureConfigs,
            @ImageCapture.CaptureMode int captureMode,
            @ImageCapture.FlashType int flashType) {
        checkArgument(captureConfigs.size() == 1, "Only support one capture config.");
        return Futures.allAsList(singletonList(mStreamSharingControl.jpegSnapshot(
                getJpegQuality(captureConfigs.get(0)),
                getRotationDegrees(captureConfigs.get(0)))));
    }

    private int getJpegQuality(@NonNull CaptureConfig captureConfig) {
        return requireNonNull(captureConfig.getImplementationOptions().retrieveOption(
                CaptureConfig.OPTION_JPEG_QUALITY, DEFAULT_JPEG_QUALITY));
    }

    private int getRotationDegrees(@NonNull CaptureConfig captureConfig) {
        return requireNonNull(captureConfig.getImplementationOptions().retrieveOption(
                CaptureConfig.OPTION_ROTATION, DEFAULT_ROTATION_DEGREES));
    }
}
