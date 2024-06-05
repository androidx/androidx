/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.ads.adservices.java.signals

import android.adservices.signals.ProtectedSignalsManager
import android.content.Context
import android.net.Uri
import android.os.OutcomeReceiver
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.java.VersionCompatUtil
import androidx.privacysandbox.ads.adservices.java.signals.ProtectedSignalsManagerFutures.Companion.from
import androidx.privacysandbox.ads.adservices.signals.UpdateSignalsRequest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.android.dx.mockito.inline.extended.StaticMockitoSession
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.invocation.InvocationOnMock

@OptIn(ExperimentalFeatures.Ext12OptIn::class)
@SmallTest
@SuppressWarnings("NewApi")
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 34)
class ProtectedSignalsManagerFuturesTest {

    private var mSession: StaticMockitoSession? = null

    @Before
    fun setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext<Context>())
    }

    @After
    fun tearDown() {
        mSession?.finishMocking()
    }

    @Test
    fun testUpdateSignals() {
        Assume.assumeTrue(
            "minSdkVersion = API 34 ext 12",
            VersionCompatUtil.isTPlusWithMinAdServicesVersion(12)
        )
        val protectedSignalsManager = mockProtectedSignalsManager(mContext)
        setupResponse(protectedSignalsManager)
        val jetpackManager = from(mContext)

        val request = UpdateSignalsRequest(uri)
        jetpackManager!!.updateSignalsAsync(request).get()

        // Verify that the compat code was invoked correctly.
        val captor =
            ArgumentCaptor.forClass(android.adservices.signals.UpdateSignalsRequest::class.java)
        verify(protectedSignalsManager).updateSignals(captor.capture(), any(), any())

        // Verify that the request that the compat code makes to the platform is correct.
        verifyUpdateSignalsRequest(captor.value)
    }

    companion object {
        private lateinit var mContext: Context
        private val uri: Uri = Uri.parse("abc.com")

        private fun mockProtectedSignalsManager(spyContext: Context): ProtectedSignalsManager {
            val signalsManager = mock(ProtectedSignalsManager::class.java)
            `when`(spyContext.getSystemService(ProtectedSignalsManager::class.java))
                .thenReturn(signalsManager)
            return signalsManager
        }

        private fun setupResponse(protectedSignalsManager: ProtectedSignalsManager) {
            val answer = { args: InvocationOnMock ->
                val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(2)
                receiver.onResult(Object())
                null
            }
            doAnswer(answer).`when`(protectedSignalsManager).updateSignals(any(), any(), any())
        }

        private fun verifyUpdateSignalsRequest(
            updateSignalsRequest: android.adservices.signals.UpdateSignalsRequest
        ) {

            val expectedRequest =
                android.adservices.signals.UpdateSignalsRequest.Builder(uri).build()

            // Verify that the actual request matches the expected one.
            assertEquals(expectedRequest.updateUri, updateSignalsRequest.updateUri)
        }
    }
}
