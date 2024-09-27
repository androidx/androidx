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
package androidx.wear.protolayout.material3

import android.R
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.wear.protolayout.ColorBuilders.ColorProp
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.material3.ColorTokens.BACKGROUND
import androidx.wear.protolayout.material3.ColorTokens.ColorToken
import androidx.wear.protolayout.material3.ColorTokens.ERROR
import androidx.wear.protolayout.material3.ColorTokens.ERROR_CONTAINER
import androidx.wear.protolayout.material3.ColorTokens.ON_BACKGROUND
import androidx.wear.protolayout.material3.ColorTokens.ON_ERROR
import androidx.wear.protolayout.material3.ColorTokens.ON_ERROR_CONTAINER
import androidx.wear.protolayout.material3.ColorTokens.ON_PRIMARY
import androidx.wear.protolayout.material3.ColorTokens.ON_PRIMARY_CONTAINER
import androidx.wear.protolayout.material3.ColorTokens.ON_SECONDARY
import androidx.wear.protolayout.material3.ColorTokens.ON_SECONDARY_CONTAINER
import androidx.wear.protolayout.material3.ColorTokens.ON_SURFACE
import androidx.wear.protolayout.material3.ColorTokens.ON_SURFACE_VARIANT
import androidx.wear.protolayout.material3.ColorTokens.ON_TERTIARY
import androidx.wear.protolayout.material3.ColorTokens.ON_TERTIARY_CONTAINER
import androidx.wear.protolayout.material3.ColorTokens.OUTLINE
import androidx.wear.protolayout.material3.ColorTokens.OUTLINE_VARIANT
import androidx.wear.protolayout.material3.ColorTokens.PRIMARY
import androidx.wear.protolayout.material3.ColorTokens.PRIMARY_CONTAINER
import androidx.wear.protolayout.material3.ColorTokens.PRIMARY_DIM
import androidx.wear.protolayout.material3.ColorTokens.SECONDARY
import androidx.wear.protolayout.material3.ColorTokens.SECONDARY_CONTAINER
import androidx.wear.protolayout.material3.ColorTokens.SECONDARY_DIM
import androidx.wear.protolayout.material3.ColorTokens.SURFACE_CONTAINER
import androidx.wear.protolayout.material3.ColorTokens.SURFACE_CONTAINER_HIGH
import androidx.wear.protolayout.material3.ColorTokens.SURFACE_CONTAINER_LOW
import androidx.wear.protolayout.material3.ColorTokens.TERTIARY
import androidx.wear.protolayout.material3.ColorTokens.TERTIARY_CONTAINER
import androidx.wear.protolayout.material3.ColorTokens.TERTIARY_DIM

/**
 * Holds helper functions for mapping Material 3 tokens to corresponding Android resource color IDs.
 */
internal object DynamicMaterialTheme {
    @VisibleForTesting
    val tokenNameToResourceId: Map<Int, Int> =
        if (Build.VERSION.SDK_INT >= 34) tokenMappings34() else emptyMap()

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun tokenMappings34(): Map<Int, Int> =
        // From
        // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/res/res/values/public-final.xml;l=3500;drc=2a8b6a18e0b7f696013ffede0cc0ab1904864d09
        // TODO: b/340192801 - Confirm once it's fully supported in system.
        mapOf(
            BACKGROUND to R.color.system_background_dark,
            ERROR to R.color.system_error_dark,
            ERROR_CONTAINER to R.color.system_error_container_dark,
            ON_BACKGROUND to R.color.system_on_background_dark,
            ON_ERROR to R.color.system_on_error_dark,
            ON_ERROR_CONTAINER to R.color.system_on_error_container_dark,
            ON_PRIMARY to R.color.system_on_primary_fixed,
            ON_PRIMARY_CONTAINER to R.color.system_on_primary_container_dark,
            ON_SECONDARY to R.color.system_on_secondary_fixed,
            ON_SECONDARY_CONTAINER to R.color.system_on_secondary_container_dark,
            ON_SURFACE to R.color.system_on_surface_dark,
            ON_SURFACE_VARIANT to R.color.system_on_surface_variant_dark,
            ON_TERTIARY to R.color.system_on_tertiary_fixed,
            ON_TERTIARY_CONTAINER to R.color.system_on_tertiary_container_dark,
            OUTLINE to R.color.system_outline_dark,
            OUTLINE_VARIANT to R.color.system_outline_variant_dark,
            PRIMARY to R.color.system_primary_fixed,
            PRIMARY_CONTAINER to R.color.system_primary_container_dark,
            PRIMARY_DIM to R.color.system_primary_fixed_dim,
            SECONDARY to R.color.system_secondary_fixed,
            SECONDARY_CONTAINER to R.color.system_secondary_container_dark,
            SECONDARY_DIM to R.color.system_secondary_fixed_dim,
            SURFACE_CONTAINER to R.color.system_surface_container_dark,
            SURFACE_CONTAINER_HIGH to R.color.system_surface_container_high_dark,
            SURFACE_CONTAINER_LOW to R.color.system_surface_container_low_dark,
            TERTIARY to R.color.system_tertiary_fixed,
            TERTIARY_CONTAINER to R.color.system_tertiary_container_dark,
            TERTIARY_DIM to R.color.system_tertiary_fixed_dim
        )

    /** Retrieves the [ColorProp] from the dynamic system theme with the given color token name. */
    fun getColorProp(context: Context, @ColorToken colorName: Int): ColorProp? {
        val mappedColor: Int? = tokenNameToResourceId.get(colorName)
        return if (mappedColor != null) {
            argb(context.resources.getColor(mappedColor, context.theme))
        } else {
            null
        }
    }
}
