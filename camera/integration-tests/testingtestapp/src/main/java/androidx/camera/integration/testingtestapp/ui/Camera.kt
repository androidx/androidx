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

package androidx.camera.integration.testingtestapp.ui

import androidx.camera.integration.testingtestapp.R
import androidx.camera.view.PreviewView
import androidx.camera.view.PreviewView.ScaleType.FIT_CENTER
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/** Shows a preview and buttons to take photos. */
@Composable
fun Camera(viewModel: CameraViewModel = viewModel()) {

    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraController =
        remember(lifecycleOwner) { viewModel.initCameraController(lifecycleOwner) }

    DisposableEffect(key1 = lifecycleOwner) { onDispose { viewModel.disposeCameraController() } }

    val errors = viewModel.errorState.collectAsStateWithLifecycle()
    if (errors.value.isNotEmpty()) {
        Text(errors.value)
    } else {
        Camera(
            toggleCamera = { viewModel.toggleCamera() },
            takePhoto = { viewModel.takePhoto() },
            frontCamera = !viewModel.isUsingBackLens.collectAsStateWithLifecycle().value
        ) {
            AndroidView(
                factory = {
                    PreviewView(context).apply {
                        try {
                            controller = cameraController
                        } catch (e: IllegalArgumentException) {
                            viewModel.setViewError(e)
                        }
                        scaleType = FIT_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Stateless composable for the Camera's UI. */
@Composable
fun Camera(
    toggleCamera: () -> Unit,
    takePhoto: () -> Unit,
    frontCamera: Boolean,
    content: @Composable () -> Unit
) {

    Box(modifier = Modifier.fillMaxSize()) {
        content()
        IconButton(
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            onClick = takePhoto
        ) {
            Icon(
                imageVector = Icons.Filled.AddCircle,
                contentDescription = stringResource(R.string.take_photo),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(54.dp)
            )
        }
        IconButton(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            onClick = toggleCamera
        ) {
            Icon(
                imageVector =
                    if (frontCamera) Icons.AutoMirrored.Filled.ArrowForward
                    else Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.swap_cameras),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(54.dp)
            )
        }
    }
}
