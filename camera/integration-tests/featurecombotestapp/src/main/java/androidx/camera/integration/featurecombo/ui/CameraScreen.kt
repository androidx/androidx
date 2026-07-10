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

import android.graphics.Matrix
import android.util.Log
import android.widget.Toast
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.integration.featurecombo.AppFeatures
import androidx.camera.integration.featurecombo.AppUseCase
import androidx.camera.integration.featurecombo.DynamicRange
import androidx.camera.integration.featurecombo.Fps
import androidx.camera.integration.featurecombo.ImageFormat
import androidx.camera.integration.featurecombo.RecordingQuality
import androidx.camera.integration.featurecombo.StabilizationMode
import androidx.camera.integration.featurecombo.effects.BouncyLogoOverlayEffect
import androidx.camera.integration.featurecombo.effects.BouncyLogoOverlayEffect.Companion.supportsEffect
import androidx.camera.integration.featurecombotestapp.R
import androidx.camera.view.PreviewView
import androidx.camera.view.PreviewView.ScaleType.FIT_CENTER
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role.Companion.RadioButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
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
        viewModel.setupCamera(context.applicationContext, lifecycleOwner)
        onStopOrDispose {}
    }

    ContentScreen(
        isRearCamera = viewModel.isRearCamera.collectAsStateWithLifecycle().value,
        isRecording = viewModel.isRecording.collectAsStateWithLifecycle().value,
        isUseCaseEnabled = viewModel.isUseCaseEnabled.collectAsStateWithLifecycle().value,
        featureUis = viewModel.featureUiList.collectAsStateWithLifecycle().value,
        useCaseResolutions = viewModel.useCaseResolutions.collectAsStateWithLifecycle().value,
        imageAnalysisFrameCount =
            viewModel.imageAnalysisFrameCount.collectAsStateWithLifecycle().value,
        cameraCaptureFps = viewModel.cameraCaptureFps.collectAsStateWithLifecycle().value,
        onToggleCamera = { viewModel.toggleCamera(lifecycleOwner) },
        onCapture = { viewModel.capture(context) },
        onRecord = { viewModel.record(context) },
        onToggleUseCase = { useCase -> viewModel.toggleUseCase(lifecycleOwner, useCase) },
        onSurfaceProviderAvailable = viewModel::setSurfaceProvider,
        onFeatureUpdated = { featureUi, newValueIndex ->
            viewModel.updateFeature(featureUi, newValueIndex, lifecycleOwner)
        },
        onReset = { viewModel.resetUseCasesAndFeatureCombo(lifecycleOwner) },
        onBouncyLogoEffectAvailable = { viewModel.setBouncyLogoOverlayEffect(it, lifecycleOwner) },
    )
}

/** Stateless composable for the Camera UI contents. */
@Composable
fun ContentScreen(
    isRearCamera: Boolean,
    isRecording: Boolean,
    isUseCaseEnabled: Map<AppUseCase, Boolean>,
    featureUis: List<FeatureUi>,
    useCaseResolutions: Map<AppUseCase, String>,
    imageAnalysisFrameCount: Int,
    cameraCaptureFps: Int,
    onToggleCamera: () -> Unit,
    onCapture: () -> Unit,
    onRecord: () -> Unit,
    onToggleUseCase: (AppUseCase) -> Unit,
    onSurfaceProviderAvailable: (SurfaceProvider) -> Unit,
    onFeatureUpdated: (FeatureUi, Int) -> Unit,
    onReset: () -> Unit,
    onBouncyLogoEffectAvailable: (BouncyLogoOverlayEffect) -> Unit,
) {
    val context = LocalContext.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = FIT_CENTER
            onSurfaceProviderAvailable(surfaceProvider)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        var androidViewSize by remember { mutableStateOf<IntSize?>(null) }

        AndroidView(
            modifier =
                Modifier.fillMaxSize().onGloballyPositioned { coordinates ->
                    androidViewSize = coordinates.size
                },
            factory = { previewView },
        )

        BouncyLogo(
            isUseCaseEnabled.filterValues { it }.keys,
            androidViewSize,
            { previewView.sensorToViewTransform },
            onBouncyLogoEffectAvailable,
        )

        FeatureCombinationRow(
            modifier =
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f))
                    .align(Alignment.TopCenter),
            featureList = featureUis,
            onFeatureUpdated = onFeatureUpdated,
        )

        CameraControlsRow(
            modifier =
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f))
                    .align(Alignment.BottomCenter),
            isRearCamera = isRearCamera,
            isRecording = isRecording,
            isUseCaseEnabled = isUseCaseEnabled,
            useCaseResolutions = useCaseResolutions,
            imageAnalysisFrameCount = imageAnalysisFrameCount,
            cameraCaptureFps = cameraCaptureFps,
            onCapture = onCapture,
            onRecord = onRecord,
            onReset = onReset,
            onToggleCamera = onToggleCamera,
            onToggleUseCase = onToggleUseCase,
        )
    }
}

