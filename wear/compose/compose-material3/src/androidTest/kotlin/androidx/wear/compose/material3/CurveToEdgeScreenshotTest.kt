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
package androidx.wear.compose.material3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class CurveToEdgeTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun curveToEdge_curved_sweep_angle() = verifyScreenshot {
        Box(Modifier.curveToEdge(360f)) {
            Text("A long piece of example text. That's really long indeed.")
        }
    }

    @Test
    fun curveToEdge_curved_sweep_angle_bottom_overflow() = verifyScreenshot {
        Box(Modifier.curveToEdge(270f, AngularDirection.CounterClockwise, 90f)) {
            Text(
                "A long piece of example text. That's really long indeed. This one's also at the bottom",
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )
        }
    }

    @Test
    fun curveToEdge_anchor_type_start() = verifyScreenshot {
        Box(Modifier.curveToEdge(anchorType = AngularAnchorType.Start)) { Text("Anchor Type") }
    }

    @Test
    fun curveToEdge_anchor_type_center() = verifyScreenshot {
        Box(Modifier.curveToEdge(anchorType = AngularAnchorType.Center)) { Text("Anchor Type") }
    }

    @Test
    fun curveToEdge_anchor_type_end() = verifyScreenshot {
        Box(Modifier.curveToEdge(anchorType = AngularAnchorType.End)) { Text("Anchor Type") }
    }

    @Test
    fun curveToEdge_anchor_clockwise() = verifyScreenshot {
        listOf(0f, 90f, 180f, 270f).forEachIndexed { _, anchorVal ->
            Box(Modifier.curveToEdge(anchor = anchorVal)) {
                Column(
                    Modifier.background(
                        brush = Brush.sweepGradient(listOf(Color.Red, Color.Green, Color.Blue))
                    )
                ) {
                    Text("$anchorVal Anchor")
                }
            }
        }
    }

    @Test
    fun curveToEdge_anchor_counterclockwise() = verifyScreenshot {
        listOf(0f, 90f, 180f, 270f).forEachIndexed { _, anchorVal ->
            Box(
                Modifier.curveToEdge(
                    angularDirection = AngularDirection.CounterClockwise,
                    anchor = anchorVal,
                )
            ) {
                Column(
                    Modifier.background(
                        brush = Brush.sweepGradient(listOf(Color.Red, Color.Green, Color.Blue))
                    )
                ) {
                    Text("$anchorVal Anchor")
                }
            }
        }
    }

    @Test
    fun curveToEdge_overflow_angular_direction() = verifyScreenshot {
        Box(Modifier.curveToEdge(30f, AngularDirection.Clockwise)) {
            Row(Modifier.background(Color.Green)) {
                Text("Top Text Goes Here", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Box(Modifier.curveToEdge(30f, AngularDirection.CounterClockwise, 90f)) {
            Row(Modifier.background(Color.Green)) {
                Text("Bottom Text Goes Here", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }

    @Test
    fun curveToEdge_angular_direction() = verifyScreenshot {
        Box(Modifier.curveToEdge(180f, AngularDirection.Clockwise)) {
            Row(Modifier.background(Color.Green)) { Text("Top Text Goes Here") }
        }

        Box(Modifier.curveToEdge(180f, AngularDirection.CounterClockwise, 90f)) {
            Row(Modifier.background(Color.Green)) { Text("Bottom Text Goes Here") }
        }
    }

    @Test
    fun curveToEdge_arabic() = verifyScreenshot {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text("مرحبا 👋 بالعالم 🌏!", Modifier.curveToEdge())
            Text(
                "مرحبا 👋 بالعالم 🌏!",
                Modifier.curveToEdge(
                    angularDirection = AngularDirection.CounterClockwise,
                    anchor = 90f,
                ),
            )
        }
    }

    @Test
    fun curveToEdge_bounds() = verifyScreenshot {
        Box(Modifier.background(Color.Magenta).curveToEdge(360f).background(Color.Cyan)) {
            Text("A long piece of example text. That's really long indeed.")
        }
    }

    @Test
    fun curveToEdge_row() = verifyScreenshot {
        Row(Modifier.curveToEdge(360f)) {
            Text("Text")
            Box(
                Modifier.size(20.dp)
                    .background(Brush.radialGradient(listOf(Color.Red, Color.Green)))
            ) {}
            Column {
                Text("Small Text", fontSize = 7.sp)
                Text("In a column", fontSize = 7.sp)
                Box(
                    Modifier.size(10.dp)
                        .background(Brush.radialGradient(listOf(Color.Blue, Color.Cyan)))
                ) {}
            }
        }
    }

    @Test
    fun curveToEdge_column() = verifyScreenshot {
        Column(Modifier.curveToEdge(360f)) {
            Text("A long piece of example text. That's really long indeed.")
            Text("And another long piece of example text.")
        }
    }

    private fun verifyScreenshot(content: @Composable (BoxScope.() -> Unit)) {
        rule.setContent {
            Box(
                Modifier.size(200.dp)
                    .background(MaterialTheme.colorScheme.background)
                    .testTag(TEST_TAG),
                content = content,
            )
        }

        rule.verifyScreenshot(testName, screenshotRule)
    }
}
