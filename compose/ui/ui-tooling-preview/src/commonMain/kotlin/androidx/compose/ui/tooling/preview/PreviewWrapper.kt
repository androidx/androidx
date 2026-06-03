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

package androidx.compose.ui.tooling.preview

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

/**
 * Interface used to define custom rendering logic for Compose Previews in Android Studio.
 *
 * Implementations of this interface allow developers to wrap the content of a [Preview] to provide
 * specific environments, themes, or containers (such as a Remote Compose) without requiring
 * repetitive code in every preview function.
 *
 * **Usage:** Implementations are applied to previews using the [PreviewWrapper] annotation.
 *
 * @see PreviewWrapper
 */
interface PreviewWrapperProvider {

    /**
     * Wraps the provided [content] with custom UI logic or containers.
     *
     * Example usage for applying a Theme:
     * ```kotlin
     * class CustomThemeWrapper : PreviewWrapperProvider {
     *     @Composable
     *     override fun Wrap(content: @Composable () -> Unit) {
     *         // Apply a light theme and provide a full-screen Surface to set a default background
     *         // color for the preview content.
     *         MaterialTheme(colorScheme = lightColorScheme()) {
     *             Surface(modifier = Modifier.fillMaxSize()) {
     *                 content()
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * @param content The original composable content of the function annotated with [Preview].
     */
    @Composable fun Wrap(content: @Composable () -> Unit)
}

/**
 * Annotation used to associate a [PreviewWrapperProvider] with a Composable.
 *
 * When a preview is rendered, Android Studio looks for this annotation to determine if the preview
 * content should be wrapped in a custom container (e.g., for Remote Compose or custom theming).
 *
 * **Scope and Precedence**
 *
 * This annotation is not repeatable. Each preview rendered uses at most one wrapper. The wrapper is
 * applied to all [Preview]s associated with this function, including direct `@Preview` annotations
 * and MultiPreview annotations.
 *
 * **Examples**
 *
 * **1. Basic Usage**
 *
 * ```kotlin
 * class SampleScaffoldWrapper : PreviewWrapperProvider {
 *     @Composable
 *     override fun Wrap(content: @Composable () -> Unit) {
 *         // Wrap the content in a Material3 Scaffold to provide a standard app structure
 *         MaterialTheme {
 *             Scaffold { padding ->
 *                 Box(Modifier.padding(padding)) { content() }
 *             }
 *         }
 *     }
 * }
 *
 * @Preview
 * @Composable
 * @PreviewWrapper(wrapper = SampleScaffoldWrapper::class)
 * fun PreviewWrapperSample() {
 *     // Your component content here
 * }
 * ```
 *
 * **2. Usage with MultiPreview**
 *
 * ```kotlin
 * @Preview(name = "Small", fontScale = 0.8f)
 * @Preview(name = "Large", fontScale = 1.2f)
 * annotation class FontPreviews
 *
 * @FontPreviews
 * @Composable
 * @PreviewWrapper(wrapper = SampleScaffoldWrapper::class)
 * fun PreviewWrapperMultiPreviewSample() {
 *     // Your component content here
 * }
 * ```
 *
 * **3. Combining Multiple Wrappers**
 *
 * Since [PreviewWrapper] allows only a single wrapper, you can create a composite wrapper to apply
 * multiple effects.
 *
 * ```kotlin
 * class ThemeWrapper : PreviewWrapperProvider {
 *     @Composable
 *     override fun Wrap(content: @Composable () -> Unit) {
 *         content()
 *     }
 * }
 *
 * class RemoteComposeWrapper : PreviewWrapperProvider {
 *     @Composable
 *     override fun Wrap(content: @Composable () -> Unit) {
 *         content()
 *     }
 * }
 *
 * class ThemeAndRemoteWrapper : PreviewWrapperProvider {
 *     private val themeWrapper = ThemeWrapper()
 *     private val remoteWrapper = RemoteComposeWrapper()
 *
 *     @Composable
 *     override fun Wrap(content: @Composable () -> Unit) {
 *         // Nest the wrappers: Theme is usually the outermost layer,
 *         // followed by the environment/container wrapper.
 *         themeWrapper.Wrap { remoteWrapper.Wrap { content() } }
 *     }
 * }
 *
 * @Preview
 * @Composable
 * @PreviewWrapper(wrapper = ThemeAndRemoteWrapper::class)
 * fun PreviewWrapperCompositeSample() {
 *     // Your component content here
 * }
 * ```
 *
 * @param wrapper The [KClass] of the [PreviewWrapperProvider] implementation to use. Must have a
 *   default zero-argument constructor.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
annotation class PreviewWrapper(val wrapper: KClass<out PreviewWrapperProvider>)
