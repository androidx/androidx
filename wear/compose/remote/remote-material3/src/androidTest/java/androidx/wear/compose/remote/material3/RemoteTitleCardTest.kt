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

import android.content.Context
import androidx.collection.buildObjectIntMap
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.remote.material3.previews.RemoteTitleCardDefault
import androidx.wear.compose.remote.material3.previews.RemoteTitleCardWithTitleSubtitle
import androidx.wear.compose.remote.material3.previews.RemoteTitleCardWithTitleTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RemoteTitleCardTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)

    private val creationDisplayInfo =
        RemoteCreationDisplayInfo(
            500,
            500,
            ApplicationProvider.getApplicationContext<Context>().resources.displayMetrics.densityDpi,
        )

    @Test
    fun title_card_default() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            RemoteTitleCardDefault()
        }
    }

    @Test
    fun title_card_rtl() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            creationDisplayInfo = creationDisplayInfo,
            layoutDirection = LayoutDirection.Rtl,
        ) {
            RemoteTitleCardWithTitleSubtitle()
        }
    }

    @Test
    fun title_card_with_title_subtitle() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            RemoteTitleCardWithTitleSubtitle()
        }
    }

    @Test
    fun title_card_with_title_time() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            RemoteTitleCardWithTitleTime()
        }
    }

    @Test
    fun title_card_dynamic_color() {
        val colorOverrides = buildObjectIntMap {
            put("WearM3.primary", Color(0xFFB8D0A0).toArgb())
            put("WearM3.onPrimary", Color(0xFF24361A).toArgb())
            put("WearM3.surfaceContainer", Color(0xFF1C1D1A).toArgb())
            put("WearM3.onSurface", Color(0xFFE2E3DC).toArgb())
        }
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            creationDisplayInfo = creationDisplayInfo,
            backgroundColor = Color.Black,
            colorOverrides = colorOverrides,
        ) {
            Center(RemoteModifier.fillMaxSize()) { RemoteTitleCardDefault() }
        }
    }

    @Composable
    @RemoteComposable
    private fun Center(
        modifier: RemoteModifier,
        content: @Composable @RemoteComposable () -> Unit,
    ) {
        RemoteBox(modifier, contentAlignment = RemoteAlignment.Center, content = content)
    }
}
