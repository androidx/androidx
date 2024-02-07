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
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.pipe.integration.CameraPipeConfig;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ExtendableBuilder;
import androidx.camera.core.impl.Config;
import androidx.camera.extensions.ExtensionMode;
import androidx.camera.extensions.ExtensionsManager;
import androidx.camera.extensions.impl.ExtensionsTestlibControl;
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
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ExtensionsTestUtil {
    public static final Config.Option<CameraCaptureSession.CaptureCallback>
            SESSION_CAPTURE_CALLBACK_OPTION =
            Config.Option.create("camera2.cameraCaptureSession.captureCallback",
                    CameraCaptureSession.CaptureCallback.class);
    public static final String CAMERA2_IMPLEMENTATION_OPTION = "camera2";
    public static final String CAMERA_PIPE_IMPLEMENTATION_OPTION = "camera_pipe";

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
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build();
                if (extensionsManager.isExtensionAvailable(cameraSelector, mode)) {
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
                || Build.MODEL.equalsIgnoreCase("Pixel");
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
