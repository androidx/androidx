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
import static androidx.camera.extensions.ExtensionMode.BOKEH;
import static androidx.camera.extensions.ExtensionMode.FACE_RETOUCH;
import static androidx.camera.extensions.ExtensionMode.HDR;
import static androidx.camera.extensions.ExtensionMode.NIGHT;
import static androidx.camera.extensions.impl.ExtensionsTestlibControl.ImplementationType.OEM_IMPL;
import static androidx.camera.extensions.impl.ExtensionsTestlibControl.ImplementationType.TESTLIB_ADVANCED;
import static androidx.camera.extensions.impl.ExtensionsTestlibControl.ImplementationType.TESTLIB_BASIC;

import android.content.Context;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.pipe.integration.CameraPipeConfig;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ExtendableBuilder;
import androidx.camera.core.impl.Config;
import androidx.camera.extensions.ExtensionMode;
import androidx.camera.extensions.ExtensionsManager;
import androidx.camera.extensions.impl.AutoImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.AutoPreviewExtenderImpl;
import androidx.camera.extensions.impl.BeautyImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.BeautyPreviewExtenderImpl;
import androidx.camera.extensions.impl.BokehImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.BokehPreviewExtenderImpl;
import androidx.camera.extensions.impl.ExtensionVersionImpl;
import androidx.camera.extensions.impl.ExtensionsTestlibControl;
import androidx.camera.extensions.impl.HdrImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.HdrPreviewExtenderImpl;
import androidx.camera.extensions.impl.NightImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.NightPreviewExtenderImpl;
import androidx.camera.extensions.impl.advanced.AutoAdvancedExtenderImpl;
import androidx.camera.extensions.impl.advanced.BeautyAdvancedExtenderImpl;
import androidx.camera.extensions.impl.advanced.BokehAdvancedExtenderImpl;
import androidx.camera.extensions.impl.advanced.HdrAdvancedExtenderImpl;
import androidx.camera.extensions.impl.advanced.NightAdvancedExtenderImpl;
import androidx.camera.extensions.internal.AdvancedVendorExtender;
import androidx.camera.extensions.internal.BasicVendorExtender;
import androidx.camera.extensions.internal.ExtensionVersion;
import androidx.camera.extensions.internal.VendorExtender;
import androidx.camera.extensions.internal.Version;
import androidx.camera.extensions.internal.compat.workaround.ExtensionDisabledValidator;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.testing.impl.CameraUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Extension test util functions.
 */
public class ExtensionsTestUtil {
    public static final Config.Option<CameraCaptureSession.CaptureCallback>
            SESSION_CAPTURE_CALLBACK_OPTION =
            Config.Option.create("camera2.cameraCaptureSession.captureCallback",
                    CameraCaptureSession.CaptureCallback.class);
    public static final String CAMERA2_IMPLEMENTATION_OPTION = "camera2";
    public static final String CAMERA_PIPE_IMPLEMENTATION_OPTION = "camera_pipe";

