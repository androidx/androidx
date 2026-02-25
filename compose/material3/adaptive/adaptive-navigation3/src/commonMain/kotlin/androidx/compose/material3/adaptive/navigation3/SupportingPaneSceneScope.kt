/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneScaffoldTransitionScope
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavEntry

/** A scope used by a [SupportingPaneSceneStrategy]. */
@ExperimentalMaterial3AdaptiveApi
public sealed interface SupportingPaneSceneScope {
    /**
     * The transition scope of the supporting pane scaffold, providing information about the
     * scaffold's current state transition and motion.
     */
    public val scaffoldTransitionScope:
        PaneScaffoldTransitionScope<ThreePaneScaffoldRole, ThreePaneScaffoldValue>
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal class SupportingPaneSceneScopeImpl(
    override val scaffoldTransitionScope:
        PaneScaffoldTransitionScope<ThreePaneScaffoldRole, ThreePaneScaffoldValue>
) : SupportingPaneSceneScope

/**
 * Local provider of [SupportingPaneSceneScope] for [NavEntry]s which are displayed in a Material
 * supporting pane scaffold. If null, this means that [SupportingPaneSceneStrategy] is not the
 * chosen strategy to display the current content.
 */
@ExperimentalMaterial3AdaptiveApi
public val LocalSupportingPaneSceneScope: ProvidableCompositionLocal<SupportingPaneSceneScope?> =
    compositionLocalOf {
        null
    }