@Composable
fun BouncyLogo(
    useCases: Set<AppUseCase>,
    containerSize: IntSize?,
    sensorToViewTransformer: () -> Matrix?,
    onBouncyLogoEffectAvailable: (BouncyLogoOverlayEffect) -> Unit,
) {
    if (!useCases.supportsEffect() || containerSize == null) return

    val bouncyLogoBgColor = MaterialTheme.colorScheme.primaryContainer
    val bouncyLogoTextColor = MaterialTheme.colorScheme.onPrimaryContainer

    DisposableEffect(useCases, containerSize, sensorToViewTransformer) {
        val bouncyLogoEffect =
            BouncyLogoOverlayEffect(
                useCases = useCases,
                logoText = "CameraX",
                bgColor = bouncyLogoBgColor.toArgb(),
                textColor = bouncyLogoTextColor.toArgb(),
                containerWidth = containerSize.width,
                containerHeight = containerSize.height,
                sensorToViewTransformer = sensorToViewTransformer,
            )

        onBouncyLogoEffectAvailable(bouncyLogoEffect)
        onDispose { bouncyLogoEffect.close() }
    }
}

@Composable
fun FeatureCombinationRow(
    modifier: Modifier = Modifier,
    featureList: List<FeatureUi>,
    onFeatureUpdated: (FeatureUi, Int) -> Unit,
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier.rowScrollbar(scrollState).horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
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
                                        MaterialTheme.colorScheme.onBackground
                                    } else {
                                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38F)
                                    },
                                style = MaterialTheme.typography.bodySmall,
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
    isRecording: Boolean,
    isUseCaseEnabled: Map<AppUseCase, Boolean>,
    useCaseResolutions: Map<AppUseCase, String>,
    imageAnalysisFrameCount: Int,
    cameraCaptureFps: Int,
    onCapture: () -> Unit,
    onRecord: () -> Unit,
    onReset: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleUseCase: (AppUseCase) -> Unit,
) {
    Row(
        modifier = modifier.height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.padding(4.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Row(
                modifier = Modifier.padding(4.dp).clickable(onClick = onToggleCamera),
                verticalAlignment = Alignment.Bottom,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Cameraswitch,
                    contentDescription = stringResource(R.string.switch_camera),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp),
                )

                Text(
                    text =
                        if (isRearCamera) stringResource(R.string.rear)
                        else stringResource(R.string.front),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            Button(modifier = Modifier.padding(4.dp), onClick = onReset) {
                Text(text = stringResource(R.string.reset))
            }
        }

        Column(
            modifier = Modifier.padding(4.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val captureSupported = isUseCaseEnabled[AppUseCase.IMAGE_CAPTURE] == true

            IconButton(
                modifier = Modifier.padding(4.dp),
                enabled = captureSupported,
                onClick = onCapture,
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = stringResource(R.string.capture),
                    tint =
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = if (captureSupported) 1.0F else 0.38F
                        ),
                    modifier = Modifier.size(48.dp),
                )
            }

            val recordingSupported = isUseCaseEnabled[AppUseCase.VIDEO_CAPTURE] == true

            IconButton(
                modifier = Modifier.padding(4.dp),
                enabled = recordingSupported && !isRecording,
                onClick = onRecord,
            ) {
                Icon(
                    imageVector =
                        if (isRecording) Icons.Default.FiberManualRecord
                        else Icons.Default.Videocam,
                    contentDescription = stringResource(R.string.record),
                    tint =
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = if (recordingSupported) 1.0F else 0.38F
                        ),
                    modifier = Modifier.size(48.dp),
                )
            }

            if (isUseCaseEnabled[AppUseCase.IMAGE_ANALYSIS] == true) {
                Text(
                    modifier = Modifier.padding(4.dp).basicMarquee(),
                    text = "Analyzed frames:\r\n$imageAnalysisFrameCount",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }

            Text(
                modifier = Modifier.padding(4.dp).basicMarquee(),
                text = "Capture Result FPS:\r\n$cameraCaptureFps",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }

        UseCaseCheckboxes(
            modifier = Modifier.padding(4.dp).fillMaxHeight(),
            isUseCaseEnabled = isUseCaseEnabled,
            useCaseResolutions = useCaseResolutions,
            onToggleUseCase = onToggleUseCase,
        )
    }
}

