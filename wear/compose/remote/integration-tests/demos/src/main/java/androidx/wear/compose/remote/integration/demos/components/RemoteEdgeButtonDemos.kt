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
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonDisabled
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonExtraSmall
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonFilledTonal
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonFilledVariant
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonLarge
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonMedium
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonOutlined
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonSmall
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonWithBorder
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonWithIcon
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Composable
fun RemoteEdgeButtonDemos(modifier: Modifier = Modifier) {
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
                        "RemoteEdgeButton Demos",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            remoteDemoItem("Extra Small") { RemoteEdgeButtonExtraSmall() }
            remoteDemoItem("Small") { RemoteEdgeButtonSmall() }
            remoteDemoItem("Medium") { RemoteEdgeButtonMedium() }
            remoteDemoItem("Large") { RemoteEdgeButtonLarge() }
            remoteDemoItem("Filled Tonal") { RemoteEdgeButtonFilledTonal() }
            remoteDemoItem("Filled Variant") { RemoteEdgeButtonFilledVariant() }
            remoteDemoItem("Outlined") { RemoteEdgeButtonOutlined() }
            remoteDemoItem("Border") { RemoteEdgeButtonWithBorder() }
            remoteDemoItem("Disabled") { RemoteEdgeButtonDisabled() }
            remoteDemoItem("With Icon") { RemoteEdgeButtonWithIcon() }
        }
    }
}

@WearPreviewDevices
@Composable
private fun RemoteEdgeButtonDemosPreview() {
    RemoteEdgeButtonDemos()
}
