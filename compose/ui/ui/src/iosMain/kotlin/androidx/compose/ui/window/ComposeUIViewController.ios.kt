/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.compose.ui.scene.ComposeHostingViewController
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import platform.UIKit.UIViewController

/**
 * Creates a [UIViewController] that can host Compose content.
 *
 * This method is a convenience wrapper around the [ComposeUIViewController] function with the
 * default configuration.
 *
 * @param content a composable lambda defining the UI content to be displayed within the
 * [UIViewController].
 * @return a [UIViewController] instance capable of hosting the specified Compose content.
 */
fun ComposeUIViewController(content: @Composable () -> Unit): UIViewController =
    ComposeUIViewController(configure = {}, content = content)

/**
 * Creates a [UIViewController] capable of hosting Compose content.
 *
 * @param configure A lambda function used to configure the behavior of the created
 * [ComposeUIViewController].
 * @param content A composable lambda defining the UI content to be displayed within the
 * [ComposeUIViewController].
 * @return A configured [UIViewController] instance capable of displaying the provided Compose
 * content.
 */
fun ComposeUIViewController(
    configure: ComposeUIViewControllerConfiguration.() -> Unit = {},
    content: @Composable () -> Unit
): UIViewController = ComposeHostingViewController(
    configuration = ComposeUIViewControllerConfiguration().apply(configure),
    content = content,
)
