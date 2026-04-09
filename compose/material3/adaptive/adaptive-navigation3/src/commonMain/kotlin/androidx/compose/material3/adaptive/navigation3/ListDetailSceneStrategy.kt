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

package androidx.compose.material3.adaptive.navigation3

import androidx.collection.mutableIntListOf
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneExpansionState
import androidx.compose.material3.adaptive.layout.PaneMotionDefaults
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldAdaptStrategies
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldScope
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import kotlin.jvm.JvmName

/**
 * Creates and remembers a [ListDetailSceneStrategy].
 *
 * @param shouldHandleSinglePaneLayout whether [ListDetailSceneStrategy] should apply when only a
 *   single pane is displayed. By default, this is false and instead yields to the next
 *   [SceneStrategy] in the chain. If true, single pane layouts will instead be handled internally
 *   by the Material adaptive scaffold instead of the Navigation 3 system.
 * @param backNavigationBehavior the behavior describing which backstack entries may be skipped
 *   during the back navigation. See [BackNavigationBehavior].
 * @param directive The top-level directives about how the list-detail scaffold should arrange its
 *   panes.
 * @param adaptStrategies adaptation strategies of each pane, which denotes how each pane should be
 *   adapted if they can't fit on screen in the [PaneAdaptedValue.Expanded] state. It is recommended
 *   to use [ListDetailPaneScaffoldDefaults.adaptStrategies] as a default, but custom
 *   [ThreePaneScaffoldAdaptStrategies] are supported as well.
 * @param paneExpansionDragHandle when two panes are displayed side-by-side, a non-null drag handle
 *   allows users to resize the panes and change the pane expansion state.
 * @param paneExpansionState the state object of pane expansion. If this is null but a
 *   [paneExpansionDragHandle] is provided, a default implementation will be created.
 * @sample androidx.compose.material3.adaptive.samples.ListDetailWithNavigation3Sample
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
public fun <T : Any> rememberListDetailSceneStrategy(
    shouldHandleSinglePaneLayout: Boolean = false,
    backNavigationBehavior: BackNavigationBehavior =
        BackNavigationBehavior.PopUntilScaffoldValueChange,
    directive: PaneScaffoldDirective =
        calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2()),
    adaptStrategies: ThreePaneScaffoldAdaptStrategies =
        ListDetailPaneScaffoldDefaults.adaptStrategies(),
    paneExpansionDragHandle: (@Composable ThreePaneScaffoldScope.(PaneExpansionState) -> Unit)? =
        null,
    paneExpansionState: PaneExpansionState? = null,
): ListDetailSceneStrategy<T> {
    return remember(
        shouldHandleSinglePaneLayout,
        backNavigationBehavior,
        directive,
        adaptStrategies,
        paneExpansionDragHandle,
        paneExpansionState,
    ) {
        ListDetailSceneStrategy(
            shouldHandleSinglePaneLayout = shouldHandleSinglePaneLayout,
            backNavigationBehavior = backNavigationBehavior,
            directive = directive,
            adaptStrategies = adaptStrategies,
            paneExpansionDragHandle = paneExpansionDragHandle,
            paneExpansionState = paneExpansionState,
        )
    }
}

/**
 * A [ListDetailSceneStrategy] supports arranging [NavEntry]s into an adaptive
 * [androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold]. By using [listPane],
 * [detailPane], or [extraPane] in a NavEntry's metadata, entries can be assigned as belonging to a
 * list pane, detail pane, or extra pane. These panes will be displayed together if the window size
 * is sufficiently large, and will automatically adapt if the window size changes, for example, on a
 * foldable device.
 *
 * @param shouldHandleSinglePaneLayout whether [ListDetailSceneStrategy] should apply when only a
 *   single pane is displayed. By default, this is false and instead yields to the next
 *   [SceneStrategy] in the chain. If true, single pane layouts will instead be handled internally
 *   by the Material adaptive scaffold instead of the Navigation 3 system.
 * @param backNavigationBehavior the behavior describing which backstack entries may be skipped
 *   during the back navigation. See [BackNavigationBehavior].
 * @param directive The top-level directives about how the list-detail scaffold should arrange its
 *   panes.
 * @param adaptStrategies adaptation strategies of each pane, which denotes how each pane should be
 *   adapted if they can't fit on screen in the [PaneAdaptedValue.Expanded] state. It is recommended
 *   to use [ListDetailPaneScaffoldDefaults.adaptStrategies] as a default, but custom
 *   [ThreePaneScaffoldAdaptStrategies] are supported as well.
 * @param paneExpansionDragHandle when two panes are displayed side-by-side, a non-null drag handle
 *   allows users to resize the panes and change the pane expansion state.
 * @param paneExpansionState the state object of pane expansion. If this is null but a
 *   [paneExpansionDragHandle] is provided, a default implementation will be created.
 * @sample androidx.compose.material3.adaptive.samples.ListDetailWithNavigation3Sample
 */
