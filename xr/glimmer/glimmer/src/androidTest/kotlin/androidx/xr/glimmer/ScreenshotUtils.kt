/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.xr.glimmer.testutils.captureToImage
import androidx.xr.glimmer.testutils.setContentWithDensity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

internal const val GOLDEN_DIRECTORY = "xr/glimmer/glimmer"

internal fun ComposeContentTestRule.setGlimmerThemeContent(
    addInitialFocusInterceptor: Boolean = false,
    density: Density = this.density,
    content: @Composable () -> Unit,
) {
    setContentWithDensity(density) {
        GlimmerTheme {
            Column(Modifier.background(GlimmerTheme.colors.surface)) {
                if (addInitialFocusInterceptor) {
                    Box(Modifier.size(1.dp).focusable())
                }
                content()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun ComposeContentTestRule.assertRootAgainstGolden(
    goldenName: String,
    screenshotRule: AndroidXScreenshotTestRule,
) {
    onRoot().captureToImage().assertAgainstGolden(screenshotRule, goldenName, MSSIMMatcher(0.995))
}

internal val AlwaysFocusedInteractionSource: MutableInteractionSource =
    StaticMutableInteractionSource(FocusInteraction.Focus())

internal val AlwaysPressedInteractionSource: MutableInteractionSource =
    StaticMutableInteractionSource(PressInteraction.Press(Offset.Zero))

internal val AlwaysFocusedAndPressedInteractionSource: MutableInteractionSource =
    StaticMutableInteractionSource(FocusInteraction.Focus(), PressInteraction.Press(Offset.Zero))

private class StaticMutableInteractionSource(vararg interactionsToShow: Interaction) :
    MutableInteractionSource {
    override val interactions: Flow<Interaction> = interactionsToShow.asFlow()

    override suspend fun emit(interaction: Interaction) {}

    override fun tryEmit(interaction: Interaction): Boolean = true
}
