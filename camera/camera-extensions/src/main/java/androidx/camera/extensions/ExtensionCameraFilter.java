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

package androidx.camera.extensions;

import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraFilter;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.Identifier;
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.PreviewExtenderImpl;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * A filter that filters camera based on extender implementation. If the implementation is
 * unavailable, the camera will be considered available.
 */
final class ExtensionCameraFilter implements CameraFilter {
    private final Identifier mId;
    private final PreviewExtenderImpl mPreviewExtenderImpl;
    private final ImageCaptureExtenderImpl mImageCaptureExtenderImpl;

    ExtensionCameraFilter(@NonNull String filterId,
            @NonNull PreviewExtenderImpl previewExtenderImpl,
            @NonNull ImageCaptureExtenderImpl imageCaptureExtenderImpl) {
        mId = Identifier.create(filterId);
        mPreviewExtenderImpl = previewExtenderImpl;
        mImageCaptureExtenderImpl = imageCaptureExtenderImpl;
    }

    @NonNull
    @Override
    public Identifier getIdentifier() {
        return mId;
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    @NonNull
    @Override
    public List<CameraInfo> filter(@NonNull List<CameraInfo> cameraInfos) {
        List<CameraInfo> result = new ArrayList<>();
        for (CameraInfo cameraInfo : cameraInfos) {
            Preconditions.checkArgument(cameraInfo instanceof CameraInfoInternal,
                    "The camera info doesn't contain internal implementation.");
            String cameraId = Camera2CameraInfo.from(cameraInfo).getCameraId();
            CameraCharacteristics cameraCharacteristics =
                    Camera2CameraInfo.extractCameraCharacteristics(cameraInfo);

            if (mPreviewExtenderImpl.isExtensionAvailable(cameraId, cameraCharacteristics)
                    && mImageCaptureExtenderImpl.isExtensionAvailable(cameraId,
                    cameraCharacteristics)) {
                result.add(cameraInfo);
            }
        }

        return result;
    }
}
