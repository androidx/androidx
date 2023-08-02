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
import androidx.testutils.assertThrows
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class GetTopicsRequestTest {
    @Test
    fun testToString() {
        val result = "GetTopicsRequest: adsSdkName=sdk1, shouldRecordObservation=false"
        val request = GetTopicsRequest("sdk1", false)
        Truth.assertThat(request.toString()).isEqualTo(result)

        // Verify Builder.
        val request2 = GetTopicsRequest.Builder()
            .setAdsSdkName("sdk1")
            .setShouldRecordObservation(false)
            .build()
        Truth.assertThat(request.toString()).isEqualTo(result)

        // Verify equality.
        Truth.assertThat(request == request2).isTrue()
    }

    @Test
    fun testToString_emptySdkName() {
        val result = "GetTopicsRequest: adsSdkName=, shouldRecordObservation=true"
        val request = GetTopicsRequest("", true)
        Truth.assertThat(request.toString()).isEqualTo(result)

        // Verify Builder.
        val request2 = GetTopicsRequest.Builder()
            .setShouldRecordObservation(true)
            .build()
        Truth.assertThat(request.toString()).isEqualTo(result)

        // Verify equality.
        Truth.assertThat(request == request2).isTrue()
    }

    @Test
    fun testBuilder_setEmptyAdsSdkName_throwsError() {
        assertThrows(IllegalStateException::class.java) {
            GetTopicsRequest.Builder()
                .setAdsSdkName("")
                .setShouldRecordObservation(true)
                .build()
        }
    }
}
