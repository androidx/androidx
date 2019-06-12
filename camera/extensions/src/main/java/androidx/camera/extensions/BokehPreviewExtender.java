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

import androidx.camera.core.PreviewConfig;
import androidx.camera.extensions.impl.BokehPreviewExtenderImpl;

/**
 * Load the OEM extension Preview implementation for bokeh effect type.
 */
public class BokehPreviewExtender extends PreviewExtender {
    private static final String TAG = "BokehPreviewExtender";

    /**
     * Create a new instance of the bokeh extender.
     *
     * @param builder Builder that will be used to create the configurations for the
     * {@link androidx.camera.core.Preview}.
     */
    public static BokehPreviewExtender create(PreviewConfig.Builder builder) {
        if (ExtensionVersion.isExtensionVersionSupported()) {
            try {
                return new VendorBokehPreviewExtender(builder);
            } catch (NoClassDefFoundError e) {
                Log.d(TAG, "No bokeh preview extender found. Falling back to default.");
            }
        }

        return new DefaultBokehPreviewExtender();
    }

    /** Empty implementation of bokeh extender which does nothing. */
    private static class DefaultBokehPreviewExtender extends BokehPreviewExtender {
        DefaultBokehPreviewExtender() {
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
    private static class VendorBokehPreviewExtender extends BokehPreviewExtender {
        private final BokehPreviewExtenderImpl mImpl;

        VendorBokehPreviewExtender(PreviewConfig.Builder builder) {
            mImpl = new BokehPreviewExtenderImpl();
            init(builder, mImpl);
        }
    }

    private BokehPreviewExtender() {}
}
