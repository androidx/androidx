/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.telecom.test.utils

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.ParcelUuid
import android.os.UserHandle
import android.os.UserManager
import android.telecom.Call
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.extensions.Participant
import androidx.core.telecom.extensions.ParticipantParcelable
import androidx.core.telecom.extensions.toParticipant
import androidx.core.telecom.internal.utils.BuildVersionAdapter
import androidx.core.telecom.test.ITestAppControlCallback
import androidx.core.telecom.util.ExperimentalAppActions
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileInputStream
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals

/** Singleton class. */
@RequiresApi(VERSION_CODES.O)
object TestUtils {
    const val LOG_TAG = "TelecomTestUtils"
    const val TEST_PACKAGE = "androidx.core.telecom.test"
    private const val COMMAND_SET_DEFAULT_DIALER =
        "telecom set-default-dialer " // DO NOT REMOVE SPACE
    private const val COMMAND_GET_DEFAULT_DIALER = "telecom get-default-dialer"
    private const val COMMAND_ENABLE_PHONE_ACCOUNT = "telecom set-phone-account-enabled "
    const val COMMAND_CLEANUP_STUCK_CALLS = "telecom cleanup-stuck-calls"
    const val COMMAND_DUMP_TELECOM = "dumpsys telecom"
    const val TEST_CALL_ATTRIB_NAME = "Elon Musk"
    const val OUTGOING_NAME = "Larry Page"
    private const val INCOMING_NAME = "Sundar Pichai"
    const val WAIT_ON_ASSERTS_TO_FINISH_TIMEOUT = 10000L
    const val WAIT_ON_CALL_STATE_TIMEOUT = 8000L
    private const val WAIT_ON_IN_CALL_SERVICE_CALL_COUNT_TIMEOUT = 5000L
    private const val ALL_CALL_CAPABILITIES =
        (CallAttributesCompat.SUPPORTS_SET_INACTIVE or
            CallAttributesCompat.SUPPORTS_STREAM or
            CallAttributesCompat.SUPPORTS_TRANSFER)
    const val VERIFICATION_TIMEOUT_MSG =
        "Timed out before asserting all values. This most likely means the platform failed to" +
            " add the call or hung on a CallControl operation."
    private const val CALLBACK_FAILED_EXCEPTION_MSG =
        "callback failed to be completed in the lambda function"
    // non-primitive constants
    val TEST_PHONE_NUMBER_9001: Uri = Uri.parse("tel:6506959001")
    val TEST_PHONE_NUMBER_8985: Uri = Uri.parse("tel:6506958985")

    // Define the minimal set of properties to start an outgoing call
    val OUTGOING_CALL_ATTRIBUTES =
        CallAttributesCompat(
            OUTGOING_NAME,
            TEST_PHONE_NUMBER_8985,
            CallAttributesCompat.DIRECTION_OUTGOING,
            CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
            ALL_CALL_CAPABILITIES
        )

    val OUTGOING_NO_HOLD_CAP_CALL_ATTRIBUTES =
        CallAttributesCompat(
            OUTGOING_NAME,
            TEST_PHONE_NUMBER_8985,
            CallAttributesCompat.DIRECTION_OUTGOING,
            CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
            CallAttributesCompat.SUPPORTS_STREAM
        )

    // Define all possible properties for CallAttributes
    val INCOMING_CALL_ATTRIBUTES =
        CallAttributesCompat(
            INCOMING_NAME,
            TEST_PHONE_NUMBER_8985,
            CallAttributesCompat.DIRECTION_INCOMING,
            ALL_CALL_CAPABILITIES
        )

    /**
     * This build version should be set when the **V2 transactional APIs** are desired as the
     * underlying call management.
     */
    internal val mV2Build =
        object : BuildVersionAdapter {
            override fun hasPlatformV2Apis(): Boolean {
                return true
            }

            override fun hasInvalidBuildVersion(): Boolean {
                return false
            }
        }

    /**
     * This build version should be set when the **ConnectionService and Connection APIs** are
     * desired as the underlying call management.
     */
    internal val mBackwardsCompatBuild =
        object : BuildVersionAdapter {
            override fun hasPlatformV2Apis(): Boolean {
                return false
            }

