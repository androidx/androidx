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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.capture.path
import androidx.compose.remote.creation.compose.capture.rememberRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteFlowRow
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.offset
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.vector.draw
import androidx.compose.remote.creation.compose.vector.painterRemoteVector
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.test.R
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
class RcPlayerScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    fun basicLayout() {
        rule.setContent {
            val document =
                rememberRemoteDocument(profile = TEST_PROFILE) {
                    RemoteColumn(
                        modifier = RemoteModifier.size(200.rdp).border(1.rdp, Color.Magenta.rc)
                    ) {
                        RemoteColumn(
                            modifier = RemoteModifier.fillMaxSize().border(1.rdp, Color.Blue.rc)
                        ) {
                            RemoteText("Column 1".rs, color = Color.Green.rc)
                            RemoteText("Column 2".rs)
                        }
                        RemoteColumn(
                            modifier = RemoteModifier.fillMaxSize().border(1.rdp, Color.Blue.rc)
                        ) {
                            RemoteText("Row 1".rs)
                            RemoteText("Row 2".rs)
                        }
                    }
                }

            Box(modifier = Modifier.size(200.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule
            .onRoot()
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "RcPlayerScreenshotTest_basicLayout")
    }

    @Test
    fun complexLayout() {
        rule.setContent {
            val document =
                rememberRemoteDocument(profile = TEST_PROFILE) {
                    RemoteColumn(
                        modifier = RemoteModifier.fillMaxSize().background(Color.LightGray.rc)
                    ) {
                        RemoteFlowRow(
                            modifier = RemoteModifier.padding(8.rdp).border(2.rdp, Color.Black.rc)
                        ) {
                            RemoteText(
                                "Flow 1".rs,
                                modifier = RemoteModifier.background(Color.Red.rc).padding(4.rdp),
                            )
                            RemoteText(
                                "Flow 2".rs,
                                modifier = RemoteModifier.background(Color.Green.rc).padding(4.rdp),
                            )
                            RemoteText(
                                "Flow 3".rs,
                                modifier = RemoteModifier.background(Color.Blue.rc).padding(4.rdp),
                            )
                        }
                        RemoteRow(modifier = RemoteModifier.padding(16.rdp).clip()) {
                            RemoteBox(
                                modifier = RemoteModifier.size(50.rdp).background(Color.Yellow.rc)
                            )
                            RemoteBox(
                                modifier =
                                    RemoteModifier.size(50.rdp)
                                        .offset(10.rdp, 10.rdp)
                                        .background(Color.Cyan.rc)
                            )
                        }
                    }
                }

            Box(modifier = Modifier.size(200.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule
            .onRoot()
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "RcPlayerScreenshotTest_complexLayout")
    }

    @Test
    fun vectorPainter_brushAndAlpha() {
        val vector =
            RemoteImageVector.Builder(
                    viewportWidth = 100f.rf,
                    viewportHeight = 100f.rf,
                    tintColor = Color.Unspecified.rc,
                )
                .path(
                    fill = SolidColor(Color.Red),
                    fillAlpha = 0.75f.rf,
                    stroke = SolidColor(Color.Green),
                    strokeAlpha = 0.5f.rf,
                    strokeLineWidth = 6f.rf,
                ) {
                    moveTo(10f.rf, 10f.rf)
                    lineTo(90f.rf, 10f.rf)
                    lineTo(90f.rf, 90f.rf)
                    lineTo(10f.rf, 90f.rf)
                    close()
                }
                .build()

        rule.setContent {
            val document =
                rememberRemoteDocument(profile = TEST_PROFILE) {
                    val painter = painterRemoteVector(vector)
                    RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                        with(painter) { onDraw() }
                    }
                }

            Box(modifier = Modifier.size(100.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule
            .onRoot()
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "RcPlayerScreenshotTest_vectorPainter_brushAndAlpha",
            )
    }

    @Test
    fun vectorPainter_pathFillTypeEvenOdd() {
        val vector =
            RemoteImageVector.Builder(
                    viewportWidth = 100f.rf,
                    viewportHeight = 100f.rf,
                    tintColor = Color.Unspecified.rc,
                )
                .path(
                    fill = SolidColor(Color.Red),
                    pathFillType = PathFillType.EvenOdd,
                ) {
                    moveTo(10f.rf, 10f.rf)
                    lineTo(90f.rf, 10f.rf)
                    lineTo(90f.rf, 90f.rf)
                    lineTo(10f.rf, 90f.rf)
                    close()
                    moveTo(30f.rf, 30f.rf)
                    lineTo(70f.rf, 30f.rf)
                    lineTo(70f.rf, 70f.rf)
                    lineTo(30f.rf, 70f.rf)
                    close()
                }
                .build()

        rule.setContent {
            val document =
                rememberRemoteDocument(profile = TEST_PROFILE) {
                    val painter = painterRemoteVector(vector)
                    RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                        with(painter) { onDraw() }
                    }
                }

            Box(modifier = Modifier.size(100.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule
            .onRoot()
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "RcPlayerScreenshotTest_vectorPainter_pathFillTypeEvenOdd",
            )
    }

    @Test
    fun vectorPainter_proceduralDraw() {
        val vector =
            RemoteImageVector.Builder(
                    viewportWidth = 100f.rf,
                    viewportHeight = 100f.rf,
                    tintColor = Color.Unspecified.rc,
                )
                .path(fill = SolidColor(Color.Blue)) {
                    moveTo(50f.rf, 10f.rf)
                    lineTo(90f.rf, 90f.rf)
                    lineTo(10f.rf, 90f.rf)
                    close()
                }
                .build()

        rule.setContent {
            val document =
                rememberRemoteDocument(profile = TEST_PROFILE) {
                    RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                        vector.draw(remoteCanvas)
                    }
                }

            Box(modifier = Modifier.size(100.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule
            .onRoot()
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "RcPlayerScreenshotTest_vectorPainter_proceduralDraw",
            )
    }

    @Test
    fun vectorDrawable_clipPath_androidx() {
        rule.setContent {
            val document =
                rememberRemoteDocument(profile = RcPlatformProfiles.ANDROIDX) {
                    val painter =
                        painterRemoteVector(
                            ImageVector.vectorResource(R.drawable.vector_icon_clip_path_1)
                        )
                    RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                        with(painter) { onDraw() }
                    }
                }

            Box(modifier = Modifier.size(100.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule
            .onRoot()
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "RcPlayerScreenshotTest_vectorDrawable_clipPath_androidx",
            )
    }

    @Test
    fun vectorDrawable_clipPath_wearWidgets() {
        rule.setContent {
            val document =
                rememberRemoteDocument(profile = RcPlatformProfiles.WEAR_WIDGETS) {
                    val painter =
                        painterRemoteVector(
                            ImageVector.vectorResource(R.drawable.vector_icon_clip_path_1)
                        )
                    RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                        with(painter) { onDraw() }
                    }
                }

            Box(modifier = Modifier.size(100.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule
            .onRoot()
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "RcPlayerScreenshotTest_vectorDrawable_clipPath_wearWidgets",
            )
    }

    @Test
    fun vectorDrawable_clipPath_comparison() {
        rule.setContent {
            Column(
                modifier = Modifier.background(Color.White).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Compose",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.width(80.dp),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "RC (AndroidX)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.width(80.dp),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "RC (without\npath_clip)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.width(80.dp),
                        textAlign = TextAlign.Center,
                    )
                }

                ComparisonRow(resId = R.drawable.vector_icon_clip_path_1)
                ComparisonRow(resId = R.drawable.vector_icon_clip_diamond_fill)
                ComparisonRow(resId = R.drawable.vector_icon_clip_diamond_stroke)
            }
        }

        rule
            .onRoot()
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "RcPlayerScreenshotTest_vectorDrawable_clipPath_comparison",
            )
    }

    @Composable
    private fun ComparisonRow(resId: Int) {
        val docAndroidx =
            rememberRemoteDocument(profile = RcPlatformProfiles.ANDROIDX) {
                val painter = painterRemoteVector(ImageVector.vectorResource(resId))
                RemoteCanvas(modifier = RemoteModifier.size(80.rdp)) {
                    with(painter) { onDraw() }
                }
            }

        val docWearWidgets =
            rememberRemoteDocument(profile = RcPlatformProfiles.WEAR_WIDGETS) {
                val painter = painterRemoteVector(ImageVector.vectorResource(resId))
                RemoteCanvas(modifier = RemoteModifier.size(80.rdp)) {
                    with(painter) { onDraw() }
                }
            }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier.size(80.dp).background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(resId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                modifier = Modifier.size(80.dp).background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center,
            ) {
                docAndroidx.value?.let { RcPlayer(document = it) }
            }
            Box(
                modifier = Modifier.size(80.dp).background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center,
            ) {
                docWearWidgets.value?.let { RcPlayer(document = it) }
            }
        }
    }
}
