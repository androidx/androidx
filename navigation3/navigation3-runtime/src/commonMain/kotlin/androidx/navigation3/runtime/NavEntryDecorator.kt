/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.navigation3.runtime

import androidx.compose.runtime.Composable
import kotlin.jvm.JvmSuppressWildcards

/**
 * Marker class to hold the onPop and decorator functions that will be invoked at runtime.
 *
 * See documentation on [androidx.navigation3.runtime.navEntryDecorator] for more info.
 */
public class NavEntryDecorator<T : Any>
internal constructor(
    internal val onPop: (key: Any) -> Unit,
    internal val navEntryDecorator: @Composable (entry: NavEntry<T>) -> Unit,
)

/**
 * Function to decorate the [NavEntry] that are integrated with a [rememberDecoratedNavEntries].
 *
 * Primary usages include but are not limited to:
 * 1. provide information to entries with [androidx.compose.runtime.CompositionLocal], i.e.
 *
 * ```
 * val decorator = navEntryDecorator<Any> { entry ->
 *    ...
 *    CompositionLocalProvider(LocalMyStateProvider provides myState) {
 *        entry.content.invoke(entry.key)
 *    }
 * }
 * ```
 * 2. Wrap entry content with other composable content
 *
 * ```
 * val decorator = navEntryDecorator<Any> { entry ->
 *    ...
 *    MyComposableFunction {
 *        entry.content.invoke(entry.key)
 *    }
 * }
 * ```
 *
 * @param T the type of the backStack key
 * @param onPop the callback to clean up the decorator state for a [NavEntry] when the entry is
 *   popped from the backstack and is leaving composition.The lambda provides the
 *   [NavEntry.contentKey] of the popped entry as input.
 * @param [decorator] the composable function to decorate a [NavEntry]. Note that this function only
 *   gets invoked for NavEntries that are actually getting rendered (i.e. by invoking the
 *   [NavEntry.content].)
 */
public fun <T : Any> navEntryDecorator(
    onPop: (contentKey: Any) -> Unit = {},
    decorator: @Composable (entry: NavEntry<T>) -> Unit,
): NavEntryDecorator<T> = NavEntryDecorator(onPop, decorator)

/**
 * Wraps a [NavEntry] with the list of [NavEntryDecorator] in the order that the decorators were
 * added to the list and invokes the content of the wrapped entry.
 *
 * @param T the type of the backStack key
 * @param entry the [NavEntry] to wrap
 * @param entryDecorators the list of decorators to wrap the [entry] with
 */
@Composable
public fun <T : Any> DecorateNavEntry(
    entry: NavEntry<T>,
    entryDecorators: List<@JvmSuppressWildcards NavEntryDecorator<*>>,
) {
    @Suppress("UNCHECKED_CAST")
    (entryDecorators as List<@JvmSuppressWildcards NavEntryDecorator<T>>)
        .fastDistinctOrDistinct()
        .foldRight(initial = entry) { decorator, wrappedEntry ->
            object : NavEntryWrapper<T>(wrappedEntry) {
                @Composable
                override fun Content() {
                    decorator.navEntryDecorator(wrappedEntry)
                }
            }
        }
        .Content()
}
