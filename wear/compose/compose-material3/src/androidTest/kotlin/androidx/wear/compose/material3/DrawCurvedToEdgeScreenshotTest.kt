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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.wear.compose.foundation.AnchorType
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class DrawCurvedToEdgeScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun drawCurvedToEdge_scrollable_content_max_sweep_angle() = verifyScreenshot {
        // Default sweep is DrawCurvedToEdgeDefaults.ScrollableContentMaxSweepAngle (70 degrees)
        Text(
            "Long text that will be cut for sure.",
            Modifier.drawCurvedToEdge(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    @Test
    fun drawCurvedToEdge_static_content_max_sweep_angle() = verifyScreenshot {
        // Static content can use DrawCurvedToEdgeDefaults.StaticContentMaxSweepAngle (120 degrees)
        Text(
            "Long text that will be cut for sure.",
            Modifier.drawCurvedToEdge(DrawCurvedToEdgeDefaults.StaticContentMaxSweepAngle),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    @Test
    fun drawCurvedToEdge_curved_sweep_angle() = verifyScreenshot {
        Box(Modifier.drawCurvedToEdge(360f)) {
            Text("A long piece of example text. That's really long indeed.")
        }
    }

    @Test
    fun drawCurvedToEdge_curved_sweep_angle_bottom_overflow() = verifyScreenshot {
        Box(Modifier.drawCurvedToEdge(270f, AngularDirection.CounterClockwise, 90f)) {
            Text(
                "A long piece of example text. That's really long indeed. This one's also at the bottom",
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )
        }
    }

    @Test
    fun drawCurvedToEdge_anchor_type_start() = verifyScreenshot {
        // Span > 35 degrees within the 70-degree default sweep so Start/End half-sweep clipping
        // would be caught.
        Box(Modifier.drawCurvedToEdge(anchorType = AnchorType.Start)) {
            Text("Longer Anchor Text", maxLines = 1)
        }
    }

    @Test
    fun drawCurvedToEdge_anchor_type_center() = verifyScreenshot {
        Box(Modifier.drawCurvedToEdge(anchorType = AnchorType.Center)) { Text("Anchor Type") }
    }

    @Test
    fun drawCurvedToEdge_anchor_type_end() = verifyScreenshot {
        // Span > 35 degrees within the 70-degree default sweep so Start/End half-sweep clipping
        // would be caught.
        Box(Modifier.drawCurvedToEdge(anchorType = AnchorType.End)) {
            Text("Longer Anchor Text", maxLines = 1)
        }
    }

    @Test
    fun drawCurvedToEdge_anchor_clockwise() = verifyScreenshot {
        listOf(0f, 90f, 180f, 270f).forEachIndexed { _, anchorVal ->
            Box(Modifier.drawCurvedToEdge(anchor = anchorVal)) {
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
    fun drawCurvedToEdge_anchor_counterclockwise() = verifyScreenshot {
        listOf(0f, 90f, 180f, 270f).forEachIndexed { _, anchorVal ->
            Box(
                Modifier.drawCurvedToEdge(
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
    fun drawCurvedToEdge_overflow_angular_direction() = verifyScreenshot {
        Box(Modifier.drawCurvedToEdge(30f, AngularDirection.Clockwise)) {
            Row(Modifier.background(Color.Green)) {
                Text("Top Text Goes Here", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Box(Modifier.drawCurvedToEdge(30f, AngularDirection.CounterClockwise, 90f)) {
            Row(Modifier.background(Color.Green)) {
                Text("Bottom Text Goes Here", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }

    @Test
    fun drawCurvedToEdge_angular_direction() = verifyScreenshot {
        Box(Modifier.drawCurvedToEdge(180f, AngularDirection.Clockwise)) {
            Row(Modifier.background(Color.Green)) { Text("Top Text Goes Here") }
        }

        Box(Modifier.drawCurvedToEdge(180f, AngularDirection.CounterClockwise, 90f)) {
            Row(Modifier.background(Color.Green)) { Text("Bottom Text Goes Here") }
        }
    }

    @Test
    fun drawCurvedToEdge_arabic() = verifyScreenshot {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text("مرحبا 👋 بالعالم 🌏!", Modifier.drawCurvedToEdge())
            Text(
                "مرحبا 👋 بالعالم 🌏!",
                Modifier.drawCurvedToEdge(
                    angularDirection = AngularDirection.CounterClockwise,
                    anchor = 90f,
                ),
            )
        }
    }

    @Test
    fun drawCurvedToEdge_bounds() = verifyScreenshot {
        Box(Modifier.background(Color.Magenta).drawCurvedToEdge(360f).background(Color.Cyan)) {
            Text("A long piece of example text. That's really long indeed.")
        }
    }

    @Test
    fun drawCurvedToEdge_row() = verifyScreenshot {
        Row(Modifier.drawCurvedToEdge(360f)) {
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
    fun drawCurvedToEdge_column() = verifyScreenshot {
        Column(Modifier.drawCurvedToEdge(360f)) {
            Text("A long piece of example text. That's really long indeed.")
            Text("And another long piece of example text.")
        }
    }

    @Test
    fun drawCurvedToEdge_padded_parent() = verifyScreenshot {
        // Padding the parent draws the arc on a smaller, concentric circle (the red border) at full
        // size, unlike padding the curved content itself.
        Box(
            Modifier.fillMaxSize()
                .background(Color.Red, CircleShape)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background, CircleShape)
        ) {
            Text(
                "Padded parent",
                Modifier.drawCurvedToEdge(DrawCurvedToEdgeDefaults.StaticContentMaxSweepAngle),
            )
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
