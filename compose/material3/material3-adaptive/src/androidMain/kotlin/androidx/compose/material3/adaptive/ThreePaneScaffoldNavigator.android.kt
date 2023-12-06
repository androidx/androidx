/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * The navigation integration entry point of [ThreePaneScaffold] implementations.
 *
 * You can use [rememberListDetailPaneScaffoldNavigator] or
 * [rememberSupportingPaneScaffoldNavigator] to get remembered default instances of this interface
 * for [ListDetailPaneScaffold] and [SupportingPaneScaffold], respectively. Those default
 * implementations work independently from any navigation frameworks. Developers can also
 * integrate with other navigation frameworks by implementing this interface if needed.
 */
@ExperimentalMaterial3AdaptiveApi
@Stable
interface ThreePaneScaffoldNavigator {
    /**
     * The current scaffold state provided by the navigator. It's supposed to be auto-updated
     * whenever a navigation operation is performed.
     */
    val scaffoldState: ThreePaneScaffoldState

    /**
     * Navigates to a new pane destination.
     */
    fun navigateTo(pane: ThreePaneScaffoldRole)

    /**
     * Returns `true` if there is a previous destination to navigate back to.
     *
     * @param scaffoldValueMustChange `true` if the navigation operation should only be performed
     *        when there are actual layout value changes.
     */
    fun canNavigateBack(scaffoldValueMustChange: Boolean = true): Boolean

    /**
     * Navigates to the previous destination.
     *
     * @param popUntilScaffoldValueChange `true` if the backstack should be popped until the layout
     *        value changes.
     */
    fun navigateBack(popUntilScaffoldValueChange: Boolean = true): Boolean
}

