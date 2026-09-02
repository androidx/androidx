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
 * An abstraction of an asynchronous video player which should be implemented using the
 * application's media loading library (such as Media3/ExoPlayer), used for rendering video playback
 * for [A2uiBasicCatalogV1.Video].
 */
@Stable
public fun interface A2uiVideoRenderer {
    /**
     * Displays a video player from a URL.
     *
     * @param url The URL of the video to load and play.
     * @param modifier The modifier to be applied to the layout.
     * @param onError The callback to be invoked when the video fails to load or play.
     */
    @Composable
    public fun Video(url: String, modifier: Modifier, onError: (throwable: Throwable?) -> Unit)
}

/** A Jetpack Compose Material 3 implementation of the A2UI Basic Catalog `"Video"` component. */
internal class MaterialA2uiBasicCatalogV1Video(private val videoRenderer: A2uiVideoRenderer) :
    A2uiBasicCatalogV1.Video {

    @Composable
    override fun A2uiComponentScope.TypedContent(url: String, modifier: Modifier) {
        videoRenderer.Video(
            url = url,
            modifier = modifier,
            onError = { throwable ->
                val errorMessage =
                    if (throwable != null && !throwable.message.isNullOrBlank()) {
                        "Video loading error from renderer for $url: ${throwable.message}"
                    } else {
                        "Video loading error from renderer: $url"
                    }
                reportError(A2uiRuntimeException(message = errorMessage))
            },
        )
    }
}
