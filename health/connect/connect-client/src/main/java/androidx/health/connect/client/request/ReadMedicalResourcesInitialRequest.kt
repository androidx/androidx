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
import androidx.health.connect.client.impl.platform.records.toPlatformMedicalResourceType
import androidx.health.connect.client.impl.platform.request.PlatformReadMedicalResourcesInitialRequestBuilder
import androidx.health.connect.client.impl.platform.request.PlatformReadMedicalResourcesRequest
import androidx.health.connect.client.records.MedicalDataSource
import androidx.health.connect.client.records.MedicalResource
import androidx.health.connect.client.records.MedicalResource.Companion.MedicalResourceType
import androidx.health.connect.client.records.toString
import androidx.health.connect.client.request.ReadMedicalResourcesRequest.Companion.DEFAULT_PAGE_SIZE

/**
 * A class to make **initial** requests when reading [MedicalResource]s with
 * [HealthConnectClient.readMedicalResources].
 *
 * This feature is dependent on the version of HealthConnect installed on the device. To check if
 * it's available call [HealthConnectFeatures.getFeatureStatus] and pass
 * [FEATURE_PERSONAL_HEALTH_RECORD] as an argument. An [UnsupportedOperationException] would be
 * thrown if the feature is not available.
 *
 * @property pageSize The maximum number of [MedicalResource]s to be read. Default value is
 *   [DEFAULT_PAGE_SIZE]. An [IllegalArgumentException] might be thrown if [pageSize] is deemed as
 *   invalid, such as too large.
 * @property medicalResourceType Only [MedicalResource]s with this [MedicalResourceType] will be
 *   returned. An [IllegalArgumentException] might be thrown if an invalid type is used, see
 *   [MedicalResourceType] for valid values.
 * @property medicalDataSourceIds Only [MedicalResource]s belong to one of the [MedicalDataSource]s
 *   represented by these IDs will be returned. An empty set is permitted, it means
 *   [MedicalResource]s from all available apps will be included in the response. An
 *   [IllegalArgumentException] might be thrown if any ID is deemed as invalid.
 * @see [HealthConnectClient.readMedicalResources]
 */
@ExperimentalPersonalHealthRecordApi
class ReadMedicalResourcesInitialRequest(
    @MedicalResourceType val medicalResourceType: Int,
    val medicalDataSourceIds: Set<String>,
    pageSize: Int = DEFAULT_PAGE_SIZE,
) : ReadMedicalResourcesRequest(pageSize) {
    @SuppressLint("NewApi") // already checked with a feature availability check
    override val platformReadMedicalResourcesRequest: PlatformReadMedicalResourcesRequest =
        withPhrFeatureCheck(this::class) {
            PlatformReadMedicalResourcesInitialRequestBuilder(
                    medicalResourceType.toPlatformMedicalResourceType()
                )
                .addDataSourceIds(medicalDataSourceIds)
                .setPageSize(pageSize)
                .build()
        }

    override fun toString(): String =
        toString(
            this,
            mapOf(
                "medicalResourceType" to medicalResourceType,
                "medicalDataSourceIds" to medicalDataSourceIds,
                "pageSize" to pageSize,
            ),
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReadMedicalResourcesInitialRequest) return false
        if (!super.equals(other)) return false

        if (medicalResourceType != other.medicalResourceType) return false
        if (medicalDataSourceIds != other.medicalDataSourceIds) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + medicalResourceType
        result = 31 * result + medicalDataSourceIds.hashCode()
        return result
    }
}
