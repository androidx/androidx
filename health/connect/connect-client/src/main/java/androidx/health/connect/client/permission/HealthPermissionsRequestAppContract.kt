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
import androidx.annotation.RestrictTo
import androidx.health.connect.client.HealthConnectClient.Companion.DEFAULT_PROVIDER_PACKAGE_NAME
import androidx.health.connect.client.HealthConnectClient.Companion.HEALTH_CONNECT_CLIENT_TAG
import androidx.health.platform.client.impl.logger.Logger
import androidx.health.platform.client.permission.Permission as ParcelablePermission
import androidx.health.platform.client.proto.PermissionProto
import androidx.health.platform.client.service.HealthDataServiceConstants.ACTION_REQUEST_PERMISSIONS
import androidx.health.platform.client.service.HealthDataServiceConstants.KEY_GRANTED_PERMISSIONS_STRING
import androidx.health.platform.client.service.HealthDataServiceConstants.KEY_REQUESTED_PERMISSIONS_STRING

/**
 * An [ActivityResultContract] to request Health Connect permissions from the HealthConnect APK.
 *
 * @param providerPackageName Optional provider package name for the backing implementation of
 *   choice.
 * @see androidx.activity.ComponentActivity.registerForActivityResult
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class HealthPermissionsRequestAppContract(
    private val providerPackageName: String = DEFAULT_PROVIDER_PACKAGE_NAME,
) : ActivityResultContract<Set<String>, Set<String>>() {

    override fun createIntent(context: Context, input: Set<String>): Intent {
        val protoPermissionList =
            input
                .asSequence()
                .map {
                    ParcelablePermission(
                        PermissionProto.Permission.newBuilder().setPermission(it).build()
                    )
                }
                .toCollection(ArrayList())
        Logger.debug(HEALTH_CONNECT_CLIENT_TAG, "Requesting ${input.size} permissions.")
        return Intent(ACTION_REQUEST_PERMISSIONS).apply {
            putParcelableArrayListExtra(KEY_REQUESTED_PERMISSIONS_STRING, protoPermissionList)
            if (providerPackageName.isNotEmpty()) {
                setPackage(providerPackageName)
            }
        }
    }

    @Suppress("Deprecation")
    override fun parseResult(resultCode: Int, intent: Intent?): Set<String> {
        val grantedPermissions =
            intent
                ?.getParcelableArrayListExtra<ParcelablePermission>(KEY_GRANTED_PERMISSIONS_STRING)
                ?.asSequence()
                ?.map { it.proto.permission }
                ?.toSet()
                ?: emptySet()
        Logger.debug(HEALTH_CONNECT_CLIENT_TAG, "Granted ${grantedPermissions.size} permissions.")
        return grantedPermissions
    }

    override fun getSynchronousResult(
        context: Context,
        input: Set<String>,
    ): SynchronousResult<Set<String>>? {
        return null
    }
}
