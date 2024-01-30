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

package androidx.privacysandbox.ads.adservices.java.appsetid

import android.content.Context
import android.os.Looper
import android.os.OutcomeReceiver
import androidx.privacysandbox.ads.adservices.appsetid.AppSetId
import androidx.privacysandbox.ads.adservices.java.VersionCompatUtil
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.dx.mockito.inline.extended.StaticMockitoSession
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import kotlin.test.assertNotEquals
import org.junit.After
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.invocation.InvocationOnMock

@SmallTest
@SuppressWarnings("NewApi")
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 30)
class AppSetIdManagerFuturesTest {

    private var mSession: StaticMockitoSession? = null
    private val mValidAdExtServicesSdkExtVersion = VersionCompatUtil.isSWithMinExtServicesVersion(9)

    @Before
    fun setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext<Context>())

        if (mValidAdExtServicesSdkExtVersion) {
            // setup a mockitoSession to return the mocked manager
            // when the static method .get() is called
            mSession = ExtendedMockito.mockitoSession()
                .mockStatic(android.adservices.appsetid.AppSetIdManager::class.java)
                .startMocking()
        }
    }

    @After
    fun tearDown() {
        mSession?.finishMocking()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33, minSdkVersion = 30)
    fun testAppSetIdOlderVersions() {
        Assume.assumeFalse("maxSdkVersion = API 33 ext 3 or API 31/32 ext 8",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion=*/ 4,
                /* minExtServicesVersion=*/ 9))
        assertThat(AppSetIdManagerFutures.from(mContext)).isEqualTo(null)
    }

    @Test
    fun testAppSetIdAsync() {
        Assume.assumeTrue("minSdkVersion = API 33 ext 4 or API 31/32 ext 9",
            VersionCompatUtil.isTestableVersion(
                /* minAdServicesVersion= */ 4,
                /* minExtServicesVersion=*/ 9))

        val appSetIdManager = mockAppSetIdManager(mContext, mValidAdExtServicesSdkExtVersion)
        setupResponse(appSetIdManager)
        val managerCompat = AppSetIdManagerFutures.from(mContext)

        // Actually invoke the compat code.
        val result: ListenableFuture<AppSetId> = managerCompat!!.getAppSetIdAsync()

        // Verify that the result of the compat call is correct.
        verifyResponse(result.get())

        // Verify that the compat code was invoked correctly.
        verify(appSetIdManager).getAppSetId(any(), any())
    }

    @SdkSuppress(minSdkVersion = 30)
    companion object {
        private lateinit var mContext: Context

        private fun mockAppSetIdManager(
            spyContext: Context,
            isExtServices: Boolean
        ): android.adservices.appsetid.AppSetIdManager {
            val appSetIdManager = mock(android.adservices.appsetid.AppSetIdManager::class.java)
            // mock the .get() method if using extServices version, otherwise mock getSystemService
            if (isExtServices) {
                `when`(android.adservices.appsetid.AppSetIdManager.get(ArgumentMatchers.any()))
                    .thenReturn(appSetIdManager)
            } else {
                `when`(spyContext.getSystemService(
                    android.adservices.appsetid.AppSetIdManager::class.java))
                    .thenReturn(appSetIdManager)
            }
            return appSetIdManager
        }

        private fun setupResponse(appSetIdManager: android.adservices.appsetid.AppSetIdManager) {
            // Set up the response that AdIdManager will return when the compat code calls it.
            val appSetId = android.adservices.appsetid.AppSetId(
                "1234",
                android.adservices.appsetid.AppSetId.SCOPE_APP)
            val answer = { args: InvocationOnMock ->
                assertNotEquals(Looper.getMainLooper(), Looper.myLooper())
                val receiver = args.getArgument<
                    OutcomeReceiver<android.adservices.appsetid.AppSetId, Exception>>(1)
                receiver.onResult(appSetId)
                null
            }
            doAnswer(answer)
                .`when`(appSetIdManager).getAppSetId(
                    any(),
                    any()
                )
        }

        private fun verifyResponse(appSetId: AppSetId) {
            Assert.assertEquals("1234", appSetId.id)
            Assert.assertEquals(AppSetId.SCOPE_APP, appSetId.scope)
        }
    }
}
