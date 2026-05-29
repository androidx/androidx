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

package androidx.core.locationbutton.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Default values used by [LocationButton]. */
public object LocationButtonDefaults {
    /** Default background color. */
    public val backgroundColor: Color
        @Composable get() = MaterialTheme.colorScheme.primary

    /** Default text color. */
    public val textColor: Color
        @Composable get() = MaterialTheme.colorScheme.onPrimary

    /** Default icon tint. */
    public val iconTint: Color
        @Composable get() = MaterialTheme.colorScheme.onPrimary

    /** Default stroke color. */
    public val strokeColor: Color = Color.Unspecified

    /** Default stroke width. */
    public val strokeWidth: Dp = Dp.Unspecified

    /** Default corner radius. */
    public val cornerRadius: Dp = Dp.Unspecified

    /** Default pressed corner radius. */
    public val pressedCornerRadius: Dp = Dp.Unspecified

    /** Default clickable padding values. */
    public val clickablePadding: PaddingValues = PaddingValues(4.dp)

    /** Default text type. */
    public val textType: LocationButtonTextType = LocationButtonTextType.PreciseLocation

    /** Default composition order for SurfaceView hosting location button. */
    public const val defaultCompositionOrder: Int = 1
}
