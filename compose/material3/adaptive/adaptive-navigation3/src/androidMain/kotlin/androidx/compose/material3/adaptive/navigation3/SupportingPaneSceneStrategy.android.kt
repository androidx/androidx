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
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.Scene
import androidx.navigation3.ui.SceneStrategy

/**
 * Creates and remembers a [SupportingPaneSceneStrategy].
 *
 * @param backNavigationBehavior the behavior describing which backstack entries may be skipped
 *   during the back navigation. See [BackNavigationBehavior].
 * @param directive The top-level directives about how the supporting-pane scaffold should arrange
 *   its panes.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
public fun <T : Any> rememberSupportingPaneSceneStrategy(
    backNavigationBehavior: BackNavigationBehavior =
        BackNavigationBehavior.PopUntilScaffoldValueChange,
    directive: PaneScaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()),
): SupportingPaneSceneStrategy<T> {
    return remember(backNavigationBehavior, directive) {
        SupportingPaneSceneStrategy(
            backNavigationBehavior = backNavigationBehavior,
            directive = directive,
        )
    }
}

/**
 * A [SupportingPaneSceneStrategy] supports arranging [NavEntry]s into an adaptive
 * [SupportingPaneScaffold]. By using [mainPane], [supportingPane], or [extraPane] in a NavEntry's
 * metadata, entries can be assigned as belonging to a main pane, supporting pane, or extra pane.
 * These panes will be displayed together if the window size is sufficiently large, and will
 * automatically adapt if the window size changes, for example, on a foldable device.
 *
 * @param backNavigationBehavior the behavior describing which backstack entries may be skipped
 *   during the back navigation. See [BackNavigationBehavior].
 * @param directive The top-level directives about how the scaffold should arrange its panes.
 */
@ExperimentalMaterial3AdaptiveApi
public class SupportingPaneSceneStrategy<T : Any>(
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

        var idx = entries.lastIndex
        while (idx >= 0) {
            val entry = entries[idx]
            val paneMetadata = getPaneMetadata(entry)
            if (paneMetadata == null) {
                break
            }

            if (paneMetadata.sceneKey == sceneKey) {
                scaffoldEntryIndices.add(idx)
                scaffoldEntries.add(0, entry)
                entriesAsNavItems.add(
                    0,
                    ThreePaneScaffoldDestinationItem(
                        pane = paneMetadata.role,
                        contentKey = entry.contentKey,
                    ),
                )
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
                adaptStrategies = SupportingPaneScaffoldDefaults.adaptStrategies(),
                allEntries = entries,
                scaffoldEntries = scaffoldEntries,
                scaffoldEntryIndices = scaffoldEntryIndices,
                entriesAsNavItems = entriesAsNavItems,
                getPaneRole = { getPaneMetadata(it)?.role },
                scaffoldType = ThreePaneScaffoldType.SupportingPane,
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

    internal class MainMetadata(override val sceneKey: Any) : PaneMetadata {
        override val role: ThreePaneScaffoldRole
            get() = SupportingPaneScaffoldRole.Main
    }

    internal class SupportingMetadata(override val sceneKey: Any) : PaneMetadata {
        override val role: ThreePaneScaffoldRole
            get() = SupportingPaneScaffoldRole.Supporting
    }

    internal class ExtraMetadata(override val sceneKey: Any) : PaneMetadata {
        override val role: ThreePaneScaffoldRole
            get() = SupportingPaneScaffoldRole.Extra
    }

    public companion object {
        internal val SupportingPaneRoleKey: String =
            SupportingPaneScaffoldRole::class.qualifiedName!!

        /**
         * Constructs metadata to mark a [NavEntry] as belonging to a
         * [main pane][SupportingPaneScaffoldRole.Main] within a [SupportingPaneScaffold].
         *
         * @param sceneKey the key to distinguish the scene of the supporting-pane scaffold, in case
         *   multiple supporting-pane scaffolds are used within the same NavDisplay.
         */
        public fun mainPane(sceneKey: Any = Unit): Map<String, Any> =
            mapOf(SupportingPaneRoleKey to MainMetadata(sceneKey))

        /**
         * Constructs metadata to mark a [NavEntry] as belonging to a
         * [supporting pane][SupportingPaneScaffoldRole.Supporting] within a
         * [SupportingPaneScaffold].
         *
         * @param sceneKey the key to distinguish the scene of the supporting-pane scaffold, in case
         *   multiple supporting-pane scaffolds are used within the same NavDisplay.
         */
        public fun supportingPane(sceneKey: Any = Unit): Map<String, Any> =
            mapOf(SupportingPaneRoleKey to SupportingMetadata(sceneKey))

        /**
         * Constructs metadata to mark a [NavEntry] as belonging to an
         * [extra pane][SupportingPaneScaffoldRole.Extra] within a [SupportingPaneScaffold].
         *
         * @param sceneKey the key to distinguish the scene of the supporting-pane scaffold, in case
         *   multiple supporting-pane scaffolds are used within the same NavDisplay.
         */
        public fun extraPane(sceneKey: Any = Unit): Map<String, Any> =
            mapOf(SupportingPaneRoleKey to ExtraMetadata(sceneKey))

        private fun <T : Any> getPaneMetadata(entry: NavEntry<T>): PaneMetadata? =
            entry.metadata[SupportingPaneRoleKey] as? PaneMetadata
    }
}
