/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.camera2.pipe

import android.app.Activity
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.os.Bundle
import android.os.Trace
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import kotlinx.coroutines.runBlocking

/**
 * This is the main activity for the CameraPipe test application.
 */
class CameraPipeActivity : Activity() {
    private lateinit var cameraPipe: CameraPipe
    private lateinit var dataVisualizations: DataVisualizations
    private lateinit var ui: CameraPipeUi

    private var lastCameraId: CameraId? = null
    private var currentCamera: SimpleCamera? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("CXCP-App", "Activity onCreate")
        cameraPipe = (applicationContext as CameraPipeApplication).cameraPipe

        // This adjusts the UI to make the activity run a a full screen application.
        configureFullScreenCameraWindow()

        // Inflate the main ui for the camera activity.
        Trace.beginSection("CXCP-App#inflate")
        ui = CameraPipeUi.inflate(this)

        // Configure and wire up basic UI behaviors.
        ui.disableButton(ui.captureButton)
        ui.disableButton(ui.infoButton)
        ui.viewfinderText.visibility = View.VISIBLE
        ui.switchButton.setOnClickListener { startNextCamera() }
        Trace.endSection()

        // TODO: Update this to work with newer versions of the visualizations and to accept
        //   the CameraPipeUi object as a parameter.
        dataVisualizations = DataVisualizations(this)
    }

    override fun onStart() {
        super.onStart()
        Log.i("CXCP-App", "Activity onStart")

        val camera = currentCamera
        if (camera == null) {
            startNextCamera()
        } else {
            camera.start()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i("CXCP-App", "Activity onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.i("CXCP-App", "Activity onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.i("CXCP-App", "Activity onStop")
        currentCamera?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("CXCP-App", "Activity onDestroy")
        currentCamera?.close()
        dataVisualizations.close()
    }

    private fun startNextCamera() {
        Trace.beginSection("CXCP-App#startNextCamera")

        Trace.beginSection("CXCP-App#stopCamera")
        var camera = currentCamera
        camera?.stop()
        Trace.endSection()

        Trace.beginSection("CXCP-App#findNextCamera")
        val cameraId = runBlocking { findNextCamera(lastCameraId) }
        Trace.endSection()

        Trace.beginSection("CXCP-App#startCameraGraph")
        camera = SimpleCamera.create(cameraPipe, cameraId, ui.viewfinder, listOf())
        Trace.endSection()
        currentCamera = camera
        lastCameraId = cameraId
        ui.viewfinderText.text = camera.cameraInfoString()

        camera.start()
        Trace.endSection()

        Trace.endSection()
    }

    private suspend fun findNextCamera(lastCameraId: CameraId?): CameraId {
        val cameras: List<CameraId> = cameraPipe.cameras().ids()
        // By default, open the first back facing camera if no camera was previously configured.
        if (lastCameraId == null) {
            return cameras.firstOrNull {
                cameraPipe.cameras().getMetadata(it)[CameraCharacteristics.LENS_FACING] ==
                    CameraCharacteristics.LENS_FACING_BACK
            } ?: cameras.first()
        }

        // If a camera was previously open, select the next camera in the list of all cameras. It is
        // possible that the list of cameras contains only one camera, in which case this will return
        // the same camera as "currentCameraId"

        val lastCameraIndex = cameras.indexOf(lastCameraId)
        if (cameras.isEmpty() || lastCameraIndex == -1) {
            Log.e("CXCP-App", "Failed to find matching camera!")
            return cameras.first()
        }

        // When we reach the end of the list of cameras, loop.
        return cameras[(lastCameraIndex + 1) % cameras.size]
    }

    @Suppress("DEPRECATION")
    private fun configureFullScreenCameraWindow() {
        Trace.beginSection("CXCP-App#windowFlags")
        // Make the navigation bar semi-transparent.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
        )

        // Hide navigation to make the app full screen
        // TODO: Alter this to use window insets class when running on Android R
        val uiOptions = (
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        window.decorView.systemUiVisibility = uiOptions

        // Make portrait / landscape rotation seamless
        val windowParams: WindowManager.LayoutParams = window.attributes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            windowParams.rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_SEAMLESS
        } else {
            windowParams.rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_JUMPCUT
        }

        // Allow the app to draw over screen cutouts (notches, camera bumps, etc)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            windowParams.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.attributes = windowParams

        Trace.endSection()
    }
}