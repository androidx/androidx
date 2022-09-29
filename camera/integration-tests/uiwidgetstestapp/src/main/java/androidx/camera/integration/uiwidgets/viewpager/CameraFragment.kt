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
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.integration.uiwidgets.databinding.FragmentTextureviewBinding
import androidx.camera.lifecycle.ExperimentalCameraProviderConfiguration
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
        const val KEY_CAMERA_IMPLEMENTATION = "camera_implementation"
        const val KEY_CAMERA_IMPLEMENTATION_NO_HISTORY = "camera_implementation_no_history"
        const val CAMERA2_IMPLEMENTATION_OPTION = "camera2"
        const val CAMERA_PIPE_IMPLEMENTATION_OPTION = "camera_pipe"
        private var cameraImpl: String? = null
    }

    private var _binding: FragmentTextureviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraProvider: ProcessCameraProvider

    @OptIn(ExperimentalCameraProviderConfiguration::class)
    override fun onAttach(context: Context) {
        super.onAttach(context)
        val newCameraImpl = activity?.intent?.getStringExtra(KEY_CAMERA_IMPLEMENTATION)
        Log.d(TAG, "Set up cameraImpl: $newCameraImpl")
        if (!TextUtils.isEmpty(newCameraImpl) && newCameraImpl != cameraImpl) {
            try {
                Log.d(TAG, "ProcessCameraProvider initialize using $newCameraImpl")
                ProcessCameraProvider.configureInstance(
                    when (newCameraImpl) {
                        CAMERA2_IMPLEMENTATION_OPTION -> Camera2Config.defaultConfig()
                        CAMERA_PIPE_IMPLEMENTATION_OPTION -> CameraPipeConfig.defaultConfig()
                        else -> Camera2Config.defaultConfig()
                    }
                )
                cameraImpl = newCameraImpl
            } catch (e: IllegalStateException) {
                throw IllegalStateException(
                    "WARNING: CameraX is currently configured to a different implementation " +
                        "this would have resulted in unexpected behavior.", e
                )
            }
        }

        activity?.intent?.let { intent ->
            if (intent.getBooleanExtra(KEY_CAMERA_IMPLEMENTATION_NO_HISTORY, false)) {
                activity?.intent = Intent(intent).apply {
                    removeExtra(KEY_CAMERA_IMPLEMENTATION)
                    removeExtra(KEY_CAMERA_IMPLEMENTATION_NO_HISTORY)
                }
                cameraImpl = null
            }
        }

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
