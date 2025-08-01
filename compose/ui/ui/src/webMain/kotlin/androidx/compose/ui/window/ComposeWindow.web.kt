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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi

/**
 * EXPERIMENTAL! Might be deleted or changed in the future!
 *
 * Creates the composition in HTML canvas created in parent container identified by [viewportContainerId] id.
 * This size of canvas is adjusted with the size of the container
 *
 * @param viewportContainerId - The id of an HTML element which would host the Compose Viewport.
 * If it's null, then `<body>` will be used as a container.
 * @param configure - A lambda for Compose Viewport configuration.
 * See [ComposeViewportConfiguration] for available options.
 * @param content - The Composable content to be rendered on the `<canvas>` element.
 */
@ExperimentalComposeUiApi
fun ComposeViewport(
    viewportContainerId: String? = null,
    configure: ComposeViewportConfiguration.() -> Unit = {},
    content: @Composable () -> Unit = { }
) {
    InternalComposeViewport(
        viewportContainerId = viewportContainerId,
        configure = configure,
        content = content
    )
}

internal expect fun InternalComposeViewport(
    viewportContainerId: String? = null,
    configure: ComposeViewportConfiguration.() -> Unit = {},
    content: @Composable () -> Unit = { }
)

// In K/JS target, an application can't start right away. We should wait until skiko.wasm is ready.
// We'll do it implicitly, rather than asking the app developers to call it.
internal expect fun onSkikoReady(block: () -> Unit)

internal expect fun onDomReady(block: () -> Unit)