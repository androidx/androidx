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
 * See the License for the naggingfivemissions and
 * limitations under the License.
 */

package androidx.wear.compose.remote.integration.demos.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.remote.material3.previews.RemoteIconButtonEnabled
import androidx.wear.compose.remote.material3.previews.RemoteIconButtonOutlined
import androidx.wear.compose.remote.material3.previews.RemoteIconButtonTonal
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Composable
fun RemoteIconButtonDemos(modifier: Modifier = Modifier) {
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
                    Text("RemoteIconButton Demos")
                }
            }
            item { RemoteDemoItem("Default") { RemoteIconButtonEnabled() } }
            item { RemoteDemoItem("Tonal") { RemoteIconButtonTonal() } }
            item { RemoteDemoItem("Outlined") { RemoteIconButtonOutlined() } }
        }
    }
}

@WearPreviewDevices
@Composable
private fun RemoteIconButtonDemosPreview() {
    RemoteIconButtonDemos()
}
