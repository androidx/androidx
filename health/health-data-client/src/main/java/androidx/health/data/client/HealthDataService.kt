package androidx.health.data.client

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RestrictTo
import java.lang.UnsupportedOperationException

/** Retrieves available implementation of [HealthDataClient]. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public object HealthDataService {

    private const val DEFAULT_PROVIDER_PACKAGE_NAME = "com.google.android.apps.healthdata"

    /**
     * Determines whether there [HealthDataClient] is available on this device at the moment.
     *
     * @param packageNames Optional package provider to choose implementation from
     * @return whether the api is available
     */
    @SuppressLint("ObsoleteSdkInt") // Will aim to support lower SDK.
    @JvmOverloads
    @JvmStatic
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
     * @param packageNames Optional package provider to choose implementation from
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
        throw UnsupportedOperationException("Not implemented yet")
    }

    @Suppress("Deprecation") // getApplicationInfo is deprecated only in T.
    private fun isPackageInstalled(packageManager: PackageManager, packageName: String): Boolean {
        return try {
            return packageManager.getApplicationInfo(packageName, /* flags= */ 0).enabled
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
