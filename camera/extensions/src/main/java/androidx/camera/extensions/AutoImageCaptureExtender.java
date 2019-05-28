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
import androidx.camera.extensions.impl.AutoImageCaptureExtenderImpl;

/**
 * Load the OEM extension implementation for auto effect type.
 */
public class AutoImageCaptureExtender extends ImageCaptureExtender {
    private static final String TAG = "AutoICExtender";

    /**
     * Create a new instance of the auto extender.
     *
     * @param builder Builder that will be used to create the configurations for the
     * {@link androidx.camera.core.ImageCapture}.
     */
    public static AutoImageCaptureExtender create(ImageCaptureConfig.Builder builder) {
        if (ExtensionVersion.isExtensionVersionSupported()) {
            try {
                return new VendorAutoImageCaptureExtender(builder);
            } catch (NoClassDefFoundError e) {
                Log.d(TAG, "No auto image capture extender found. Falling back to default.");
            }
        }

        return new DefaultAutoImageCaptureExtender();
    }

    /** Empty implementation of auto extender which does nothing. */
    static class DefaultAutoImageCaptureExtender extends AutoImageCaptureExtender {
        DefaultAutoImageCaptureExtender() {
        }

        @Override
        public boolean isExtensionAvailable() {
            return false;
        }

        @Override
        public void enableExtension() {
        }
    }

    /** Auto extender that calls into the vendor provided implementation. */
    static class VendorAutoImageCaptureExtender extends AutoImageCaptureExtender {
        private final AutoImageCaptureExtenderImpl mImpl;

        VendorAutoImageCaptureExtender(ImageCaptureConfig.Builder builder) {
            mImpl = new AutoImageCaptureExtenderImpl();
            init(builder, mImpl);
        }
    }

    private AutoImageCaptureExtender() {}
}
