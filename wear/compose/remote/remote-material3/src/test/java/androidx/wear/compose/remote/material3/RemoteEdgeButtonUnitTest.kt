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
@file:Suppress("RestrictedApiAndroidX")

package androidx.wear.compose.remote.material3

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.RcPlayerTestRule
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.remote.material3.util.TestProfiles
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w167dp-h167dp-xxhdpi")
class RemoteEdgeButtonUnitTest {
    @get:Rule val playerRule = RcPlayerTestRule()

    @get:Rule val captureRule = RemoteCaptureTestRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun calculateEdgeButtonContentWidth_matchesExpectedDpFormulaAcrossSizes() {
        val buttonWidthDp = 500f / 3f
        val sizes =
            listOf(
                RemoteEdgeButtonSize.ExtraSmall to 46f,
                RemoteEdgeButtonSize.Small to 56f,
                RemoteEdgeButtonSize.Medium to 70f,
                RemoteEdgeButtonSize.Large to 96f,
            )

        for ((size, heightDp) in sizes) {
            val actualDp =
                calculateEdgeButtonContentWidth(
                        actualButtonWidth = buttonWidthDp.rdp,
                        buttonHeight = size.maximumHeight,
                    )
                    .constantValue
            val expectedDp = expectedEdgeButtonContentWidthDp(buttonWidthDp, heightDp)
            assertWithMessage("Content width (dp) for height=$heightDp")
                .that(actualDp)
                .isWithin(0.01f)
                .of(expectedDp)
        }
    }

    @Test
    fun remoteEdgeButton_measuresNonZeroContentWidthAndTextAcrossSizes() = runTest {
        val density = 3f
        val widthPx = 500
        val heightPx = 260
        val displayInfo =
            RemoteCreationDisplayInfo(width = widthPx, height = heightPx, densityDpi = 480)

        val sizes =
            listOf(
                "ExtraSmall" to RemoteEdgeButtonSize.ExtraSmall,
                "Small" to RemoteEdgeButtonSize.Small,
                "Medium" to RemoteEdgeButtonSize.Medium,
                "Large" to RemoteEdgeButtonSize.Large,
            )

        val activeDocument = mutableStateOf<CoreDocument?>(null)
        playerRule.setContent {
            val currentDoc = activeDocument.value
            Box(
                modifier =
                    Modifier.size((widthPx / density).dp, (heightPx / density).dp).testTag("player")
            ) {
                if (currentDoc != null) {
                    key(currentDoc) { RcPlayer(document = currentDoc) }
                }
            }
        }

        for ((name, buttonSize) in sizes) {
            val label = "Confirm $name action with long label"
            val document =
                captureRule.captureDocument(
                    context = context,
                    creationDisplayInfo = displayInfo,
                    profile = TestProfiles.wearWidgetsWithCoreText,
                ) {
                    RemoteMaterialTheme {
                        RemoteEdgeButton(
                            onClick = RemoteAction.None,
                            buttonSize = buttonSize,
                            colors = RemoteEdgeButtonDefaults.filledTonalButtonColors(),
                            border =
                                RemoteBorderStroke(
                                    width = 1.rdp,
                                    color = RemoteMaterialTheme.colorScheme.outline,
                                ),
                        ) {
                            RemoteText(label.rs)
                        }
                    }
                }

            activeDocument.value = document
            playerRule.waitForIdle()
            playerRule.onNodeWithText(label).assertExists()

            val hierarchy = document.displayHierarchy()
            val expectedMaxWidthPx =
                expectedEdgeButtonContentWidthDp(
                    widthPx / density,
                    buttonSize.maximumHeight.constantValue,
                ) * density

            val rowDims =
                Regex("""ROW\s+\[[^\]]+]\s*=\s*\[[^,]+,\s*[^,]+,\s*([\d.]+),\s*([\d.]+)]""")
                    .find(hierarchy)
            assertNotNull(rowDims, "Expected ROW bounds in hierarchy for $name:\n$hierarchy")
            val rowWidthPx = rowDims.groupValues[1].toFloat()
            assertWithMessage("ROW width px for $name")
                .that(rowWidthPx)
                .isWithin(1.5f)
                .of(expectedMaxWidthPx)

            val coreTextDims =
                Regex("""CORE_TEXT\s+\[[^\]]+]\s*=\s*\[[^,]+,\s*[^,]+,\s*([\d.]+),\s*([\d.]+)]""")
                    .find(hierarchy)
            assertNotNull(
                coreTextDims,
                "Expected CORE_TEXT bounds in hierarchy for $name:\n$hierarchy",
            )
            val textWidthPx = coreTextDims.groupValues[1].toFloat()
            val textHeightPx = coreTextDims.groupValues[2].toFloat()
            assertWithMessage("CORE_TEXT width px for $name")
                .that(textWidthPx)
                .isWithin(1.5f)
                .of(expectedMaxWidthPx)
            assertWithMessage("CORE_TEXT height px for $name").that(textHeightPx).isGreaterThan(10f)
            assertThat(textWidthPx).isAtMost(rowWidthPx + 0.5f)
        }
    }

    private fun expectedEdgeButtonContentWidthDp(widthDp: Float, heightDp: Float): Float {
        val extraSmallHeight = 46f
        val bottomPadding = 3f
        val extraSmallEllipseHeight = 58f
        val buttonToEllipseRatio = 1.42f
        val targetSidePadding = 20f

        val finalFadeProgress = (1f - heightDp / extraSmallHeight).coerceIn(0f, 1f)
        val ellipseHeight =
            (extraSmallEllipseHeight + (heightDp - extraSmallHeight) * buttonToEllipseRatio) *
                (1f - finalFadeProgress) + heightDp * finalFadeProgress
        val screenRadius = widthDp / 2f
        val diff = max(screenRadius - bottomPadding - ellipseHeight / 2f, 0f)
        val localHalfWidth = sqrt(max(screenRadius * screenRadius - diff * diff, 0f))
        val r = heightDp - ellipseHeight / 2f
        return max(localHalfWidth - targetSidePadding - r, 0f) * 2f
    }
}
