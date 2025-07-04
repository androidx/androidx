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

@file:Suppress("UNUSED_VARIABLE")

package androidx.health.connect.client.samples

import android.net.Uri
import androidx.annotation.Sampled
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures.Companion.FEATURE_PERSONAL_HEALTH_RECORD
import androidx.health.connect.client.HealthConnectFeatures.Companion.FEATURE_STATUS_AVAILABLE
import androidx.health.connect.client.feature.ExperimentalPersonalHealthRecordApi
import androidx.health.connect.client.records.FhirVersion
import androidx.health.connect.client.records.MedicalDataSource
import androidx.health.connect.client.records.MedicalResource
import androidx.health.connect.client.records.MedicalResource.Companion.MEDICAL_RESOURCE_TYPE_LABORATORY_RESULTS
import androidx.health.connect.client.records.MedicalResource.Companion.MEDICAL_RESOURCE_TYPE_MEDICATIONS
import androidx.health.connect.client.records.MedicalResourceId
import androidx.health.connect.client.request.CreateMedicalDataSourceRequest
import androidx.health.connect.client.request.DeleteMedicalResourcesRequest
import androidx.health.connect.client.request.ReadMedicalResourcesInitialRequest
import androidx.health.connect.client.request.ReadMedicalResourcesPageRequest
import androidx.health.connect.client.request.ReadMedicalResourcesRequest
import androidx.health.connect.client.request.UpsertMedicalResourceRequest
import androidx.health.connect.client.response.ReadMedicalResourcesResponse

@OptIn(ExperimentalPersonalHealthRecordApi::class)
@Sampled
suspend fun UpsertMedicalResourcesSample(
    healthConnectClient: HealthConnectClient,
    medicationJsonToInsert: String,
    updatedMedicationJsonToInsert: String,
) {
    // Ensure `FEATURE_PERSONAL_HEALTH_RECORD` is available before calling PHR apis
    if (
        healthConnectClient.features.getFeatureStatus(FEATURE_PERSONAL_HEALTH_RECORD) !=
            FEATURE_STATUS_AVAILABLE
    ) {
        return
    }

    // Get or create a `MedicalDataSource`
    val medicalDataSource: MedicalDataSource =
        healthConnectClient.createMedicalDataSource(
            CreateMedicalDataSourceRequest(
                fhirBaseUri = Uri.parse("https://fhir.com/oauth/api/FHIR/R4/"),
                displayName = "Test Data Source",
                fhirVersion = FhirVersion(4, 0, 1),
            )
        )

    // Insert `MedicalResource`s into the `MedicalDataSource`
    val medicalResources: List<MedicalResource> =
        healthConnectClient.upsertMedicalResources(
            listOf(
                UpsertMedicalResourceRequest(
                    medicalDataSource.id,
                    medicalDataSource.fhirVersion,
                    medicationJsonToInsert, // a valid FHIR json string
                )
            )
        )

    // Update `MedicalResource`s in the `MedicalDataSource`
    val updatedMedicalResources: List<MedicalResource> =
        healthConnectClient.upsertMedicalResources(
            listOf(
                UpsertMedicalResourceRequest(
                    medicalDataSource.id,
                    medicalDataSource.fhirVersion,
                    // a valid FHIR json string
                    // if this resource has the same type and ID as in `medicationJsonToInsert`,
                    // this `upsertMedicalResources()` call will update the previously inserted
                    // `MedicalResource`
                    updatedMedicationJsonToInsert,
                )
            )
        )
}

@OptIn(ExperimentalPersonalHealthRecordApi::class)
@Sampled
suspend fun ReadMedicalResourcesByRequestSample(
    healthConnectClient: HealthConnectClient,
    exampleLabResults: List<UpsertMedicalResourceRequest>,
) {
    // Ensure `FEATURE_PERSONAL_HEALTH_RECORD` is available before calling PHR apis
    if (
        healthConnectClient.features.getFeatureStatus(FEATURE_PERSONAL_HEALTH_RECORD) !=
            FEATURE_STATUS_AVAILABLE
    ) {
        return
    }

    // Get or create a `MedicalDataSource`
    val medicalDataSource: MedicalDataSource =
        healthConnectClient.createMedicalDataSource(
            CreateMedicalDataSourceRequest(
                fhirBaseUri = Uri.parse("https://fhir.com/oauth/api/FHIR/R4/"),
                displayName = "Test Data Source",
                fhirVersion = FhirVersion(4, 0, 1),
            )
        )

    // Insert `MedicalResource`s into the `MedicalDataSource`
    healthConnectClient.upsertMedicalResources(exampleLabResults)

    // Read `MedicalResource`s back from the `MedicalDataSource`
    // Read 100 resources / page. See `pageSize` doc for defaults and limits.
    val pageSize = 100
    // Prepare the initial read request.
    // All `MedicalResource`s in the given `MedicalDataSource`s and of given `medicalResourceType`
    // will be retrieved.
    val initialRequest: ReadMedicalResourcesRequest =
        ReadMedicalResourcesInitialRequest(
            MEDICAL_RESOURCE_TYPE_LABORATORY_RESULTS,
            setOf(medicalDataSource.id),
            pageSize = pageSize,
        )
    // Continue reading pages until all `MedicalResource`s are read
    var pageToken: String? = null
    do {
        // Prepare paged request if needed
        val request: ReadMedicalResourcesRequest =
            if (pageToken == null) initialRequest
            else ReadMedicalResourcesPageRequest(pageToken, pageSize = pageSize)
        // Read `MedicalResource`s
        val response: ReadMedicalResourcesResponse =
            healthConnectClient.readMedicalResources(request)
        // Process `MedicalResource`s as desired
        val resources: List<MedicalResource> = response.medicalResources
        // Advance to next page
        pageToken = response.nextPageToken
    } while (pageToken != null)
}

