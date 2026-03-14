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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.material3.Text
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Screenshot test for comparing [RemoteText] to Compose [Text] with different font scales. */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteTextFontScaleComparisonTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo =
        CreationDisplayInfo(500, 500, context.resources.displayMetrics.densityDpi)

    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            profile = TestProfiles.androidXWithCoreText,
        )

    private val size1 = 12
    private val size2 = 16
    private val size3 = 30
    private val size4 = 50
    private val text = "HH"

    @Test fun textComparison_fontScale_0_94() = runFontScaleTest(0.94f)

    @Test fun textComparison_fontScale_1_0() = runFontScaleTest(1.0f)

    @Test fun textComparison_fontScale_1_06() = runFontScaleTest(1.06f)

    @Test fun textComparison_fontScale_1_12() = runFontScaleTest(1.12f)

    @Test fun textComparison_fontScale_1_18() = runFontScaleTest(1.18f)

    @Test fun textComparison_fontScale_1_24() = runFontScaleTest(1.24f)

    private fun runFontScaleTest(fontScale: Float) {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = creationDisplayInfo,
            backgroundColor = Color.Black,
            outerContent = { modifier, remoteDocs ->
                OuterContent(modifier, fontScale) {
                    ComposeText()
                    Spacer(modifier = Modifier.width(4.dp).fillMaxHeight().background(Color.Red))
                    Box(modifier = Modifier.weight(1f)) { remoteDocs() }
                }
            },
        ) {
            RCText(fontScale)
        }
    }

    @Composable
    private fun OuterContent(
        modifier: Modifier,
        fontScale: Float,
        content: @Composable RowScope.() -> Unit,
    ) {
        Column(
            modifier = modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "Font Scale: $fontScale", color = Color.White)
            Row(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                Text(text = "Compose", color = Color.White)
                Text(text = "RC", color = Color.White)
            }
            CompositionLocalProvider(
                LocalDensity provides
                    Density(density = LocalDensity.current.density, fontScale = fontScale)
            ) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) { content() }
            }
        }
    }

    @Composable
    @RemoteComposable
    private fun RCText(fontScale: Float) {
        RemoteColumn(
            RemoteModifier.fillMaxSize(),
            horizontalAlignment = RemoteAlignment.Start,
            verticalArrangement = RemoteArrangement.Center,
        ) {
            val state = LocalRemoteComposeCreationState.current
            val density = LocalDensity.current
            state.remoteDensity =
                RemoteDensity(density = density.density.rf, fontScale = fontScale.rf)

            RemoteText(text = text.rs, fontSize = size1.rsp, color = RemoteColor(Color.White))
            RemoteText(text = text.rs, fontSize = size2.rsp, color = RemoteColor(Color.White))
            RemoteText(text = text.rs, fontSize = size3.rsp, color = RemoteColor(Color.White))
            RemoteText(text = text.rs, fontSize = size4.rsp, color = RemoteColor(Color.White))
        }
    }

    @Composable
    private fun RowScope.ComposeText() {
        Column(
            modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
            horizontalAlignment = Alignment.End,
        ) {
            Text(text = text, fontSize = size1.sp, color = Color.White)
            Text(text = text, fontSize = size2.sp, color = Color.White)
            Text(text = text, fontSize = size3.sp, color = Color.White)
            Text(text = text, fontSize = size4.sp, color = Color.White)
        }
    }
}
