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
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.feature.ExperimentalPersonalHealthRecordApi
import androidx.health.connect.client.feature.withPhrFeatureCheck
import androidx.health.connect.client.impl.platform.request.PlatformUpsertMedicalResourceRequest
import androidx.health.connect.client.impl.platform.request.PlatformUpsertMedicalResourceRequestBuilder
import androidx.health.connect.client.records.FhirVersion
import androidx.health.connect.client.records.MedicalDataSource
import androidx.health.connect.client.records.MedicalResource
import androidx.health.connect.client.records.MedicalResourceId
import androidx.health.connect.client.records.toString

/**
 * A request class for both inserting and updating [MedicalResource]s.
 *
 * From [dataSourceId], fhir resource type and fhir resource ID extracted from [data], a
 * [MedicalResourceId] will be constructed. If there already exists a [MedicalResource] with that
 * [MedicalResourceId] in Health Connect, then that [MedicalResource] will be updated, otherwise a
 * new [MedicalResource] will be inserted.
 *
 * The data representation follows the
 * [Fast Healthcare Interoperability Resources (FHIR)](https://hl7.org/fhir/) standard.
 *
 * This feature is dependent on the version of HealthConnect installed on the device. To check if
 * it's available call [HealthConnectFeatures.getFeatureStatus] and pass
 * [HealthConnectFeatures.Companion.FEATURE_PERSONAL_HEALTH_RECORD] as an argument.
 *
 * @property dataSourceId The ID of an **existing** [MedicalDataSource] where the data being
 *   inserted comes from.
 * @property fhirVersion The FHIR version being used for [data]. This must match [dataSourceId]'s
 *   [MedicalDataSource.fhirVersion].
 * @property data The FHIR resource data in JSON representation.
 */
@ExperimentalPersonalHealthRecordApi
class UpsertMedicalResourceRequest(
    val dataSourceId: String,
    val fhirVersion: FhirVersion,
    val data: String,
) {
    @SuppressLint("NewApi") // already checked with a feature availability check
    internal val platformUpsertMedicalResourceRequest: PlatformUpsertMedicalResourceRequest =
        withPhrFeatureCheck(UpsertMedicalResourceRequest::class.java.simpleName) {
            PlatformUpsertMedicalResourceRequestBuilder(
                    dataSourceId,
                    fhirVersion.platformFhirVersion,
                    data,
                )
                .build()
        }

    override fun toString(): String =
        toString(
            this,
            mapOf("dataSourceId" to dataSourceId, "fhirVersion" to fhirVersion, "data" to data),
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UpsertMedicalResourceRequest) return false

        if (dataSourceId != other.dataSourceId) return false
        if (fhirVersion != other.fhirVersion) return false
        if (data != other.data) return false
        if (platformUpsertMedicalResourceRequest != other.platformUpsertMedicalResourceRequest)
            return false

        return true
    }

    override fun hashCode(): Int {
        var result = dataSourceId.hashCode()
        result = 31 * result + fhirVersion.hashCode()
        result = 31 * result + data.hashCode()
        result = 31 * result + platformUpsertMedicalResourceRequest.hashCode()
        return result
    }
}
