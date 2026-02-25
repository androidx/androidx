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
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.integration.uiwidgets.databinding.FragmentTextureviewBinding
import androidx.camera.integration.uiwidgets.viewpager.BaseActivity.Companion.COMPATIBLE_MODE
import androidx.camera.integration.uiwidgets.viewpager.BaseActivity.Companion.PERFORMANCE_MODE
import androidx.camera.lifecycle.ExperimentalCameraProviderConfiguration
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView.ImplementationMode
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CountDownLatch

/** A Fragment that displays a {@link PreviewView} with TextureView mode. */
class CameraFragment : Fragment() {

    companion object {
        fun newInstance() = CameraFragment()

        private const val TAG = "CameraFragment"
    }

    private var _binding: FragmentTextureviewBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraProvider: ProcessCameraProvider

    // for testing preview updates
    private var previewUpdatingLatch: CountDownLatch? = null

    @OptIn(ExperimentalCameraProviderConfiguration::class)
    override fun onAttach(context: Context) {
        super.onAttach(context)
        cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
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
                val lifecycleOwner = viewLifecycleOwnerLiveData.value
                if (
                    lifecycleOwner != null &&
                        lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED
                ) {
                    bindPreview()
                } else {
                    Log.d(TAG, "Skip camera setup since the lifecycle is closed")
                }
            },
            ContextCompat.getMainExecutor(requireContext()),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bindPreview() {
        Log.d(TAG, "bindPreview")

        val previewBuilder = Preview.Builder()
        previewBuilder.addCaptureCompletedCallback()
        val preview = previewBuilder.setTargetName("Preview").build()

        cameraProvider.bindToLifecycle(this, getCameraSelector(), preview)

        binding.previewTextureview.implementationMode = getImplementationMode()
        preview.setSurfaceProvider(binding.previewTextureview.surfaceProvider)
    }

    private fun getCameraSelector(): CameraSelector {
        val lensFacing =
            (requireActivity() as BaseActivity)
                .intent
                .getIntExtra(BaseActivity.INTENT_LENS_FACING, CameraSelector.LENS_FACING_BACK)
        return CameraSelector.Builder().requireLensFacing(lensFacing).build()
    }

    /**
     * Returns the implementation mode from the intent, or return the compatibility mode if not set.
     */
    private fun getImplementationMode(): ImplementationMode {
        val mode =
            (requireActivity() as BaseActivity)
                .intent
                .getIntExtra(BaseActivity.INTENT_IMPLEMENTATION_MODE, COMPATIBLE_MODE)

        return when (mode) {
            PERFORMANCE_MODE -> ImplementationMode.PERFORMANCE
            else -> ImplementationMode.COMPATIBLE
        }
    }

    /**
     * Implements preview updating latch with interop to workaround the situation that SurfaceView's
     * content can not be got.
     */
    @OptIn(ExperimentalCamera2Interop::class)
    private fun Preview.Builder.addCaptureCompletedCallback() {
        val captureCallback =
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult,
                ) {
                    super.onCaptureCompleted(session, request, result)

                    if (previewUpdatingLatch != null) {
                        previewUpdatingLatch!!.countDown()
                    }
                }
            }

        Camera2Interop.Extender(this).setSessionCaptureCallback(captureCallback)
    }

    @VisibleForTesting
    fun setPreviewUpdatingLatch(latch: CountDownLatch) {
        previewUpdatingLatch = latch
    }
}
