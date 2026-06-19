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

package androidx.compose.remote.creation.compose.modifier

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteSpacer
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class BorderComparisonScreenshotTest {
    @get:Rule
    val composeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )

    @Test
    fun borderComparison() =
        composeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo = getScaledDisplayInfo(400, 800, 240),
            playComposableWrapper = { content ->
                Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Left Column: Standard Compose
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "Compose",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black,
                            )

                            // Case 1: Rectangle Border
                            Box(
                                modifier =
                                    Modifier.size(80.dp)
                                        .background(Color.Green)
                                        .border(15.dp, Color.Red)
                            )

                            // Case 2: Rounded Corner Border
                            Box(
                                modifier =
                                    Modifier.size(80.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Green)
                                        .border(15.dp, Color.Red, RoundedCornerShape(12.dp))
                            )

                            // Case 3: Circle Border
                            Box(
                                modifier =
                                    Modifier.size(80.dp)
                                        .clip(CircleShape)
                                        .background(Color.Green)
                                        .border(15.dp, Color.Red, CircleShape)
                            )

                            // Case 4: Filled Border (border width >= size / 2)
                            Box(
                                modifier =
                                    Modifier.size(80.dp)
                                        .background(Color.Green)
                                        .border(50.dp, Color.Red)
                            )
                        }

                        // Right Column Space holder for Remote Compose
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Render Remote Compose on top (its left half is empty/transparent spacer)
                    content()
                }
            },
        ) {
            RemoteRow(
                modifier = RemoteModifier.fillMaxSize().padding(16.rdp),
                horizontalArrangement = RemoteArrangement.spacedBy(16.rdp),
            ) {
                // Left Column: Empty space matching Compose column
                RemoteSpacer(modifier = RemoteModifier.weight(1f))

                // Right Column: Remote Compose content
                RemoteColumn(
                    modifier = RemoteModifier.weight(1f),
                    verticalArrangement = RemoteArrangement.spacedBy(20.rdp),
                    horizontalAlignment = RemoteAlignment.CenterHorizontally,
                ) {
                    RemoteText(
                        text = "Remote",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.rsp,
                        color = Color.Black.rc,
                    )

                    // Case 1: Rectangle Border
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(80.rdp)
                                .background(Color.Green)
                                .border(15.rdp, Color.Red.rc)
                    )

                    // Case 2: Rounded Corner Border
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(80.rdp)
                                .clip(RemoteRoundedCornerShape(12.rdp))
                                .background(Color.Green)
                                .border(15.rdp, Color.Red.rc, RemoteRoundedCornerShape(12.rdp))
                    )

                    // Case 3: Circle Border
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(80.rdp)
                                .clip(RemoteCircleShape)
                                .background(Color.Green)
                                .border(15.rdp, Color.Red.rc, RemoteCircleShape)
                    )

                    // Case 4: Filled Border (border width >= size / 2)
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(80.rdp)
                                .background(Color.Green)
                                .border(50.rdp, Color.Red.rc)
                    )
                }
            }
        }

    private fun getScaledDisplayInfo(
        originalWidthPx: Int,
        originalHeightPx: Int,
        originalDensity: Int,
    ): RemoteCreationDisplayInfo {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val hostDensityDpi = context.resources.displayMetrics.densityDpi
        val scale = hostDensityDpi.toFloat() / originalDensity.toFloat()
        val widthPx = (originalWidthPx * scale).toInt()
        val heightPx = (originalHeightPx * scale).toInt()
        return RemoteCreationDisplayInfo(widthPx, heightPx, hostDensityDpi)
    }
}
