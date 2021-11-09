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

package androidx.camera.extensions.impl;

/**
 * Stub implementation for the extension version check.
 *
 * <p>This class should be implemented by OEM and deployed to the target devices.
 *
 * @since 1.0
 */
public class ExtensionVersionImpl {
    public ExtensionVersionImpl() {
    }

    /**
     * Provide the current CameraX extension library version to vendor library and vendor would
     * need to return the supported version for this device. If the returned version is not
     * supported by CameraX library, the Preview and ImageCapture would not be able to enable the
     * specific effects provided by the vendor.
     *
     * <p>CameraX library provides the Semantic Versioning string in a form of
     * MAJOR.MINOR.PATCH-description
     * We will increment the
     * MAJOR version when make incompatible API changes,
     * MINOR version when add functionality in a backwards-compatible manner, and
     * PATCH version when make backwards-compatible bug fixes. And the description can be ignored.
     *
     * <p>Vendor library should provide MAJOR.MINOR.PATCH to CameraX. The MAJOR and MINOR
     * version is used to map to the version of CameraX that it supports, and CameraX extension
     * would only available when MAJOR version is matched with CameraX current version. The PATCH
     * version does not indicate compatibility. The patch version should be incremented whenever
     * the vendor library makes bug fixes or updates to the algorithm.
     *
     * @param version the version of CameraX library formatted as MAJOR.MINOR.PATCH-description.
     * @return the version that vendor supported in this device. The MAJOR.MINOR.PATCH format
     * should be used.
     */
    public String checkApiVersion(String version) {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    /**
     * Specify whether or not CameraX should invoke the AdvancedExtenderImpl instead of
     * PreviewExtenderImpl/ImageCaptureExtenderImpl.
     *
     * <p>Starting from version 1.2, a set of alternative interfaces called advanced extender for
     * implementing extensions are provided to OEMs as another option. OEMs can continue using
     * previous interfaces (PreviewExtenderImpl/ImageCaptureExtenderImpl, also called basic
     * extender).
     *
     * <p>OEMs should return false here if only basic extender is implemented. When returning true,
     * CameraX will invoke the AdvancedExtenderImpl implementation in advanced package for all
     * types of extension modes.
     *
     * <p>ExtensionVersionImpl, InitializerImpl will still be called for both basic and advanced
     * extender implementation paths.
     *
     * @return true if AdvancedExtenderImpl is implemented
     * @since 1.2
     */
    public boolean isAdvancedExtenderImplemented() {
        return false;
    }
}
