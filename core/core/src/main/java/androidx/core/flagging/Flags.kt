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

@file:Suppress("BanConcurrentHashMap") // Only used on SDK 36 and above

package androidx.core.flagging

import android.os.Build.VERSION.SDK_INT
import android.os.flagging.AconfigPackage
import android.os.flagging.AconfigStorageReadException
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Wrapper class for accessing [AconfigPackageCompat] flags from Jetpack libraries.
 *
 * This class should only be used when developing against a pre-release platform SDK.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class Flags {
    public companion object {
        @get:RequiresApi(36)
        private val aconfigCache: MutableMap<String, AconfigPackage>? =
            if (SDK_INT >= 36) ConcurrentHashMap() else null

        @get:RequiresApi(36)
        private val missingPackageCache: MutableSet<String>? =
            if (SDK_INT >= 36) CopyOnWriteArraySet() else null

        /**
         * Retrieves the value of a boolean flag from the specified Aconfig Package.
         *
         * If the specified Aconfig Package does not exist or the flag does not exist within the
         * package, returns the provided `defaultValue`. Otherwise, returns the value of the flag.
         *
         * The first call to a given Aconfig Package will result in a call to
         * [AconfigPackageCompat.load]. Subsequent calls will use a cached package. Individual flag
         * values are cached within [AconfigPackageCompat].
         *
         * **Note: This method differs from calling [AconfigPackageCompat] directly in that it will
         * return a default value rather than throw an exception in the event of an error.**
         *
         * Platform-specific behavior:
         * - Prior to API level 36, this method always returns `defaultValue`
         *
         * @param flagName The name of the flag (excluding any package name prefix).
         * @param defaultValue The value to return if the flag is not found.
         * @return The boolean value of the flag, or `defaultValue` if the flag is not found.
         */
        @Suppress("MemberExtensionConflict") // b/406991279 fixed in AGP 8.11.0-alpha05
        @JvmOverloads
        @JvmStatic
        public fun getBooleanFlagValue(
            packageName: String,
            flagName: String,
            defaultValue: Boolean = false,
        ): Boolean {
            if (SDK_INT < 36) {
                return defaultValue
            } else {
                val aconfigPackageCache = aconfigCache!!
                val missingPackageCache = missingPackageCache!!
                var aconfigPackage: AconfigPackage?
                if (aconfigPackageCache.contains(packageName)) {
                    aconfigPackage = aconfigPackageCache[packageName]
                } else if (missingPackageCache.contains(packageName)) {
                    aconfigPackage = null
                } else {
                    try {
                        aconfigPackage = AconfigPackage.load(packageName)
                        aconfigPackageCache[packageName] = aconfigPackage
                    } catch (_: AconfigStorageReadException) {
                        aconfigPackage = null
                        missingPackageCache.add(packageName)
                    }
                }
                return aconfigPackage?.getBooleanFlagValue(flagName, defaultValue) ?: defaultValue
            }
        }
    }
}
