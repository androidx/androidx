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

import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImageCaptureUseCase;
import androidx.camera.core.ImageCaptureUseCaseConfiguration;
import androidx.camera.core.ViewFinderUseCase;
import androidx.camera.core.ViewFinderUseCaseConfiguration;

/**
 * Provides interfaces for third party app developers to get capabilities info of extension
 * functions.
 */
public final class ExtensionsManager {
    /** The effect mode options applied on the bound use cases */
    public enum EffectMode {
        /** Normal mode without any specific effect applied. */
        NORMAL,
        /** Bokeh mode that is often applied as portrait mode for people pictures. */
        BOKEH,
        /**
         * HDR mode that may get source pictures with different AE settings to generate a best
         * result.
         */
        HDR
    }

    /**
     * Indicates whether the camera device with the {@link LensFacing} can support the specific
     * extension function.
     *
     * @param effectMode The extension function to be checked.
     * @param lensFacing The {@link LensFacing} of the camera device to be checked.
     * @return True if the specific extension function is supported for the camera device.
     */
    public static boolean isExtensionAvailable(EffectMode effectMode, LensFacing lensFacing) {
        return checkImageCaptureExtensionCapability(effectMode, lensFacing)
                || checkViewFinderExtensionCapability(effectMode, lensFacing);
    }

    /**
     * Indicates whether the camera device with the {@link LensFacing} can support the specific
     * extension function for specific use case.
     *
     * @param klass      The {@link androidx.camera.core.ImageCaptureUseCase} or
     *                   {@link androidx.camera.core.ViewFinderUseCase} class
     *                   to be checked.
     * @param effectMode The extension function to be checked.
     * @param lensFacing The {@link LensFacing} of the camera device to be checked.
     * @return True if the specific extension function is supported for the camera device.
     */
    public static boolean isExtensionAvailable(
            Class<?> klass, EffectMode effectMode, LensFacing lensFacing) {
        boolean isAvailable = false;

        if (klass == ImageCaptureUseCase.class) {
            isAvailable = checkImageCaptureExtensionCapability(effectMode, lensFacing);
        } else if (klass.equals(ViewFinderUseCase.class)) {
            isAvailable = checkViewFinderExtensionCapability(effectMode, lensFacing);
        }

        return isAvailable;
    }

    private static boolean checkImageCaptureExtensionCapability(EffectMode effectMode,
            LensFacing lensFacing) {
        ImageCaptureUseCaseConfiguration.Builder builder =
                new ImageCaptureUseCaseConfiguration.Builder();
        builder.setLensFacing(lensFacing);
        ImageCaptureUseCaseExtender extender;

        switch (effectMode) {
            case BOKEH:
                extender = new BokehImageCaptureExtender(builder);
                break;
            case HDR:
                extender = new HdrImageCaptureExtender(builder);
                break;
            default:
                extender = new DefaultImageCaptureUseCaseExtender(builder);
                break;
        }

        return extender.isExtensionAvailable();
    }

    private static boolean checkViewFinderExtensionCapability(EffectMode effectMode,
            LensFacing lensFacing) {
        ViewFinderUseCaseConfiguration.Builder builder =
                new ViewFinderUseCaseConfiguration.Builder();
        builder.setLensFacing(lensFacing);
        ViewFinderUseCaseExtender extender;

        switch (effectMode) {
            case BOKEH:
                extender = new BokehViewFinderExtender(builder);
                break;
            case HDR:
                extender = new HdrViewFinderExtender(builder);
                break;
            default:
                extender = new DefaultViewFinderUseCaseExtender(builder);
                break;
        }

        return extender.isExtensionAvailable();
    }

    private ExtensionsManager() {
    }
}
