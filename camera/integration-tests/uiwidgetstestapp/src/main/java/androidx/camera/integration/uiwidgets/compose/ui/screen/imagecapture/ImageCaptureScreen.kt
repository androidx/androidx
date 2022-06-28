/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.integration.uiwidgets.compose.ui.screen.imagecapture

import android.view.ViewGroup
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.integration.uiwidgets.compose.ui.screen.components.CameraControlButton
import androidx.camera.integration.uiwidgets.compose.ui.screen.components.CameraControlButtonPlaceholder
import androidx.camera.integration.uiwidgets.compose.ui.screen.components.CameraControlRow
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.FlipCameraAndroid
import androidx.compose.material.icons.sharp.Lens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun ImageCaptureScreen(
    modifier: Modifier = Modifier,
    state: ImageCaptureScreenState = rememberImageCaptureScreenState()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val localContext = LocalContext.current

    LaunchedEffect(key1 = state.lensFacing) {
        state.startCamera(context = localContext, lifecycleOwner = lifecycleOwner)
    }

    ImageCaptureScreen(
        modifier = modifier,
        zoomRatio = state.zoomRatio,
        linearZoom = state.linearZoom,
        onLinearZoomChange = state::setLinearZoom,
        isCameraReady = state.isCameraReady,
        hasFlashUnit = state.hasFlashUnit,
        flashModeIcon = state.flashModeIcon,
        onFlashModeIconClicked = state::toggleFlashMode,
        onFlipCameraIconClicked = state::toggleLensFacing,
        onImageCaptureIconClicked = {
            state.takePhoto(localContext)
        },
        onSurfaceProviderReady = state::setSurfaceProvider,
    )
}

@Composable
fun ImageCaptureScreen(
    modifier: Modifier,
    zoomRatio: Float,
    linearZoom: Float,
    onLinearZoomChange: (Float) -> Unit,
    isCameraReady: Boolean,
    hasFlashUnit: Boolean,
    flashModeIcon: ImageVector,
    onFlashModeIconClicked: () -> Unit,
    onFlipCameraIconClicked: () -> Unit,
    onImageCaptureIconClicked: () -> Unit,
    onSurfaceProviderReady: (SurfaceProvider) -> Unit,
) {
    val localContext = LocalContext.current

    // Saving an instance of PreviewView outside of AndroidView
    // This allows us to access properties of PreviewView (e.g. ViewPort and OutputTransform)
    // Allows us to support functionalities such as UseCaseGroup in bindToLifecycle()
    // This instance needs to be carefully used in controlled environments (e.g. LaunchedEffect)
    val previewView = remember {
        PreviewView(localContext).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            onSurfaceProviderReady(this.surfaceProvider)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView }
        )

        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.Bottom
        ) {

            // Display Zoom Slider only when Camera is ready
            if (isCameraReady) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f)) {
                        Slider(
                            value = linearZoom,
                            onValueChange = onLinearZoomChange
                        )
                    }

                    Text(
                        text = "%.2f x".format(zoomRatio),
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .background(Color.White)
                    )
                }
            }

            CameraControlRow {
                CameraControlButton(
                    imageVector = Icons.Sharp.FlipCameraAndroid,
                    contentDescription = "Toggle Camera Lens",
                    onClick = onFlipCameraIconClicked
                )

                CameraControlButton(
                    imageVector = Icons.Sharp.Lens,
                    contentDescription = "Image Capture",
                    modifier = Modifier
                        .padding(1.dp)
                        .border(1.dp, MaterialTheme.colors.onSecondary, CircleShape),
                    onClick = onImageCaptureIconClicked
                )

                if (hasFlashUnit) {
                    CameraControlButton(
                        imageVector = flashModeIcon,
                        contentDescription = "Toggle Flash Mode",
                        modifier = Modifier
                            .padding(1.dp)
                            .border(1.dp, MaterialTheme.colors.onSecondary, RectangleShape),
                        onClick = onFlashModeIconClicked
                    )
                } else {
                    CameraControlButtonPlaceholder()
                }
            }
        }
    }
}