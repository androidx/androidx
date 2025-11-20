/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.wear.tiles

import android.content.res.Resources
import androidx.annotation.DimenRes
import androidx.annotation.RestrictTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.unit.ColorProvider

/**
 * Apply a border around an element, border width is provided in Dp
 *
 * @param width The width of the border, in DP
 * @param color The color of the border
 */
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public fun GlanceModifier.border(width: Dp, color: ColorProvider): GlanceModifier =
    this.then(BorderModifier(BorderDimension(dp = width), color))

/**
 * Apply a border around an element, border width is provided with dimension resource
 *
 * @param width The width of the border, value provided by a dimension resource
 * @param color The color of the border
 */
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public fun GlanceModifier.border(@DimenRes width: Int, color: ColorProvider): GlanceModifier =
    this.then(BorderModifier(BorderDimension(resourceId = width), color))

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public data class BorderModifier(
    public val width: BorderDimension,
    public val color: ColorProvider,
) : GlanceModifier.Element

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public data class BorderDimension(
    public val dp: Dp = 0.dp,
    @DimenRes public val resourceId: Int = 0,
) {
    public fun toDp(resources: Resources): Dp =
        if (resourceId == 0) dp
        else (resources.getDimension(resourceId) / resources.displayMetrics.density).dp
}
