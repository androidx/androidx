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
package androidx.health.connect.client.permission.platform

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.annotation.RestrictTo

/**
 * An [ActivityResultContract] to request Health Connect system permissions.
 *
 * @see androidx.activity.ComponentActivity.registerForActivityResult
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class HealthPermissionsRequestModuleContract :
    ActivityResultContract<Set<String>, Set<String>>() {

    private val requestPermissions = RequestMultiplePermissions()

    override fun createIntent(context: Context, input: Set<String>): Intent =
        requestPermissions.createIntent(context, input.toTypedArray())

    override fun parseResult(resultCode: Int, intent: Intent?): Set<String> =
        requestPermissions.parseResult(resultCode, intent).filterValues { it }.keys

    override fun getSynchronousResult(
        context: Context,
        input: Set<String>,
    ): SynchronousResult<Set<String>>? =
        requestPermissions.getSynchronousResult(context, input.toTypedArray())?.let { result ->
            SynchronousResult(result.value.filterValues { it }.keys)
        }
}
