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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.CameraIdFilter;
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.PreviewExtenderImpl;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Filter camera id by extender implementation. If the implementation is null, all the camera ids
 * will be considered available.
 */
public final class ExtensionCameraIdFilter implements CameraIdFilter {
    private PreviewExtenderImpl mPreviewExtenderImpl;
    private ImageCaptureExtenderImpl mImageCaptureExtenderImpl;

    ExtensionCameraIdFilter(@Nullable PreviewExtenderImpl previewExtenderImpl) {
        mPreviewExtenderImpl = previewExtenderImpl;
        mImageCaptureExtenderImpl = null;
    }

    ExtensionCameraIdFilter(@Nullable ImageCaptureExtenderImpl imageCaptureExtenderImpl) {
        mPreviewExtenderImpl = null;
        mImageCaptureExtenderImpl = imageCaptureExtenderImpl;
    }

    ExtensionCameraIdFilter(@Nullable PreviewExtenderImpl previewExtenderImpl,
            @Nullable ImageCaptureExtenderImpl imageCaptureExtenderImpl) {
        mPreviewExtenderImpl = previewExtenderImpl;
        mImageCaptureExtenderImpl = imageCaptureExtenderImpl;
    }

    @Override
    @NonNull
    public Set<String> filter(@NonNull Set<String> cameraIdSet) {
        Set<String> resultCameraIdSet = new LinkedHashSet<>();
        for (String cameraId : cameraIdSet) {
            boolean available = true;

            // If preview extender impl isn't null, check if the camera id is supported.
            if (mPreviewExtenderImpl != null) {
                available = mPreviewExtenderImpl.isExtensionAvailable(cameraId,
                        CameraUtil.getCameraCharacteristics(cameraId));
            }

            // If image capture extender impl isn't null, check if the camera id is supported.
            if (mImageCaptureExtenderImpl != null) {
                available = mImageCaptureExtenderImpl.isExtensionAvailable(cameraId,
                        CameraUtil.getCameraCharacteristics(cameraId));
            }

            // If the camera id is supported, add it to the result set.
            if (available) {
                resultCameraIdSet.add(cameraId);
            }
        }

        return resultCameraIdSet;
    }
}
