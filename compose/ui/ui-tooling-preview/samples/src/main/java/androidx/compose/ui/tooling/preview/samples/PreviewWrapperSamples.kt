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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.tooling.preview.PreviewWrapperProvider

/*
 * NOTE: Any change in this sample file needs to be manually copied over to the KDoc in
 * PreviewWrapper.kt and vice versa. This is to ensure the documentation examples remain
 * accurate, self-contained, and compile correctly.
 */

/** Basic sample showing how to implement a [PreviewWrapperProvider] to provide a custom theme. */
fun PreviewWrapperProviderSample() {
    class CustomThemeWrapper : PreviewWrapperProvider {
        @Composable
        override fun Wrap(content: @Composable () -> Unit) {
            // Apply a light theme and provide a full-screen Surface to set a default background
            // color for the preview content.
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) { content() }
            }
        }
    }
}

/** Basic sample showing how to use [PreviewWrapper] to apply a custom wrapper to a preview. */
@Preview
@Composable
@PreviewWrapper(wrapper = SampleScaffoldWrapper::class)
fun PreviewWrapperSample() {
    // Your component content here
}

internal class SampleScaffoldWrapper : PreviewWrapperProvider {
    @Composable
    override fun Wrap(content: @Composable () -> Unit) {
        // Wrap the content in a Material3 Scaffold to provide a standard app structure
        MaterialTheme { Scaffold { padding -> Box(Modifier.padding(padding)) { content() } } }
    }
}

/** MultiPreview annotation for different font scales. */
@Preview(name = "Small", fontScale = 0.8f)
@Preview(name = "Large", fontScale = 1.2f)
annotation class FontPreviews

/** Sample showing [PreviewWrapper] used in conjunction with a MultiPreview annotation. */
@FontPreviews
@Composable
@PreviewWrapper(wrapper = SampleScaffoldWrapper::class)
fun PreviewWrapperMultiPreviewSample() {
    // Your component content here
}

/** Sample showing how to combine multiple wrappers using a composite [PreviewWrapper]. */
@Preview
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
