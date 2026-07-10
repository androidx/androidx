/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.camera.extensions.impl.advanced;

import android.hardware.camera2.CameraExtensionCharacteristics;

import androidx.camera.extensions.impl.ExtensionVersionImpl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A factory class for creating {@link AdvancedExtenderImpl} instances.
 *
 * <p>This class is part of the OEM implementation of the CameraX Extensions library. It follows a
 * singleton pattern to provide a single point of entry for creating extension-specific
 * implementations. Vendors must implement the logic within this class to return instances of their
 * custom {@link AdvancedExtenderImpl} for each supported extension mode.</p>
 *
 * <p>Do note that the existing public extension stubs like {@link NightAdvancedExtenderImpl},
 * {@link BokehAdvancedExtenderImpl}, {@link HdrAdvancedExtenderImpl} and
 * {@link AutoAdvancedExtenderImpl} must continue to be implemented, and included in the vendor
 * extension library.</p>
 *
 * <p>Along with all existing public extension
 * {@link android.hardware.camera2.CameraExtensionCharacteristics types}, vendors are allowed
 * to add support for device-specific extensions starting with the first vendor extension
 * {@link android.hardware.camera2.CameraExtensionCharacteristics#EXTENSION_VENDOR_START mode}.
 * Do note that the range from the current public extension modes until
 * {@link android.hardware.camera2.CameraExtensionCharacteristics#EXTENSION_VENDOR_START} is
 * reserved and client requests for extension modes within this range must return null.</p>
 *
 * @see AdvancedExtenderImpl
 * @see ExtensionVersionImpl
 *
 * @since 1.6
 */
public class AdvancedExtenderCreator {

    private AdvancedExtenderCreator() {
        // add vendor specific logic
    }

    private static final class AdvancedExtenderCreatorHolder {
        // The instance is created only when AdvancedExtenderCreatorHolder is loaded, which happens
        // when getInstance() is called.
        static final AdvancedExtenderCreator INSTANCE = new AdvancedExtenderCreator();
    }

    /**
     * Returns the singleton instance of the {@code AdvancedExtenderCreator}.
     *
     * <p>This method is thread-safe.
     *
     * @return The singleton {@code AdvancedExtenderCreator} instance.
     * @since 1.6
     */
    @NonNull public static AdvancedExtenderCreator getInstance() {
        return AdvancedExtenderCreatorHolder.INSTANCE;
    }

    /**
     * Creates an {@link AdvancedExtenderImpl} for the specified extension mode.
     *
     * <p>OEMs must implement this method to return a concrete implementation of
     * {@link AdvancedExtenderImpl} for any extension mode they support. If the given
     * {@code extensionMode} is not supported, this method should return {@code null}.</p>
     *
     * @param extensionMode The extension mode to be created. This will be one of the
     *                      {@code CameraExtensionCharacteristics.EXTENSION_*} constants.
     *                      Vendor implementation are allowed to add support for device-specific
     *                      extensions starting with the first vendor extension
     * {@link android.hardware.camera2.CameraExtensionCharacteristics#EXTENSION_VENDOR_START mode}.
     *                      Do note that the range from the current public extension modes until
     * {@link android.hardware.camera2.CameraExtensionCharacteristics#EXTENSION_VENDOR_START} is
     *                      reserved and client requests for extension modes within this range must
     *                      return null.
     * @return An instance of {@link AdvancedExtenderImpl} for the specified mode, or {@code null}
     *         if the mode is not supported.
     * @since 1.6
     */
    @Nullable public AdvancedExtenderImpl createAdvancedExtender(int extensionMode) {
        switch (extensionMode) {
            case CameraExtensionCharacteristics.EXTENSION_AUTOMATIC:
                return new AutoAdvancedExtenderImpl();
            case CameraExtensionCharacteristics.EXTENSION_FACE_RETOUCH:
                return new BeautyAdvancedExtenderImpl();
            case CameraExtensionCharacteristics.EXTENSION_BOKEH:
                return new BokehAdvancedExtenderImpl();
            case CameraExtensionCharacteristics.EXTENSION_HDR:
                return new HdrAdvancedExtenderImpl();
            case CameraExtensionCharacteristics.EXTENSION_NIGHT:
                return new NightAdvancedExtenderImpl();
            default:
                return null;
        }
    }
}
