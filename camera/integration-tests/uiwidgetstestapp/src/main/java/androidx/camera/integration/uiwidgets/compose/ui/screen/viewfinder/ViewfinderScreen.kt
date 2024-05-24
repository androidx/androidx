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

package androidx.camera.integration.uiwidgets.compose.ui.screen.viewfinder

import android.view.ViewGroup
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.integration.uiwidgets.compose.ui.screen.components.CameraControlButton
import androidx.camera.integration.uiwidgets.compose.ui.screen.components.CameraControlButtonPlaceholder
import androidx.camera.integration.uiwidgets.compose.ui.screen.components.CameraControlRow
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.FlipCameraAndroid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

private const val TAG = "ViewfinderScreen"

/**
 * Simple Viewfinder screen with zoom and ability to flip camera.
 *
 * This is to test the CameraX Preview Use Case can be be displayed with a composable Viewfinder.
 */
@Composable
fun ViewfinderScreen(
    modifier: Modifier = Modifier,
    state: ViewfinderScreenState = rememberViewfinderScreenState()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val localContext = LocalContext.current

    LaunchedEffect(key1 = state.lensFacing) {
        state.startCamera(context = localContext, lifecycleOwner = lifecycleOwner)
    }

    ViewfinderScreen(
        modifier = modifier,
        zoomRatio = state.zoomRatio,
        linearZoom = state.linearZoom,
        onLinearZoomChange = state::setLinearZoom,
        isCameraReady = state.isCameraReady,
        onFlipCameraIconClicked = state::toggleLensFacing,
        onSurfaceProviderReady = state::setSurfaceProvider,
        onTouch = state::startTapToFocus
    )
}

// It uses OutputTransform hence we need to @SuppressWarnings to get past the linter
@SuppressWarnings("UnsafeOptInUsageError")
@Composable
fun ViewfinderScreen(
    modifier: Modifier,
    zoomRatio: Float,
    linearZoom: Float,
    onLinearZoomChange: (Float) -> Unit,
    isCameraReady: Boolean,
    onFlipCameraIconClicked: () -> Unit,
    onSurfaceProviderReady: (SurfaceProvider) -> Unit,
    onTouch: (MeteringPoint) -> Unit,
) {
    val localContext = LocalContext.current

    // Saving an instance of PreviewView outside of AndroidView
    // This allows us to access properties of PreviewView (e.g. ViewPort and OutputTransform)
    // Allows us to support functionalities such as UseCaseGroup in bindToLifecycle()
    // This instance needs to be carefully used in controlled environments (e.g. LaunchedEffect)
    val previewView = remember {
        PreviewView(localContext).apply {
            layoutParams =
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

            // Uses TextureView. Required by MLKitAnalyzer to acquire the correct OutputTransform
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE

            onSurfaceProviderReady(this.surfaceProvider)

            setOnTouchListener { view, motionEvent ->
                val meteringPointFactory = (view as PreviewView).meteringPointFactory
                val meteringPoint = meteringPointFactory.createPoint(motionEvent.x, motionEvent.y)
                onTouch(meteringPoint)

                return@setOnTouchListener true
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(factory = { previewView })

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
                        Slider(value = linearZoom, onValueChange = onLinearZoomChange)
                    }

                    Text(
                        text = "%.2f x".format(zoomRatio),
                        modifier = Modifier.padding(horizontal = 10.dp).background(Color.White)
                    )
                }
            }

            CameraControlRow {
                CameraControlButton(
                    imageVector = Icons.Sharp.FlipCameraAndroid,
                    contentDescription = "Toggle Camera Lens",
                    onClick = onFlipCameraIconClicked
                )

                // Placeholder for where capture button would reside
                CameraControlButtonPlaceholder()

                // Placeholder for where flash button would reside
                CameraControlButtonPlaceholder()
            }
        }
    }
}
