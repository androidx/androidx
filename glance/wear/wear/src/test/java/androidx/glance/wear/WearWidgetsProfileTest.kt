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

package androidx.glance.wear

import androidx.compose.remote.core.Operations.CLIP_PATH
import androidx.compose.remote.core.Operations.COLOR_THEME
import androidx.compose.remote.core.Operations.DATA_BITMAP_FONT
import androidx.compose.remote.core.Operations.DATA_SHADER
import androidx.compose.remote.core.Operations.DRAW_BITMAP_FONT_TEXT_RUN
import androidx.compose.remote.core.Operations.DRAW_BITMAP_FONT_TEXT_RUN_ON_PATH
import androidx.compose.remote.core.Operations.DRAW_TEXT_ON_CIRCLE
import androidx.compose.remote.core.Operations.DRAW_TEXT_ON_PATH
import androidx.compose.remote.core.Operations.DRAW_TO_BITMAP
import androidx.compose.remote.core.Operations.LAYOUT_COLLAPSIBLE_COLUMN
import androidx.compose.remote.core.Operations.LAYOUT_COLLAPSIBLE_ROW
import androidx.compose.remote.core.Operations.MODIFIER_ALIGN_BY
import androidx.compose.remote.core.Operations.MODIFIER_COLLAPSIBLE_PRIORITY
import androidx.compose.remote.core.Operations.MODIFIER_SCROLL
import androidx.compose.remote.core.Operations.MODIFIER_TOUCH_CANCEL
import androidx.compose.remote.core.Operations.MODIFIER_TOUCH_DOWN
import androidx.compose.remote.core.Operations.MODIFIER_TOUCH_UP
import androidx.compose.remote.core.Operations.PARTICLE_COMPARE
import androidx.compose.remote.core.Operations.PARTICLE_DEFINE
import androidx.compose.remote.core.Operations.PARTICLE_LOOP
import androidx.compose.remote.core.Operations.ROOT_CONTENT_DESCRIPTION
import androidx.compose.remote.core.Operations.SKIP
import androidx.compose.remote.core.Operations.TEXT_STYLE
import androidx.compose.remote.core.Operations.THEME
import androidx.compose.remote.core.Operations.TOUCH_EXPRESSION
import androidx.compose.remote.core.Operations.WAKE_IN
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearWidgetsProfileTest {

    @Test
    fun testRequiredOperations() {
        val profile = GlanceWearProfiles.wearWidgets()
        val operations = profile.supportedOperations
        val exclusions =
            setOf(
                COLOR_THEME,
                CLIP_PATH,
                DATA_BITMAP_FONT,
                DATA_SHADER,
                DRAW_BITMAP_FONT_TEXT_RUN,
                DRAW_BITMAP_FONT_TEXT_RUN_ON_PATH,
                DRAW_TEXT_ON_PATH,
                DRAW_TO_BITMAP,
                LAYOUT_COLLAPSIBLE_COLUMN,
                LAYOUT_COLLAPSIBLE_ROW,
                MODIFIER_COLLAPSIBLE_PRIORITY,
                MODIFIER_SCROLL,
                MODIFIER_TOUCH_CANCEL,
                MODIFIER_TOUCH_DOWN,
                MODIFIER_TOUCH_UP,
                PARTICLE_COMPARE,
                PARTICLE_DEFINE,
                PARTICLE_LOOP,
                ROOT_CONTENT_DESCRIPTION,
                SKIP,
                TEXT_STYLE,
                THEME,
                TOUCH_EXPRESSION,
                WAKE_IN,
            )

        // Things not in AndroidX profile, so either experimental, Widgets or Wear related
        val missingFromAndroidX = setOf(DRAW_TEXT_ON_CIRCLE, MODIFIER_ALIGN_BY)

        assertThat(operations.intersect(exclusions)).isEmpty()

        // Test the things deliberately excluded
        assertThat(operations - RcPlatformProfiles.ANDROIDX.supportedOperations)
            .containsExactlyElementsIn(missingFromAndroidX)

        // Fail if things are added to AndroidX Baseline without consideration here
        assertThat(operations)
            .containsAtLeastElementsIn(RcPlatformProfiles.ANDROIDX.supportedOperations - exclusions)
    }
}
