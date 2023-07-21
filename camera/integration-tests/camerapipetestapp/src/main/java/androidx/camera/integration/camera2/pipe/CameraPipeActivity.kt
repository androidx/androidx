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

import android.Manifest
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import android.os.Trace
import android.util.Log
import android.view.View
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import kotlinx.coroutines.runBlocking

/**
 * This is the main activity for the CameraPipe test application.
 */
class CameraPipeActivity : CameraPermissionActivity() {
    private lateinit var cameraPipe: CameraPipe
    private lateinit var dataVisualizations: DataVisualizations
    private lateinit var ui: CameraPipeUi

    private var lastCameraId: CameraId? = null
    private var currentCamera: SimpleCamera? = null
    private var operatingMode: CameraGraph.OperatingMode = CameraGraph.OperatingMode.NORMAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("CXCP-App", "Activity onCreate")
        cameraPipe = (applicationContext as CameraPipeApplication).cameraPipe

        // This adjusts the UI to make the activity run a a full screen application.
        configureFullScreenCameraWindow(this)

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

        checkPermissionsAndRun(
            setOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        ) {
            val camera = currentCamera
            if (camera == null) {
                startNextCamera()
            } else {
                camera.start()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i("CXCP-App", "Activity onResume")
        currentCamera?.resume()
    }

    override fun onPause() {
        super.onPause()
        Log.i("CXCP-App", "Activity onPause")
        currentCamera?.pause()
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
        camera = SimpleCamera.create(cameraPipe, cameraId, ui.viewfinder, listOf(), operatingMode)
        Trace.endSection()
        currentCamera = camera
        lastCameraId = cameraId
        ui.viewfinderText.text = camera.cameraInfoString()

        camera.start()
        Trace.endSection()

        Trace.endSection()
    }

    private suspend fun findNextCamera(lastCameraId: CameraId?): CameraId {
        val cameras = cameraPipe.cameras().getCameraIds()
        checkNotNull(cameras) { "Unable to load CameraIds from CameraPipe" }

        // By default, open the first back facing camera if no camera was previously configured.
        if (lastCameraId == null) {
            for (id in cameras) {
                val metadata = cameraPipe.cameras().getCameraMetadata(id)
                if (metadata != null && metadata[CameraCharacteristics.LENS_FACING] ==
                    CameraCharacteristics.LENS_FACING_BACK
                ) {
                    return id
                }
            }
            return cameras.first()
        }

        // If a camera was previously opened and the operating mode is NORMAL, return the same
        // camera but switch to HIGH_SPEED operating mode
        if (operatingMode == CameraGraph.OperatingMode.NORMAL) {
            operatingMode = CameraGraph.OperatingMode.HIGH_SPEED
            return lastCameraId
        }

        // If the operating mode is not NORMAL, continue finding the next camera, which will
        // be opened in NORMAL operating mode
        operatingMode = CameraGraph.OperatingMode.NORMAL

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
}