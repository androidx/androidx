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

package androidx.camera.integration.view

import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ToggleButton
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning

/**
 * Fragment for testing MLKit integration.
 */
class MlKitFragment : Fragment() {

    private lateinit var cameraController: LifecycleCameraController
    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var toggle: ToggleButton
    private var barcodeScanner: BarcodeScanner? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Set up UI.
        val view = inflater.inflate(R.layout.mlkit_view, container, false)
        previewView = view.findViewById(R.id.preview_view)
        overlayView = view.findViewById(R.id.overlay_view)
        toggle = view.findViewById(R.id.toggle_camera)
        toggle.setOnCheckedChangeListener { _, _ -> updateCameraOrientation() }
        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        // This button resets the pipeline. This is for testing when the detector is closed,
        // MlKitAnalyzer handles it correctly.
        view.findViewById<Button>(R.id.restart).setOnClickListener { clearAndSetAnalyzer() }

        // Set up CameraX.
        cameraController = LifecycleCameraController(requireContext())
        cameraController.bindToLifecycle(viewLifecycleOwner)
        previewView.controller = cameraController
        clearAndSetAnalyzer()

        // Update states to match UI.
        updateCameraOrientation()
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        barcodeScanner?.close()
    }

    private fun updateCameraOrientation() {
        cameraController.cameraSelector =
            if (toggle.isChecked) DEFAULT_BACK_CAMERA else DEFAULT_FRONT_CAMERA
    }

    private fun clearAndSetAnalyzer() {
        barcodeScanner?.close()
        barcodeScanner = BarcodeScanning.getClient()

        cameraController.clearImageAnalysisAnalyzer()
        val scanner = barcodeScanner!!
        cameraController.setImageAnalysisAnalyzer(mainThreadExecutor(),
            MlKitAnalyzer(
                listOf(scanner),
                COORDINATE_SYSTEM_VIEW_REFERENCED,
                mainThreadExecutor()
            ) { result ->
                val barcodes = result.getValue(scanner)
                if (barcodes != null && barcodes.size > 0) {
                    overlayView.setTileRect(RectF(barcodes[0].boundingBox))
                    overlayView.invalidate()
                }
            })
    }
}
