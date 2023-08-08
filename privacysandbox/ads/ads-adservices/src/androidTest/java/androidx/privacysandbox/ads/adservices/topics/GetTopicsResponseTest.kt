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

package androidx.privacysandbox.ads.adservices.topics

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class GetTopicsResponseTest {
    @Test
    fun testToString() {
        val topicsString = "Topic { TaxonomyVersion=1, ModelVersion=10, TopicCode=100 }, " +
            "Topic { TaxonomyVersion=2, ModelVersion=20, TopicCode=200 }"
        val result = "Topics=[$topicsString]"

        val topic1 = Topic(1, 10, 100)
        var topic2 = Topic(2, 20, 200)
        val response1 = GetTopicsResponse(listOf(topic1, topic2))
        Truth.assertThat(response1.toString()).isEqualTo(result)
    }

    @Test
    fun testEquals() {
        val topic1 = Topic(1, 10, 100)
        var topic2 = Topic(2, 20, 200)
        val response1 = GetTopicsResponse(listOf(topic1, topic2))
        val response2 = GetTopicsResponse(listOf(topic2, topic1))
        Truth.assertThat(response1 == response2).isTrue()
    }
}
