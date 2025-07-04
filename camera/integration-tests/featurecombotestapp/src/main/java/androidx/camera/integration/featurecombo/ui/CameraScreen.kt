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

package androidx.camera.integration.featurecombo.ui

import android.util.Log
import android.widget.Toast
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.integration.featurecombotestapp.R
import androidx.camera.view.PreviewView
import androidx.camera.view.PreviewView.ScaleType.FIT_CENTER
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role.Companion.RadioButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private const val TAG = "CamXFcqCameraScreen"

/** Shows a preview and various other UI elements for camera control. */
@Composable
fun CameraScreen(viewModel: CameraViewModel = viewModel()) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.toastMessages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LifecycleStartEffect(Unit) {
        viewModel.init(context.applicationContext, lifecycleOwner)
        onStopOrDispose {}
    }

    ContentScreen(
        isRearCamera = viewModel.isRearCamera.collectAsStateWithLifecycle().value,
        isVideoMode = viewModel.isVideoMode.collectAsStateWithLifecycle().value,
        featureUis = viewModel.featureUiList.collectAsStateWithLifecycle().value,
        useCaseDetails = viewModel.useCaseDetails.collectAsStateWithLifecycle().value,
        onToggleCamera = { viewModel.toggleCamera(lifecycleOwner) },
        onCapture = { viewModel.capture(context) },
        onToggleVideoMode = { viewModel.toggleVideoMode(lifecycleOwner) },
        onSurfaceProviderAvailable = viewModel::setSurfaceProvider,
        onFeatureUpdated = { featureUi, newValueIndex ->
            viewModel.updateFeature(featureUi, newValueIndex, lifecycleOwner)
        },
        onReset = { viewModel.resetFeatureCombination(lifecycleOwner) },
    )
}

/** Stateless composable for the Camera UI contents. */
@Composable
fun ContentScreen(
    isRearCamera: Boolean,
    isVideoMode: Boolean,
    featureUis: List<FeatureUi>,
    useCaseDetails: String,
    onToggleCamera: () -> Unit,
    onCapture: () -> Unit,
    onToggleVideoMode: () -> Unit,
    onSurfaceProviderAvailable: (SurfaceProvider) -> Unit,
    onFeatureUpdated: (FeatureUi, Int) -> Unit,
    onReset: () -> Unit,
) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PreviewView(context).apply {
                    onSurfaceProviderAvailable(surfaceProvider)
                    scaleType = FIT_CENTER
                }
            },
        )

        FeatureCombinationRow(
            modifier =
                Modifier.fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .align(Alignment.TopCenter),
            featureList = featureUis,
            onFeatureUpdated = onFeatureUpdated,
        )

        Column(
            modifier =
                Modifier.align(Alignment.BottomCenter).background(Color.Black.copy(alpha = 0.75f))
        ) {
            UseCasesAndResetRow(
                modifier = Modifier.fillMaxWidth(),
                useCaseDetails = useCaseDetails,
                onReset = onReset,
            )

            CameraControlsRow(
                modifier = Modifier.fillMaxWidth(),
                isRearCamera = isRearCamera,
                isVideoMode = isVideoMode,
                onToggleCamera = onToggleCamera,
                onCapture = onCapture,
                onToggleVideoMode = onToggleVideoMode,
            )
        }
    }
}

@Composable
fun FeatureCombinationRow(
    modifier: Modifier = Modifier,
    featureList: List<FeatureUi>,
    onFeatureUpdated: (FeatureUi, Int) -> Unit,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceBetween) {
        featureList.forEach { feature ->
            Log.d(TAG, "FeatureCombinationRow: feature = $feature")

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = feature.title.value,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall,
                )

                // Note that Modifier.selectableGroup() is essential to ensure correct accessibility
                // behavior
                Column(Modifier.selectableGroup()) {
                    feature.possibleValues.forEachIndexed { index, value ->
                        Row(
                            Modifier.height(32.dp)
                                .selectable(
                                    selected = (value == feature.selectedValue),
                                    enabled = !feature.unsupportedValues.contains(value),
                                    onClick = { onFeatureUpdated(feature, index) },
                                    role = RadioButton,
                                )
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = (value == feature.selectedValue),
                                enabled = !feature.unsupportedValues.contains(value),
                                // null recommended for accessibility with screen-readers
                                onClick = null,
                            )
                            Text(
                                text = value,
                                color =
                                    if (!feature.unsupportedValues.contains(value)) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Gray
                                    },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraControlsRow(
    modifier: Modifier = Modifier,
    isRearCamera: Boolean,
    isVideoMode: Boolean,
    onToggleCamera: () -> Unit,
    onCapture: () -> Unit,
    onToggleVideoMode: () -> Unit,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(
            modifier = Modifier.padding(16.dp).clickable(onClick = onToggleCamera),
            verticalAlignment = Alignment.Bottom,
        ) {
            Icon(
                imageVector = Icons.Outlined.Cameraswitch,
                contentDescription = stringResource(R.string.switch_camera),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(54.dp),
            )

            Text(
                text =
                    if (isRearCamera) stringResource(R.string.rear)
                    else stringResource(R.string.front),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        IconButton(modifier = Modifier.padding(16.dp), onClick = onCapture) {
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = stringResource(R.string.capture),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(54.dp),
            )
        }

        IconButton(modifier = Modifier.padding(16.dp), onClick = onToggleVideoMode) {
            Icon(
                imageVector =
                    if (isVideoMode) Icons.Default.Videocam else Icons.Default.PhotoCamera,
                contentDescription = stringResource(R.string.toggle_photo_video),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(54.dp),
            )
        }
    }
}

@Composable
fun UseCasesAndResetRow(
    modifier: Modifier = Modifier,
    useCaseDetails: String,
    onReset: () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(modifier = Modifier.padding(16.dp), onClick = onReset) {
            Text(text = stringResource(R.string.reset))
        }

        Text(
            modifier = Modifier.padding(4.dp),
            text = useCaseDetails,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
        )
    }
}
