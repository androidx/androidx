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

package androidx.privacysandbox.ads.adservices.java.topics

import android.adservices.topics.Topic
import android.adservices.topics.TopicsManager
import android.content.Context
import android.os.OutcomeReceiver
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.privacysandbox.ads.adservices.java.topics.TopicsManagerFutures.Companion.from
import androidx.privacysandbox.ads.adservices.topics.GetTopicsRequest
import androidx.privacysandbox.ads.adservices.topics.GetTopicsResponse
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import org.junit.Assert
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

@SmallTest
@SuppressWarnings("NewApi")
@RunWith(AndroidJUnit4::class)
class TopicsManagerFuturesTest {

    @Before
    fun setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext<Context>())
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33)
    fun testTopicsOlderVersions() {
        val sdkExtVersion = SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES)

        Assume.assumeTrue("maxSdkVersion = API 33 ext 3", sdkExtVersion < 4)
        assertThat(from(mContext)).isEqualTo(null)
    }

    @Test
    @SuppressWarnings("NewApi")
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    fun testTopicsAsync() {
        val sdkExtVersion = SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES)

        Assume.assumeTrue("minSdkVersion = API 33 ext 4", sdkExtVersion >= 4)
        val topicsManager = mockTopicsManager(mContext)
        setupTopicsResponse(topicsManager)
        val managerCompat = from(mContext)

        // Actually invoke the compat code.
        val request =
            GetTopicsRequest.Builder().setAdsSdkName(mSdkName).setShouldRecordObservation(true)
                .build()

        val result: ListenableFuture<GetTopicsResponse> = managerCompat!!.getTopicsAsync(request)

        // Verify that the result of the compat call is correct.
        verifyResponse(result.get())

        // Verify that the compat code was invoked correctly.
        val captor = ArgumentCaptor.forClass(android.adservices.topics.GetTopicsRequest::class.java)
        verify(topicsManager).getTopics(captor.capture(), any(), any())

        // Verify that the request that the compat code makes to the platform is correct.
        verifyRequest(captor.value)
    }

    @Test
    @SuppressWarnings("NewApi")
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
    fun testTopicsAsyncPreviewSupported() {
        val sdkExtVersion = SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES)

        Assume.assumeTrue("minSdkVersion = API 33 ext 5", sdkExtVersion >= 5)
        val topicsManager = mockTopicsManager(mContext)
        setupTopicsResponse(topicsManager)
        val managerCompat = from(mContext)

        // Actually invoke the compat Preview API code.
        val request =
            GetTopicsRequest.Builder().setAdsSdkName(mSdkName).setShouldRecordObservation(false)
                .build()

        val result: ListenableFuture<GetTopicsResponse> = managerCompat!!.getTopicsAsync(request)

        // Verify that the result of the compat call is correct.
        verifyResponse(result.get())

        // Verify that the compat code was invoked correctly.
        val captor = ArgumentCaptor.forClass(android.adservices.topics.GetTopicsRequest::class.java)
        verify(topicsManager).getTopics(captor.capture(), any(), any())

        // Verify that the request that the compat code makes to the platform is correct.
        verifyRequestPreviewApi(captor.value)
    }

    companion object {
        private lateinit var mContext: Context
        private val mSdkName: String = "sdk1"

        @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
        private fun mockTopicsManager(spyContext: Context): TopicsManager {
            val topicsManager = mock(TopicsManager::class.java)
            `when`(spyContext.getSystemService(TopicsManager::class.java)).thenReturn(topicsManager)
            return topicsManager
        }

        @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
        private fun setupTopicsResponse(topicsManager: TopicsManager) {
            // Set up the response that TopicsManager will return when the compat code calls it.
            val topic1 = Topic(1, 1, 1)
            val topic2 = Topic(2, 2, 2)
            val topics = listOf(topic1, topic2)
            val response = android.adservices.topics.GetTopicsResponse.Builder(topics).build()
            val answer = { args: InvocationOnMock ->
                val receiver = args.getArgument<
                    OutcomeReceiver<android.adservices.topics.GetTopicsResponse, Exception>>(2)
                receiver.onResult(response)
                null
            }
            doAnswer(answer).`when`(topicsManager).getTopics(
                any(), any(), any()
            )
        }

        @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
        private fun verifyRequest(topicsRequest: android.adservices.topics.GetTopicsRequest) {
            // Set up the request that we expect the compat code to invoke.
            val expectedRequest =
                android.adservices.topics.GetTopicsRequest.Builder().setAdsSdkName(mSdkName).build()

            Assert.assertEquals(expectedRequest.adsSdkName, topicsRequest.adsSdkName)
        }

        @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
        private fun verifyRequestPreviewApi(
            topicsRequest: android.adservices.topics.GetTopicsRequest
        ) {
            // Set up the request that we expect the compat code to invoke.
            val expectedRequest =
                android.adservices.topics.GetTopicsRequest.Builder().setAdsSdkName(mSdkName)
                    .setShouldRecordObservation(false).build()

            Assert.assertEquals(expectedRequest.adsSdkName, topicsRequest.adsSdkName)
            Assert.assertEquals(
                expectedRequest.shouldRecordObservation(), topicsRequest.shouldRecordObservation()
            )
        }

        @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
        private fun verifyResponse(getTopicsResponse: GetTopicsResponse) {
            Assert.assertEquals(2, getTopicsResponse.topics.size)
            val topic1 = getTopicsResponse.topics[0]
            val topic2 = getTopicsResponse.topics[1]
            Assert.assertEquals(1, topic1.topicId)
            Assert.assertEquals(1, topic1.modelVersion)
            Assert.assertEquals(1, topic1.taxonomyVersion)
            Assert.assertEquals(2, topic2.topicId)
            Assert.assertEquals(2, topic2.modelVersion)
            Assert.assertEquals(2, topic2.taxonomyVersion)
        }
    }
}