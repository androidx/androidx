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
import android.os.Build
import androidx.annotation.ColorRes
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.glance.unit.ColorProvider
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertWithMessage
import kotlin.test.assertIs
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ColorProvidersTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun testGlanceMatchMaterial3Colors() {
        val lightColors = mapOf(
            androidx.glance.R.color.glance_colorPrimary to
                com.google.android.material.R.color.m3_sys_color_light_primary,
            androidx.glance.R.color.glance_colorOnPrimary to
                com.google.android.material.R.color.m3_sys_color_light_on_primary,
            androidx.glance.R.color.glance_colorPrimaryInverse to
                com.google.android.material.R.color.m3_sys_color_light_inverse_primary,
            androidx.glance.R.color.glance_colorPrimaryContainer to
                com.google.android.material.R.color.m3_sys_color_light_primary_container,
            androidx.glance.R.color.glance_colorOnPrimaryContainer to
                com.google.android.material.R.color.m3_sys_color_light_on_primary_container,
            androidx.glance.R.color.glance_colorSecondary to
                com.google.android.material.R.color.m3_sys_color_light_secondary,
            androidx.glance.R.color.glance_colorOnSecondary to
                com.google.android.material.R.color.m3_sys_color_light_on_secondary,
            androidx.glance.R.color.glance_colorSecondaryContainer to
                com.google.android.material.R.color.m3_sys_color_light_secondary_container,
            androidx.glance.R.color.glance_colorOnSecondaryContainer to
                com.google.android.material.R.color.m3_sys_color_light_on_secondary_container,
            androidx.glance.R.color.glance_colorTertiary to
                com.google.android.material.R.color.m3_sys_color_light_tertiary,
            androidx.glance.R.color.glance_colorOnTertiary to
                com.google.android.material.R.color.m3_sys_color_light_on_tertiary,
            androidx.glance.R.color.glance_colorTertiaryContainer to
                com.google.android.material.R.color.m3_sys_color_light_tertiary_container,
            androidx.glance.R.color.glance_colorOnTertiaryContainer to
                com.google.android.material.R.color.m3_sys_color_light_on_tertiary_container,
            androidx.glance.R.color.glance_colorBackground to
                com.google.android.material.R.color.m3_sys_color_light_background,
            androidx.glance.R.color.glance_colorOnBackground to
                com.google.android.material.R.color.m3_sys_color_light_on_background,
            androidx.glance.R.color.glance_colorSurface to
                com.google.android.material.R.color.m3_sys_color_light_surface,
            androidx.glance.R.color.glance_colorOnSurface to
                com.google.android.material.R.color.m3_sys_color_light_on_surface,
            androidx.glance.R.color.glance_colorSurfaceVariant to
                com.google.android.material.R.color.m3_sys_color_light_surface_variant,
            androidx.glance.R.color.glance_colorOnSurfaceVariant to
                com.google.android.material.R.color.m3_sys_color_light_on_surface_variant,
            androidx.glance.R.color.glance_colorSurfaceInverse to
                com.google.android.material.R.color.m3_sys_color_light_inverse_surface,
            androidx.glance.R.color.glance_colorOnSurfaceInverse to
                com.google.android.material.R.color.m3_sys_color_light_inverse_on_surface,
            androidx.glance.R.color.glance_colorOutline to
                com.google.android.material.R.color.m3_sys_color_light_outline,
            androidx.glance.R.color.glance_colorError to
                com.google.android.material.R.color.m3_sys_color_light_error,
            androidx.glance.R.color.glance_colorOnError to
                com.google.android.material.R.color.m3_sys_color_light_on_error,
            androidx.glance.R.color.glance_colorErrorContainer to
                com.google.android.material.R.color.m3_sys_color_light_error_container,
            androidx.glance.R.color.glance_colorOnErrorContainer to
                com.google.android.material.R.color.m3_sys_color_light_on_error_container,
        )
        lightColors.forEach {
            assertColor(it.key, it.value)
        }
    }

    @Test
    @Config(qualifiers = "night", sdk = [Build.VERSION_CODES.R])
    fun testGlanceMatchMaterial3NightColors() {
        val darkColors = mapOf(
            androidx.glance.R.color.glance_colorPrimary to
                com.google.android.material.R.color.m3_sys_color_dark_primary,
            androidx.glance.R.color.glance_colorOnPrimary to
                com.google.android.material.R.color.m3_sys_color_dark_on_primary,
            androidx.glance.R.color.glance_colorPrimaryInverse to
                com.google.android.material.R.color.m3_sys_color_dark_inverse_primary,
            androidx.glance.R.color.glance_colorPrimaryContainer to
                com.google.android.material.R.color.m3_sys_color_dark_primary_container,
            androidx.glance.R.color.glance_colorOnPrimaryContainer to
                com.google.android.material.R.color.m3_sys_color_dark_on_primary_container,
            androidx.glance.R.color.glance_colorSecondary to
                com.google.android.material.R.color.m3_sys_color_dark_secondary,
            androidx.glance.R.color.glance_colorOnSecondary to
                com.google.android.material.R.color.m3_sys_color_dark_on_secondary,
            androidx.glance.R.color.glance_colorSecondaryContainer to
                com.google.android.material.R.color.m3_sys_color_dark_secondary_container,
            androidx.glance.R.color.glance_colorOnSecondaryContainer to
                com.google.android.material.R.color.m3_sys_color_dark_on_secondary_container,
            androidx.glance.R.color.glance_colorTertiary to
                com.google.android.material.R.color.m3_sys_color_dark_tertiary,
            androidx.glance.R.color.glance_colorOnTertiary to
                com.google.android.material.R.color.m3_sys_color_dark_on_tertiary,
            androidx.glance.R.color.glance_colorTertiaryContainer to
                com.google.android.material.R.color.m3_sys_color_dark_tertiary_container,
            androidx.glance.R.color.glance_colorOnTertiaryContainer to
                com.google.android.material.R.color.m3_sys_color_dark_on_tertiary_container,
            androidx.glance.R.color.glance_colorBackground to
                com.google.android.material.R.color.m3_sys_color_dark_background,
            androidx.glance.R.color.glance_colorOnBackground to
                com.google.android.material.R.color.m3_sys_color_dark_on_background,
            androidx.glance.R.color.glance_colorSurface to
                com.google.android.material.R.color.m3_sys_color_dark_surface,
            androidx.glance.R.color.glance_colorOnSurface to
                com.google.android.material.R.color.m3_sys_color_dark_on_surface,
            androidx.glance.R.color.glance_colorSurfaceVariant to
                com.google.android.material.R.color.m3_sys_color_dark_surface_variant,
            androidx.glance.R.color.glance_colorOnSurfaceVariant to
                com.google.android.material.R.color.m3_sys_color_dark_on_surface_variant,
            androidx.glance.R.color.glance_colorSurfaceInverse to
                com.google.android.material.R.color.m3_sys_color_dark_inverse_surface,
            androidx.glance.R.color.glance_colorOnSurfaceInverse to
                com.google.android.material.R.color.m3_sys_color_dark_inverse_on_surface,
            androidx.glance.R.color.glance_colorOutline to
                com.google.android.material.R.color.m3_sys_color_dark_outline,
            androidx.glance.R.color.glance_colorError to
                com.google.android.material.R.color.m3_sys_color_dark_error,
            androidx.glance.R.color.glance_colorOnError to
                com.google.android.material.R.color.m3_sys_color_dark_on_error,
            androidx.glance.R.color.glance_colorErrorContainer to
                com.google.android.material.R.color.m3_sys_color_dark_error_container,
            androidx.glance.R.color.glance_colorOnErrorContainer to
                com.google.android.material.R.color.m3_sys_color_dark_on_error_container,
        )
        darkColors.forEach {
            assertColor(it.key, it.value)
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun testGlanceMatchMaterial3v31Colors() {
        val v31Colors = mapOf(
            androidx.glance.R.color.glance_colorPrimary to
                com.google.android.material.R.color.m3_sys_color_dynamic_light_primary,
            androidx.glance.R.color.glance_colorOnPrimary to
                com.google.android.material.R.color.m3_sys_color_dynamic_light_on_primary,
            androidx.glance.R.color.glance_colorPrimaryInverse to
                com.google.android.material.R.color.m3_sys_color_dynamic_light_inverse_primary,
            androidx.glance.R.color.glance_colorPrimaryContainer to
                com.google.android.material.R.color.m3_sys_color_dynamic_light_primary_container,
            androidx.glance.R.color.glance_colorOnPrimaryContainer to
                com.google.android.material.R.color.m3_sys_color_dynamic_light_on_primary_container,
            androidx.glance.R.color.glance_colorSecondary to
                com.google.android.material.R.color.m3_sys_color_dynamic_light_secondary,
            androidx.glance.R.color.glance_colorOnSecondary to
                com.google.android.material.R.color.m3_sys_color_dynamic_light_on_secondary,
            androidx.glance.R.color.glance_colorSecondaryContainer to
                com.google.android.material.R.color.m3_sys_color_dynamic_light_secondary_container,
            androidx.glance.R.color.glance_colorOnSecondaryContainer to
                com.google.android.material.R.color.m3_sys_color_dynamic_light_on_secondary_container, // ktlint-disable max-line-length
            androidx.glance.R.color.glance_colorTertiary to
                com.google.android.material.R.color.m3_sys_color_dynamic_light_tertiary,
            androidx.glance.R.color.glance_colorOnTertiary to
                com.google.android.material.R.color.m3_sys_color_dynamic_light_on_tertiary,
            androidx.glance.R.color.glance_colorTertiaryContainer to
                com.google.android.material.R.color.m3_sys_color_dynamic_light_tertiary_container,
            androidx.glance.R.color.glance_colorOnTertiaryContainer to
                com.google.android.material.R.color.m3_sys_color_dynamic_light_on_tertiary_container, // ktlint-disable max-line-length
            androidx.glance.R.color.glance_colorBackground to
                com.google.android.material.R.color.m3_sys_color_dynamic_light_background,
            androidx.glance.R.color.glance_colorOnBackground to
                com.google.android.material.R.color.m3_sys_color_dynamic_light_on_background,
            androidx.glance.R.color.glance_colorSurface to
                com.google.android.material.R.color.m3_sys_color_dynamic_light_surface,
            androidx.glance.R.color.glance_colorOnSurface to
                com.google.android.material.R.color.m3_sys_color_dynamic_light_on_surface,
            androidx.glance.R.color.glance_colorSurfaceVariant to
                com.google.android.material.R.color.m3_sys_color_dynamic_light_surface_variant,
            androidx.glance.R.color.glance_colorOnSurfaceVariant to
                com.google.android.material.R.color.m3_sys_color_dynamic_light_on_surface_variant,
            androidx.glance.R.color.glance_colorSurfaceInverse to
                com.google.android.material.R.color.m3_sys_color_dynamic_light_inverse_surface,
            androidx.glance.R.color.glance_colorOnSurfaceInverse to
                com.google.android.material.R.color.m3_sys_color_dynamic_light_inverse_on_surface,
            androidx.glance.R.color.glance_colorOutline to
                com.google.android.material.R.color.m3_sys_color_dynamic_light_outline,
            androidx.glance.R.color.glance_colorError to
                com.google.android.material.R.color.m3_sys_color_light_error,
            androidx.glance.R.color.glance_colorOnError to
                com.google.android.material.R.color.m3_sys_color_light_on_error,
            androidx.glance.R.color.glance_colorErrorContainer to
                com.google.android.material.R.color.m3_sys_color_light_error_container,
            androidx.glance.R.color.glance_colorOnErrorContainer to
                com.google.android.material.R.color.m3_sys_color_light_on_error_container,
        )
        v31Colors.forEach {
            assertColor(it.key, it.value)
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S], qualifiers = "night")
    fun testGlanceMatchMaterial3v31NightColors() {
        val v31NightColors = mapOf(
            androidx.glance.R.color.glance_colorPrimary to
                com.google.android.material.R.color.m3_sys_color_dynamic_dark_primary,
            androidx.glance.R.color.glance_colorOnPrimary to
                com.google.android.material.R.color.m3_sys_color_dynamic_dark_on_primary,
            androidx.glance.R.color.glance_colorPrimaryInverse to
                com.google.android.material.R.color.m3_sys_color_dynamic_dark_inverse_primary,
            androidx.glance.R.color.glance_colorPrimaryContainer to
                com.google.android.material.R.color.m3_sys_color_dynamic_dark_primary_container,
            androidx.glance.R.color.glance_colorOnPrimaryContainer to
                com.google.android.material.R.color.m3_sys_color_dynamic_dark_on_primary_container,
            androidx.glance.R.color.glance_colorSecondary to
                com.google.android.material.R.color.m3_sys_color_dynamic_dark_secondary,
            androidx.glance.R.color.glance_colorOnSecondary to
                com.google.android.material.R.color.m3_sys_color_dynamic_dark_on_secondary,
            androidx.glance.R.color.glance_colorSecondaryContainer to
                com.google.android.material.R.color.m3_sys_color_dynamic_dark_secondary_container,
            androidx.glance.R.color.glance_colorOnSecondaryContainer to
                com.google.android.material.R.color.m3_sys_color_dynamic_dark_on_secondary_container, // ktlint-disable max-line-length
            androidx.glance.R.color.glance_colorTertiary to
                com.google.android.material.R.color.m3_sys_color_dynamic_dark_tertiary,
            androidx.glance.R.color.glance_colorOnTertiary to
                com.google.android.material.R.color.m3_sys_color_dynamic_dark_on_tertiary,
            androidx.glance.R.color.glance_colorTertiaryContainer to
                com.google.android.material.R.color.m3_sys_color_dynamic_dark_tertiary_container,
            androidx.glance.R.color.glance_colorOnTertiaryContainer to
                com.google.android.material.R.color.m3_sys_color_dynamic_dark_on_tertiary_container,
            androidx.glance.R.color.glance_colorBackground to
                com.google.android.material.R.color.m3_sys_color_dynamic_dark_background,
            androidx.glance.R.color.glance_colorOnBackground to
                com.google.android.material.R.color.m3_sys_color_dynamic_dark_on_background,
            androidx.glance.R.color.glance_colorSurface to
                com.google.android.material.R.color.m3_sys_color_dynamic_dark_surface,
            androidx.glance.R.color.glance_colorOnSurface to
                com.google.android.material.R.color.m3_sys_color_dynamic_dark_on_surface,
            androidx.glance.R.color.glance_colorSurfaceVariant to
                com.google.android.material.R.color.m3_sys_color_dynamic_dark_surface_variant,
            androidx.glance.R.color.glance_colorOnSurfaceVariant to
                com.google.android.material.R.color.m3_sys_color_dynamic_dark_on_surface_variant,
            androidx.glance.R.color.glance_colorSurfaceInverse to
                com.google.android.material.R.color.m3_sys_color_dynamic_dark_inverse_surface,
            androidx.glance.R.color.glance_colorOnSurfaceInverse to
                com.google.android.material.R.color.m3_sys_color_dynamic_dark_inverse_on_surface,
            androidx.glance.R.color.glance_colorOutline to
                com.google.android.material.R.color.m3_sys_color_dynamic_dark_outline,
            androidx.glance.R.color.glance_colorError to
                com.google.android.material.R.color.m3_sys_color_dark_error,
            androidx.glance.R.color.glance_colorOnError to
                com.google.android.material.R.color.m3_sys_color_dark_on_error,
            androidx.glance.R.color.glance_colorErrorContainer to
                com.google.android.material.R.color.m3_sys_color_dark_error_container,
            androidx.glance.R.color.glance_colorOnErrorContainer to
                com.google.android.material.R.color.m3_sys_color_dark_on_error_container,
        )
        v31NightColors.forEach {
            assertColor(it.key, it.value)
        }
    }

    private fun assertColor(@ColorRes source: Int, @ColorRes target: Int) {
        val sourceColor = ContextCompat.getColor(context, source)
        val targetColor = ContextCompat.getColor(context, target)

        val sourceHex = String.format("0x%08X", sourceColor)
        val targetHex = String.format("0x%08X", targetColor)

        val sourceName = context.resources.getResourceEntryName(source)
        val targetName = context.resources.getResourceEntryName(target)

        val message = "$sourceName is $sourceHex but $targetName is $targetHex"

        assertWithMessage(message).that(sourceColor).isEqualTo(targetColor)
    }

    @Test
    fun ensureCustomColorSchemesArePossible() {
        val testColor = ColorProvider(Color.Magenta)
        assertIs<CustomColorProviders>(
            colorProviders(
                primary = testColor,
                onPrimary = testColor,
                primaryContainer = testColor,
                onPrimaryContainer = testColor,
                secondary = testColor,
                onSecondary = testColor,
                secondaryContainer = testColor,
                onSecondaryContainer = testColor,
                tertiary = testColor,
                onTertiary = testColor,
                tertiaryContainer = testColor,
                onTertiaryContainer = testColor,
                error = testColor,
                errorContainer = testColor,
                onError = testColor,
                onErrorContainer = testColor,
                background = testColor,
                onBackground = testColor,
                surface = testColor,
                onSurface = testColor,
                surfaceVariant = testColor,
                onSurfaceVariant = testColor,
                outline = testColor,
                inverseOnSurface = testColor,
                inverseSurface = testColor,
                inversePrimary = testColor
            )
        )
    }
}
