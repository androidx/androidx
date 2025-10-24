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

package androidx.core.uwb.impl

// import org.junit.Before
// import org.junit.Test
import android.os.Build
import android.ranging.RangingManager
import android.ranging.RangingSession
import androidx.core.uwb.RangingResult
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.common.TestCommons
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Captor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
class UwbControllerSessionScopeRangingImplTest {
    private lateinit var rangingManager: RangingManager
    private lateinit var rangingSession: RangingSession
    private lateinit var uwbControllerSessionScope: UwbControllerSessionScopeRangingImpl

    @Captor private lateinit var callbackCaptor: ArgumentCaptor<RangingSession.Callback>

    //    @Before
    fun setUp() {
        rangingManager = mock(RangingManager::class.java)
        rangingSession = mock(RangingSession::class.java)
        `when`(
                rangingManager.createRangingSession(
                    any(Executor::class.java),
                    any(RangingSession.Callback::class.java),
                )
            )
            .thenReturn(rangingSession)

        uwbControllerSessionScope =
            UwbControllerSessionScopeRangingImpl(
                rangingManager,
                TestCommons.Companion.GenericRanging.CAPABILITIES_GENERIC_RANGING,
                UwbAddress(TestCommons.Companion.GenericRanging.LOCAL_RANGING_ADDRESS),
                androidx.core.uwb.UwbComplexChannel(
                    TestCommons.COMPLEX_CHANNEL.channel,
                    TestCommons.COMPLEX_CHANNEL.preambleIndex,
                ),
            )
    }

    //    @Test
    fun testStartRangingAndAddControlee_success() = runBlocking {
        val channel = Channel<RangingResult>(1)
        val sessionFlow =
            uwbControllerSessionScope.prepareSession(
                TestCommons.Companion.GenericRanging.GENERIC_RANGING_PARAMETERS
            )
        val job = sessionFlow.cancellable().onEach { channel.send(it) }.launchIn(this)

        val deferredResult = async { channel.receive() }

        delay(100)
        verify(rangingManager)
            .createRangingSession(any(Executor::class.java), callbackCaptor.capture())
        callbackCaptor.value.onOpened()

        val rangingResult = deferredResult.await()
        assertThat(rangingResult is RangingResult.RangingResultInitialized).isTrue()

        uwbControllerSessionScope.addControlee(
            UwbAddress(TestCommons.Companion.GenericRanging.NEIGHBOR_3)
        )
        verify(rangingSession).addDeviceToRangingSession(any())

        job.cancel()
    }
}
