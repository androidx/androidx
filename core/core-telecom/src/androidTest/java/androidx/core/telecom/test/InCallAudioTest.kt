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

package androidx.core.telecom.test

import android.media.AudioManager.MODE_IN_COMMUNICATION
import android.os.Build
import android.telecom.DisconnectCause
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.internal.utils.Utils
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.TestUtils
import androidx.core.telecom.test.utils.TestUtils.getAudioModeName
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test class verifies the in call audio functionality is working as intended when adding
 * a VoIP call.  Each test should add a call via [CallsManager.addCall] and changes the call state
 * via the [CallControlScope].
 *
 * Note: Be careful with using a delay in a runBlocking scope to avoid missing flows. ex:
 * runBlocking {
 *      addCall(...){
 *          delay(x time) // The flow will be emitted here and missed
 *          currentCallEndpoint.counter.getFirst() // The flow may never be collected
 *      }
 * }
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RequiresApi(Build.VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class InCallAudioTest : BaseTelecomTest() {
    val LOG_TAG = "InCallAudioTest"
    @Before
    fun setUp() {
        Utils.resetUtils()
    }

    @After
    fun onDestroy() {
        Utils.resetUtils()
    }

    /***********************************************************************************************
     *                           V2 APIs (Android U and above) tests
     *********************************************************************************************/

    /**
     * assert that a newly added *OUTGOING* call has an audio mode equal to [MODE_IN_COMMUNICATION].
     * The call should use the *V2 platform APIs* under the hood.
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @LargeTest
    @Test(timeout = 10000)
    @Ignore
    fun testAddCallAssertModeInCommunication() {
        setUpV2Test()
        runBlocking_addCall_assertAudioModeInCommunication()
    }

    /***********************************************************************************************
     *                           Backwards Compatibility Layer tests
     *********************************************************************************************/

    /**
     * assert that a newly added *OUTGOING* call has an audio mode equal to [MODE_IN_COMMUNICATION].
     * The call should use the *[android.telecom.ConnectionService] and [android.telecom.Connection]
     * APIs* under the hood.
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @LargeTest
    @Test(timeout = 10000)
    @Ignore
    fun testAddCallAssertModeInCommunication_BackwardsCompat() {
        setUpBackwardsCompatTest()
        runBlocking_addCall_assertAudioModeInCommunication()
    }

    /***********************************************************************************************
     *                           Helpers
     *********************************************************************************************/

    /**
     * This helper facilitates adding a call, checking that the audio mode equals
     * [MODE_IN_COMMUNICATION], and disconnecting.
     *
     * Note: delays are inserted to simulate more natural calling. Otherwise the call dumpsys
     * does not reflect realistic transitions.
     *
     * Note: This helper blocks the TestRunner from finishing until all asserts and async functions
     * have finished or the timeout has been reached.
     */
    private fun runBlocking_addCall_assertAudioModeInCommunication() {
        runBlocking {
            assertWithinTimeout_addCall(TestUtils.OUTGOING_CALL_ATTRIBUTES) {
                launch {
                    Log.i(LOG_TAG, "runBlocking_addCall_assertAudioModeInCommunication: " +
                        "initial AudioManager mode = ${getAudioModeName(mAudioManager.mode)}")
                    while (isActive /* aka  within timeout window */ &&
                        mAudioManager.mode != MODE_IN_COMMUNICATION) {
                        Log.d(LOG_TAG, "runBlocking_addCall_assertAudioModeInCommunication: " +
                            "current AudioManager mode = ${getAudioModeName(mAudioManager.mode)}")
                        yield() // mechanism to stop the while loop if the coroutine is dead
                        delay(1) // sleep x millisecond(s) instead of spamming check
                    }
                    Assert.assertEquals(CallControlResult.Success(),
                        disconnect(DisconnectCause(DisconnectCause.LOCAL)))
                }
            }
        }
    }
}
