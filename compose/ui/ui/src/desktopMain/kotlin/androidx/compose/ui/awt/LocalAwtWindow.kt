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

package androidx.compose.ui.awt

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene

/**
 * Window-owner of the current composition.
 *
 * This could be:
 * - A Compose window, e.g., [ComposeWindow] or [ComposeDialog]
 * - A non-Compose window, e.g., [java.awt.Frame] or [java.awt.Dialog] if Compose is embedded into
 *   Swing via [ComposePanel].
 * - `null`, if the current composition is not inside a window, such as in unit tests, or with
 *   [ImageComposeScene].
 */
@ExperimentalComposeUiApi
val LocalAwtWindow = staticCompositionLocalOf<java.awt.Window?> { null }