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
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.test.R
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
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
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
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
        TestGlanceAppWidget.uiDefinition = { CheckBoxScreenshotTest() }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "checkBoxWidget")
    }

    @WithNightMode
    @Test
    fun createCheckBoxAppWidget_dark() {
        TestGlanceAppWidget.uiDefinition = { CheckBoxScreenshotTest() }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "checkBoxWidget_dark")
    }

    @Test
    fun createCheckSwitchAppWidget() {
        TestGlanceAppWidget.uiDefinition = { SwitchTest() }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "switchWidget")
    }

    @WithNightMode
    @Test
    fun createCheckSwitchAppWidget_dark() {
        TestGlanceAppWidget.uiDefinition = { SwitchTest() }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "switchWidget_dark")
    }

    @Test
    fun createRadioButtonAppWidget() {
        TestGlanceAppWidget.uiDefinition = { RadioButtonScreenshotTest() }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "radioButtonWidget")
    }

    @WithNightMode
    @Test
    fun createRadioButtonAppWidget_dark() {
        TestGlanceAppWidget.uiDefinition = { RadioButtonScreenshotTest() }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "radioButtonWidget_dark")
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
    fun checkTextColor_light() {
        TestGlanceAppWidget.uiDefinition = { TextColorTest() }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "textColor")
    }

    @Test
    @WithNightMode
    fun checkTextColor_dark() {
        TestGlanceAppWidget.uiDefinition = { TextColorTest() }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "textColor_dark")
    }

    @Test
    fun checkButtonRoundedCorners_light() {
        TestGlanceAppWidget.uiDefinition = { RoundedButtonScreenshotTest() }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "roundedButton_light")
    }

    @Test
    @WithNightMode
    fun checkButtonRoundedCorners_dark() {
        TestGlanceAppWidget.uiDefinition = { RoundedButtonScreenshotTest() }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "roundedButton_dark")
    }

    @Test
    fun checkButtonTextAlignment() {
        TestGlanceAppWidget.uiDefinition = {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                    Button(
                        "Start",
                        onClick = actionStartActivity<Activity>(),
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = ColorProvider(Color.Transparent),
                            contentColor = ColorProvider(Color.DarkGray)
                        ),
                        style = TextStyle(textAlign = TextAlign.Start)
                    )
                    Button(
                        "End",
                        onClick = actionStartActivity<Activity>(),
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = ColorProvider(Color.Transparent),
                            contentColor = ColorProvider(Color.DarkGray)
                        ),
                        style = TextStyle(textAlign = TextAlign.End)
                    )
                }
                Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                    CheckBox(
                        checked = false,
                        onCheckedChange = null,
                        text = "Start",
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        style = TextStyle(textAlign = TextAlign.Start)
                    )
                    CheckBox(
                        checked = true,
                        onCheckedChange = null,
                        text = "End",
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        style = TextStyle(textAlign = TextAlign.End)
                    )
                }
                Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                    Switch(
                        checked = false,
                        onCheckedChange = null,
                        text = "Start",
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        style = TextStyle(textAlign = TextAlign.Start)
                    )
                    Switch(
                        checked = true,
                        onCheckedChange = null,
                        text = "End",
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        style = TextStyle(textAlign = TextAlign.End)
                    )
                }
                Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                    RadioButton(
                        checked = false,
                        onClick = null,
                        text = "Start",
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        style = TextStyle(textAlign = TextAlign.Start)
                    )
                    RadioButton(
                        checked = true,
                        onClick = null,
                        text = "End",
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        style = TextStyle(textAlign = TextAlign.End)
                    )
                }
            }
        }

        mHostRule.setSizes(DpSize(300.dp, 400.dp))
        mHostRule.startHost()

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

    @Test
    fun alignment() {
        TestGlanceAppWidget.uiDefinition = {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.End,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text("##")
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxHeight(),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = GlanceModifier.height(80.dp),
                    ) {
                        Text("Center")
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = GlanceModifier.height(80.dp),
                    ) {
                        Text("BottomCenter")
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = GlanceModifier.height(80.dp),
                    ) {
                        Text("CenterStart")
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxHeight(),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            ImageProvider(R.drawable.compose),
                            "Compose",
                            modifier = GlanceModifier.size(80.dp),
                        )
                        Text("OXO", style = TextStyle(fontSize = 18.sp))
                    }
                    Box(contentAlignment = Alignment.BottomCenter) {
                        Image(
                            ImageProvider(R.drawable.compose),
                            "Compose",
                            modifier = GlanceModifier.size(80.dp),
                        )
                        Text("OXO", style = TextStyle(fontSize = 18.sp))
                    }
                    Box(contentAlignment = Alignment.CenterStart) {
                        Image(
                            ImageProvider(R.drawable.compose),
                            "Compose",
                            modifier = GlanceModifier.size(80.dp),
                        )
                        Text("OXO", style = TextStyle(fontSize = 18.sp))
                    }
                }
            }
        }

        mHostRule.startHost()

        mScreenshotRule.checkScreenshot(mHostRule.mHostView, "alignment")
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

@Composable
private fun TextColorTest() {
    Column(modifier = GlanceModifier.background(R.color.background_color)) {
        Text("Cyan", style = TextStyle(color = ColorProvider(Color.Cyan)))
        Text(
            "Red (light) or yellow (dark)",
            style = TextStyle(color = ColorProvider(day = Color.Red, night = Color.Yellow))
        )
        Text(
            "Resource (inverse of background color)",
            style = TextStyle(color = ColorProvider(R.color.text_color))
        )
    }
}

