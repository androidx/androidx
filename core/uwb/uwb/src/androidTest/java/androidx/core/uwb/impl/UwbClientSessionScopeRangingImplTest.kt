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
import androidx.annotation.RequiresApi
import androidx.core.uwb.RangingResult
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.common.TestCommons
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
class UwbClientSessionScopeRangingImplTest {

    @Mock private lateinit var rangingManager: RangingManager
    @Mock private lateinit var rangingSession: RangingSession
    private lateinit var uwbClientSessionScope: UwbClientSessionScopeRangingImpl

    @Captor private lateinit var callbackCaptor: ArgumentCaptor<RangingSession.Callback>

    //    @Before
    fun setUp() {
        `when`(
                rangingManager.createRangingSession(
                    any(Executor::class.java),
                    any(RangingSession.Callback::class.java),
                )
            )
            .thenReturn(rangingSession)

        uwbClientSessionScope =
            @RequiresApi(Build.VERSION_CODES.BAKLAVA)
            object :
                UwbClientSessionScopeRangingImpl(
                    rangingManager,
                    TestCommons.Companion.GenericRanging.CAPABILITIES_GENERIC_RANGING,
                    UwbAddress(TestCommons.LOCAL_ADDRESS.address),
                ) {
                override fun buildRangingPreference(
                    parameters: androidx.core.uwb.RangingParameters
                ) = mock(android.ranging.RangingPreference::class.java)
            }
    }

    //    @Test
    fun testPrepareSession_success() = runBlocking {
        val sessionFlow =
            uwbClientSessionScope.prepareSession(
                TestCommons.Companion.GenericRanging.GENERIC_RANGING_PARAMETERS
            )
        val deferredResult = async { sessionFlow.first() }
        delay(100)
        verify(rangingManager)
            .createRangingSession(any(Executor::class.java), callbackCaptor.capture())
        callbackCaptor.value.onOpened()
        val rangingResult = deferredResult.await()
        assertThat(rangingResult is RangingResult.RangingResultInitialized).isTrue()
    }
}
