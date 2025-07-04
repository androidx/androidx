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

package androidx.health.connect.client.impl.platform.requests

import android.annotation.SuppressLint
import androidx.health.connect.client.feature.ExperimentalPersonalHealthRecordApi
import androidx.health.connect.client.feature.isPersonalHealthRecordFeatureAvailableInPlatform
import androidx.health.connect.client.impl.platform.request.PlatformReadMedicalResourcesPageRequestBuilder
import androidx.health.connect.client.request.ReadMedicalResourcesPageRequest
import androidx.health.connect.client.request.ReadMedicalResourcesRequest.Companion.DEFAULT_PAGE_SIZE
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalPersonalHealthRecordApi::class)
@RunWith(AndroidJUnit4::class)
class ReadMedicalResourcesPageRequestTest {

    @Before
    fun setup() {
        Assume.assumeTrue(
            "FEATURE_PERSONAL_HEALTH_RECORD is not available on this device!",
            isPersonalHealthRecordFeatureAvailableInPlatform(),
        )
    }

    @Test
    fun equalsTests() {
        val request1 = ReadMedicalResourcesPageRequest(PAGE_TOKEN, DEFAULT_PAGE_SIZE)
        val request2 = ReadMedicalResourcesPageRequest(PAGE_TOKEN, DEFAULT_PAGE_SIZE)
        // page size is not specified, DEFAULT_PAGE_SIZE should be used, hence it should
        // be equal to others in this group
        val request3 = ReadMedicalResourcesPageRequest(PAGE_TOKEN)
        val request4 = ReadMedicalResourcesPageRequest("$PAGE_TOKEN-diff", DEFAULT_PAGE_SIZE)

        assertThat(request1).isEqualTo(request2)
        assertThat(request1).isEqualTo(request3)
        assertThat(request2).isEqualTo(request3)

        assertThat(request1).isNotEqualTo(request4)
        assertThat(request2).isNotEqualTo(request4)
        assertThat(request3).isNotEqualTo(request4)
    }

    @Test
    fun toString_expectCorrectString() {
        val request = ReadMedicalResourcesPageRequest(PAGE_TOKEN, DEFAULT_PAGE_SIZE)

        val toString = request.toString()

        assertThat(toString).contains("pageToken=$PAGE_TOKEN")
        assertThat(toString).contains("pageSize=$DEFAULT_PAGE_SIZE")
    }

    @SuppressLint("NewApi") // checked with feature availability check
    @Test
    fun toPlatformRequest_expectCorrectConversion() {
        val sdkRequest = ReadMedicalResourcesPageRequest(PAGE_TOKEN, DEFAULT_PAGE_SIZE)

        assertThat(sdkRequest.platformReadMedicalResourcesRequest)
            .isEqualTo(
                PlatformReadMedicalResourcesPageRequestBuilder(PAGE_TOKEN)
                    .setPageSize(DEFAULT_PAGE_SIZE)
                    .build()
            )
    }

    companion object {
        private const val PAGE_TOKEN = "pageToken"
    }
}
