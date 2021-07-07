/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.extensions.internal;

import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraInfo;
import androidx.camera.extensions.ExtensionMode;
import androidx.camera.extensions.impl.advanced.AdvancedExtenderImpl;
import androidx.camera.extensions.impl.advanced.AutoAdvancedExtenderImpl;
import androidx.camera.extensions.impl.advanced.BeautyAdvancedExtenderImpl;
import androidx.camera.extensions.impl.advanced.BokehAdvancedExtenderImpl;
import androidx.camera.extensions.impl.advanced.HdrAdvancedExtenderImpl;
import androidx.camera.extensions.impl.advanced.NightAdvancedExtenderImpl;

import java.util.Map;

/**
 * Advanced vendor interface implementation
 */
public class AdvancedVendorExtender implements VendorExtender {
    private final AdvancedExtenderImpl mAdvancedExtenderImpl;

    public AdvancedVendorExtender(@ExtensionMode.Mode int mode) {
        try {
            switch (mode) {
                case ExtensionMode.BOKEH:
                    mAdvancedExtenderImpl = new BokehAdvancedExtenderImpl();
                    break;
                case ExtensionMode.HDR:
                    mAdvancedExtenderImpl = new HdrAdvancedExtenderImpl();
                    break;
                case ExtensionMode.NIGHT:
                    mAdvancedExtenderImpl = new NightAdvancedExtenderImpl();
                    break;
                case ExtensionMode.BEAUTY:
                    mAdvancedExtenderImpl = new BeautyAdvancedExtenderImpl();
                    break;
                case ExtensionMode.AUTO:
                    mAdvancedExtenderImpl = new AutoAdvancedExtenderImpl();
                    break;
                case ExtensionMode.NONE:
                default:
                    throw new IllegalArgumentException("Should not active ExtensionMode.NONE");
            }
        } catch (NoClassDefFoundError e) {
            throw new IllegalArgumentException("AdvancedExtenderImpl does not exist");
        }
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    @Override
    public void init(@NonNull CameraInfo cameraInfo) {
        String cameraId = Camera2CameraInfo.from(cameraInfo).getCameraId();

        Map<String, CameraCharacteristics> cameraCharacteristicsMap =
                Camera2CameraInfo.from(cameraInfo).getCameraCharacteristicsMap();

        mAdvancedExtenderImpl.init(cameraId, cameraCharacteristicsMap);
    }

    @Override
    public boolean isExtensionAvailable(@NonNull String cameraId,
            @NonNull Map<String, CameraCharacteristics> characteristicsMap) {
        return mAdvancedExtenderImpl.isExtensionAvailable(cameraId, characteristicsMap);
    }
}
