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

package androidx.core.telecom.utils

import android.net.Uri
import android.os.Build.VERSION_CODES
import android.telecom.Call
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlCallback
import androidx.core.telecom.internal.utils.BuildVersionAdapter
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileInputStream
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield

/**
 * Singleton class.
 */
@RequiresApi(VERSION_CODES.O)
object TestUtils {
    const val LOG_TAG = "TelecomTestUtils"
    const val TEST_PACKAGE = "androidx.core.telecom.test"
    const val COMMAND_SET_DEFAULT_DIALER = "telecom set-default-dialer " // DO NOT REMOVE SPACE
    const val COMMAND_GET_DEFAULT_DIALER = "telecom get-default-dialer"
    const val COMMAND_CLEANUP_STUCK_CALLS = "cleanup-stuck-calls"
    const val COMMAND_DUMP_TELECOM = "dumpsys telecom"
    const val TEST_CALL_ATTRIB_NAME = "Elon Musk"
    const val OUTGOING_NAME = "Larry Page"
    const val INCOMING_NAME = "Sundar Pichai"
    const val WAIT_ON_ASSERTS_TO_FINISH_TIMEOUT = 5000L
    const val WAIT_ON_CALL_STATE_TIMEOUT = 5000L
    const val WAIT_ON_IN_CALL_SERVICE_CALL_COUNT_TIMEOUT = 5000L
    const val ALL_CALL_CAPABILITIES = (CallAttributesCompat.SUPPORTS_SET_INACTIVE
        or CallAttributesCompat.SUPPORTS_STREAM or CallAttributesCompat.SUPPORTS_TRANSFER)
    val VERIFICATION_TIMEOUT_MSG =
        "Timed out before asserting all values. This most likely means the platform failed to" +
            " add the call or hung on a CallControl operation."

    // non-primitive constants
    val TEST_PHONE_NUMBER_9001 = Uri.parse("tel:6506959001")
    val TEST_PHONE_NUMBER_8985 = Uri.parse("tel:6506958985")

    // Define the minimal set of properties to start an outgoing call
    val OUTGOING_CALL_ATTRIBUTES = CallAttributesCompat(
        OUTGOING_NAME,
        TEST_PHONE_NUMBER_8985,
        CallAttributesCompat.DIRECTION_OUTGOING,
        CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
        ALL_CALL_CAPABILITIES
    )

    val OUTGOING_NO_HOLD_CAP_CALL_ATTRIBUTES = CallAttributesCompat(
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
     * This build version should be set when the **V2 transactional APIs** are desired as
     * the underlying call management.
     */
    internal val mV2Build = object : BuildVersionAdapter {
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
    internal val mBackwardsCompatBuild = object : BuildVersionAdapter {
        override fun hasPlatformV2Apis(): Boolean {
            return false
        }

        override fun hasInvalidBuildVersion(): Boolean {
            return false
        }
    }

    /**
     * This build version should be set when edge case testing on invalid builds
     */
    internal val mInvalidBuild = object : BuildVersionAdapter {
        override fun hasPlatformV2Apis(): Boolean {
            return false
        }

        override fun hasInvalidBuildVersion(): Boolean {
            return true
        }
    }

    /**
     * This [CallControlCallback] implementation will be called by the platform whenever an
     * InCallService wants to [answer, setActive, setInactive, or disconnect] a particular call
     * and will immediately complete the transaction.
     */
    val mCompleteAllCallControlCallbacksImpl = object : CallControlCallback {
        override suspend fun onSetActive(): Boolean {
            Log.i(LOG_TAG, "mCACCCI: onSetActive: completing")
            mOnSetActiveCallbackCalled = true
            return true
        }

        override suspend fun onSetInactive(): Boolean {
            Log.i(LOG_TAG, "mCACCCI: onSetInactive: completing")
            mOnSetInactiveCallbackCalled = true
            return true
        }

        override suspend fun onAnswer(callType: Int): Boolean {
            Log.i(LOG_TAG, "mCACCCI: onAnswer: callType=[$callType]")
            mOnAnswerCallbackCalled = true
            return true
        }

        override suspend fun onDisconnect(disconnectCause: DisconnectCause): Boolean {
            Log.i(LOG_TAG, "mCACCCI: onDisconnect: disconnectCause=[$disconnectCause]")
            mOnDisconnectCallbackCalled = true
            return true
        }
    }

    var mOnSetActiveCallbackCalled = false
    var mOnSetInactiveCallbackCalled = false
    var mOnAnswerCallbackCalled = false
    var mOnDisconnectCallbackCalled = false

    fun resetCallbackConfigs() {
        mOnSetActiveCallbackCalled = false
        mOnSetInactiveCallbackCalled = false
        mOnAnswerCallbackCalled = false
        mOnDisconnectCallbackCalled = false
    }

    fun createCallAttributes(
        callDirection: Int,
        phoneAccountHandle: PhoneAccountHandle,
        callType: Int? = CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
    ): CallAttributesCompat {

        val attributes: CallAttributesCompat = if (callType != null) {
            CallAttributesCompat(
                TEST_CALL_ATTRIB_NAME,
                TEST_PHONE_NUMBER_9001,
                callDirection, callType
            )
        } else {
            CallAttributesCompat(
                TEST_CALL_ATTRIB_NAME,
                TEST_PHONE_NUMBER_9001,
                callDirection
            )
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

    fun getDefaultDialer(): String {
        val s = runShellCommand(COMMAND_GET_DEFAULT_DIALER)
        return s.replace("\n", "")
    }

    fun dumpTelecom() {
        Log.i(LOG_TAG, "telecom dumpsys=[${runShellCommand(COMMAND_DUMP_TELECOM)}]")
        Log.i(LOG_TAG, "defaultDialer=[${getDefaultDialer()}]")
    }

    @Suppress("deprecation")
    suspend fun waitOnInCallServiceToReachXCalls(targetCallCount: Int): Call? {
        var targetCall: Call?
        try {
            withTimeout(WAIT_ON_IN_CALL_SERVICE_CALL_COUNT_TIMEOUT) {
                Log.i(LOG_TAG, "waitOnInCallServiceToReachXCalls: starting call check")
                while (isActive &&
                    (MockInCallService.getCallCount() < targetCallCount)
                ) {
                    yield() // ensure the coroutine is not canceled
                    delay(1) // sleep x millisecond(s) instead of spamming check
                }
                targetCall = MockInCallService.getLastCall()
                Log.i(
                    LOG_TAG, "waitOnInCallServiceToReachXCalls: " +
                        "found targetCall=[$targetCall]"
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.i(LOG_TAG, "waitOnInCallServiceToReachXCalls: timeout reached")
            dumpTelecom()
            MockInCallService.destroyAllCalls()
            throw AssertionError(
                "Expected call count to be <$targetCallCount>" +
                    " but the Actual call count was <${MockInCallService.getCallCount()}>"
            )
        }
        return targetCall
    }

    @Suppress("deprecation")
    suspend fun waitOnCallState(call: Call, targetState: Int) {
        try {
            withTimeout(WAIT_ON_CALL_STATE_TIMEOUT) {
                while (isActive /* aka  within timeout window */ &&
                    (call.state != targetState)
                ) {
                    yield() // another mechanism to stop the while loop if the coroutine is dead
                    delay(1) // sleep x millisecond(s) instead of spamming check
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.i(LOG_TAG, "waitOnCallState: timeout reached")
            dumpTelecom()
            MockInCallService.destroyAllCalls()
            throw AssertionError(
                "Expected call state to be <$targetState>" +
                    " but the Actual call state was <${call.state}>"
            )
        }
    }
}