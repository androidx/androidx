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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3ComponentOverrideApi
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarOverride
import androidx.compose.material3.ShortNavigationBarOverrideScope
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * XR-specific <a href="https://m3.material.io/components/navigation-bar/overview" class="external"
 * target="_blank">Material Design short navigation bar</a>.
 *
 * Short navigation bars offer a persistent and convenient way to switch between primary
 * destinations in an app.
 *
 * ![Short navigation bar with vertical items
 * image](https://developer.android.com/images/reference/androidx/compose/material3/short-navigation-bar-vertical-items.png)
 *
 * ![Short navigation bar with horizontal items
 * image](https://developer.android.com/images/reference/androidx/compose/material3/short-navigation-bar-horizontal-items.png)
 *
 * The recommended configuration of the [ShortNavigationBar] in an XR environment is three to six
 * [ShortNavigationBarItem]s, each representing a singular destination.
 *
 * @sample androidx.compose.material3.samples.ShortNavigationBarWithHorizontalItemsSample
 *
 * See [ShortNavigationBarItem] for configurations specific to each item, and not the overall
 * [ShortNavigationBar] component.
 *
 * @param modifier the [Modifier] to be applied to this navigation bar
 * @param containerColor the color used for the background of this navigation bar. Use
 *   [Color.Transparent] to have no color
 * @param contentColor the color for content inside this navigation bar.
 * @param content the content of this navigation bar, typically [ShortNavigationBarItem]s
 */
@ExperimentalMaterial3ExpressiveApi
@ExperimentalMaterial3XrApi
@Composable
public fun ShortNavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = ShortNavigationBarDefaults.containerColor,
    contentColor: Color = ShortNavigationBarDefaults.contentColor,
    content: @Composable () -> Unit,
) {
    HorizontalOrbiter(LocalShortNavigationBarOrbiterProperties.current) {
        Surface(color = containerColor, contentColor = contentColor) {
            Row(
                modifier =
                    Modifier.defaultMinSize(minHeight = XrNavigationBarTokens.ContainerHeight)
                        .selectableGroup(),
                horizontalArrangement =
                    Arrangement.spacedBy(XrShortNavigationBarDefaults.paddingAroundItems),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(XrShortNavigationBarDefaults.paddingAroundItems))
                content()
                Spacer(Modifier.width(XrShortNavigationBarDefaults.paddingAroundItems))
            }
        }
    }
}

@ExperimentalMaterial3XrApi
internal object XrShortNavigationBarDefaults {
    internal val paddingAroundItems: Dp = 32.dp
}

/** [ShortNavigationBarOverride] that uses the XR-specific [ShortNavigationBar]. */
@ExperimentalMaterial3XrApi
@OptIn(ExperimentalMaterial3ComponentOverrideApi::class, ExperimentalMaterial3ExpressiveApi::class)
internal object XrShortNavigationBarOverride : ShortNavigationBarOverride {
    @Composable
    override fun ShortNavigationBarOverrideScope.ShortNavigationBar() {
        ShortNavigationBar(
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor,
            content = content,
        )
    }
}

/** The [HorizontalOrbiterProperties] used by [ShortNavigationBar]. */
@ExperimentalMaterial3XrApi
public val LocalShortNavigationBarOrbiterProperties:
    ProvidableCompositionLocal<HorizontalOrbiterProperties> =
    compositionLocalOf {
        DefaultNavigationBarOrbiterProperties
    }
