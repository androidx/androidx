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

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;

import androidx.camera.core.CameraSelector;
import androidx.camera.extensions.ExtensionMode;
import androidx.camera.extensions.ExtensionsManager;
import androidx.camera.extensions.internal.Camera2ExtensionsInfo;
import androidx.camera.extensions.internal.Camera2ExtensionsVendorExtender;
import androidx.camera.extensions.internal.VendorExtender;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.testing.impl.CameraUtil;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Extension test util functions.
 */
public class ExtensionsTestUtil {
    private static final int BASE_COMBINATION_ARRAY_POS_MODE = 0;
    private static final int BASE_COMBINATION_ARRAY_POS_LENS_FACING = 1;

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
        return extensionsManager.isExtensionAvailable(cameraSelector, extensionMode);
    }

    /**
     * Returns the parameters which contains the combination of extensions mode and lens facing.
     */
    public static @NonNull Collection<Object[]> getAllExtensionsLensFacingCombinations(
            @NonNull Context context,
            boolean excludeUnavailableModes
    ) {
        // BASE_COMBINATION_ARRAY_POS_MODE = 0
        // BASE_COMBINATION_ARRAY_POS_LENS_FACING = 1
        List<Object[]> allPossibleCombinations = Arrays.asList(new Object[][]{
                {BOKEH, CameraSelector.LENS_FACING_FRONT},
                {BOKEH, CameraSelector.LENS_FACING_BACK},
                {HDR, CameraSelector.LENS_FACING_FRONT},
                {HDR, CameraSelector.LENS_FACING_BACK},
                {FACE_RETOUCH, CameraSelector.LENS_FACING_FRONT},
                {FACE_RETOUCH, CameraSelector.LENS_FACING_BACK},
                {NIGHT, CameraSelector.LENS_FACING_FRONT},
                {NIGHT, CameraSelector.LENS_FACING_BACK},
                {AUTO, CameraSelector.LENS_FACING_FRONT},
                {AUTO, CameraSelector.LENS_FACING_BACK}
        });

        return excludeUnavailableModes ? filterOutUnavailableMode(context, allPossibleCombinations)
                : allPossibleCombinations;
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
                int mode = (int) item[BASE_COMBINATION_ARRAY_POS_MODE];
                int lensFacing = (int) item[BASE_COMBINATION_ARRAY_POS_LENS_FACING];
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

    /**
     * Returns whether the target camera device can support the test for a specific extension mode.
     */
    public static boolean isTargetDeviceAvailableForExtensions(
            @CameraSelector.LensFacing int lensFacing, @ExtensionMode.Mode int mode) {
        return CameraUtil.hasCameraWithLensFacing(lensFacing) && isLimitedAboveDevice(lensFacing)
                && !isSpecificSkippedDevice() && !isSpecificSkippedDeviceWithExtensionMode(mode);
    }

    /**
     * Creates the {@link VendorExtender} instance for testing.
     *
     * @param applicationContext the application context which will be used to retrieve the
     *                           camera characteristics info.
     * @param mode               the target extension mode.
     * @return the corresponding {@link VendorExtender} instance.
     */
    public static @NonNull VendorExtender createVendorExtender(@NonNull Context applicationContext,
            @ExtensionMode.Mode int mode) {
        // Returns Camera2ExtensionsVendorExtender only when API level is 33 or above.
        // CameraExtensionCharacteristics#getAvailableCaptureRequestKeys(int) is supported since
        // API level 33 that allows app to clearly know whether features like tap-to-focus or zoom
        // ratio are supported or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            CameraManager cameraManager = applicationContext.getSystemService(
                    CameraManager.class);
            return new Camera2ExtensionsVendorExtender(mode,
                    new Camera2ExtensionsInfo(cameraManager));
        } else {
            return new VendorExtender() {
            };
        }
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
}
