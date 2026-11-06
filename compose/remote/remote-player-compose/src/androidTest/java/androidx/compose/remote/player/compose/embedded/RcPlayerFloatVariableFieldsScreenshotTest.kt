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
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxHeight
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.RemoteEmbeddedScreenshotTestRule
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Embedded player ([RcPlayer]) screenshots of layout and text fields backed by RemoteFloat
 * variables, which the player resolves rather than reading as NaN.
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RcPlayerFloatVariableFieldsScreenshotTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val remoteComposeTestRule =
        RemoteEmbeddedScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = context,
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    @Test
    fun variableWeights() {
        // Rows split 1:2:3 and columns 3:1, with every weight a named RemoteFloat variable.
        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo = textOnCircleDisplayInfo(context),
            playComposableWrapper = ComposableWrappers.blackBackground,
        ) {
            val one = remember { createNamedRemoteFloat("weight_one", 1f) }
            val two = remember { createNamedRemoteFloat("weight_two", 2f) }
            val three = remember { createNamedRemoteFloat("weight_three", 3f) }
            RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
                RemoteRow(modifier = RemoteModifier.fillMaxWidth().weight(three)) {
                    RemoteBox(RemoteModifier.weight(one).fillMaxHeight().background(Color.Red.rc))
                    RemoteBox(RemoteModifier.weight(two).fillMaxHeight().background(Color.Green.rc))
                    RemoteBox(
                        RemoteModifier.weight(three).fillMaxHeight().background(Color.Blue.rc)
                    )
                }
                RemoteBox(RemoteModifier.fillMaxWidth().weight(one).background(Color.Yellow.rc))
            }
        }
    }
}
