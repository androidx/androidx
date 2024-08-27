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

package androidx.privacysandbox.ads.adservices.java.adid

import android.adservices.adid.AdIdManager
import android.content.Context
import android.os.Looper
import android.os.OutcomeReceiver
import androidx.privacysandbox.ads.adservices.adid.AdId
import androidx.privacysandbox.ads.adservices.java.VersionCompatUtil
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.dx.mockito.inline.extended.StaticMockitoSession
import com.google.common.truth.Truth
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import kotlin.test.assertNotEquals
import org.junit.After
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.spy
import org.mockito.Mockito.`when`
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any

@SmallTest
@SuppressWarnings("NewApi")
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 30)
class AdIdManagerFuturesTest {

    private var mSession: StaticMockitoSession? = null
    private val mValidAdExtServicesSdkExtVersionS =
        VersionCompatUtil.isSWithMinExtServicesVersion(9)

    @Before
    fun setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext<Context>())

        if (mValidAdExtServicesSdkExtVersionS) {
            // setup a mockitoSession to return the mocked manager
            // when the static method .get() is called
            mSession =
                ExtendedMockito.mockitoSession().mockStatic(AdIdManager::class.java).startMocking()
        }
    }

    @After
    fun tearDown() {
        mSession?.finishMocking()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33, minSdkVersion = 30)
    fun testAdIdOlderVersions() {
        Assume.assumeFalse(
            "maxSdkVersion = API 33 ext 3 or API 31/32 ext 8",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion=*/ 4,
                /* minExtServicesVersionS=*/ 9,
            )
        )
        Truth.assertThat(AdIdManagerFutures.from(mContext)).isEqualTo(null)
    }

    @Test
    fun testAdIdAsync() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 4 or API 31/32 ext 9",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion= */ 4,
                /* minExtServicesVersionS=*/ 9,
            )
        )

        val adIdManager = mockAdIdManager(mContext, mValidAdExtServicesSdkExtVersionS)
        setupResponseSPlus(adIdManager)

        val managerCompat = AdIdManagerFutures.from(mContext)

        // Actually invoke the compat code.
        val result: ListenableFuture<AdId> = managerCompat!!.getAdIdAsync()

        // Verify that the result of the compat call is correct.
        verifyResponse(result.get())
        verifyOnSPlus(adIdManager)
    }

    @SdkSuppress(minSdkVersion = 30)
    companion object {
        private lateinit var mContext: Context

        private fun mockAdIdManager(spyContext: Context, isExtServices: Boolean): AdIdManager {
            val adIdManager = Mockito.mock(AdIdManager::class.java)
            // mock the .get() method if using extServices version, otherwise mock getSystemService
            if (isExtServices) {
                `when`(AdIdManager.get(any())).thenReturn(adIdManager)
            } else {
                `when`(spyContext.getSystemService(AdIdManager::class.java)).thenReturn(adIdManager)
            }
            return adIdManager
        }

        private fun setupResponseSPlus(adIdManager: AdIdManager) {
            // Set up the response that AdIdManager will return when the compat code calls it.
            val adId = android.adservices.adid.AdId("1234", false)
            val answer = { args: InvocationOnMock ->
                assertNotEquals(Looper.getMainLooper(), Looper.myLooper())
                val receiver =
                    args.getArgument<OutcomeReceiver<android.adservices.adid.AdId, Exception>>(1)
                receiver.onResult(adId)
                null
            }
            Mockito.doAnswer(answer)
                .`when`(adIdManager)
                .getAdId(
                    any<Executor>(),
                    any<OutcomeReceiver<android.adservices.adid.AdId, Exception>>()
                )
        }

        private fun verifyOnSPlus(adIdManager: AdIdManager) {
            // Verify that the compat code was invoked correctly.
            Mockito.verify(adIdManager)
                .getAdId(
                    any<Executor>(),
                    any<OutcomeReceiver<android.adservices.adid.AdId, Exception>>()
                )
        }

        private fun verifyResponse(adId: androidx.privacysandbox.ads.adservices.adid.AdId) {
            Assert.assertEquals("1234", adId.adId)
            Assert.assertEquals(false, adId.isLimitAdTrackingEnabled)
        }
    }
}
