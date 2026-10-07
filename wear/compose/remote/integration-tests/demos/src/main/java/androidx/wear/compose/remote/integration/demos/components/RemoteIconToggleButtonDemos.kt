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
import androidx.wear.compose.remote.material3.previews.RemoteIconToggleButtonChecked
import androidx.wear.compose.remote.material3.previews.RemoteIconToggleButtonDisabledChecked
import androidx.wear.compose.remote.material3.previews.RemoteIconToggleButtonDisabledUnchecked
import androidx.wear.compose.remote.material3.previews.RemoteIconToggleButtonExtraLarge
import androidx.wear.compose.remote.material3.previews.RemoteIconToggleButtonLarge
import androidx.wear.compose.remote.material3.previews.RemoteIconToggleButtonSmall
import androidx.wear.compose.remote.material3.previews.RemoteIconToggleButtonUnchecked
import androidx.wear.compose.remote.material3.previews.RemoteIconToggleButtonVariantChecked
import androidx.wear.compose.remote.material3.previews.RemoteIconToggleButtonVariantUnchecked
import androidx.wear.compose.remote.material3.samples.RemoteIconToggleButtonSample
import androidx.wear.compose.remote.material3.samples.RemoteIconToggleButtonVariantAnimatedSample
import androidx.wear.compose.remote.material3.samples.RemoteIconToggleButtonVariantSample
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Composable
fun RemoteIconToggleButtonDemos(modifier: Modifier = Modifier) {
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
                        "RemoteIconToggleButton Demos",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            remoteDemoItem("Checked") { RemoteIconToggleButtonChecked() }
            remoteDemoItem("Unchecked") { RemoteIconToggleButtonUnchecked() }
            remoteDemoItem("Disabled Checked") { RemoteIconToggleButtonDisabledChecked() }
            remoteDemoItem("Disabled Unchecked") { RemoteIconToggleButtonDisabledUnchecked() }
            remoteDemoItem("Variant Checked") { RemoteIconToggleButtonVariantChecked() }
            remoteDemoItem("Variant Unchecked") { RemoteIconToggleButtonVariantUnchecked() }
            remoteDemoItem("Small") { RemoteIconToggleButtonSmall() }
            remoteDemoItem("Large") { RemoteIconToggleButtonLarge() }
            remoteDemoItem("Extra Large") { RemoteIconToggleButtonExtraLarge() }
            remoteDemoItem("Sample") { RemoteIconToggleButtonSample() }
            remoteDemoItem("Variant Sample") { RemoteIconToggleButtonVariantSample() }
            remoteDemoItem("Variant Animated Sample") {
                RemoteIconToggleButtonVariantAnimatedSample()
            }
        }
    }
}

@WearPreviewDevices
@Composable
private fun RemoteIconToggleButtonDemosPreview() {
    RemoteIconToggleButtonDemos()
}