@ExperimentalMaterial3AdaptiveApi
public class ListDetailSceneStrategy<T : Any>(
    @get:JvmName("shouldHandleSinglePaneLayout") public val shouldHandleSinglePaneLayout: Boolean,
    public val backNavigationBehavior: BackNavigationBehavior,
    public val directive: PaneScaffoldDirective,
    public val adaptStrategies: ThreePaneScaffoldAdaptStrategies,
    public val paneExpansionDragHandle:
        (@Composable
        ThreePaneScaffoldScope.(PaneExpansionState) -> Unit)?,
    public val paneExpansionState: PaneExpansionState?,
) : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val lastPaneMetadata = getPaneMetadata(entries.last()) ?: return null
        val sceneKey = lastPaneMetadata.sceneKey

        val scaffoldEntries = mutableListOf<NavEntry<T>>()
        val scaffoldEntryIndices = mutableIntListOf()
        val entriesAsNavItems = mutableListOf<ThreePaneScaffoldDestinationItem<Any>>()

        var detailPlaceholder: (@Composable ThreePaneScaffoldScope.() -> Unit)? = null

        var idx = entries.lastIndex
        while (idx >= 0) {
            val entry = entries[idx]
            val paneMetadata = getPaneMetadata(entry) ?: break

            if (paneMetadata.sceneKey == sceneKey) {
                scaffoldEntryIndices.add(0, idx)
                scaffoldEntries.add(0, entry)
                entriesAsNavItems.add(
                    0,
                    ThreePaneScaffoldDestinationItem(
                        pane = paneMetadata.role,
                        contentKey = entry.contentKey,
                    ),
                )
                if (paneMetadata is ListMetadata) {
                    detailPlaceholder = paneMetadata.detailPlaceholder
                }
            }
            idx--
        }

        if (scaffoldEntries.isEmpty()) return null

        val scene =
            ThreePaneScaffoldScene(
                key = sceneKey,
                onBack = onBack,
                backNavBehavior = backNavigationBehavior,
                directive = directive,
                adaptStrategies = adaptStrategies,
                allEntries = entries,
                scaffoldEntries = scaffoldEntries,
                scaffoldEntryIndices = scaffoldEntryIndices,
                entriesAsNavItems = entriesAsNavItems,
                getPaneRole = { getPaneMetadata(it)?.role },
                scaffoldType = ThreePaneScaffoldType.ListDetail(detailPlaceholder ?: {}),
                paneExpansionDragHandle = paneExpansionDragHandle,
                paneExpansionState = paneExpansionState,
            )

        return when {
            scene.currentScaffoldValue.paneCount == 1 && shouldHandleSinglePaneLayout -> scene
            scene.currentScaffoldValue.paneCount <= 1 -> null
            else -> scene
        }
    }

    internal sealed interface PaneMetadata {
        val sceneKey: Any
        val role: ThreePaneScaffoldRole
    }

    internal class ListMetadata(
        override val sceneKey: Any,
        val detailPlaceholder: @Composable ThreePaneScaffoldScope.() -> Unit,
    ) : PaneMetadata {
        override val role: ThreePaneScaffoldRole
            get() = ListDetailPaneScaffoldRole.List
    }

    internal class DetailMetadata(override val sceneKey: Any) : PaneMetadata {
        override val role: ThreePaneScaffoldRole
            get() = ListDetailPaneScaffoldRole.Detail
    }

    internal class ExtraMetadata(override val sceneKey: Any) : PaneMetadata {
        override val role: ThreePaneScaffoldRole
            get() = ListDetailPaneScaffoldRole.Extra
    }

    public companion object {
        internal const val ListDetailRoleKey: String =
            "androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole"

        /**
         * Constructs metadata to mark a [NavEntry] as belonging to a
         * [list pane][ListDetailPaneScaffoldRole.List] within a
         * [androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold].
         *
         * @param sceneKey the key to distinguish the scene of the list-detail scaffold, in case
         *   multiple list-detail scaffolds are supported within the same NavDisplay.
         * @param detailPlaceholder composable content to display in the detail pane in case there
         *   is no other [NavEntry] representing a detail pane in the backstack. Note that this
         *   content does not receive the same scoping mechanisms as a full-fledged [NavEntry].
         */
        public fun listPane(
            sceneKey: Any = Unit,
            detailPlaceholder: @Composable ThreePaneScaffoldScope.() -> Unit = {},
        ): Map<String, Any> = mapOf(ListDetailRoleKey to ListMetadata(sceneKey, detailPlaceholder))

        /**
         * Constructs metadata to mark a [NavEntry] as belonging to a
         * [detail pane][ListDetailPaneScaffoldRole.Detail] within a
         * [androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold].
         *
         * @param sceneKey the key to distinguish the scene of the list-detail scaffold, in case
         *   multiple list-detail scaffolds are supported within the same NavDisplay.
         */
        public fun detailPane(sceneKey: Any = Unit): Map<String, Any> =
            mapOf(ListDetailRoleKey to DetailMetadata(sceneKey))

        /**
         * Constructs metadata to mark a [NavEntry] as belonging to an
         * [extra pane][ListDetailPaneScaffoldRole.Extra] within a
         * [androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold].
         *
         * @param sceneKey the key to distinguish the scene of the list-detail scaffold, in case
         *   multiple list-detail scaffolds are supported within the same NavDisplay.
         */
        public fun extraPane(sceneKey: Any = Unit): Map<String, Any> =
            mapOf(ListDetailRoleKey to ExtraMetadata(sceneKey))

        /**
         * Constructs metadata to set the preferred size of a pane within a list-detail scaffold,
         * defined in [Dp]s.
         *
         * If the value is unset or set to [Dp.Unspecified],
         * [defaultPanePreferredWidth][PaneScaffoldDirective.defaultPanePreferredWidth] and
         * [defaultPanePreferredHeight][PaneScaffoldDirective.defaultPanePreferredHeight] from
         * [directive] will be used instead.
         *
         * @param width the preferred width of the pane, defined in Dp. The implementation will try
         *   to respect this value when the pane is rendered as a fixed pane. Note that the
         *   preferred width may be ignored when this pane has higher priority than the other panes
         *   so it is forced to fill the available width, or if the pane needs to shrink or expand
         *   to avoid intersecting with the hinge areas.
         * @param height the preferred height of the pane, defined in Dp. The implementation will
         *   try to respect this value when the pane is rendered in a [PaneAdaptedValue.Reflowed] or
         *   [PaneAdaptedValue.Levitated] state. Note that the preferred height may be ignored when
         *   the pane is expanded to stretch the available height, or if the pane needs to shrink or
         *   expand to avoid intersecting with the hinge areas.
         */
        public fun preferredPaneSize(
            width: Dp = Dp.Unspecified,
            height: Dp = Dp.Unspecified,
        ): Map<String, Any> = buildMap {
            if (width != Dp.Unspecified) {
                put(MetadataPreferredWidthKey, width)
            }
            if (height != Dp.Unspecified) {
                put(MetadataPreferredHeightKey, height)
            }
        }

        /**
         * Constructs metadata to set the preferred size of a pane within a list-detail scaffold,
         * defined as a fraction of the total scaffold size.
         *
         * If the value is unset or set to [Dp.Unspecified],
         * [defaultPanePreferredWidth][PaneScaffoldDirective.defaultPanePreferredWidth] and
         * [defaultPanePreferredHeight][PaneScaffoldDirective.defaultPanePreferredHeight] from
         * [directive] will be used instead.
         *
         * @param width the preferred width of the pane, defined as a fraction from 0.0 to 1.0 of
         *   the total scaffold width. The implementation will try to respect this value when the
         *   pane is rendered as a fixed pane. Note that the preferred width may be ignored when
         *   this pane has higher priority than the other panes so it is forced to fill the
         *   available width, or if the pane needs to shrink or expand to avoid intersecting with
         *   the hinge areas.
         * @param height the preferred height of the pane, defined as a fraction from 0.0 to 1.0 of
         *   the total scaffold height. The implementation will try to respect this value when the
         *   pane is rendered in a [PaneAdaptedValue.Reflowed] or [PaneAdaptedValue.Levitated]
         *   state. Note that the preferred height may be ignored when the pane is expanded to
         *   stretch the available height, or if the pane needs to shrink or expand to avoid
         *   intersecting with the hinge areas.
         */
        public fun preferredPaneSize(
            width: Float = Float.NaN,
            height: Float = Float.NaN,
        ): Map<String, Any> = buildMap {
            if (!width.isNaN()) {
                put(MetadataPreferredWidthKey, width)
            }
            if (!height.isNaN()) {
                put(MetadataPreferredHeightKey, height)
            }
        }

        /**
         * Constructs metadata to customize the animation of panes within a list-detail scaffold.
         *
         * If the value is null or unset, the default motions defined in [PaneMotionDefaults] will
         * be used instead.
         *
         * @param enterTransition The [EnterTransition] used to animate the pane in.
         * @param exitTransition The [ExitTransition] used to animate the pane out.
         * @param boundsAnimationSpec The [FiniteAnimationSpec] used to animate the bounds of the
         *   pane when it remains showing but changes its size and/or position.
         */
        public fun paneAnimation(
            enterTransition: EnterTransition? = null,
            exitTransition: ExitTransition? = null,
            boundsAnimationSpec: FiniteAnimationSpec<IntRect>? = null,
        ): Map<String, Any> = buildMap {
            if (enterTransition != null) {
                put(MetadataEnterTransitionKey, enterTransition)
            }
            if (exitTransition != null) {
                put(MetadataExitTransitionKey, exitTransition)
            }
            if (boundsAnimationSpec != null) {
                put(MetadataBoundsAnimationSpecKey, boundsAnimationSpec)
            }
        }

        private fun <T : Any> getPaneMetadata(entry: NavEntry<T>): PaneMetadata? =
            entry.metadata[ListDetailRoleKey] as? PaneMetadata
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal val ThreePaneScaffoldValue.paneCount: Int
    get() {
        var count = 0
        if (this.primary != PaneAdaptedValue.Hidden) count++
        if (this.secondary != PaneAdaptedValue.Hidden) count++
        if (this.tertiary != PaneAdaptedValue.Hidden) count++
        return count
    }
