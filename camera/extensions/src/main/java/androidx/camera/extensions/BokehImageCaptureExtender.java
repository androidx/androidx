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

package androidx.camera.extensions;

import android.util.Log;

import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.extensions.impl.BokehImageCaptureExtenderImpl;

/**
 * Loads the OEM extension implementation for bokeh effect type.
 */
public class BokehImageCaptureExtender extends ImageCaptureExtender {
    private static final String TAG = "BokehImgCaptureExtender";

    /**
     * Creates a new instance of the bokeh extender.
     *
     * @param builder Builder that will be used to create the configurations for the
     * {@link androidx.camera.core.ImageCapture}.
     */
    public static BokehImageCaptureExtender create(ImageCaptureConfig.Builder builder) {
        if (ExtensionVersion.isExtensionVersionSupported()) {
            try {
                return new VendorBokehImageCaptureExtender(builder);
            } catch (NoClassDefFoundError e) {
                Log.d(TAG, "No bokeh image capture extender found. Falling back to default.");
            }
        }

        return new DefaultBokehImageCaptureExtender();
    }

    /** Empty implementation of bokeh extender which does nothing. */
    private static class DefaultBokehImageCaptureExtender extends BokehImageCaptureExtender {
        DefaultBokehImageCaptureExtender() {
        }

        @Override
        public boolean isExtensionAvailable() {
            return false;
        }

        @Override
        public void enableExtension() {
        }
    }

    /** Bokeh extender that calls into the vendor provided implementation. */
    private static class VendorBokehImageCaptureExtender extends BokehImageCaptureExtender {
        private final BokehImageCaptureExtenderImpl mImpl;

        VendorBokehImageCaptureExtender(ImageCaptureConfig.Builder builder) {
            mImpl = new BokehImageCaptureExtenderImpl();
            init(builder, mImpl);
        }
    }

    private BokehImageCaptureExtender() {}
}
