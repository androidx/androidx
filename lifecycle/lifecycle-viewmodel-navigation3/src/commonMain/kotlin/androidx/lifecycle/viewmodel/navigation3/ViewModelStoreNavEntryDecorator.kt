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

@file:JvmName("ViewModelStoreNavEntryDecoratorKt")
@file:JvmMultifileClass

package androidx.lifecycle.viewmodel.navigation3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Returns a [ViewModelStoreNavEntryDecorator] that is remembered across recompositions.
 *
 * @param [viewModelStoreOwner] The [ViewModelStoreOwner] that provides the [ViewModelStore] to
 *   NavEntries
 * @param [removeViewModelStoreOnPop] A lambda that returns a Boolean for whether the store for a
 *   [NavEntry] should be removed when the [NavEntry] is popped from the backStack. If true, the
 *   entry's ViewModelStore will be removed.
 */
@Composable
@Deprecated(
    message =
        "This parameter was a workaround for detecting configuration changes and was never " +
            "intended for conditional popping. Configuration changes are now handled internally. " +
            "All decorator state must clear at the same time on pop. To keep decorator state " +
            "around outside of when a back stack is passed to a NavDisplay, use the " +
            "rememberDecoratedNavEntries API.",
    replaceWith = ReplaceWith("rememberViewModelStoreNavEntryDecorator(viewModelStoreOwner)"),
)
public fun <T : Any> rememberViewModelStoreNavEntryDecorator(
    viewModelStoreOwner: ViewModelStoreOwner =
        checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        },
    removeViewModelStoreOnPop: () -> Boolean = { true },
): ViewModelStoreNavEntryDecorator<T> {
    return remember(viewModelStoreOwner) {
        ViewModelStoreNavEntryDecorator(viewModelStoreOwner.viewModelStore)
    }
}

/**
 * Returns a [ViewModelStoreNavEntryDecorator] that is remembered across recompositions.
 *
 * @param [viewModelStoreOwner] The [ViewModelStoreOwner] that provides the [ViewModelStore] to
 *   NavEntries
 */
@Composable
public fun <T : Any> rememberViewModelStoreNavEntryDecorator(
    viewModelStoreOwner: ViewModelStoreOwner =
        checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        }
): ViewModelStoreNavEntryDecorator<T> {
    return remember(viewModelStoreOwner) {
        ViewModelStoreNavEntryDecorator(viewModelStoreOwner.viewModelStore)
    }
}

/**
 * Provides the content of a [NavEntry] with a [ViewModelStoreOwner] and provides that
 * [ViewModelStoreOwner] as a [LocalViewModelStoreOwner] so that it is available within the content.
 *
 * This requires the usage of [androidx.navigation3.runtime.SaveableStateHolderNavEntryDecorator] to
 * ensure that the [NavEntry] scoped [ViewModel]s can properly provide access to
 * [androidx.lifecycle.SavedStateHandle]s
 *
 * @param [viewModelStore] The [ViewModelStore] that provides to NavEntries
 * @param [removeViewModelStoreOnPop] A lambda that returns a Boolean for whether the store for a
 *   [NavEntry] should be cleared when the [NavEntry] is popped from the backStack. If true, the
 *   entry's ViewModelStore will be removed.
 * @see NavEntryDecorator.onPop for more details on when this callback is invoked
 */
public class ViewModelStoreNavEntryDecorator<T : Any>
private constructor(viewModelStoreProvider: ViewModelStoreProvider) :
    NavEntryDecorator<T>(
        onPop = { key -> viewModelStoreProvider.clearKey(key) },
        decorate = { entry ->
            val owner =
                rememberViewModelStoreOwner(
                    viewModelStoreProvider,
                    entry.contentKey,
                    savedStateRegistryOwner = LocalSavedStateRegistryOwner.current,
                )
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) { entry.Content() }
        },
    ) {

    @Deprecated(
        message =
            "This parameter was a workaround for detecting configuration changes and was never " +
                "intended for conditional popping. Configuration changes are now handled " +
                "internally. All decorator state must clear at the same time on pop. To keep " +
                "decorator state around outside of when a back stack is passed to a NavDisplay, " +
                "use the rememberDecoratedNavEntries API.",
        replaceWith = ReplaceWith("ViewModelStoreNavEntryDecorator(viewModelStore)"),
    )
    public constructor(
        viewModelStore: ViewModelStore,
        removeViewModelStoreOnPop: () -> Boolean,
    ) : this(ViewModelStoreProvider(viewModelStore))

    public constructor(
        viewModelStore: ViewModelStore
    ) : this(ViewModelStoreProvider(viewModelStore))
}

/** Holds the default functions for the [ViewModelStoreNavEntryDecorator]. */
@Deprecated(
    message =
        "This object is obsolete. The removeViewModelStoreOnPop parameter was a workaround for " +
            "detecting configuration changes, which are now handled internally. To preserve " +
            "decorator state outside of a NavDisplay, use the rememberDecoratedNavEntries API."
)
public object ViewModelStoreNavEntryDecoratorDefaults {
    /**
     * Controls whether the [ViewModelStoreNavEntryDecorator] should clear the ViewModelStore scoped
     * to a [NavEntry] when [NavEntryDecorator.onPop] is invoked for that [NavEntry]'s
     * [NavEntry.contentKey]
     *
     * The ViewModelStore is cleared if this returns true. The store is retained if false.
     */
    @Composable
    @Suppress("PairedRegistration")
    public fun removeViewModelStoreOnPop(): () -> Boolean = { true }
}
