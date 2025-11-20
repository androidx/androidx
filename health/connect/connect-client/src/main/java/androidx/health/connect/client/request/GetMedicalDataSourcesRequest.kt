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
import androidx.health.connect.client.impl.platform.request.PlatformGetMedicalDataSourcesRequest
import androidx.health.connect.client.impl.platform.request.PlatformGetMedicalDataSourcesRequestBuilder
import androidx.health.connect.client.records.MedicalDataSource
import androidx.health.connect.client.records.toString

/**
 * Request to read medical data sources using [HealthConnectManager#getMedicalDataSources]
 *
 * If no package names are set in a request, all data sources from all packages will be returned.
 * Otherwise the response is limited to the requested package names per permission enforced in
 * [HealthConnectClient.getMedicalDataSources]
 *
 * This feature is dependent on the version of HealthConnect installed on the device. To check if
 * it's available call [HealthConnectFeatures.getFeatureStatus] and pass
 * [FEATURE_PERSONAL_HEALTH_RECORD] as an argument. An [UnsupportedOperationException] would be
 * thrown if the feature is not available.
 *
 * @property packageNames Only [MedicalDataSource]s created by the apps with these package names
 *   will be returned.
 */
@ExperimentalPersonalHealthRecordApi
class GetMedicalDataSourcesRequest(val packageNames: List<String>) {

    @SuppressLint("NewApi") // already checked with a feature availability check
    internal val platformGetMedicalDataSourcesRequest: PlatformGetMedicalDataSourcesRequest =
        withPhrFeatureCheck(this::class) {
            PlatformGetMedicalDataSourcesRequestBuilder()
                .apply { packageNames.forEach { addPackageName(it) } }
                .build()
        }

    override fun toString() = toString(this, mapOf("packageNames" to packageNames))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GetMedicalDataSourcesRequest

        if (packageNames != other.packageNames) return false
        if (platformGetMedicalDataSourcesRequest != other.platformGetMedicalDataSourcesRequest)
            return false

        return true
    }

    override fun hashCode(): Int {
        var result = packageNames.hashCode()
        result = 31 * result + platformGetMedicalDataSourcesRequest.hashCode()
        return result
    }
}
