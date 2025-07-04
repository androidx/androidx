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
import android.net.Uri
import androidx.health.connect.client.feature.ExperimentalPersonalHealthRecordApi
import androidx.health.connect.client.feature.isPersonalHealthRecordFeatureAvailableInPlatform
import androidx.health.connect.client.records.FhirVersion
import androidx.health.connect.client.records.MedicalDataSource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalPersonalHealthRecordApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class MedicalDataSourceTest {

    @Before
    fun setup() {
        Assume.assumeTrue(
            "FEATURE_PERSONAL_HEALTH_RECORD is not available on this device!",
            isPersonalHealthRecordFeatureAvailableInPlatform(),
        )
    }

    @Test
    fun validMedicalDataSource_equals() {
        val medicalDataSource1 =
            MedicalDataSource(
                id = ID,
                packageName = PACKAGE_NAME,
                fhirBaseUri = FHIR_BASE_URI,
                displayName = DISPLAY_NAME,
                fhirVersion = FHIR_VERSION,
                lastDataUpdateTime = LAST_UPDATED_TIME,
            )
        val medicalDataSource2 =
            MedicalDataSource(
                id = ID,
                packageName = PACKAGE_NAME,
                fhirBaseUri = FHIR_BASE_URI,
                displayName = DISPLAY_NAME,
                fhirVersion = FHIR_VERSION,
                lastDataUpdateTime = LAST_UPDATED_TIME,
            )
        val medicalDataSource3 =
            MedicalDataSource(
                id = ID + "2",
                packageName = PACKAGE_NAME,
                fhirBaseUri = FHIR_BASE_URI,
                displayName = DISPLAY_NAME,
                fhirVersion = FHIR_VERSION,
                lastDataUpdateTime = LAST_UPDATED_TIME,
            )
        val medicalDataSource4 =
            MedicalDataSource(
                id = ID,
                packageName = PACKAGE_NAME + "two",
                fhirBaseUri = FHIR_BASE_URI,
                displayName = DISPLAY_NAME,
                fhirVersion = FHIR_VERSION,
                lastDataUpdateTime = LAST_UPDATED_TIME,
            )
        val medicalDataSource5 =
            MedicalDataSource(
                id = ID,
                packageName = PACKAGE_NAME,
                fhirBaseUri = FHIR_BASE_URI.buildUpon().appendPath("2/").build(),
                displayName = DISPLAY_NAME,
                fhirVersion = FHIR_VERSION,
                lastDataUpdateTime = LAST_UPDATED_TIME,
            )
        val medicalDataSource6 =
            MedicalDataSource(
                id = ID,
                packageName = PACKAGE_NAME,
                fhirBaseUri = FHIR_BASE_URI,
                displayName = "$DISPLAY_NAME Two",
                fhirVersion = FHIR_VERSION,
                lastDataUpdateTime = LAST_UPDATED_TIME,
            )
        val medicalDataSource7 =
            MedicalDataSource(
                id = ID,
                packageName = PACKAGE_NAME,
                fhirBaseUri = FHIR_BASE_URI,
                displayName = DISPLAY_NAME,
                fhirVersion = FhirVersion(4, 3, 0),
                lastDataUpdateTime = LAST_UPDATED_TIME,
            )
        val medicalDataSource8 =
            MedicalDataSource(
                id = ID,
                packageName = PACKAGE_NAME,
                fhirBaseUri = FHIR_BASE_URI,
                displayName = DISPLAY_NAME,
                fhirVersion = FHIR_VERSION,
                lastDataUpdateTime =
                    Instant.parse(LAST_DATA_UPDATE_TIMESTAMP.replace("2025-01-27", "2025-01-28")),
            )

        assertThat(medicalDataSource1).isEqualTo(medicalDataSource2)
        assertThat(medicalDataSource1).isNotEqualTo(medicalDataSource3)
        assertThat(medicalDataSource1).isNotEqualTo(medicalDataSource4)
        assertThat(medicalDataSource1).isNotEqualTo(medicalDataSource5)
        assertThat(medicalDataSource1).isNotEqualTo(medicalDataSource6)
        assertThat(medicalDataSource1).isNotEqualTo(medicalDataSource7)
        assertThat(medicalDataSource1).isNotEqualTo(medicalDataSource8)
        assertThat(medicalDataSource3).isNotEqualTo(medicalDataSource4)
        assertThat(medicalDataSource3).isNotEqualTo(medicalDataSource5)
    }

    @Test
    fun toString_expectCorrectString() {
        val medicalDataSource =
            MedicalDataSource(
                id = ID,
                packageName = PACKAGE_NAME,
                fhirBaseUri = FHIR_BASE_URI,
                displayName = DISPLAY_NAME,
                fhirVersion = FHIR_VERSION,
                lastDataUpdateTime = LAST_UPDATED_TIME,
            )

        val toString = medicalDataSource.toString()

        assertThat(toString).contains("id=testid")
        assertThat(toString).contains("packageName=androidx.health.connect.client")
        assertThat(toString).contains("fhirBaseUri=https://fhir.com/oauth/api/FHIR/R4/")
        assertThat(toString).contains("displayName=Test Data Source")
        assertThat(toString).contains("fhirVersion=FhirVersion")
        assertThat(toString).contains("(4.0.1)")
        assertThat(toString).contains("lastDataUpdateTime=$LAST_DATA_UPDATE_TIMESTAMP")
    }

    @SuppressLint("NewApi") // checked with feature availability check
    @Test
    fun toPlatformMedicalDataSource_expectCorrectConversion() {
        val medicalDataSource =
            MedicalDataSource(
                id = ID,
                packageName = PACKAGE_NAME,
                fhirBaseUri = FHIR_BASE_URI,
                displayName = DISPLAY_NAME,
                fhirVersion = FHIR_VERSION,
                lastDataUpdateTime = LAST_UPDATED_TIME,
            )

        val platformMedicalDataSource = medicalDataSource.platformMedicalDataSource

        assertThat(platformMedicalDataSource)
            .isEqualTo(
                PlatformMedicalDataSourceBuilder(
                        ID,
                        PACKAGE_NAME,
                        FHIR_BASE_URI,
                        DISPLAY_NAME,
                        FHIR_VERSION.platformFhirVersion,
                    )
                    .setLastDataUpdateTime(LAST_UPDATED_TIME)
                    .build()
            )
    }

    companion object {
        private const val ID = "testid"
        private const val PACKAGE_NAME = "androidx.health.connect.client"
        private const val DISPLAY_NAME = "Test Data Source"
        private const val LAST_DATA_UPDATE_TIMESTAMP = "2025-01-27T08:55:29.550677Z"
        private val FHIR_BASE_URI = Uri.parse("https://fhir.com/oauth/api/FHIR/R4/")
        private val LAST_UPDATED_TIME = Instant.parse(LAST_DATA_UPDATE_TIMESTAMP)
        private val FHIR_VERSION: FhirVersion by lazy { FhirVersion(4, 0, 1) }
    }
}
