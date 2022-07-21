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
import androidx.camera.integration.uiwidgets.compose.ui.screen.components.CameraControlButton
import androidx.camera.integration.uiwidgets.compose.ui.screen.components.CameraControlButtonPlaceholder
import androidx.camera.integration.uiwidgets.compose.ui.screen.components.CameraControlRow
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.FlipCameraAndroid
import androidx.compose.material.icons.sharp.Lens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun ImageCaptureScreen(
    modifier: Modifier = Modifier,
    stateHolder: ImageCaptureScreenStateHolder = rememberImageCaptureScreenStateHolder()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val localContext = LocalContext.current
    val previewView = remember {
        PreviewView(localContext).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            stateHolder.setSurfaceProvider(this.surfaceProvider)
        }
    }

    LaunchedEffect(key1 = stateHolder.lensFacing) {
        stateHolder.startCamera(context = localContext, lifecycleOwner = lifecycleOwner)
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                previewView
            }
        )

        CameraControlRow(modifier = Modifier.align(Alignment.BottomCenter)) {
            CameraControlButton(
                imageVector = Icons.Sharp.FlipCameraAndroid,
                contentDescription = "Toggle Camera Lens",
            ) {
                stateHolder.toggleLensFacing()
            }

            CameraControlButton(
                imageVector = Icons.Sharp.Lens,
                contentDescription = "Image Capture",
                modifier = Modifier
                    .padding(1.dp)
                    .border(1.dp, MaterialTheme.colors.onSecondary, CircleShape)
            ) {
                stateHolder.takePhoto(localContext)
            }

            if (stateHolder.hasFlashMode) {
                CameraControlButton(
                    imageVector = stateHolder.flashModeIcon,
                    contentDescription = "Toggle Flash Mode",
                    modifier = Modifier
                        .padding(1.dp)
                        .border(1.dp, MaterialTheme.colors.onSecondary, RectangleShape)
                ) {
                    stateHolder.toggleFlashMode()
                }
            } else {
                CameraControlButtonPlaceholder()
            }
        }
    }
}