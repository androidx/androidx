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

package androidx.wear.compose.remote.material3

import androidx.collection.buildObjectIntMap
import androidx.compose.foundation.background
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.GridScreenshotUI
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.wear.compose.remote.material3.util.SCREENSHOT_GOLDEN_DIRECTORY
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class DynamicColorTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            matcher = MSSIMMatcher(threshold = 0.999),
        )
    private val creationDisplayInfo =
        createCreationDisplayInfo(ApplicationProvider.getApplicationContext(), Size(800f, 2000f))

    @Test
    fun dynamic_color_grid() {
        val colorOverrides = buildObjectIntMap {
            put("WearM3.primary", Color(0xFFB8D0A0).toArgb())
            put("WearM3.primaryDim", Color(0xFFB0C0A0).toArgb())
            put("WearM3.primaryContainer", Color(0xFFC0D0B0).toArgb())
            put("WearM3.onPrimary", Color(0xFF24361A).toArgb())
            put("WearM3.onPrimaryContainer", Color(0xFF34462A).toArgb())
            put("WearM3.secondary", Color(0xFFD0E0C0).toArgb())
            put("WearM3.secondaryDim", Color(0xFFC0D0B0).toArgb())
            put("WearM3.secondaryContainer", Color(0xFFB0C0A0).toArgb())
            put("WearM3.onSecondary", Color(0xFF24361A).toArgb())
            put("WearM3.onSecondaryContainer", Color(0xFF34462A).toArgb())
            put("WearM3.tertiary", Color(0xFFE0F0D0).toArgb())
            put("WearM3.tertiaryDim", Color(0xFFD0E0C0).toArgb())
            put("WearM3.tertiaryContainer", Color(0xFFC0D0B0).toArgb())
            put("WearM3.onTertiary", Color(0xFF24361A).toArgb())
            put("WearM3.onTertiaryContainer", Color(0xFF34462A).toArgb())
            put("WearM3.surfaceContainerLow", Color(0xFF121310).toArgb())
            put("WearM3.surfaceContainer", Color(0xFF1C1D1A).toArgb())
            put("WearM3.surfaceContainerHigh", Color(0xFF262724).toArgb())
            put("WearM3.onSurface", Color(0xFFE2E3DC).toArgb())
            put("WearM3.onSurfaceVariant", Color(0xFFC2C3BC).toArgb())
            put("WearM3.outline", Color(0xFF8C8D86).toArgb())
            put("WearM3.outlineVariant", Color(0xFF6C6D66).toArgb())
            put("WearM3.background", Color(0xFF0C0D0A).toArgb())
            put("WearM3.onBackground", Color(0xFFE2E3DC).toArgb())
            put("WearM3.error", Color(0xFFFFB4AB).toArgb())
            put("WearM3.errorDim", Color(0xFFF28B82).toArgb())
            put("WearM3.errorContainer", Color(0xFF93000A).toArgb())
            put("WearM3.onError", Color(0xFF690005).toArgb())
            put("WearM3.onErrorContainer", Color(0xFFFFDAD6).toArgb())
        }

        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo = creationDisplayInfo,
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            update = { player ->
                colorOverrides.forEach { name, colorInt ->
                    player.setUserLocalColor(name, colorInt)
                }
            },
            playComposableWrapper = ComposableWrappers.blackBackground,
        ) {
            val gridUI = GridScreenshotUI(itemsPerRow = 4, ContainerSize = 50.rdp)
            val colorScheme = RemoteMaterialTheme.colorScheme

            val colors =
                listOf(
                    "primary" to colorScheme.primary,
                    "primaryDim" to colorScheme.primaryDim,
                    "primaryContainer" to colorScheme.primaryContainer,
                    "onPrimary" to colorScheme.onPrimary,
                    "onPrimaryContainer" to colorScheme.onPrimaryContainer,
                    "secondary" to colorScheme.secondary,
                    "secondaryDim" to colorScheme.secondaryDim,
                    "secondaryContainer" to colorScheme.secondaryContainer,
                    "onSecondary" to colorScheme.onSecondary,
                    "onSecondaryContainer" to colorScheme.onSecondaryContainer,
                    "tertiary" to colorScheme.tertiary,
                    "tertiaryDim" to colorScheme.tertiaryDim,
                    "tertiaryContainer" to colorScheme.tertiaryContainer,
                    "onTertiary" to colorScheme.onTertiary,
                    "onTertiaryContainer" to colorScheme.onTertiaryContainer,
                    "surfaceContainerLow" to colorScheme.surfaceContainerLow,
                    "surfaceContainer" to colorScheme.surfaceContainer,
                    "surfaceContainerHigh" to colorScheme.surfaceContainerHigh,
                    "onSurface" to colorScheme.onSurface,
                    "onSurfaceVariant" to colorScheme.onSurfaceVariant,
                    "outline" to colorScheme.outline,
                    "outlineVariant" to colorScheme.outlineVariant,
                    "background" to colorScheme.background,
                    "onBackground" to colorScheme.onBackground,
                    "error" to colorScheme.error,
                    "errorDim" to colorScheme.errorDim,
                    "errorContainer" to colorScheme.errorContainer,
                    "onError" to colorScheme.onError,
                    "onErrorContainer" to colorScheme.onErrorContainer,
                )
            gridUI.GridContent(
                innerContentList =
                    colors.map { (name, color) ->
                        name to @Composable @RemoteComposable { ColorRect(color) }
                    }
            )
        }
    }

    @Composable
    @RemoteComposable
    private fun ColorRect(remoteColor: RemoteColor) {
        RemoteCanvas(RemoteModifier.size(50.rdp)) { drawRect(RemotePaint { color = remoteColor }) }
    }
}
