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
import android.telecom.CallAudioState
import android.telecom.CallAudioState.ROUTE_EARPIECE
import android.telecom.CallAudioState.ROUTE_SPEAKER
import androidx.core.telecom.internal.CallChannels
import androidx.core.telecom.internal.CallSessionLegacy
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.TestUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class CallSessionLegacyFilteringTest : BaseTelecomTest() {

    @SmallTest
    @Test
    fun testCurrentEndpointFiltering() {
        runBlocking {
            val callChannels = CallChannels()
            val callSession = initCallSessionLegacy(coroutineContext, callChannels)

            val cas = CallAudioState(false, ROUTE_EARPIECE, ROUTE_EARPIECE or ROUTE_SPEAKER)

            // First update should go through
            callSession.onCallAudioStateChanged(cas)
            val firstResult = callChannels.currentEndpointChannel.tryReceive()
            assertTrue(firstResult.isSuccess)
            assertEquals(ROUTE_EARPIECE, firstResult.getOrThrow().type)

            // Second redundant update should be filtered
            callSession.onCallAudioStateChanged(cas)
            val secondResult = callChannels.currentEndpointChannel.tryReceive()
            assertTrue(secondResult.isFailure)
        }
    }

    @SmallTest
    @Test
    fun testAvailableEndpointsFiltering() {
        runBlocking {
            val callChannels = CallChannels()
            val callSession = initCallSessionLegacy(coroutineContext, callChannels)

            val cas = CallAudioState(false, ROUTE_EARPIECE, ROUTE_EARPIECE or ROUTE_SPEAKER)

            // First update should go through
            callSession.onCallAudioStateChanged(cas)
            val firstResult = callChannels.availableEndpointChannel.tryReceive()
            assertTrue(firstResult.isSuccess)
            assertEquals(2, firstResult.getOrThrow().size)

            // Second redundant update should be filtered
            callSession.onCallAudioStateChanged(cas)
            val secondResult = callChannels.availableEndpointChannel.tryReceive()
            assertTrue(secondResult.isFailure)
        }
    }

    @SmallTest
    @Test
    fun testMuteStateFiltering() {
        runBlocking {
            val callChannels = CallChannels()
            val callSession = initCallSessionLegacy(coroutineContext, callChannels)

            val casMuted = CallAudioState(true, ROUTE_EARPIECE, ROUTE_EARPIECE)

            // First update should go through
            callSession.onCallAudioStateChanged(casMuted)
            val firstResult = callChannels.isMutedChannel.tryReceive()
            assertTrue(firstResult.isSuccess)
            assertEquals(true, firstResult.getOrThrow())

            // Second redundant update should be filtered
            callSession.onCallAudioStateChanged(casMuted)
            val secondResult = callChannels.isMutedChannel.tryReceive()
            assertTrue(secondResult.isFailure)

            // Change to unmuted should go through
            val casUnmuted = CallAudioState(false, ROUTE_EARPIECE, ROUTE_EARPIECE)
            callSession.onCallAudioStateChanged(casUnmuted)
            val thirdResult = callChannels.isMutedChannel.tryReceive()
            assertTrue(thirdResult.isSuccess)
            assertEquals(false, thirdResult.getOrThrow())
        }
    }

    private fun initCallSessionLegacy(
        coroutineContext: CoroutineContext,
        callChannels: CallChannels,
    ): CallSessionLegacy {
        return CallSessionLegacy(
            ParcelUuid.fromString(UUID.randomUUID().toString()),
            mContext,
            TestUtils.INCOMING_CALL_ATTRIBUTES,
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
        )
    }
}
