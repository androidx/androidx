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

package androidx.privacysandbox.ads.adservices.adid

import android.content.Context
import android.os.OutcomeReceiver
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.dx.mockito.inline.extended.StaticMockitoSession
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any

@SmallTest
@SuppressWarnings("NewApi")
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 30)
class AdIdManagerTest {
    private var mSession: StaticMockitoSession? = null
    private val mValidAdServicesSdkExtVersion = AdServicesInfo.adServicesVersion() >= 4
    private val mValidAdExtServicesSdkExtVersionS = AdServicesInfo.extServicesVersionS() >= 9

    @Before
    fun setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext<Context>())

        if (mValidAdExtServicesSdkExtVersionS) {
            // setup a mockitoSession to return the mocked manager
            // when the static method .get() is called
            mSession =
                ExtendedMockito.mockitoSession()
                    .mockStatic(android.adservices.adid.AdIdManager::class.java)
                    .startMocking()
        }
    }

    @After
    fun tearDown() {
        mSession?.finishMocking()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33, minSdkVersion = 30)
    fun testAdIdOlderVersions() {
        Assume.assumeTrue("maxSdkVersion = API 33 ext 3", !mValidAdServicesSdkExtVersion)
        Assume.assumeTrue("maxSdkVersion = API 31/32 ext 8", !mValidAdExtServicesSdkExtVersionS)
        assertThat(AdIdManager.obtain(mContext)).isNull()
    }

    @Test
    fun testAdIdManagerNoClassDefFoundError() {
        Assume.assumeTrue("minSdkVersion = API 31/32 ext 9", mValidAdExtServicesSdkExtVersionS)

        `when`(android.adservices.adid.AdIdManager.get(any())).thenThrow(NoClassDefFoundError())
        assertThat(AdIdManager.obtain(mContext)).isNull()
    }

    @Test
    fun testAdIdAsync() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 4 or API 31/32 ext 9",
            mValidAdServicesSdkExtVersion || mValidAdExtServicesSdkExtVersionS
        )

        val adIdManager = mockAdIdManager(mContext, mValidAdExtServicesSdkExtVersionS)
        setupResponseSPlus(adIdManager)

        val managerCompat = AdIdManager.obtain(mContext)

        // Actually invoke the compat code.
        val result = runBlocking { managerCompat!!.getAdId() }

        // Verify that the compat code was invoked correctly.
        verifyOnSPlus(adIdManager)

        // Verify that the result of the compat call is correct.
        verifyResponse(result)
    }

    @SdkSuppress(minSdkVersion = 30)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    companion object {
        private lateinit var mContext: Context

        private fun mockAdIdManager(
            spyContext: Context,
            isExtServices: Boolean
        ): android.adservices.adid.AdIdManager {
            val adIdManager = mock(android.adservices.adid.AdIdManager::class.java)
            // only mock the .get() method if using extServices version
            if (isExtServices) {
                `when`(android.adservices.adid.AdIdManager.get(any())).thenReturn(adIdManager)
            } else {
                `when`(spyContext.getSystemService(android.adservices.adid.AdIdManager::class.java))
                    .thenReturn(adIdManager)
            }
            return adIdManager
        }

        private fun setupResponseSPlus(adIdManager: android.adservices.adid.AdIdManager) {
            // Set up the response that AdIdManager will return when the compat code calls it.
            val adId = android.adservices.adid.AdId("1234", false)
            val answer = { args: InvocationOnMock ->
                val receiver =
                    args.getArgument<OutcomeReceiver<android.adservices.adid.AdId, Exception>>(1)
                receiver.onResult(adId)
                null
            }
            doAnswer(answer)
                .`when`(adIdManager)
                .getAdId(
                    any<Executor>(),
                    any<OutcomeReceiver<android.adservices.adid.AdId, Exception>>()
                )
        }

        private fun verifyOnSPlus(adIdManager: android.adservices.adid.AdIdManager) {
            verify(adIdManager)
                .getAdId(
                    any<Executor>(),
                    any<OutcomeReceiver<android.adservices.adid.AdId, Exception>>()
                )
        }

        private fun verifyResponse(adId: AdId) {
            Assert.assertEquals("1234", adId.adId)
            Assert.assertEquals(false, adId.isLimitAdTrackingEnabled)
        }
    }
}
