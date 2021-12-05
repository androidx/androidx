/*
 * Copyright 2021 The Android Open Source Project
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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.integration.view.MainActivity.CAMERA_DIRECTION_BACK
import androidx.camera.integration.view.MainActivity.CAMERA_DIRECTION_FRONT
import androidx.camera.integration.view.MainActivity.INTENT_EXTRA_CAMERA_DIRECTION
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner

class ComposeUiFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val cameraProvider = ProcessCameraProvider.getInstance(requireContext()).get()
        val previewView = PreviewView(requireContext())

        return ComposeView(requireContext()).apply {
            setContent {
                AddPreviewView(
                    cameraProvider,
                    previewView
                )
            }
        }
    }

    @Composable
    private fun AddPreviewView(cameraProvider: ProcessCameraProvider, previewView: PreviewView) {
        previewView.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

        AndroidView(
            factory = {
                previewView
            }
        )

        CameraXExecutors.mainThreadExecutor().execute {
            bindPreview(cameraProvider, this, previewView)
        }
    }

    private fun bindPreview(
        cameraProvider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
    ) {
        val preview = Preview.Builder().build()
        val cameraSelector = getCameraSelector()

        preview.setSurfaceProvider(previewView.surfaceProvider)
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
    }

    private fun getCameraSelector(): CameraSelector {
        var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        val bundle: Bundle? = requireActivity().intent.extras
        if (bundle != null) {
            cameraSelector =
                when (bundle.getString(INTENT_EXTRA_CAMERA_DIRECTION, CAMERA_DIRECTION_BACK)) {
                    CAMERA_DIRECTION_BACK -> CameraSelector.DEFAULT_BACK_CAMERA
                    CAMERA_DIRECTION_FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> CameraSelector.DEFAULT_BACK_CAMERA
                }
        }
        return cameraSelector
    }
}
