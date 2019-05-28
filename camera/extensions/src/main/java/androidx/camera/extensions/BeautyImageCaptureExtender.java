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

import android.util.Log;

import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.extensions.impl.BeautyImageCaptureExtenderImpl;

/**
 * Load the OEM extension implementation for beauty effect type.
 */
public class BeautyImageCaptureExtender extends ImageCaptureExtender {
    private static final String TAG = "BeautyICExtender";

    /**
     * Create a new instance of the beauty extender.
     *
     * @param builder Builder that will be used to create the configurations for the
     * {@link androidx.camera.core.ImageCapture}.
     */
    public static BeautyImageCaptureExtender create(ImageCaptureConfig.Builder builder) {
        if (ExtensionVersion.isExtensionVersionSupported()) {
            try {
                return new VendorBeautyImageCaptureExtender(builder);
            } catch (NoClassDefFoundError e) {
                Log.d(TAG, "No beauty image capture extender found. Falling back to default.");
            }
        }

        return new DefaultBeautyImageCaptureExtender();
    }

    /** Empty implementation of beauty extender which does nothing. */
    static class DefaultBeautyImageCaptureExtender extends BeautyImageCaptureExtender {
        DefaultBeautyImageCaptureExtender() {
        }

        @Override
        public boolean isExtensionAvailable() {
            return false;
        }

        @Override
        public void enableExtension() {
        }
    }

    /** Beauty extender that calls into the vendor provided implementation. */
    static class VendorBeautyImageCaptureExtender extends BeautyImageCaptureExtender {
        private final BeautyImageCaptureExtenderImpl mImpl;

        VendorBeautyImageCaptureExtender(ImageCaptureConfig.Builder builder) {
            mImpl = new BeautyImageCaptureExtenderImpl();
            init(builder, mImpl);
        }
    }

    private BeautyImageCaptureExtender() {}
}