@OptIn(ExperimentalPersonalHealthRecordApi::class)
@Sampled
suspend fun ReadMedicalResourcesByIdsSample(
    healthConnectClient: HealthConnectClient,
    medicationJsonToInsert: String,
) {
    // Ensure `FEATURE_PERSONAL_HEALTH_RECORD` is available before calling PHR apis
    if (
        healthConnectClient.features.getFeatureStatus(FEATURE_PERSONAL_HEALTH_RECORD) !=
            FEATURE_STATUS_AVAILABLE
    ) {
        return
    }

    // Get or create a `MedicalDataSource`
    val medicalDataSource: MedicalDataSource =
        healthConnectClient.createMedicalDataSource(
            CreateMedicalDataSourceRequest(
                fhirBaseUri = Uri.parse("https://fhir.com/oauth/api/FHIR/R4/"),
                displayName = "Test Data Source",
                fhirVersion = FhirVersion(4, 0, 1),
            )
        )

    // Insert `MedicalResource`s into the `MedicalDataSource`
    val medicalResources: List<MedicalResource> =
        healthConnectClient.upsertMedicalResources(
            listOf(
                UpsertMedicalResourceRequest(
                    medicalDataSource.id,
                    medicalDataSource.fhirVersion,
                    medicationJsonToInsert, // a valid FHIR json string
                )
            )
        )

    // Retrieve `fhirResourceType` type `MedicalResource`s with the specified `id`s from the
    // provided `MedicalDataSource`
    val retrievedMedicalResources: List<MedicalResource> =
        healthConnectClient.readMedicalResources(
            medicalResources.map { medicalResource: MedicalResource ->
                MedicalResourceId(
                    dataSourceId = medicalDataSource.id,
                    fhirResourceType = medicalResource.id.fhirResourceType,
                    fhirResourceId = medicalResource.id.fhirResourceId,
                )
            }
        )
}

@OptIn(ExperimentalPersonalHealthRecordApi::class)
@Sampled
suspend fun DeleteMedicalResourcesSample(
    healthConnectClient: HealthConnectClient,
    medicationJsonToInsert: String,
) {
    // Ensure `FEATURE_PERSONAL_HEALTH_RECORD` is available before calling PHR apis
    if (
        healthConnectClient.features.getFeatureStatus(FEATURE_PERSONAL_HEALTH_RECORD) !=
            FEATURE_STATUS_AVAILABLE
    ) {
        return
    }

    // Get or create a `MedicalDataSource`
    val medicalDataSource: MedicalDataSource =
        healthConnectClient.createMedicalDataSource(
            CreateMedicalDataSourceRequest(
                fhirBaseUri = Uri.parse("https://fhir.com/oauth/api/FHIR/R4/"),
                displayName = "Test Data Source",
                fhirVersion = FhirVersion(4, 0, 1),
            )
        )

    // Insert `MedicalResource`s into the `MedicalDataSource`
    val medicalResources: List<MedicalResource> =
        healthConnectClient.upsertMedicalResources(
            listOf(
                UpsertMedicalResourceRequest(
                    medicalDataSource.id,
                    medicalDataSource.fhirVersion,
                    medicationJsonToInsert, // a valid FHIR json string
                )
            )
        )

    // Delete `MedicalResource`s matching the specified `dataSourceId`, `type` and `fhirResourceId`
    healthConnectClient.deleteMedicalResources(
        medicalResources.map { medicalResource: MedicalResource ->
            MedicalResourceId(
                dataSourceId = medicalDataSource.id,
                fhirResourceType = medicalResource.id.fhirResourceType,
                fhirResourceId = medicalResource.id.fhirResourceId,
            )
        }
    )
}

@OptIn(ExperimentalPersonalHealthRecordApi::class)
@Sampled
suspend fun DeleteMedicalResourcesByRequestSample(
    healthConnectClient: HealthConnectClient,
    medicationJsonToInsert: String,
) {
    // Ensure `FEATURE_PERSONAL_HEALTH_RECORD` is available before calling PHR apis
    if (
        healthConnectClient.features.getFeatureStatus(FEATURE_PERSONAL_HEALTH_RECORD) !=
            FEATURE_STATUS_AVAILABLE
    ) {
        return
    }

    // Get or create a `MedicalDataSource`
    val medicalDataSource: MedicalDataSource =
        healthConnectClient.createMedicalDataSource(
            CreateMedicalDataSourceRequest(
                fhirBaseUri = Uri.parse("https://fhir.com/oauth/api/FHIR/R4/"),
                displayName = "Test Data Source",
                fhirVersion = FhirVersion(4, 0, 1),
            )
        )

    // Insert `MedicalResource`s into the `MedicalDataSource`
    val medicalResources: List<MedicalResource> =
        healthConnectClient.upsertMedicalResources(
            listOf(
                UpsertMedicalResourceRequest(
                    medicalDataSource.id,
                    medicalDataSource.fhirVersion,
                    medicationJsonToInsert, // a valid FHIR json string
                )
            )
        )

    // Delete all `MedicalResource`s that are in any pair of provided `dataSourceIds` and
    // `medicalResourceTypes`
    healthConnectClient.deleteMedicalResources(
        DeleteMedicalResourcesRequest(
            dataSourceIds = setOf(medicalDataSource.id),
            medicalResourceTypes = setOf(MEDICAL_RESOURCE_TYPE_MEDICATIONS),
        )
    )
}
