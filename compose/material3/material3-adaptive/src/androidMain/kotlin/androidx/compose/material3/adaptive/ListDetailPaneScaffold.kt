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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * A Material opinionated implementation of [ThreePaneScaffold] that will display the provided three
 * panes in a canonical list-detail layout.
 *
 * @param layoutState the state of the scaffold, which will decide the current layout directive
 *        and scaffold layout value, and perform navigation within the scaffold.
 * @param listPane the list pane of the scaffold. See [ListDetailPaneScaffoldRole.List].
 * @param modifier [Modifier] of the scaffold layout.
 * @param extraPane the list pane of the scaffold. See [ListDetailPaneScaffoldRole.Extra].
 * @param detailPane the list pane of the scaffold. See [ListDetailPaneScaffoldRole.Detail].
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun ListDetailPaneScaffold(
    layoutState: ListDetailPaneScaffoldState,
    listPane: @Composable ThreePaneScaffoldScope.(PaneAdaptedValue) -> Unit,
    modifier: Modifier = Modifier,
    extraPane: (@Composable ThreePaneScaffoldScope.(PaneAdaptedValue) -> Unit)? = null,
    detailPane: @Composable ThreePaneScaffoldScope.(PaneAdaptedValue) -> Unit
) {
    ThreePaneScaffold(
        modifier = modifier.fillMaxSize(),
        layoutDirective = layoutState.layoutDirective,
        scaffoldValue = layoutState.layoutValue,
        arrangement = ThreePaneScaffoldDefaults.ListDetailLayoutArrangement,
        secondaryPane = listPane,
        tertiaryPane = extraPane,
        primaryPane = detailPane
    )
}

/**
 * Provides default values of [ListDetailPaneScaffold].
 */
@ExperimentalMaterial3AdaptiveApi
object ListDetailPaneScaffoldDefaults {
    /**
     * Creates a default [ThreePaneScaffoldAdaptStrategies] for [ListDetailPaneScaffold].
     *
     * @param detailPaneAdaptStrategy the adapt strategy of the primary pane
     * @param listPaneAdaptStrategy the adapt strategy of the secondary pane
     * @param extraPaneAdaptStrategy the adapt strategy of the tertiary pane
     */
    fun adaptStrategies(
        detailPaneAdaptStrategy: AdaptStrategy = AdaptStrategy.Hide,
        listPaneAdaptStrategy: AdaptStrategy = AdaptStrategy.Hide,
        extraPaneAdaptStrategy: AdaptStrategy = AdaptStrategy.Hide,
    ): ThreePaneScaffoldAdaptStrategies =
        ThreePaneScaffoldAdaptStrategies(
            detailPaneAdaptStrategy,
            listPaneAdaptStrategy,
            extraPaneAdaptStrategy
        )
}

/**
 * The state of [ListDetailPaneScaffold]. It provides the layout directive and value state that will
 * be updated directly. It also provides functions to perform navigation.
 *
 * Use [rememberListDetailPaneScaffoldState] to get a remembered default instance of this interface,
 * which works independently from any navigation frameworks. Developers can also integrate with
 * other navigation frameworks by implementing this interface.
 *
 * @property layoutDirective the current layout directives that the associated
 *           [ListDetailPaneScaffold] needs to follow. It's supposed to be automatically updated
 *           when the window configuration changes.
 * @property layoutValue the current layout value of the associated [ListDetailPaneScaffold], which
 *           represents unique layout states of the scaffold.
 */
@ExperimentalMaterial3AdaptiveApi
@Stable
interface ListDetailPaneScaffoldState {
    val layoutDirective: AdaptiveLayoutDirective
    val layoutValue: ThreePaneScaffoldValue

    /**
     * Navigates to a new focus.
     */
    fun navigateTo(pane: ListDetailPaneScaffoldRole)

    /**
     * Returns `true` if there is a previous focus to navigate back to.
     *
     * @param layoutValueMustChange `true` if the navigation operation should only be performed when
     *        there are actual layout value changes.
     */
    fun canNavigateBack(layoutValueMustChange: Boolean = true): Boolean

