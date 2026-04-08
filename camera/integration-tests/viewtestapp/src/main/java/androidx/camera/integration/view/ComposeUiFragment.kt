/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.integration.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.SurfaceRequest
import androidx.camera.integration.view.MainActivity.CAMERA_DIRECTION_BACK
import androidx.camera.integration.view.MainActivity.CAMERA_DIRECTION_FRONT
import androidx.camera.integration.view.MainActivity.INTENT_EXTRA_CAMERA_DIRECTION
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.concurrent.futures.await
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.LocalLifecycleOwner

/** A fragment that demonstrates how to use [ComposeView] to display camera preview. */
class ComposeUiFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val bundle: Bundle? = requireActivity().intent.extras
        val (contentScale, alignment) =
            if (bundle != null) {
                val scaleTypeId =
                    bundle.getInt(
                        MainActivity.INTENT_EXTRA_SCALE_TYPE,
                        MainActivity.DEFAULT_SCALE_TYPE_ID,
                    )
                // Map the scale type ID to ContentScale and Alignment.
                // 0: FILL_START, 1: FILL_CENTER, 2: FILL_END,
                // 3: FIT_START, 4: FIT_CENTER, 5: FIT_END
                when (scaleTypeId) {
                    0 -> Pair(ContentScale.Crop, Alignment.TopStart) // FILL_START
                    1 -> Pair(ContentScale.Crop, Alignment.Center) // FILL_CENTER
                    2 -> Pair(ContentScale.Crop, Alignment.BottomEnd) // FILL_END
                    3 -> Pair(ContentScale.Fit, Alignment.TopStart) // FIT_START
                    4 -> Pair(ContentScale.Fit, Alignment.Center) // FIT_CENTER
                    5 -> Pair(ContentScale.Fit, Alignment.BottomEnd) // FIT_END
                    else -> Pair(ContentScale.Crop, Alignment.Center)
                }
            } else {
                Pair(ContentScale.Crop, Alignment.Center)
            }

        val initialLensFacing =
            if (bundle != null) {
                when (bundle.getString(INTENT_EXTRA_CAMERA_DIRECTION, CAMERA_DIRECTION_BACK)) {
                    CAMERA_DIRECTION_BACK -> LENS_FACING_BACK
                    CAMERA_DIRECTION_FRONT -> LENS_FACING_FRONT
                    else -> LENS_FACING_BACK
                }
            } else {
                LENS_FACING_BACK
            }

        return ComposeView(requireContext()).apply {
            setContent { CameraScreen(initialLensFacing, contentScale, alignment) }
        }
    }
}

private val SCALE_OPTIONS: List<Triple<String, ContentScale, Alignment>> =
    listOf(
        Triple("FILL_START", ContentScale.Crop, Alignment.TopStart),
        Triple("FILL_CENTER", ContentScale.Crop, Alignment.Center),
        Triple("FILL_END", ContentScale.Crop, Alignment.BottomEnd),
        Triple("FIT_START", ContentScale.Fit, Alignment.TopStart),
        Triple("FIT_CENTER", ContentScale.Fit, Alignment.Center),
        Triple("FIT_END", ContentScale.Fit, Alignment.BottomEnd),
    )

