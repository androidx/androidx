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

package androidx.camera.integration.uiwidgets.viewpager

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.integration.uiwidgets.databinding.FragmentTextureviewBinding
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.google.common.util.concurrent.ListenableFuture

/** A Fragment that displays a {@link PreviewView} with TextureView mode. */
class CameraFragment : Fragment() {

    companion object {
        fun newInstance() = CameraFragment()
        private const val TAG = "CameraFragment"
    }

    private var _binding: FragmentTextureviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraProvider: ProcessCameraProvider

    override fun onAttach(context: Context) {
        super.onAttach(context)
        cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTextureviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated")
        (requireActivity() as BaseActivity).previewView = binding.previewTextureview

        cameraProviderFuture.addListener(
            Runnable {
                cameraProvider = cameraProviderFuture.get()
                if (viewLifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
                    bindPreview()
                } else {
                    Log.d(TAG, "Skip camera setup since the lifecycle is closed")
                }
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bindPreview() {
        Log.d(TAG, "bindPreview")

        val preview = Preview.Builder()
            .setTargetName("Preview")
            .build()

        cameraProvider.bindToLifecycle(this, getCameraSelector(), preview)

        binding.previewTextureview.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        preview.setSurfaceProvider(binding.previewTextureview.surfaceProvider)
    }

    private fun getCameraSelector(): CameraSelector {
        val lensFacing = (requireActivity() as BaseActivity).intent.getIntExtra(
            BaseActivity.INTENT_LENS_FACING,
            CameraSelector
                .LENS_FACING_BACK
        )
        return CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
    }
}
