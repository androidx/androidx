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

package androidx.window.extensions;

import androidx.annotation.NonNull;

/**
 * A class to return global information about the library. From this class you can get the
 * library version corresponding to the extensions jar. This is useful since multiple versions of
 * extensions may have the same API.
 *
 * @see WindowLibraryInfo#getLibraryVersion()
 */
public class WindowLibraryInfo {

    private WindowLibraryInfo() {
    }

    /**
     * Gets the version of the vendor library on this device. If the returned version is not
     * supported by the WindowManager library, then some functions may not be available or
     * replaced with stub implementations.
     *
     * <p>WindowManager library provides the Semantic Versioning string in a form of
     * MAJOR.MINOR.PATCH-description
     * We will increment the
     * MAJOR version when make incompatible API changes,
     * MINOR version when add functionality in a backwards-compatible manner, and
     * PATCH version when make backwards-compatible bug fixes.
     * And the description can be ignored.
     *
     * <p>Vendor extension library should provide MAJOR.MINOR.PATCH to the WindowManager library.
     * The MAJOR and MINOR version are used to identify the interface version that the library will
     * use. The PATCH version does not indicate compatibility. The patch version should be
     * incremented whenever the vendor library makes bug fixes or updates to the algorithm.
     *
     * @return the version that vendor supported in this device. The MAJOR.MINOR.PATCH format
     * should be used.
     */
    @NonNull
    public static String getLibraryVersion() {
        throw new UnsupportedOperationException("Stub, replace with implementation.");
    }
}
