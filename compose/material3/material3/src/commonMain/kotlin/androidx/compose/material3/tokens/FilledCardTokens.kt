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
// VERSION: 41.1.19
// GENERATED CODE - DO NOT MODIFY BY HAND

package androidx.compose.material3.tokens

import androidx.compose.ui.unit.dp

internal object FilledCardTokens {
    // References
    inline val ContainerColor: ColorToken
        get() = ColorSchemeKeyTokens.SurfaceContainerHighest

    inline val ContainerShape: ShapeToken
        get() = ShapeKeyTokens.CornerMedium

    // TODO(b/588071802): This value is outdated, but updating it is a breaking visual change
    inline val DisabledContainerColor: ColorToken
        get() = ColorSchemeKeyTokens.SurfaceVariant

    inline val FocusIndicatorColor: ColorToken
        get() = ColorSchemeKeyTokens.Secondary

    inline val IconColor: ColorToken
        get() = ColorSchemeKeyTokens.Primary

    // Values
    inline val ContainerElevation: androidx.compose.ui.unit.Dp
        get() = ElevationTokens.Level0

    inline val DisabledContainerElevation: androidx.compose.ui.unit.Dp
        get() = ElevationTokens.Level0

    const val DisabledContainerOpacity: Float = 0.38f
    inline val DraggedContainerElevation: androidx.compose.ui.unit.Dp
        get() = ElevationTokens.Level3

    inline val FocusContainerElevation: androidx.compose.ui.unit.Dp
        get() = ElevationTokens.Level0

    inline val HoverContainerElevation: androidx.compose.ui.unit.Dp
        get() = ElevationTokens.Level1

    inline val IconSize: androidx.compose.ui.unit.Dp
        get() = 24.0.dp

    inline val PressedContainerElevation: androidx.compose.ui.unit.Dp
        get() = ElevationTokens.Level0
}
