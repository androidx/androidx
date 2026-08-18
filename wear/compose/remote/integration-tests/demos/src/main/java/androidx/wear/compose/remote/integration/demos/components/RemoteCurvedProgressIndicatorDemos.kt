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
package androidx.wear.compose.remote.integration.demos.components

import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.wear.compose.remote.material3.previews.RemoteCurvedProgressEnabled
import androidx.wear.compose.remote.material3.previews.RemoteCurvedProgressIndicatorCollapseToZero
import androidx.wear.compose.remote.material3.previews.RemoteCurvedProgressIndicatorCountdownIntroLoop
import androidx.wear.compose.remote.material3.previews.RemoteCurvedProgressIndicatorCountdownOutroLoop
import androidx.wear.compose.remote.material3.previews.RemoteCurvedProgressIndicatorCustomColor
import androidx.wear.compose.remote.material3.previews.RemoteCurvedProgressIndicatorDisabled
import androidx.wear.compose.remote.material3.previews.RemoteCurvedProgressIndicatorExpandFromZero
import androidx.wear.compose.remote.material3.previews.RemoteCurvedProgressIndicatorIntroLoop
import androidx.wear.compose.remote.material3.previews.RemoteCurvedProgressIndicatorNoCollapse
import androidx.wear.compose.remote.material3.previews.RemoteCurvedProgressIndicatorOutroLoop
import androidx.wear.compose.remote.material3.previews.RemoteCurvedProgressNoGap
import androidx.wear.compose.remote.material3.samples.RemoteCurvedProgressIndicatorAnimatedSample
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Composable
fun RemoteCurvedProgressIndicatorDemos(modifier: Modifier = Modifier) {
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
                        "RemoteCurvedProgressIndicator Demos",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            // Basic Demos
            remoteDemoItem("Enabled") { RemoteCurvedProgressEnabled() }
            remoteDemoItem("Disabled") { RemoteCurvedProgressIndicatorDisabled() }
            remoteDemoItem("Custom Color") { RemoteCurvedProgressIndicatorCustomColor() }
            remoteDemoItem("No Gap Custom Angle") { RemoteCurvedProgressNoGap() }
            remoteDemoItem("Animated") { RemoteCurvedProgressIndicatorAnimatedSample() }
            // Dynamic looping demos
            remoteDemoItem("Intro Dot Loop") { RemoteCurvedProgressIndicatorIntroLoop() }
            remoteDemoItem("Outro Dot Loop") { RemoteCurvedProgressIndicatorOutroLoop() }
            remoteDemoItem("Countdown Intro Loop") {
                RemoteCurvedProgressIndicatorCountdownIntroLoop()
            }
            remoteDemoItem("Countdown Outro Loop") {
                RemoteCurvedProgressIndicatorCountdownOutroLoop()
            }
            remoteDemoItem("Expand from Zero") { RemoteCurvedProgressIndicatorExpandFromZero() }
            remoteDemoItem("Collapse to Zero") { RemoteCurvedProgressIndicatorCollapseToZero() }
            remoteDemoItem("No Collapse") { RemoteCurvedProgressIndicatorNoCollapse() }
        }
    }
}

@WearPreviewDevices
@Composable
private fun RemoteCurvedProgressIndicatorDemosPreview() {
    RemoteCurvedProgressIndicatorDemos()
}
