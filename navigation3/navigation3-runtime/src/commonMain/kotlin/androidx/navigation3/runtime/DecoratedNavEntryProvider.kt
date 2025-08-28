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

@file:Suppress("UNCHECKED_CAST")

package androidx.navigation3.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.jvm.JvmSuppressWildcards

/**
 * Function that provides all of the [NavEntry]s wrapped with the given [NavEntryDecorator]s. It is
 * responsible for executing the functions provided by each [NavEntryDecorator] appropriately.
 *
 * Note: the order in which the [NavEntryDecorator]s are added to the list determines their scope,
 * i.e. a [NavEntryDecorator] added earlier in a list has its data available to those added later.
 *
 * @param T the type of the backStack key
 * @param backStack the list of keys that represent the backstack
 * @param entryDecorators the [NavEntryDecorator]s that are providing data to the content
 * @param entryProvider a function that returns the [NavEntry] for a given key
 * @param content the content to be displayed
 */
@Composable
public fun <T : Any> DecoratedNavEntryProvider(
    backStack: List<T>,
    entryProvider: (key: T) -> NavEntry<out T>,
    entryDecorators: List<@JvmSuppressWildcards NavEntryDecorator<*>> =
        listOf(rememberSavedStateNavEntryDecorator()),
    content: @Composable (List<NavEntry<T>>) -> Unit,
) {
    // Kotlin does not know these things are compatible so we need this explicit cast
    // to ensure our lambda below takes the correct type
    entryProvider as (T) -> NavEntry<T>
    val entries =
        backStack.fastMapOrMap { key ->
            val entry = entryProvider.invoke(key)
            decorateEntry(entry, entryDecorators)
        }

    // Provides the entire backstack to the previously wrapped entries
    val initial: @Composable () -> Unit = remember(entries) { { content(entries) } }

    PrepareBackStack(entries, entryDecorators, initial)
}

/**
 * Wraps a [NavEntry] with the list of [NavEntryDecorator] in the order that the decorators were
 * added to the list.
 *
 * Invokes pop callback for popped entries that had pop animations and thus could not be cleaned up
 * by [PrepareBackStack]. PrepareBackStack has no access to animation state so we rely on this
 * function to call onPop when animation finishes.
 */
@Composable
internal fun <T : Any> decorateEntry(
    entry: NavEntry<T>,
    decorators: List<NavEntryDecorator<*>>,
): NavEntry<T> {
    val latestDecorators by rememberUpdatedState(decorators)
    val initial =
        object : NavEntryWrapper<T>(entry) {
            @Composable
            override fun Content() {
                val localInfo = LocalNavEntryDecoratorLocalInfo.current
                val idsInComposition = localInfo.idsInComposition
                DisposableEffect(key1 = contentKey) {
                    idsInComposition.add(contentKey)
                    onDispose {
                        val notInComposition = idsInComposition.remove(contentKey)
                        val popped = !localInfo.contentKeys.contains(contentKey)
                        if (popped && notInComposition) {
                            // we reverse the scopes before popping to imitate the order
                            // of onDispose calls if each scope/decorator had their own
                            // onDispose
                            // calls for clean up
                            // convert to mutableList first for backwards compat.
                            latestDecorators.fastForEachReversedOrForEachReversed {
                                it.onPop(contentKey)
                            }
                        }
                    }
                }
                DecorateNavEntry(entry, decorators)
            }
        }
    return initial
}

/**
 * Sets up logic to track changes to the backstack and invokes the [DecoratedNavEntryProvider]
 * content.
 *
 * Invokes pop callback for popped entries that:
 * 1. are not animating (i.e. no pop animations) AND / OR
 * 2. have never been composed (i.e. never invoked with [DecorateNavEntry])
 */
@Composable
internal fun <T : Any> PrepareBackStack(
    entries: List<NavEntry<T>>,
    decorators: List<NavEntryDecorator<*>>,
    content: @Composable (() -> Unit),
) {
    val localInfo = remember { NavEntryDecoratorLocalInfo() }
    val contentKeys = localInfo.contentKeys

    // update this backStack so that onDispose has access to the latest backStack to check
    // if an entry has been popped
    val latestEntries by rememberUpdatedState(entries)
    val latestDecorators by rememberUpdatedState(decorators)
    entries.fastForEachOrForEach {
        val contentKey = it.contentKey
        contentKeys.add(contentKey)

        DisposableEffect(contentKey) {
            onDispose {
                val latestBackStack = latestEntries.fastMapOrMap { entry -> entry.contentKey }
                val popped =
                    if (!latestBackStack.contains(contentKey)) {
                        contentKeys.remove(contentKey)
                    } else false
                // run onPop callback
                if (popped && !localInfo.idsInComposition.contains(contentKey)) {
                    // we reverse the order before popping to imitate the order
                    // of onDispose calls if each scope/decorator had their own onDispose
                    // calls for clean up
                    latestDecorators.fastForEachReversedOrForEachReversed { it.onPop(contentKey) }
                }
            }
        }
    }
    CompositionLocalProvider(LocalNavEntryDecoratorLocalInfo provides localInfo, content)
}

private class NavEntryDecoratorLocalInfo {
    val contentKeys: MutableSet<Any> = mutableSetOf()
    val idsInComposition: MutableSet<Any> = mutableSetOf()
}

private val LocalNavEntryDecoratorLocalInfo =
    staticCompositionLocalOf<NavEntryDecoratorLocalInfo> {
        error(
            "CompositionLocal LocalProviderLocalInfo not present. You must call " +
                "ProvideToBackStack before calling ProvideToEntry."
        )
    }
