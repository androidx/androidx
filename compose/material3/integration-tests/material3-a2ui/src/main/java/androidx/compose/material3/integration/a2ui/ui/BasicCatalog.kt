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

package androidx.compose.material3.integration.a2ui.ui

import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import androidx.compose.material3.a2ui.catalog.materialA2uiBasicCatalogV1
import androidx.compose.material3.integration.a2ui.ui.samples.AudioPlayerRenderer
import androidx.compose.material3.integration.a2ui.ui.samples.ImageRenderer
import androidx.compose.material3.integration.a2ui.ui.samples.VideoRenderer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun rememberBasicCatalog(): A2uiCatalog {
    val context = LocalContext.current
    return remember(context) {
        materialA2uiBasicCatalogV1(
            image =
                MaterialA2uiBasicCatalogV1Defaults.image {
                    url,
                    contentDescription,
                    contentScale,
                    modifier,
                    _ ->
                    ImageRenderer(
                        url = url,
                        contentDescription = contentDescription,
                        contentScale = contentScale,
                        modifier = modifier,
                    )
                },
            video =
                MaterialA2uiBasicCatalogV1Defaults.video { url, modifier, onError ->
                    VideoRenderer(
                        url = url,
                        modifier = modifier,
                        onError = onError,
                    )
                },
            audioPlayer =
                MaterialA2uiBasicCatalogV1Defaults.audioPlayer {
                    url,
                    contentDescription,
                    modifier,
                    onError ->
                    AudioPlayerRenderer(
                        url = url,
                        contentDescription = contentDescription,
                        modifier = modifier,
                        onError = onError,
                    )
                },
            functions = A2uiBasicCatalogV1.Functions(context),
        )
    }
}
