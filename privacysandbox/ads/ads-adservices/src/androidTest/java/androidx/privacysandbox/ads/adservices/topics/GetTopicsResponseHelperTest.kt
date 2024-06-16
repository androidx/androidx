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

package androidx.privacysandbox.ads.adservices.topics

import android.adservices.topics.EncryptedTopic
import android.adservices.topics.GetTopicsResponse
import android.adservices.topics.Topic
import android.annotation.SuppressLint
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.test.assertContains
import kotlin.test.assertEquals
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("NewApi")
@SmallTest
@RunWith(AndroidJUnit4::class)
@ExperimentalFeatures.Ext11OptIn
class GetTopicsResponseHelperTest {
    private val mValidAdServicesSdkExt4Version = AdServicesInfo.adServicesVersion() >= 4
    private val mValidAdServicesSdkExt11Version = AdServicesInfo.adServicesVersion() >= 11
    private val mValidAdExtServicesSdkExt9Version = AdServicesInfo.extServicesVersionS() >= 9
    private val mValidAdExtServicesSdkExt11Version = AdServicesInfo.extServicesVersionS() >= 11

    // Verify legacy tests with just plaintext topics
    @Suppress("DEPRECATION")
    @Test
    fun testResponse() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 4 or API 31/32 ext 9",
            mValidAdServicesSdkExt4Version || mValidAdExtServicesSdkExt9Version,
        )

        var topic1 = Topic(3, 7, 10023)
        var topic2 = Topic(3, 7, 10024)

        var response = GetTopicsResponse.Builder(listOf(topic1, topic2)).build()
        var convertedResponse = GetTopicsResponseHelper.convertResponse(response)

        assertEquals(2, convertedResponse.topics.size)
        assertContains(
            convertedResponse.topics,
            androidx.privacysandbox.ads.adservices.topics.Topic(3, 7, 10023),
        )
    }

    @Test
    fun testResponseWithEncryptedTopics() {
        Assume.assumeTrue(
            "minSdkVersion = API 33 ext 11 or API 31/32 ext 11",
            mValidAdServicesSdkExt11Version || mValidAdExtServicesSdkExt11Version,
        )

        var topic1 = Topic(3, 7, 10023)
        var topic2 = Topic(3, 7, 10024)
        var encryptedTopic1 =
            EncryptedTopic(
                "encryptedTopic".toByteArray(),
                "publicKey",
                "encapsulatedKey".toByteArray(),
            )

        var response =
            GetTopicsResponse.Builder(listOf(topic1, topic2), listOf(encryptedTopic1)).build()
        var convertedResponse = GetTopicsResponseHelper.convertResponseWithEncryptedTopics(response)

        assertEquals(2, convertedResponse.topics.size)
        assertEquals(1, convertedResponse.encryptedTopics.size)
        assertContains(
            convertedResponse.topics,
            androidx.privacysandbox.ads.adservices.topics.Topic(3, 7, 10023),
        )
        assertContains(
            convertedResponse.encryptedTopics,
            androidx.privacysandbox.ads.adservices.topics.EncryptedTopic(
                "encryptedTopic".toByteArray(),
                "publicKey",
                "encapsulatedKey".toByteArray(),
            ),
        )
    }
}
