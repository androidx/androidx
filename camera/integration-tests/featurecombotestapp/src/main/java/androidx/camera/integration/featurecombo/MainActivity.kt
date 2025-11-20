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

package androidx.camera.integration.featurecombo

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.integration.featurecombo.MainActivity.Companion.REQUIRED_PERMISSIONS
import androidx.camera.integration.featurecombo.ui.CameraScreen
import androidx.camera.integration.featurecombo.ui.PermissionScreen
import androidx.camera.integration.featurecombo.ui.theme.FcqTestAppTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

const val TAG = "CamXFcqMainActivity"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.updatePadding(
                top = insets.top,
                bottom = insets.bottom,
                left = insets.left,
                right = insets.right,
            )

            WindowInsetsCompat.CONSUMED
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window?.colorMode = ActivityInfo.COLOR_MODE_HDR
        }

        setContent { FcqTestAppTheme { RootScreen() } }
    }

    companion object {
        const val INTENT_EXTRA_CAMERA_IMPLEMENTATION = "camera_implementation"

        val REQUIRED_PERMISSIONS = listOf(android.Manifest.permission.CAMERA)
    }
}

@Composable
fun RootScreen() {
    var isPermissionGranted by remember { mutableStateOf(false) }

    PermissionScreen(REQUIRED_PERMISSIONS) { isPermissionGranted = true }

    if (isPermissionGranted) {
        Log.d(TAG, "Permissions granted.")
        CameraScreen()
    } else {
        Log.e(TAG, "Permissions not granted!.")
    }
}
