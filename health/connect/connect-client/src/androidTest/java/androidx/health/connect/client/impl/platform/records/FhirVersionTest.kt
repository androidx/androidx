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
import androidx.health.connect.client.records.FhirVersion
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalPersonalHealthRecordApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class FhirVersionTest {
    @Before
    fun setup() {
        Assume.assumeTrue(
            "FEATURE_PERSONAL_HEALTH_RECORD is not available on this device!",
            isPersonalHealthRecordFeatureAvailableInPlatform(),
        )
    }

    @Test
    fun compareTo_sameVersion_returnsZero() {
        val version1 = FhirVersion(1, 0, 0)
        val version2 = FhirVersion(1, 0, 0)

        assertThat(version1.compareTo(version2)).isEqualTo(0)
        assertThat(version2.compareTo(version1)).isEqualTo(0)
    }

    @Test
    fun compareTo_differentMajor_returnsCorrectValue() {
        val version1 = FhirVersion(2, 0, 0)
        val version2 = FhirVersion(1, 0, 0)

        assertThat(version1.compareTo(version2)).isGreaterThan(0)
        assertThat(version2.compareTo(version1)).isLessThan(0)
    }

    @Test
    fun compareTo_differentMinor_returnsCorrectValue() {
        val version1 = FhirVersion(1, 1, 0)
        val version2 = FhirVersion(1, 0, 0)

        assertThat(version1.compareTo(version2)).isGreaterThan(0)
        assertThat(version2.compareTo(version1)).isLessThan(0)
    }

    @Test
    fun compareTo_differentPatch_returnsCorrectValue() {
        val version1 = FhirVersion(1, 0, 1)
        val version2 = FhirVersion(1, 0, 0)

        assertThat(version1.compareTo(version2)).isGreaterThan(0)
        assertThat(version2.compareTo(version1)).isLessThan(0)
    }

    @Test
    fun validFhirVersion_equals() {
        assertThat(FhirVersion(major = 1, minor = 2, patch = 3))
            .isEqualTo(FhirVersion(major = 1, minor = 2, patch = 3))
        assertThat(FhirVersion(major = 1, minor = 2, patch = 3))
            .isNotEqualTo(FhirVersion(major = 1, minor = 2, patch = 5))
    }

    @Test
    fun toString_expectCorrectString() {
        val fhirVersion = FhirVersion(major = 1, minor = 2, patch = 3)

        val fhirVersionString = fhirVersion.toString()

        assertThat(fhirVersionString).contains("(1.2.3)")
    }

    @Test
    fun parseFhirVersionString_invalidInput_expectError() {
        val input = "1.1.a"

        assertThrows(IllegalArgumentException::class.java) { FhirVersion.parseFhirVersion(input) }
    }

    @Test
    fun parseFhirVersionString_validInput_expectCorrectFhirVersion() {
        val input = "1.1.10"

        val fhirVersion = FhirVersion.parseFhirVersion(input)

        assertThat(fhirVersion).isEqualTo(FhirVersion(major = 1, minor = 1, patch = 10))
    }

    @SuppressLint("NewApi") // checked with feature availability check
    @Test
    fun toPlatformFhirVersion_expectCorrectConversion() {
        val sdk = FhirVersion(major = 1, minor = 2, patch = 3)

        assertThat(sdk.platformFhirVersion).isEqualTo(PlatformFhirVersion.parseFhirVersion("1.2.3"))
    }

    @Test
    fun isSupportedVersion_notSupportedVersion_expectFalse() {
        val fhirVersion = FhirVersion(major = 1, minor = 2, patch = 3)

        assertThat(fhirVersion.isSupportedFhirVersion()).isFalse()
    }

    @Test
    fun isSupportedVersion_supportedVersion_expectTrue() {
        val fhirVersion = FhirVersion(major = 4, minor = 0, patch = 1)

        assertThat(fhirVersion.isSupportedFhirVersion()).isTrue()
    }
}
