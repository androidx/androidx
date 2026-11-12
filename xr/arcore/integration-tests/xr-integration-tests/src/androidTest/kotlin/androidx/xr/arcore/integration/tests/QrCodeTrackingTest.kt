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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.QrCode
import androidx.xr.arcore.TrackingState
import androidx.xr.runtime.Config
import androidx.xr.runtime.QrCodeTrackingMode
import androidx.xr.runtime.SessionConfigureResult
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.filterSupportedPermissions
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.AssumptionViolatedException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated integration tests for JXR ARCore [QrCode] tracking subscription, decoded payload
 * inspection, and [QrCodeTrackingMode] configuration.
 *
 * Target-detection tests expect a physical QR fixture encoding `"https://www.google.com"`. When QR
 * code size estimation is unsupported, tests configure and require a 0.10 m square fixture edge
 * (the OpenXR QR trackable runtime filters out targets of mismatched size). When estimation is
 * supported, tests configure `qrCodeSizeMeters = 0f` to estimate any positive square edge size.
 */
// TODO(b/573219883): Once a mechanism for injecting synthetic QR code data is available, migrate
// target-detection assertions to use a deterministic fixture instead of timing out and skipping
// when no QR code is in the camera view.
@RunWith(AndroidJUnit4::class)
@LargeTest
class QrCodeTrackingTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *filterSupportedPermissions("android.permission.SCENE_UNDERSTANDING_COARSE")
        )

    @get:Rule(order = 1) val sessionRule = createXrSessionRule()

    private fun configureOrSkipUnsupported(config: Config): SessionConfigureResult {
        return try {
            sessionRule.session.configure(config)
        } catch (e: UnsupportedOperationException) {
            throw AssumptionViolatedException(
                "Skipping test: QrCodeTrackingMode.DYNAMIC is not supported: ${e.message}",
                e,
            )
        }
    }

    private fun configureDynamicQrCodeSession(qrCodeSizeMeters: Float = QR_CODE_SIZE_METERS) {
        val config =
            Config.Builder()
                .setQrCodeTracking(QrCodeTrackingMode.DYNAMIC)
                .setQrCodeSizeMeters(qrCodeSizeMeters)
                .build()
        val configResult = configureOrSkipUnsupported(config)
        check(configResult is SessionConfigureSuccess) {
            "Failed to configure session: $configResult"
        }
        waitForPerceptionStateUpdate(sessionRule.session)
    }

    private fun awaitDecodedQrCodeStateOrSkip(): QrCode.State {
        val qrCodesFlow = QrCode.subscribe(sessionRule.session)
        val trackedQrCode =
            waitUntilFoundOrNull(timeout = PERCEPTION_DISCOVERY_TIMEOUT) {
                qrCodesFlow.value.firstOrNull {
                    it.state.value.trackingState == TrackingState.TRACKING
                }
            }
        assumeTrue(
            "Skipping QR target assertions: No tracked QR code in camera view",
            trackedQrCode != null,
        )
        var decodedState: QrCode.State? = null
        waitUntil("decoded QR code payload", timeout = QR_DECODE_TIMEOUT) {
            decodedState =
                qrCodesFlow.value.firstNotNullOfOrNull { qrCode ->
                    qrCode.state.value.takeIf {
                        it.trackingState == TrackingState.TRACKING && it.data.isNotEmpty()
                    }
                }
            decodedState != null
        }
        return checkNotNull(decodedState)
    }

    @Test
    @XrDeviceTest
    fun configureAndSubscribe_withDynamicMode_succeedsAndSubscribes() {
        val config =
            Config.Builder()
                .setQrCodeTracking(QrCodeTrackingMode.DYNAMIC)
                .setQrCodeSizeMeters(QR_CODE_SIZE_METERS)
                .build()

        val configResult = configureOrSkipUnsupported(config)
        waitForPerceptionStateUpdate(sessionRule.session)
        val subscribedQrCodes = QrCode.subscribe(sessionRule.session).value

        assertThat(configResult).isInstanceOf(SessionConfigureSuccess::class.java)
        assertThat(sessionRule.session.config.qrCodeTracking).isEqualTo(QrCodeTrackingMode.DYNAMIC)
        assertThat(sessionRule.session.config.qrCodeSizeMeters)
            .isWithin(FLOAT_TOLERANCE)
            .of(QR_CODE_SIZE_METERS)
        assertThat(subscribedQrCodes).containsNoDuplicates()
        for (qrCode in subscribedQrCodes) {
            assertThat(qrCode.state.value.owner).isEqualTo(qrCode)
        }
    }

    @Test
    @XrDeviceTest
    fun subscribe_whenTargetDetectedWithoutSizeEstimation_emitsDecodedQrCodeAndConfiguredExtents() {
        assumeFalse(
            "Skipping test: Device supports QR code size estimation",
            sessionRule.session.requirePerceptionManager().isQrCodeSizeEstimationSupported,
        )
        configureDynamicQrCodeSession()

        val state = awaitDecodedQrCodeStateOrSkip()

        assertThat(state.data).isEqualTo(EXPECTED_QR_PAYLOAD)
        assertFinitePose(state.centerPose)
        assertThat(state.extents.width).isWithin(EXTENTS_TOLERANCE_METERS).of(QR_CODE_SIZE_METERS)
        assertThat(state.extents.height).isWithin(EXTENTS_TOLERANCE_METERS).of(QR_CODE_SIZE_METERS)
    }

    @Test
    @XrDeviceTest
    fun subscribe_whenTargetDetectedWithSizeEstimation_emitsDecodedQrCodeAndPositiveExtents() {
        assumeTrue(
            "Skipping test: Device does not support QR code size estimation",
            sessionRule.session.requirePerceptionManager().isQrCodeSizeEstimationSupported,
        )
        configureDynamicQrCodeSession(qrCodeSizeMeters = 0f)

        val state = awaitDecodedQrCodeStateOrSkip()

        assertThat(state.data).isEqualTo(EXPECTED_QR_PAYLOAD)
        assertFinitePose(state.centerPose)
        assertThat(state.extents.width).isGreaterThan(0f)
        assertThat(state.extents.height).isGreaterThan(0f)
    }

    @Test
    @XrDeviceTest
    fun subscribe_whenDisabled_throwsIllegalStateException() {
        configureSessionAndWaitForPerception(
            sessionRule.session,
            Config.Builder().setQrCodeTracking(QrCodeTrackingMode.DISABLED).build(),
        )

        val exception =
            assertFailsWith<IllegalStateException> { QrCode.subscribe(sessionRule.session) }

        assertThat(exception).hasMessageThat().contains("DISABLED")
    }

    // TODO(b/584834729): Fix Config.qrCodeSizeMeters KDoc: OpenXrRuntime throws
    // IllegalArgumentException for negative sizes (and for 0f without size estimation).
    @Test
    @XrDeviceTest
    fun configure_withNegativeQrCodeSizeMeters_throwsIllegalArgumentException() {
        val config =
            Config.Builder()
                .setQrCodeTracking(QrCodeTrackingMode.DYNAMIC)
                .setQrCodeSizeMeters(-0.1f)
                .build()

        val exception =
            assertFailsWith<IllegalArgumentException> { sessionRule.session.configure(config) }

        assertThat(exception).hasMessageThat().contains("qrCodeSizeMeters")
    }

    @Test
    @XrDeviceTest
    fun subscribe_afterReconfiguringDisabled_throwsIllegalStateException() {
        configureDynamicQrCodeSession()
        // Subscribe while enabled so reconfiguring DISABLED runs with an active subscription.
        QrCode.subscribe(sessionRule.session)
        configureSessionAndWaitForPerception(
            sessionRule.session,
            Config.Builder().setQrCodeTracking(QrCodeTrackingMode.DISABLED).build(),
        )

        val exception =
            assertFailsWith<IllegalStateException> { QrCode.subscribe(sessionRule.session) }

        assertThat(exception).hasMessageThat().contains("DISABLED")
    }

    @Test
    @XrDeviceTest
    fun configureAndSubscribe_afterDisablingDynamicMode_reEnablesQrCodeTracking() {
        configureDynamicQrCodeSession()
        // Subscribe while enabled so toggling to DISABLED runs with an active subscription.
        QrCode.subscribe(sessionRule.session)
        configureSessionAndWaitForPerception(
            sessionRule.session,
            Config.Builder().setQrCodeTracking(QrCodeTrackingMode.DISABLED).build(),
        )
        val reEnableConfig =
            Config.Builder()
                .setQrCodeTracking(QrCodeTrackingMode.DYNAMIC)
                .setQrCodeSizeMeters(QR_CODE_SIZE_METERS)
                .build()

        val reEnableResult = sessionRule.session.configure(reEnableConfig)
        waitForPerceptionStateUpdate(
            sessionRule.session,
            description = "perception state update after re-enabling QR tracking",
        )
        val subscribedQrCodes = QrCode.subscribe(sessionRule.session).value

        assertThat(reEnableResult).isInstanceOf(SessionConfigureSuccess::class.java)
        assertThat(sessionRule.session.config.qrCodeTracking).isEqualTo(QrCodeTrackingMode.DYNAMIC)
        assertThat(sessionRule.session.config.qrCodeSizeMeters)
            .isWithin(FLOAT_TOLERANCE)
            .of(QR_CODE_SIZE_METERS)
        assertThat(subscribedQrCodes).containsNoDuplicates()
        for (qrCode in subscribedQrCodes) {
            assertThat(qrCode.state.value.owner).isEqualTo(qrCode)
        }
    }

    private companion object {
        private val QR_DECODE_TIMEOUT = 5.seconds
        private const val QR_CODE_SIZE_METERS = 0.1f
        private const val EXPECTED_QR_PAYLOAD = "https://www.google.com"
    }
}
