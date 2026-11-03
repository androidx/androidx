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

import android.app.AppInteractionException
import android.app.AppInteractionManager
import android.app.AppInteractionSession
import android.app.UiAutomation
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.OutcomeReceiver
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.AppFunctionName
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
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

    suspend fun Context.awaitAppFunctionsIndexed(
        targetPackage: String,
        expectedFunctionIds: Set<String> = emptySet(),
        unexpectedFunctionIds: Set<String> = emptySet(),
    ) {
        retryAssert {
            val functionIds = AppSearchMetadataHelper.collectFunctionIds(this, targetPackage)
            assertThat(functionIds).isNotEmpty()
            if (expectedFunctionIds.isNotEmpty()) {
                assertThat(functionIds).containsAtLeastElementsIn(expectedFunctionIds)
            }
            if (unexpectedFunctionIds.isNotEmpty()) {
                assertThat(functionIds).containsNoneIn(unexpectedFunctionIds)
            }
        }
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
            contentResolver.openAssetFile(uri, "r", null).use { _ -> }
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
        executeShellCommandSync("cmd app_function purge-allowlist-cache")
        executeShellCommandSync(
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
        executeShellCommandSync("cmd app_function set-temporary-caller ${context.packageName}")
        executeShellCommandSync("cmd app_function flush-allowlist-changes")
    }

    /** Revokes AppFunction access. */
    fun UiAutomation.revokeAppFunctionAccess() {
        if (Build.VERSION.SDK_INT < 37) return
        executeShellCommandSync("cmd app_function purge-allowlist-cache")
        executeShellCommandSync("cmd allowlist clear-shell-allowlist $APP_FUNCTION_ALLOWLIST_ID")
        executeShellCommandSync("cmd app_function clear-temporary-caller")
        executeShellCommandSync("cmd app_function flush-allowlist-changes")
    }

    /** Grants [context] the AppInteraction access permission for [targetPackageName]. */
    fun UiAutomation.grantAppInteractionAccess(context: Context, targetPackageName: String) {
        getAppInteractionAccessController().grantAccess(this, context, targetPackageName)
    }

    /** Revokes [context] the AppInteraction access permission for [targetPackageName]. */
    fun UiAutomation.revokeAppInteractionAccess(context: Context, targetPackageName: String) {
        getAppInteractionAccessController().revokeAccess(this, context, targetPackageName)
    }

    private fun getAppInteractionAccessController(): AppInteractionAccessController {
        return if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN &&
                Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.CINNAMON_BUN_2
        ) {
            Api37Point2AppInteractionAccessController
        } else {
            NoOpAppInteractionAccessController
        }
    }

    private interface AppInteractionAccessController {
        fun grantAccess(
            uiAutomation: UiAutomation,
            context: Context,
            targetPackageName: String,
        )

        fun revokeAccess(
            uiAutomation: UiAutomation,
            context: Context,
            targetPackageName: String,
        )
    }

    private object NoOpAppInteractionAccessController : AppInteractionAccessController {
        override fun grantAccess(
            uiAutomation: UiAutomation,
            context: Context,
            targetPackageName: String,
        ) {}

        override fun revokeAccess(
            uiAutomation: UiAutomation,
            context: Context,
            targetPackageName: String,
        ) {}
    }

    @RequiresApi(Build.VERSION_CODES_FULL.CINNAMON_BUN_2)
    private object Api37Point2AppInteractionAccessController : AppInteractionAccessController {
        override fun grantAccess(
            uiAutomation: UiAutomation,
            context: Context,
            targetPackageName: String,
        ) {
            updateAccessFlags(
                uiAutomation,
                context,
                targetPackageName,
                APP_INTERACTION_ACCESS_FLAG_USER_GRANTED,
            )
        }

        override fun revokeAccess(
            uiAutomation: UiAutomation,
            context: Context,
            targetPackageName: String,
        ) {
            updateAccessFlags(uiAutomation, context, targetPackageName, 0)
        }

        private fun updateAccessFlags(
            uiAutomation: UiAutomation,
            context: Context,
            targetPackageName: String,
            flags: Int,
        ) {
            // Use reflection to call updateAccessFlags and preserve existing adopted shell
            // permissions so we don't leak permission details to callers; this can be replaced
            // with the `cmd app_interaction grant-access/revoke-access` shell commands in 27Q1.
            @Suppress("UNCHECKED_CAST")
            val previousPermissions =
                UiAutomation::class
                    .java
                    .getMethod("getAdoptedShellPermissions")
                    .invoke(uiAutomation) as Set<String>
            val allPermissions = setOf("ALL_PERMISSIONS")
            try {
                if (previousPermissions != allPermissions) {
                    uiAutomation.adoptShellPermissionIdentity(
                        *(previousPermissions + MANAGE_APP_INTERACTION_ACCESS_PERMISSION)
                            .toTypedArray()
                    )
                }
                val appInteractionManager =
                    checkNotNull(context.getSystemService(AppInteractionManager::class.java))
                val updateAccessFlagsMethod =
                    AppInteractionManager::class
                        .java
                        .getMethod(
                            "updateAccessFlags",
                            String::class.java,
                            String::class.java,
                            Int::class.javaPrimitiveType,
                            Int::class.javaPrimitiveType,
                        )
                updateAccessFlagsMethod.invoke(
                    appInteractionManager,
                    context.packageName,
                    targetPackageName,
                    APP_INTERACTION_ACCESS_FLAG_MASK_ALL,
                    flags,
                )
            } finally {
                when {
                    previousPermissions.isEmpty() -> uiAutomation.dropShellPermissionIdentity()
                    previousPermissions != allPermissions ->
                        uiAutomation.adoptShellPermissionIdentity(
                            *previousPermissions.toTypedArray()
                        )
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES_FULL.CINNAMON_BUN_2)
    suspend fun AppInteractionManager.createSessionAndAllowThisTime(
        targetContext: Context,
        targetPackages: List<String>,
    ): AppInteractionSession = coroutineScope {
        val intentSenderDeferred = CompletableDeferred<IntentSender>()
        val createParams =
            AppInteractionSession.CreateParams.Builder()
                .setOnStartIntentSenderCallback(
                    { it.run() },
                    { sender -> intentSenderDeferred.complete(sender) },
                )
                .setTargetPackages(targetPackages)
                .build()
        val sessionDeferred = async {
            this@createSessionAndAllowThisTime.createSession(createParams)
        }

        val intentSender = withTimeout(LONG_TIMEOUT_SECOND * 1000) { intentSenderDeferred.await() }
        assertThat(intentSender).isNotNull()
        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        uiDevice.wakeUp()
        uiDevice.executeShellCommand("wm dismiss-keyguard")
        targetContext.startIntentSender(
            intentSender,
            Intent().apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            0,
            0,
            0,
            null,
        )
        val allowThisTimeButton =
            uiDevice.wait(
                Until.findObject(
                    By.res("com.android.permissioncontroller:id/only_this_time_button")
                ),
                UI_TIMEOUT_MS,
            )
        assertThat(allowThisTimeButton).isNotNull()
        allowThisTimeButton.click()
        withTimeout(LONG_TIMEOUT_SECOND * 1000) { sessionDeferred.await() }
    }

    @RequiresApi(Build.VERSION_CODES_FULL.CINNAMON_BUN_2)
    suspend fun AppInteractionManager.createSession(
        params: AppInteractionSession.CreateParams
    ): AppInteractionSession {
        return suspendCancellableCoroutine { continuation ->
            createSession(
                params,
                Runnable::run,
                object : OutcomeReceiver<AppInteractionSession, AppInteractionException> {
                    override fun onResult(result: AppInteractionSession) {
                        continuation.resume(result)
                    }

                    override fun onError(error: AppInteractionException) {
                        continuation.resumeWithException(error)
                    }
                },
            )
        }
    }

    /** Starts a background service using shell command to bypass background start restrictions. */
    fun UiAutomation.startService(packageName: String, className: String, action: String) {
        executeShellCommandSync("cmd deviceidle tempwhitelist -d 10000 $packageName")
        executeShellCommandSync("am startservice -a $action -n $packageName/$className")
    }

    /** Force stops the given package using shell command. */
    fun UiAutomation.forceStop(packageName: String) {
        executeShellCommandSync("am force-stop $packageName")
    }

    /** Sets the app function with the given state. */
    fun setAppFunctionStateRemoteAsync(appFunctionName: AppFunctionName, state: Int) = doBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent =
            android.content.Intent("androidx.appfunctions.integration.testapp.ACTION_SET_STATE")
        intent.setPackage(appFunctionName.packageName)
        intent.putExtra("function_id", appFunctionName.functionIdentifier)
        intent.putExtra("state", state)
        context.sendBroadcast(intent)
    }

    /** Verifies that the given app function has the given enabled state. */
    suspend fun AppFunctionManager.assertAppFunctionEnabledState(
        targetFunctionName: AppFunctionName,
        expectedEnabled: Boolean,
    ) {
        val isEnabled =
            getAppFunctionStates(
                    appFunctionNames =
                        listOf(
                            AppFunctionName(
                                targetFunctionName.packageName,
                                targetFunctionName.functionIdentifier,
                            )
                        )
                )
                .single()
                .isEnabled
        assertThat(isEnabled).isEqualTo(expectedEnabled)
    }

    private fun UiAutomation.executeShellCommandSync(command: String): String =
        AutoCloseInputStream(executeShellCommand(command)).bufferedReader().use { it.readText() }

    private const val RETRY_CHECK_INTERVAL_MILLIS: Long = 500
    private const val RETRY_MAX_INTERVALS: Long = 10
    private const val LONG_TIMEOUT_SECOND: Long = 10
    private const val UI_TIMEOUT_MS: Long = 5000
    private const val APP_FUNCTION_ALLOWLIST_ID: Int = 2
    private const val APP_INTERACTION_ACCESS_FLAG_USER_GRANTED: Int = 1 shl 4
    private const val APP_INTERACTION_ACCESS_FLAG_MASK_ALL: Int = (1 shl 9) - 1
    private const val MANAGE_APP_INTERACTION_ACCESS_PERMISSION =
        "android.permission.MANAGE_APP_INTERACTION_ACCESS"
}
