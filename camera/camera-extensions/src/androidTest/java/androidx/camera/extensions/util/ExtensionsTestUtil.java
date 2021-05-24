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

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.PreviewConfig;
import androidx.camera.extensions.AutoImageCaptureExtender;
import androidx.camera.extensions.AutoPreviewExtender;
import androidx.camera.extensions.BeautyImageCaptureExtender;
import androidx.camera.extensions.BeautyPreviewExtender;
import androidx.camera.extensions.BokehImageCaptureExtender;
import androidx.camera.extensions.BokehPreviewExtender;
import androidx.camera.extensions.ExtensionMode;
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

import org.junit.AssumptionViolatedException;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ExtensionsTestUtil {
    @NonNull
    public static Collection<Object[]> getAllEffectLensFacingCombinations() {
        return Arrays.asList(new Object[][]{
                {EffectMode.BOKEH, CameraSelector.LENS_FACING_FRONT},
                {EffectMode.BOKEH, CameraSelector.LENS_FACING_BACK},
                {EffectMode.HDR, CameraSelector.LENS_FACING_FRONT},
                {EffectMode.HDR, CameraSelector.LENS_FACING_BACK},
                {EffectMode.BEAUTY, CameraSelector.LENS_FACING_FRONT},
                {EffectMode.BEAUTY, CameraSelector.LENS_FACING_BACK},
                {EffectMode.NIGHT, CameraSelector.LENS_FACING_FRONT},
                {EffectMode.NIGHT, CameraSelector.LENS_FACING_BACK},
                {EffectMode.AUTO, CameraSelector.LENS_FACING_FRONT},
                {EffectMode.AUTO, CameraSelector.LENS_FACING_BACK}
        });
    }

    @NonNull
    public static Collection<Object[]> getAllExtensionsLensFacingCombinations() {
        return Arrays.asList(new Object[][]{
                {ExtensionMode.BOKEH, CameraSelector.LENS_FACING_FRONT},
                {ExtensionMode.BOKEH, CameraSelector.LENS_FACING_BACK},
                {ExtensionMode.HDR, CameraSelector.LENS_FACING_FRONT},
                {ExtensionMode.HDR, CameraSelector.LENS_FACING_BACK},
                {ExtensionMode.BEAUTY, CameraSelector.LENS_FACING_FRONT},
                {ExtensionMode.BEAUTY, CameraSelector.LENS_FACING_BACK},
                {ExtensionMode.NIGHT, CameraSelector.LENS_FACING_FRONT},
                {ExtensionMode.NIGHT, CameraSelector.LENS_FACING_BACK},
                {ExtensionMode.AUTO, CameraSelector.LENS_FACING_FRONT},
                {ExtensionMode.AUTO, CameraSelector.LENS_FACING_BACK}
        });
    }

    /**
     * Initializes the extensions for running the following tests.
     *
     * @return True if initializing successfully.
     */
    public static boolean initExtensions(@NonNull Context context)
            throws InterruptedException, ExecutionException, TimeoutException {
        ListenableFuture<ExtensionsAvailability> availability = ExtensionsManager.init(context);
        ExtensionsAvailability extensionsAvailability = availability.get(1, TimeUnit.SECONDS);

        // Checks that there is vendor library on device for test.
        if (extensionsAvailability == ExtensionsAvailability.NONE) {
            return false;
        }

        // Checks that vendor library is loaded successfully.
        assertTrue(extensionsAvailability == ExtensionsAvailability.LIBRARY_AVAILABLE);

        return true;
    }

    @ExtensionMode.Mode
    public static int effectModeToExtensionMode(@NonNull EffectMode effectMode) {
        switch (effectMode) {
            case NORMAL:
                return ExtensionMode.NONE;
            case BOKEH:
                return ExtensionMode.BOKEH;
            case HDR:
                return ExtensionMode.HDR;
            case NIGHT:
                return ExtensionMode.NIGHT;
            case BEAUTY:
                return ExtensionMode.BEAUTY;
            case AUTO:
                return ExtensionMode.AUTO;
        }
        throw new IllegalArgumentException("Effect mode not found: " + effectMode);
    }

    public static EffectMode extensionModeToEffectMode(@ExtensionMode.Mode int mode) {
        switch (mode) {
            case ExtensionMode.NONE:
                return EffectMode.NORMAL;
            case ExtensionMode.BOKEH:
                return EffectMode.BOKEH;
            case ExtensionMode.HDR:
                return EffectMode.HDR;
            case ExtensionMode.NIGHT:
                return EffectMode.NIGHT;
            case ExtensionMode.BEAUTY:
                return EffectMode.BEAUTY;
            case ExtensionMode.AUTO:
                return EffectMode.AUTO;
        }
        throw new IllegalArgumentException("Extension mode not found: " + mode);
    }

    /**
     * Creates an {@link ImageCapture.Builder} object for specific {@link EffectMode} and
     * {@link CameraSelector.LensFacing}.
     *
     * @param effectMode The effect mode for the created object.
     * @param lensFacing The lens facing for the created object.
     * @return An {@link ImageCapture.Builder} object.
     */
    @NonNull
    public static ImageCapture.Builder createImageCaptureConfigBuilderWithEffect(
            @NonNull EffectMode effectMode, @CameraSelector.LensFacing int lensFacing) {
        ImageCapture.Builder builder = new ImageCapture.Builder();
        CameraSelector selector =
                new CameraSelector.Builder().requireLensFacing(lensFacing).build();
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
            assertTrue(extender.isExtensionAvailable(selector));
            extender.enableExtension(selector);
        }

        return builder;
    }

    /**
     * Creates a {@link Preview.Builder} object for specific {@link EffectMode} and
     * {@link CameraSelector.LensFacing}.
     *
     * @param effectMode The effect mode for the created object.
     * @param lensFacing The lens facing for the created object.
     * @return A {@link Preview.Builder} object.
     */
    @NonNull
    public static Preview.Builder createPreviewBuilderWithEffect(@NonNull EffectMode effectMode,
            @CameraSelector.LensFacing int lensFacing) {
        Preview.Builder builder = new Preview.Builder();
        CameraSelector selector =
                new CameraSelector.Builder().requireLensFacing(lensFacing).build();
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
            assertTrue(extender.isExtensionAvailable(selector));
            extender.enableExtension(selector);
        }

        return builder;
    }

    /**
     * Creates an {@link ImageCaptureConfig} object for specific {@link EffectMode} and
     * {@link CameraSelector.LensFacing}.
     *
     * @param effectMode The effect mode for the created object.
     * @param lensFacing The lens facing for the created object.
     * @return An {@link ImageCaptureConfig} object.
     */
    @NonNull
    public static ImageCaptureConfig createImageCaptureConfigWithEffect(
            @NonNull EffectMode effectMode, @CameraSelector.LensFacing int lensFacing) {
        ImageCapture.Builder imageCaptureConfigBuilder =
                createImageCaptureConfigBuilderWithEffect(effectMode, lensFacing);
        return imageCaptureConfigBuilder.getUseCaseConfig();
    }

    /**
     * Creates a {@link PreviewConfig} object for specific {@link EffectMode} and
     * {@link CameraSelector.LensFacing}.
     *
     * @param effectMode The effect mode for the created object.
     * @param lensFacing The lens facing for the created object.
     * @return A {@link PreviewConfig} object.
     */
    @NonNull
    public static PreviewConfig createPreviewConfigWithEffect(@NonNull EffectMode effectMode,
            @CameraSelector.LensFacing int lensFacing) {
        Preview.Builder previewBuilder =
                createPreviewBuilderWithEffect(effectMode, lensFacing);
        return previewBuilder.getUseCaseConfig();
    }

    /**
     * Creates an {@link ImageCapture} object for specific {@link EffectMode} and
     * {@link CameraSelector.LensFacing}.
     *
     * @param effectMode The effect mode for the created object.
     * @param lensFacing The lens facing for the created object.
     * @return An {@link ImageCapture} object.
     */
    @NonNull
    public static ImageCapture createImageCaptureWithEffect(@NonNull EffectMode effectMode,
            @CameraSelector.LensFacing int lensFacing) {
        return createImageCaptureConfigBuilderWithEffect(effectMode, lensFacing).build();
    }

    /**
     * Creates a {@link Preview} object for specific {@link EffectMode} and
     * {@link CameraSelector.LensFacing}.
     *
     * @param effectMode The effect mode for the created object.
     * @param lensFacing The lens facing for the created object.
     * @return A {@link Preview} object.
     */
    @NonNull
    public static Preview createPreviewWithEffect(@NonNull EffectMode effectMode,
            @CameraSelector.LensFacing int lensFacing) {
        return createPreviewBuilderWithEffect(effectMode, lensFacing).build();
    }

    /**
     * Creates an {@link ImageCaptureExtenderImpl} object for specific {@link EffectMode} and
     * {@link CameraSelector.LensFacing}.
     *
     * @param effectMode The effect mode for the created object.
     * @param lensFacing The lens facing for the created object.
     * @return An {@link ImageCaptureExtenderImpl} object.
     */
    @NonNull
    public static ImageCaptureExtenderImpl createImageCaptureExtenderImpl(
            @NonNull EffectMode effectMode, @CameraSelector.LensFacing int lensFacing)
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

        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        String cameraId = getCameraIdUnchecked(cameraSelector);
        CameraCharacteristics cameraCharacteristics =
                CameraUtil.getCameraManager().getCameraCharacteristics(cameraId);

        impl.init(cameraId, cameraCharacteristics);

        return impl;
    }

    /**
     * Creates a {@link PreviewExtenderImpl} object for specific {@link EffectMode} and
     * {@link CameraSelector.LensFacing}.
     *
     * @param effectMode The effect mode for the created object.
     * @param lensFacing The lens facing for the created object.
     * @return A {@link PreviewExtenderImpl} object.
     */
    @NonNull
    public static PreviewExtenderImpl createPreviewExtenderImpl(@NonNull EffectMode effectMode,
            @CameraSelector.LensFacing int lensFacing)
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

        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        String cameraId = getCameraIdUnchecked(cameraSelector);
        CameraCharacteristics cameraCharacteristics =
                CameraUtil.getCameraManager().getCameraCharacteristics(cameraId);

        impl.init(cameraId, cameraCharacteristics);

        return impl;
    }

    /**
     * Creates an {@link ImageCaptureExtender} object for specific {@link EffectMode} and
     * {@link ImageCapture.Builder}.
     *
     * @param effectMode The effect mode for the created object.
     * @param builder    The {@link ImageCapture.Builder} for the created object.
     * @return An {@link ImageCaptureExtender} object.
     */
    @NonNull
    public static ImageCaptureExtender createImageCaptureExtender(@NonNull EffectMode effectMode,
            @NonNull ImageCapture.Builder builder) {
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
     * {@link Preview.Builder}.
     *
     * @param effectMode The effect mode for the created object.
     * @param builder    The {@link Preview.Builder} for the created object.
     * @return A {@link PreviewExtender} object.
     */
    @NonNull
    public static PreviewExtender createPreviewExtender(@NonNull EffectMode effectMode,
            @NonNull Preview.Builder builder) {
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

    public static void assumeCompatibleDevice() {
        if (Build.DEVICE.equalsIgnoreCase("sailfish") && Build.VERSION.SDK_INT == 26) {
            throw new AssumptionViolatedException("Known issue, b/187711991.");
        }
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
