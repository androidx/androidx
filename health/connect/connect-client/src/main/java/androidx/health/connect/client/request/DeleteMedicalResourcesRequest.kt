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

package androidx.health.connect.client.request

import android.annotation.SuppressLint
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.HealthConnectFeatures.Companion.FEATURE_PERSONAL_HEALTH_RECORD
import androidx.health.connect.client.feature.ExperimentalPersonalHealthRecordApi
import androidx.health.connect.client.feature.withPhrFeatureCheck
import androidx.health.connect.client.impl.platform.request.PlatformDeleteMedicalResourcesRequest
import androidx.health.connect.client.impl.platform.request.PlatformDeleteMedicalResourcesRequestBuilder
import androidx.health.connect.client.records.MedicalDataSource
import androidx.health.connect.client.records.MedicalResource
import androidx.health.connect.client.records.MedicalResource.Companion.MedicalResourceType
import androidx.health.connect.client.records.toString

/**
 * A class to delete [MedicalResource]s with [HealthConnectClient.deleteMedicalResources].
 *
 * At least one of [dataSourceIds] and [medicalResourceTypes] must be non-empty.
 *
 * This feature is dependent on the version of HealthConnect installed on the device. To check if
 * it's available call [HealthConnectFeatures.getFeatureStatus] and pass
 * [FEATURE_PERSONAL_HEALTH_RECORD] as an argument. An [UnsupportedOperationException] would be
 * thrown if the feature is not available.
 *
 * @property dataSourceIds IDs of [MedicalDataSource]s to delete [MedicalResource]s from. This being
 *   empty is equivalent as if all [MedicalDataSource]s are set.
 * @property medicalResourceTypes [MedicalResourceType]s of [MedicalResource]s to delete. This being
 *   empty is equivalent as if all [MedicalResourceType]s are set.
 * @throws IllegalArgumentException if both of [dataSourceIds] and [medicalResourceTypes] are empty.
 * @see [HealthConnectClient.deleteMedicalResources]
 */
@ExperimentalPersonalHealthRecordApi
class DeleteMedicalResourcesRequest(
    val dataSourceIds: Set<String> = emptySet<String>(),
    val medicalResourceTypes: Set<Int> = emptySet<Int>(),
) {
    @SuppressLint("NewApi") // already checked with a feature availability check
    internal val platformReadMedicalResourcesRequest: PlatformDeleteMedicalResourcesRequest =
        withPhrFeatureCheck(this::class) {
            PlatformDeleteMedicalResourcesRequestBuilder()
                .also { builder -> dataSourceIds.forEach { builder.addDataSourceId(it) } }
                .also { builder ->
                    medicalResourceTypes.forEach { builder.addMedicalResourceType(it) }
                }
                .build()
        }

    override fun toString() =
        toString(
            this,
            mapOf("dataSourceIds" to dataSourceIds, "medicalResourceTypes" to medicalResourceTypes),
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeleteMedicalResourcesRequest) return false

        if (dataSourceIds != other.dataSourceIds) return false
        if (medicalResourceTypes != other.medicalResourceTypes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dataSourceIds.hashCode()
        result = 31 * result + medicalResourceTypes.hashCode()
        return result
    }
}
