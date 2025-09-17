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
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldScope
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.Scene
import androidx.navigation3.ui.SceneStrategy

/**
 * Creates and remembers a [ListDetailSceneStrategy].
 *
 * @param backNavigationBehavior the behavior describing which backstack entries may be skipped
 *   during the back navigation. See [BackNavigationBehavior].
 * @param directive The top-level directives about how the list-detail scaffold should arrange its
 *   panes.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
public fun <T : Any> rememberListDetailSceneStrategy(
    backNavigationBehavior: BackNavigationBehavior =
        BackNavigationBehavior.PopUntilScaffoldValueChange,
    directive: PaneScaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()),
): ListDetailSceneStrategy<T> {
    return remember(backNavigationBehavior, directive) {
        ListDetailSceneStrategy(
            backNavigationBehavior = backNavigationBehavior,
            directive = directive,
        )
    }
}

/**
 * A [ListDetailSceneStrategy] supports arranging [NavEntry]s into an adaptive
 * [ListDetailPaneScaffold]. By using [listPane], [detailPane], or [extraPane] in a NavEntry's
 * metadata, entries can be assigned as belonging to a list pane, detail pane, or extra pane. These
 * panes will be displayed together if the window size is sufficiently large, and will automatically
 * adapt if the window size changes, for example, on a foldable device.
 *
 * @param backNavigationBehavior the behavior describing which backstack entries may be skipped
 *   during the back navigation. See [BackNavigationBehavior].
 * @param directive The top-level directives about how the list-detail scaffold should arrange its
 *   panes.
 */
@ExperimentalMaterial3AdaptiveApi
public class ListDetailSceneStrategy<T : Any>(
    public val backNavigationBehavior: BackNavigationBehavior,
    public val directive: PaneScaffoldDirective,
) : SceneStrategy<T> {
    @Composable
    override fun calculateScene(
        entries: List<NavEntry<T>>,
        onBack: (count: Int) -> Unit,
    ): Scene<T>? {
        val lastPaneMetadata = getPaneMetadata(entries.last()) ?: return null
        val sceneKey = lastPaneMetadata.sceneKey

        val scaffoldEntries = mutableListOf<NavEntry<T>>()
        val scaffoldEntryIndices = mutableIntListOf()
        val entriesAsNavItems = mutableListOf<ThreePaneScaffoldDestinationItem<Any>>()

        var detailPlaceholder: (@Composable ThreePaneScaffoldScope.() -> Unit)? = null

        var idx = entries.lastIndex
        while (idx >= 0) {
            val entry = entries[idx]
            val paneMetadata = getPaneMetadata(entry)
            if (paneMetadata == null) {
                break
            }

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
                adaptStrategies = ListDetailPaneScaffoldDefaults.adaptStrategies(),
                allEntries = entries,
                scaffoldEntries = scaffoldEntries,
                scaffoldEntryIndices = scaffoldEntryIndices,
                entriesAsNavItems = entriesAsNavItems,
                getPaneRole = { getPaneMetadata(it)?.role },
                scaffoldType = ThreePaneScaffoldType.ListDetail(detailPlaceholder ?: {}),
            )

        // TODO(b/417475283): decide if/how we should handle scenes with only a single pane
        if (scene.currentScaffoldValue.paneCount <= 1) {
            return null
        }

        return scene
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
        internal val ListDetailRoleKey: String = ListDetailPaneScaffoldRole::class.qualifiedName!!

        /**
         * Constructs metadata to mark a [NavEntry] as belonging to a
         * [list pane][ListDetailPaneScaffoldRole.List] within a [ListDetailPaneScaffold].
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
         * [detail pane][ListDetailPaneScaffoldRole.Detail] within a [ListDetailPaneScaffold].
         *
         * @param sceneKey the key to distinguish the scene of the list-detail scaffold, in case
         *   multiple list-detail scaffolds are supported within the same NavDisplay.
         */
        public fun detailPane(sceneKey: Any = Unit): Map<String, Any> =
            mapOf(ListDetailRoleKey to DetailMetadata(sceneKey))

        /**
         * Constructs metadata to mark a [NavEntry] as belonging to an
         * [extra pane][ListDetailPaneScaffoldRole.Extra] within a [ListDetailPaneScaffold].
         *
         * @param sceneKey the key to distinguish the scene of the list-detail scaffold, in case
         *   multiple list-detail scaffolds are supported within the same NavDisplay.
         */
        public fun extraPane(sceneKey: Any = Unit): Map<String, Any> =
            mapOf(ListDetailRoleKey to ExtraMetadata(sceneKey))

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
