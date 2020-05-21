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

package androidx.camera.integration.uiwidgets

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.fragment_textureview.preview_textureview

/** A Fragment that displays a {@link PreviewView} with TextureView mode. */
class CameraFragment : Fragment() {

    companion object {
        fun newInstance() = CameraFragment()
        private const val TAG = "CameraFragment"
    }

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
        return inflater.inflate(R.layout.fragment_textureview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated")

        cameraProviderFuture.addListener(Runnable {
            cameraProvider = cameraProviderFuture.get()
            bindPreview()
        }, ContextCompat.getMainExecutor(this.context))
    }

    private fun bindPreview() {
        Log.d(TAG, "bindPreview")

        var preview = Preview.Builder()
            .setTargetName("Preview")
            .build()

        var cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        cameraProvider.bindToLifecycle(this, cameraSelector, preview)

        preview_textureview.preferredImplementationMode =
            PreviewView.ImplementationMode.TEXTURE_VIEW
        preview.setSurfaceProvider(preview_textureview.createSurfaceProvider())
    }
}
