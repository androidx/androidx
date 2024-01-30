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

package androidx.camera.integration.uiwidgets.compose.ui.screen.videocapture

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.integration.uiwidgets.compose.ui.screen.components.CameraControlButton
import androidx.camera.integration.uiwidgets.compose.ui.screen.components.CameraControlRow
import androidx.camera.integration.uiwidgets.compose.ui.screen.components.CameraControlText
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Observer

private const val TAG = "VideoCaptureScreen"

@Composable
fun VideoCaptureScreen(
    modifier: Modifier = Modifier,
    state: VideoCaptureScreenState = rememberVideoCaptureScreenState(),
    onStreamStateChange: (PreviewView.StreamState) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val localContext = LocalContext.current

    LaunchedEffect(key1 = state.lensFacing) {
        state.startCamera(context = localContext, lifecycleOwner = lifecycleOwner)
    }

    VideoCaptureScreen(
        modifier = modifier,
        zoomRatio = state.zoomRatio,
        linearZoom = state.linearZoom,
        onLinearZoomChange = state::setLinearZoom,
        isCameraReady = state.isCameraReady,
        recordState = state.recordState,
        recordingStatsMsg = state.recordingStatsMsg,
        onFlipCameraIconClicked = state::toggleLensFacing,
        onVideoCaptureIconClicked = {
            state.captureVideo(localContext)
        },
        onSurfaceProviderReady = state::setSurfaceProvider,
        onTouch = state::startTapToFocus,
        onStreamStateChange = onStreamStateChange
    )
}

@Composable
fun VideoCaptureScreen(
    modifier: Modifier = Modifier,
    zoomRatio: Float,
    linearZoom: Float,
    onLinearZoomChange: (Float) -> Unit,
    isCameraReady: Boolean,
    recordState: VideoCaptureScreenState.RecordState,
    recordingStatsMsg: String,
    onFlipCameraIconClicked: () -> Unit,
    onVideoCaptureIconClicked: () -> Unit,
    onSurfaceProviderReady: (SurfaceProvider) -> Unit,
    onTouch: (MeteringPoint) -> Unit,
    onStreamStateChange: (PreviewView.StreamState) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val localContext = LocalContext.current

    val streamStateObserver = remember {
        Observer<PreviewView.StreamState> { state ->
            onStreamStateChange(state)
        }
    }

    val previewView = remember {
        PreviewView(localContext).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

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

                VideoRecordButton(
                    recordState = recordState,
                    onVideoCaptureIconClicked = onVideoCaptureIconClicked
                )

                CameraControlText(text = recordingStatsMsg)
            }
        }
    }
}

@Composable
private fun VideoRecordButton(
    recordState: VideoCaptureScreenState.RecordState,
    onVideoCaptureIconClicked: () -> Unit
) {
    val iconColor = when (recordState) {
        VideoCaptureScreenState.RecordState.IDLE -> Color.Black
        VideoCaptureScreenState.RecordState.RECORDING -> Color.Red
        VideoCaptureScreenState.RecordState.STOPPING -> Color.Gray
    }

    CameraControlButton(
        imageVector = Icons.Sharp.Lens,
        contentDescription = "Video Capture",
        modifier = Modifier
            .padding(1.dp)
            .border(1.dp, MaterialTheme.colors.onSecondary, CircleShape),
        tint = iconColor,
        onClick = onVideoCaptureIconClicked
    )
}
