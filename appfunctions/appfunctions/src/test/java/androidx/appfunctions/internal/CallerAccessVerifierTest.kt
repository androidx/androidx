/*
 * Copyright 2026 The Android Open Source Project
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

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.os.Binder
import android.os.Bundle
import android.os.Process
import androidx.appfunctions.AppFunctionDeniedException
import androidx.appfunctions.ExperimentalAppFunctionsApi
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionName
import androidx.appfunctions.metadata.AppFunctionPackageMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionUnitTypeMetadata
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowProcess

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 33)
class CallerAccessVerifierTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val shadowApplication = shadowOf(application)

    @Before
    fun setUp() {
        val shadowPackageManager = shadowOf(context.packageManager)
        shadowPackageManager.setPackagesForUid(Process.myUid(), context.packageName)
    }

    @After
    fun tearDown() {
        CallerAccessVerifier.testingSender = null
        shadowApplication.denyPermissions("android.permission.EXECUTE_APP_FUNCTIONS_SYSTEM")
    }

    private fun createMetadata(
        accessLevel: Int = AppFunctionMetadata.ACCESS_LEVEL_ANDROID_TRUSTED,
        isCompatEnforcementEnabled: Boolean = true,
        packageName: String = context.packageName,
    ): AppFunctionMetadata {
        val name = AppFunctionName(packageName, "testFunction")
        return AppFunctionMetadata(
            name = name,
            schema = null,
            parameters = emptyList(),
            response =
                AppFunctionResponseMetadata(
                    valueType = AppFunctionUnitTypeMetadata(),
                    description = "unit",
                ),
            packageMetadata =
                AppFunctionPackageMetadata(
                    packageName = packageName,
                    components = AppFunctionComponentsMetadata(),
                ),
            accessLevel = accessLevel,
            isCompatEnforcementEnabled = isCompatEnforcementEnabled,
        )
    }

    @Test
    fun verifyCallerAccess_androidTrusted_allowsWithoutExtras() = runTest {
        val metadata =
            createMetadata(accessLevel = AppFunctionMetadata.ACCESS_LEVEL_ANDROID_TRUSTED)
        CallerAccessVerifier.verifyCallerAccess(context, metadata, Bundle())
    }

    @Test
    fun verifyCallerAccess_enforcementOnOlderPlatformsDisabled_allowsWithoutExtras() = runTest {
        val metadata =
            createMetadata(
                accessLevel = AppFunctionMetadata.ACCESS_LEVEL_SELF,
                isCompatEnforcementEnabled = false,
            )
        CallerAccessVerifier.verifyCallerAccess(context, metadata, Bundle())
    }

    @Test
    fun verifyCallerAccess_self_missingToken_throwsDenied() = runTest {
        val metadata = createMetadata(accessLevel = AppFunctionMetadata.ACCESS_LEVEL_SELF)
        val exception =
            assertFailsWith<AppFunctionDeniedException> {
                CallerAccessVerifier.verifyCallerAccess(context, metadata, Bundle())
            }
        assertThat(exception.message).contains("Missing caller verification token")
    }

    @Test
    fun verifyCallerAccess_self_uidMismatch_throwsDenied() = runTest {
        val originalUid = Process.myUid()
        try {
            ShadowProcess.setUid(12345)
            val metadata = createMetadata(accessLevel = AppFunctionMetadata.ACCESS_LEVEL_SELF)
            val extras = Bundle()
            CallerAccessVerifier.attachCallerVerificationTokens(
                context,
                AppFunctionMetadata.ACCESS_LEVEL_SELF,
                extras,
            )
            extras.remove(CallerAccessVerifier.EXTRA_CALLER_BINDER_TOKEN)

            ShadowProcess.setUid(originalUid)

            val exception =
                assertFailsWith<AppFunctionDeniedException> {
                    CallerAccessVerifier.verifyCallerAccess(context, metadata, extras)
                }
            assertThat(exception.message).contains("Caller UID does not match host application UID")
        } finally {
            ShadowProcess.setUid(originalUid)
        }
    }

    @Test
    fun verifyCallerAccess_self_validBinderToken_succeedsSynchronously() = runTest {
        val metadata = createMetadata(accessLevel = AppFunctionMetadata.ACCESS_LEVEL_SELF)
        val extras = Bundle()
        extras.putBinder(
            CallerAccessVerifier.EXTRA_CALLER_BINDER_TOKEN,
            CallerAccessVerifier.selfBinderToken,
        )

        // Verifies synchronously without requiring any testingSender or PendingIntent
        CallerAccessVerifier.verifyCallerAccess(context, metadata, extras)
    }

    @Test
    fun verifyCallerAccess_self_invalidBinderToken_fallsBackToPendingIntent() = runTest {
        val originalUid = Process.myUid()
        try {
            ShadowProcess.setUid(0)
            CallerAccessVerifier.testingSender = { _, _ ->
                CallerAccessVerifier.VerificationResult.SUCCESS
            }
            val metadata = createMetadata(accessLevel = AppFunctionMetadata.ACCESS_LEVEL_SELF)
            val extras = Bundle()
            CallerAccessVerifier.attachCallerVerificationTokens(
                context,
                AppFunctionMetadata.ACCESS_LEVEL_SELF,
                extras,
            )
            // Unknown foreign Binder token
            extras.putBinder(CallerAccessVerifier.EXTRA_CALLER_BINDER_TOKEN, Binder())

            CallerAccessVerifier.verifyCallerAccess(context, metadata, extras)
        } finally {
            ShadowProcess.setUid(originalUid)
        }
    }

    @Test
    fun verifyCallerAccess_self_invalidBinderToken_noPendingIntent_throwsDenied() = runTest {
        val metadata = createMetadata(accessLevel = AppFunctionMetadata.ACCESS_LEVEL_SELF)
        val extras = Bundle()
        extras.putBinder(CallerAccessVerifier.EXTRA_CALLER_BINDER_TOKEN, Binder())

        val exception =
            assertFailsWith<AppFunctionDeniedException> {
                CallerAccessVerifier.verifyCallerAccess(context, metadata, extras)
            }
        assertThat(exception.message).contains("Missing caller verification token")
    }

    @Test
    fun verifyCallerAccess_self_validToken_succeeds() = runTest {
        val originalUid = Process.myUid()
        try {
            ShadowProcess.setUid(0)
            CallerAccessVerifier.testingSender = { _, _ ->
                CallerAccessVerifier.VerificationResult.SUCCESS
            }
            val metadata = createMetadata(accessLevel = AppFunctionMetadata.ACCESS_LEVEL_SELF)
            val extras = Bundle()
            CallerAccessVerifier.attachCallerVerificationTokens(
                context,
                AppFunctionMetadata.ACCESS_LEVEL_SELF,
                extras,
            )
            extras.remove(CallerAccessVerifier.EXTRA_CALLER_BINDER_TOKEN)

            CallerAccessVerifier.verifyCallerAccess(context, metadata, extras)
        } finally {
            ShadowProcess.setUid(originalUid)
        }
    }

    @Test
    fun verifyCallerAccess_self_verificationFails_throwsDenied() = runTest {
        val originalUid = Process.myUid()
        try {
            ShadowProcess.setUid(0)
            CallerAccessVerifier.testingSender = { _, _ ->
                CallerAccessVerifier.VerificationResult.FAILED
            }
            val metadata = createMetadata(accessLevel = AppFunctionMetadata.ACCESS_LEVEL_SELF)
            val extras = Bundle()
            CallerAccessVerifier.attachCallerVerificationTokens(
                context,
                AppFunctionMetadata.ACCESS_LEVEL_SELF,
                extras,
            )
            extras.remove(CallerAccessVerifier.EXTRA_CALLER_BINDER_TOKEN)

            val exception =
                assertFailsWith<AppFunctionDeniedException> {
                    CallerAccessVerifier.verifyCallerAccess(context, metadata, extras)
                }
            assertThat(exception.message).contains("Caller signature invalid")
        } finally {
            ShadowProcess.setUid(originalUid)
        }
    }

    @Test
    fun verifyCallerAccess_self_verificationTimeout_throwsDenied() = runTest {
        val originalUid = Process.myUid()
        try {
            ShadowProcess.setUid(0)
            CallerAccessVerifier.testingSender = { _, _ ->
                CallerAccessVerifier.VerificationResult.TIMEOUT
            }
            val metadata = createMetadata(accessLevel = AppFunctionMetadata.ACCESS_LEVEL_SELF)
            val extras = Bundle()
            CallerAccessVerifier.attachCallerVerificationTokens(
                context,
                AppFunctionMetadata.ACCESS_LEVEL_SELF,
                extras,
            )
            extras.remove(CallerAccessVerifier.EXTRA_CALLER_BINDER_TOKEN)

            val exception =
                assertFailsWith<AppFunctionDeniedException> {
                    CallerAccessVerifier.verifyCallerAccess(context, metadata, extras)
                }
            assertThat(exception.message).contains("Caller verification timed out")
        } finally {
            ShadowProcess.setUid(originalUid)
        }
    }

    @Test
    fun verifyCallerAccess_system_missingToken_throwsDenied() = runTest {
        val metadata = createMetadata(accessLevel = AppFunctionMetadata.ACCESS_LEVEL_SYSTEM)
        val exception =
            assertFailsWith<AppFunctionDeniedException> {
                CallerAccessVerifier.verifyCallerAccess(context, metadata, Bundle())
            }
        assertThat(exception.message).contains("Missing system caller verification token")
    }

    @Test
    fun verifyCallerAccess_system_callerLacksPermission_throwsDenied() = runTest {
        val metadata = createMetadata(accessLevel = AppFunctionMetadata.ACCESS_LEVEL_SYSTEM)
        val extras = Bundle()
        CallerAccessVerifier.attachCallerVerificationTokens(
            context,
            AppFunctionMetadata.ACCESS_LEVEL_SYSTEM,
            extras,
        )

        val exception =
            assertFailsWith<AppFunctionDeniedException> {
                CallerAccessVerifier.verifyCallerAccess(context, metadata, extras)
            }
        assertThat(exception.message).contains("Caller lacks EXECUTE_APP_FUNCTIONS_SYSTEM")
    }

    @Test
    fun verifyCallerAccess_system_validToken_succeeds() = runTest {
        shadowApplication.grantPermissions("android.permission.EXECUTE_APP_FUNCTIONS_SYSTEM")
        CallerAccessVerifier.testingSender = { _, _ ->
            CallerAccessVerifier.VerificationResult.SUCCESS
        }
        val metadata = createMetadata(accessLevel = AppFunctionMetadata.ACCESS_LEVEL_SYSTEM)
        val extras = Bundle()
        CallerAccessVerifier.attachCallerVerificationTokens(
            context,
            AppFunctionMetadata.ACCESS_LEVEL_SYSTEM,
            extras,
        )
        @Suppress("DEPRECATION")
        val pi =
            extras.getParcelable<PendingIntent>(
                CallerAccessVerifier.EXTRA_CALLER_VERIFICATION_PENDING_INTENT
            )!!
        shadowOf(pi).setCreatorUid(Process.myUid())

        CallerAccessVerifier.verifyCallerAccess(context, metadata, extras)
    }

    @Test
    fun verifyCallerAccess_system_verificationFails_throwsDenied() = runTest {
        shadowApplication.grantPermissions("android.permission.EXECUTE_APP_FUNCTIONS_SYSTEM")
        CallerAccessVerifier.testingSender = { _, _ ->
            CallerAccessVerifier.VerificationResult.FAILED
        }
        val metadata = createMetadata(accessLevel = AppFunctionMetadata.ACCESS_LEVEL_SYSTEM)
        val extras = Bundle()
        CallerAccessVerifier.attachCallerVerificationTokens(
            context,
            AppFunctionMetadata.ACCESS_LEVEL_SYSTEM,
            extras,
        )
        @Suppress("DEPRECATION")
        val pi =
            extras.getParcelable<PendingIntent>(
                CallerAccessVerifier.EXTRA_CALLER_VERIFICATION_PENDING_INTENT
            )!!
        shadowOf(pi).setCreatorUid(Process.myUid())

        val exception =
            assertFailsWith<AppFunctionDeniedException> {
                CallerAccessVerifier.verifyCallerAccess(context, metadata, extras)
            }
        assertThat(exception.message).contains("Caller signature invalid")
    }

    @Test
    fun verifyCallerAccess_system_verificationTimeout_throwsDenied() = runTest {
        shadowApplication.grantPermissions("android.permission.EXECUTE_APP_FUNCTIONS_SYSTEM")
        CallerAccessVerifier.testingSender = { _, _ ->
            CallerAccessVerifier.VerificationResult.TIMEOUT
        }
        val metadata = createMetadata(accessLevel = AppFunctionMetadata.ACCESS_LEVEL_SYSTEM)
        val extras = Bundle()
        CallerAccessVerifier.attachCallerVerificationTokens(
            context,
            AppFunctionMetadata.ACCESS_LEVEL_SYSTEM,
            extras,
        )
        @Suppress("DEPRECATION")
        val pi =
            extras.getParcelable<PendingIntent>(
                CallerAccessVerifier.EXTRA_CALLER_VERIFICATION_PENDING_INTENT
            )!!
        shadowOf(pi).setCreatorUid(Process.myUid())

        val exception =
            assertFailsWith<AppFunctionDeniedException> {
                CallerAccessVerifier.verifyCallerAccess(context, metadata, extras)
            }
        assertThat(exception.message).contains("Caller verification timed out")
    }

    @Test
    fun verifyCallerAccess_system_canceledPendingIntent_throwsDenied() = runTest {
        shadowApplication.grantPermissions("android.permission.EXECUTE_APP_FUNCTIONS_SYSTEM")
        CallerAccessVerifier.testingSender = null
        val metadata = createMetadata(accessLevel = AppFunctionMetadata.ACCESS_LEVEL_SYSTEM)
        val extras = Bundle()
        CallerAccessVerifier.attachCallerVerificationTokens(
            context,
            AppFunctionMetadata.ACCESS_LEVEL_SYSTEM,
            extras,
        )
        @Suppress("DEPRECATION")
        val pi =
            extras.getParcelable<PendingIntent>(
                CallerAccessVerifier.EXTRA_CALLER_VERIFICATION_PENDING_INTENT
            )!!
        shadowOf(pi).setCreatorUid(Process.myUid())
        pi.cancel()

        val exception =
            assertFailsWith<AppFunctionDeniedException> {
                CallerAccessVerifier.verifyCallerAccess(context, metadata, extras)
            }
        assertThat(exception.message).contains("Caller signature invalid")
    }

    @Test
    fun attachCallerVerificationTokens_self_addsTokens() {
        val extras = Bundle()
        CallerAccessVerifier.attachCallerVerificationTokens(
            context,
            AppFunctionMetadata.ACCESS_LEVEL_SELF,
            extras,
        )
        if (!CallerAccessVerifier.isAtLeastCinnamonBunMinor2()) {
            assertThat(extras.getBinder(CallerAccessVerifier.EXTRA_CALLER_BINDER_TOKEN))
                .isEqualTo(CallerAccessVerifier.selfBinderToken)
            @Suppress("DEPRECATION")
            assertThat(
                    extras.getParcelable<PendingIntent>(
                        CallerAccessVerifier.EXTRA_CALLER_VERIFICATION_PENDING_INTENT
                    )
                )
                .isNotNull()
        }
    }

    @Test
    fun attachCallerVerificationTokens_system_addsPendingIntentOnly() {
        val extras = Bundle()
        CallerAccessVerifier.attachCallerVerificationTokens(
            context,
            AppFunctionMetadata.ACCESS_LEVEL_SYSTEM,
            extras,
        )
        if (!CallerAccessVerifier.isAtLeastCinnamonBunMinor2()) {
            assertThat(extras.getBinder(CallerAccessVerifier.EXTRA_CALLER_BINDER_TOKEN)).isNull()
            @Suppress("DEPRECATION")
            assertThat(
                    extras.getParcelable<PendingIntent>(
                        CallerAccessVerifier.EXTRA_CALLER_VERIFICATION_PENDING_INTENT
                    )
                )
                .isNotNull()
        }
    }

    @Test
    fun attachCallerVerificationTokens_androidTrusted_doesNotAddTokens() {
        val extras = Bundle()
        CallerAccessVerifier.attachCallerVerificationTokens(
            context,
            AppFunctionMetadata.ACCESS_LEVEL_ANDROID_TRUSTED,
            extras,
        )
        assertThat(extras.isEmpty).isTrue()
    }

    @Test
    fun canCallerDiscoverFunction_androidTrusted_returnsTrue() {
        val result =
            CallerAccessVerifier.canCallerDiscoverFunction(
                context,
                "some.other.package",
                AppFunctionMetadata.ACCESS_LEVEL_ANDROID_TRUSTED,
            )
        assertThat(result).isTrue()
    }

    @Test
    fun canCallerDiscoverFunction_self_samePackage_returnsTrue() {
        val result =
            CallerAccessVerifier.canCallerDiscoverFunction(
                context,
                context.packageName,
                AppFunctionMetadata.ACCESS_LEVEL_SELF,
            )
        assertThat(result).isTrue()
    }

    @Test
    fun canCallerDiscoverFunction_self_otherPackage_returnsFalse() {
        val result =
            CallerAccessVerifier.canCallerDiscoverFunction(
                context,
                "some.other.package",
                AppFunctionMetadata.ACCESS_LEVEL_SELF,
            )
        assertThat(result).isFalse()
    }

    @Test
    fun canCallerDiscoverFunction_system_samePackage_returnsTrue() {
        val result =
            CallerAccessVerifier.canCallerDiscoverFunction(
                context,
                context.packageName,
                AppFunctionMetadata.ACCESS_LEVEL_SYSTEM,
            )
        assertThat(result).isTrue()
    }

    @Test
    fun canCallerDiscoverFunction_system_otherPackageNotSystemCaller_returnsFalse() {
        val result =
            CallerAccessVerifier.canCallerDiscoverFunction(
                context,
                "some.other.package",
                AppFunctionMetadata.ACCESS_LEVEL_SYSTEM,
            )
        assertThat(result).isFalse()
    }

    @Test
    fun canCallerDiscoverFunction_system_callerWithSystemPermission_returnsTrue() {
        shadowApplication.grantPermissions("android.permission.EXECUTE_APP_FUNCTIONS_SYSTEM")
        val result =
            CallerAccessVerifier.canCallerDiscoverFunction(
                context,
                "some.other.package",
                AppFunctionMetadata.ACCESS_LEVEL_SYSTEM,
            )
        assertThat(result).isTrue()
    }

    @Test
    fun canCallerDiscoverFunction_self_otherPackage_enforcementDisabled_returnsTrue() {
        val result =
            CallerAccessVerifier.canCallerDiscoverFunction(
                context,
                "some.other.package",
                AppFunctionMetadata.ACCESS_LEVEL_SELF,
                isCompatEnforcementEnabled = false,
            )
        assertThat(result).isTrue()
    }

    @Test
    fun canCallerDiscoverFunction_system_otherPackage_enforcementDisabled_returnsTrue() {
        val result =
            CallerAccessVerifier.canCallerDiscoverFunction(
                context,
                "some.other.package",
                AppFunctionMetadata.ACCESS_LEVEL_SYSTEM,
                isCompatEnforcementEnabled = false,
            )
        assertThat(result).isTrue()
    }
}
