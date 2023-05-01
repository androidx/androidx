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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.extensions.impl.ExtensionVersionImpl;

import org.jetbrains.annotations.TestOnly;

/**
 * Provides interfaces to check the extension version.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public abstract class ExtensionVersion {
    private static final String TAG = "ExtenderVersion";

    private static volatile ExtensionVersion sExtensionVersion;

    /**
     * For testing only. Inject a fake {@link ExtensionVersion}. Set it to {@code null} to unset
     * it.
     */
    @TestOnly
    public static void injectInstance(@Nullable ExtensionVersion extensionVersion) {
        sExtensionVersion = extensionVersion;
    }

    private static ExtensionVersion getInstance() {
        if (sExtensionVersion != null) {
            return sExtensionVersion;
        }
        synchronized (ExtensionVersion.class) {
            if (sExtensionVersion == null) {
                try {
                    sExtensionVersion = new VendorExtenderVersioning();
                } catch (NoClassDefFoundError e) {
                    Logger.d(TAG, "No versioning extender found. Falling back to default.");
                    sExtensionVersion = new DefaultExtenderVersioning();
                }
            }
        }

        return sExtensionVersion;
    }

    /**
     * Indicate the compatibility of CameraX and OEM library.
     *
     * @return true if OEM returned a major version is matched with the current version, false
     * otherwise.
     */
    public static boolean isExtensionVersionSupported() {
        return getInstance().getVersionObject() != null;
    }

    /**
     * Return the Version object of the OEM library if the version is compatible with CameraX.
     *
     * @return a Version object which composed of the version number string that's returned from
     * {@link ExtensionVersionImpl#checkApiVersion(String)}.
     * <tt>null</tt> if the OEM library didn't implement the version checking method or the
     * version is not compatible with CameraX.
     */
    @Nullable
    public static Version getRuntimeVersion() {
        return getInstance().getVersionObject();
    }

    public static boolean isAdvancedExtenderSupported() {
        return getInstance().isAdvancedExtenderSupportedInternal();
    }

    /**
     * Check if the Runtime Version meets the minimum compatible version requirement. This implies
     * that the runtime version is equal to or newer than the version.
     *
     * <p> The compatible version is comprised of the major and minor version numbers. The patch
     * number is ignored.
     *
     * @param version The minimum compatible version required
     * @return True if the Runtime version meets the minimum version requirement and False
     * otherwise.
     */
    public static boolean isMinimumCompatibleVersion(@NonNull Version version) {
        return ExtensionVersion.getRuntimeVersion()
                .compareTo(version.getMajor(), version.getMinor()) >= 0;
    }

    /**
     * Check if the Runtime Version meets the maximum compatible version requirement. This implies
     * that the runtime version is equal to or older than the version.
     *
     * <p> The compatible version is comprised of the major and minor version numbers. The patch
     * number is ignored.
     *
     * @param version The maximum compatible version required
     * @return True if the Runtime version meets the maximum version requirement and False
     * otherwise.
     */
    public static boolean isMaximumCompatibleVersion(@NonNull Version version) {
        return ExtensionVersion.getRuntimeVersion()
                .compareTo(version.getMajor(), version.getMinor()) <= 0;
    }

    abstract boolean isAdvancedExtenderSupportedInternal();

    /**
     * @return a Version object returned from the extension implementation.
     */
    abstract Version getVersionObject();

    /** An implementation that calls into the vendor provided implementation. */
    private static class VendorExtenderVersioning extends ExtensionVersion {
        private static ExtensionVersionImpl sImpl;
        private Version mRuntimeVersion;

        VendorExtenderVersioning() {
            if (sImpl == null) {
                sImpl = new ExtensionVersionImpl();
            }

            String vendorVersion = sImpl.checkApiVersion(
                    VersionName.getCurrentVersion().toVersionString());
            Version vendorVersionObj = Version.parse(vendorVersion);
            if (vendorVersionObj != null
                    && VersionName.getCurrentVersion().getVersion().getMajor()
                    == vendorVersionObj.getMajor()) {
                mRuntimeVersion = vendorVersionObj;
            }

            Logger.d(TAG, "Selected vendor runtime: " + mRuntimeVersion);
        }

        @Override
        Version getVersionObject() {
            return mRuntimeVersion;
        }

        @Override
        boolean isAdvancedExtenderSupportedInternal() {
            try {
                return sImpl.isAdvancedExtenderImplemented();
            } catch (NoSuchMethodError e) {
                return false;
            }
        }
    }

    /** Empty implementation of ExtensionVersion which does nothing. */
    private static class DefaultExtenderVersioning extends ExtensionVersion {
        DefaultExtenderVersioning() {
        }

        @Override
        Version getVersionObject() {
            return null;
        }

        @Override
        boolean isAdvancedExtenderSupportedInternal() {
            return false;
        }
    }
}