/**
 * Returns a remembered default implementation of [ThreePaneScaffoldNavigator] for
 * [ListDetailPaneScaffold], which will be updated automatically when the input values change.
 * The default navigator is supposed to be used independently from any navigation frameworks and
 * it will address the navigation purely inside the [ListDetailPaneScaffold].
 *
 * @param scaffoldDirective the current layout directives to follow. The default value will be
 *        calculated with [calculateStandardPaneScaffoldDirective] using [WindowAdaptiveInfo]
 *        retrieved from the current context.
 * @param adaptStrategies adaptation strategies of each pane.
 * @param initialDestinationHistory the initial pane destination history of the scaffold, by default
 *        it will be just the list pane.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun rememberListDetailPaneScaffoldNavigator(
    scaffoldDirective: PaneScaffoldDirective =
        calculateStandardPaneScaffoldDirective(currentWindowAdaptiveInfo()),
    adaptStrategies: ThreePaneScaffoldAdaptStrategies =
        ListDetailPaneScaffoldDefaults.adaptStrategies(),
    initialDestinationHistory: List<ThreePaneScaffoldRole> = listOf(ListDetailPaneScaffoldRole.List)
): ThreePaneScaffoldNavigator =
    rememberThreePaneScaffoldNavigator(
        scaffoldDirective,
        adaptStrategies,
        initialDestinationHistory
    )

/**
 * Returns a remembered default implementation of [ThreePaneScaffoldNavigator] for
 * [SupportingPaneScaffold], which will be updated automatically when the input values change.
 * The default navigator is supposed to be used independently from any navigation frameworks and
 * it will address the navigation purely inside the [SupportingPaneScaffold].
 *
 * @param scaffoldDirective the current layout directives to follow. The default value will be
 *        calculated with [calculateStandardPaneScaffoldDirective] using [WindowAdaptiveInfo]
 *        retrieved from the current context.
 * @param adaptStrategies adaptation strategies of each pane.
 * @param initialDestinationHistory the initial destination history of the scaffold, by default it
 *        will be just the main pane.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun rememberSupportingPaneScaffoldNavigator(
    scaffoldDirective: PaneScaffoldDirective =
        calculateStandardPaneScaffoldDirective(currentWindowAdaptiveInfo()),
    adaptStrategies: ThreePaneScaffoldAdaptStrategies =
        SupportingPaneScaffoldDefaults.adaptStrategies(),
    initialDestinationHistory: List<ThreePaneScaffoldRole> =
        listOf(SupportingPaneScaffoldRole.Main)
): ThreePaneScaffoldNavigator =
    rememberThreePaneScaffoldNavigator(
        scaffoldDirective,
        adaptStrategies,
        initialDestinationHistory
    )

@ExperimentalMaterial3AdaptiveApi
@Composable
internal fun rememberThreePaneScaffoldNavigator(
    scaffoldDirective: PaneScaffoldDirective,
    adaptStrategies: ThreePaneScaffoldAdaptStrategies,
    initialDestinationHistory: List<ThreePaneScaffoldRole>
): ThreePaneScaffoldNavigator =
    rememberSaveable(
        saver = DefaultThreePaneScaffoldNavigator.saver(scaffoldDirective, adaptStrategies)
    ) {
        DefaultThreePaneScaffoldNavigator(
            initialDestinationHistory = initialDestinationHistory,
            initialScaffoldDirective = scaffoldDirective,
            initialAdaptStrategies = adaptStrategies
        )
    }.apply {
        this.scaffoldDirective = scaffoldDirective
        this.adaptStrategies = adaptStrategies
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal class DefaultThreePaneScaffoldNavigator(
    initialDestinationHistory: List<ThreePaneScaffoldRole>,
    initialScaffoldDirective: PaneScaffoldDirective,
    initialAdaptStrategies: ThreePaneScaffoldAdaptStrategies,
) : ThreePaneScaffoldNavigator, ThreePaneScaffoldState {

    private val destinationHistory = mutableStateListOf<ThreePaneScaffoldRole>().apply {
        addAll(initialDestinationHistory)
    }

    override val scaffoldState = this

    override var scaffoldDirective by mutableStateOf(initialScaffoldDirective)

    var adaptStrategies by mutableStateOf(initialAdaptStrategies)

    val currentDestination: ThreePaneScaffoldRole? get() = destinationHistory.lastOrNull()

    override val scaffoldValue by derivedStateOf {
        calculateScaffoldValue(currentDestination)
    }

    override fun navigateTo(pane: ThreePaneScaffoldRole) {
        destinationHistory.add(pane)
    }

    override fun canNavigateBack(scaffoldValueMustChange: Boolean): Boolean =
        getPreviousDestinationIndex(scaffoldValueMustChange) >= 0

    override fun navigateBack(popUntilScaffoldValueChange: Boolean): Boolean {
        val previousDestinationIndex = getPreviousDestinationIndex(popUntilScaffoldValueChange)
        if (previousDestinationIndex < 0) {
            destinationHistory.clear()
            return false
        }
        val targetSize = previousDestinationIndex + 1
        while (destinationHistory.size > targetSize) {
            destinationHistory.removeLast()
        }
        return true
    }

    private fun getPreviousDestinationIndex(withScaffoldValueChange: Boolean): Int {
        if (destinationHistory.size <= 1) {
            // No previous destination
            return -1
        }
        if (!withScaffoldValueChange) {
            return destinationHistory.lastIndex - 1
        }
        for (previousDestinationIndex in destinationHistory.lastIndex - 1 downTo 0) {
            val newValue = calculateScaffoldValue(destinationHistory[previousDestinationIndex])
            if (newValue != scaffoldValue) {
                return previousDestinationIndex
            }
        }
        return -1
    }

    private fun calculateScaffoldValue(
        destination: ThreePaneScaffoldRole?
    ): ThreePaneScaffoldValue =
        calculateThreePaneScaffoldValue(
            scaffoldDirective.maxHorizontalPartitions,
            adaptStrategies,
            destination
        )

    companion object {
        /**
         * To keep destination history saved
         */
        fun saver(
            initialScaffoldDirective: PaneScaffoldDirective,
            initialAdaptStrategies: ThreePaneScaffoldAdaptStrategies
        ): Saver<DefaultThreePaneScaffoldNavigator, *> = listSaver(
            save = {
                it.destinationHistory
            },
            restore = {
                DefaultThreePaneScaffoldNavigator(
                    initialDestinationHistory = it,
                    initialScaffoldDirective = initialScaffoldDirective,
                    initialAdaptStrategies = initialAdaptStrategies
                )
            }
        )
    }
}
