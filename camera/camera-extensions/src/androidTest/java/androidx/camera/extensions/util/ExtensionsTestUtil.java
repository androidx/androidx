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

package androidx.camera.extensions.util;

import static androidx.camera.extensions.ExtensionMode.AUTO;
import static androidx.camera.extensions.ExtensionMode.BEAUTY;
import static androidx.camera.extensions.ExtensionMode.BOKEH;
import static androidx.camera.extensions.ExtensionMode.HDR;
import static androidx.camera.extensions.ExtensionMode.NIGHT;

import static junit.framework.TestCase.assertNotNull;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.extensions.ExtensionMode;
import androidx.camera.extensions.impl.AutoImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.AutoPreviewExtenderImpl;
import androidx.camera.extensions.impl.BeautyImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.BeautyPreviewExtenderImpl;
import androidx.camera.extensions.impl.BokehImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.BokehPreviewExtenderImpl;
import androidx.camera.extensions.impl.HdrImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.HdrPreviewExtenderImpl;
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.NightImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.NightPreviewExtenderImpl;
import androidx.camera.extensions.impl.PreviewExtenderImpl;
import androidx.camera.testing.CameraUtil;

import java.util.Arrays;
import java.util.Collection;

/**
 * Extension test util functions.
 */
public class ExtensionsTestUtil {
    @NonNull
    public static Collection<Object[]> getAllExtensionsLensFacingCombinations() {
        return Arrays.asList(new Object[][]{
                {BOKEH, CameraSelector.LENS_FACING_FRONT},
                {BOKEH, CameraSelector.LENS_FACING_BACK},
                {HDR, CameraSelector.LENS_FACING_FRONT},
                {HDR, CameraSelector.LENS_FACING_BACK},
                {BEAUTY, CameraSelector.LENS_FACING_FRONT},
                {BEAUTY, CameraSelector.LENS_FACING_BACK},
                {NIGHT, CameraSelector.LENS_FACING_FRONT},
                {NIGHT, CameraSelector.LENS_FACING_BACK},
                {AUTO, CameraSelector.LENS_FACING_FRONT},
                {AUTO, CameraSelector.LENS_FACING_BACK}
        });
    }

    /**
     * Creates an {@link ImageCaptureExtenderImpl} object for specific {@link ExtensionMode} and
     * {@link CameraSelector.LensFacing}.
     *
     * @param extensionMode The extension mode for the created object.
     * @param lensFacing The lens facing for the created object.
     * @return An {@link ImageCaptureExtenderImpl} object.
     */
    @NonNull
    public static ImageCaptureExtenderImpl createImageCaptureExtenderImpl(
            @ExtensionMode.Mode int extensionMode, @CameraSelector.LensFacing int lensFacing)
            throws CameraAccessException {
        ImageCaptureExtenderImpl impl = null;

        switch (extensionMode) {
            case HDR:
                impl = new HdrImageCaptureExtenderImpl();
                break;
            case BOKEH:
                impl = new BokehImageCaptureExtenderImpl();
                break;
            case BEAUTY:
                impl = new BeautyImageCaptureExtenderImpl();
                break;
            case NIGHT:
                impl = new NightImageCaptureExtenderImpl();
                break;
            case AUTO:
                impl = new AutoImageCaptureExtenderImpl();
                break;
        }
        assertNotNull(impl);

        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        String cameraId = getCameraIdUnchecked(cameraSelector);
        CameraCharacteristics cameraCharacteristics =
                CameraUtil.getCameraManager().getCameraCharacteristics(cameraId);

        impl.init(cameraId, cameraCharacteristics);

        return impl;
    }

    /**
     * Creates a {@link PreviewExtenderImpl} object for specific {@link ExtensionMode} and
     * {@link CameraSelector.LensFacing}.
     *
     * @param extensionMode The extension mode for the created object.
     * @param lensFacing The lens facing for the created object.
     * @return A {@link PreviewExtenderImpl} object.
     */
    @NonNull
    public static PreviewExtenderImpl createPreviewExtenderImpl(
            @ExtensionMode.Mode int extensionMode, @CameraSelector.LensFacing int lensFacing)
            throws CameraAccessException {
        PreviewExtenderImpl impl = null;

        switch (extensionMode) {
            case HDR:
                impl = new HdrPreviewExtenderImpl();
                break;
            case BOKEH:
                impl = new BokehPreviewExtenderImpl();
                break;
            case BEAUTY:
                impl = new BeautyPreviewExtenderImpl();
                break;
            case NIGHT:
                impl = new NightPreviewExtenderImpl();
                break;
            case AUTO:
                impl = new AutoPreviewExtenderImpl();
                break;
        }
        assertNotNull(impl);

        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        String cameraId = getCameraIdUnchecked(cameraSelector);
        CameraCharacteristics cameraCharacteristics =
                CameraUtil.getCameraManager().getCameraCharacteristics(cameraId);

        impl.init(cameraId, cameraCharacteristics);

        return impl;
    }

    @Nullable
    private static String getCameraIdUnchecked(@NonNull CameraSelector cameraSelector) {
        try {
            return CameraX.getCameraWithCameraSelector(
                    cameraSelector).getCameraInfoInternal().getCameraId();
        } catch (IllegalArgumentException e) {
            // Returns null if there's no camera id can be found.
            return null;
        }
    }
}
