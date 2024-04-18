/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.camera.integration.testingtestapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.integration.testingtestapp.ui.theme.MultimoduleTemplateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ComposeCameraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MultimoduleTemplateTheme { Camera() }
            //            } else {
            //                Column {
            //                    val textToShow = if
            // (cameraPermissionState.status.shouldShowRationale) {
            //                        // If the user has denied the permission but the rationale can
            // be shown,
            //                        // then gently explain why the app requires this permission
            //                        "The camera is important for this app. Please grant the
            // permission."
            //                    } else {
            //                        // If it's the first time the user lands on this feature, or
            // the user
            //                        // doesn't want to be asked again for this permission, explain
            // that the
            //                        // permission is required
            //                        "Camera permission required for this feature to be available.
            // " +
            //                            "Please grant the permission"
            //                    }
            //                    Text(textToShow)
            //                    Button(onClick = { cameraPermissionState.launchPermissionRequest()
            // }) {
            //                        Text("Request permission")
            //                    }
            //                }
            //            }
        }
    }
}
