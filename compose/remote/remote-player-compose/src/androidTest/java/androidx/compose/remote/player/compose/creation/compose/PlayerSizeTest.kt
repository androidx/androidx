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

package androidx.compose.remote.player.compose.creation.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RemoteComposePlayerFlags
import androidx.compose.remote.player.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalRemotePlayerApi::class)
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class PlayerSizeTest {
    @get:Rule
    val composeTestRule =
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)

    @Before
    fun setUp() {
        RemoteComposePlayerFlags.shouldPlayerWrapContentSize = false
    }

    @Test
    fun scenario_containerTakesScreenSize() {
        composeTestRule.runScreenshotTest(
            outerContent = Scenario.outerContent,
            content = Scenario.content,
        )
    }

    @Test
    fun scenario_flagTrue_containerTakesScreenSize() {
        RemoteComposePlayerFlags.shouldPlayerWrapContentSize = true
        composeTestRule.runScreenshotTest(
            outerContent = Scenario.outerContent,
            content = Scenario.content,
        )
    }

    @Ignore(
        "b/484308924: If RemoteComposeScreenshotTestRule.runScreenshotTest is called passing " +
            "a document with value zero to Header.FEATURE_PAINT_MEASURE, the test displays the " +
            "expected results, but it hangs."
    )
    @Test
    fun scenario_featurePaintMeasureDisabled_containerTakesScreenSize() {
        val profileFeaturePaintMeasureDisabled =
            Profile(
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX,
                AndroidxRcPlatformServices(),
            ) { _, profile, _ ->
                RemoteComposeWriterAndroid(
                    profile,
                    RemoteComposeWriter.hTag(Header.FEATURE_PAINT_MEASURE, 0),
                )
            }

        composeTestRule.runScreenshotTest(
            profile = profileFeaturePaintMeasureDisabled,
            outerContent = Scenario.outerContent,
            content = Scenario.content,
        )
    }

    @Ignore(
        "b/484308924: If RemoteComposeScreenshotTestRule.runScreenshotTest is called passing " +
            "a document with value zero to Header.FEATURE_PAINT_MEASURE, the test displays the " +
            "expected results, but it hangs."
    )
    @Test
    fun scenario_featurePaintMeasureDisabledAndBiggerSizeHeaders_containerTakesScreenSize() {
        val profileFeaturePaintMeasureDisabled =
            Profile(
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX,
                AndroidxRcPlatformServices(),
            ) { creationDisplayInfo, profile, _ ->
                RemoteComposeWriterAndroid(
                    profile,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.FEATURE_PAINT_MEASURE, 0),
                )
            }

        composeTestRule.runScreenshotTest(
            profile = profileFeaturePaintMeasureDisabled,
            outerContent = Scenario.outerContent,
            content = Scenario.content,
        )
    }

    @Test
    fun scenario_flagTrue_featurePaintMeasureDisabled_containerAdjustsSize() {
        RemoteComposePlayerFlags.shouldPlayerWrapContentSize = true
        val profileFeaturePaintMeasureDisabled =
            Profile(
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX,
                AndroidxRcPlatformServices(),
            ) { _, profile, _ ->
                RemoteComposeWriterAndroid(
                    profile,
                    RemoteComposeWriter.hTag(Header.FEATURE_PAINT_MEASURE, 0),
                )
            }

        composeTestRule.runScreenshotTest(
            profile = profileFeaturePaintMeasureDisabled,
            outerContent = Scenario.outerContent,
            content = Scenario.content,
        )
    }

    @Test
    fun scenario_flagTrue_featurePaintMeasureDisabledAndBiggerSizeHeaders_containerAdjustsSize() {
        RemoteComposePlayerFlags.shouldPlayerWrapContentSize = true
        val profileFeaturePaintMeasureDisabled =
            Profile(
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX,
                AndroidxRcPlatformServices(),
            ) { creationDisplayInfo, profile, _ ->
                RemoteComposeWriterAndroid(
                    profile,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.FEATURE_PAINT_MEASURE, 0),
                )
            }

        composeTestRule.runScreenshotTest(
            profile = profileFeaturePaintMeasureDisabled,
            outerContent = Scenario.outerContent,
            content = Scenario.content,
        )
    }

    /**
     * Represents scenario that the content has size defined and the container does not have a sized
     * defined.
     */
    private object Scenario {
        val outerContent:
            (@Composable
            (modifier: Modifier, content: @Composable @RemoteComposable () -> Unit) -> Unit) =
            { _, content ->
                Box(modifier = Modifier.background(Color.Gray).border(3.dp, Color.Red)) {
                    content()
                }
            }

        val content: @Composable @RemoteComposable () -> Unit = {
            RemoteBox(
                modifier =
                    RemoteModifier.size(400.rdp)
                        .background(Color.Blue.rc)
                        .padding(10.dp)
                        .border(1.rdp, Color.Green.rc)
            )
        }
    }
}
