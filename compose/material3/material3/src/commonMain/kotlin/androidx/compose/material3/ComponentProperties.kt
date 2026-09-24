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

package androidx.compose.material3

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.Dp

internal class ComponentProperties(
    val checkboxProperties: CheckboxProperties = CheckboxProperties.Default,
    val radioButtonProperties: RadioButtonProperties = RadioButtonProperties.Default,
    val searchBarProperties: SearchBarProperties = SearchBarProperties.Default,
    val appBarWithSearchBarProperties: AppBarWithSearchProperties =
        AppBarWithSearchProperties.Default,
    val navigationBarProperties: NavigationBarProperties = NavigationBarProperties.Default,
    val navigationBarItemProperties: NavigationBarItemProperties =
        NavigationBarItemProperties.Default,
    val modalNavigationRailProperties: ModalNavigationRailProperties =
        ModalNavigationRailProperties.Default,
    val navigationRailProperties: NavigationRailProperties = NavigationRailProperties.Default,
    val navigationRailItemProperties: NavigationRailItemProperties =
        NavigationRailItemProperties.Default,
    // TODO(b/543061101): Add properties for components.
) {
    companion object {
        val Default = ComponentProperties()
    }
}

internal class CheckboxProperties(val style: CheckboxStyle = CheckboxStyle.Default) {
    companion object {
        val Default = CheckboxProperties()
    }
}

internal class RadioButtonProperties(val style: RadioButtonStyle = RadioButtonStyle.Default) {
    companion object {
        val Default = RadioButtonProperties()
    }
}

internal class SearchBarProperties(val style: SearchBarStyle = SearchBarStyle.Default) {
    companion object {
        val Default = SearchBarProperties()
    }
}

internal class AppBarWithSearchProperties(
    val style: AppBarWithSearchStyle = AppBarWithSearchStyle.Default
) {
    companion object {
        val Default = AppBarWithSearchProperties()
    }
}

internal class NavigationBarProperties(
    val style: NavigationBarStyle = NavigationBarStyle.Default,
    var windowInsets: WindowInsets = WindowInsets.Unspecified,
    val arrangement: ShortNavigationBarArrangement = ShortNavigationBarArrangement.EqualWeight,
) {
    companion object {
        val Default = NavigationBarProperties()
    }
}

internal class NavigationBarItemProperties(
    val style: NavigationBarItemStyle = NavigationBarItemStyle.Default
) {
    companion object {
        val Default = NavigationBarItemProperties()
    }
}

internal class ModalNavigationRailProperties(
    val style: NavigationRailStyle = NavigationRailStyle.Modal,
    var windowInsets: WindowInsets = WindowInsets.Unspecified,
    val arrangement: Arrangement.Vertical = Arrangement.Top,
    val expandedProperties: ModalWideNavigationRailProperties =
        createDefaultModalWideNavigationRailProperties(),
) {
    companion object {
        val Default = ModalNavigationRailProperties()
    }
}

internal class NavigationRailProperties(
    val style: NavigationRailStyle = NavigationRailStyle.Default,
    var windowInsets: WindowInsets = WindowInsets.Unspecified,
    val arrangement: Arrangement.Vertical = Arrangement.Top,
) {
    companion object {
        val Default = NavigationRailProperties()
    }
}

internal class NavigationRailItemProperties(
    val style: NavigationRailItemStyle = NavigationRailItemStyle.Default
) {
    companion object {
        val Default = NavigationRailItemProperties()
    }
}

internal val WindowInsets.Companion.Unspecified: WindowInsets
    get() = WindowInsets(Dp.Unspecified, Dp.Unspecified, Dp.Unspecified, Dp.Unspecified)
