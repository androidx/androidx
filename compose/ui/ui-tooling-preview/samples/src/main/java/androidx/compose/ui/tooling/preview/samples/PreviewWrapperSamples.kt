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

package androidx.compose.ui.tooling.preview.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.tooling.preview.PreviewWrapperProvider

/** Basic sample showing how to implement a [PreviewWrapperProvider] to provide a custom theme. */
@Sampled
fun PreviewWrapperProviderSample() {
    class CustomThemeWrapper : PreviewWrapperProvider {
        @Composable
        override fun Wrap(content: @Composable () -> Unit) {
            // Apply your custom theme or environment here
            // MyTheme {
            content()
            // }
        }
    }
}

/** Basic sample showing how to use [PreviewWrapper] to apply a custom wrapper to a preview. */
@Preview
@Sampled
@Composable
@PreviewWrapper(wrapper = SampleThemeWrapper::class)
fun PreviewWrapperSample() {
    // Your component content here
}

internal class SampleThemeWrapper : PreviewWrapperProvider {
    @Composable
    override fun Wrap(content: @Composable () -> Unit) {
        // Apply your custom theme here
        // MyTheme {
        content()
        // }
    }
}

/** MultiPreview annotation for different font scales. */
@Preview(name = "Small", fontScale = 0.8f)
@Preview(name = "Large", fontScale = 1.2f)
annotation class FontPreviews

/** Sample showing [PreviewWrapper] used in conjunction with a MultiPreview annotation. */
@FontPreviews
@Sampled
@Composable
@PreviewWrapper(wrapper = SampleThemeWrapper::class)
fun PreviewWrapperMultiPreviewSample() {
    // Your component content here
}

/** Sample showing how to combine multiple wrappers using a composite [PreviewWrapper]. */
@Preview
@Sampled
@Composable
@PreviewWrapper(wrapper = ThemeAndRemoteWrapper::class)
fun PreviewWrapperCompositeSample() {
    // Your component content here
}

internal class ThemeWrapper : PreviewWrapperProvider {
    @Composable
    override fun Wrap(content: @Composable () -> Unit) {
        content()
    }
}

internal class RemoteComposeWrapper : PreviewWrapperProvider {
    @Composable
    override fun Wrap(content: @Composable () -> Unit) {
        content()
    }
}

/** A composite wrapper that combines multiple individual [PreviewWrapper]s. */
internal class ThemeAndRemoteWrapper : PreviewWrapperProvider {
    private val themeWrapper = ThemeWrapper()
    private val remoteWrapper = RemoteComposeWrapper()

    @Composable
    override fun Wrap(content: @Composable () -> Unit) {
        // Nest the wrappers: Theme is usually the outermost layer,
        // followed by the environment/container wrapper.
        themeWrapper.Wrap { remoteWrapper.Wrap { content() } }
    }
}
