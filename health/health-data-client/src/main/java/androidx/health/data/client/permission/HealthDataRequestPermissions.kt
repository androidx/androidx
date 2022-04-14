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
package androidx.health.data.client.permission

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import androidx.health.data.client.HealthDataClient.Companion.DEFAULT_PROVIDER_PACKAGE_NAME
import androidx.health.data.client.impl.converters.permission.toJetpackPermission
import androidx.health.data.client.impl.converters.permission.toProtoPermission
import androidx.health.platform.client.permission.Permission as ProtoPermission
import androidx.health.platform.client.service.HealthDataServiceConstants.ACTION_REQUEST_PERMISSIONS
import androidx.health.platform.client.service.HealthDataServiceConstants.KEY_GRANTED_PERMISSIONS_JETPACK
import androidx.health.platform.client.service.HealthDataServiceConstants.KEY_REQUESTED_PERMISSIONS_JETPACK

/**
 * An [ActivityResultContract] to request Health Data permissions.
 *
 * @param providerPackageName Optional provider package name for the backing implementation of
 * choice.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class HealthDataRequestPermissions(
    private val providerPackageName: String = DEFAULT_PROVIDER_PACKAGE_NAME,
) : ActivityResultContract<Set<Permission>, Set<Permission>>() {

    override fun createIntent(context: Context, input: Set<Permission>): Intent {
        require(input.isNotEmpty()) { "At least one permission is required!" }

        val protoPermissionList =
            input
                .asSequence()
                .map { ProtoPermission(it.toProtoPermission()) }
                .toCollection(ArrayList())
        return Intent(ACTION_REQUEST_PERMISSIONS).apply {
            putParcelableArrayListExtra(KEY_REQUESTED_PERMISSIONS_JETPACK, protoPermissionList)
            if (providerPackageName.isNotEmpty()) {
                setPackage(providerPackageName)
            }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Set<Permission> {
        return intent
            ?.getParcelableArrayListExtra<ProtoPermission>(KEY_GRANTED_PERMISSIONS_JETPACK)
            ?.asSequence()
            ?.map { it.proto.toJetpackPermission() }
            ?.toSet()
            ?: emptySet()
    }

    override fun getSynchronousResult(
        context: Context,
        input: Set<Permission>,
    ): SynchronousResult<Set<Permission>>? {
        return null
    }
}
