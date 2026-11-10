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

package androidx.xr.arcore.integration.tests

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.xr.runtime.NativeInstanceData
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateResult
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.XrDevice
import androidx.xr.runtime.getNativeInstanceData
import androidx.xr.runtime.getNativeSessionData
import androidx.xr.testutils.XrDeviceTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.test.assertFailsWith
import org.junit.AssumptionViolatedException
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

// TODO: b/584314628 - Isolate pre-init extension tests in an orchestrator-enabled test module
// matching :webkit:integration-tests:orchestratortests (after consulting the AndroidX core team)
// so this test does not skip in shared-process CI runs.
/**
 * Automated integration tests for JXR Runtime [XrDevice.getNativeInstanceData] and OpenXR extension
 * injection contracts.
 *
 * Pre-initialization extension injection can only run before [Session] or [XrDevice] initializes
 * the process-wide `XrInstanceManager` singleton. Enabling `useOrchestrator` on this shared test
 * module or editing root `settings.gradle` for a dedicated orchestrator module requires consulting
 * the AndroidX core team, so [MethodSorters.NAME_ASCENDING] keeps the pre-init test ordered before
 * post-init tests in standalone runs (`-e class ...NativeDataTest`).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class NativeDataTest {

    private sealed interface PreInitExtensionOutcome {
        data object InvalidExtensionAccepted : PreInitExtensionOutcome

        data class RejectedAndCreatedSession(
            val invalidFailure: UnsupportedOperationException,
            val sessionResult: SessionCreateResult,
            val nativeInstanceData: NativeInstanceData?,
        ) : PreInitExtensionOutcome
    }

    @Suppress("RestrictedApiAndroidX")
    private fun probeInvalidExtensionRejection(context: Context): UnsupportedOperationException? {
        return try {
            XrDevice.getCurrentDevice(context, listOf(INVALID_OPENXR_EXTENSION))
            null
        } catch (e: IllegalStateException) {
            if (e.message?.contains(ALREADY_INITIALIZED_ERROR) == true) {
                throw AssumptionViolatedException(
                    "Skipping pre-init test: XrInstanceManager is already initialized in this " +
                        "process"
                )
            }
            throw e
        } catch (e: UnsupportedOperationException) {
            e
        }
    }

    @Suppress("RestrictedApiAndroidX")
    private fun nativeInstanceDataOrNull(
        device: XrDevice,
        result: SessionCreateResult,
    ): NativeInstanceData? =
        (result as? SessionCreateSuccess)?.let { device.getNativeInstanceData(it.session.context) }

    @Suppress("RestrictedApiAndroidX")
    private suspend fun runPreInitExtensionCheck(context: Context): PreInitExtensionOutcome {
        return try {
            val invalidFailure =
                probeInvalidExtensionRejection(context)
                    ?: return PreInitExtensionOutcome.InvalidExtensionAccepted
            val validDevice = XrDevice.getCurrentDevice(context, listOf(VALID_OPENXR_EXTENSION))
            val sessionResult = createSessionResultOrSkip(context)
            PreInitExtensionOutcome.RejectedAndCreatedSession(
                invalidFailure = invalidFailure,
                sessionResult = sessionResult,
                nativeInstanceData = nativeInstanceDataOrNull(validDevice, sessionResult),
            )
        } finally {
            resetPendingExtensionsIfUninitialized(context)
        }
    }

    /**
     * Initializes `XrInstanceManager` with empty extra extensions if still uninitialized so a
     * skipped or aborted pre-init check never leaves an invalid or unsupported extension name
     * pending for subsequent tests.
     */
    @Suppress("RestrictedApiAndroidX")
    private fun resetPendingExtensionsIfUninitialized(context: Context) {
        try {
            XrDevice.getCurrentDevice(context, emptyList())
        } catch (e: IllegalStateException) {
            if (e.message?.contains(ALREADY_INITIALIZED_ERROR) == true) {
                return
            }
            throw e
        }
    }

    // TODO: b/543942634 - Remove once the legacy natives are removed.
    /**
     * Resolves the native XrSession handle across both modular and legacy OpenXR native binaries:
     * legacy natives expose XrSession only through [Session.getNativeSessionData] (retained while
     * modular natives roll out for 1P support), whereas modular natives expose it directly via
     * [NativeInstanceData.sessionPointer].
     */
    @Suppress("RestrictedApiAndroidX")
    private fun resolveNativeSessionPointer(
        session: Session,
        instanceData: NativeInstanceData,
    ): Long {
        if (isUsingLegacyNatives()) {
            @Suppress("DEPRECATION")
            return session.getNativeSessionData().sessionPointer
        }
        return instanceData.sessionPointer
    }

    @Test
    @XrDeviceTest
    fun getCurrentDevice_beforeSessionCreate_rejectsInvalidThenAllowsValidExtensions() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            runWithActivity(scenario) { activity ->
                val outcome = runPreInitExtensionCheck(activity)

                assertWithMessage(
                        "Expected UnsupportedOperationException for '$INVALID_OPENXR_EXTENSION'"
                    )
                    .that(outcome)
                    .isInstanceOf(PreInitExtensionOutcome.RejectedAndCreatedSession::class.java)
                val rejectedOutcome = outcome as PreInitExtensionOutcome.RejectedAndCreatedSession
                assertThat(rejectedOutcome.sessionResult)
                    .isInstanceOf(SessionCreateSuccess::class.java)
                assertThat(rejectedOutcome.nativeInstanceData?.instancePointer ?: 0L)
                    .isNotEqualTo(0L)
                assertThat(rejectedOutcome.nativeInstanceData?.functionTablePointer ?: 0L)
                    .isNotEqualTo(0L)
            }
        }
    }

    @Test
    @XrDeviceTest
    fun getNativeInstanceData_calledRepeatedly_returnsConsistentHandles() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            initSessionAndRunTest(scenario) { session ->
                val xrDevice = XrDevice.getCurrentDevice(session.context)
                val firstInstanceData = xrDevice.getNativeInstanceData(session.context)
                check(
                    firstInstanceData.instancePointer != 0L &&
                        firstInstanceData.functionTablePointer != 0L
                ) {
                    "Expected non-zero native instance and function table handles for initial " +
                        "call, got instance=${firstInstanceData.instancePointer}, " +
                        "functionTable=${firstInstanceData.functionTablePointer}"
                }

                val secondInstanceData = xrDevice.getNativeInstanceData(session.context)

                assertThat(secondInstanceData).isEqualTo(firstInstanceData)
            }
        }
    }

    @Test
    @XrDeviceTest
    fun getNativeInstanceData_whenSessionCreated_returnsNonZeroPointers() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            initSessionAndRunTest(scenario) { session ->
                val xrDevice = XrDevice.getCurrentDevice(session.context)

                val nativeInstanceData = xrDevice.getNativeInstanceData(session.context)
                val sessionPointer = resolveNativeSessionPointer(session, nativeInstanceData)

                assertThat(nativeInstanceData.instancePointer).isNotEqualTo(0L)
                assertThat(nativeInstanceData.functionTablePointer).isNotEqualTo(0L)
                assertThat(sessionPointer).isNotEqualTo(0L)
            }
        }
    }

    @Suppress("RestrictedApiAndroidX")
    @Test
    @XrDeviceTest
    fun getCurrentDevice_whenSessionCreated_throwsIllegalStateException() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            initSessionAndRunTest(scenario) { session ->
                val extensions = listOf(VALID_OPENXR_EXTENSION)

                assertFailsWith<IllegalStateException> {
                    XrDevice.getCurrentDevice(session.context, extensions)
                }
            }
        }
    }

    private companion object {
        const val INVALID_OPENXR_EXTENSION = "XR_INVALID_EXTENSION_NAME"
        const val VALID_OPENXR_EXTENSION = "XR_ANDROID_trackables_marker"
        const val ALREADY_INITIALIZED_ERROR = "already been initialized"

        // TODO: b/543942634 - Remove once the legacy natives are removed.
        /**
         * Returns whether legacy Jetpack XR natives are active, matching native lookup of the
         * `debug.jetpack_xr_natives.use_legacy` system property (legacy unless `"0"` or `"false"`).
         */
        @Suppress("BanUncheckedReflection")
        fun isUsingLegacyNatives(): Boolean =
            try {
                val value =
                    Class.forName("android.os.SystemProperties")
                        .getMethod("get", String::class.java)
                        .invoke(/* obj= */ null, "debug.jetpack_xr_natives.use_legacy") as? String
                value != "0" && value != "false"
            } catch (_: ReflectiveOperationException) {
                true
            } catch (_: SecurityException) {
                true
            }
    }
}