@Composable
private fun CameraScreen(
    initialLensFacing: Int,
    initialContentScale: ContentScale,
    initialAlignment: Alignment,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by rememberSaveable { mutableStateOf(initialLensFacing) }
    var implementationMode by
        rememberSaveable(
            saver =
                Saver<MutableState<ImplementationMode>, Int>(
                    save = { it.value.ordinal },
                    restore = { mutableStateOf(ImplementationMode.values()[it]) },
                )
        ) {
            mutableStateOf(ImplementationMode.EXTERNAL)
        }
    var selectedScaleIndex by rememberSaveable {
        mutableStateOf(
            SCALE_OPTIONS.indexOfFirst {
                    it.second == initialContentScale && it.third == initialAlignment
                }
                .takeIf { it != -1 } ?: 1
        )
    }
    val contentScale = SCALE_OPTIONS[selectedScaleIndex].second
    val alignment = SCALE_OPTIONS[selectedScaleIndex].third

    var hasEffect by rememberSaveable { mutableStateOf(false) }
    var isStreamSharingEnabled by rememberSaveable { mutableStateOf(false) }
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val preview = remember { Preview.Builder().build() }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val imageAnalysis = remember { ImageAnalysis.Builder().build() }
    val videoCapture = remember {
        val recorder = Recorder.Builder().build()
        VideoCapture.Builder(recorder).build()
    }

    val toneMappingEffect = remember {
        ToneMappingSurfaceEffect(CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE)
    }

    DisposableEffect(toneMappingEffect) { onDispose { toneMappingEffect.release() } }

    LaunchedEffect(preview) { preview.setSurfaceProvider { request -> surfaceRequest = request } }

    LaunchedEffect(Unit) { cameraProvider = ProcessCameraProvider.getInstance(context).await() }

    LaunchedEffect(lensFacing, hasEffect, isStreamSharingEnabled, cameraProvider) {
        Log.d(
            "ComposeUiFragment",
            "implementationMode: $implementationMode, isStreamSharingEnabled: $isStreamSharingEnabled",
        )

        val provider = cameraProvider ?: return@LaunchedEffect

        val cameraSelector =
            if (lensFacing == LENS_FACING_BACK) {
                DEFAULT_BACK_CAMERA
            } else {
                DEFAULT_FRONT_CAMERA
            }

        val useCases = mutableListOf(preview, imageCapture, imageAnalysis)
        if (isStreamSharingEnabled) {
            useCases.add(videoCapture)
        }

        val sessionConfig =
            SessionConfig(
                useCases = useCases,
                effects = if (hasEffect) listOf(toneMappingEffect) else emptyList(),
            )
        provider.bindToLifecycle(lifecycleOwner, cameraSelector, sessionConfig)
    }

    var showImplementationMode by remember { mutableStateOf(false) }
    var showScaleMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        surfaceRequest?.let { request ->
            CameraXViewfinder(
                surfaceRequest = request,
                contentScale = contentScale,
                alignment = alignment,
                implementationMode = implementationMode,
            )
        }

        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 5.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Stream Sharing Toggle
            Button(
                onClick = { isStreamSharingEnabled = !isStreamSharingEnabled },
                shape = CircleShape,
                modifier = Modifier.size(46.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        backgroundColor =
                            if (isStreamSharingEnabled) Color.Green else Color(0xAA2255FF),
                        contentColor = Color.White,
                    ),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(text = "SS", fontSize = 14.sp)
            }

            // Implementation Mode Selector
            Box {
                Button(
                    onClick = { showImplementationMode = true },
                    shape = CircleShape,
                    modifier = Modifier.size(46.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xAA2255FF),
                            contentColor = Color(0xEEEEEEEE),
                        ),
                    contentPadding = PaddingValues(0.dp),
                    elevation =
                        ButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            hoveredElevation = 0.dp,
                            focusedElevation = 0.dp,
                        ),
                ) {
                    Text(
                        text =
                            when (implementationMode) {
                                ImplementationMode.EXTERNAL -> "EXT"
                                ImplementationMode.EMBEDDED -> "EMB"
                            },
                        fontSize = 14.sp,
                    )
                }
                DropdownMenu(
                    expanded = showImplementationMode,
                    onDismissRequest = { showImplementationMode = false },
                ) {
                    ImplementationMode.values().forEach { mode ->
                        DropdownMenuItem(
                            onClick = {
                                implementationMode = mode
                                showImplementationMode = false
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = implementationMode == mode, onClick = null)
                                Text(text = mode.name)
                            }
                        }
                    }
                }
            }

            // Content Scale Selector
            Box {
                Button(
                    onClick = { showScaleMenu = true },
                    shape = CircleShape,
                    modifier = Modifier.size(46.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xAA2255FF),
                            contentColor = Color(0xEEEEEEEE),
                        ),
                    contentPadding = PaddingValues(0.dp),
                    elevation =
                        ButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            hoveredElevation = 0.dp,
                            focusedElevation = 0.dp,
                        ),
                ) {
                    Text(text = "SCL", fontSize = 14.sp)
                }
                DropdownMenu(
                    expanded = showScaleMenu,
                    onDismissRequest = { showScaleMenu = false },
                ) {
                    SCALE_OPTIONS.forEachIndexed { index, (name, _, _) ->
                        DropdownMenuItem(
                            onClick = {
                                selectedScaleIndex = index
                                showScaleMenu = false
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedScaleIndex == index, onClick = null)
                                Text(text = name)
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                Button(onClick = { hasEffect = !hasEffect }) { Text("Effect") }
                Button(
                    onClick = {
                        lensFacing =
                            if (lensFacing == LENS_FACING_BACK) LENS_FACING_FRONT
                            else LENS_FACING_BACK
                    }
                ) {
                    Text("Toggle")
                }
            }
        }
    }
}
