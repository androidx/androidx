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

import androidx.compose.ui.ExperimentalComposeUiApi

/**
 * Configuration of [ComposeViewport] behavior.
 */
@ExperimentalComposeUiApi
class ComposeViewportConfiguration internal constructor() {

    /**
     * Indicates whether accessibility (a11y) is enabled for the associated Compose viewport.
     * When it's enabled, the Compose Viewport will maintain a DOM tree mirroring the Compose semantics nodes.
     * That DOM tree is visibly hidden, but reachable by the accessibility tools.
     * It can be disabled to avoid the overhead of maintaining the DOM tree.
     * By default, it is set to `true`.
     *
     * Note: This API is experimental and subject to change in the future.
     */
    @ExperimentalComposeUiApi
    var isA11YEnabled: Boolean = true
}