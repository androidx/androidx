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

package androidx.compose.material3.a2ui.catalog

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

/**
 * An abstraction of an asynchronous audio player which should be implemented using the
 * application's media loading library (such as Media3/ExoPlayer), used for rendering audio playback
 * controls for [A2uiBasicCatalogV1.AudioPlayer].
 */
@Stable
public fun interface A2uiAudioPlayerRenderer {
    /**
     * Displays an audio player from a URL.
     *
     * @param url The URL of the audio to load and play.
     * @param contentDescription Accessibility text for the audio player.
     * @param modifier The modifier to be applied to the layout.
     * @param onError The callback to be invoked when the audio fails to load or play.
     */
    @Composable
    public fun AudioPlayer(
        url: String,
        contentDescription: String?,
        modifier: Modifier,
        onError: (throwable: Throwable?) -> Unit,
    )
}

/**
 * A Jetpack Compose Material 3 implementation of the A2UI Basic Catalog `"AudioPlayer"` component.
 */
internal class MaterialA2uiBasicCatalogV1AudioPlayer(
    private val audioPlayerRenderer: A2uiAudioPlayerRenderer
) : A2uiBasicCatalogV1.AudioPlayer {

    @Composable
    override fun A2uiComponentScope.TypedContent(
        url: String,
        description: String?,
        modifier: Modifier,
    ) {
        audioPlayerRenderer.AudioPlayer(
            url = url,
            contentDescription = description,
            modifier = modifier,
            onError = { throwable ->
                val errorMessage =
                    if (throwable != null && !throwable.message.isNullOrBlank()) {
                        "Audio loading error from renderer for $url: ${throwable.message}"
                    } else {
                        "Audio loading error from renderer: $url"
                    }
                reportError(A2uiRuntimeException(message = errorMessage))
            },
        )
    }
}
