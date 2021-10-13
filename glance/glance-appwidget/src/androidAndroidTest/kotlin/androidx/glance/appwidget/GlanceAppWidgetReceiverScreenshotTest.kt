/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget

import androidx.compose.runtime.Composable
import androidx.glance.Modifier
import androidx.glance.background
import androidx.glance.appwidget.layout.CheckBox
import androidx.glance.appwidget.layout.Switch
import androidx.glance.appwidget.test.R
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Text
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.Color
import androidx.glance.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

@SdkSuppress(minSdkVersion = 29)
@MediumTest
class GlanceAppWidgetReceiverScreenshotTest {
    private val mScreenshotRule = screenshotRule()
    private val mHostRule = AppWidgetHostRule()

    @Rule
    @JvmField
    val mRule: TestRule = RuleChain.outerRule(mHostRule).around(mScreenshotRule)
        .around(WithRtlRule)
        .around(WithNightModeRule)

    @Test
    fun createSimpleAppWidget() {
        TestGlanceAppWidget.uiDefinition = {
            Text(
                "text",
                style = TextStyle(
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                )
            )
        }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "simpleAppWidget")
    }

    @Test
    fun createCheckBoxAppWidget() {
        TestGlanceAppWidget.uiDefinition = {
            Column {
                CheckBox(
                    checked = true,
                    text = "Hello Checked Checkbox",
                    textStyle = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Normal,
                    )
                )

                CheckBox(
                    checked = false,
                    text = "Hello Unchecked Checkbox",
                    textStyle = TextStyle(
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                    )
                )
            }
        }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "checkBoxWidget")
    }

    @Test
    fun createCheckSwitchAppWidget() {
        TestGlanceAppWidget.uiDefinition = {
            Column {
                Switch(
                    checked = true,
                    text = "Hello Checked Switch",
                    textStyle = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Normal,
                    )
                )

                Switch(
                    checked = false,
                    text = "Hello Unchecked Switch",
                    textStyle = TextStyle(
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                    )
                )
            }
        }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "switchWidget")
    }

    @Test
    fun createRowWidget() {
        TestGlanceAppWidget.uiDefinition = { RowTest() }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "rowWidget")
    }

    @WithRtl
    @Test
    fun createRowWidget_rtl() {
        TestGlanceAppWidget.uiDefinition = { RowTest() }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "rowWidget_rtl")
    }

    @Test
    fun checkTextAlignment() {
        TestGlanceAppWidget.uiDefinition = { TextAlignmentTest() }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "textAlignment")
    }

    @WithRtl
    @Test
    fun checkTextAlignment_rtl() {
        TestGlanceAppWidget.uiDefinition = { TextAlignmentTest() }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "textAlignment_rtl")
    }

    @Test
    fun checkBackgroundColor_light() {
        TestGlanceAppWidget.uiDefinition = { BackgroundTest() }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "backgroundColor")
    }

    @Test
    @WithNightMode
    fun checkBackgroundColor_dark() {
        TestGlanceAppWidget.uiDefinition = { BackgroundTest() }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "backgroundColor_dark")
    }
}

@Composable
private fun TextAlignmentTest() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Center",
            style = TextStyle(textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Left",
            style = TextStyle(textAlign = TextAlign.Left),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Right",
            style = TextStyle(textAlign = TextAlign.Right),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Start",
            style = TextStyle(textAlign = TextAlign.Start),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "End",
            style = TextStyle(textAlign = TextAlign.End),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RowTest() {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Start",
            style = TextStyle(textAlign = TextAlign.Start),
            modifier = Modifier.defaultWeight()
        )
        Text(
            "Center",
            style = TextStyle(textAlign = TextAlign.Center),
            modifier = Modifier.defaultWeight()
        )
        Text(
            "End",
            style = TextStyle(textAlign = TextAlign.End),
            modifier = Modifier.defaultWeight()
        )
    }
}

@Composable
private fun BackgroundTest() {
    Column(modifier = Modifier.background(R.color.background_color)) {
        Text(
            "100x50 and cyan",
            modifier = Modifier.width(100.dp).height(50.dp).background(Color.Cyan)
        )
        Text(
            "Transparent background",
            modifier = Modifier.height(50.dp).background(Color.Transparent)
        )
        Text(
            "wrapx30 and red (light), yellow (dark)",
            modifier = Modifier.height(30.dp).background(day = Color.Red, night = Color.Yellow)
        )
        Text("Below this should be 4 color boxes")
        Row(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier =
                Modifier
                    .width(32.dp)
                    .height(32.dp)
                    .background(day = Color.Black, night = Color.White)
            ) {}
            val colors = listOf(Color.Red, Color.Green, Color.Blue)
            repeat(3) {
                Box(modifier = Modifier.width(8.dp).height(1.dp)) {}
                Box(
                    modifier = Modifier.width(32.dp).height(32.dp).background(colors[it])
                ) {}
            }
        }
    }
}
