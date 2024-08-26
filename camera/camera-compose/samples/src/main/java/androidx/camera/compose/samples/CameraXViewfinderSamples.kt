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

package androidx.camera.compose.samples

import android.util.Size
import androidx.annotation.Sampled
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.camera.viewfinder.surface.ImplementationMode
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Suppress("unused", "UNUSED_PARAMETER")
@Sampled
fun CameraXViewfinderSample() {
    class PreviewViewModel : ViewModel() {
        private val _surfaceRequests = MutableStateFlow<SurfaceRequest?>(null)

        val surfaceRequests: StateFlow<SurfaceRequest?>
            get() = _surfaceRequests.asStateFlow()

        private fun produceSurfaceRequests(previewUseCase: Preview) {
            // Always publish new SurfaceRequests from Preview
            previewUseCase.setSurfaceProvider { newSurfaceRequest ->
                _surfaceRequests.value = newSurfaceRequest
            }
        }

        fun focusOnPoint(surfaceBounds: Size, x: Float, y: Float) {
            // Create point for CameraX's CameraControl.startFocusAndMetering() and submit...
        }

        // ...
    }

    @Composable
    fun MyCameraViewfinder(viewModel: PreviewViewModel, modifier: Modifier = Modifier) {
        val currentSurfaceRequest: SurfaceRequest? by viewModel.surfaceRequests.collectAsState()

        currentSurfaceRequest?.let { surfaceRequest ->

            // CoordinateTransformer for transforming from Offsets to Surface coordinates
            val coordinateTransformer = remember { MutableCoordinateTransformer() }

            CameraXViewfinder(
                surfaceRequest = surfaceRequest,
                implementationMode = ImplementationMode.EXTERNAL, // Can also use EMBEDDED
                modifier =
                    modifier.pointerInput(Unit) {
                        detectTapGestures {
                            with(coordinateTransformer) {
                                val surfaceCoords = it.transform()
                                viewModel.focusOnPoint(
                                    surfaceRequest.resolution,
                                    surfaceCoords.x,
                                    surfaceCoords.y
                                )
                            }
                        }
                    },
                coordinateTransformer = coordinateTransformer
            )
        }
    }
}
