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

package androidx.navigation3.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlin.jvm.JvmSuppressWildcards

/**
 * Remembers and returns a list of [NavEntry] decorated with the list of [entryDecorators]
 *
 * The returned list of [NavEntry] survives configuration change and recompositions but does not
 * survive process death. To ensure that the backStack and its states are recovered properly, the
 * [backStack] and the [entryDecorators]'s states passed into this function should be hoisted and
 * saved across process death.
 *
 * For example:
 * ```
 * // backStack saved during process death
 * val backStack = rememberNavBackStack()
 *
 * // the hoisted SaveableStateHolder used by the decorator is saved during process death
 * val decorators = listOf(rememberSavedStateNavEntryDecorator())
 *
 * // finally, decorate the entries
 * val entries = rememberDecoratedNavEntries(backStack, decorators, entryProvider)
 * ```
 *
 * **HOW TO USE** The returned list of entries should be stored and reused for the same backStack.
 * If you want to support multiple backStacks (i.e. each bottom tab with their own backStack), then
 * each backStack should be their own [rememberDecoratedNavEntries] with new [backStack] and
 * [entryDecorators] passed in.
 *
 * **MUTABLE BACKSTACK NAVIGATE/POP** The [backStack] should be a hoisted OBSERVABLE list, and
 * navigates or pops should be applied directly to this backStack. For example:
 * ```
 * val backStack = mutableStateListOf(A)
 * val entries = rememberDecoratedNavEntries(backStack, ...)
 *
 * // to navigate
 * backStack.add(B)
 *
 * // to pop
 * backStack.removeLastOrNull()
 * ```
 *
 * **IMMUTABLE BACKSTACK NAVIGATE/POP** This composable also supports navigates and pops with
 * immutable backStack. Simply replace the previous [backStack] with a new one but DO NOT replace
 * the decorator states. Also, DO NOT create a brand new [rememberDecoratedNavEntries]. For example
 *
 * ```
 *  val backStack = mutableStateOf(listOf(1, 2))
 *  val entries = rememberDecoratedNavEntries(backStack, ...)
 *
 *  // to navigate
 *  backStack.value = listOf(1, 2, 3)
 *
 *  // to pop
 *  backStack.value = listOf(1)
 * ```
 *
 * **MULTIPLE BACKSTACKS** Each call to [rememberDecoratedNavEntries] represents a single backStack.
 * To support multiple backStack, there should be one [rememberDecoratedNavEntries] for each
 * backStack. For example
 *
 * ```
 * val homeBackStack = mutableStateListOf(HomeKey)
 * val homeDecorators = mutableStateListOf(rememberSavedStateNavEntryDecorator(), ...)
 * val homeTabEntries = rememberDecoratedNavEntries(homeBackStack, homeDecorators, ...)
 *
 * val favoritesBackStack = mutableStateListOf(FavoritesKey)
 * val favoritesDecorators = mutableStateListOf(rememberSavedStateNavEntryDecorator(),...)
 * val favoritesTabEntries = rememberDecoratedNavEntries(favoritesBackStack, favoritesDecorators, ...)
 * ```
 *
 * You can also concatenate multiple backStacks to form a larger one. So, given the above setup:
 * ```
 * val concatenatedEntries = homeTabEntries + favoritesTabEntries
 *
 * // To navigate within the favorites backStack
 * favoritesBackStack.add(FavoritesDetailKey)
 * ```
 *
 * In this case, the updated favoritesBackStack and updated states will be reflected in
 * concatenatedEntries.
 *
 * @param T the type of the backStack key
 * @param backStack the list of keys that represent the backstack. If this backStack is observable,
 *   i.e. a [androidx.compose.runtime.snapshots.SnapshotStateList], then updates to this backStack
 *   will automatically trigger a re-calculation of the list of [NavEntry] to reflect the new
 *   backStack state.
 * @param entryDecorators the [NavEntryDecorator]s that are providing data to the content. If this
 *   list is observable (i.e. a [androidx.compose.runtime.snapshots.SnapshotStateList]), then
 *   updates to this list of decorators will automatically trigger a re-calculation of the list of
 *   [NavEntry] to reflect the new decorators state.
 * @param entryProvider a function that returns the [NavEntry] for a given key
 * @return a list of decorated [NavEntry]
 */
@Composable
public fun <T : Any> rememberDecoratedNavEntries(
    backStack: List<T>,
    entryDecorators: List<@JvmSuppressWildcards NavEntryDecorator<T>> = listOf(),
    entryProvider: (key: T) -> NavEntry<T>,
): List<NavEntry<T>> {
    @Suppress("ListIterator")
    val entries =
        remember(backStack.toList()) { backStack.fastMapOrMap { key -> entryProvider.invoke(key) } }

    return rememberDecoratedNavEntries(entries, entryDecorators)
}

