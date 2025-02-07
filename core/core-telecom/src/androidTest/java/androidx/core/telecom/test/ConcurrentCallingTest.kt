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

package androidx.core.telecom.test

import android.os.Build
import android.os.Build.VERSION_CODES
import android.telecom.DisconnectCause
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.TestUtils.INCOMING_CALL_ATTRIBUTES
import androidx.core.telecom.test.utils.TestUtils.OUTGOING_CALL_ATTRIBUTES
import androidx.core.telecom.test.utils.TestUtils.mOnAnswerLambda
import androidx.core.telecom.test.utils.TestUtils.mOnDisconnectLambda
import androidx.core.telecom.test.utils.TestUtils.mOnSetActiveLambda
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = VERSION_CODES.O)
@RequiresApi(VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class ConcurrentCallingTest : BaseTelecomTest() {
    companion object {
        val TAG = ConcurrentCallingTest::class.simpleName
        const val DISCONNECT_DELAY: Long = 1000L
    }

    // Data class to represent different call scenarios. Each scenario defines the
    // attributes of the first and second call, and whether each call is expected
    // to be set to inactive.
    data class CallScenario(
        val firstCallAttributes: CallAttributesCompat,
        val firstCallExpectInactive: Boolean = true,
        val secondCallAttributes: CallAttributesCompat,
        val secondCallExpectInactive: Boolean = false
    )

    // List of call scenarios to test. Each scenario represents a different
    // combination of incoming and outgoing calls.
    val callScenarios =
        listOf(
            // Scenario 1: Outgoing call followed by incoming call. The outgoing call
            // is expected to become inactive when the incoming call arrives.
            CallScenario(
                firstCallAttributes = OUTGOING_CALL_ATTRIBUTES,
                firstCallExpectInactive = true,
                secondCallAttributes = INCOMING_CALL_ATTRIBUTES,
                secondCallExpectInactive = false
            ),
            // Scenario 2: Start a call and make an outgoing call. The active call
            // is expected to become inactive when the outgoing call starts.
            CallScenario(
                firstCallAttributes = INCOMING_CALL_ATTRIBUTES,
                firstCallExpectInactive = true,
                secondCallAttributes = OUTGOING_CALL_ATTRIBUTES,
                secondCallExpectInactive = false
            )
        )

    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test(timeout = 10000)
    fun testCallStateTransitions() {
        // This parameterized test verifies the behavior of the CallsManager when
        // two calls are made in sequence. It checks that both calls are added
        // successfully and that the first call transitions to the inactive state
        // when the second call arrives, based on the provided scenario.
        for (scenario in callScenarios) {
            runBlocking {
                try {
                    // Make the first call with the attributes and expectation defined
                    // in the scenario.
                    val firstCallIsActive = CompletableDeferred<CallControlResult>()
                    val firstCallDeferred = async {
                        addCall(
                            activeResult = firstCallIsActive,
                            callAttributes = scenario.firstCallAttributes,
                            expectInactiveCallback = scenario.firstCallExpectInactive
                        )
                    }

                    // Wait for the first call to go active before adding the second. This simulates
                    // the scenario where one call is already in progress when another call arrives.
                    firstCallIsActive.await()

                    // Make the second call with the attributes and expectation defined
                    // in the scenario.
                    val secondCallDeferred = async {
                        addCall(
                            callAttributes = scenario.secondCallAttributes,
                            expectInactiveCallback = scenario.secondCallExpectInactive
                        )
                    }

                    // Wait for both calls to complete.
                    val (firstCallResult, secondCallResult) =
                        awaitAll(firstCallDeferred, secondCallDeferred)

                    // Assert that both calls transited to active
                    assertEquals(CallControlResult.Success(), firstCallResult)
                    assertEquals(CallControlResult.Success(), secondCallResult)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in test", e)
                    fail("Test failed with exception: ${e.message}")
                }
            }
        }
    }

    /**
     * This helper function simulates adding a call to the CallsManager and waits for it to become
     * active, then disconnects it after a delay. It also handles the expectation of the call being
     * set to inactive.
     */
    suspend fun addCall(
        activeResult: CompletableDeferred<CallControlResult> =
            CompletableDeferred<CallControlResult>(),
        callAttributes: CallAttributesCompat,
        expectInactiveCallback: Boolean
    ): CallControlResult {
        val inactiveCall = run {
            // TODO:: b/391963273 to fix onSetInactive issue on P- builds
            if (expectInactiveCallback && isBuildAboveP()) {
                CompletableDeferred<Unit>()
            } else {
                CompletableDeferred(Unit)
            }
        }
        val endCall = CompletableDeferred<Unit>()
        Log.d(TAG, "addCall: callAttributes=[$callAttributes]")
        try {
            mCallsManager.addCall(
                callAttributes,
                mOnAnswerLambda,
                mOnDisconnectLambda,
                mOnSetActiveLambda,
                {
                    Log.d(TAG, "onSetInActive: name=[${callAttributes.displayName}] completing")
                    inactiveCall.complete(Unit)
                }
            ) {
                launch {
                    try {
                        activeResult.complete(setActive())
                        Log.d(TAG, "setActive: id=[${getCallId()}]")
                        // Delay before disconnecting the call to give the other call time to go
                        // active and the first call to be put on hold
                        delay(DISCONNECT_DELAY)
                        // Disconnect the call and complete the endCall deferred.
                        disconnect(DisconnectCause(DisconnectCause.LOCAL))
                        Log.d(TAG, "endCall: id=[${getCallId()}]")
                        endCall.complete(Unit)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in addCall coroutine", e)
                        activeResult.completeExceptionally(e)
                        endCall.completeExceptionally(e)
                    }
                }
            }
            // Wait for the call to become active, then disconnect it and wait for
            // the disconnection to complete. Also wait for the inactiveCall to
            // complete if it was expected to become inactive.
            return activeResult.await().also {
                endCall.await()
                inactiveCall.await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding call", e)
            throw e // Re-throw the exception for the test to fail.
        }
    }

    private fun isBuildAboveP(): Boolean {
        return Build.VERSION.SDK_INT > VERSION_CODES.P
    }
}
