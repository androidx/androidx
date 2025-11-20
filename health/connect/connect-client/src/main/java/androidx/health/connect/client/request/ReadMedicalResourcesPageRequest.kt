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
import androidx.health.connect.client.impl.platform.request.PlatformReadMedicalResourcesPageRequestBuilder
import androidx.health.connect.client.impl.platform.request.PlatformReadMedicalResourcesRequest
import androidx.health.connect.client.records.MedicalResource
import androidx.health.connect.client.records.toString
import androidx.health.connect.client.request.ReadMedicalResourcesRequest.Companion.DEFAULT_PAGE_SIZE
import androidx.health.connect.client.response.ReadMedicalResourcesResponse

/**
 * A class to make **subsequent** requests when reading [MedicalResource]s with
 * [HealthConnectClient.readMedicalResources].
 *
 * This class should only be used after a successful initial request has been made with
 * [ReadMedicalResourcesInitialRequest].
 *
 * This feature is dependent on the version of HealthConnect installed on the device. To check if
 * it's available call [HealthConnectFeatures.getFeatureStatus] and pass
 * [FEATURE_PERSONAL_HEALTH_RECORD] as an argument. An [UnsupportedOperationException] would be
 * thrown if the feature is not available.
 *
 * @property pageSize The maximum number of [MedicalResource]s to be read. Default value is
 *   [DEFAULT_PAGE_SIZE]. An [IllegalArgumentException] might be thrown if [pageSize] is deemed as
 *   invalid, such as too large.
 * @property pageToken The token to read the next page, this should be obtained from
 *   [ReadMedicalResourcesResponse.nextPageToken].
 * @see [HealthConnectClient.readMedicalResources]
 */
@ExperimentalPersonalHealthRecordApi
class ReadMedicalResourcesPageRequest(val pageToken: String, pageSize: Int = DEFAULT_PAGE_SIZE) :
    ReadMedicalResourcesRequest(pageSize) {

    @SuppressLint("NewApi") // already checked with a feature availability check
    override val platformReadMedicalResourcesRequest: PlatformReadMedicalResourcesRequest =
        withPhrFeatureCheck(this::class) {
            PlatformReadMedicalResourcesPageRequestBuilder(pageToken).setPageSize(pageSize).build()
        }

    override fun toString(): String =
        toString(this, mapOf("pageToken" to pageToken, "pageSize" to pageSize))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReadMedicalResourcesPageRequest) return false
        if (!super.equals(other)) return false

        if (pageToken != other.pageToken) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + pageToken.hashCode()
        return result
    }
}
