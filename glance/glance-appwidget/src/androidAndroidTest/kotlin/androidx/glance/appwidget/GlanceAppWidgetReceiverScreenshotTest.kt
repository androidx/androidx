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

import android.app.Activity
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionLaunchActivity
import androidx.glance.appwidget.layout.CheckBox
import androidx.glance.appwidget.layout.Switch
import androidx.glance.appwidget.test.R
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Button
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.ImageProvider
import androidx.glance.layout.Row
import androidx.glance.layout.Text
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentSize
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
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
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Normal,
                    )
                )

                CheckBox(
                    checked = false,
                    text = "Hello Unchecked Checkbox",
                    style = TextStyle(
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
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Normal,
                    )
                )

                Switch(
                    checked = false,
                    text = "Hello Unchecked Switch",
                    style = TextStyle(
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

    @Test
    fun checkButtonTextAlignement() {
        TestGlanceAppWidget.uiDefinition = {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                    Button(
                        "Start",
                        onClick = actionLaunchActivity<Activity>(),
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        style = TextStyle(textAlign = TextAlign.Start)
                    )
                    Button(
                        "End",
                        onClick = actionLaunchActivity<Activity>(),
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        style = TextStyle(textAlign = TextAlign.End)
                    )
                }
                Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                    CheckBox(
                        checked = false,
                        text = "Start",
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        style = TextStyle(textAlign = TextAlign.Start)
                    )
                    CheckBox(
                        checked = true,
                        text = "End",
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        style = TextStyle(textAlign = TextAlign.End)
                    )
                }
                Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                    Switch(
                        checked = false,
                        text = "Start",
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        style = TextStyle(textAlign = TextAlign.Start)
                    )
                    Switch(
                        checked = true,
                        text = "End",
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        style = TextStyle(textAlign = TextAlign.End)
                    )
                }
            }
        }

        mHostRule.setSizes(DpSize(300.dp, 400.dp))
        mHostRule.startHost()

        Thread.sleep(5000)

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "button_text_align")
    }

    @Test
    fun checkFixTopLevelSize() {
        TestGlanceAppWidget.uiDefinition = {
            Column(
                modifier = GlanceModifier.size(100.dp)
                    .background(Color.DarkGray),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    "Upper half",
                    modifier = GlanceModifier.defaultWeight().fillMaxWidth()
                        .background(Color.Green)
                )
                Text(
                    "Lower right half",
                    modifier = GlanceModifier.defaultWeight().width(50.dp)
                        .background(Color.Cyan)
                )
            }
        }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "fixed_top_level_size")
    }

    @Test
    fun checkTopLevelFill() {
        TestGlanceAppWidget.uiDefinition = {
            Column(
                modifier = GlanceModifier.fillMaxSize()
                    .background(Color.DarkGray),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    "Upper half",
                    modifier = GlanceModifier.defaultWeight().fillMaxWidth()
                        .background(Color.Green)
                )
                Text(
                    "Lower right half",
                    modifier = GlanceModifier.defaultWeight().width(50.dp)
                        .background(Color.Cyan)
                )
            }
        }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "fill_top_level_size")
    }

    @Test
    fun checkTopLevelWrap() {
        TestGlanceAppWidget.uiDefinition = {
            Column(
                modifier = GlanceModifier.wrapContentSize()
                    .background(Color.DarkGray),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Above",
                    modifier = GlanceModifier.background(Color.Green)
                )
                Text(
                    "Larger below",
                    modifier = GlanceModifier.background(Color.Cyan)
                )
            }
        }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "wrap_top_level_size")
    }

    @Test
    fun drawableBackground() {
        TestGlanceAppWidget.uiDefinition = {
            Box(
                modifier = GlanceModifier.fillMaxSize().background(Color.Green).padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Some useful text",
                    modifier = GlanceModifier.fillMaxWidth().height(220.dp)
                        .background(ImageProvider(R.drawable.filled_oval))
                )
            }
        }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "drawable_background")
    }

    @Test
    fun drawableFitBackground() {
        TestGlanceAppWidget.uiDefinition = {
            Box(
                modifier = GlanceModifier.fillMaxSize().background(Color.Green).padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Some useful text",
                    modifier = GlanceModifier.fillMaxWidth().height(220.dp)
                        .background(
                            ImageProvider(R.drawable.filled_oval),
                            contentScale = ContentScale.Fit
                        )
                )
            }
        }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "drawable_fit_background")
    }

    @Test
    fun bitmapBackground() {
        TestGlanceAppWidget.uiDefinition = {
            val context = LocalContext.current
            val bitmap = context.resources.getDrawable(R.drawable.compose, null) as BitmapDrawable
            Box(
                modifier = GlanceModifier.fillMaxSize().background(Color.Green).padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Some useful text",
                    modifier = GlanceModifier.fillMaxWidth().height(220.dp)
                        .background(ImageProvider(bitmap.bitmap!!))
                )
            }
        }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "bitmap_background")
    }
}

@Composable
private fun TextAlignmentTest() {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Text(
            "Center",
            style = TextStyle(textAlign = TextAlign.Center),
            modifier = GlanceModifier.fillMaxWidth()
        )
        Text(
            "Left",
            style = TextStyle(textAlign = TextAlign.Left),
            modifier = GlanceModifier.fillMaxWidth()
        )
        Text(
            "Right",
            style = TextStyle(textAlign = TextAlign.Right),
            modifier = GlanceModifier.fillMaxWidth()
        )
        Text(
            "Start",
            style = TextStyle(textAlign = TextAlign.Start),
            modifier = GlanceModifier.fillMaxWidth()
        )
        Text(
            "End",
            style = TextStyle(textAlign = TextAlign.End),
            modifier = GlanceModifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RowTest() {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        Text(
            "Start",
            style = TextStyle(textAlign = TextAlign.Start),
            modifier = GlanceModifier.defaultWeight()
        )
        Text(
            "Center",
            style = TextStyle(textAlign = TextAlign.Center),
            modifier = GlanceModifier.defaultWeight()
        )
        Text(
            "End",
            style = TextStyle(textAlign = TextAlign.End),
            modifier = GlanceModifier.defaultWeight()
        )
    }
}

@Composable
private fun BackgroundTest() {
    Column(modifier = GlanceModifier.background(R.color.background_color)) {
        Text(
            "100x50 and cyan",
            modifier = GlanceModifier.width(100.dp).height(50.dp).background(Color.Cyan)
        )
        Text(
            "Transparent background",
            modifier = GlanceModifier.height(50.dp).background(Color.Transparent)
        )
        Text(
            "wrapx30 and red (light), yellow (dark)",
            modifier = GlanceModifier
                .height(30.dp)
                .background(day = Color.Red, night = Color.Yellow)
        )
        Text("Below this should be 4 color boxes")
        Row(modifier = GlanceModifier.padding(8.dp)) {
            Box(
                modifier =
                GlanceModifier
                    .width(32.dp)
                    .height(32.dp)
                    .background(day = Color.Black, night = Color.White)
            ) {}
            val colors = listOf(Color.Red, Color.Green, Color.Blue)
            repeat(3) {
                Box(modifier = GlanceModifier.width(8.dp).height(1.dp)) {}
                Box(
                    modifier = GlanceModifier.width(32.dp).height(32.dp).background(colors[it])
                ) {}
            }
        }
    }
}
