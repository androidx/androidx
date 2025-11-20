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
import androidx.health.connect.client.records.FhirResource
import androidx.health.connect.client.records.FhirResource.Companion.FHIR_RESOURCE_TYPE_PATIENT
import androidx.health.connect.client.records.FhirVersion
import androidx.health.connect.client.records.MedicalResource
import androidx.health.connect.client.records.MedicalResource.Companion.MEDICAL_RESOURCE_TYPE_MEDICATIONS
import androidx.health.connect.client.records.MedicalResourceId
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
class MedicalResourceTest {
    @Before
    fun setup() {
        Assume.assumeTrue(
            "FEATURE_PERSONAL_HEALTH_RECORD is not available on this device!",
            isPersonalHealthRecordFeatureAvailableInPlatform(),
        )
    }

    @Test
    fun validMedicalResource_equals() {
        val medicalResource1 =
            MedicalResource(
                MEDICAL_RESOURCE_TYPE_MEDICATIONS,
                MEDICAL_RESOURCE_ID,
                MEDICAL_DATA_SOURCE_ID_STRING,
                FHIR_VERSION_4_0_1,
                FHIR_RESOURCE_EMPTY,
            )
        val medicalResource2 =
            MedicalResource(
                MEDICAL_RESOURCE_TYPE_MEDICATIONS,
                MEDICAL_RESOURCE_ID,
                MEDICAL_DATA_SOURCE_ID_STRING,
                FHIR_VERSION_4_0_1,
                FHIR_RESOURCE_EMPTY,
            )
        val medicalResource3 =
            MedicalResource(
                MEDICAL_RESOURCE_TYPE_MEDICATIONS,
                MEDICAL_RESOURCE_ID,
                MEDICAL_DATA_SOURCE_ID_STRING,
                FhirVersion.parseFhirVersion("4.3.0"),
                FHIR_RESOURCE_EMPTY,
            )

        assertThat(medicalResource1).isEqualTo(medicalResource2)
        assertThat(medicalResource1).isNotEqualTo(medicalResource3)
    }

    @Test
    fun toString_expectCorrectString() {
        val medicalResource =
            MedicalResource(
                MEDICAL_RESOURCE_TYPE_MEDICATIONS,
                MEDICAL_RESOURCE_ID,
                MEDICAL_DATA_SOURCE_ID_STRING,
                FHIR_VERSION_4_0_1,
                FHIR_RESOURCE_EMPTY,
            )

        val toString = medicalResource.toString()

        assertThat(toString).contains("type=9")
        assertThat(toString).contains("dataSourceId=3008de9d-8c24-4591-b58c-43eaf30fa168")
        assertThat(toString).contains("fhirVersion=FhirVersion")
        assertThat(toString).contains("(4.0.1)")
        assertThat(toString).contains("fhirResource=FhirResource")
        assertThat(toString).contains("(type=9, id=id1, data={})")
    }

    @SuppressLint("NewApi") // checked with feature availability check
    @Test
    fun toPlatformMedicalResource_expectCorrectConversion() {
        val sdk =
            MedicalResource(
                MEDICAL_RESOURCE_TYPE_MEDICATIONS,
                MEDICAL_RESOURCE_ID,
                MEDICAL_DATA_SOURCE_ID_STRING,
                FHIR_VERSION_4_0_1,
                FHIR_RESOURCE_EMPTY,
            )

        val platform = sdk.platformMedicalResource

        assertThat(platform)
            .isEqualTo(
                PlatformMedicalResourceBuilder(
                        PlatformMedicalResource.MEDICAL_RESOURCE_TYPE_MEDICATIONS,
                        MEDICAL_DATA_SOURCE_ID_STRING,
                        FHIR_VERSION_4_0_1.platformFhirVersion,
                        FHIR_RESOURCE_EMPTY.platformFhirResource,
                    )
                    .build()
            )
    }

    companion object {
        private const val MEDICAL_DATA_SOURCE_ID_STRING = "3008de9d-8c24-4591-b58c-43eaf30fa168"
        private val MEDICAL_RESOURCE_ID by lazy {
            // lazy is needed, otherwise a MedicalResourceId would be constructed before the
            // assumeTrue in setUp() is called. Same goes for other fields in this companion object.
            MedicalResourceId(
                MEDICAL_DATA_SOURCE_ID_STRING,
                FHIR_RESOURCE_TYPE_PATIENT,
                "fhir_rs_id",
            )
        }
        private val FHIR_VERSION_4_0_1 by lazy { FhirVersion.Companion.parseFhirVersion("4.0.1") }
        private val FHIR_RESOURCE_EMPTY by lazy {
            FhirResource(FHIR_RESOURCE_TYPE_PATIENT, "id1", "{}")
        }
    }
}
