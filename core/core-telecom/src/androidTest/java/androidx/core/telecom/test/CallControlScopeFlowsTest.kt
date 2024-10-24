/*
 * Copyright 2024 The Android Open Source Project
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
import android.telecom.DisconnectCause
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.TestUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = VERSION_CODES.O /* api=26 */)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RequiresApi(VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class CallControlScopeFlowsTest : BaseTelecomTest() {

    companion object {
        val TAG = CallControlScopeFlowsTest::class.java.simpleName
    }

    /**
     * verify when a call starts, all the CallControlScope flows echo values for transactional
     * calls.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testFlowsEchoValues_Transactional() {
        setUpV2Test()
        runBlocking { assertFlowsEchoValues() }
    }

    /**
     * verify when a call starts, all the CallControlScope flows echo values for ConnectionService
     * calls.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @SmallTest
    @Test
    fun testFlowsEchoValues_BackwardsCompat() {
        setUpBackwardsCompatTest()
        runBlocking { assertFlowsEchoValues() }
    }

    suspend fun assertFlowsEchoValues() {
        assertWithinTimeout_addCall(
            CallAttributesCompat(
                TestUtils.OUTGOING_NAME,
                TestUtils.TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                CallAttributesCompat.CALL_TYPE_VIDEO_CALL
            )
        ) {
            launch {
                val waitUntilCurrentEndpointJob = CompletableDeferred<CallEndpointCompat>()
                val waitUntilAvailableEndpointJob = CompletableDeferred<List<CallEndpointCompat>>()
                val waitUntilMuteStateJob = CompletableDeferred<Boolean>()

                launch {
                    currentCallEndpoint.collect {
                        Log.i(TAG, "currentCallEndpoint: $it")
                        waitUntilCurrentEndpointJob.complete(it)
                    }
                }

                launch {
                    availableEndpoints.collect {
                        Log.i(TAG, "availableEndpoints: $it")
                        waitUntilAvailableEndpointJob.complete(it)
                    }
                }

                launch {
                    isMuted.collect {
                        Log.i(TAG, "isMuted: $it")
                        waitUntilMuteStateJob.complete(it)
                    }
                }

                Log.i(TAG, "before awaitAll")
                awaitAll(
                    waitUntilCurrentEndpointJob,
                    waitUntilAvailableEndpointJob,
                    waitUntilMuteStateJob
                )
                Log.i(TAG, "after awaitAll")

                // at this point, the CallEndpoint has been found
                assertNotNull(waitUntilCurrentEndpointJob.getCompleted())
                assertNotNull(waitUntilAvailableEndpointJob.getCompleted())
                assertFalse(waitUntilMuteStateJob.getCompleted())

                // finally, terminate the call
                disconnect(DisconnectCause(DisconnectCause.LOCAL))

                cancel()
                Log.i(TAG, "cancel called")
            }
        }
    }
}
