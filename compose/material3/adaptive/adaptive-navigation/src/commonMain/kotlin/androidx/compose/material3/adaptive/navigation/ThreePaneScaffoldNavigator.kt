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

package androidx.compose.material3.adaptive.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldAdaptStrategies
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculateThreePaneScaffoldValue
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
import androidx.compose.ui.util.fastMap
import kotlin.collections.removeLast as removeLastKt

/**
 * The common interface of the default navigation implementations for different three-pane
 * scaffolds.
 *
 * In general, we suggest you to use [rememberListDetailPaneScaffoldNavigator] or
 * [rememberSupportingPaneScaffoldNavigator] to get remembered default instances of this interface
 * for [ListDetailPaneScaffold] and [SupportingPaneScaffold], respectively. Those default
 * implementations work independently from any navigation frameworks.
 *
 * If you need to integrate with existing navigation frameworks or implement your own custom
 * navigation logic, usually creating whole new APIs that's tailored for your own solution will be
 * recommended, instead of implementing this interface. But we recommend you refer to the API design
 * and the default implementation to get better understanding and address the intricacies of
 * navigation in an adaptive scenario.
 *
 * @param T the type representing the content, or id of the content, for a navigation destination.
 *   This type must be storable in a Bundle. Used to customize navigation behavior (for example,
 *   [BackNavigationBehavior]). If this customization is unneeded, you can pass [Nothing].
 */
@ExperimentalMaterial3AdaptiveApi
@Stable
interface ThreePaneScaffoldNavigator<T> {
    /**
     * The current layout directives that the associated three pane scaffold needs to follow. It's
     * supposed to be automatically updated when the window configuration changes.
     */
    val scaffoldDirective: PaneScaffoldDirective

    /**
     * The current layout value of the associated three pane scaffold value, which represents unique
     * layout states of the scaffold.
     */
    val scaffoldValue: ThreePaneScaffoldValue

    /**
     * Returns the scaffold value associated with the previous destination, assuming there is a
     * previous destination to navigate back to. If not, this is the same as [scaffoldValue].
     *
     * @param backNavigationBehavior the behavior describing which backstack entries may be skipped
     *   during the back navigation. See [BackNavigationBehavior].
     */
    fun peekPreviousScaffoldValue(
        backNavigationBehavior: BackNavigationBehavior =
            BackNavigationBehavior.PopUntilScaffoldValueChange
    ): ThreePaneScaffoldValue

    /**
     * The current destination as tracked by the navigator.
     *
     * Implementors of this interface should ensure this value is updated whenever a navigation
     * operation is performed.
     */
    val currentDestination: ThreePaneScaffoldDestinationItem<T>?

    /**
     * Indicates if the navigator should be aware of pane destination history when deciding the
     * result [ThreePaneScaffoldValue] by a navigation operation. If the value is `false`, only the
     * current destination will be considered in the scaffold value calculation.
     *
     * @see calculateThreePaneScaffoldValue for more detailed explanation about history awareness.
     */
    var isDestinationHistoryAware: Boolean

    /**
     * Navigates to a new destination. The new destination is supposed to have the highest priority
     * when calculating the new [scaffoldValue].
     *
     * Implementors of this interface should ensure the new destination pane will be expanded or
     * adapted in a reasonable way so it provides users the sense that the new destination is the
     * pane currently being used.
     *
     * @param pane the new destination pane.
     * @param content the optional content, or an id representing the content of the new
     *   destination.
     */
    fun navigateTo(pane: ThreePaneScaffoldRole, content: T? = null)

    /**
     * Returns `true` if there is a previous destination to navigate back to.
     *
     * Implementors of this interface should ensure the logic of this function is consistent with
     * [navigateBack].
     *
     * @param backNavigationBehavior the behavior describing which backstack entries may be skipped
     *   during the back navigation. See [BackNavigationBehavior].
     */
    fun canNavigateBack(
        backNavigationBehavior: BackNavigationBehavior =
            BackNavigationBehavior.PopUntilScaffoldValueChange
    ): Boolean

    /**
     * Navigates to the previous destination. Returns `true` if there is a previous destination to
     * navigate back to.
     *
     * Implementors of this interface should ensure the logic of this function is consistent with
     * [canNavigateBack].
     *
     * @param backNavigationBehavior the behavior describing which backstack entries may be skipped
     *   during the back navigation. See [BackNavigationBehavior].
     */
    fun navigateBack(
        backNavigationBehavior: BackNavigationBehavior =
            BackNavigationBehavior.PopUntilScaffoldValueChange
    ): Boolean
}

