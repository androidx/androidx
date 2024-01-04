/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.health.connect.client.contracts

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.permission.HealthPermissionsRequestAppContract
import androidx.health.connect.client.permission.platform.HealthPermissionsRequestModuleContract

/**
 * An [ActivityResultContract] to request Health permissions.
 *
 * It receives a set of permissions as input and returns a set with the granted permissions as
 * output.
 */
class HealthPermissionsRequestContract(
    providerPackageName: String = HealthConnectClient.DEFAULT_PROVIDER_PACKAGE_NAME
) : ActivityResultContract<Set<String>, Set<String>>() {

    private val delegate: ActivityResultContract<Set<String>, Set<String>> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HealthPermissionsRequestModuleContract()
        } else {
            HealthPermissionsRequestAppContract(providerPackageName)
        }

    /**
     * Creates an intent to request HealthConnect permissions. It receives as [input] a [Set] of
     * HealthConnect permissions.
     *
     * @param context the context
     * @param input the health permission strings to request permissions for
     * @see ActivityResultContract.createIntent
     */
    override fun createIntent(context: Context, input: Set<String>): Intent {
        require(input.all { it.startsWith(HealthPermission.PERMISSION_PREFIX) }) {
            "Unsupported health connect permission"
        }
        require(input.isNotEmpty()) { "At least one permission is required!" }
        return delegate.createIntent(context, input)
    }

    /**
     * Converts the activity result into a [Set] of granted permissions. This will be a subset of
     * [Set] passed in [createIntent].
     *
     * @see ActivityResultContract.parseResult
     */
    override fun parseResult(resultCode: Int, intent: Intent?): Set<String> {
        return delegate.parseResult(resultCode, intent)
    }
}
