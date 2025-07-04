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
import androidx.health.connect.client.impl.platform.request.PlatformUpsertMedicalResourceRequestBuilder
import androidx.health.connect.client.records.FhirVersion
import androidx.health.connect.client.request.UpsertMedicalResourceRequest
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
class UpsertMedicalResourceRequestTest {
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
            UpsertMedicalResourceRequest(
                MEDICAL_DATA_SOURCE_ID_STRING,
                FHIR_VERSION_4_0_1,
                DATA_JSON,
            )
        val request2 =
            UpsertMedicalResourceRequest(
                MEDICAL_DATA_SOURCE_ID_STRING,
                FHIR_VERSION_4_0_1,
                DATA_JSON,
            )
        val request3 =
            UpsertMedicalResourceRequest(
                MEDICAL_DATA_SOURCE_ID_STRING,
                FHIR_VERSION_4_3_0,
                DATA_JSON,
            )

        assertThat(request1).isEqualTo(request2)
        assertThat(request1).isNotEqualTo(request3)
        assertThat(request2).isNotEqualTo(request3)
    }

    @Test
    fun toString_expectCorrectString() {
        val upsertMedicalResourceRequest =
            UpsertMedicalResourceRequest(
                MEDICAL_DATA_SOURCE_ID_STRING,
                FHIR_VERSION_4_0_1,
                DATA_JSON,
            )

        val toString = upsertMedicalResourceRequest.toString()

        assertThat(toString).contains("dataSourceId=3008de9d-8c24-4591-b58c-43eaf30fa168")
        assertThat(toString).contains("data={}")
        assertThat(toString).contains("fhirVersion=FhirVersion")
        assertThat(toString).contains("(4.0.1)")
    }

    @SuppressLint("NewApi") // checked with feature availability check
    @Test
    fun toPlatformUpsertMedicalResourceRequest_expectCorrectConversion() {
        val sdk =
            UpsertMedicalResourceRequest(
                MEDICAL_DATA_SOURCE_ID_STRING,
                FHIR_VERSION_4_0_1,
                DATA_JSON,
            )

        assertThat(sdk.platformUpsertMedicalResourceRequest)
            .isEqualTo(
                PlatformUpsertMedicalResourceRequestBuilder(
                        MEDICAL_DATA_SOURCE_ID_STRING,
                        FHIR_VERSION_4_0_1.platformFhirVersion,
                        DATA_JSON,
                    )
                    .build()
            )
    }

    companion object {
        private const val MEDICAL_DATA_SOURCE_ID_STRING = "3008de9d-8c24-4591-b58c-43eaf30fa168"
        private val FHIR_VERSION_4_0_1 by lazy { FhirVersion.Companion.parseFhirVersion("4.0.1") }
        private val FHIR_VERSION_4_3_0 by lazy { FhirVersion.Companion.parseFhirVersion("4.3.0") }
        private const val DATA_JSON = "{}"
    }
}
