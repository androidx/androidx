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
import androidx.compose.ui.scene.ComposeHostingView
import androidx.compose.ui.uikit.ComposeUIViewConfiguration
import platform.UIKit.UIView

/**
 * Creates a [UIView] that can host Compose content.
 *
 * This method is a convenience wrapper around the [ComposeUIView] function with the default
 * configuration.
 *
 * @param content A composable lambda defining the UI content to be displayed within the
 * [ComposeUIView].
 * @return A [UIView] instance capable of hosting the specified Compose content.
 */
@ExperimentalComposeUiApi
fun ComposeUIView(content: @Composable () -> Unit): UIView =
    ComposeUIView(configure = {}, content = content)

/**
 * Creates a [UIView] capable of hosting Compose content.
 *
 * @param configure A lambda function used to configure the behavior of the created [ComposeUIView].
 * @param content A composable lambda defining the UI content to be displayed within the
 * [ComposeUIView].
 * @return A configured [UIView] instance capable of displaying the provided Compose content.
 */
@ExperimentalComposeUiApi
fun ComposeUIView(
    configure: ComposeUIViewConfiguration.() -> Unit = {},
    content: @Composable () -> Unit
): UIView = ComposeHostingView(
    configuration = ComposeUIViewConfiguration().apply(configure),
    content = content,
)