/**
 * Decorates the [entries] with the [entryDecorators] and returns the list of decorated NavEntries.
 *
 * **WHEN TO USE** This API can be used to decorate undecorated [NavEntry] as well as entries that
 * have already been decorated with other [NavEntryDecorator].
 *
 * **HOW IT WORKS** When you redecorate NavEntries with this function, the [entryDecorators] passed
 * in here will be invoked first, followed by the original decorators that decorated the [entries].
 * For example
 *
 * ```
 * val originalDecorator = listOf(navEntryDecorator { println("original") })
 * val originalEntries = rememberDecoratedNavEntries(backStack, originalDecorator, ...)
 *
 * val newDecorator = listOf(navEntryDecorator { println("additional") })
 * val newEntries = rememberDecoratedNavEntries(originalEntries, newDecorator)
 *
 * // println output
 * additional
 * original
 * ```
 *
 * @param T the type of the backStack key
 * @param entries the list of NavEntry to decorate. If this list is observable, i.e. a
 *   [androidx.compose.runtime.snapshots.SnapshotStateList], then updates to this list will
 *   automatically trigger a re-calculation of the returned list of [NavEntry] to reflect the new
 *   state.
 * @param entryDecorators the [NavEntryDecorator]s that are providing data to the content. If this
 *   list is observable (i.e. a [androidx.compose.runtime.snapshots.SnapshotStateList]), then
 *   updates to this list of decorators will automatically trigger a re-calculation of the returned
 *   list of [NavEntry] to reflect the new decorators state.
 * @return a list of decorated [NavEntry]
 */
@Composable
public fun <T : Any> rememberDecoratedNavEntries(
    entries: List<NavEntry<T>>,
    entryDecorators: List<@JvmSuppressWildcards NavEntryDecorator<T>> = listOf(),
): List<NavEntry<T>> {
    val keysInBackstack: MutableSet<Any> = remember { mutableSetOf() }
    val keysInComposition: MutableSet<Any> = remember { mutableSetOf() }
    val decoratedEntries =
        entries.fastMapOrMap { entry ->
            decorateEntry(entry, entryDecorators, keysInBackstack, keysInComposition)
        }

    PrepareBackStack(decoratedEntries, entryDecorators, keysInBackstack, keysInComposition)
    return decoratedEntries
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
private fun <T : Any> decorateEntry(
    entry: NavEntry<T>,
    decorators: List<NavEntryDecorator<T>>,
    keysInBackstack: MutableSet<Any>,
    keysInComposition: MutableSet<Any>,
): NavEntry<T> {
    val latestDecorators by rememberUpdatedState(decorators)
    val contentKey = entry.contentKey

    // decorateEntry gets called in a loop by rememberDecoratedNavEntries for each entry in the
    // backstack.
    // Since looped content all share the same position in compose tree, we need to key the NavEntry
    // instance
    // to help compose differentiate between NavEntries if backStacks were swapped.
    key(contentKey) {
        return NavEntry(navEntry = entry) {
            val keysInComposition = keysInComposition
            // track if entry is in backstack and/or still in composition
            DisposableEffect(key1 = contentKey) {
                keysInComposition.add(contentKey)
                onDispose {
                    val notInComposition = keysInComposition.remove(contentKey)
                    val popped = !keysInBackstack.contains(contentKey)
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
            // wrap entry with decorators then invoke Content
            decorators
                .fastDistinctOrDistinct()
                .foldRight(initial = entry) { decorator, wrappedEntry ->
                    NavEntry<T>(wrappedEntry) { decorator.decorate(wrappedEntry) }
                }
                .Content()
        }
    }
}

/**
 * Sets up logic to track changes to the backstack
 *
 * Invokes pop callback for popped entries that:
 * 1. are not animating (i.e. no pop animations) AND / OR
 * 2. have never been composed
 */
@Composable
private fun <T : Any> PrepareBackStack(
    entries: List<NavEntry<T>>,
    decorators: List<NavEntryDecorator<T>>,
    keysInBackstack: MutableSet<Any>,
    keysInComposition: MutableSet<Any>,
) {
    val keysInBackStack = keysInBackstack

    // update this backStack so that onDispose has access to the latest backStack to check
    // if an entry has been popped
    val latestEntries by rememberUpdatedState(entries)
    val latestDecorators by rememberUpdatedState(decorators)
    entries.fastForEachOrForEach {
        val contentKey = it.contentKey
        keysInBackStack.add(contentKey)
        @Suppress("ListIterator")
        DisposableEffect(contentKey, entries.toList()) {
            onDispose {
                val latestBackStack = latestEntries.fastMapOrMap { entry -> entry.contentKey }
                val popped =
                    if (!latestBackStack.contains(contentKey)) {
                        keysInBackStack.remove(contentKey)
                    } else false
                // run onPop callback
                if (popped && !keysInComposition.contains(contentKey)) {
                    // we reverse the order before popping to imitate the order
                    // of onDispose calls if each scope/decorator had their own onDispose
                    // calls for clean up
                    latestDecorators.fastForEachReversedOrForEachReversed { it.onPop(contentKey) }
                }
            }
        }
    }
}
