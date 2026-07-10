/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.extensions.samples

import android.content.Context
import androidx.annotation.Sampled
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionSessionConfig
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.lifecycle.LifecycleOwner

/** Sample code for [ExtensionsManager] to bind UseCases with BOKEH mode. */
// START(bindUseCasesWithBokehMode)
@Sampled
suspend fun bindUseCasesWithBokehMode(context: Context, lifecycleOwner: LifecycleOwner) {
    // Create a camera provider.
    val cameraProvider = ProcessCameraProvider.awaitInstance(context)

    // Retrieve the ExtensionsManager instance.
    val extensionsManager = ExtensionsManager.getInstance(context, cameraProvider)

    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    // Query if extension is available.
    if (extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.BOKEH)) {
        // Create an ExtensionSessionConfig.
        val imageCapture = ImageCapture.Builder().build()
        val preview = Preview.Builder().build()
        val sessionConfig =
            ExtensionSessionConfig(ExtensionMode.BOKEH, extensionsManager, imageCapture, preview)
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, sessionConfig)
    }
}
// END(bindUseCasesWithBokehMode)
