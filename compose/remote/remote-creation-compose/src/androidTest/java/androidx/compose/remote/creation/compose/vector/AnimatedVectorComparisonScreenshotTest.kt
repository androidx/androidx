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

package androidx.compose.remote.creation.compose.vector

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.heightDp
import androidx.compose.remote.creation.compose.capture.rememberRemoteDocument
import androidx.compose.remote.creation.compose.capture.widthDp
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.test.R
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RemoteComposePlayerFlags
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.remote.player.compose.test.utils.GoldenScreenshotNameTestRule
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalRemotePlayerApi::class)
class AnimatedVectorComparisonScreenshotTest {
    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @get:Rule val goldenScreenshotNameTestRule = GoldenScreenshotNameTestRule()

    @Before
    fun setUp() {
        RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = true
    }

    @Test
    fun smallAnimatedVector_sideBySide() =
        runAvdComparisonTest(
            resId = R.drawable.small_animated_vector,
            progressValues = listOf(0f, 0.5f, 1f),
        )

    @Test
    fun targetDuplicated_sideBySide() =
        runAvdComparisonTest(
            resId = R.drawable.target_duplicated,
            progressValues = listOf(0f, 0.5f, 1f),
        )

    @Test
    fun avdComplex_sideBySide() =
        runAvdComparisonTest(
            resId = R.drawable.avd_complex,
            progressValues = listOf(0f, 0.5f, 1f),
        )

    @Test
    fun icHourglassAnimated_sideBySide() =
        runAvdComparisonTest(
            resId = R.drawable.ic_hourglass_animated,
            progressValues = listOf(0f, 0.5f, 1f),
        )

    @Test
    fun injectedProgress_sideBySide() =
        runAvdComparisonTest(
            resId = R.drawable.ic_hourglass_animated,
            progressValues = listOf(0.25f, 0.75f),
        )

    @Test
    fun avdHeartFill_sideBySide() =
        runAvdComparisonTest(
            resId = R.drawable.avd_heart_fill,
            progressValues = listOf(0f, 0.5f, 1f),
            cardBackground = Color(0xFF1E1E1E),
        )

    @Test
    fun avdHeartEmpty_sideBySide() =
        runAvdComparisonTest(
            resId = R.drawable.avd_heart_empty,
            progressValues = listOf(0f, 0.5f, 1f),
            cardBackground = Color(0xFF1E1E1E),
        )

    @Test
    fun openOnPhoneAnimation_sideBySide() =
        runAvdComparisonTest(
            resId = R.drawable.open_on_phone_animation,
            progressValues = listOf(0f, 0.5f, 1f),
            cardBackground = Color(0xFF1E1E1E),
        )

    @Test
    fun wearOneHandedGesture_sideBySide() =
        runAvdComparisonTest(
            resId = R.drawable.wear_one_handed_gesture_primary_indicator_animation,
            progressValues = listOf(0f, 0.2f, 0.7f),
        )

    @Test
    fun threeSegments_sideBySide() =
        runAvdComparisonTest(
            resId = R.drawable.avd_three_segments,
            progressValues = listOf(0f, 0.5f, 1f),
        )

    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
    private fun runAvdComparisonTest(
        @DrawableRes resId: Int,
        progressValues: List<Float>,
        cardBackground: Color = Color.White,
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val avd = RemoteAnimatedVector.fromXml(context.resources, resId)
        val displayInfo = getScaledDisplayInfo(400, 800, 240)

        composeTestRule.setContent {
            val remoteDoc =
                rememberRemoteDocument(creationDisplayInfo = displayInfo) {
                    RemoteRow(
                        modifier = RemoteModifier.fillMaxSize().padding(16.rdp),
                        horizontalArrangement = RemoteArrangement.spacedBy(16.rdp),
                    ) {
                        // Left Column: Empty space matching Compose column
                        RemoteBox(modifier = RemoteModifier.weight(1f))

                        // Right Column: Remote Compose content
                        RemoteColumn(
                            modifier = RemoteModifier.weight(1f),
                            verticalArrangement = RemoteArrangement.spacedBy(16.rdp),
                            horizontalAlignment = RemoteAlignment.CenterHorizontally,
                        ) {
                            RemoteText(
                                text = "Remote".rs,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.rsp,
                                color = Color.Black.rc,
                            )
                            for (progress in progressValues) {
                                RemoteText(
                                    text = "p = $progress".rs,
                                    fontSize = 12.rsp,
                                    color = Color.DarkGray.rc,
                                )
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(80.rdp)
                                            .background(cardBackground.rc)
                                            .padding(4.rdp)
                                ) {
                                    val painter =
                                        rememberRemoteAnimatedVectorPainter(
                                            animatedVector = avd,
                                            progress = progress.rf,
                                        )
                                    RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                                        with(painter) { onDraw() }
                                    }
                                }
                            }
                        }
                    }
                }

            Box(
                modifier =
                    Modifier.size(displayInfo.widthDp, displayInfo.heightDp)
                        .background(Color(0xFFE8E8E8))
                        .testTag(ROOT_TEST_TAG)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Left Column: Standard Compose
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Compose",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black,
                        )
                        for (progress in progressValues) {
                            Text(
                                text = "p = $progress",
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                            )
                            val snapshot = avd.evaluate(progress)
                            val painter = rememberVectorPainter(snapshot.toImageVector())
                            Box(
                                modifier =
                                    Modifier.size(80.dp).background(cardBackground).padding(4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    painter = painter,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }

                    // Right Column: Space holder for Remote Compose
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Render Remote Compose using Embedded Player (RcPlayer)
                remoteDoc.value?.let { doc ->
                    RcPlayer(
                        document = doc,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        val goldenName = goldenScreenshotNameTestRule.getGoldenScreenshotName().getName()
        composeTestRule
            .onNodeWithTag(ROOT_TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }

    companion object {
        const val ROOT_TEST_TAG = "ROOT_TEST_TAG"
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