/**
 * Returns a remembered default implementation of [ThreePaneScaffoldNavigator] for
 * [ListDetailPaneScaffold], which will be updated automatically when the input values change. The
 * default navigator is supposed to be used independently from any navigation frameworks and handles
 * the navigation purely inside the [ListDetailPaneScaffold].
 *
 * @param T the type representing the content, or id of the content, for a navigation destination.
 *   This type must be storable in a Bundle. Used to customize navigation behavior (for example,
 *   [BackNavigationBehavior]). If this customization is unneeded, you can pass [Nothing].
 * @param scaffoldDirective the current layout directives to follow. The default value will be
 *   calculated with [calculatePaneScaffoldDirective] using
 *   [WindowAdaptiveInfo][androidx.compose.material3.adaptive.WindowAdaptiveInfo] retrieved from the
 *   current context.
 * @param adaptStrategies adaptation strategies of each pane.
 * @param isDestinationHistoryAware `true` if the scaffold value calculation should be aware of the
 *   full destination history, instead of just the current destination. See
 *   [calculateThreePaneScaffoldValue] for more relevant details.
 * @param initialDestinationHistory the initial pane destination history of the scaffold, by default
 *   it will be just the list pane.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun <T> rememberListDetailPaneScaffoldNavigator(
    scaffoldDirective: PaneScaffoldDirective =
        calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()),
    adaptStrategies: ThreePaneScaffoldAdaptStrategies =
        ListDetailPaneScaffoldDefaults.adaptStrategies(),
    isDestinationHistoryAware: Boolean = true,
    initialDestinationHistory: List<ThreePaneScaffoldDestinationItem<T>> =
        DefaultListDetailPaneHistory,
): ThreePaneScaffoldNavigator<T> =
    rememberThreePaneScaffoldNavigator(
        scaffoldDirective,
        adaptStrategies,
        isDestinationHistoryAware,
        initialDestinationHistory
    )

/**
 * Returns a remembered default implementation of [ThreePaneScaffoldNavigator] for
 * [ListDetailPaneScaffold], which will be updated automatically when the input values change. The
 * default navigator is supposed to be used independently from any navigation frameworks and handles
 * the navigation purely inside the [ListDetailPaneScaffold].
 *
 * @param scaffoldDirective the current layout directives to follow. The default value will be
 *   calculated with [calculatePaneScaffoldDirective] using
 *   [WindowAdaptiveInfo][androidx.compose.material3.adaptive.WindowAdaptiveInfo] retrieved from the
 *   current context.
 * @param adaptStrategies adaptation strategies of each pane.
 * @param isDestinationHistoryAware `true` if the scaffold value calculation should be aware of the
 *   full destination history, instead of just the current destination. See
 *   [calculateThreePaneScaffoldValue] for more relevant details.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun rememberListDetailPaneScaffoldNavigator(
    scaffoldDirective: PaneScaffoldDirective =
        calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()),
    adaptStrategies: ThreePaneScaffoldAdaptStrategies =
        ListDetailPaneScaffoldDefaults.adaptStrategies(),
    isDestinationHistoryAware: Boolean = true,
): ThreePaneScaffoldNavigator<Nothing> =
    rememberListDetailPaneScaffoldNavigator<Nothing>(
        scaffoldDirective,
        adaptStrategies,
        isDestinationHistoryAware,
    )

/**
 * Returns a remembered default implementation of [ThreePaneScaffoldNavigator] for
 * [SupportingPaneScaffold], which will be updated automatically when the input values change. The
 * default navigator is supposed to be used independently from any navigation frameworks and handles
 * the navigation purely inside the [SupportingPaneScaffold].
 *
 * @param T the type representing the content, or id of the content, for a navigation destination.
 *   This type must be storable in a Bundle. Used to customize navigation behavior (for example,
 *   [BackNavigationBehavior]). If this customization is unneeded, you can pass [Nothing].
 * @param scaffoldDirective the current layout directives to follow. The default value will be
 *   calculated with [calculatePaneScaffoldDirective] using
 *   [WindowAdaptiveInfo][androidx.compose.material3.adaptive.WindowAdaptiveInfo] retrieved from the
 *   current context.
 * @param adaptStrategies adaptation strategies of each pane.
 * @param isDestinationHistoryAware `true` if the scaffold value calculation should be aware of the
 *   full destination history, instead of just the current destination. See
 *   [calculateThreePaneScaffoldValue] for more relevant details.
 * @param initialDestinationHistory the initial destination history of the scaffold, by default it
 *   will be just the main pane.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun <T> rememberSupportingPaneScaffoldNavigator(
    scaffoldDirective: PaneScaffoldDirective =
        calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()),
    adaptStrategies: ThreePaneScaffoldAdaptStrategies =
        SupportingPaneScaffoldDefaults.adaptStrategies(),
    isDestinationHistoryAware: Boolean = true,
    initialDestinationHistory: List<ThreePaneScaffoldDestinationItem<T>> =
        DefaultSupportingPaneHistory,
): ThreePaneScaffoldNavigator<T> =
    rememberThreePaneScaffoldNavigator(
        scaffoldDirective,
        adaptStrategies,
        isDestinationHistoryAware,
        initialDestinationHistory
    )

/**
 * Returns a remembered default implementation of [ThreePaneScaffoldNavigator] for
 * [SupportingPaneScaffold], which will be updated automatically when the input values change. The
 * default navigator is supposed to be used independently from any navigation frameworks and handles
 * the navigation purely inside the [SupportingPaneScaffold].
 *
 * @param scaffoldDirective the current layout directives to follow. The default value will be
 *   calculated with [calculatePaneScaffoldDirective] using
 *   [WindowAdaptiveInfo][androidx.compose.material3.adaptive.WindowAdaptiveInfo] retrieved from the
 *   current context.
 * @param adaptStrategies adaptation strategies of each pane.
 * @param isDestinationHistoryAware `true` if the scaffold value calculation should be aware of the
 *   full destination history, instead of just the current destination. See
 *   [calculateThreePaneScaffoldValue] for more relevant details.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun rememberSupportingPaneScaffoldNavigator(
    scaffoldDirective: PaneScaffoldDirective =
        calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()),
    adaptStrategies: ThreePaneScaffoldAdaptStrategies =
        SupportingPaneScaffoldDefaults.adaptStrategies(),
    isDestinationHistoryAware: Boolean = true,
): ThreePaneScaffoldNavigator<Nothing> =
    rememberSupportingPaneScaffoldNavigator<Nothing>(
        scaffoldDirective,
        adaptStrategies,
        isDestinationHistoryAware,
    )

@ExperimentalMaterial3AdaptiveApi
@Composable
internal fun <T> rememberThreePaneScaffoldNavigator(
    scaffoldDirective: PaneScaffoldDirective,
    adaptStrategies: ThreePaneScaffoldAdaptStrategies,
    isDestinationHistoryAware: Boolean,
    initialDestinationHistory: List<ThreePaneScaffoldDestinationItem<T>>
): ThreePaneScaffoldNavigator<T> =
    rememberSaveable(
            saver =
                DefaultThreePaneScaffoldNavigator.saver(
                    scaffoldDirective,
                    adaptStrategies,
                    isDestinationHistoryAware
                )
        ) {
            DefaultThreePaneScaffoldNavigator(
                initialDestinationHistory = initialDestinationHistory,
                initialScaffoldDirective = scaffoldDirective,
                initialAdaptStrategies = adaptStrategies,
                initialIsDestinationHistoryAware = isDestinationHistoryAware
            )
        }
        .apply {
            this.scaffoldDirective = scaffoldDirective
            this.adaptStrategies = adaptStrategies
            this.isDestinationHistoryAware = isDestinationHistoryAware
        }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal class DefaultThreePaneScaffoldNavigator<T>(
    initialDestinationHistory: List<ThreePaneScaffoldDestinationItem<T>>,
    initialScaffoldDirective: PaneScaffoldDirective,
    initialAdaptStrategies: ThreePaneScaffoldAdaptStrategies,
    initialIsDestinationHistoryAware: Boolean
) : ThreePaneScaffoldNavigator<T> {

    private val destinationHistory =
        mutableStateListOf<ThreePaneScaffoldDestinationItem<T>>().apply {
            addAll(initialDestinationHistory)
        }

    override var scaffoldDirective by mutableStateOf(initialScaffoldDirective)

    override var isDestinationHistoryAware by mutableStateOf(initialIsDestinationHistoryAware)

    var adaptStrategies by mutableStateOf(initialAdaptStrategies)

    override val currentDestination
        get() = destinationHistory.lastOrNull()

    override val scaffoldValue by derivedStateOf {
        calculateScaffoldValue(destinationHistory.lastIndex)
    }

    override fun peekPreviousScaffoldValue(
        backNavigationBehavior: BackNavigationBehavior
    ): ThreePaneScaffoldValue {
        val index = getPreviousDestinationIndex(backNavigationBehavior)
        return if (index == -1) scaffoldValue else calculateScaffoldValue(index)
    }

    override fun navigateTo(pane: ThreePaneScaffoldRole, content: T?) {
        destinationHistory.add(ThreePaneScaffoldDestinationItem(pane, content))
    }

    override fun canNavigateBack(backNavigationBehavior: BackNavigationBehavior): Boolean =
        getPreviousDestinationIndex(backNavigationBehavior) >= 0

    override fun navigateBack(backNavigationBehavior: BackNavigationBehavior): Boolean {
        val previousDestinationIndex = getPreviousDestinationIndex(backNavigationBehavior)
        if (previousDestinationIndex < 0) {
            destinationHistory.clear()
            return false
        }
        val targetSize = previousDestinationIndex + 1
        while (destinationHistory.size > targetSize) {
            destinationHistory.removeLastKt()
        }
        return true
    }

    private fun getPreviousDestinationIndex(backNavBehavior: BackNavigationBehavior): Int {
        if (destinationHistory.size <= 1) {
            // No previous destination
            return -1
        }
        when (backNavBehavior) {
            BackNavigationBehavior.PopLatest -> return destinationHistory.lastIndex - 1
            BackNavigationBehavior.PopUntilScaffoldValueChange ->
                for (previousDestinationIndex in destinationHistory.lastIndex - 1 downTo 0) {
                    val previousValue = calculateScaffoldValue(previousDestinationIndex)
                    if (previousValue != scaffoldValue) {
                        return previousDestinationIndex
                    }
                }
            BackNavigationBehavior.PopUntilCurrentDestinationChange ->
                for (previousDestinationIndex in destinationHistory.lastIndex - 1 downTo 0) {
                    val destination = destinationHistory[previousDestinationIndex].pane
                    if (destination != currentDestination?.pane) {
                        return previousDestinationIndex
                    }
                }
            BackNavigationBehavior.PopUntilContentChange ->
                for (previousDestinationIndex in destinationHistory.lastIndex - 1 downTo 0) {
                    val content = destinationHistory[previousDestinationIndex].content
                    if (content != currentDestination?.content) {
                        return previousDestinationIndex
                    }
                    // A scaffold value change also counts as a content change.
                    val previousValue = calculateScaffoldValue(previousDestinationIndex)
                    if (previousValue != scaffoldValue) {
                        return previousDestinationIndex
                    }
                }
        }

        return -1
    }

    private fun calculateScaffoldValue(destinationIndex: Int) =
        if (destinationIndex == -1) {
            calculateThreePaneScaffoldValue(
                scaffoldDirective.maxHorizontalPartitions,
                adaptStrategies,
                null
            )
        } else if (isDestinationHistoryAware) {
            calculateThreePaneScaffoldValue(
                scaffoldDirective.maxHorizontalPartitions,
                adaptStrategies,
                destinationHistory.subList(0, destinationIndex + 1)
            )
        } else {
            calculateThreePaneScaffoldValue(
                scaffoldDirective.maxHorizontalPartitions,
                adaptStrategies,
                destinationHistory[destinationIndex]
            )
        }

    companion object {
        /** To keep destination history saved */
        fun <T> saver(
            initialScaffoldDirective: PaneScaffoldDirective,
            initialAdaptStrategies: ThreePaneScaffoldAdaptStrategies,
            initialDestinationHistoryAware: Boolean
        ): Saver<DefaultThreePaneScaffoldNavigator<T>, *> {
            val destinationItemSaver = destinationItemSaver<T>()
            return listSaver(
                save = {
                    it.destinationHistory.fastMap { destination ->
                        with(destinationItemSaver) { save(destination) }
                    }
                },
                restore = {
                    DefaultThreePaneScaffoldNavigator(
                        initialDestinationHistory =
                            it.fastMap { savedDestination ->
                                destinationItemSaver.restore(savedDestination!!)!!
                            },
                        initialScaffoldDirective = initialScaffoldDirective,
                        initialAdaptStrategies = initialAdaptStrategies,
                        initialIsDestinationHistoryAware = initialDestinationHistoryAware
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun <T> destinationItemSaver(): Saver<ThreePaneScaffoldDestinationItem<T>, Any> =
    listSaver(
        save = { listOf(it.pane, it.content) },
        restore = {
            @Suppress("UNCHECKED_CAST")
            (ThreePaneScaffoldDestinationItem(
                pane = it[0] as ThreePaneScaffoldRole,
                content = it[1] as T?
            ))
        }
    )

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val DefaultListDetailPaneHistory: List<ThreePaneScaffoldDestinationItem<Nothing>> =
    listOf(ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List))

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val DefaultSupportingPaneHistory: List<ThreePaneScaffoldDestinationItem<Nothing>> =
    listOf(ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Main))
