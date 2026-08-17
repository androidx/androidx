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

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.operations.ColorTheme
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcColorThemePlayerTest {

    @Test
    fun testResolveThemeMode() {
        assertThat(resolveThemeMode(Theme.LIGHT, isSystemInDarkTheme = false))
            .isEqualTo(Theme.LIGHT)
        assertThat(resolveThemeMode(Theme.LIGHT, isSystemInDarkTheme = true)).isEqualTo(Theme.LIGHT)
        assertThat(resolveThemeMode(Theme.DARK, isSystemInDarkTheme = false)).isEqualTo(Theme.DARK)
        assertThat(resolveThemeMode(Theme.DARK, isSystemInDarkTheme = true)).isEqualTo(Theme.DARK)
        assertThat(resolveThemeMode(Theme.SYSTEM, isSystemInDarkTheme = false))
            .isEqualTo(Theme.LIGHT)
        assertThat(resolveThemeMode(Theme.SYSTEM, isSystemInDarkTheme = true)).isEqualTo(Theme.DARK)
        assertThat(resolveThemeMode(Theme.UNSPECIFIED, isSystemInDarkTheme = false))
            .isEqualTo(Theme.LIGHT)
        assertThat(resolveThemeMode(Theme.UNSPECIFIED, isSystemInDarkTheme = true))
            .isEqualTo(Theme.DARK)
    }

    @Test
    fun testAndroidColorThemeMappingResolvesFrameworkColors() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val colorTheme =
            ColorTheme(
                    100, // id
                    -1, // colorGroupId
                    3.toShort(), // lightModeIndex (darker_gray: android.R.color.darker_gray)
                    2.toShort(), // darkModeIndex (black: android.R.color.black)
                    0xFFEEEEEE.toInt(), // fallbackLight
                    0xFF111111.toInt(), // fallbackDark
                )
                .apply { mColorGroupName = "android" }

        AndroidColorThemeResolver.mapColors(context, listOf(colorTheme))

        val expectedBlack = context.getColor(android.R.color.black)
        val expectedDarkerGray = context.getColor(android.R.color.darker_gray)

        assertThat(colorTheme.mDarkMode).isEqualTo(expectedBlack)
        assertThat(colorTheme.mLightMode).isEqualTo(expectedDarkerGray)
    }

    @Test
    fun testAndroidColorThemeMappingSkipsNonAndroidColorGroup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val colorTheme =
            ColorTheme(
                    100, // id
                    -1, // colorGroupId
                    3.toShort(), // lightModeIndex
                    2.toShort(), // darkModeIndex
                    0xFFEEEEEE.toInt(), // fallbackLight
                    0xFF111111.toInt(), // fallbackDark
                )
                .apply { mColorGroupName = "custom_group" }

        AndroidColorThemeResolver.mapColors(context, listOf(colorTheme))

        // Authored fallbacks must remain untouched
        assertThat(colorTheme.mLightMode).isEqualTo(0xFFEEEEEE.toInt())
        assertThat(colorTheme.mDarkMode).isEqualTo(0xFF111111.toInt())
    }

    @Test
    fun testAndroidColorThemeMappingSkipsNullColorGroup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val colorTheme =
            ColorTheme(
                    100, // id
                    -1, // colorGroupId
                    3.toShort(), // lightModeIndex
                    2.toShort(), // darkModeIndex
                    0xFFEEEEEE.toInt(), // fallbackLight
                    0xFF111111.toInt(), // fallbackDark
                )
                .apply { mColorGroupName = null }

        AndroidColorThemeResolver.mapColors(context, listOf(colorTheme))

        // Authored fallbacks must remain untouched
        assertThat(colorTheme.mLightMode).isEqualTo(0xFFEEEEEE.toInt())
        assertThat(colorTheme.mDarkMode).isEqualTo(0xFF111111.toInt())
    }

    @Test
    fun testColorThemeAppliesAccordingToPaintTheme() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val document = CoreDocument(RemoteClock.SYSTEM)
        val remoteContext =
            AndroidRemoteContext(RemoteClock.SYSTEM).apply { setAndroidContext(context) }

        val colorTheme =
            ColorTheme(
                100,
                -1,
                (-1).toShort(),
                (-1).toShort(),
                0xFFFFFFFF.toInt(), // light color
                0xFF000000.toInt(), // dark color
            )
        document.getOperationsReflection().add(colorTheme)

        // Light mode
        remoteContext.paintTheme = Theme.LIGHT
        colorTheme.apply(remoteContext)
        assertThat(remoteContext.getColor(100)).isEqualTo(0xFFFFFFFF.toInt())

        // Dark mode
        remoteContext.paintTheme = Theme.DARK
        colorTheme.apply(remoteContext)
        assertThat(remoteContext.getColor(100)).isEqualTo(0xFF000000.toInt())
    }
}
