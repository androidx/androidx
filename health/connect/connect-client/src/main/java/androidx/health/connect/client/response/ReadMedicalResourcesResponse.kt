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

package androidx.health.connect.client.response

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.HealthConnectFeatures.Companion.FEATURE_PERSONAL_HEALTH_RECORD
import androidx.health.connect.client.feature.ExperimentalPersonalHealthRecordApi
import androidx.health.connect.client.records.MedicalResource
import androidx.health.connect.client.records.toString
import androidx.health.connect.client.request.ReadMedicalResourcesPageRequest

/**
 * A class to hold responses for requests made with [HealthConnectClient.readMedicalResources].
 *
 * This feature is dependent on the version of HealthConnect installed on the device. To check if
 * it's available call [HealthConnectFeatures.getFeatureStatus] and pass
 * [FEATURE_PERSONAL_HEALTH_RECORD] as an argument. An [UnsupportedOperationException] would be
 * thrown if the feature is not available.
 *
 * @property medicalResources A list of [MedicalResource]s.
 * @property nextPageToken A token which can be used with [ReadMedicalResourcesPageRequest] to read
 *   the next page. `null` if there are no more pages available.
 * @property remainingCount The total number of [MedicalResource]s remaining, excluding the ones in
 *   this response.
 * @see [HealthConnectClient.readMedicalResources]
 */
@ExperimentalPersonalHealthRecordApi
class ReadMedicalResourcesResponse(
    val medicalResources: List<MedicalResource>,
    val nextPageToken: String?,
    val remainingCount: Int,
) {
    override fun toString(): String =
        toString(
            this,
            mapOf(
                "medicalResources" to medicalResources,
                "nextPageToken" to nextPageToken,
                "remainingCount" to remainingCount,
            ),
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReadMedicalResourcesResponse) return false

        if (remainingCount != other.remainingCount) return false
        if (medicalResources != other.medicalResources) return false
        if (nextPageToken != other.nextPageToken) return false

        return true
    }

    override fun hashCode(): Int {
        var result = remainingCount
        result = 31 * result + medicalResources.hashCode()
        result = 31 * result + (nextPageToken?.hashCode() ?: 0)
        return result
    }
}
