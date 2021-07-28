/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.navigation.compose.material

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.lifecycle.Lifecycle
import androidx.navigation.FloatingWindow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.NavigatorState
import androidx.navigation.compose.material.BottomSheetNavigator.Destination

/**
 * Create and remember a [BottomSheetNavigator]
 *
 * @param sheetState The [ModalBottomSheetState] that the [BottomSheetNavigator] will use to
 * drive the sheet state
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
public fun rememberBottomSheetNavigator(
    sheetState: ModalBottomSheetState =
        rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
): BottomSheetNavigator = remember(sheetState) {
    BottomSheetNavigator(sheetState = sheetState)
}

/**
 * Navigator that drives a [ModalBottomSheetState] for use of [ModalBottomSheetLayout]s
 * with the navigation library. Every destination using this Navigator must set a valid
 * [Composable] by setting it directly on an instantiated [Destination] or calling
 * [androidx.navigation.compose.material.bottomSheet].
 *
 * <b>The [sheetContent] [Composable] will always host the latest entry of the back stack. When
 * navigating from a [BottomSheetNavigator.Destination] to another
 * [BottomSheetNavigator.Destination], the content of the sheet will be replaced instead of a
 * new bottom sheet being shown.</b>
 *
 * When the sheet is dismissed by the user, the [state]'s [NavigatorState.backStack] will be popped.
 *
 * @param sheetState The [ModalBottomSheetState] that the [BottomSheetNavigator] will use to
 * drive the sheet state
 */
@OptIn(ExperimentalMaterialApi::class)
@Navigator.Name("BottomSheetNavigator")
public class BottomSheetNavigator(
    val sheetState: ModalBottomSheetState
) : Navigator<BottomSheetNavigator.Destination>() {

    /**
     * A [Composable] function that hosts the current sheet content. This should be set as
     * sheetContent of your [ModalBottomSheetLayout].
     */
    public val sheetContent: @Composable ColumnScope.() -> Unit = @Composable {
        val columnScope = this
        val saveableStateHolder = rememberSaveableStateHolder()
        val backStackEntries by state.backStack.collectAsState()
        val transitionsInProgress by state.transitionsInProgress.collectAsState()

        // We always replace the sheet's content instead of overlaying and nesting floating
        // window destinations. That means that only *one* concurrent destination is supported by
        // this navigator.
        val latestEntry = backStackEntries.lastOrNull { entry ->
            // We might have entries in the back stack that aren't started currently, so filter
            // these
            entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }

        SheetContentHost(
            columnHost = columnScope,
            backStackEntry = latestEntry,
            sheetState = sheetState,
            saveableStateHolder = saveableStateHolder,
            onSheetShown = { backStackEntry ->
                val transitionForEntry = transitionsInProgress[backStackEntry]
                transitionForEntry?.onTransitionComplete()
            },
            onSheetDismissed = { backStackEntry ->
                // Sheet dismissal can be started through popBackStack in which case we have a
                // transition that we'll want to complete
                val transitionForEntry = transitionsInProgress[backStackEntry]
                if (transitionForEntry != null) {
                    transitionForEntry.onTransitionComplete()
                } else {
                    // When the sheet has been dismissed by the user we always want to pop
                    // the whole back stack of this navigator's state
                    state.pop(popUpTo = state.backStack.value.first(), saveState = false)
                }
            }
        )
    }

    override fun createDestination(): Destination = Destination(navigator = this, content = {})

    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ) {
        entries.forEach { entry ->
            state.pushWithTransition(entry)
        }
    }

    override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        state.popWithTransition(popUpTo, savedState)
    }

    /**
     * [NavDestination] specific to [BottomSheetNavigator]
     */
    @NavDestination.ClassType(Composable::class)
    public class Destination(
        navigator: BottomSheetNavigator,
        internal val content: @Composable ColumnScope.(NavBackStackEntry) -> Unit
    ) : NavDestination(navigator), FloatingWindow
}
