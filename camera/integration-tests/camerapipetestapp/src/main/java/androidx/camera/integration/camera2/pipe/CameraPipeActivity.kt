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
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Trace
import android.util.Log
import android.util.Size
import android.view.View
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe

/** This is the main activity for the CameraPipe test application. */
@SuppressLint("RestrictedApiAndroidX")
class CameraPipeActivity : CameraPermissionActivity() {
    private lateinit var cameraPipe: CameraPipe
    private lateinit var dataVisualizations: DataVisualizations
    private lateinit var ui: CameraPipeUi
    private lateinit var cameraIdGroups: List<List<CameraId>>

    private var lastCameraIds: List<CameraId>? = null
    private var currentCameras: List<SimpleCamera>? = null
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

        val cameraDevices = cameraPipe.cameras()
        cameraIdGroups =
            cameraDevices.awaitCameraIds()!!.map { listOf(it) } +
                cameraDevices
                    .awaitConcurrentCameraIds()!!
                    .filter { it.size <= 2 }
                    .map { it.toList() }

        // TODO: Update this to work with newer versions of the visualizations and to accept
        //   the CameraPipeUi object as a parameter.
        dataVisualizations = DataVisualizations(this)
    }

    override fun onStart() {
        super.onStart()
        Log.i("CXCP-App", "Activity onStart")

        checkPermissionsAndRun(
            setOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        ) {
            val cameras = currentCameras
            if (cameras == null) {
                startNextCamera()
            } else {
                for (camera in cameras) {
                    camera.start()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i("CXCP-App", "Activity onResume")
        currentCameras?.let {
            for (camera in it) {
                camera.resume()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.i("CXCP-App", "Activity onPause")
        currentCameras?.let {
            for (camera in it) {
                camera.pause()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        Log.i("CXCP-App", "Activity onStop")
        currentCameras?.let {
            for (camera in it) {
                camera.stop()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("CXCP-App", "Activity onDestroy")
        currentCameras?.let {
            for (camera in it) {
                camera.close()
            }
        }
        dataVisualizations.close()
    }

    private fun startNextCamera() {
        Trace.beginSection("CXCP-App#startNextCamera")

        Trace.beginSection("CXCP-App#stopCamera")
        var cameras = currentCameras
        cameras?.let {
            for (camera in it) {
                camera.close()
            }
        }
        Trace.endSection()

        Trace.beginSection("CXCP-App#findNextCamera")
        val cameraIds = findNextCameraIdGroup(lastCameraIds)
        Trace.endSection()

        Trace.beginSection("CXCP-App#startCameraGraph")
        if (cameraIds.size == 1) {
            cameras =
                listOf(
                    SimpleCamera.create(
                        cameraPipe,
                        cameraIds.first(),
                        ui.viewfinder,
                        emptyList(),
                        operatingMode
                    )
                )
            ui.viewfinderText.text = cameras[0].cameraInfoString()
            ui.viewfinder2.visibility = View.INVISIBLE
            ui.viewfinderText2.visibility = View.INVISIBLE
        } else {
            cameras =
                SimpleCamera.create(
                    cameraPipe,
                    cameraIds,
                    listOf(ui.viewfinder, ui.viewfinder2),
                    listOf(Size(1280, 720), Size(1280, 720))
                )
            ui.viewfinderText.text = cameras[0].cameraInfoString()
            ui.viewfinderText2.text = cameras[1].cameraInfoString()
            ui.viewfinder2.visibility = View.VISIBLE
            ui.viewfinderText2.visibility = View.VISIBLE
        }
        Trace.endSection()
        currentCameras = cameras
        lastCameraIds = cameraIds

        for (camera in cameras) {
            camera.start()
        }
        Trace.endSection()

        Trace.endSection()
    }

    private fun findNextCameraIdGroup(lastCameraIdGroup: List<CameraId>?): List<CameraId> {
        // By default, open the first back facing camera if no camera was previously configured.
        if (lastCameraIdGroup == null) {
            return cameraIdGroups.first()
        }

        // If a camera was previously opened and the operating mode is NORMAL, return the same
        // camera but switch to HIGH_SPEED operating mode
        if (lastCameraIdGroup.size == 1 && operatingMode == CameraGraph.OperatingMode.NORMAL) {
            operatingMode = CameraGraph.OperatingMode.HIGH_SPEED
            return lastCameraIdGroup
        }

        // If the operating mode is not NORMAL, continue finding the next camera, which will
        // be opened in NORMAL operating mode
        operatingMode = CameraGraph.OperatingMode.NORMAL

        // If a camera was previously open, select the next camera in the list of all cameras. It is
        // possible that the list of cameras contains only one camera, in which case this will
        // return
        // the same camera as "currentCameraId"

        val lastCamerasIndex = cameraIdGroups.indexOf(lastCameraIdGroup)
        if (lastCamerasIndex == -1) {
            Log.e("CXCP-App", "Failed to find matching camera!")
            return cameraIdGroups.first()
        }

        // When we reach the end of the list of cameras, loop.
        return cameraIdGroups[(lastCamerasIndex + 1) % cameraIdGroups.size]
    }
}
