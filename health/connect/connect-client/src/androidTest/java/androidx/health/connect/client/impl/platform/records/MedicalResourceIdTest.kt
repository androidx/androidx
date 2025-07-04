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

package androidx.health.connect.client.impl.platform.records

import android.annotation.SuppressLint
import androidx.health.connect.client.feature.ExperimentalPersonalHealthRecordApi
import androidx.health.connect.client.feature.isPersonalHealthRecordFeatureAvailableInPlatform
import androidx.health.connect.client.records.FhirResource.Companion.FHIR_RESOURCE_TYPE_IMMUNIZATION
import androidx.health.connect.client.records.FhirResource.Companion.FHIR_RESOURCE_TYPE_PATIENT
import androidx.health.connect.client.records.MedicalResourceId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalPersonalHealthRecordApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class MedicalResourceIdTest {
    @Before
    fun setup() {
        Assume.assumeTrue(
            "FEATURE_PERSONAL_HEALTH_RECORD is not available on this device!",
            isPersonalHealthRecordFeatureAvailableInPlatform(),
        )
    }

    @Test
    fun validMedicalResourceId_equals() {
        val medicalResourceId1 =
            MedicalResourceId(
                MEDICAL_DATA_SOURCE_ID_STRING,
                FHIR_RESOURCE_TYPE_PATIENT,
                "fhir_rs_id_1",
            )
        val medicalResourceId2 =
            MedicalResourceId(
                MEDICAL_DATA_SOURCE_ID_STRING,
                FHIR_RESOURCE_TYPE_PATIENT,
                "fhir_rs_id_1",
            )
        val medicalResourceId3 =
            MedicalResourceId(
                MEDICAL_DATA_SOURCE_ID_STRING,
                FHIR_RESOURCE_TYPE_PATIENT,
                "fhir_rs_id_2",
            )

        assertThat(medicalResourceId1).isEqualTo(medicalResourceId2)
        assertThat(medicalResourceId1).isNotEqualTo(medicalResourceId3)
        assertThat(medicalResourceId2).isNotEqualTo(medicalResourceId3)
    }

    @Test
    fun toString_expectCorrectString() {
        val medicalResourceId =
            MedicalResourceId(
                MEDICAL_DATA_SOURCE_ID_STRING,
                FHIR_RESOURCE_TYPE_PATIENT,
                "fhir_rs_id_1",
            )

        val toString = medicalResourceId.toString()

        assertThat(toString)
            .contains(
                "(dataSourceId=3008de9d-8c24-4591-b58c-43eaf30fa168, fhirResourceType=9, fhirResourceId=fhir_rs_id_1)"
            )
    }

    @SuppressLint("NewApi") // checked with feature availability check
    @Test
    fun toPlatformMedicalResourceId_expectCorrectConversion() {
        val sdk =
            MedicalResourceId(
                MEDICAL_DATA_SOURCE_ID_STRING,
                FHIR_RESOURCE_TYPE_PATIENT,
                "fhir_rs_id_1",
            )

        assertThat(sdk.platformMedicalResourceId)
            .isEqualTo(
                PlatformMedicalResourceId(
                    MEDICAL_DATA_SOURCE_ID_STRING,
                    FHIR_RESOURCE_TYPE_PATIENT,
                    "fhir_rs_id_1",
                )
            )
    }

    @Test
    fun fromFhirReference_validReference() {
        val medicalResourceId =
            MedicalResourceId.fromFhirReference(
                MEDICAL_DATA_SOURCE_ID_STRING,
                "Immunization/034-AB16.0",
            )

        assertThat(medicalResourceId)
            .isEqualTo(
                MedicalResourceId(
                    MEDICAL_DATA_SOURCE_ID_STRING,
                    FHIR_RESOURCE_TYPE_IMMUNIZATION,
                    "034-AB16.0",
                )
            )
    }

    companion object {
        private const val MEDICAL_DATA_SOURCE_ID_STRING = "3008de9d-8c24-4591-b58c-43eaf30fa168"
    }
}
