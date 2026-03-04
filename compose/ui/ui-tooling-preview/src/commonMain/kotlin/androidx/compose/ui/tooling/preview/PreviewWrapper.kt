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
 * **Scope and Precedence:** This annotation is not repeatable. Each preview rendered uses at most
 * one wrapper.
 *
 * [AnnotationTarget.FUNCTION]: The wrapper is applied to previews directly annotating this
 * function, including MultiPreviews.
 *
 * **Example**
 *
 * ```kotlin
 * @PreviewWrapperProvider(wrapper = CustomThemeWrapper::class)
 * @Preview
 * @Composable
 * fun MyThemedComponent() { ... }
 * ```
 *
 * @param wrapper The [KClass] of the [PreviewWrapper] implementation to use. Must have a default
 *   zero-argument constructor.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class PreviewWrapperProvider(val wrapper: KClass<out PreviewWrapper>)