            override fun hasInvalidBuildVersion(): Boolean {
                return false
            }
        }

    /** This build version should be set when edge case testing on invalid builds */
    internal val mInvalidBuild =
        object : BuildVersionAdapter {
            override fun hasPlatformV2Apis(): Boolean {
                return false
            }

            override fun hasInvalidBuildVersion(): Boolean {
                return true
            }
        }

    val mOnSetActiveLambda: suspend () -> Unit = {
        Log.i(LOG_TAG, "onSetActive: completing")
        mOnSetActiveCallbackCalled = true
        if (!mCompleteOnSetActive) {
            throw Exception(CALLBACK_FAILED_EXCEPTION_MSG)
        }
    }

    val mOnSetInActiveLambda: suspend () -> Unit = {
        Log.i(LOG_TAG, "onSetInactive: completing")
        mOnSetInactiveCallbackCalled = true
        if (!mCompleteOnSetInactive) {
            throw Exception(CALLBACK_FAILED_EXCEPTION_MSG)
        }
    }

    internal val mOnEventLambda: suspend (event: String, extras: Bundle) -> Unit = { event, _ ->
        Log.i(LOG_TAG, "onEvent: $event")
        // No users of this yet
    }

    val mOnAnswerLambda: suspend (type: Int) -> Unit = {
        Log.i(LOG_TAG, "onAnswer: callType=[$it]")
        mOnAnswerCallbackCalled = true
        if (!mCompleteOnAnswer) {
            throw Exception(CALLBACK_FAILED_EXCEPTION_MSG)
        }
    }

    val mOnDisconnectLambda: suspend (cause: DisconnectCause) -> Unit = {
        Log.i(LOG_TAG, "onDisconnect: disconnectCause=[$it]")
        mOnDisconnectCallbackCalled = true
        mCompleteOnDisconnect
        if (!mCompleteOnDisconnect) {
            throw Exception(CALLBACK_FAILED_EXCEPTION_MSG)
        }
    }

    // Flags for determining whether the given callback was invoked or not
    var mOnSetActiveCallbackCalled = false
    var mOnSetInactiveCallbackCalled = false
    var mOnAnswerCallbackCalled = false
    var mOnDisconnectCallbackCalled = false
    // Flags for determining whether to complete/reject the transaction
    var mCompleteOnSetActive = true
    var mCompleteOnSetInactive = true
    var mCompleteOnAnswer = true
    var mCompleteOnDisconnect = true

    fun resetCallbackConfigs() {
        mOnSetActiveCallbackCalled = false
        mOnSetInactiveCallbackCalled = false
        mOnAnswerCallbackCalled = false
        mOnDisconnectCallbackCalled = false
        mCompleteOnSetActive = true
        mCompleteOnSetInactive = true
        mCompleteOnAnswer = true
        mCompleteOnDisconnect = true
    }

    fun createCallAttributes(
        callDirection: Int,
        phoneAccountHandle: PhoneAccountHandle,
        callType: Int? = CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
    ): CallAttributesCompat {

        val attributes: CallAttributesCompat =
            if (callType != null) {
                CallAttributesCompat(
                    TEST_CALL_ATTRIB_NAME,
                    TEST_PHONE_NUMBER_9001,
                    callDirection,
                    callType
                )
            } else {
                CallAttributesCompat(TEST_CALL_ATTRIB_NAME, TEST_PHONE_NUMBER_9001, callDirection)
            }

        attributes.mHandle = phoneAccountHandle

        return attributes
    }

    /** Run a command and retrieve the output as a string. */
    fun runShellCommand(command: String): String {
        return InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
            .use { FileInputStream(it.fileDescriptor).reader().readText() }
    }

    fun setDefaultDialer(packageName: String) {
        Log.i(
            LOG_TAG,
            "setDefaultDialer=[${runShellCommand((COMMAND_SET_DEFAULT_DIALER + packageName))}]"
        )
    }

