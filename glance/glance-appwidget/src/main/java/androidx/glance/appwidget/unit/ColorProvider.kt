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

package androidx.glance.appwidget.unit

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.util.Log
import androidx.annotation.ColorRes
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.GlanceAppWidgetTag
import androidx.glance.appwidget.R
import androidx.glance.color.DayNightColorProvider
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.FixedColorProvider
import androidx.glance.unit.ResourceColorProvider

/** Provider of different colors depending on a checked state. */
internal sealed interface CheckableColorProvider

internal data class ResourceCheckableColorProvider(
    @ColorRes val resId: Int,
) : CheckableColorProvider

/**
 * Combination of two different [ColorProvider]s representing checked and unchecked states. These
 * must be [FixedColorProvider]s or [DayNightColorProvider]s.
 */
internal data class CheckedUncheckedColorProvider
private constructor(
    private val source: String,
    private val checked: ColorProvider,
    private val unchecked: ColorProvider,
) : CheckableColorProvider {

    init {
        require(checked !is ResourceColorProvider && unchecked !is ResourceColorProvider) {
            "Cannot provide resource-backed ColorProviders to $source"
        }
    }

    /**
     * Resolves the [CheckedUncheckedColorProvider] to a single [Color] given the night mode and
     * checked states.
     */
    fun getColor(context: Context, isNightMode: Boolean, isChecked: Boolean) =
        when {
            isChecked ->
                getColor(colorProvider = checked, isNightMode = isNightMode, context = context)
            else ->
                getColor(colorProvider = unchecked, isNightMode = isNightMode, context = context)
        }

    private fun getColor(colorProvider: ColorProvider, isNightMode: Boolean, context: Context) =
        if (colorProvider is DayNightColorProvider) {
            colorProvider.getColor(isNightMode)
        } else {
            colorProvider.getColor(context)
        }

    companion object {
        fun createCheckableColorProvider(
            source: String,
            checked: ColorProvider,
            unchecked: ColorProvider,
        ): CheckableColorProvider {
            return CheckedUncheckedColorProvider(source, checked, unchecked)
        }
    }
}

/** Resolves a color resource to a single color for the given checked state. */
internal fun resolveCheckedColor(
    context: Context,
    @ColorRes resId: Int,
    isChecked: Boolean,
    isNightMode: Boolean? = null
): Color? {
    if (resId == 0) return null

    val resolveContext =
        if (isNightMode == null) {
            context
        } else {
            val configuration = Configuration()
            configuration.uiMode =
                if (isNightMode) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            context.createConfigurationContext(configuration)
        }
    val colorStateList =
        try {
            ContextCompat.getColorStateList(resolveContext, resId) ?: return null
        } catch (e: Resources.NotFoundException) {
            Log.w(GlanceAppWidgetTag, "Could not resolve the checked color", e)
            return null
        }
    return Color(
        colorStateList.getColorForState(
            if (isChecked) CheckedStateSet else UncheckedStateSet,
            colorStateList.defaultColor
        )
    )
}

internal val CheckedStateSet = intArrayOf(android.R.attr.state_checked)
internal val UncheckedStateSet = intArrayOf(-android.R.attr.state_checked)
