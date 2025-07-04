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

package androidx.xr.compose.material3

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveComponentOverrideApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteColors
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldOverride
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldOverrideScope
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldState
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldValue
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScope
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.width

/**
 * XR-specific Navigation Suite Scaffold that wraps its content in a [SpatialPanel].
 *
 * The Navigation Suite Scaffold wraps the provided content and places the adequate provided
 * navigation component on the screen according to the current [NavigationSuiteType].
 *
 * The navigation component can be animated to be hidden or shown via a
 * [NavigationSuiteScaffoldState].
 *
 * @param navigationSuiteItems the navigation items to be displayed
 * @param modifier the [SubspaceModifier] to be applied to the navigation suite scaffold
 * @param layoutType the current [NavigationSuiteType]
 * @param navigationSuiteColors [NavigationSuiteColors] that will be used to determine the container
 *   (background) color of the navigation component and the preferred color for content inside the
 *   navigation component
 * @param state the [NavigationSuiteScaffoldState] of this navigation suite scaffold
 * @param content the content of your screen
 */
@ExperimentalMaterial3XrApi
@Composable
public fun NavigationSuiteScaffold(
    navigationSuiteItems: NavigationSuiteScope.() -> Unit,
    modifier: SubspaceModifier,
    layoutType: NavigationSuiteType,
    navigationSuiteColors: NavigationSuiteColors = NavigationSuiteDefaults.colors(),
    state: NavigationSuiteScaffoldState = rememberNavigationSuiteScaffoldState(),
    content: @Composable () -> Unit = {},
) {
    Subspace {
        // TODO(b/394913962): Find a way to dynamically size this SpatialPanel
        SpatialPanel(
            modifier =
                modifier
                    .height(XrNavigationSuiteScaffoldTokens.ScaffoldHeight)
                    .width(XrNavigationSuiteScaffoldTokens.ScaffoldWidth)
        ) {
            // TODO(b/395684702): Support show/hide animation
            if (state.currentValue == NavigationSuiteScaffoldValue.Visible) {
                NavigationSuite(
                    layoutType = layoutType,
                    colors = navigationSuiteColors,
                    content = navigationSuiteItems,
                )
            }
            content()
        }
    }
}

/**
 * [NavigationSuiteScaffoldOverride] that uses the XR-specific [NavigationSuiteScaffold].
 *
 * Note that when using this override, the containerColor, contentColor, and any modifiers passed in
 * to the 2D composable are ignored.
 *
 * To add containerColor and/or contentColor, wrap your content in a Surface. Example:
 * ```
 * Surface(color = containerColor, contentColor = contentColor) {
 *     content()
 * }
 * ```
 */
@ExperimentalMaterial3XrApi
@OptIn(ExperimentalMaterial3AdaptiveComponentOverrideApi::class)
internal object XrNavigationSuiteScaffoldOverride : NavigationSuiteScaffoldOverride {
    @Composable
    override fun NavigationSuiteScaffoldOverrideScope.NavigationSuiteScaffold() {
        NavigationSuiteScaffold(
            navigationSuiteItems = navigationSuiteItems,
            modifier = SubspaceModifier,
            layoutType = layoutType,
            navigationSuiteColors = navigationSuiteColors,
            state = state,
            content = content,
        )
    }
}

private object XrNavigationSuiteScaffoldTokens {
    val ScaffoldHeight = 1024.dp
    val ScaffoldWidth = 1280.dp
}
