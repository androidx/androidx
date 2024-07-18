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

package androidx.camera.integration.uiwidgets.compose.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector

// Contains each destination screen as an enum
// and associates an icon to show in the navigation tabs
enum class ComposeCameraScreen(
    val icon: ImageVector
) {
    ImageCapture(
        icon = Icons.Filled.CameraAlt
    ),
    VideoCapture(
        icon = Icons.Filled.Videocam
    );

    companion object {
        fun fromRoute(route: String?, defaultRoute: ComposeCameraScreen): ComposeCameraScreen {
            return when (route?.substringBefore("/")) {
                ImageCapture.name -> ImageCapture
                VideoCapture.name -> VideoCapture
                null -> defaultRoute
                else -> throw IllegalArgumentException("Route $route is not recognized.")
            }
        }
    }
}
