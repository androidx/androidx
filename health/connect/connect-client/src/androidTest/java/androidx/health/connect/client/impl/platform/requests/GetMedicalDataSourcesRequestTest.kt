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
import androidx.health.connect.client.impl.platform.request.PlatformGetMedicalDataSourcesRequestBuilder
import androidx.health.connect.client.request.GetMedicalDataSourcesRequest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalPersonalHealthRecordApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class GetMedicalDataSourcesRequestTest {

    @Before
    fun setup() {
        Assume.assumeTrue(
            "FEATURE_PERSONAL_HEALTH_RECORD is not available on this device!",
            isPersonalHealthRecordFeatureAvailableInPlatform(),
        )
    }

    @Test
    fun validGetMedicalDataSourcesRequest_equalsAndHashCode() {
        val request1 = GetMedicalDataSourcesRequest(packageNames = TEST_PACKAGE_NAMES)
        val request2 = GetMedicalDataSourcesRequest(packageNames = TEST_PACKAGE_NAMES)
        val request3 =
            GetMedicalDataSourcesRequest(
                packageNames = TEST_PACKAGE_NAMES + (TEST_PACKAGE_NAMES.first() + "two")
            )

        assertThat(request1).isEqualTo(request2)
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode())
        assertThat(request1).isNotEqualTo(request3)
        assertThat(request1.hashCode()).isNotEqualTo(request3.hashCode())
        assertThat(request2).isNotEqualTo(request3)
    }

    @Test
    fun toString_expectCorrectString() {
        val getMedicalDataSourcesRequest =
            GetMedicalDataSourcesRequest(packageNames = TEST_PACKAGE_NAMES)

        val toString = getMedicalDataSourcesRequest.toString()

        assertThat(toString).contains("packageNames=[androidx.health.connect]")
    }

    @SuppressLint("NewApi") // checked with feature availability check
    @Test
    fun toPlatformGetMedicalDataSourcesRequest_expectCorrectConversion() {
        val getMedicalDataSourcesRequest =
            GetMedicalDataSourcesRequest(packageNames = TEST_PACKAGE_NAMES)

        val platformGetMedicalDataSourcesRequest =
            getMedicalDataSourcesRequest.platformGetMedicalDataSourcesRequest

        assertThat(platformGetMedicalDataSourcesRequest)
            .isEqualTo(
                PlatformGetMedicalDataSourcesRequestBuilder()
                    .apply { TEST_PACKAGE_NAMES.forEach { addPackageName(it) } }
                    .build()
            )
    }

    companion object {
        private val TEST_PACKAGE_NAMES = listOf("androidx.health.connect")
    }
}
