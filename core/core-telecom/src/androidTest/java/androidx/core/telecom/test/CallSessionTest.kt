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
import android.os.ParcelUuid
import android.telecom.CallEndpoint
import androidx.annotation.RequiresApi
import androidx.core.telecom.extensions.voip.VoipExtensionManager
import androidx.core.telecom.internal.CallChannels
import androidx.core.telecom.internal.CallSession
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.util.ExperimentalAppActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test class should be used to test behavior in the
 * [androidx.core.telecom.internal.CallSession] object.  All transactional calls are wrapped in a
 * [androidx.core.telecom.internal.CallSession] object.
 */
@SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE /* api=34 */)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, ExperimentalAppActions::class)
@RequiresApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
@RunWith(AndroidJUnit4::class)
class CallSessionTest : BaseTelecomTest() {

    /**
     * verify the CallEvent CompletableDeferred objects complete after endpoints are echoed.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testCompletableDeferredObjectsComplete() {
        setUpV2Test()
        runBlocking {
            val callChannels = CallChannels()
            val callEvents = initCallEvents(callChannels, coroutineContext)

            assertFalse(callEvents.getIsAvailableEndpointsSet().isCompleted)
            assertFalse(callEvents.getIsCurrentEndpointSet().isCompleted)

            callEvents.onCallEndpointChanged(getCurrentEndpoint())
            callEvents.onAvailableCallEndpointsChanged(getAvailableEndpoint())

            assertTrue(callEvents.getIsAvailableEndpointsSet().isCompleted)
            assertTrue(callEvents.getIsCurrentEndpointSet().isCompleted)
            callChannels.closeAllChannels()
        }
    }

    /**
     * verify the call channels are receivable given the new CompletableDeferred object logic
     * in the CallEvent callbacks.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testCallEventsEchoEndpoints() {
        setUpV2Test()
        runBlocking {
            val callChannels = CallChannels()
            val callEvents = initCallEvents(callChannels, coroutineContext)

            callEvents.onCallEndpointChanged(getCurrentEndpoint())
            callEvents.onAvailableCallEndpointsChanged(getAvailableEndpoint())

            assertEquals(
                getAvailableEndpoint().size,
                callChannels.availableEndpointChannel.receive().size
            )
            assertNotNull(callChannels.currentEndpointChannel.receive())
            callChannels.closeAllChannels()
        }
    }

    private fun initCallEvents(callChannels: CallChannels, coroutineContext: CoroutineContext):
        CallSession.CallEventCallbackImpl {
        return CallSession.CallEventCallbackImpl(callChannels, coroutineContext,
            VoipExtensionManager(mContext, coroutineContext, callChannels, mutableListOf())
        )
    }

    fun getCurrentEndpoint(): CallEndpoint {
        return CallEndpoint("EARPIECE", CallEndpoint.TYPE_EARPIECE,
            ParcelUuid.fromString(UUID.randomUUID().toString()))
    }
    fun getAvailableEndpoint(): List<CallEndpoint> {
        val endpoints = mutableListOf<CallEndpoint>()
        endpoints.add(getCurrentEndpoint())
        return endpoints
    }
}