@Composable
fun UseCaseCheckboxes(
    modifier: Modifier = Modifier,
    isUseCaseEnabled: Map<AppUseCase, Boolean>,
    useCaseResolutions: Map<AppUseCase, String>,
    onToggleUseCase: (AppUseCase) -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        val enabledUseCaseCount = isUseCaseEnabled.count { (_, v) -> v }

        isUseCaseEnabled.forEach { (key, value) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        text = key.uiName,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = useCaseResolutions[key] ?: "",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Checkbox(
                    checked = value,
                    enabled = !(value && enabledUseCaseCount == 1),
                    onCheckedChange = { onToggleUseCase(key) },
                )
            }
        }
    }
}

@Composable
fun Modifier.rowScrollbar(
    scrollState: ScrollState,
    height: Dp = 4.dp,
    showScrollBarTrack: Boolean = true,
    scrollBarTrackColor: Color = MaterialTheme.colorScheme.primaryContainer,
    scrollBarColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    scrollBarCornerRadius: Float = 4f,
): Modifier {
    return drawWithContent {
        // Draw the row's content
        drawContent()

        // Dimensions and calculations
        val viewportWidth = this.size.width
        val totalContentWidth = scrollState.maxValue.toFloat() + viewportWidth
        val scrollValue = scrollState.value.toFloat()

        // Compute scrollbar width and position
        val scrollBarWidth = (viewportWidth / totalContentWidth) * viewportWidth
        val scrollBarStartOffset = (scrollValue / totalContentWidth) * viewportWidth

        // Draw the track (optional)
        if (showScrollBarTrack) {
            drawRoundRect(
                cornerRadius = CornerRadius(scrollBarCornerRadius),
                color = scrollBarTrackColor,
                topLeft = Offset(0f, this.size.height),
                size = Size(viewportWidth, height.toPx()),
            )
        }

        // Draw the scrollbar
        drawRoundRect(
            cornerRadius = CornerRadius(scrollBarCornerRadius),
            color = scrollBarColor,
            topLeft = Offset(scrollBarStartOffset, this.size.height),
            size = Size(scrollBarWidth, height.toPx()),
        )
    }
}

@Preview
@Composable
fun ContentScreenPreview() {
    val isUseCaseEnabled = buildMap {
        put(AppUseCase.PREVIEW, true)
        put(AppUseCase.IMAGE_CAPTURE, true)
        put(AppUseCase.VIDEO_CAPTURE, true)
        put(AppUseCase.IMAGE_ANALYSIS, true)
    }

    val appFeatures =
        AppFeatures(
            dynamicRange = DynamicRange.HLG_10,
            fps = Fps.FPS_30,
            stabilizationMode = StabilizationMode.VIDEO,
            imageFormat = ImageFormat.JPEG_R,
            recordingQuality = RecordingQuality.HD,
            unsupportedStabilizationModes = listOf(StabilizationMode.PREVIEW),
            unsupportedRecordingQualities = listOf(RecordingQuality.UHD, RecordingQuality.SD),
        )

    ContentScreen(
        isRearCamera = true,
        isRecording = false,
        isUseCaseEnabled = isUseCaseEnabled,
        featureUis = appFeatures.toFeatureUiList(isUseCaseEnabled.filterValues { it }.keys),
        useCaseResolutions =
            buildMap {
                put(AppUseCase.PREVIEW, "(1920 x 1080)")
                put(AppUseCase.IMAGE_CAPTURE, "(4000 x 3000)")
                put(AppUseCase.VIDEO_CAPTURE, "(3840 x 2160)")
                put(AppUseCase.IMAGE_ANALYSIS, "(640 x 480)")
            },
        imageAnalysisFrameCount = 0,
        cameraCaptureFps = 0,
        onToggleCamera = {},
        onCapture = {},
        onRecord = {},
        onToggleUseCase = {},
        onSurfaceProviderAvailable = {},
        onFeatureUpdated = { _, _ -> },
        onReset = {},
        onBouncyLogoEffectAvailable = {},
    )
}