    /**
     * Navigates to the previous focus.
     *
     * @param popUntilLayoutValueChange `true` if the backstack should be popped until the layout
     *        value changes.
     */
    fun navigateBack(popUntilLayoutValueChange: Boolean = true): Boolean
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private class DefaultListDetailPaneScaffoldState(
    initialFocusHistory: List<ListDetailPaneScaffoldRole>,
    initialLayoutDirective: AdaptiveLayoutDirective,
    initialAdaptStrategies: ThreePaneScaffoldAdaptStrategies,
) : ListDetailPaneScaffoldState {

    private val focusHistory = mutableStateListOf<ListDetailPaneScaffoldRole>().apply {
        addAll(initialFocusHistory)
    }

    override var layoutDirective by mutableStateOf(initialLayoutDirective)

    var adaptStrategies by mutableStateOf(initialAdaptStrategies)

    val currentFocus: ListDetailPaneScaffoldRole?
        get() = focusHistory.lastOrNull()

    override val layoutValue: ThreePaneScaffoldValue
        get() = calculateScaffoldValue(currentFocus)

    override fun navigateTo(pane: ListDetailPaneScaffoldRole) {
        focusHistory.add(pane)
    }

    override fun canNavigateBack(layoutValueMustChange: Boolean): Boolean =
        getPreviousFocusIndex(layoutValueMustChange) >= 0

    override fun navigateBack(popUntilLayoutValueChange: Boolean): Boolean {
        val previousFocusIndex = getPreviousFocusIndex(popUntilLayoutValueChange)
        if (previousFocusIndex < 0) {
            focusHistory.clear()
            return false
        }
        val targetSize = previousFocusIndex + 1
        while (focusHistory.size > targetSize) {
            focusHistory.removeLast()
        }
        return true
    }

    private fun getPreviousFocusIndex(withLayoutValueChange: Boolean): Int {
        if (focusHistory.size <= 1) {
            // No previous focus
            return -1
        }
        if (!withLayoutValueChange) {
            return focusHistory.lastIndex - 1
        }
        for (previousFocusIndex in focusHistory.lastIndex - 1 downTo 0) {
            val newValue = calculateScaffoldValue(focusHistory[previousFocusIndex])
            if (newValue != layoutValue) {
                return previousFocusIndex
            }
        }
        return -1
    }

    private fun calculateScaffoldValue(
        focus: ListDetailPaneScaffoldRole?
    ): ThreePaneScaffoldValue =
        calculateThreePaneScaffoldValue(
            layoutDirective.maxHorizontalPartitions,
            adaptStrategies,
            focus?.threePaneScaffoldRole
        )

    companion object {
        /**
         * To keep focus history saved
         */
        fun saver(
            initialLayoutDirective: AdaptiveLayoutDirective,
            initialAdaptStrategies: ThreePaneScaffoldAdaptStrategies,
        ): Saver<DefaultListDetailPaneScaffoldState, *> = listSaver(
            save = {
                it.focusHistory.toList()
            },
            restore = {
                DefaultListDetailPaneScaffoldState(
                    initialFocusHistory = it,
                    initialLayoutDirective = initialLayoutDirective,
                    initialAdaptStrategies = initialAdaptStrategies,
                )
            }
        )
    }
}

/**
 * Returns a remembered default implementation of [ListDetailPaneScaffoldState], which will
 * be updated automatically when the input values change. The default state is supposed to be
 * used independently from any navigation frameworks and it will address the navigation purely
 * inside the [ListDetailPaneScaffold].
 *
 * @param layoutDirectives the current layout directives to follow. The default value will be
 *        Calculated with [calculateStandardAdaptiveLayoutDirective] using [WindowAdaptiveInfo]
 *        retrieved from the current context.
 * @param adaptStrategies adaptation strategies of each pane.
 * @param initialFocusHistory the initial focus history of the scaffold, by default it will be just
 *        the list pane.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun rememberListDetailPaneScaffoldState(
    layoutDirectives: AdaptiveLayoutDirective =
        calculateStandardAdaptiveLayoutDirective(calculateWindowAdaptiveInfo()),
    adaptStrategies: ThreePaneScaffoldAdaptStrategies =
        ListDetailPaneScaffoldDefaults.adaptStrategies(),
    initialFocusHistory: List<ListDetailPaneScaffoldRole> = listOf(ListDetailPaneScaffoldRole.List)
): ListDetailPaneScaffoldState =
    rememberSaveable(
        saver = DefaultListDetailPaneScaffoldState.saver(
            layoutDirectives,
            adaptStrategies,
        )
    ) {
        DefaultListDetailPaneScaffoldState(
            initialFocusHistory = initialFocusHistory,
            initialLayoutDirective = layoutDirectives,
            initialAdaptStrategies = adaptStrategies,
        )
    }.apply {
        this.layoutDirective = layoutDirectives
        this.adaptStrategies = adaptStrategies
    }

/**
 * The set of the available pane roles of [ListDetailPaneScaffold].
 */
@ExperimentalMaterial3AdaptiveApi
enum class ListDetailPaneScaffoldRole(internal val threePaneScaffoldRole: ThreePaneScaffoldRole) {
    /**
     * The list pane of [ListDetailPaneScaffold]. It is mapped to [ThreePaneScaffoldRole.Secondary].
     */
    List(ThreePaneScaffoldRole.Secondary),
    /**
     * The detail pane of [ListDetailPaneScaffold]. It is mapped to [ThreePaneScaffoldRole.Primary].
     */
    Detail(ThreePaneScaffoldRole.Primary),
    /**
     * The extra pane of [ListDetailPaneScaffold]. It is mapped to [ThreePaneScaffoldRole.Tertiary].
     */
    Extra(ThreePaneScaffoldRole.Tertiary)
}
