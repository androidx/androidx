/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.activity.result.ActivityResultCaller
import androidx.annotation.Sampled
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.feature.ExperimentalPersonalHealthRecordApi
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_READ_HEALTH_DATA_HISTORY
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_READ_MEDICAL_DATA_VACCINES
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_WRITE_MEDICAL_DATA
import androidx.health.connect.client.records.StepsRecord

@Sampled
fun RequestPermission(activity: ActivityResultCaller) {
    val requestPermission =
        activity.registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { grantedPermissions: Set<String> ->
            if (
                grantedPermissions.contains(HealthPermission.getReadPermission(StepsRecord::class))
            ) {
                // Read or process steps related health records.
            } else {
                // user denied permission
            }
        }
    requestPermission.launch(setOf(HealthPermission.getReadPermission(StepsRecord::class)))
}

@Sampled
fun RequestBackgroundReadPermission(
    features: HealthConnectFeatures,
    activity: ActivityResultCaller,
) {
    val requestPermission =
        activity.registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { grantedPermissions: Set<String> ->
            if (PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND in grantedPermissions) {
                // It will be possible to read data in background from now on
            } else {
                // Permission denied, it won't be possible to read data in background
            }
        }

    if (
        features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND) ==
            HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
    ) {
        // The feature is available, request background reads permission
        requestPermission.launch(setOf(PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND))
    }
}

@Sampled
fun RequestHistoryReadPermission(features: HealthConnectFeatures, activity: ActivityResultCaller) {
    val requestPermission =
        activity.registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { grantedPermissions: Set<String> ->
            if (PERMISSION_READ_HEALTH_DATA_HISTORY in grantedPermissions) {
                // It will be possible to read data older than 30 days from now on
            } else {
                // Permission denied, it won't be possible to read data older than 30 days
            }
        }

    if (
        features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY) ==
            HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
    ) {
        // The feature is available, request history read permissions
        requestPermission.launch(setOf(PERMISSION_READ_HEALTH_DATA_HISTORY))
    }
}

@Sampled
suspend fun GetPermissions(permissionController: PermissionController) {
    val grantedPermissions = permissionController.getGrantedPermissions()

    if (grantedPermissions.contains(HealthPermission.getReadPermission(StepsRecord::class))) {
        // Read or process steps related health records.
    } else {
        // user denied permission
    }
}

@OptIn(ExperimentalPersonalHealthRecordApi::class)
@Sampled
fun RequestMedicalPermissions(features: HealthConnectFeatures, activity: ActivityResultCaller) {
    // The set of medical permissions to be requested (add additional read permissions as required)
    val medicalPermissions =
        setOf(PERMISSION_WRITE_MEDICAL_DATA, PERMISSION_READ_MEDICAL_DATA_VACCINES)
    val requestPermission =
        activity.registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { grantedPermissions: Set<String> ->
            if (grantedPermissions.containsAll(medicalPermissions)) {
                // Permissions granted to write health data and read immunizations
            } else {
                // User denied permission to write health data and/or read immunizations
            }
        }

    if (
        features.getFeatureStatus(HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD) ==
            HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
    ) {
        // The feature is available, request medical permissions
        requestPermission.launch(medicalPermissions)
    }
}
