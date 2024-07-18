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

import android.graphics.Rect
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.integration.uiwidgets.compose.ui.screen.components.CameraControlButton
import androidx.camera.integration.uiwidgets.compose.ui.screen.components.CameraControlButtonPlaceholder
import androidx.camera.integration.uiwidgets.compose.ui.screen.components.CameraControlRow
import androidx.camera.view.PreviewView
import androidx.camera.view.transform.OutputTransform
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Observer

private const val TAG = "ImageCaptureScreen"

// ImageCaptureScreen with QR-Code Overlay (Supports Preview + ImageCapture + ImageAnalysis)
// Screen provides ImageCapture functionality and draws a bounding box around a detected QR Code
// This is to ensure that the combination of use cases can be well supported in Compose
// It uses OutputTransform hence we need to @SuppressWarnings to get past the linter
@SuppressWarnings("UnsafeOptInUsageError")
@Composable
fun ImageCaptureScreen(
    modifier: Modifier = Modifier,
    state: ImageCaptureScreenState = rememberImageCaptureScreenState(),
    onStreamStateChange: (PreviewView.StreamState) -> Unit = {}
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
        onOutputTransformReady = state::setOutputTransform,
        onTouch = state::startTapToFocus,
        onStreamStateChange = onStreamStateChange,
        onDispose = state::releaseResources
    ) {
        // Uses overlay to draw detected QRCode in the image stream
        QRCodeOverlay(qrCodeBoundingBox = state.qrCodeBoundingBox)
    }
}

// It uses OutputTransform hence we need to @SuppressWarnings to get past the linter
@SuppressWarnings("UnsafeOptInUsageError")
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
    onOutputTransformReady: (OutputTransform) -> Unit,
    onTouch: (MeteringPoint) -> Unit,
    onStreamStateChange: (PreviewView.StreamState) -> Unit = {},
    onDispose: () -> Unit = {},
    content: @Composable () -> Unit = {} // overlay to display something above PreviewView
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val localContext = LocalContext.current

    val streamStateObserver = remember {
        Observer<PreviewView.StreamState> { state ->
            onStreamStateChange(state)
        }
    }

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

    // Attach StreamState Observer when the screen first renders
    DisposableEffect(key1 = Unit) {
        Log.d(TAG, "[DisposableEffect] Detaching StreamState Observers from PreviewView")
        previewView.previewStreamState.removeObservers(lifecycleOwner)

        Log.d(TAG, "[DisposableEffect] Attaching StreamState Observer to PreviewView")
        previewView.previewStreamState.observe(lifecycleOwner, streamStateObserver)

        // Detach observer when the screen is removed from the Composition
        onDispose {
            Log.d(TAG, "[onDispose] Detaching current StreamState Observer from PreviewView")
            previewView.previewStreamState.removeObservers(lifecycleOwner)

            // Clean up resources when PreviewView is removed from the composition
            onDispose()
        }
    }

    // Provides OutputTransform when PreviewView is attached on the screen
    LaunchedEffect(key1 = previewView.outputTransform) {
        if (previewView.outputTransform != null) {
            onOutputTransformReady(previewView.outputTransform!!)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView }
        )

        // Overlay over PreviewView in ImageCaptureScreen
        content()

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

@Composable
fun QRCodeOverlay(qrCodeBoundingBox: Rect?) {

    // Draw Bounding Box around QR Code if detected
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (qrCodeBoundingBox != null) {
            drawRect(
                color = Color.Red,
                topLeft = Offset(
                    qrCodeBoundingBox.left.toFloat(),
                    qrCodeBoundingBox.top.toFloat()
                ),
                size = Size(
                    qrCodeBoundingBox.width().toFloat(),
                    qrCodeBoundingBox.height().toFloat()
                ),
                style = Stroke(
                    width = 5.0f
                )
            )
        }
    }
}
