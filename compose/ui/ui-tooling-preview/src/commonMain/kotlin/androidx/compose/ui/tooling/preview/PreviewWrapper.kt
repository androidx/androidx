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
 * **Usage:** Implementations are applied to previews using the [PreviewWrapperProvider] annotation.
 *
 * @see PreviewWrapperProvider
 */
interface PreviewWrapper {

    /**
     * Wraps the provided [content] with custom UI logic or containers.
     *
     * Example usage for applying a Theme:
     * ```
     * @Composable
     * override fun Wrap(content: @Composable () -> Unit) {
     *     MyTheme {
     *         content()
     *     }
     * }
     * ```
     *
     * @param content The original composable content of the function annotated with [Preview].
     */
    @Composable fun Wrap(content: @Composable () -> Unit)
}

/**
 * Annotation used to associate a [PreviewWrapper] with a Composable.
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
 * class CustomThemeWrapper : PreviewWrapper {
 *     @Composable
 *     override fun Wrap(content: @Composable () -> Unit) {
 *         // Apply your custom theme here
 *         MyTheme {
 *             content()
 *         }
 *     }
 * }
 *
 * @PreviewWrapperProvider(wrapper = CustomThemeWrapper::class)
 * @Preview
 * @Composable
 * fun MyThemedComponent() { ... }
 * ```
 *
 * **2. Usage with MultiPreview** The wrapper `CustomThemeWrapper` will be applied to both "Small"
 * and "Large" previews.
 *
 * ```kotlin
 * @Preview(name = "Small", fontScale = 0.8f)
 * @Preview(name = "Large", fontScale = 1.2f)
 * annotation class FontPreviews
 *
 * @PreviewWrapperProvider(wrapper = CustomThemeWrapper::class)
 * @FontPreviews
 * @Composable
 * fun MyMultiPreviewComponent() { ... }
 * ```
 *
 * **3. Combining Multiple Wrappers**
 *
 * Since [PreviewWrapperProvider] allows only a single wrapper, you can create a composite wrapper
 * to apply multiple effects.
 *
 * ```kotlin
 * // A composite wrapper that combines Theming and Remote Compose logic.
 * class ThemeAndRemoteWrapper : PreviewWrapper {
 *
 *     // Instantiate the individual wrappers
 *     private val themeWrapper = ThemeWrapper()
 *     private val remoteWrapper = RemoteComposeWrapper()
 *
 *     @Composable
 *     override fun Wrap(content: @Composable () -> Unit) {
 *         // Nest the wrappers: Theme is usually the outermost layer,
 *         // followed by the environment/container wrapper.
 *         themeWrapper.Wrap {
 *             remoteWrapper.Wrap {
 *                 content()
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @param wrapper The [KClass] of the [PreviewWrapper] implementation to use. Must have a default
 *   zero-argument constructor.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class PreviewWrapperProvider(val wrapper: KClass<out PreviewWrapper>)
