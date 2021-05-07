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
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Logger;
import androidx.camera.core.Preview;
import androidx.camera.extensions.impl.BeautyPreviewExtenderImpl;

/**
 * Load the OEM extension Preview implementation for beauty effect type.
 */
public class BeautyPreviewExtender extends PreviewExtender {
    private static final String TAG = "BeautyPreviewExtender";

    /**
     * Create a new instance of the beauty extender.
     *
     * @param builder Builder that will be used to create the configurations for the
     *                {@link androidx.camera.core.Preview}.
     */
    @NonNull
    public static BeautyPreviewExtender create(@NonNull Preview.Builder builder) {
        if (ExtensionVersion.isExtensionVersionSupported()) {
            try {
                return new VendorBeautyPreviewExtender(builder);
            } catch (NoClassDefFoundError e) {
                Logger.d(TAG, "No beauty preview extender found. Falling back to default.");
            }
        }

        return new DefaultBeautyPreviewExtender();
    }

    /** Empty implementation of beauty extender which does nothing. */
    static class DefaultBeautyPreviewExtender extends BeautyPreviewExtender {
        DefaultBeautyPreviewExtender() {
        }

        @Override
        public boolean isExtensionAvailable(@NonNull CameraSelector selector) {
            return false;
        }

        @Override
        public void enableExtension(@NonNull CameraSelector selector) {
        }
    }

    /** Beauty extender that calls into the vendor provided implementation. */
    static class VendorBeautyPreviewExtender extends BeautyPreviewExtender {
        private final BeautyPreviewExtenderImpl mImpl;

        VendorBeautyPreviewExtender(Preview.Builder builder) {
            mImpl = new BeautyPreviewExtenderImpl();
            init(builder, mImpl, ExtensionMode.BEAUTY);
        }
    }

    private BeautyPreviewExtender() {
    }
}
