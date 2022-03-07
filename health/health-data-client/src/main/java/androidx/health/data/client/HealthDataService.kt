/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.data.client

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.health.data.client.impl.HealthDataClientImpl
import androidx.health.platform.client.HealthDataService
import java.lang.UnsupportedOperationException

/**
 * Entry point for connecting to Health Data Provider on the device and creating instances of
 * [HealthDataClient].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
object HealthDataService {
    private const val DEFAULT_PROVIDER_PACKAGE_NAME = "com.google.android.apps.healthdata"

    /**
     * Determines whether there [HealthDataClient] is available on this device at the moment.
     *
     * @param packageNames optional package provider to choose implementation from
     * @return whether the api is available
     */
    @JvmOverloads
    @JvmStatic
    @Suppress("ObsoleteSdkInt") // We will aim to support lower
    public fun isAvailable(
        context: Context,
        packageNames: List<String> = listOf(DEFAULT_PROVIDER_PACKAGE_NAME),
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            return false
        }
        return packageNames.any { isPackageInstalled(context.packageManager, it) }
    }

    /**
     * Creates an IPC-backed [HealthDataClient] instance binding to an available implementation.
     *
     * @param packageNames optional package provider to choose implementation from
     * @return instance of [HealthDataClient] ready for issuing requests
     * @throws UnsupportedOperationException if service not available
     */
    @JvmOverloads
    @JvmStatic
    public fun getClient(
        context: Context,
        packageNames: List<String> = listOf(DEFAULT_PROVIDER_PACKAGE_NAME)
    ): HealthDataClient {
        if (!isAvailable(context, packageNames)) {
            throw UnsupportedOperationException("Not supported yet")
        }
        val enabledPackage = packageNames.first { isPackageInstalled(context.packageManager, it) }
        return HealthDataClientImpl(HealthDataService.getClient(context, enabledPackage))
    }

    @Suppress("Deprecation") // getApplicationInfo deprecated in T but is the only choice.
    private fun isPackageInstalled(packageManager: PackageManager, packageName: String): Boolean {
        return try {
            return packageManager.getApplicationInfo(packageName, /* flags= */ 0).enabled
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
