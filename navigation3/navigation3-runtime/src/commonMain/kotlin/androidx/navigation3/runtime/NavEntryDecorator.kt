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
import androidx.compose.runtime.Immutable

/**
 * Decorate the [NavEntry]s that are integrated with a [rememberDecoratedNavEntries].
 *
 * **HOW TO USE** Primary usages include but are not limited to:
 * 1. provide information to entries with [androidx.compose.runtime.CompositionLocal], i.e.
 *
 * ```
 * val decorator = NavEntryDecorator<Any> { entry ->
 *    ...
 *    CompositionLocalProvider(LocalMyStateProvider provides myState) {
 *        entry.content.invoke(entry.key)
 *    }
 * }
 * ```
 * 2. Wrap entry content with other composable content
 *
 * ```
 * val decorator = NavEntryDecorator<Any> { entry ->
 *    ...
 *    MyComposableFunction {
 *        entry.content.invoke(entry.key)
 *    }
 * }
 * ```
 *
 * **REUSABILITY** To enhance reusability, the NavEntryDecorator can be returned by a function or
 * subclassed:
 * ```
 * // a reusable function
 * fun <T : Any> myDecorator(val myState: MyState): NavEntryDecorator<T> =
 *     NavEntryDecorator(onPop = { contentKey ->  myState.clear(contentKey) }) { entry ->
 *           myState.storeState(entry.contentKey)
 *           entry.Content()
 *     }
 *
 * // or subclass NavEntryDecorator
 * class MyDecorator(
 *      val myState: MyState
 * ): NavEntryDecorator(
 *      onPop = { contentKey ->  myState.clear(contentKey) },
 *      decorate = { entry ->
 *          myState.storeState(entry.contentKey)
 *          entry.Content()
 *      }
 * )
 * ```
 *
 * @param T the type of the backStack key
 * @param onPop the callback to clean up the decorator state associated with a [NavEntry.contentKey]
 *   when the last [NavEntry] with that contentKey has been popped from the backStack. It provides
 *   the [NavEntry.contentKey] of the popped entry as input. This callback is invoked if and only if
 *   all these conditions are met:
 *     1. A [NavEntry] has been popped from the backStack
 *     2. The [NavEntry] that has been popped is the last entry on the backStack with that
 *        particular [NavEntry.contentKey]
 *     3. The [NavEntry.content] of the popped NavEntry has left composition
 *
 * @param [decorate] the composable function to decorate a [NavEntry]. Note that this function only
 *   gets invoked for NavEntries that are actually getting rendered (i.e. by invoking the
 *   [NavEntry.content].)
 * @see NavEntry.contentKey
 */
@Immutable
public open class NavEntryDecorator<T : Any>(
    internal val onPop: (key: Any) -> Unit = {},
    internal val decorate: @Composable (entry: NavEntry<T>) -> Unit,
)
