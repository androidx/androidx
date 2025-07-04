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
import androidx.health.connect.client.request.CreateMedicalDataSourceRequest
import androidx.health.connect.client.request.GetMedicalDataSourcesRequest

@OptIn(ExperimentalPersonalHealthRecordApi::class)
@Sampled
suspend fun CreateMedicalDataSourceSample(healthConnectClient: HealthConnectClient) {
    // Ensure `FEATURE_PERSONAL_HEALTH_RECORD` is available before calling PHR apis
    if (
        healthConnectClient.features.getFeatureStatus(FEATURE_PERSONAL_HEALTH_RECORD) !=
            FEATURE_STATUS_AVAILABLE
    ) {
        return
    }

    // Create a `MedicalDataSource`
    // Note that `displayName` must be unique across `MedicalDataSource`s
    val medicalDataSource: MedicalDataSource =
        healthConnectClient.createMedicalDataSource(
            CreateMedicalDataSourceRequest(
                fhirBaseUri = Uri.parse("https://fhir.com/oauth/api/FHIR/R4/"),
                displayName = "Test Data Source",
                fhirVersion = FhirVersion(4, 0, 1),
            )
        )
}

@OptIn(ExperimentalPersonalHealthRecordApi::class)
@Sampled
suspend fun DeleteMedicalDataSourceWithDataSample(healthConnectClient: HealthConnectClient) {
    // Ensure `FEATURE_PERSONAL_HEALTH_RECORD` is available before calling PHR apis
    if (
        healthConnectClient.features.getFeatureStatus(FEATURE_PERSONAL_HEALTH_RECORD) !=
            FEATURE_STATUS_AVAILABLE
    ) {
        return
    }

    // Get or creates a `MedicalDataSource`
    // Each `MedicalDataSource` is assigned an `id` by the system on creation
    val medicalDataSource: MedicalDataSource =
        healthConnectClient.createMedicalDataSource(
            CreateMedicalDataSourceRequest(
                fhirBaseUri = Uri.parse("https://fhir.com/oauth/api/FHIR/R4/"),
                displayName = "Test Data Source",
                fhirVersion = FhirVersion(4, 0, 1),
            )
        )

    // Delete the `MedicalDataSource` that has the specified `id`
    healthConnectClient.deleteMedicalDataSourceWithData(medicalDataSource.id)
}

@OptIn(ExperimentalPersonalHealthRecordApi::class)
@Sampled
suspend fun GetMedicalDataSourcesByRequestSample(
    healthConnectClient: HealthConnectClient,
    anotherPackageName: String,
) {
    // Ensure `FEATURE_PERSONAL_HEALTH_RECORD` is available before calling PHR apis
    if (
        healthConnectClient.features.getFeatureStatus(FEATURE_PERSONAL_HEALTH_RECORD) !=
            FEATURE_STATUS_AVAILABLE
    ) {
        return
    }

    // Get or create a `MedicalDataSource`
    // Each `MedicalDataSource` contains the `packageName` which created it
    val medicalDataSource: MedicalDataSource =
        healthConnectClient.createMedicalDataSource(
            CreateMedicalDataSourceRequest(
                fhirBaseUri = Uri.parse("https://fhir.com/oauth/api/FHIR/R4/"),
                displayName = "Test Data Source",
                fhirVersion = FhirVersion(4, 0, 1),
            )
        )

    // Retrieve all `MedicalDataSource`s created by any of the specified package names
    // Package names may be found in other `MedicalDataSource`s or from arbitrary input
    val medicalDataSources: List<MedicalDataSource> =
        healthConnectClient.getMedicalDataSources(
            GetMedicalDataSourcesRequest(listOf(medicalDataSource.packageName, anotherPackageName))
        )
}

@OptIn(ExperimentalPersonalHealthRecordApi::class)
@Sampled
suspend fun GetMedicalDataSourcesByIdsSample(
    healthConnectClient: HealthConnectClient,
    anotherId: String,
) {
    // Ensure `FEATURE_PERSONAL_HEALTH_RECORD` is available before calling PHR apis
    if (
        healthConnectClient.features.getFeatureStatus(FEATURE_PERSONAL_HEALTH_RECORD) !=
            FEATURE_STATUS_AVAILABLE
    ) {
        return
    }

    // Get or create a `MedicalDataSource`
    // Each `MedicalDataSource` is assigned an `id` by the system on creation
    val medicalDataSource: MedicalDataSource =
        healthConnectClient.createMedicalDataSource(
            CreateMedicalDataSourceRequest(
                fhirBaseUri = Uri.parse("https://fhir.com/oauth/api/FHIR/R4/"),
                displayName = "Test Data Source",
                fhirVersion = FhirVersion(4, 0, 1),
            )
        )

    // Retrieve all `MedicalDataSource` with `id` matching any of the given ids
    val medicalDataSources: List<MedicalDataSource> =
        healthConnectClient.getMedicalDataSources(listOf(medicalDataSource.id, anotherId))
}
