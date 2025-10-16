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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveComponentOverrideApi
import androidx.compose.material3.adaptive.layout.AnimatedPaneOverride
import androidx.compose.material3.adaptive.layout.AnimatedPaneOverrideScope
import androidx.compose.material3.adaptive.layout.AnimatedPaneScope
import androidx.compose.material3.adaptive.layout.ExtendedPaneScaffoldPaneScope
import androidx.compose.material3.adaptive.layout.PaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneScaffoldValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(
    ExperimentalMaterial3AdaptiveComponentOverrideApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
)
@Composable
private fun <S : PaneScaffoldRole, T : PaneScaffoldValue<S>> Pane(
    scope: ExtendedPaneScaffoldPaneScope<S, T>,
    modifier: Modifier,
    content: @Composable (AnimatedPaneScope.() -> Unit),
) {
    with(scope) {
        scaffoldStateTransition.AnimatedVisibility(visible = { true }, modifier = modifier) {
            AnimatedPaneScope.create(this).content()
        }
    }
}

@OptIn(
    ExperimentalMaterial3AdaptiveComponentOverrideApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
)
@ExperimentalMaterial3XrApi
internal object XrAnimatedPaneOverride : AnimatedPaneOverride {
    @Composable
    override fun <S : PaneScaffoldRole, T : PaneScaffoldValue<S>> AnimatedPaneOverrideScope<S, T>
        .AnimatedPane() {
        Pane(scope, modifier, content)
    }
}