    fun getAudioModeName(mode: Int): String {
        return when (mode) {
            AudioManager.MODE_NORMAL -> "MODE_NORMAL"
            AudioManager.MODE_RINGTONE -> "MODE_RINGTONE"
            AudioManager.MODE_IN_CALL -> "MODE_IN_CALL"
            AudioManager.MODE_IN_COMMUNICATION -> "MODE_IN_COMMUNICATION"
            AudioManager.MODE_CALL_SCREENING -> "MODE_CALL_SCREENING"
            AudioManager.MODE_CALL_REDIRECT -> "MODE_CALL_REDIRECT"
            AudioManager.MODE_COMMUNICATION_REDIRECT -> "MODE_COMMUNICATION_REDIRECT"
            else -> "UNKNOWN mode = <$mode>"
        }
    }

    fun enablePhoneAccountHandle(context: Context, phoneAccountHandle: PhoneAccountHandle) {
        val pn = phoneAccountHandle.componentName.packageName
        val cn = phoneAccountHandle.componentName.className
        val userHandleId = getCurrentUserSerialNumber(context, phoneAccountHandle.userHandle)
        Log.i(
            LOG_TAG,
            "enable phoneAccountHandle=[$phoneAccountHandle], success=[${
                runShellCommand(
                    (COMMAND_ENABLE_PHONE_ACCOUNT +
                        pn + "/" + cn + " " + phoneAccountHandle.id + " " + userHandleId)
                )
            }]"
        )
    }

    private fun getCurrentUserSerialNumber(context: Context, userHandle: UserHandle): Long {
        val userManager = context.getSystemService(UserManager::class.java)
        return userManager.getSerialNumberForUser(userHandle)
    }

    fun getDefaultDialer(): String {
        val s = runShellCommand(COMMAND_GET_DEFAULT_DIALER)
        return s.replace("\n", "")
    }

    fun dumpTelecom() {
        Log.i(LOG_TAG, "telecom dumpsys=[${runShellCommand(COMMAND_DUMP_TELECOM)}]")
        Log.i(LOG_TAG, "defaultDialer=[${getDefaultDialer()}]")
    }

    fun generateRandomUuid(): ParcelUuid {
        return ParcelUuid.fromString(UUID.randomUUID().toString())
    }

    /**
     * Suspends until the [targetCallCount] is reached, or times out after
     * [WAIT_ON_IN_CALL_SERVICE_CALL_COUNT_TIMEOUT] milliseconds.
     */
    internal suspend fun waitOnInCallServiceToReachXCalls(
        service: TestInCallService,
        targetCallCount: Int
    ): Call? {
        var targetCall: Call? = null
        Log.i(
            LOG_TAG,
            "waitOnInCallServiceToReachXCalls: target count=$targetCallCount, " +
                "starting call check"
        )
        if (targetCallCount > 0) {
            waitForCondition(
                WAIT_ON_IN_CALL_SERVICE_CALL_COUNT_TIMEOUT,
                "Expected call count to be <$targetCallCount>" +
                    " but the Actual call count was <${service.getCallCount()}>"
            ) {
                service.getCallCount() >= targetCallCount
            }
            targetCall = service.getLastCall()
            Log.i(LOG_TAG, "waitOnInCallServiceToReachXCalls: found targetCall=[$targetCall]")
        } else {
            waitForCondition(
                WAIT_ON_IN_CALL_SERVICE_CALL_COUNT_TIMEOUT,
                "Expected call count to be <$targetCallCount>" +
                    " but the Actual call count was <${service.getCallCount()}>"
            ) {
                service.getCallCount() <= 0
            }
            Log.i(LOG_TAG, "waitOnInCallServiceToReachXCalls: reached 0 calls")
        }
        return targetCall
    }

    @Suppress("DEPRECATION")
    suspend fun waitOnCallState(call: Call, targetState: Int) {
        waitForCondition(
            WAIT_ON_CALL_STATE_TIMEOUT,
            "Expected call state to be <$targetState>" +
                " but the Actual call state was <${call.state}>"
        ) {
            call.state == targetState
        }
    }

    private suspend fun waitForCondition(
        timeout: Long,
        failureMessage: String,
        expectedCondition: () -> Boolean
    ) {
        try {
            withTimeout(timeout) {
                while (isActive /* aka  within timeout window */ && !expectedCondition()) {
                    yield() // another mechanism to stop the while loop if the coroutine is dead
                    delay(1) // sleep x millisecond(s) instead of spamming check
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.i(LOG_TAG, "waitOnCondition: timeout reached")
            dumpTelecom()
            throw AssertionError(failureMessage)
        }
    }

    /**
     * Used for testing in V. The build version is not available for referencing so this helper
     * performs a manual check instead.
     */
    fun buildIsAtLeastV(): Boolean {
        // V is not referencable as a valid build version yet. Enforce strict manual check instead.
        return Build.VERSION.SDK_INT > 34
    }

    /** Generate a List of [Participant]s, where each ID corresponds to a range of 1 to [num] */
    @ExperimentalAppActions
    fun generateParticipants(num: Int): List<Participant> {
        val participants = ArrayList<Participant>()
        for (i in 1..num) {
            participants.add(Participant(i.toString(), "part-$i"))
        }
        return participants
    }

    @ExperimentalAppActions
    fun getDefaultParticipant(): Participant {
        return Participant("123", "Gemini")
    }

    @ExperimentalAppActions
    fun getDefaultParticipantParcelable(): ParticipantParcelable {
        return getDefaultParticipant().toParticipantParcelable()
    }

    @ExperimentalAppActions
    fun printParticipants(participants: Collection<Participant>, tag: String) {
        Log.i(LOG_TAG, tag + ": printParticipants: set size=${participants.size}")
        for (v in participants) {
            Log.i(LOG_TAG, "\t $v")
        }
    }
}

@ExperimentalAppActions
class TestCallCallbackListener(private val scope: CoroutineScope) : ITestAppControlCallback.Stub() {
    private val raisedHandFlow: MutableSharedFlow<Pair<String, Boolean>> =
        MutableSharedFlow(replay = 1)
    private val kickParticipantFlow: MutableSharedFlow<Pair<String, Participant?>> =
        MutableSharedFlow(replay = 1)
    private val isLocallySilencedFlow: MutableSharedFlow<Pair<String, Boolean>> =
        MutableStateFlow(Pair("", false))
    private val callAddedFlow: MutableSharedFlow<Pair<Int, String>> = MutableSharedFlow(replay = 1)

    override fun onCallAdded(requestId: Int, callId: String?) {
        if (callId == null) return
        scope.launch { callAddedFlow.emit(Pair(requestId, callId)) }
    }

    override fun raiseHandStateAction(callId: String?, isHandRaised: Boolean) {
        if (callId == null) return
        scope.launch { raisedHandFlow.emit(Pair(callId, isHandRaised)) }
    }

    override fun kickParticipantAction(callId: String?, participant: ParticipantParcelable?) {
        if (callId == null) return
        scope.launch { kickParticipantFlow.emit(Pair(callId, participant?.toParticipant())) }
    }

    override fun setLocalCallSilenceState(callId: String?, isLocallySilenced: Boolean) {
        if (callId == null) return
        scope.launch { isLocallySilencedFlow.emit(Pair(callId, isLocallySilenced)) }
    }

    suspend fun waitForCallAdded(requestId: Int): String? {
        return withTimeoutOrNull(5000) {
            callAddedFlow.filter { it.first == requestId }.map { it.second }.first()
        }
    }

    suspend fun waitForRaiseHandState(callId: String, expectedState: Boolean) {
        val result =
            withTimeoutOrNull(5000) {
                raisedHandFlow.filter { it.first == callId && it.second == expectedState }.first()
            }
        assertEquals("raised hands action never received", expectedState, result?.second)
    }

    suspend fun waitForIsLocalSilenced(callId: String, expectedState: Boolean) {
        val result =
            withTimeoutOrNull(5000) {
                isLocallySilencedFlow
                    .filter { it.first == callId && it.second == expectedState }
                    .first()
            }
        assertEquals("<LOCAL CALL SILENCE> never received", expectedState, result?.second)
    }

    suspend fun waitForKickParticipant(callId: String, expectedParticipant: Participant?) {
        val result =
            withTimeoutOrNull(5000) {
                kickParticipantFlow
                    .filter { it.first == callId && it.second == expectedParticipant }
                    .first()
            }
        assertEquals("kick participant action never received", expectedParticipant, result?.second)
    }
}
