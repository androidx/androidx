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

package androidx.glance.color

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.RestrictTo
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

/**
 * Returns a [ColorProvider] that provides [day] when night mode is off, and [night] when night
 * mode is on.
 */
fun ColorProvider(day: Color, night: Color): ColorProvider {
    return DayNightColorProvider(day, night)
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class DayNightColorProvider(val day: Color, val night: Color) : ColorProvider {
    override fun getColor(context: Context) = getColor(isNightMode = context.isNightMode)

    fun getColor(isNightMode: Boolean) = if (isNightMode) night else day
}

val Context.isNightMode: Boolean
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    get() = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
