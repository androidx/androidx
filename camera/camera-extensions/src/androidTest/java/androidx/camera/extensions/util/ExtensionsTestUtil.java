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

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraSelector;
import androidx.camera.extensions.ExtensionMode;
import androidx.camera.extensions.internal.AdvancedVendorExtender;
import androidx.camera.extensions.internal.BasicVendorExtender;
import androidx.camera.extensions.internal.ExtensionVersion;
import androidx.camera.extensions.internal.VendorExtender;
import androidx.camera.extensions.internal.Version;
import androidx.camera.extensions.internal.compat.workaround.ExtensionDisabledValidator;
import androidx.camera.testing.impl.CameraUtil;

import java.util.Arrays;
import java.util.Collection;

/**
 * Extension test util functions.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ExtensionsTestUtil {
    @NonNull
    public static Collection<Object[]> getAllExtensionsLensFacingCombinations() {
        return Arrays.asList(new Object[][]{
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
}
