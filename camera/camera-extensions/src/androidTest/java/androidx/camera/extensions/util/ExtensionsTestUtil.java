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

import static junit.framework.TestCase.assertNotNull;

import static org.junit.Assert.assertTrue;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraDeviceConfig;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.LensFacing;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.extensions.AutoImageCaptureExtender;
import androidx.camera.extensions.AutoPreviewExtender;
import androidx.camera.extensions.BeautyImageCaptureExtender;
import androidx.camera.extensions.BeautyPreviewExtender;
import androidx.camera.extensions.BokehImageCaptureExtender;
import androidx.camera.extensions.BokehPreviewExtender;
import androidx.camera.extensions.ExtensionsManager;
import androidx.camera.extensions.ExtensionsManager.EffectMode;
import androidx.camera.extensions.ExtensionsManager.ExtensionsAvailability;
import androidx.camera.extensions.HdrImageCaptureExtender;
import androidx.camera.extensions.HdrPreviewExtender;
import androidx.camera.extensions.ImageCaptureExtender;
import androidx.camera.extensions.NightImageCaptureExtender;
import androidx.camera.extensions.NightPreviewExtender;
import androidx.camera.extensions.PreviewExtender;
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

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ExtensionsTestUtil {
    @NonNull
    public static Collection<Object[]> getAllEffectLensFacingCombinations() {
        return Arrays.asList(new Object[][]{
                {EffectMode.BOKEH, LensFacing.FRONT},
                {EffectMode.BOKEH, LensFacing.BACK},
                {EffectMode.HDR, LensFacing.FRONT},
                {EffectMode.HDR, LensFacing.BACK},
                {EffectMode.BEAUTY, LensFacing.FRONT},
                {EffectMode.BEAUTY, LensFacing.BACK},
                {EffectMode.NIGHT, LensFacing.FRONT},
                {EffectMode.NIGHT, LensFacing.BACK},
                {EffectMode.AUTO, LensFacing.FRONT},
                {EffectMode.AUTO, LensFacing.BACK}
        });
    }

    /**
     * Initializes the extensions for running the following tests.
     *
     * @return True if initializing successfully.
     */
    public static boolean initExtensions()
            throws InterruptedException, ExecutionException, TimeoutException {
        ListenableFuture<ExtensionsAvailability> availability = ExtensionsManager.init();
        ExtensionsAvailability extensionsAvailability = availability.get(1, TimeUnit.SECONDS);

        // Checks that there is vendor library on device for test.
        if (extensionsAvailability == ExtensionsAvailability.NONE) {
            return false;
        }

        // Checks that vendor library is loaded successfully.
        assertTrue(extensionsAvailability == ExtensionsAvailability.LIBRARY_AVAILABLE);

        return true;
    }

    /**
     * Creates an {@link ImageCaptureConfig.Builder} object for specific {@link EffectMode} and
     * {@link LensFacing}.
     *
     * @param effectMode The effect mode for the created object.
     * @param lensFacing The lens facing for the created object.
     * @return An {@link ImageCaptureConfig.Builder} object.
     */
    @NonNull
    public static ImageCaptureConfig.Builder createImageCaptureConfigBuilderWithEffect(
            @NonNull EffectMode effectMode, @NonNull LensFacing lensFacing) {
        ImageCaptureConfig.Builder builder =
                new ImageCaptureConfig.Builder().setLensFacing(lensFacing);
        ImageCaptureExtender extender = null;

        switch (effectMode) {
            case HDR:
                extender = HdrImageCaptureExtender.create(builder);
                break;
            case BOKEH:
                extender = BokehImageCaptureExtender.create(builder);
                break;
            case BEAUTY:
                extender = BeautyImageCaptureExtender.create(builder);
                break;
            case NIGHT:
                extender = NightImageCaptureExtender.create(builder);
                break;
            case AUTO:
                extender = AutoImageCaptureExtender.create(builder);
                break;
        }

        // Applies effect configs if it is not normal mode.
        if (effectMode != EffectMode.NORMAL) {
            assertNotNull(extender);
            assertTrue(extender.isExtensionAvailable());
            extender.enableExtension();
        }

        return builder;
    }

    /**
     * Creates a {@link PreviewConfig.Builder} object for specific {@link EffectMode} and
     * {@link LensFacing}.
     *
     * @param effectMode The effect mode for the created object.
     * @param lensFacing The lens facing for the created object.
     * @return A {@link PreviewConfig.Builder} object.
     */
    @NonNull
    public static PreviewConfig.Builder createPreviewConfigBuilderWithEffect(
            @NonNull EffectMode effectMode,
            @NonNull LensFacing lensFacing) {
        PreviewConfig.Builder builder =
                new PreviewConfig.Builder().setLensFacing(lensFacing);
        PreviewExtender extender = null;

        switch (effectMode) {
            case HDR:
                extender = HdrPreviewExtender.create(builder);
                break;
            case BOKEH:
                extender = BokehPreviewExtender.create(builder);
                break;
            case BEAUTY:
                extender = BeautyPreviewExtender.create(builder);
                break;
            case NIGHT:
                extender = NightPreviewExtender.create(builder);
                break;
            case AUTO:
                extender = AutoPreviewExtender.create(builder);
                break;
        }

        // Applies effect configs if it is not normal mode.
        if (effectMode != EffectMode.NORMAL) {
            assertNotNull(extender);
            assertTrue(extender.isExtensionAvailable());
            extender.enableExtension();
        }

        return builder;
    }

    /**
     * Creates an {@link ImageCaptureConfig} object for specific {@link EffectMode} and
     * {@link LensFacing}.
     *
     * @param effectMode The effect mode for the created object.
     * @param lensFacing The lens facing for the created object.
     * @return An {@link ImageCaptureConfig} object.
     */
    @NonNull
    public static ImageCaptureConfig createImageCaptureConfigWithEffect(
            @NonNull EffectMode effectMode,
            @NonNull LensFacing lensFacing) {
        ImageCaptureConfig.Builder imageCaptureConfigBuilder =
                createImageCaptureConfigBuilderWithEffect(effectMode, lensFacing);
        ImageCaptureConfig imageCaptureConfig = imageCaptureConfigBuilder.build();

        return imageCaptureConfig;
    }

    /**
     * Creates a {@link PreviewConfig} object for specific {@link EffectMode} and
     * {@link LensFacing}.
     *
     * @param effectMode The effect mode for the created object.
     * @param lensFacing The lens facing for the created object.
     * @return A {@link PreviewConfig} object.
     */
    @NonNull
    public static PreviewConfig createPreviewConfigWithEffect(@NonNull EffectMode effectMode,
            @NonNull LensFacing lensFacing) {
        PreviewConfig.Builder previewConfigBuilder =
                createPreviewConfigBuilderWithEffect(effectMode, lensFacing);
        PreviewConfig previewConfig = previewConfigBuilder.build();

        return previewConfig;
    }

    /**
     * Creates an {@link ImageCapture} object for specific {@link EffectMode} and
     * {@link LensFacing}.
     *
     * @param effectMode The effect mode for the created object.
     * @param lensFacing The lens facing for the created object.
     * @return An {@link ImageCapture} object.
     */
    @NonNull
    public static ImageCapture createImageCaptureWithEffect(@NonNull EffectMode effectMode,
            @NonNull LensFacing lensFacing) {
        ImageCaptureConfig imageCaptureConfig = createImageCaptureConfigWithEffect(effectMode,
                lensFacing);
        ImageCapture imageCapture = new ImageCapture(imageCaptureConfig);

        return imageCapture;
    }

    /**
     * Creates a {@link Preview} object for specific {@link EffectMode} and {@link LensFacing}.
     *
     * @param effectMode The effect mode for the created object.
     * @param lensFacing The lens facing for the created object.
     * @return A {@link Preview} object.
     */
    @NonNull
    public static Preview createPreviewWithEffect(@NonNull EffectMode effectMode,
            @NonNull LensFacing lensFacing) {
        PreviewConfig previewConfig = createPreviewConfigWithEffect(effectMode, lensFacing);
        Preview preview = new Preview(previewConfig);

        return preview;
    }

    /**
     * Creates an {@link ImageCaptureExtenderImpl} object for specific {@link EffectMode} and
     * {@link LensFacing}.
     *
     * @param effectMode The effect mode for the created object.
     * @param lensFacing The lens facing for the created object.
     * @return An {@link ImageCaptureExtenderImpl} object.
     */
    @NonNull
    public static ImageCaptureExtenderImpl createImageCaptureExtenderImpl(
            @NonNull EffectMode effectMode,
            @NonNull LensFacing lensFacing)
            throws CameraInfoUnavailableException, CameraAccessException {
        ImageCaptureExtenderImpl impl = null;

        switch (effectMode) {
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

        ImageCaptureConfig.Builder configBuilder = new ImageCaptureConfig.Builder().setLensFacing(
                lensFacing);

        String cameraId = CameraX.getCameraWithCameraDeviceConfig(
                ((CameraDeviceConfig) configBuilder.build()));
        CameraCharacteristics cameraCharacteristics =
                CameraUtil.getCameraManager().getCameraCharacteristics(
                        CameraX.getCameraWithLensFacing(lensFacing));

        impl.init(cameraId, cameraCharacteristics);

        return impl;
    }

    /**
     * Creates a {@link PreviewExtenderImpl} object for specific {@link EffectMode} and
     * {@link LensFacing}.
     *
     * @param effectMode The effect mode for the created object.
     * @param lensFacing The lens facing for the created object.
     * @return A {@link PreviewExtenderImpl} object.
     */
    @NonNull
    public static PreviewExtenderImpl createPreviewExtenderImpl(@NonNull EffectMode effectMode,
            @NonNull LensFacing lensFacing)
            throws CameraInfoUnavailableException, CameraAccessException {
        PreviewExtenderImpl impl = null;

        switch (effectMode) {
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

        PreviewConfig.Builder configBuilder = new PreviewConfig.Builder().setLensFacing(lensFacing);

        String cameraId = CameraX.getCameraWithCameraDeviceConfig(
                ((CameraDeviceConfig) configBuilder.build()));
        CameraCharacteristics cameraCharacteristics =
                CameraUtil.getCameraManager().getCameraCharacteristics(
                        CameraX.getCameraWithLensFacing(lensFacing));

        impl.init(cameraId, cameraCharacteristics);

        return impl;
    }

    /**
     * Creates an {@link ImageCaptureExtender} object for specific {@link EffectMode} and
     * {@link ImageCaptureConfig.Builder}.
     *
     * @param effectMode The effect mode for the created object.
     * @param builder The {@link ImageCaptureConfig.Builder} for the created object.
     * @return An {@link ImageCaptureExtender} object.
     */
    @NonNull
    public static ImageCaptureExtender createImageCaptureExtender(@NonNull EffectMode effectMode,
            @NonNull ImageCaptureConfig.Builder builder) {
        ImageCaptureExtender extender = null;

        switch (effectMode) {
            case HDR:
                extender = HdrImageCaptureExtender.create(builder);
                break;
            case BOKEH:
                extender = BokehImageCaptureExtender.create(builder);
                break;
            case BEAUTY:
                extender = BeautyImageCaptureExtender.create(builder);
                break;
            case NIGHT:
                extender = NightImageCaptureExtender.create(builder);
                break;
            case AUTO:
                extender = AutoImageCaptureExtender.create(builder);
                break;
        }
        assertNotNull(extender);

        return extender;
    }

    /**
     * Creates a {@link PreviewExtender} object for specific {@link EffectMode} and
     * {@link PreviewConfig.Builder}.
     *
     * @param effectMode The effect mode for the created object.
     * @param builder The {@link PreviewConfig.Builder} for the created object.
     * @return A {@link PreviewExtender} object.
     */
    @NonNull
    public static PreviewExtender createPreviewExtender(@NonNull EffectMode effectMode,
            @NonNull PreviewConfig.Builder builder) {
        PreviewExtender extender = null;

        switch (effectMode) {
            case HDR:
                extender = HdrPreviewExtender.create(builder);
                break;
            case BOKEH:
                extender = BokehPreviewExtender.create(builder);
                break;
            case BEAUTY:
                extender = BeautyPreviewExtender.create(builder);
                break;
            case NIGHT:
                extender = NightPreviewExtender.create(builder);
                break;
            case AUTO:
                extender = AutoPreviewExtender.create(builder);
                break;
        }
        assertNotNull(extender);

        return extender;
    }
}
