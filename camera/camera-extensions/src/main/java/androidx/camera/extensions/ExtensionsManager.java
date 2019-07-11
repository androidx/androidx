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

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;

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
        HDR,
        /**
         * Night mode is used for taking better still capture images under low-light situations,
         * typically at night time.
         */
        NIGHT,
        /**
         * Beauty mode is used for taking still capture images that incorporate facial changes
         * like skin tone, geometry, or retouching.
         */
        BEAUTY,
        /**
         * Auto mode is used for taking still capture images that automatically adjust to the
         * surrounding scenery.
         */
        AUTO
    }

    private static final Object ERROR_LOCK = new Object();

    @GuardedBy("ERROR_LOCK")
    private static final Handler DEFAULT_HANDLER = new Handler(Looper.getMainLooper());
    @GuardedBy("ERROR_LOCK")
    private static volatile ExtensionsErrorListener sExtensionsErrorListener = null;

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
                || checkPreviewExtensionCapability(effectMode, lensFacing);
    }

    /**
     * Indicates whether the camera device with the {@link LensFacing} can support the specific
     * extension function for specific use case.
     *
     * @param klass      The {@link ImageCapture} or {@link Preview} class to be checked.
     * @param effectMode The extension function to be checked.
     * @param lensFacing The {@link LensFacing} of the camera device to be checked.
     * @return True if the specific extension function is supported for the camera device.
     */
    public static boolean isExtensionAvailable(
            Class<?> klass, EffectMode effectMode, LensFacing lensFacing) {
        boolean isAvailable = false;

        if (klass == ImageCapture.class) {
            isAvailable = checkImageCaptureExtensionCapability(effectMode, lensFacing);
        } else if (klass.equals(Preview.class)) {
            isAvailable = checkPreviewExtensionCapability(effectMode, lensFacing);
        }

        return isAvailable;
    }

    private static boolean checkImageCaptureExtensionCapability(EffectMode effectMode,
            LensFacing lensFacing) {
        ImageCaptureConfig.Builder builder = new ImageCaptureConfig.Builder();
        builder.setLensFacing(lensFacing);
        ImageCaptureExtender extender;

        switch (effectMode) {
            case BOKEH:
                extender = BokehImageCaptureExtender.create(builder);
                break;
            case HDR:
                extender = HdrImageCaptureExtender.create(builder);
                break;
            case NIGHT:
                extender = NightImageCaptureExtender.create(builder);
                break;
            case BEAUTY:
                extender = BeautyImageCaptureExtender.create(builder);
                break;
            case AUTO:
                extender = AutoImageCaptureExtender.create(builder);
                break;
            case NORMAL:
                return true;
            default:
                return false;
        }

        return extender.isExtensionAvailable();
    }

    /**
     * Sets an {@link ExtensionsErrorListener} which will get called any time an
     * extensions error is encountered.
     *
     * @param listener The {@link ExtensionsErrorListener} listener that will be run.
     */
    public static void setExtensionsErrorListener(@Nullable ExtensionsErrorListener listener) {
        synchronized (ERROR_LOCK) {
            sExtensionsErrorListener = listener;
        }
    }

    static void postExtensionsError(ExtensionsErrorListener.ExtensionsErrorCode errorCode) {
        synchronized (ERROR_LOCK) {
            final ExtensionsErrorListener listenerReference = sExtensionsErrorListener;
            if (listenerReference != null) {
                DEFAULT_HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        listenerReference.onError(errorCode);
                    }
                });
            }
        }
    }

    private static boolean checkPreviewExtensionCapability(EffectMode effectMode,
            LensFacing lensFacing) {
        PreviewConfig.Builder builder = new PreviewConfig.Builder();
        builder.setLensFacing(lensFacing);
        PreviewExtender extender;

        switch (effectMode) {
            case BOKEH:
                extender = BokehPreviewExtender.create(builder);
                break;
            case HDR:
                extender = HdrPreviewExtender.create(builder);
                break;
            case NIGHT:
                extender = NightPreviewExtender.create(builder);
                break;
            case BEAUTY:
                extender = BeautyPreviewExtender.create(builder);
                break;
            case AUTO:
                extender = AutoPreviewExtender.create(builder);
                break;
            case NORMAL:
                return true;
            default:
                return false;
        }

        return extender.isExtensionAvailable();
    }

    private ExtensionsManager() {
    }
}
