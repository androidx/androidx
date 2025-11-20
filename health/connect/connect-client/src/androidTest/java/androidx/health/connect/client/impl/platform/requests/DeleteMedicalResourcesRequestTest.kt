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
import androidx.health.connect.client.impl.platform.request.PlatformDeleteMedicalResourcesRequestBuilder
import androidx.health.connect.client.records.MedicalResource.Companion.MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES
import androidx.health.connect.client.records.MedicalResource.Companion.MEDICAL_RESOURCE_TYPE_CONDITIONS
import androidx.health.connect.client.request.DeleteMedicalResourcesRequest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalPersonalHealthRecordApi::class)
@RunWith(AndroidJUnit4::class)
class DeleteMedicalResourcesRequestTest {

    @Before
    fun setup() {
        Assume.assumeTrue(
            "FEATURE_PERSONAL_HEALTH_RECORD is not available on this device!",
            isPersonalHealthRecordFeatureAvailableInPlatform(),
        )
    }

    @Test
    fun validDeleteMedicalResourcesRequest_equals() {
        val request1 =
            DeleteMedicalResourcesRequest(
                dataSourceIds = TEST_DATA_SOURCE_IDS,
                medicalResourceTypes = TEST_MEDICAL_RESOURCE_TYPES,
            )
        val request2 =
            DeleteMedicalResourcesRequest(
                dataSourceIds = TEST_DATA_SOURCE_IDS,
                medicalResourceTypes = TEST_MEDICAL_RESOURCE_TYPES,
            )
        val request3 = DeleteMedicalResourcesRequest(dataSourceIds = TEST_DATA_SOURCE_IDS_2)
        val request4 =
            DeleteMedicalResourcesRequest(
                medicalResourceTypes =
                    TEST_MEDICAL_RESOURCE_TYPES + MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES
            )
        val request5 =
            DeleteMedicalResourcesRequest(
                medicalResourceTypes = setOf(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
            )

        assertThat(request1).isEqualTo(request2)
        assertThat(request1).isNotEqualTo(request3)
        assertThat(request1).isNotEqualTo(request4)
        assertThat(request1).isNotEqualTo(request5)
        assertThat(request3).isNotEqualTo(request4)
        assertThat(request3).isNotEqualTo(request5)
        assertThat(request4).isNotEqualTo(request5)
        assertThat(request2).isNotEqualTo(request3)
        assertThat(request2).isNotEqualTo(request4)
        assertThat(request2).isNotEqualTo(request5)
    }

    @Test
    fun toString_expectCorrectString() {
        val deleteMedicalResourcesRequest =
            DeleteMedicalResourcesRequest(
                dataSourceIds = TEST_DATA_SOURCE_IDS,
                medicalResourceTypes = TEST_MEDICAL_RESOURCE_TYPES,
            )

        val toString = deleteMedicalResourcesRequest.toString()

        assertThat(toString).contains("dataSourceIds=$TEST_DATA_SOURCE_IDS")
        assertThat(toString).contains("medicalResourceTypes=$TEST_MEDICAL_RESOURCE_TYPES")
    }

    @Test
    fun invalidRequest_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            DeleteMedicalResourcesRequest(emptySet(), emptySet())
        }
    }

    @SuppressLint("NewApi") // checked with feature availability check
    @Test
    fun toPlatformDeleteMedicalResourcesRequest_expectCorrectConversion() {
        val deleteMedicalResourcesRequest =
            DeleteMedicalResourcesRequest(
                dataSourceIds = TEST_DATA_SOURCE_IDS,
                medicalResourceTypes = TEST_MEDICAL_RESOURCE_TYPES,
            )

        val platformDeleteMedicalResourcesRequest =
            deleteMedicalResourcesRequest.platformReadMedicalResourcesRequest

        assertThat(platformDeleteMedicalResourcesRequest)
            .isEqualTo(
                PlatformDeleteMedicalResourcesRequestBuilder()
                    .apply { TEST_DATA_SOURCE_IDS.forEach { addDataSourceId(it) } }
                    .apply { TEST_MEDICAL_RESOURCE_TYPES.forEach { addMedicalResourceType(it) } }
                    .build()
            )
    }

    companion object {
        private val TEST_DATA_SOURCE_IDS = setOf("3008de9d-8c24-4591-b58c-43eaf30fa168")
        private val TEST_DATA_SOURCE_IDS_2 = setOf("ca761232-ed42-11ce-bacd-00aa0057b223")
        private val TEST_MEDICAL_RESOURCE_TYPES = setOf(MEDICAL_RESOURCE_TYPE_CONDITIONS)
    }
}
