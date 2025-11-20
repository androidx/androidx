/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.material3

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveComponentOverrideApi
import androidx.compose.material3.adaptive.layout.AnimatedPaneOverride
import androidx.compose.material3.adaptive.layout.AnimatedPaneOverrideScope
import androidx.compose.material3.adaptive.layout.AnimatedPaneScope
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneScaffoldValue
import androidx.compose.runtime.Composable
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.animation.AnimatedSpatialVisibility
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxSize

@OptIn(
    ExperimentalMaterial3AdaptiveComponentOverrideApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
)
@ExperimentalMaterial3XrApi
internal object XrAnimatedPaneOverride : AnimatedPaneOverride {
    @Composable
    override fun <
        Role : PaneScaffoldRole,
        ScaffoldValue : PaneScaffoldValue<Role>,
    > AnimatedPaneOverrideScope<Role, ScaffoldValue>.AnimatedPane() {
        // TODO(kmost): No way to convert between Enter/ExitTransition and
        //  SpatialEnter/ExitTransition, so for now we cannot respect those scope properties.

        val state = MutableTransitionState(false)
        state.targetState =
            scope.scaffoldStateTransition.targetState[scope.paneRole] != PaneAdaptedValue.Hidden

        AnimatedSpatialVisibility(visibleState = state) {
            SpatialPanel(SubspaceModifier.fillMaxSize()) {
                AnimatedPaneScope.create(this).content()
            }
        }
    }
}
