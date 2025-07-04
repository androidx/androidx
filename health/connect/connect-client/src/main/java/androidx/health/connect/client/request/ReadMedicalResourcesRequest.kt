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

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.HealthConnectFeatures.Companion.FEATURE_PERSONAL_HEALTH_RECORD
import androidx.health.connect.client.feature.ExperimentalPersonalHealthRecordApi
import androidx.health.connect.client.impl.platform.request.PlatformReadMedicalResourcesRequest
import androidx.health.connect.client.records.MedicalResource

/**
 * A base class for reading [MedicalResource]s with [HealthConnectClient.readMedicalResources].
 *
 * This feature is dependent on the version of HealthConnect installed on the device. To check if
 * it's available call [HealthConnectFeatures.getFeatureStatus] and pass
 * [FEATURE_PERSONAL_HEALTH_RECORD] as an argument. An [UnsupportedOperationException] would be
 * thrown if the feature is not available.
 *
 * @property pageSize The maximum number of [MedicalResource]s to be read.
 * @see [HealthConnectClient.readMedicalResources]
 */
@ExperimentalPersonalHealthRecordApi
abstract class ReadMedicalResourcesRequest internal constructor(val pageSize: Int) {
    internal abstract val platformReadMedicalResourcesRequest: PlatformReadMedicalResourcesRequest

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReadMedicalResourcesRequest) return false

        if (pageSize != other.pageSize) return false

        return true
    }

    override fun hashCode(): Int {
        return pageSize
    }

    companion object {
        /** Default value for [ReadMedicalResourcesRequest.pageSize]. */
        const val DEFAULT_PAGE_SIZE = 1000
    }
}