    private static boolean isAdvancedExtender() {
        ExtensionVersionImpl extensionVersion = new ExtensionVersionImpl();
        try {
            if (ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_2)
                    && extensionVersion.isAdvancedExtenderImplemented()) {
                return true;
            }
        } catch (NoSuchMethodError e) {
            // in case some devices remove the isAdvancedExtenderImplemented method in
            // ExtensionVersionImpl.
            return false;
        }
        return false;
    }

    private static boolean hasNoSuchMethod(Runnable runnable) {
        try {
            runnable.run();
        } catch (NoSuchMethodError e) {
            return true;
        }
        return false;
    }

    // Check if the OEM implementation class for the given mode exists or not.
    private static boolean doesOEMImplementationExistForMode(int extensionMode) {
        if (isAdvancedExtender()) {
            switch (extensionMode) {
                case HDR:
                    return hasNoSuchMethod(
                            () -> HdrAdvancedExtenderImpl.checkTestlibRunning());
                case BOKEH:
                    return hasNoSuchMethod(
                            () -> BokehAdvancedExtenderImpl.checkTestlibRunning());
                case AUTO:
                    return hasNoSuchMethod(
                            () -> AutoAdvancedExtenderImpl.checkTestlibRunning());
                case FACE_RETOUCH:
                    return hasNoSuchMethod(
                            () -> BeautyAdvancedExtenderImpl.checkTestlibRunning());
                case NIGHT:
                    return hasNoSuchMethod(
                            () -> NightAdvancedExtenderImpl.checkTestlibRunning());
            }
        } else {
            switch (extensionMode) {
                case HDR:
                    return hasNoSuchMethod(
                            () -> HdrImageCaptureExtenderImpl.checkTestlibRunning())
                            && hasNoSuchMethod(
                                    () -> HdrPreviewExtenderImpl.checkTestlibRunning());
                case BOKEH:
                    return hasNoSuchMethod(
                            () -> BokehImageCaptureExtenderImpl.checkTestlibRunning())
                            && hasNoSuchMethod(
                                    () -> BokehPreviewExtenderImpl.checkTestlibRunning());
                case AUTO:
                    return hasNoSuchMethod(
                            () -> AutoImageCaptureExtenderImpl.checkTestlibRunning())
                            && hasNoSuchMethod(
                                    () -> AutoPreviewExtenderImpl.checkTestlibRunning());
                case FACE_RETOUCH:
                    return hasNoSuchMethod(
                            () -> BeautyImageCaptureExtenderImpl.checkTestlibRunning())
                            && hasNoSuchMethod(
                                    () -> BeautyPreviewExtenderImpl.checkTestlibRunning());
                case NIGHT:
                    return hasNoSuchMethod(
                            () -> NightImageCaptureExtenderImpl.checkTestlibRunning())
                            && hasNoSuchMethod(
                                    () -> NightPreviewExtenderImpl.checkTestlibRunning());
            }
        }
        return false;
    }

    /**
     * Returns if extension is supported with the given mode and lens facing. Please note that
     * if some classes are removed by OEMs, the classes in the test lib could still be used so we
     * need to return false in this case.
     */
    public static boolean isExtensionAvailable(
            ExtensionsManager extensionsManager, int lensFacing, int extensionMode) {
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();
        return isExtensionAvailable(extensionsManager, cameraSelector, extensionMode);
    }

    /**
     * Returns if extension is supported with the given mode and camera selector. Please note that
     * if some classes are removed by OEMs, the classes in the test lib could still be used so we
     * need to return false in this case.
     */
    public static boolean isExtensionAvailable(@NonNull ExtensionsManager extensionsManager,
            @NonNull CameraSelector cameraSelector, int extensionMode) {
        // Return false if classes are removed by OEMs
        if (ExtensionsTestlibControl.getInstance().getImplementationType() == OEM_IMPL
                && !doesOEMImplementationExistForMode(extensionMode)) {
            return false;
        }

        return extensionsManager.isExtensionAvailable(cameraSelector, extensionMode);
    }

    /**
     * Returns the parameters which contains the combination of CameraXConfig
     * name, CameraXConfig, implementationType, extensions mode and lens facing.
     */
    @NonNull
    public static Collection<Object[]> getAllImplExtensionsLensFacingCombinations(
            @NonNull Context context,
            boolean excludeUnavailableModes
    ) {
        ExtensionsTestlibControl.ImplementationType implType =
                ExtensionsTestlibControl.getInstance().getImplementationType();

        if (implType == TESTLIB_ADVANCED) {
            ExtensionsTestlibControl.getInstance().setImplementationType(TESTLIB_BASIC);
            implType = TESTLIB_BASIC;
        }

        List<Object[]> basicOrOemImplList = Arrays.asList(new Object[][]{
                {implType, BOKEH, CameraSelector.LENS_FACING_FRONT},
                {implType, BOKEH, CameraSelector.LENS_FACING_BACK},
                {implType, HDR, CameraSelector.LENS_FACING_FRONT},
                {implType, HDR, CameraSelector.LENS_FACING_BACK},
                {implType, FACE_RETOUCH, CameraSelector.LENS_FACING_FRONT},
                {implType, FACE_RETOUCH, CameraSelector.LENS_FACING_BACK},
                {implType, NIGHT, CameraSelector.LENS_FACING_FRONT},
                {implType, NIGHT, CameraSelector.LENS_FACING_BACK},
                {implType, AUTO, CameraSelector.LENS_FACING_FRONT},
                {implType, AUTO, CameraSelector.LENS_FACING_BACK}
        });

        if (implType == OEM_IMPL) {
            List<Object[]> allList = excludeUnavailableModes ? filterOutUnavailableMode(context,
                    basicOrOemImplList) : basicOrOemImplList;
            return getConfigPrependedCombinations(allList);
        }

        List<Object[]> advancedList = Arrays.asList(new Object[][]{
                {TESTLIB_ADVANCED, BOKEH, CameraSelector.LENS_FACING_FRONT},
                {TESTLIB_ADVANCED, BOKEH, CameraSelector.LENS_FACING_BACK},
                {TESTLIB_ADVANCED, HDR, CameraSelector.LENS_FACING_FRONT},
                {TESTLIB_ADVANCED, HDR, CameraSelector.LENS_FACING_BACK},
                {TESTLIB_ADVANCED, FACE_RETOUCH, CameraSelector.LENS_FACING_FRONT},
                {TESTLIB_ADVANCED, FACE_RETOUCH, CameraSelector.LENS_FACING_BACK},
                {TESTLIB_ADVANCED, NIGHT, CameraSelector.LENS_FACING_FRONT},
                {TESTLIB_ADVANCED, NIGHT, CameraSelector.LENS_FACING_BACK},
                {TESTLIB_ADVANCED, AUTO, CameraSelector.LENS_FACING_FRONT},
                {TESTLIB_ADVANCED, AUTO, CameraSelector.LENS_FACING_BACK}
        });

        List<Object[]> allList = new ArrayList<>();
        allList.addAll(excludeUnavailableModes
                ? filterOutUnavailableMode(context, basicOrOemImplList) : basicOrOemImplList);
        ExtensionsTestlibControl.getInstance().setImplementationType(TESTLIB_ADVANCED);

        allList.addAll(excludeUnavailableModes
                ? filterOutUnavailableMode(context, advancedList) : advancedList);

        // Reset to basic in case advanced is used accidentally.
        ExtensionsTestlibControl.getInstance().setImplementationType(TESTLIB_BASIC);

        return getConfigPrependedCombinations(allList);
    }

    private static List<Object[]> filterOutUnavailableMode(Context context,
            List<Object[]> list) {
        ExtensionsManager extensionsManager = null;
        ProcessCameraProvider cameraProvider = null;
        try {
            cameraProvider = ProcessCameraProvider.getInstance(context).get(2, TimeUnit.SECONDS);
            extensionsManager = ExtensionsManager.getInstanceAsync(context, cameraProvider)
                            .get(2, TimeUnit.SECONDS);

            List<Object[]> result = new ArrayList<>();
            for (Object[] item : list) {
                int mode = (int) item[1];
                int lensFacing = (int) item[2];
                if (isExtensionAvailable(extensionsManager, lensFacing, mode)) {
                    result.add(item);
                }
            }
            return result;
        } catch (Exception e) {
            return list;
        } finally {
            try {
                if (cameraProvider != null) {
                    cameraProvider.shutdownAsync().get();
                }
                if (extensionsManager != null) {
                    extensionsManager.shutdown().get();
                }
            } catch (Exception e) {
            }
        }
    }

    private static List<Object[]> getConfigPrependedCombinations(List<Object[]> combinations) {
        CameraXConfig camera2Config = Camera2Config.defaultConfig();
        CameraXConfig cameraPipeConfig = CameraPipeConfig.defaultConfig();
        List<Object[]> combinationsWithConfig = new ArrayList<Object[]>();
        for (Object[] combination: combinations) {
            List<Object> combinationCamera2 = new ArrayList<Object>(
                    Arrays.asList(CAMERA2_IMPLEMENTATION_OPTION, camera2Config));
            combinationCamera2.addAll(Arrays.asList(combination));
            combinationsWithConfig.add(combinationCamera2.toArray());

            List<Object> combinationCameraPipe = new ArrayList<Object>(
                    Arrays.asList(CAMERA_PIPE_IMPLEMENTATION_OPTION, cameraPipeConfig));
            combinationCameraPipe.addAll(Arrays.asList(combination));
            combinationsWithConfig.add(combinationCameraPipe.toArray());
        }
        return combinationsWithConfig;
    }

    /**
     * Returns whether the target camera device can support the test for a specific extension mode.
     */
    public static boolean isTargetDeviceAvailableForExtensions(
            @CameraSelector.LensFacing int lensFacing, @ExtensionMode.Mode int mode) {
        return CameraUtil.hasCameraWithLensFacing(lensFacing) && isLimitedAboveDevice(lensFacing)
                && !isSpecificSkippedDevice() && !isSpecificSkippedDeviceWithExtensionMode(mode);
    }

    private static boolean isAdvancedExtenderSupported() {
        if (ExtensionVersion.getRuntimeVersion().compareTo(Version.VERSION_1_2) < 0) {
            return false;
        }
        return ExtensionVersion.isAdvancedExtenderSupported();
    }

    public static VendorExtender createVendorExtender(@ExtensionMode.Mode int mode) {
        if (isAdvancedExtenderSupported()) {
            return new AdvancedVendorExtender(mode);
        }
        return new BasicVendorExtender(mode);
    }

    /**
     * Returns whether the device is LIMITED hardware level above.
     *
     * <p>The test cases bind both ImageCapture and Preview. In the test lib implementation for
     * HDR mode, both use cases will occupy YUV_420_888 format of stream. Therefore, the testing
     * target devices need to be LIMITED hardware level at least to support two YUV_420_888
     * streams at the same time.
     *
     * @return true if the testing target camera device is LIMITED hardware level at least.
     * @throws IllegalArgumentException if unable to retrieve {@link CameraCharacteristics} for
     * given lens facing.
     */
    private static boolean isLimitedAboveDevice(@CameraSelector.LensFacing int lensFacing) {
        CameraCharacteristics cameraCharacteristics = CameraUtil.getCameraCharacteristics(
                lensFacing);

        if (cameraCharacteristics != null) {
            Integer keyValue = cameraCharacteristics.get(
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);

            if (keyValue != null) {
                return keyValue != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
            }
        } else {
            throw new IllegalArgumentException(
                    "Unable to retrieve info for " + lensFacing + " camera.");
        }

        return false;
    }

    /**
     * Returns that whether the device should be skipped for the test.
     */
    private static boolean isSpecificSkippedDevice() {
        return (Build.BRAND.equalsIgnoreCase("SONY") && (Build.MODEL.equalsIgnoreCase("G8142")
                || Build.MODEL.equalsIgnoreCase("G8342")))
                || Build.MODEL.contains("Cuttlefish")
                || Build.MODEL.equalsIgnoreCase("Pixel XL")
                || Build.MODEL.equalsIgnoreCase("Pixel")
                // Skip all devices that have ExtraCropping Quirk
                || Build.MODEL.equalsIgnoreCase("SM-T580")
                || Build.MODEL.equalsIgnoreCase("SM-J710MN")
                || Build.MODEL.equalsIgnoreCase("SM-A320FL")
                || Build.MODEL.equalsIgnoreCase("SM-G570M")
                || Build.MODEL.equalsIgnoreCase("SM-G610F")
                || Build.MODEL.equalsIgnoreCase("SM-G610M");
    }

    /**
     * Returns that whether the device with specific extension mode should be skipped for the test.
     */
    private static boolean isSpecificSkippedDeviceWithExtensionMode(@ExtensionMode.Mode int mode) {
        return "tecno".equalsIgnoreCase(Build.BRAND) && "tecno-ke5".equalsIgnoreCase(Build.DEVICE)
                && (mode == ExtensionMode.HDR || mode == ExtensionMode.NIGHT);
    }

    /**
     * Returns whether extensions is disabled by quirk.
     */
    public static boolean extensionsDisabledByQuirk() {
        return new ExtensionDisabledValidator().shouldDisableExtension();
    }

    /**
     * Sets the camera2 repeating request capture callback to the use case builder.
     */
    public static <T> void setCamera2SessionCaptureCallback(
            ExtendableBuilder<T> usecaseBuilder,
            @NonNull CameraCaptureSession.CaptureCallback captureCallback) {
        usecaseBuilder.getMutableConfig().insertOption(
                SESSION_CAPTURE_CALLBACK_OPTION,
                captureCallback
        );
    }
}
