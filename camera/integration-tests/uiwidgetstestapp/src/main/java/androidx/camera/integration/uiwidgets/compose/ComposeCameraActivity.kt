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

package androidx.camera.integration.uiwidgets.compose

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.integration.uiwidgets.compose.ui.ComposeCameraApp
import androidx.camera.integration.uiwidgets.compose.ui.PermissionsUI
import androidx.camera.integration.uiwidgets.compose.ui.navigation.ComposeCameraScreen
import androidx.camera.view.PreviewView.StreamState
import androidx.core.content.ContextCompat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ComposeCameraActivity : ComponentActivity() {

    // Variables for testing StreamState changes in PreviewView
    private var expectedScreen: ComposeCameraScreen = ComposeCameraScreen.ImageCapture
    private var expectedStreamState: StreamState = StreamState.STREAMING
    private var latchForState: CountDownLatch = CountDownLatch(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PermissionsUI(
                permissions = REQUIRED_PERMISSIONS,
                checkAllPermissionGranted = {
                    checkAllPermissionsGranted(it)
                }
            ) {
                ComposeCameraApp(onStreamStateChange = this::onStreamStateChange)
            }
        }
    }

    private fun checkAllPermissionsGranted(permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Saves the expected ComposeCameraScreen and StreamState for testing PreviewView
    // Once saved, this method waits to be notified of StreamState changes
    // Used to assert that PreviewView is streaming within reasonable timeout
    fun waitForStreamState(
        expectedScreen: ComposeCameraScreen,
        expectedState: StreamState
    ): Boolean {
        this.expectedScreen = expectedScreen
        expectedStreamState = expectedState
        latchForState = CountDownLatch(1)
        return latchForState.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS)
    }

    // Callback to observe changes in PreviewView.StreamState happening in some ComposeCameraScreen
    // Used to observe changes in StreamState when Composables render (during testing)
    private fun onStreamStateChange(screen: ComposeCameraScreen, streamState: StreamState) {
        // StreamState change not coming from expected screen
        if (screen != expectedScreen) {
            return
        }

        when (streamState) {
            StreamState.STREAMING -> {
                Log.d(TAG, "PreviewView.StreamState.STREAMING from ${screen.name}")
                if (expectedStreamState == StreamState.STREAMING) {
                    latchForState.countDown()
                }
            }
            StreamState.IDLE -> {
                Log.d(TAG, "PreviewView.StreamState.IDLE in ${screen.name}")
                if (expectedStreamState == StreamState.IDLE) {
                    latchForState.countDown()
                }
            }
            else -> {
                Log.e(TAG, "Wrong PreviewView.StreamState in ${screen.name}! Return IDLE")
            }
        }
    }

    companion object {
        private const val TAG = "ComposeCameraActivity"
        private const val LATCH_TIMEOUT: Long = 5000
        val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}
