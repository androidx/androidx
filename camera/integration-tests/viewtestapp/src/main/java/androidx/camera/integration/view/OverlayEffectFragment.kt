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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.effects.OverlayEffect
import androidx.camera.integration.view.effects.BouncyLogoEffect
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.fragment.app.Fragment

/** Fragment for testing effects integration. */
class OverlayEffectFragment : Fragment() {

    private lateinit var cameraController: LifecycleCameraController
    private lateinit var previewView: PreviewView
    private lateinit var bouncyLogoEffect: OverlayEffect

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
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bouncyLogoEffect.close()
    }
}
