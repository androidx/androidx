/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.integration.macrobenchmark.target

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.featuregroup.GroupableFeature.Companion.FPS_60
import androidx.camera.core.featuregroup.GroupableFeature.Companion.HDR_HLG10
import androidx.camera.core.featuregroup.GroupableFeature.Companion.IMAGE_ULTRA_HDR
import androidx.camera.core.featuregroup.GroupableFeature.Companion.PREVIEW_STABILIZATION
import androidx.camera.integration.macrobenchmark.target.CameraXSetup.initCameraX
import androidx.camera.integration.macrobenchmark.target.CameraXSetup.toCameraSelector
import androidx.camera.integration.macrobenchmark.target.CameraXSetup.toCameraXConfig
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

class SessionConfigBindActivity : ComponentActivity() {
    private val preview = Preview.Builder().build()
    private val imageCapture = ImageCapture.Builder().build()
    private val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent { CameraScreen(intent.toCameraXConfig(), intent.toCameraSelector()) }
    }

    @Composable
    fun CameraScreen(cameraXConfig: CameraXConfig, cameraSelector: CameraSelector) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        val lifecycleOwner = LocalLifecycleOwner.current

        LifecycleStartEffect(Unit) {
            coroutineScope.launch {
                initCameraX(
                    cameraXConfig,
                    cameraSelector,
                    context.applicationContext,
                    lifecycleOwner,
                    if (intent.extras?.getBoolean("prefer_all_features") == true) {
                        SessionConfig(
                            listOf(preview, imageCapture, videoCapture),
                            preferredFeatureGroup =
                                listOf(HDR_HLG10, FPS_60, PREVIEW_STABILIZATION, IMAGE_ULTRA_HDR),
                        )
                    } else {
                        SessionConfig(listOf(preview, imageCapture, videoCapture))
                    },
                )
            }
            onStopOrDispose {}
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PreviewView(context).apply {
                    preview.surfaceProvider = this.surfaceProvider
                    scaleType = PreviewView.ScaleType.FIT_CENTER
                }
            },
        )
    }
}
