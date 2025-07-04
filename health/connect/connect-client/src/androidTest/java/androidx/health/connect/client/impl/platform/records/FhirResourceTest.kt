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
class FhirResourceTest {

    @Before
    fun setup() {
        Assume.assumeTrue(
            "FEATURE_PERSONAL_HEALTH_RECORD is not available on this device!",
            isPersonalHealthRecordFeatureAvailableInPlatform(),
        )
    }

    @Test
    fun validFhirResource_equalsAndHashCode() {
        val fhirResource1 = FhirResource(type = FHIR_RESOURCE_TYPE_PATIENT, id = "id1", data = "{}")
        val fhirResource2 = FhirResource(type = FHIR_RESOURCE_TYPE_PATIENT, id = "id1", data = "{}")
        val fhirResource3 = FhirResource(type = FHIR_RESOURCE_TYPE_PATIENT, id = "id2", data = "{}")

        assertThat(fhirResource1).isEqualTo(fhirResource2)
        assertThat(fhirResource1).isNotEqualTo(fhirResource3)
    }

    @Test
    fun toString_expectCorrectString() {
        val fhirResource = FhirResource(type = FHIR_RESOURCE_TYPE_PATIENT, id = "id1", data = "{}")

        val toString = fhirResource.toString()

        assertThat(toString).contains("(type=9, id=id1, data={})")
    }

    @SuppressLint("NewApi") // checked with feature availability check
    @Test
    fun toPlatformFhirResource_expectCorrectConversion() {
        val sdk = FhirResource(type = FHIR_RESOURCE_TYPE_PATIENT, id = "id1", data = "{}")

        assertThat(sdk.platformFhirResource)
            .isEqualTo(
                PlatformFhirResourceBuilder(
                        PlatformFhirResource.FHIR_RESOURCE_TYPE_PATIENT,
                        "id1",
                        "{}",
                    )
                    .build()
            )
    }
}
