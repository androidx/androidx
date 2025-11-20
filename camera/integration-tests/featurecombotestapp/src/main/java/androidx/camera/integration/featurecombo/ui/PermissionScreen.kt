/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.integration.featurecombo.ui

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun PermissionScreen(requiredPermissions: List<String>, onPermissionGranted: (Boolean) -> Unit) {
    val context = LocalContext.current
    var isPermissionMissing by remember {
        mutableStateOf(context.isPermissionMissing(requiredPermissions))
    }

    if (!isPermissionMissing) {
        onPermissionGranted(true)
        return
    }

    rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            onResult = { permissions ->
                var isPermissionStillMissing = false

                permissions.forEach { entry ->
                    if (!entry.value) {
                        Toast.makeText(
                                context,
                                "${entry.key} permission denied!",
                                Toast.LENGTH_SHORT,
                            )
                            .show()
                        isPermissionStillMissing = true
                        return@forEach
                    }
                }

                isPermissionMissing = isPermissionStillMissing
                onPermissionGranted(isPermissionMissing)
            },
        )
        .apply { SideEffect { launch(requiredPermissions.toTypedArray()) } }
}

/** Returns true if any of the required permissions is missing. */
private fun Context.isPermissionMissing(requiredPermissions: List<String>): Boolean {
    requiredPermissions.forEach {
        if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
            return true
        }
    }
    return false
}
