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
@file:OptIn(ExperimentalPersonalHealthRecordApi::class)

import androidx.health.connect.client.feature.ExperimentalPersonalHealthRecordApi
import androidx.health.connect.client.feature.isPersonalHealthRecordFeatureAvailableInPlatform
import androidx.health.connect.client.records.FhirResource
import androidx.health.connect.client.records.FhirResource.Companion.FHIR_RESOURCE_TYPE_IMMUNIZATION
import androidx.health.connect.client.records.FhirVersion
import androidx.health.connect.client.records.MedicalResource
import androidx.health.connect.client.records.MedicalResource.Companion.MEDICAL_RESOURCE_TYPE_VACCINES
import androidx.health.connect.client.records.MedicalResourceId
import androidx.health.connect.client.response.ReadMedicalResourcesResponse
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ReadMedicalResourcesResponseTest {
    @Before
    fun setup() {
        Assume.assumeTrue(
            "FEATURE_PERSONAL_HEALTH_RECORD is not available on this device!",
            isPersonalHealthRecordFeatureAvailableInPlatform(),
        )
    }

    @Test
    fun equalsTests() {
        val medicalResources = listOf(MEDICAL_RESOURCE)
        val response1 =
            ReadMedicalResourcesResponse(listOf(MEDICAL_RESOURCE), NEXT_PAGE_TOKEN, REMAINING_COUNT)
        val response2 =
            ReadMedicalResourcesResponse(listOf(MEDICAL_RESOURCE), NEXT_PAGE_TOKEN, REMAINING_COUNT)
        val response3 =
            ReadMedicalResourcesResponse(
                listOf(MEDICAL_RESOURCE, MEDICAL_RESOURCE),
                NEXT_PAGE_TOKEN,
                REMAINING_COUNT,
            )
        val response4 =
            ReadMedicalResourcesResponse(
                listOf(MEDICAL_RESOURCE),
                NEXT_PAGE_TOKEN + "diff",
                REMAINING_COUNT,
            )
        val response5 =
            ReadMedicalResourcesResponse(medicalResources, NEXT_PAGE_TOKEN, REMAINING_COUNT + 1)

        assertThat(response1).isEqualTo(response2)
        assertThat(response1).isNotEqualTo(response3)
        assertThat(response1).isNotEqualTo(response4)
        assertThat(response1).isNotEqualTo(response5)
        assertThat(response3).isNotEqualTo(response4)
        assertThat(response3).isNotEqualTo(response5)
        assertThat(response4).isNotEqualTo(response5)
    }

    @Test
    fun toString_expectCorrectString() {
        val medicalResources = listOf(MEDICAL_RESOURCE)
        val response =
            ReadMedicalResourcesResponse(medicalResources, NEXT_PAGE_TOKEN, REMAINING_COUNT)

        val toString = response.toString()

        assertThat(toString).contains("ReadMedicalResourcesResponse")
        assertThat(toString).contains("medicalResources=$medicalResources")
        assertThat(toString).contains("nextPageToken=$NEXT_PAGE_TOKEN")
        assertThat(toString).contains("remainingCount=$REMAINING_COUNT")
    }

    companion object {
        private const val MEDICAL_DATA_SOURCE_ID_STRING = "3008de9d-8c24-4591-b58c-43eaf30fa168"
        private val MEDICAL_RESOURCE_ID by lazy {
            // lazy is needed, otherwise a MedicalResourceId would be constructed before the
            // assumeTrue in setUp() is called. Same goes for other fields in this companion object.
            MedicalResourceId(
                MEDICAL_DATA_SOURCE_ID_STRING,
                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                "fhir_rs_id",
            )
        }
        private val FHIR_VERSION_4_0_1 by lazy { FhirVersion.Companion.parseFhirVersion("4.0.1") }
        private val FHIR_RESOURCE_EMPTY by lazy {
            FhirResource(FHIR_RESOURCE_TYPE_IMMUNIZATION, MEDICAL_RESOURCE_ID.fhirResourceId, "{}")
        }
        private val MEDICAL_RESOURCE by lazy {
            MedicalResource(
                MEDICAL_RESOURCE_TYPE_VACCINES,
                MEDICAL_RESOURCE_ID,
                MEDICAL_DATA_SOURCE_ID_STRING,
                FHIR_VERSION_4_0_1,
                FHIR_RESOURCE_EMPTY,
            )
        }
        private const val NEXT_PAGE_TOKEN = "nextPageToken"
        private const val REMAINING_COUNT = 1000
    }
}
