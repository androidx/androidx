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

package androidx.wear.compose.material3.tokens

internal object SliderTokens {
    val ContainerColor = ColorSchemeKeyTokens.SurfaceContainer
    val ButtonIconColor = ColorSchemeKeyTokens.OnSurface
    val SelectedBarColor = ColorSchemeKeyTokens.Primary
    val UnselectedBarSeparatorColor = ColorSchemeKeyTokens.Primary
    val VariantSelectedBarColor = ColorSchemeKeyTokens.PrimaryDim
    val VariantUnselectedBarSeparatorColor = ColorSchemeKeyTokens.PrimaryDim
    val UnselectedBarSeparatorOpacity = 0.5f
    val UnselectedBarColor = ColorSchemeKeyTokens.Background
    val SelectedBarSeparatorColor = ColorSchemeKeyTokens.PrimaryContainer

    val DisabledContainerColor = ColorSchemeKeyTokens.OnSurface
    val DisabledContainerOpacity = 0.12f
    val DisabledButtonIconColor = ColorSchemeKeyTokens.OnSurface
    val DisabledButtonIconOpacity = 0.38f
    val DisabledSelectedBarColor = ColorSchemeKeyTokens.OutlineVariant
    val DisabledUnselectedBarColor = ColorSchemeKeyTokens.Background
    val DisabledSelectedBarSeparatorColor = ColorSchemeKeyTokens.SurfaceContainer
    val DisabledUnselectedBarSeparatorColor = ColorSchemeKeyTokens.OutlineVariant

    val ContainerShape = ShapeKeyTokens.CornerLarge
}
