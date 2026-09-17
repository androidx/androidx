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
@file:OptIn(ExperimentalAppFunctionsApi::class)

package androidx.appfunctions.internal

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Process
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.appfunctions.AppFunctionDeniedException
import androidx.appfunctions.ExperimentalAppFunctionsApi
import androidx.appfunctions.metadata.AppFunctionMetadata
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Encapsulates caller identity and access verification for backward compatibility on pre-17.2
 * platforms.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object CallerAccessVerifier {
    @VisibleForTesting
    internal var testingSender: (suspend (Context, PendingIntent) -> VerificationResult)? = null

    /**
     * Extra key used to pass a [PendingIntent] caller verification token across IPC.
     *
     * The caller creates a [PendingIntent] targeting its [AppFunctionCallerVerificationReceiver].
     * The recipient can inspect [PendingIntent.getCreatorUid] to reliably determine the caller's
     * UID without relying on unauthenticated Bundle fields. When invoked by the recipient, the
     * receiver returns [android.app.Activity.RESULT_OK], confirming the token was actively issued
     * by the authentic caller.
     */
    internal const val EXTRA_CALLER_VERIFICATION_PENDING_INTENT =
        "androidx.appfunctions.extra.CALLER_VERIFICATION_PENDING_INTENT"

    /**
     * Extra key used to pass an in-process [IBinder] caller identity token.
     *
     * In-process calls can share a direct [IBinder] object reference. If the token matches
     * [selfBinderToken], the caller is verified to be within the same process, bypassing the need
     * for [PendingIntent] / [android.content.BroadcastReceiver] IPC round-trip overhead.
     */
    internal const val EXTRA_CALLER_BINDER_TOKEN = "androidx.appfunctions.extra.CALLER_BINDER_TOKEN"

    /** In-process token instance used to verify that a caller resides in the same process. */
    internal val selfBinderToken: IBinder = Binder()

    private const val SDK_INT_FULL_CINNAMON_BUN_2 = 3700002

    fun isAtLeastCinnamonBunMinor2(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA &&
            Api36Impl.getSdkIntFull() >= SDK_INT_FULL_CINNAMON_BUN_2
    }

    /**
     * Attaches backward compatibility caller verification tokens to the request extras if running
     * on platform versions prior to Android 17.2.
     */
    fun attachCallerVerificationTokens(
        context: Context,
        @AppFunctionMetadata.AccessLevel accessLevel: Int,
        extras: Bundle,
    ) {
        if (isAtLeastCinnamonBunMinor2()) {
            return
        }
        when (accessLevel) {
            AppFunctionMetadata.ACCESS_LEVEL_SELF -> {
                extras.putBinder(EXTRA_CALLER_BINDER_TOKEN, selfBinderToken)
                extras.putParcelable(
                    EXTRA_CALLER_VERIFICATION_PENDING_INTENT,
                    createVerificationPendingIntent(context),
                )
            }
            AppFunctionMetadata.ACCESS_LEVEL_SYSTEM -> {
                extras.putParcelable(
                    EXTRA_CALLER_VERIFICATION_PENDING_INTENT,
                    createVerificationPendingIntent(context),
                )
            }
        }
    }

    private fun createVerificationPendingIntent(context: Context): PendingIntent {
        val intent =
            Intent(context, AppFunctionCallerVerificationReceiver::class.java).apply {
                action = "androidx.appfunctions.action.VERIFY_CALLER_" + UUID.randomUUID()
            }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /**
     * Verifies that the caller meets the required access level of the app function.
     *
     * On Android 17.2+, access enforcement is handled natively by the platform framework and this
     * method returns immediately. On earlier platforms, if the function has
     * [AppFunctionMetadata.isCompatEnforcementEnabled] enabled:
     * - [AppFunctionMetadata.ACCESS_LEVEL_ANDROID_TRUSTED]: Accessible by all certified callers.
     * - [AppFunctionMetadata.ACCESS_LEVEL_SELF]: Verified via in-process [selfBinderToken]
     *   fast-path, or via [PendingIntent] token proving caller UID matches [Process.myUid].
     * - [AppFunctionMetadata.ACCESS_LEVEL_SYSTEM]: Verified via [PendingIntent] token proving
     *   caller UID has the system permission `android.permission.EXECUTE_APP_FUNCTIONS_SYSTEM`.
     */
    suspend fun verifyCallerAccess(
        context: Context,
        metadata: AppFunctionMetadata,
        extras: Bundle,
    ) {
        if (isAtLeastCinnamonBunMinor2()) {
            return // Platform enforces natively
        }

        if (!metadata.isCompatEnforcementEnabled) {
            // Dev opted out of access enforcement on older platforms: allow execution for any
            // caller, matching the platform behavior on pre-17.2 devices.
            return
        }

        when (metadata.accessLevel) {
            AppFunctionMetadata.ACCESS_LEVEL_ANDROID_TRUSTED -> return
            AppFunctionMetadata.ACCESS_LEVEL_SELF -> verifySelfAccess(context, extras)
            AppFunctionMetadata.ACCESS_LEVEL_SYSTEM -> verifySystemAccess(context, extras)
            else ->
                throw AppFunctionDeniedException("Unknown access level: ${metadata.accessLevel}")
        }
    }

    private suspend fun verifySelfAccess(
        context: Context,
        extras: Bundle,
    ) {
        val binderToken = extras.getBinder(EXTRA_CALLER_BINDER_TOKEN)
        if (binderToken != null && binderToken == selfBinderToken) {
            return
        }

        // Fall back to checking the PendingIntent when the in-process binder token does not match.
        // The caller may belong to the same app (same UID) but reside in a separate process,
        // which does not share the in-memory binder token instance.
        verifyCallerPendingIntent(
            context = context,
            extras = extras,
            requiredUidCheck = { it == Process.myUid() },
            missingTokenMessage = "Execution denied: Missing caller verification token.",
            uidMismatchMessage =
                "Execution denied: Caller UID does not match host application UID.",
        )
    }

    private suspend fun verifySystemAccess(
        context: Context,
        extras: Bundle,
    ) {
        verifyCallerPendingIntent(
            context = context,
            extras = extras,
            requiredUidCheck = { isUidSystem(context, it) },
            missingTokenMessage = "Execution denied: Missing system caller verification token.",
            uidMismatchMessage = "Execution denied: Caller lacks EXECUTE_APP_FUNCTIONS_SYSTEM.",
        )
    }

    private suspend fun verifyCallerPendingIntent(
        context: Context,
        extras: Bundle,
        requiredUidCheck: (Int) -> Boolean,
        missingTokenMessage: String,
        uidMismatchMessage: String,
    ) {
        @Suppress("DEPRECATION")
        val pendingIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(
                    EXTRA_CALLER_VERIFICATION_PENDING_INTENT,
                    PendingIntent::class.java,
                )
            } else {
                extras.getParcelable(EXTRA_CALLER_VERIFICATION_PENDING_INTENT) as? PendingIntent
            } ?: throw AppFunctionDeniedException(missingTokenMessage)

        val creatorUid = pendingIntent.creatorUid
        if (!requiredUidCheck(creatorUid)) {
            throw AppFunctionDeniedException(uidMismatchMessage)
        }

        when (sendVerification(context, pendingIntent)) {
            VerificationResult.SUCCESS -> {}
            VerificationResult.TIMEOUT ->
                throw AppFunctionDeniedException(
                    "Execution denied: Caller verification timed out waiting for caller confirmation receiver."
                )
            VerificationResult.FAILED ->
                throw AppFunctionDeniedException("Execution denied: Caller signature invalid.")
        }
    }

    internal enum class VerificationResult {
        SUCCESS,
        TIMEOUT,
        FAILED,
    }

    private suspend fun sendVerification(
        context: Context,
        pendingIntent: PendingIntent,
    ): VerificationResult {
        val customSender = testingSender
        if (customSender != null) {
            return customSender(context, pendingIntent)
        }
        return withTimeoutOrNull(1000L) {
            suspendCancellableCoroutine { cont ->
                try {
                    pendingIntent.send(
                        context,
                        0,
                        null,
                        { _, _, resultCode, _, _ ->
                            cont.resume(
                                if (resultCode == Activity.RESULT_OK) {
                                    VerificationResult.SUCCESS
                                } else {
                                    VerificationResult.FAILED
                                }
                            )
                        },
                        null,
                    )
                } catch (e: PendingIntent.CanceledException) {
                    cont.resume(VerificationResult.FAILED)
                }
            }
        } ?: VerificationResult.TIMEOUT
    }

    /**
     * Checks if the caller identity can discover an app function on older platform versions (<
     * 17.2).
     *
     * For [AppFunctionMetadata.ACCESS_LEVEL_SELF], discovery is restricted to callers sharing the
     * same application UID as the target package.
     */
    fun canCallerDiscoverFunction(
        context: Context,
        targetPackageName: String,
        @AppFunctionMetadata.AccessLevel accessLevel: Int,
        isCompatEnforcementEnabled: Boolean = true,
    ): Boolean {
        if (isAtLeastCinnamonBunMinor2() || !isCompatEnforcementEnabled) {
            return true
        }
        return when (accessLevel) {
            AppFunctionMetadata.ACCESS_LEVEL_ANDROID_TRUSTED -> true
            AppFunctionMetadata.ACCESS_LEVEL_SELF -> isSelf(context, targetPackageName)
            AppFunctionMetadata.ACCESS_LEVEL_SYSTEM ->
                isSelf(context, targetPackageName) || isCallerSystem(context)
            else -> false
        }
    }

    /** Determines whether the target package shares the same UID as the caller [Process.myUid]. */
    private fun isSelf(context: Context, targetPackageName: String): Boolean {
        if (context.packageName == targetPackageName) {
            return true
        }
        return try {
            context.packageManager.getPackageUid(targetPackageName, 0) == Process.myUid()
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isCallerSystem(context: Context): Boolean {
        val uid = Process.myUid()
        return isUidSystem(context, uid)
    }

    private fun isUidSystem(context: Context, uid: Int): Boolean {
        return context.checkPermission(
            "android.permission.EXECUTE_APP_FUNCTIONS_SYSTEM",
            -1,
            uid,
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(36)
    private object Api36Impl {
        @DoNotInline fun getSdkIntFull(): Int = Build.VERSION.SDK_INT_FULL
    }
}
