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
import androidx.camera.core.CameraProvider;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Logger;
import androidx.camera.extensions.impl.AutoImageCaptureExtenderImpl;
import androidx.camera.extensions.internal.ExtensionVersion;

/**
 * Load the OEM extension implementation for auto effect type.
 *
 * @deprecated Use
 * {@link ExtensionsManager#isExtensionAvailable(CameraProvider, CameraSelector, int)}
 * to check whether extension function can support with the given {@link CameraSelector}. Use
 * {@link ExtensionsManager#getExtensionEnabledCameraSelector(CameraProvider, CameraSelector, int)}
 * to get a {@link CameraSelector} for the specific extension mode, then use it to bind the use
 * cases to a lifecycle owner.
 */
@Deprecated
public class AutoImageCaptureExtender extends ImageCaptureExtender {
    private static final String TAG = "AutoICExtender";

    /**
     * Create a new instance of the auto extender.
     *
     * @param builder Builder that will be used to create the configurations for the
     * {@link androidx.camera.core.ImageCapture}.
     */
    @NonNull
    public static AutoImageCaptureExtender create(@NonNull ImageCapture.Builder builder) {
        if (ExtensionVersion.isExtensionVersionSupported()) {
            try {
                return new VendorAutoImageCaptureExtender(builder);
            } catch (NoClassDefFoundError e) {
                Logger.d(TAG, "No auto image capture extender found. Falling back to default.");
            }
        }

        return new DefaultAutoImageCaptureExtender();
    }

    /** Empty implementation of auto extender which does nothing. */
    static class DefaultAutoImageCaptureExtender extends AutoImageCaptureExtender {
        DefaultAutoImageCaptureExtender() {
        }

        @Override
        public boolean isExtensionAvailable(@NonNull CameraSelector selector) {
            return false;
        }

        @Override
        public void enableExtension(@NonNull CameraSelector selector) {
        }
    }

    /** Auto extender that calls into the vendor provided implementation. */
    static class VendorAutoImageCaptureExtender extends AutoImageCaptureExtender {
        private final AutoImageCaptureExtenderImpl mImpl;

        VendorAutoImageCaptureExtender(ImageCapture.Builder builder) {
            mImpl = new AutoImageCaptureExtenderImpl();
            init(builder, mImpl, ExtensionMode.AUTO);
        }
    }

    private AutoImageCaptureExtender() {}
}
