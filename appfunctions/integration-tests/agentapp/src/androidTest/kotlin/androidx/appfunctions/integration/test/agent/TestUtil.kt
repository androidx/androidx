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

package androidx.appfunctions.integration.test.agent

import android.app.UiAutomation
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.fail

internal object TestUtil {
    fun doBlocking(block: suspend CoroutineScope.() -> Unit) = runBlocking(block = block)

    fun interface ThrowRunnable {
        @Throws(Throwable::class) suspend fun run()
    }

    /** Retries an assertion with a delay between attempts. */
    @Throws(Throwable::class)
    suspend fun retryAssert(runnable: ThrowRunnable) {
        var lastError: Throwable? = null

        for (attempt in 0 until RETRY_MAX_INTERVALS) {
            try {
                runnable.run()
                return
            } catch (e: Throwable) {
                lastError = e
                delay(RETRY_CHECK_INTERVAL_MILLIS)
            }
        }
        throw lastError!!
    }

    fun Context.assertPersistedGranted(uri: Uri) {
        val contentResolver = getContentResolver()
        val targetPersistedUri =
            contentResolver.persistedUriPermissions.singleOrNull { uriPermission ->
                uriPermission.uri == uri
            }
        assertThat(targetPersistedUri).isNotNull()
    }

    fun Context.assertNotPersistedGranted(uri: Uri) {
        val contentResolver = getContentResolver()
        val targetPersistedUri =
            contentResolver.persistedUriPermissions.singleOrNull { uriPermission ->
                uriPermission.uri == uri
            }
        assertThat(targetPersistedUri).isNull()
    }

    /** Asserts that the [Context] having read access to [uri]. */
    fun Context.assertReadAccessible(uri: Uri) {
        val contentResolver = getContentResolver()
        try {
            contentResolver.openAssetFile(uri, "r", null).use { fd ->
                if (fd != null) {
                    return
                }
            }
        } catch (_: Exception) {}
        fail("Uri $uri is not read accessible from $packageName")
    }

    /** Asserts that the [Context] not having read access to [uri]. */
    fun Context.assertReadInaccessible(uri: Uri) {
        val contentResolver = getContentResolver()
        try {
            contentResolver.openAssetFile(uri, "r", null).use { fd -> }
        } catch (_: SecurityException) {
            return
        }
        fail("Uri $uri is still read accessible from $packageName")
    }

    /** Asserts that the [Context] having write access to [uri]. */
    fun Context.assertWriteAccessible(uri: Uri) {
        val contentResolver = getContentResolver()
        try {
            val result =
                contentResolver.update(
                    uri,
                    ContentValues().apply { put("echo_value", 100) },
                    Bundle.EMPTY,
                )
            if (result == 100) {
                return
            }
        } catch (_: Exception) {}
        fail("Uri $uri is not write accessible from $packageName")
    }

    /** Asserts that the [Context] not having write access to [uri]. */
    fun Context.assertWriteInaccessible(uri: Uri) {
        val contentResolver = getContentResolver()
        try {
            contentResolver.update(
                uri,
                ContentValues().apply { put("echo_value", 100) },
                Bundle.EMPTY,
            )
        } catch (_: Exception) {
            return
        }
        fail("Uri $uri is still write accessible from $packageName")
    }

    /** Grants [context] the AppFunction access permission for [targetPackageName]. */
    fun UiAutomation.grantAppFunctionAccess(context: Context, targetPackageName: String) {
        if (Build.VERSION.SDK_INT < 37) return
        val packageInfo =
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES,
            )
        val signatures = packageInfo.signingInfo?.signingCertificateHistory
        val certificate =
            if (!signatures.isNullOrEmpty()) {
                val digest =
                    MessageDigest.getInstance("SHA-256").digest(signatures.last().toByteArray())
                digest.joinToString("") { "%02X".format(it) }
            } else {
                throw IllegalStateException(
                    "No signatures found for package ${context.packageName}"
                )
            }
        val signedPackage = "${context.packageName}:$certificate"
        executeShellCommand("cmd app_function purge-allowlist-cache")
        executeShellCommand(
            buildString {
                append("cmd allowlist add-package-multimap")
                append(" ")
                append("$APP_FUNCTION_ALLOWLIST_ID")
                append(" ")
                append(signedPackage)
                append(" ")
                append(targetPackageName)
            }
        )
    }

    /** Revokes AppFunction access. */
    fun UiAutomation.revokeAppFunctionAccess() {
        if (Build.VERSION.SDK_INT < 37) return
        executeShellCommand("cmd app_function purge-allowlist-cache")
        executeShellCommand("cmd allowlist clear-shell-allowlist $APP_FUNCTION_ALLOWLIST_ID")
    }

    private const val RETRY_CHECK_INTERVAL_MILLIS: Long = 500
    private const val RETRY_MAX_INTERVALS: Long = 10
    private const val APP_FUNCTION_ALLOWLIST_ID: Int = 2
}