@Composable
private fun RoundedButtonScreenshotTest() {
    val columnColors = listOf(Color(0xffffdbcd), Color(0xff7d2d00))
    val buttonBgColors = listOf(Color(0xffa33e00), Color(0xffffb596))
    val buttonTextColors = listOf(Color(0xffffffff), Color(0xff581e00))

    Column(
        modifier = GlanceModifier.padding(10.dp)
            .background(day = columnColors[0], night = columnColors[1])
    ) {
        Button(
            "Button with textAlign = Start",
            onClick = actionStartActivity<Activity>(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = ColorProvider(day = buttonBgColors[0], night = buttonBgColors[1]),
                contentColor = ColorProvider(day = buttonTextColors[0], night = buttonTextColors[1])
            ),
            style = TextStyle(textAlign = TextAlign.Start)
        )
        Spacer(modifier = GlanceModifier.height(5.dp).fillMaxWidth())
        Button(
            "Button with textAlign = Center and padding (30dp, 30dp)",
            onClick = actionStartActivity<Activity>(),
            modifier = GlanceModifier.padding(horizontal = 30.dp, vertical = 30.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = ColorProvider(day = buttonBgColors[0], night = buttonBgColors[1]),
                contentColor = ColorProvider(day = buttonTextColors[0], night = buttonTextColors[1])
            ),
            style = TextStyle(textAlign = TextAlign.Center)
        )
        Spacer(modifier = GlanceModifier.height(5.dp).fillMaxWidth())
        Button(
            "Button with textAlign = End",
            onClick = actionStartActivity<Activity>(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = ColorProvider(day = buttonBgColors[0], night = buttonBgColors[1]),
                contentColor = ColorProvider(day = buttonTextColors[0], night = buttonTextColors[1])
            ),
            style = TextStyle(textAlign = TextAlign.End)
        )
    }
}

@Composable
private fun CheckBoxScreenshotTest() {
    Column(modifier = GlanceModifier.background(day = Color.White, night = Color.Black)) {
        CheckBox(
            checked = true,
            onCheckedChange = null,
            text = "Hello Checked Checkbox (text: day=black, night=white| box: day=magenta, " +
                "night=yellow)",
            style = TextStyle(
                color = ColorProvider(day = Color.Black, night = Color.White),
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Normal,
            ),
            colors = CheckboxDefaults.colors(
                checkedColor = ColorProvider(day = Color.Magenta, night = Color.Yellow),
                uncheckedColor = ColorProvider(day = Color.Black, night = Color.Gray)
            )
        )

        CheckBox(
            checked = false,
            onCheckedChange = null,
            text = "Hello Unchecked Checkbox (text: day=dark gray, night=light gray, green box)",
            style = TextStyle(
                color = ColorProvider(day = Color.DarkGray, night = Color.LightGray),
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
            ),
            colors = CheckboxDefaults.colors(checkedColor = Color.Red, uncheckedColor = Color.Green)
        )
    }
}

@Composable
private fun SwitchTest() {
    Column(modifier = GlanceModifier.background(day = Color.White, night = Color.Black)) {
        Switch(
            checked = true,
            onCheckedChange = null,
            text = "Hello Checked Switch (day: Blue/Green, night: Red/Yellow)",
            style = TextStyle(
                color = ColorProvider(day = Color.Black, night = Color.White),
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Normal,
            ),
            colors = SwitchDefaults.colors(
                checkedThumbColor = ColorProvider(day = Color.Blue, night = Color.Red),
                uncheckedThumbColor = ColorProvider(Color.Magenta),
                checkedTrackColor = ColorProvider(day = Color.Green, night = Color.Yellow),
                uncheckedTrackColor = ColorProvider(Color.Magenta)
            )
        )

        Switch(
            checked = false,
            onCheckedChange = null,
            text = "Hello Unchecked Switch. day: thumb magenta / track cyan, night: thumb cyan",
            style = TextStyle(
                color = ColorProvider(day = Color.Black, night = Color.White),
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
            ),
            colors = SwitchDefaults.colors(
                checkedThumbColor = ColorProvider(Color.Blue),
                uncheckedThumbColor = ColorProvider(day = Color.Magenta, night = Color.Cyan),
                checkedTrackColor = ColorProvider(Color.Blue),
                uncheckedTrackColor = ColorProvider(day = Color.Cyan, night = Color.Magenta)
            )
        )
    }
}

@Composable
private fun RadioButtonScreenshotTest() {
    Column(
        modifier = GlanceModifier.background(day = Color.White, night = Color.Black)
    ) {
        RadioButton(
            checked = true,
            onClick = null,
            text = "Hello Checked Radio (text: day=black, night=white| radio: day=magenta, " +
                "night=yellow)",
            style = TextStyle(
                color = ColorProvider(day = Color.Black, night = Color.White),
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Normal,
            ),
            colors = RadioButtonDefaults.colors(
                checkedColor = ColorProvider(day = Color.Magenta, night = Color.Yellow),
                uncheckedColor = ColorProvider(day = Color.Yellow, night = Color.Magenta)
            )
        )

        RadioButton(
            checked = false,
            onClick = null,
            text = "Hello Unchecked Radio (text: day=dark gray, night=light gray| radio: green)",
            style = TextStyle(
                color = ColorProvider(day = Color.DarkGray, night = Color.LightGray),
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
            ),
            colors = RadioButtonDefaults.colors(
                checkedColor = Color.Red,
                uncheckedColor = Color.Green
            )
        )
    }
}
