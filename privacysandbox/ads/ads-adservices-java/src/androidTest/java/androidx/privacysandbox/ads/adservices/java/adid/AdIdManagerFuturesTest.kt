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

import android.content.Context
import android.os.Looper
import android.os.OutcomeReceiver
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.privacysandbox.ads.adservices.adid.AdId
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import com.google.common.util.concurrent.ListenableFuture
import kotlin.test.assertNotEquals
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock

@SmallTest
@SuppressWarnings("NewApi")
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 30)
class AdIdManagerFuturesTest {

    @Before
    fun setUp() {
        mContext = Mockito.spy(ApplicationProvider.getApplicationContext<Context>())
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33, minSdkVersion = 30)
    fun testAdIdOlderVersions() {
        val sdkExtVersion = SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES)
        Assume.assumeTrue("maxSdkVersion = API 33 ext 3", sdkExtVersion < 4)
        Truth.assertThat(AdIdManagerFutures.from(mContext)).isEqualTo(null)
    }

    @Test
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    fun testAdIdAsync() {
        val sdkExtVersion = SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES)
        Assume.assumeTrue("minSdkVersion = API 33 ext 4", sdkExtVersion >= 4)
        val adIdManager = mockAdIdManager(mContext)
        setupResponse(adIdManager)
        val managerCompat = AdIdManagerFutures.from(mContext)

        // Actually invoke the compat code.
        val result: ListenableFuture<AdId> = managerCompat!!.getAdIdAsync()

        // Verify that the result of the compat call is correct.
        verifyResponse(result.get())

        // Verify that the compat code was invoked correctly.
        Mockito.verify(adIdManager).getAdId(ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    @SuppressWarnings("NewApi")
    @SdkSuppress(minSdkVersion = 30)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    companion object {
        private lateinit var mContext: Context

        private fun mockAdIdManager(spyContext: Context): android.adservices.adid.AdIdManager {
            val adIdManager = Mockito.mock(android.adservices.adid.AdIdManager::class.java)
            Mockito.`when`(spyContext.getSystemService(
                android.adservices.adid.AdIdManager::class.java)).thenReturn(adIdManager)
            return adIdManager
        }

        private fun setupResponse(adIdManager: android.adservices.adid.AdIdManager) {
            // Set up the response that AdIdManager will return when the compat code calls it.
            val adId = android.adservices.adid.AdId("1234", false)
            val answer = { args: InvocationOnMock ->
                assertNotEquals(Looper.getMainLooper(), Looper.myLooper())
                val receiver = args.getArgument<
                    OutcomeReceiver<android.adservices.adid.AdId, Exception>>(1)
                receiver.onResult(adId)
                null
            }
            Mockito.doAnswer(answer)
                .`when`(adIdManager).getAdId(
                    ArgumentMatchers.any(),
                    ArgumentMatchers.any()
                )
        }

        private fun verifyResponse(adId: androidx.privacysandbox.ads.adservices.adid.AdId) {
            Assert.assertEquals("1234", adId.adId)
            Assert.assertEquals(false, adId.isLimitAdTrackingEnabled)
        }
    }
}