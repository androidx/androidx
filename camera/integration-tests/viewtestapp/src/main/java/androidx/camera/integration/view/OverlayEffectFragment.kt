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
package androidx.camera.integration.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.ImageCapture
import androidx.camera.effects.OverlayEffect
import androidx.camera.integration.view.effects.BouncyLogoEffect
import androidx.camera.integration.view.util.takePicture
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.fragment.app.Fragment
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Fragment for testing effects integration. */
class OverlayEffectFragment : Fragment() {
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    private lateinit var cameraController: LifecycleCameraController
    private lateinit var previewView: PreviewView
    private lateinit var bouncyLogoEffect: OverlayEffect

    private lateinit var flashModeButton: Button
    private lateinit var onDiskCheckBox: CheckBox

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment.
        val view = inflater.inflate(R.layout.overlay_effect_view, container, false)
        previewView = view.findViewById(R.id.preview_view)

        // Create overlay effect
        bouncyLogoEffect =
            BouncyLogoEffect(
                PREVIEW or OverlayEffect.IMAGE_CAPTURE or OverlayEffect.VIDEO_CAPTURE,
                "CameraX",
                previewView
            )

        // Set up the camera controller.
        cameraController = LifecycleCameraController(requireContext())
        cameraController.setEnabledUseCases(
            CameraController.VIDEO_CAPTURE or CameraController.IMAGE_CAPTURE
        )
        cameraController.setEffects(setOf(bouncyLogoEffect))
        previewView.controller = cameraController
        cameraController.bindToLifecycle(viewLifecycleOwner)

        flashModeButton = view.findViewById(R.id.flash_mode)
        flashModeButton.setOnClickListener {
            cameraController.setImageCaptureFlashMode(
                when (cameraController.getImageCaptureFlashMode()) {
                    ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                    ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                    else ->
                        throw IllegalStateException(
                            "Invalid flash mode: ${cameraController.getImageCaptureFlashMode()}"
                        )
                }
            )
            flashModeButton.setText(getFlashModeTextResId())
        }
        flashModeButton.setText(getFlashModeTextResId())

        onDiskCheckBox = view.findViewById(R.id.on_disk)
        view.findViewById<View>(R.id.capture).setOnClickListener {
            cameraController.takePicture(
                requireContext(),
                executorService,
                ::toast,
                onDiskCheckBox::isChecked
            )
        }

        return view
    }

    private fun getFlashModeTextResId() =
        when (cameraController.getImageCaptureFlashMode()) {
            ImageCapture.FLASH_MODE_AUTO -> R.string.flash_mode_auto
            ImageCapture.FLASH_MODE_ON -> R.string.flash_mode_on
            ImageCapture.FLASH_MODE_SCREEN -> R.string.flash_mode_screen
            ImageCapture.FLASH_MODE_OFF -> R.string.flash_mode_off
            else ->
                throw java.lang.IllegalStateException(
                    "Invalid flash mode: ${cameraController.getImageCaptureFlashMode()}"
                )
        }

    private fun toast(message: String?) {
        activity?.runOnUiThread {
            if (isAdded) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
        Log.d(TAG, message!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bouncyLogoEffect.close()
    }

    private companion object {
        const val TAG: String = "CameraCtrlFragment"
    }
}
