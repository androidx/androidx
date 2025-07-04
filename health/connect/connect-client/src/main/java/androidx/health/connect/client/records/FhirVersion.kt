/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.health.connect.client.records

import android.annotation.SuppressLint
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.feature.ExperimentalPersonalHealthRecordApi
import androidx.health.connect.client.feature.withPhrFeatureCheck
import androidx.health.connect.client.impl.platform.records.PlatformFhirVersion

/**
 * Represents the FHIR version. This is designed according to
 * [the official FHIR versions](https://build.fhir.org/versions.html#versions) of the Fast
 * Healthcare Interoperability Resources (FHIR) standard. "label", which represents a 'working'
 * version, is not supported for now.
 *
 * The versions R4 (4.0.1) and R4B (4.3.0) are supported in Health Connect. Use
 * [isSupportedFhirVersion] to check whether a FHIR version is supported.
 *
 * This feature is dependent on the version of HealthConnect installed on the device. To check if
 * it's available call [HealthConnectFeatures.getFeatureStatus] and pass
 * [HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD] as an argument. An
 * [UnsupportedOperationException] would be thrown if the feature is not available.
 */
@ExperimentalPersonalHealthRecordApi
class FhirVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<FhirVersion> {
    @SuppressLint("NewApi") // already checked with a feature availability check
    internal val platformFhirVersion: PlatformFhirVersion =
        withPhrFeatureCheck(this::class) {
            PlatformFhirVersion.parseFhirVersion("$major.$minor.$patch")
        }

    override fun compareTo(other: FhirVersion): Int {
        if (major != other.major) {
            return if (major > other.major) 1 else -1
        }
        if (minor != other.minor) {
            return if (minor > other.minor) 1 else -1
        }
        if (patch != other.patch) {
            return if (patch > other.patch) 1 else -1
        }
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FhirVersion) return false

        if (major != other.major) return false
        if (minor != other.minor) return false
        if (patch != other.patch) return false

        return true
    }

    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + patch
        return result
    }

    override fun toString(): String {
        return "${this::class.java.simpleName}@${Integer.toHexString(System.identityHashCode(this))}($major.$minor.$patch)"
    }

    /**
     * Returns `true` if this [FhirVersion] is supported, `false` otherwise.
     *
     * This method is dependent on the version of HealthConnect installed on the device. To check if
     * it's available call [HealthConnectFeatures.getFeatureStatus] and pass
     * [HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD] as an argument.
     */
    @SuppressLint("NewApi") // checked by `getFeatureStatus()`
    fun isSupportedFhirVersion(): Boolean =
        withPhrFeatureCheck(this::class, "isSupportedFhirVersion") {
            platformFhirVersion.isSupportedFhirVersion
        }

    companion object {
        private val VERSION_REGEX = Regex("(\\d+)\\.(\\d+)\\.(\\d+)")

        /**
         * Creates a [FhirVersion] object with the version of string format.
         *
         * The format should look like "4.0.1" which contains 3 numbers - major, minor and patch,
         * separated by ".". This aligns with
         * [the official FHIR versions](https://build.fhir.org/versions.html#versions). Note that
         * the "label" is not supported for now, which represents a 'working' version.
         */
        @JvmStatic
        fun parseFhirVersion(fhirVersionString: String): FhirVersion {
            val result = VERSION_REGEX.find(fhirVersionString)
            require(result != null) { "Invalid FHIR version string: $fhirVersionString" }
            return FhirVersion(
                result.groupValues[1].toInt(),
                result.groupValues[2].toInt(),
                result.groupValues[3].toInt(),
            )
        }
    }
}
