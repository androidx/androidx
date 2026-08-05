/*
 * Copyright 2022 The Android Open Source Project
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

// VERSION: v0_162
// GENERATED CODE - DO NOT MODIFY BY HAND

package androidx.compose.material3.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

internal object PrimaryNavigationTabTokens {
    inline val ActiveIndicatorColor: ColorToken
        get() = ColorSchemeKeyTokens.Primary

    inline val ActiveIndicatorHeight: androidx.compose.ui.unit.Dp
        get() = 3.0.dp

    val ActiveIndicatorShape = RoundedCornerShape(3.0.dp)
    inline val ContainerColor: ColorToken
        get() = ColorSchemeKeyTokens.Surface

    inline val ContainerElevation: androidx.compose.ui.unit.Dp
        get() = ElevationTokens.Level0

    inline val ContainerHeight: androidx.compose.ui.unit.Dp
        get() = 48.0.dp

    inline val ContainerShape: ShapeToken
        get() = ShapeKeyTokens.CornerNone

    inline val ActiveFocusIconColor: ColorToken
        get() = ColorSchemeKeyTokens.Primary

    inline val ActiveHoverIconColor: ColorToken
        get() = ColorSchemeKeyTokens.Primary

    inline val ActiveIconColor: ColorToken
        get() = ColorSchemeKeyTokens.Primary

    inline val ActivePressedIconColor: ColorToken
        get() = ColorSchemeKeyTokens.Primary

    inline val IconAndLabelTextContainerHeight: androidx.compose.ui.unit.Dp
        get() = 64.0.dp

    inline val IconSize: androidx.compose.ui.unit.Dp
        get() = 24.0.dp

    inline val InactiveFocusIconColor: ColorToken
        get() = ColorSchemeKeyTokens.OnSurface

    inline val InactiveHoverIconColor: ColorToken
        get() = ColorSchemeKeyTokens.OnSurface

    inline val InactiveIconColor: ColorToken
        get() = ColorSchemeKeyTokens.OnSurfaceVariant

    inline val InactivePressedIconColor: ColorToken
        get() = ColorSchemeKeyTokens.OnSurface

    inline val ActiveFocusLabelTextColor: ColorToken
        get() = ColorSchemeKeyTokens.Primary

    inline val ActiveHoverLabelTextColor: ColorToken
        get() = ColorSchemeKeyTokens.Primary

    inline val ActiveLabelTextColor: ColorToken
        get() = ColorSchemeKeyTokens.Primary

    inline val ActivePressedLabelTextColor: ColorToken
        get() = ColorSchemeKeyTokens.Primary

    inline val InactiveFocusLabelTextColor: ColorToken
        get() = ColorSchemeKeyTokens.OnSurface

    inline val InactiveHoverLabelTextColor: ColorToken
        get() = ColorSchemeKeyTokens.OnSurface

    inline val InactiveLabelTextColor: ColorToken
        get() = ColorSchemeKeyTokens.OnSurfaceVariant

    inline val InactivePressedLabelTextColor: ColorToken
        get() = ColorSchemeKeyTokens.OnSurface

    inline val LabelTextFont: TypographyToken
        get() = TypographyKeyTokens.TitleSmall
}
