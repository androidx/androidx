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

package androidx.core.flagging

import android.os.Build.VERSION.SDK_INT
import android.os.flagging.AconfigPackage
import android.os.flagging.AconfigStorageReadException
import androidx.annotation.RequiresApi

/**
 * An Aconfig Package containing the enabled state of its flags.
 *
 * **Note: This class is intended only to be used by generated code. To determine if a given flag is
 * enabled in app or library code, the generated Android flag accessor methods should be used.**
 *
 * This class is used to read the flags from an Aconfig Package. Each instance of this class will
 * cache information related to one package. To read flags from a different package, a new instance
 * of this class should be created using [AconfigPackageCompat.load].
 */
public interface AconfigPackageCompat {
    /**
     * Retrieves the value of a boolean flag.
     *
     * This method retrieves the value of the specified flag. If the flag exists within the loaded
     * Aconfig Package, its value is returned. Otherwise, the provided `defaultValue` is returned.
     *
     * Platform-specific behavior:
     * - Prior to API level 36, this method always returns `defaultValue`
     *
     * @param flagName The name of the flag (excluding any package name prefix).
     * @param defaultValue The value to return if the flag is not found.
     * @return The boolean value of the flag, or `defaultValue` if the flag is not found.
     */
    public fun getBooleanFlagValue(flagName: String, defaultValue: Boolean): Boolean

    public companion object {
        /**
         * Loads an Aconfig Package from Aconfig Storage.
         *
         * This method attempts to load the specified Aconfig package.
         *
         * **Note: This method differs from the platform implementation in that is guaranteed to
         * return an empty package if the [packageName] is not found in the container, rather than
         * throwing an exception.**
         *
         * Platform-specific behavior:
         * - Prior to API level 36, this method always returns an empty package
         *
         * @param packageName The name of the Aconfig package to load.
         * @return An instance of [AconfigPackageCompat], which may be empty if the package is not
         *   found in the container.
         * @throws AconfigStorageReadException if there is an error reading from Aconfig Storage,
         *   such as if the storage system is not found or there is an error reading the storage
         *   file. The specific error code can be obtained using
         *   [AconfigStorageReadException.getErrorCode].
         */
        @Suppress("BannedThrow") // Parity with platform SDK
        @Throws(AconfigStorageReadException::class)
        @JvmStatic
        public fun load(packageName: String): AconfigPackageCompat =
            if (SDK_INT >= 36) {
                try {
                    AconfigPackageCompatApi36Impl(AconfigPackage.load(packageName))
                } catch (e: AconfigStorageReadException) {
                    if (e.errorCode == AconfigStorageReadException.ERROR_PACKAGE_NOT_FOUND) {
                        AconfigPackageCompatNoopImpl()
                    } else {
                        throw e
                    }
                }
            } else {
                AconfigPackageCompatNoopImpl()
            }
    }
}

@RequiresApi(36)
private class AconfigPackageCompatApi36Impl(private val aconfigPackageImpl: AconfigPackage) :
    AconfigPackageCompat {
    override fun getBooleanFlagValue(flagName: String, defaultValue: Boolean): Boolean =
        aconfigPackageImpl.getBooleanFlagValue(flagName, defaultValue)
}

private class AconfigPackageCompatNoopImpl : AconfigPackageCompat {
    override fun getBooleanFlagValue(flagName: String, defaultValue: Boolean): Boolean =
        defaultValue
}
