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

package androidx.camera.integration.uiwidgets.compose.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun PermissionsUI(
    permissions: Array<String>,
    checkAllPermissionGranted: (Array<String>) -> Boolean,
    content: @Composable () -> Unit
) {
    var allPermissionsGranted by remember { mutableStateOf(checkAllPermissionGranted(permissions)) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        allPermissionsGranted = results.all {
            it.value
        }
    }

    LaunchedEffect(key1 = permissions) {
        if (!allPermissionsGranted) {
            launcher.launch(permissions)
        }
    }

    if (allPermissionsGranted) {
        content()
    } else {
        Text("Permissions are not granted to the app.")
    }
}
