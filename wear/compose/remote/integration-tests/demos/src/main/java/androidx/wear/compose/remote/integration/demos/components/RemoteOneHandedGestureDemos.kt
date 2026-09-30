/*
 * Copyright 2026 The Android Open Source Project
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
@file:Suppress("RestrictedApiAndroidX")

package androidx.wear.compose.remote.integration.demos.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.layout.RemoteTime
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.remote.material3.RemoteButton
import androidx.wear.compose.remote.material3.internal.RemotePrimaryGestureIndicator
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

/**
 * Demos for the one-handed gesture hint drawn as Remote Compose paths.
 *
 * The hint loops here so that it is observable without a host. That is a demo driver and not the
 * production contract: the native indicator plays the animation once, when
 * `OneHandedGestureManager` decides it should be shown. See [RemotePrimaryGestureIndicator] for why
 * a captured document cannot make that decision for itself.
 *
 * @see RemotePrimaryGestureIndicator
 */
@Composable
fun RemoteOneHandedGestureDemos(modifier: Modifier = Modifier) {
    val transformationSpec = rememberTransformationSpec()
    val columnState = rememberTransformingLazyColumnState()

    ScreenScaffold(scrollState = columnState, modifier = modifier) { contentPadding ->
        TransformingLazyColumn(state = columnState, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier =
                        Modifier.fillMaxWidth()
                            .transformedHeight(
                                scope = this,
                                transformationSpec = transformationSpec,
                            ),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(
                        "Gesture Hint (looping demo)",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            val cycleTime = RemoteTime().ContinuousSec() % 1.617f
            val loopingProgress = clamp(cycleTime / 0.617f, 0f.rf, 1f.rf)
            remoteDemoItem("Primary (double pinch)") {
                RemotePrimaryGestureIndicator(progress = loopingProgress)
            }
            remoteDemoItem("On RemoteButton") {
                RemoteButton(onClick = Action.Empty) {
                    RemotePrimaryGestureIndicator(progress = loopingProgress)
                }
            }
        }
    }
}

@WearPreviewDevices
@Composable
private fun RemoteOneHandedGestureDemosPreview() {
    RemoteOneHandedGestureDemos()
}
