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
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.integration.view.MainActivity.CAMERA_DIRECTION_BACK
import androidx.camera.integration.view.MainActivity.CAMERA_DIRECTION_FRONT
import androidx.camera.integration.view.MainActivity.INTENT_EXTRA_CAMERA_DIRECTION
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment

/** A fragment that demonstrates how to use [ComposeView] to display a [PreviewView]. */
class ComposeUiFragment : Fragment() {

    private var currentScaleType = PreviewView.ScaleType.FILL_CENTER

    private lateinit var cameraController: LifecycleCameraController
    private lateinit var toneMappingEffect: ToneMappingSurfaceEffect
    private var hasEffect = false
    private var lensFacing = LENS_FACING_BACK

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bundle: Bundle? = requireActivity().intent.extras
        if (bundle != null) {
            val scaleTypeId =
                bundle.getInt(
                    MainActivity.INTENT_EXTRA_SCALE_TYPE,
                    MainActivity.DEFAULT_SCALE_TYPE_ID
                )
            currentScaleType = PreviewView.ScaleType.values()[scaleTypeId]

            lensFacing =
                when (bundle.getString(INTENT_EXTRA_CAMERA_DIRECTION, CAMERA_DIRECTION_BACK)) {
                    CAMERA_DIRECTION_BACK -> LENS_FACING_BACK
                    CAMERA_DIRECTION_FRONT -> LENS_FACING_FRONT
                    else -> LENS_FACING_BACK
                }
        }
        val previewView = PreviewView(requireContext())
        previewView.scaleType = currentScaleType

        toneMappingEffect =
            ToneMappingSurfaceEffect(CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE)

        cameraController = LifecycleCameraController(requireContext())
        cameraController.setEnabledUseCases(
            CameraController.VIDEO_CAPTURE or CameraController.IMAGE_CAPTURE
        )
        previewView.controller = cameraController
        updateCameraOrientation()
        cameraController.bindToLifecycle(viewLifecycleOwner)

        return ComposeView(requireContext()).apply { setContent { AddPreviewView(previewView) } }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        toneMappingEffect.release()
    }

    private fun onToggleCamera() {
        lensFacing = if (lensFacing == LENS_FACING_BACK) LENS_FACING_FRONT else LENS_FACING_BACK
        updateCameraOrientation()
    }

    private fun onToggleEffect() {
        hasEffect =
            if (hasEffect) {
                cameraController.clearEffects()
                false
            } else {
                cameraController.setEffects(setOf(toneMappingEffect))
                true
            }
    }

    private fun updateCameraOrientation() {
        if (lensFacing == LENS_FACING_BACK) {
            cameraController.cameraSelector = DEFAULT_BACK_CAMERA
        } else {
            cameraController.cameraSelector = DEFAULT_FRONT_CAMERA
        }
    }

    private fun onTakePicture() {
        TODO("Not yet implemented")
    }

    private fun onRecord() {
        TODO("Not yet implemented")
    }

    @Composable
    private fun AddPreviewView(previewView: PreviewView) {
        previewView.layoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(factory = { previewView })
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                    // Distribute buttons with spacing
                ) {
                    Button(
                        onClick = ::onToggleEffect,
                    ) {
                        Text("Effect")
                    }
                    Button(onClick = ::onToggleCamera) { Text("Toggle") }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Button(onClick = ::onTakePicture) { Text("Capture") }
                    Button(onClick = ::onRecord) { Text("Record") }
                }
            }
        }
    }
}
