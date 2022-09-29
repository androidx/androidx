/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.connect.client.permission

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient.Companion.DEFAULT_PROVIDER_PACKAGE_NAME
import androidx.health.connect.client.HealthConnectClient.Companion.HEALTH_CONNECT_CLIENT_TAG
import androidx.health.connect.client.impl.converters.permission.toJetpackPermission
import androidx.health.connect.client.impl.converters.permission.toProtoPermission
import androidx.health.platform.client.permission.Permission as ProtoPermission
import androidx.health.platform.client.impl.logger.Logger
import androidx.health.platform.client.service.HealthDataServiceConstants.ACTION_REQUEST_PERMISSIONS
import androidx.health.platform.client.service.HealthDataServiceConstants.KEY_GRANTED_PERMISSIONS_JETPACK
import androidx.health.platform.client.service.HealthDataServiceConstants.KEY_REQUESTED_PERMISSIONS_JETPACK

/**
 * An [ActivityResultContract] to request Health Connect permissions.
 *
 * @param providerPackageName Optional provider package name for the backing implementation of
 * choice.
 *
 * @see androidx.activity.ComponentActivity.registerForActivityResult
 */
internal class HealthDataRequestPermissions(
    private val providerPackageName: String = DEFAULT_PROVIDER_PACKAGE_NAME,
) : ActivityResultContract<Set<HealthPermission>, Set<HealthPermission>>() {

    override fun createIntent(context: Context, input: Set<HealthPermission>): Intent {
        require(input.isNotEmpty()) { "At least one permission is required!" }

        val protoPermissionList =
            input
                .asSequence()
                .map { ProtoPermission(it.toProtoPermission()) }
                .toCollection(ArrayList())
        Logger.debug(HEALTH_CONNECT_CLIENT_TAG, "Requesting ${input.size} permissions.")
        return Intent(ACTION_REQUEST_PERMISSIONS).apply {
            putParcelableArrayListExtra(KEY_REQUESTED_PERMISSIONS_JETPACK, protoPermissionList)
            if (providerPackageName.isNotEmpty()) {
                setPackage(providerPackageName)
            }
        }
    }

    @Suppress("Deprecation")
    override fun parseResult(resultCode: Int, intent: Intent?): Set<HealthPermission> {
        val grantedPermissions = intent
            ?.getParcelableArrayListExtra<ProtoPermission>(KEY_GRANTED_PERMISSIONS_JETPACK)
            ?.asSequence()
            ?.map { it.proto.toJetpackPermission() }
            ?.toSet()
            ?: emptySet()
        Logger.debug(HEALTH_CONNECT_CLIENT_TAG, "Granted ${grantedPermissions.size} permissions.")
        return grantedPermissions
    }

    override fun getSynchronousResult(
        context: Context,
        input: Set<HealthPermission>,
    ): SynchronousResult<Set<HealthPermission>>? {
        return null
    }
}

@Suppress("Deprecation") // Utility to allow usage internally while suppressing deprecation.
internal fun createHealthDataRequestPermissions(
    providerPackageName: String
): ActivityResultContract<Set<HealthPermission>, Set<HealthPermission>> {
    return HealthDataRequestPermissions(providerPackageName = providerPackageName)
}
