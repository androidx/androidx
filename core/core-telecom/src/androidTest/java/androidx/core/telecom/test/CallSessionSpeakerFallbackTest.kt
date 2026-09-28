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

package androidx.core.telecom.test

import android.os.Build.VERSION_CODES
import android.os.ParcelUuid
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.internal.BluetoothDeviceChecker
import androidx.core.telecom.internal.CallChannels
import androidx.core.telecom.internal.CallSession
import androidx.core.telecom.internal.CallSessionLegacy
import androidx.core.telecom.internal.utils.EndpointUtils
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.TestUtils
import androidx.core.telecom.test.utils.TestUtils.OUTGOING_NAME
import androidx.core.telecom.test.utils.TestUtils.TEST_ADDRESS
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Parameterized test suite that verifies video/audio speaker fallback and headset disconnect
 * behavior across both [CallSession] (Transactional V2 API) and [CallSessionLegacy]
 * (ConnectionService Legacy API) without duplicating test logic.
 */
@SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE /* api=34 */)
@SmallTest
@RunWith(Parameterized::class)
class CallSessionSpeakerFallbackTest(private val sessionType: SessionType) : BaseTelecomTest() {

    enum class SessionType {
        CALL_SESSION_V2,
        CALL_SESSION_LEGACY,
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(SessionType.CALL_SESSION_V2),
                arrayOf(SessionType.CALL_SESSION_LEGACY),
            )
        }
    }

    private class FakeBluetoothDeviceChecker(var hasNonWatchDevice: Boolean = false) :
        BluetoothDeviceChecker {
        override fun hasAvailableNonWatchDevice(
            availableEndpoints: List<CallEndpointCompat>
        ): Boolean = hasNonWatchDevice
    }

    /**
     * Unified test harness wrapping either a [CallSession] or [CallSessionLegacy] instance so the
     * same test logic executes against both implementations.
     */
    private interface SessionHarness : AutoCloseable {
        var lastClientRequestedEndpoint: CallEndpointCompat?

        fun onAvailableEndpointsChanged(endpoints: List<CallEndpointCompat>)

        fun onCurrentEndpointChanged(endpoint: CallEndpointCompat)
    }

    private inner class V2SessionHarness(private val session: CallSession) : SessionHarness {
        override var lastClientRequestedEndpoint: CallEndpointCompat?
            get() = session.mLastClientRequestedEndpoint
            set(value) {
                session.mLastClientRequestedEndpoint = value
            }

        override fun onAvailableEndpointsChanged(endpoints: List<CallEndpointCompat>) {
            session.onAvailableCallEndpointsChanged(
                endpoints.map { EndpointUtils.Api34PlusImpl.toCallEndpoint(it) }
            )
        }

        override fun onCurrentEndpointChanged(endpoint: CallEndpointCompat) {
            session.onCallEndpointChanged(EndpointUtils.Api34PlusImpl.toCallEndpoint(endpoint))
        }

        override fun close() {
            session.close()
        }
    }

    private inner class LegacySessionHarness(private val session: CallSessionLegacy) :
        SessionHarness {
        override var lastClientRequestedEndpoint: CallEndpointCompat?
            get() = session.mLastClientRequestedEndpoint
            set(value) {
                session.mLastClientRequestedEndpoint = value
            }

        override fun onAvailableEndpointsChanged(endpoints: List<CallEndpointCompat>) {
            session.setAvailableCallEndpoints(endpoints)
            session.getCurrentCallEndpointForSession()?.let {
                session.enforceVideoCallSpeakerFallback(it)
            }
        }

        override fun onCurrentEndpointChanged(endpoint: CallEndpointCompat) {
            val prevEndpoint = session.getCurrentCallEndpointForSession()
            session.setCurrentCallEndpoint(endpoint)
            session.avoidSpeakerOverrideOnCallStart(prevEndpoint, endpoint)
            session.enforceVideoCallSpeakerFallback(endpoint)
        }

        override fun close() {
            session.close()
        }
    }

    private fun createSessionHarness(
        isVideoCall: Boolean,
        bluetoothDeviceChecker: BluetoothDeviceChecker,
        coroutineContext: CoroutineContext,
        callChannels: CallChannels,
    ): SessionHarness {
        val attributes =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                if (isVideoCall) CallAttributesCompat.CALL_TYPE_VIDEO_CALL
                else CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
                CallAttributesCompat.SUPPORTS_STREAM,
            )
        return when (sessionType) {
            SessionType.CALL_SESSION_V2 ->
                V2SessionHarness(
                    CallSession(
                        bluetoothDeviceChecker,
                        coroutineContext,
                        attributes,
                        TestUtils.mOnAnswerLambda,
                        TestUtils.mOnDisconnectLambda,
                        TestUtils.mOnSetActiveLambda,
                        TestUtils.mOnSetInActiveLambda,
                        callChannels,
                        MutableSharedFlow(),
                        { _, _ -> },
                        CompletableDeferred(Unit),
                    )
                )
            SessionType.CALL_SESSION_LEGACY ->
                LegacySessionHarness(
                    CallSessionLegacy(
                        ParcelUuid.fromString(UUID.randomUUID().toString()),
                        mContext,
                        attributes,
                        callChannels,
                        coroutineContext,
                        TestUtils.mOnAnswerLambda,
                        TestUtils.mOnDisconnectLambda,
                        TestUtils.mOnSetActiveLambda,
                        TestUtils.mOnSetInActiveLambda,
                        { _, _ -> },
                        MutableSharedFlow(),
                        null,
                        CompletableDeferred(Unit),
                        bluetoothDeviceChecker,
                    )
                )
        }
    }

    private fun randomUuid(): ParcelUuid = ParcelUuid.fromString(UUID.randomUUID().toString())

    // =========================================================================================
    // Headset Disconnect & Transient Route Regression Tests
    // =========================================================================================

    /**
     * Regression test for b/559066252: Verify that a transient `BLUETOOTH` -> `EARPIECE` transition
     * while the Bluetooth headset is still present in `mAvailableEndpoints` does NOT trigger an
     * unwanted switch to `SPEAKER`.
     */
    @Test
    fun testTransientBluetoothToEarpiece_doesNotSwitchToSpeakerWhenBtStillAvailable() {
        runBlocking {
            val callChannels = CallChannels()
            val fakeBtChecker = FakeBluetoothDeviceChecker(hasNonWatchDevice = true)
            val harness = createSessionHarness(true, fakeBtChecker, coroutineContext, callChannels)
            yield()

            harness.onAvailableEndpointsChanged(
                listOf(mEarpieceEndpoint, mSpeakerEndpoint, mBluetoothEndpoint)
            )
            harness.onCurrentEndpointChanged(mBluetoothEndpoint)
            harness.lastClientRequestedEndpoint = null

            // Platform emits a transient BLUETOOTH -> EARPIECE change while BT is still available
            harness.onCurrentEndpointChanged(mEarpieceEndpoint)
            yield()

            assertNull(harness.lastClientRequestedEndpoint)
            harness.close()
            callChannels.closeAllChannels()
        }
    }

    /**
     * Verify that when a Bluetooth headset genuinely disconnects and `onCurrentEndpointChanged`
     * (`EARPIECE`) arrives BEFORE `onAvailableEndpointsChanged` removes the Bluetooth endpoint, the
     * session switches to `SPEAKER` as soon as `onAvailableEndpointsChanged` updates the available
     * list.
     */
    @Test
    fun testBluetoothDisconnect_endpointChangedBeforeAvailableChanged_switchesToSpeaker() {
        runBlocking {
            val callChannels = CallChannels()
            val fakeBtChecker =
                object : BluetoothDeviceChecker {
                    override fun hasAvailableNonWatchDevice(
                        availableEndpoints: List<CallEndpointCompat>
                    ): Boolean = availableEndpoints.any { it.isBluetoothType() }
                }
            val harness = createSessionHarness(true, fakeBtChecker, coroutineContext, callChannels)
            yield()

            harness.onAvailableEndpointsChanged(
                listOf(mEarpieceEndpoint, mSpeakerEndpoint, mBluetoothEndpoint)
            )
            harness.onCurrentEndpointChanged(mBluetoothEndpoint)
            harness.lastClientRequestedEndpoint = null

            // 1. Current endpoint drops to EARPIECE while BT is still in availableEndpoints
            harness.onCurrentEndpointChanged(mEarpieceEndpoint)
            yield()
            assertNull(harness.lastClientRequestedEndpoint)

            // 2. Available endpoints list updates with BT removed
            harness.onAvailableEndpointsChanged(listOf(mEarpieceEndpoint, mSpeakerEndpoint))
            yield()

            assertEquals(
                CallEndpointCompat.TYPE_SPEAKER,
                harness.lastClientRequestedEndpoint?.type,
            )
            harness.close()
            callChannels.closeAllChannels()
        }
    }

    /**
     * Verify that when a Bluetooth headset genuinely disconnects and `onAvailableEndpointsChanged`
     * removes the Bluetooth endpoint BEFORE `onCurrentEndpointChanged(EARPIECE)` arrives, the
     * session switches to `SPEAKER` when `onCurrentEndpointChanged(EARPIECE)` is received.
     */
    @Test
    fun testBluetoothDisconnect_availableChangedBeforeEndpointChanged_switchesToSpeaker() {
        runBlocking {
            val callChannels = CallChannels()
            val fakeBtChecker =
                object : BluetoothDeviceChecker {
                    override fun hasAvailableNonWatchDevice(
                        availableEndpoints: List<CallEndpointCompat>
                    ): Boolean = availableEndpoints.any { it.isBluetoothType() }
                }
            val harness = createSessionHarness(true, fakeBtChecker, coroutineContext, callChannels)
            yield()

            harness.onAvailableEndpointsChanged(
                listOf(mEarpieceEndpoint, mSpeakerEndpoint, mBluetoothEndpoint)
            )
            harness.onCurrentEndpointChanged(mBluetoothEndpoint)
            harness.lastClientRequestedEndpoint = null

            // 1. Available endpoints list updates with BT removed while current endpoint is BT
            harness.onAvailableEndpointsChanged(listOf(mEarpieceEndpoint, mSpeakerEndpoint))
            yield()
            assertNull(harness.lastClientRequestedEndpoint)

            // 2. Current endpoint changes to EARPIECE
            harness.onCurrentEndpointChanged(mEarpieceEndpoint)
            yield()

            assertEquals(
                CallEndpointCompat.TYPE_SPEAKER,
                harness.lastClientRequestedEndpoint?.type,
            )
            harness.close()
            callChannels.closeAllChannels()
        }
    }

    // =========================================================================================
    // 10-Case Endpoint Matrix Regression Suite (5 Endpoint Sets x Video / Audio Call)
    // =========================================================================================

    /**
     * Case 1: `[EARPIECE, SPEAKER]` + Video Call -> Must switch to `SPEAKER` when landing on
     * `EARPIECE`.
     */
    @Test
    fun testEndpointMatrix_case1_earpieceAndSpeaker_videoCall_switchesToSpeaker() {
        runBlocking {
            val callChannels = CallChannels()
            val fakeBtChecker = FakeBluetoothDeviceChecker(hasNonWatchDevice = false)
            val harness = createSessionHarness(true, fakeBtChecker, coroutineContext, callChannels)
            yield()

            harness.onAvailableEndpointsChanged(listOf(mEarpieceEndpoint, mSpeakerEndpoint))
            harness.onCurrentEndpointChanged(mEarpieceEndpoint)
            yield()

            assertEquals(
                CallEndpointCompat.TYPE_SPEAKER,
                harness.lastClientRequestedEndpoint?.type,
            )
            harness.close()
            callChannels.closeAllChannels()
        }
    }

    /**
     * Case 2: `[EARPIECE, SPEAKER]` + Audio Call -> Must stay on `EARPIECE` (no switch to
     * `SPEAKER`).
     */
    @Test
    fun testEndpointMatrix_case2_earpieceAndSpeaker_audioCall_staysOnEarpiece() {
        runBlocking {
            val callChannels = CallChannels()
            val fakeBtChecker = FakeBluetoothDeviceChecker(hasNonWatchDevice = false)
            val harness = createSessionHarness(false, fakeBtChecker, coroutineContext, callChannels)
            yield()

            harness.onAvailableEndpointsChanged(listOf(mEarpieceEndpoint, mSpeakerEndpoint))
            harness.onCurrentEndpointChanged(mEarpieceEndpoint)
            yield()

            assertNull(harness.lastClientRequestedEndpoint)
            harness.close()
            callChannels.closeAllChannels()
        }
    }

    /**
     * Case 3: `[EARPIECE, SPEAKER, BLUETOOTH_HEADSET]` + Video Call -> Must NOT switch to `SPEAKER`
     * when `BLUETOOTH_HEADSET` -> `EARPIECE` occurs while `BLUETOOTH_HEADSET` is still available.
     */
    @Test
    fun testEndpointMatrix_case3_earpieceSpeakerBtHeadset_videoCall_doesNotSwitchToSpeaker() {
        runBlocking {
            val callChannels = CallChannels()
            val fakeBtChecker = FakeBluetoothDeviceChecker(hasNonWatchDevice = true)
            val harness = createSessionHarness(true, fakeBtChecker, coroutineContext, callChannels)
            yield()

            harness.onAvailableEndpointsChanged(
                listOf(mEarpieceEndpoint, mSpeakerEndpoint, mBluetoothEndpoint)
            )
            harness.onCurrentEndpointChanged(mBluetoothEndpoint)
            harness.lastClientRequestedEndpoint = null

            harness.onCurrentEndpointChanged(mEarpieceEndpoint)
            yield()

            assertNull(harness.lastClientRequestedEndpoint)
            harness.close()
            callChannels.closeAllChannels()
        }
    }

    /**
     * Case 4: `[EARPIECE, SPEAKER, BLUETOOTH_HEADSET]` + Audio Call -> Must NOT switch to
     * `SPEAKER`.
     */
    @Test
    fun testEndpointMatrix_case4_earpieceSpeakerBtHeadset_audioCall_doesNotSwitchToSpeaker() {
        runBlocking {
            val callChannels = CallChannels()
            val fakeBtChecker = FakeBluetoothDeviceChecker(hasNonWatchDevice = true)
            val harness = createSessionHarness(false, fakeBtChecker, coroutineContext, callChannels)
            yield()

            harness.onAvailableEndpointsChanged(
                listOf(mEarpieceEndpoint, mSpeakerEndpoint, mBluetoothEndpoint)
            )
            harness.onCurrentEndpointChanged(mBluetoothEndpoint)
            harness.lastClientRequestedEndpoint = null

            harness.onCurrentEndpointChanged(mEarpieceEndpoint)
            yield()

            assertNull(harness.lastClientRequestedEndpoint)
            harness.close()
            callChannels.closeAllChannels()
        }
    }

    /**
     * Case 5: `[EARPIECE, SPEAKER, BLUETOOTH_WATCH]` + Video Call -> Must switch to `SPEAKER` when
     * on `EARPIECE`, because only a watch Bluetooth device (`hasNonWatchDevice = false`) is
     * available.
     */
    @Test
    fun testEndpointMatrix_case5_earpieceSpeakerBtWatch_videoCall_switchesToSpeaker() {
        runBlocking {
            val callChannels = CallChannels()
            val btWatch =
                CallEndpointCompat("Pixel Watch", CallEndpointCompat.TYPE_BLUETOOTH, randomUuid())
            val fakeBtChecker = FakeBluetoothDeviceChecker(hasNonWatchDevice = false)
            val harness = createSessionHarness(true, fakeBtChecker, coroutineContext, callChannels)
            yield()

            harness.onAvailableEndpointsChanged(
                listOf(mEarpieceEndpoint, mSpeakerEndpoint, btWatch)
            )
            harness.onCurrentEndpointChanged(mEarpieceEndpoint)
            yield()

            assertEquals(
                CallEndpointCompat.TYPE_SPEAKER,
                harness.lastClientRequestedEndpoint?.type,
            )
            harness.close()
            callChannels.closeAllChannels()
        }
    }

    /**
     * Case 6: `[EARPIECE, SPEAKER, BLUETOOTH_WATCH]` + Audio Call -> Must stay on `EARPIECE` (no
     * switch to `SPEAKER`).
     */
    @Test
    fun testEndpointMatrix_case6_earpieceSpeakerBtWatch_audioCall_staysOnEarpiece() {
        runBlocking {
            val callChannels = CallChannels()
            val btWatch =
                CallEndpointCompat("Pixel Watch", CallEndpointCompat.TYPE_BLUETOOTH, randomUuid())
            val fakeBtChecker = FakeBluetoothDeviceChecker(hasNonWatchDevice = false)
            val harness = createSessionHarness(false, fakeBtChecker, coroutineContext, callChannels)
            yield()

            harness.onAvailableEndpointsChanged(
                listOf(mEarpieceEndpoint, mSpeakerEndpoint, btWatch)
            )
            harness.onCurrentEndpointChanged(mEarpieceEndpoint)
            yield()

            assertNull(harness.lastClientRequestedEndpoint)
            harness.close()
            callChannels.closeAllChannels()
        }
    }

    /**
     * Case 7: `[EARPIECE, SPEAKER, BLUETOOTH_HEADSET, BLUETOOTH_WATCH]` + Video Call -> Must NOT
     * switch to `SPEAKER` because a non-watch Bluetooth headset (`hasNonWatchDevice = true`) is
     * available alongside the watch.
     */
    @Test
    fun testEndpointMatrix_case7_earpieceSpeakerBtHeadsetAndBtWatch_videoCall_doesNotSwitchToSpeaker() {
        runBlocking {
            val callChannels = CallChannels()
            val btWatch =
                CallEndpointCompat("Pixel Watch", CallEndpointCompat.TYPE_BLUETOOTH, randomUuid())
            val fakeBtChecker = FakeBluetoothDeviceChecker(hasNonWatchDevice = true)
            val harness = createSessionHarness(true, fakeBtChecker, coroutineContext, callChannels)
            yield()

            harness.onAvailableEndpointsChanged(
                listOf(mEarpieceEndpoint, mSpeakerEndpoint, mBluetoothEndpoint, btWatch)
            )
            harness.onCurrentEndpointChanged(mBluetoothEndpoint)
            harness.lastClientRequestedEndpoint = null

            harness.onCurrentEndpointChanged(mEarpieceEndpoint)
            yield()

            assertNull(harness.lastClientRequestedEndpoint)
            harness.close()
            callChannels.closeAllChannels()
        }
    }

    /**
     * Case 8: `[EARPIECE, SPEAKER, BLUETOOTH_HEADSET, BLUETOOTH_WATCH]` + Audio Call -> Must NOT
     * switch to `SPEAKER`.
     */
    @Test
    fun testEndpointMatrix_case8_earpieceSpeakerBtHeadsetAndBtWatch_audioCall_doesNotSwitchToSpeaker() {
        runBlocking {
            val callChannels = CallChannels()
            val btWatch =
                CallEndpointCompat("Pixel Watch", CallEndpointCompat.TYPE_BLUETOOTH, randomUuid())
            val fakeBtChecker = FakeBluetoothDeviceChecker(hasNonWatchDevice = true)
            val harness = createSessionHarness(false, fakeBtChecker, coroutineContext, callChannels)
            yield()

            harness.onAvailableEndpointsChanged(
                listOf(mEarpieceEndpoint, mSpeakerEndpoint, mBluetoothEndpoint, btWatch)
            )
            harness.onCurrentEndpointChanged(mBluetoothEndpoint)
            harness.lastClientRequestedEndpoint = null

            harness.onCurrentEndpointChanged(mEarpieceEndpoint)
            yield()

            assertNull(harness.lastClientRequestedEndpoint)
            harness.close()
            callChannels.closeAllChannels()
        }
    }

    /**
     * Case 9: `[EARPIECE, SPEAKER, BLUETOOTH_HEADSET_1, BLUETOOTH_HEADSET_2]` + Video Call -> Must
     * NOT switch to `SPEAKER` when `BLUETOOTH_HEADSET_1` transitions to `EARPIECE` while
     * `BLUETOOTH_HEADSET_2` remains available.
     */
    @Test
    fun testEndpointMatrix_case9_earpieceSpeakerTwoBtHeadsets_videoCall_doesNotSwitchToSpeaker() {
        runBlocking {
            val callChannels = CallChannels()
            val btHeadset2 =
                CallEndpointCompat(
                    "Bluetooth Headset 2",
                    CallEndpointCompat.TYPE_BLUETOOTH,
                    randomUuid(),
                )
            val fakeBtChecker = FakeBluetoothDeviceChecker(hasNonWatchDevice = true)
            val harness = createSessionHarness(true, fakeBtChecker, coroutineContext, callChannels)
            yield()

            harness.onAvailableEndpointsChanged(
                listOf(mEarpieceEndpoint, mSpeakerEndpoint, mBluetoothEndpoint, btHeadset2)
            )
            harness.onCurrentEndpointChanged(mBluetoothEndpoint)
            harness.lastClientRequestedEndpoint = null

            harness.onCurrentEndpointChanged(mEarpieceEndpoint)
            yield()

            assertNull(harness.lastClientRequestedEndpoint)
            harness.close()
            callChannels.closeAllChannels()
        }
    }

    /**
     * Case 10: `[EARPIECE, SPEAKER, BLUETOOTH_HEADSET_1, BLUETOOTH_HEADSET_2]` + Audio Call -> Must
     * NOT switch to `SPEAKER`.
     */
    @Test
    fun testEndpointMatrix_case10_earpieceSpeakerTwoBtHeadsets_audioCall_doesNotSwitchToSpeaker() {
        runBlocking {
            val callChannels = CallChannels()
            val btHeadset2 =
                CallEndpointCompat(
                    "Bluetooth Headset 2",
                    CallEndpointCompat.TYPE_BLUETOOTH,
                    randomUuid(),
                )
            val fakeBtChecker = FakeBluetoothDeviceChecker(hasNonWatchDevice = true)
            val harness = createSessionHarness(false, fakeBtChecker, coroutineContext, callChannels)
            yield()

            harness.onAvailableEndpointsChanged(
                listOf(mEarpieceEndpoint, mSpeakerEndpoint, mBluetoothEndpoint, btHeadset2)
            )
            harness.onCurrentEndpointChanged(mBluetoothEndpoint)
            harness.lastClientRequestedEndpoint = null

            harness.onCurrentEndpointChanged(mEarpieceEndpoint)
            yield()

            assertNull(harness.lastClientRequestedEndpoint)
            harness.close()
            callChannels.closeAllChannels()
        }
    }
}
