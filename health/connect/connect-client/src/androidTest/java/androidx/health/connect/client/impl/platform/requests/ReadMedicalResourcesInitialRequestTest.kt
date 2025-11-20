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
import androidx.health.connect.client.impl.platform.request.PlatformReadMedicalResourcesInitialRequestBuilder
import androidx.health.connect.client.records.MedicalResource.Companion.MEDICAL_RESOURCE_TYPE_CONDITIONS
import androidx.health.connect.client.records.MedicalResource.Companion.MEDICAL_RESOURCE_TYPE_VITAL_SIGNS
import androidx.health.connect.client.request.ReadMedicalResourcesInitialRequest
import androidx.health.connect.client.request.ReadMedicalResourcesRequest.Companion.DEFAULT_PAGE_SIZE
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalPersonalHealthRecordApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class ReadMedicalResourcesInitialRequestTest {
    @Before
    fun setup() {
        Assume.assumeTrue(
            "FEATURE_PERSONAL_HEALTH_RECORD is not available on this device!",
            isPersonalHealthRecordFeatureAvailableInPlatform(),
        )
    }

    @Test
    fun equalsTests() {
        val request1 =
            ReadMedicalResourcesInitialRequest(
                MEDICAL_RESOURCE_TYPE_CONDITIONS,
                MEDICAL_DATA_SOURCE_IDS_1,
                DEFAULT_PAGE_SIZE,
            )
        val request2 =
            ReadMedicalResourcesInitialRequest(
                MEDICAL_RESOURCE_TYPE_CONDITIONS,
                MEDICAL_DATA_SOURCE_IDS_1,
                DEFAULT_PAGE_SIZE,
            )
        val request3 =
            ReadMedicalResourcesInitialRequest(
                MEDICAL_RESOURCE_TYPE_CONDITIONS,
                MEDICAL_DATA_SOURCE_IDS_1,
                // page size is not specified, DEFAULT_PAGE_SIZE should be used, hence it should
                // be equal to others in this group
            )
        val request4 =
            ReadMedicalResourcesInitialRequest(
                MEDICAL_RESOURCE_TYPE_VITAL_SIGNS, // different type
                MEDICAL_DATA_SOURCE_IDS_1,
                DEFAULT_PAGE_SIZE,
            )
        val request5 =
            ReadMedicalResourcesInitialRequest(
                MEDICAL_RESOURCE_TYPE_CONDITIONS,
                MEDICAL_DATA_SOURCE_IDS_2, // different data source
                DEFAULT_PAGE_SIZE,
            )
        val request6 =
            ReadMedicalResourcesInitialRequest(
                MEDICAL_RESOURCE_TYPE_CONDITIONS,
                MEDICAL_DATA_SOURCE_IDS_1,
                DEFAULT_PAGE_SIZE + 1, // different page size
            )

        assertThat(request1).isEqualTo(request2)
        assertThat(request1).isEqualTo(request3)
        assertThat(request2).isEqualTo(request3)

        assertThat(request1).isNotEqualTo(request4)
        assertThat(request2).isNotEqualTo(request4)
        assertThat(request3).isNotEqualTo(request4)

        assertThat(request1).isNotEqualTo(request5)
        assertThat(request2).isNotEqualTo(request5)
        assertThat(request3).isNotEqualTo(request5)

        assertThat(request1).isNotEqualTo(request6)
        assertThat(request2).isNotEqualTo(request6)
        assertThat(request3).isNotEqualTo(request6)
    }

    @Test
    fun invalidMedicalResourceType_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            ReadMedicalResourcesInitialRequest(-1, MEDICAL_DATA_SOURCE_IDS_1, DEFAULT_PAGE_SIZE)
        }
    }

    @Test
    fun toString_expectCorrectString() {
        val request =
            ReadMedicalResourcesInitialRequest(
                MEDICAL_RESOURCE_TYPE_CONDITIONS,
                MEDICAL_DATA_SOURCE_IDS_1,
                DEFAULT_PAGE_SIZE,
            )

        val toString = request.toString()

        assertThat(toString).contains("medicalResourceType=$MEDICAL_RESOURCE_TYPE_CONDITIONS")
        assertThat(toString).contains("medicalDataSourceIds=$MEDICAL_DATA_SOURCE_IDS_1")
        assertThat(toString).contains("pageSize=$DEFAULT_PAGE_SIZE")
    }

    @SuppressLint("NewApi") // checked with feature availability check
    @Test
    fun toPlatformRequest_expectCorrectConversion() {
        val sdkRequest =
            ReadMedicalResourcesInitialRequest(
                MEDICAL_RESOURCE_TYPE_CONDITIONS,
                MEDICAL_DATA_SOURCE_IDS_1,
                DEFAULT_PAGE_SIZE,
            )

        assertThat(sdkRequest.platformReadMedicalResourcesRequest)
            .isEqualTo(
                PlatformReadMedicalResourcesInitialRequestBuilder(
                        android.health.connect.datatypes.MedicalResource
                            .MEDICAL_RESOURCE_TYPE_CONDITIONS
                    )
                    .addDataSourceIds(MEDICAL_DATA_SOURCE_IDS_1)
                    .setPageSize(DEFAULT_PAGE_SIZE)
                    .build()
            )
    }

    companion object {
        private val MEDICAL_DATA_SOURCE_IDS_1 = setOf("3008de9d-8c24-4591-b58c-43eaf30fa168")
        private val MEDICAL_DATA_SOURCE_IDS_2 = setOf("ca761232-ed42-11ce-bacd-00aa0057b223")
    }
}
