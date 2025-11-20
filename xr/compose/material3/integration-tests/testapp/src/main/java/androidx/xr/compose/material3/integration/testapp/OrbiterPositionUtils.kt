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

package androidx.xr.compose.material3.integration.testapp

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.xr.compose.material3.integration.testapp.OrbiterTokens.NavigationBarOrbiterDefaultHeight
import androidx.xr.compose.material3.integration.testapp.OrbiterTokens.NavigationRailOrbiterDefaultWidth
import androidx.xr.compose.material3.integration.testapp.OrbiterTokens.NavigationSuiteOrbiterDefaultSpacing
import androidx.xr.compose.spatial.OrbiterOffsetType

internal fun OrbiterPosition.getOffsetType(): OrbiterOffsetType =
    when (this) {
        OrbiterPosition.Outside -> OrbiterOffsetType.InnerEdge
        OrbiterPosition.Overlapping -> OrbiterOffsetType.OuterEdge
        OrbiterPosition.Inside -> OrbiterOffsetType.Overlap
    }

internal fun NavigationSuiteType?.calculateOffsetForPosition(position: OrbiterPosition): Dp {
    val containerSize =
        when (this) {
            NavigationSuiteType.NavigationRail -> NavigationRailOrbiterDefaultWidth
            NavigationSuiteType.NavigationBar,
            NavigationSuiteType.ShortNavigationBarCompact,
            NavigationSuiteType.ShortNavigationBarMedium -> NavigationBarOrbiterDefaultHeight
            else -> NavigationRailOrbiterDefaultWidth
        }
    return when (position) {
        OrbiterPosition.Outside -> NavigationSuiteOrbiterDefaultSpacing
        OrbiterPosition.Overlapping -> {
            containerSize / 2
        }
        OrbiterPosition.Inside -> {
            containerSize + NavigationSuiteOrbiterDefaultSpacing
        }
    }
}

private object OrbiterTokens {
    val NavigationBarOrbiterDefaultHeight = 80.dp
    val NavigationRailOrbiterDefaultWidth = 96.dp

    val NavigationSuiteOrbiterDefaultSpacing = 24.dp
}
