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

package androidx.glance.wear.tiles

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.Button
import androidx.glance.ButtonColors
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.wear.tiles.action.ActionCallback
import androidx.glance.wear.tiles.action.actionRunCallback
import androidx.glance.wear.tiles.curved.AnchorType
import androidx.glance.wear.tiles.curved.CurvedRow
import androidx.glance.wear.tiles.curved.CurvedTextStyle
import androidx.glance.wear.tiles.curved.GlanceCurvedModifier
import androidx.glance.wear.tiles.curved.RadialAlignment
import androidx.glance.wear.tiles.curved.clickable
import androidx.glance.wear.tiles.curved.semantics
import androidx.glance.wear.tiles.curved.sweepAngleDegrees
import androidx.glance.wear.tiles.curved.thickness
import androidx.glance.wear.tiles.test.R
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.util.Arrays
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WearCompositionTranslatorTest {
    private lateinit var fakeCoroutineScope: TestScope

    @Before
    fun setUp() {
        fakeCoroutineScope = TestScope()
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun canTranslateBox() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            Box {}
        }.layout

        // runAndTranslate wraps the result in a Box...ensure that the layout generated two Boxes
        val outerBox = content as androidx.wear.tiles.LayoutElementBuilders.Box
        assertThat(outerBox.contents).hasSize(1)

        assertIs<androidx.wear.tiles.LayoutElementBuilders.Box>(outerBox.contents[0])
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun canTranslateBoxWithAlignment() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            Box(contentAlignment = Alignment.Center) {}
        }.layout

        val innerBox =
            (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0]
                as androidx.wear.tiles.LayoutElementBuilders.Box

        assertThat(innerBox.verticalAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
        assertThat(innerBox.horizontalAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun canTranslateBoxWithChildren() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            Box {
                Box(contentAlignment = Alignment.TopCenter) {}
                Box(contentAlignment = Alignment.BottomEnd) {}
            }
        }.layout

        val innerBox =
            (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0]
                as androidx.wear.tiles.LayoutElementBuilders.Box
        val leaf0 = innerBox.contents[0] as androidx.wear.tiles.LayoutElementBuilders.Box
        val leaf1 = innerBox.contents[1] as androidx.wear.tiles.LayoutElementBuilders.Box

        assertThat(leaf0.verticalAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_TOP)
        assertThat(leaf0.horizontalAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)

        assertThat(leaf1.verticalAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
        assertThat(leaf1.horizontalAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_END)
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun canTranslatePaddingModifier() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            Box(
                modifier =
                GlanceModifier.padding(start = 1.dp, top = 2.dp, end = 3.dp, bottom = 4.dp)
            ) {}
        }.layout

        val innerBox =
            (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0]
                as androidx.wear.tiles.LayoutElementBuilders.Box
        val padding = requireNotNull(innerBox.modifiers!!.padding)

        assertThat(padding.start!!.value).isEqualTo(1f)
        assertThat(padding.top!!.value).isEqualTo(2f)
        assertThat(padding.end!!.value).isEqualTo(3f)
        assertThat(padding.bottom!!.value).isEqualTo(4f)
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun canTranslateBorderModifier() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            Box(
                modifier = GlanceModifier.border(
                    width = 3.dp,
                    color = ColorProvider(color = Color.Blue)
                )
            ) {}
            Box(
                modifier = GlanceModifier.border(
                    width = R.dimen.dimension1,
                    color = ColorProvider(color = Color.Red)
                )
            ) {}
        }.layout

        val innerBox1 =
            (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0]
                as androidx.wear.tiles.LayoutElementBuilders.Box
        val innerBox2 = content.contents[1] as androidx.wear.tiles.LayoutElementBuilders.Box

        val border1 = requireNotNull(innerBox1.modifiers!!.border)
        assertThat(border1.width!!.value).isEqualTo(3f)
        assertThat(border1.color!!.argb).isEqualTo(Color.Blue.toArgb())

        val border2 = requireNotNull(innerBox2.modifiers!!.border)
        val context = getApplicationContext<Context>()
        assertThat(border2.width!!.value).isEqualTo(
            context.resources.getDimension(R.dimen.dimension1)
        )
        assertThat(border2.color!!.argb).isEqualTo(Color.Red.toArgb())
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun canTranslateBackgroundModifier() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            Box(modifier = GlanceModifier.background(Color(0x11223344))) {}
        }.layout

        val innerBox =
            (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0]
                as androidx.wear.tiles.LayoutElementBuilders.Box
        val background = requireNotNull(innerBox.modifiers!!.background)

        assertThat(background.color!!.argb).isEqualTo(0x11223344)
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun canTranslateBackgroundModifier_resId() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            Box(modifier = GlanceModifier.background(R.color.color1)) {}
        }.layout

        val innerBox =
            (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0]
                as androidx.wear.tiles.LayoutElementBuilders.Box
        val background = requireNotNull(innerBox.modifiers!!.background)

        assertThat(background.color!!.argb)
            .isEqualTo(android.graphics.Color.rgb(0xC0, 0xFF, 0xEE))
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun canTranslateSemanticsModifier() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            Box(modifier = GlanceModifier.semantics({ contentDescription = "test_description" })) {}
        }.layout

        val innerBox =
            (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0]
                as androidx.wear.tiles.LayoutElementBuilders.Box
        val semantics = requireNotNull(innerBox.modifiers!!.semantics)
        assertThat(semantics.contentDescription).isEqualTo("test_description")
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun canTranslateSemanticsCurvedModifier() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            CurvedRow {
                curvedText(
                    text = "Hello World",
                    curvedModifier =
                    GlanceCurvedModifier.semantics({ contentDescription = "test_description" })
                )
            }
        }.layout

        val innerArc = (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0]
            as androidx.wear.tiles.LayoutElementBuilders.Arc
        val innerArcText = innerArc.contents[0] as androidx.wear.tiles.LayoutElementBuilders.ArcText
        val semantics = requireNotNull(innerArcText.modifiers!!.semantics)
        assertThat(semantics.contentDescription).isEqualTo("test_description")
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun canTranslateRow() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Box(contentAlignment = Alignment.TopCenter) {}
                Box(contentAlignment = Alignment.BottomEnd) {}
            }
        }.layout

        val innerRow =
            (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0]
                as androidx.wear.tiles.LayoutElementBuilders.Row

        assertThat(innerRow.verticalAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_CENTER)

        val leaf0 = innerRow.contents[0] as androidx.wear.tiles.LayoutElementBuilders.Box
        val leaf1 = innerRow.contents[1] as androidx.wear.tiles.LayoutElementBuilders.Box

        assertThat(leaf0.verticalAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_TOP)
        assertThat(leaf0.horizontalAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)

        assertThat(leaf1.verticalAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
        assertThat(leaf1.horizontalAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_END)
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun rowWithHorizontalAlignmentInflatesInColumn() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            Row(
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color(0x11223344))
            ) {}
        }.layout

        val innerColumn =
            (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0]
                as androidx.wear.tiles.LayoutElementBuilders.Column
        val innerRow = innerColumn.contents[0] as androidx.wear.tiles.LayoutElementBuilders.Row

        assertThat(innerColumn.horizontalAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)

        // Column should inherit the size of the inner Row
        assertIs<androidx.wear.tiles.DimensionBuilders.ExpandedDimensionProp>(innerColumn.width)
        assertThat((innerColumn.height as androidx.wear.tiles.DimensionBuilders.DpProp).value)
            .isEqualTo(100f)

        // Column should also inherit the modifiers
        assertThat(innerColumn.modifiers!!.background!!.color!!.argb).isEqualTo(0x11223344)

        // The row should have a wrapped width, but still use the height
        assertIs<androidx.wear.tiles.DimensionBuilders.WrappedDimensionProp>(innerRow.width)
        assertThat((innerRow.height as androidx.wear.tiles.DimensionBuilders.DpProp).value)
            .isEqualTo(100f)

        // And no modifiers.
        assertThat(innerRow.modifiers).isNull()

        // Should have the vertical alignment set still though
        assertThat(innerRow.verticalAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun canTranslateColumn() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                Box(contentAlignment = Alignment.TopCenter) {}
                Box(contentAlignment = Alignment.BottomEnd) {}
            }
        }.layout

        val innerColumn =
            (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0]
                as androidx.wear.tiles.LayoutElementBuilders.Column

        assertThat(innerColumn.horizontalAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)

        val leaf0 = innerColumn.contents[0] as androidx.wear.tiles.LayoutElementBuilders.Box
        val leaf1 = innerColumn.contents[1] as androidx.wear.tiles.LayoutElementBuilders.Box

        assertThat(leaf0.verticalAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_TOP)
        assertThat(leaf0.horizontalAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)

        assertThat(leaf1.verticalAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
        assertThat(leaf1.horizontalAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_END)
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun columnWithVerticalAlignmentInflatesInRow() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            Column(
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                modifier = GlanceModifier
                    .fillMaxHeight()
                    .width(100.dp)
                    .background(Color(0x11223344))
            ) {}
        }.layout

        val innerRow =
            (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0]
                as androidx.wear.tiles.LayoutElementBuilders.Row
        val innerColumn = innerRow.contents[0] as androidx.wear.tiles.LayoutElementBuilders.Column

        assertThat(innerRow.verticalAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_CENTER)

        // Row should inherit the size of the inner Row
        assertThat((innerRow.width as androidx.wear.tiles.DimensionBuilders.DpProp).value)
            .isEqualTo(100f)
        assertIs<androidx.wear.tiles.DimensionBuilders.ExpandedDimensionProp>(innerRow.height)

        // Row should also inherit the modifiers
        assertThat(innerRow.modifiers!!.background!!.color!!.argb).isEqualTo(0x11223344)

        // The Column should have a wrapped width, but still use the height
        assertThat((innerColumn.width as androidx.wear.tiles.DimensionBuilders.DpProp).value)
            .isEqualTo(100f)
        assertIs<androidx.wear.tiles.DimensionBuilders.WrappedDimensionProp>(innerColumn.height)

        // And no modifiers.
        assertThat(innerColumn.modifiers).isNull()

        // Should have the horizontal alignment set still though
        assertThat(innerColumn.horizontalAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun canInflateText() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            val style = TextStyle(
                color = ColorProvider(Color.Gray),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                textDecoration = TextDecoration.Underline,
                textAlign = TextAlign.End
            )
            Text("Hello World", modifier = GlanceModifier.padding(1.dp), style = style)
        }.layout

        val innerText = (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0]
            as androidx.wear.tiles.LayoutElementBuilders.Text

        assertThat(innerText.text!!.value).isEqualTo("Hello World")
        assertThat(innerText.fontStyle!!.color!!.argb).isEqualTo(Color.Gray.toArgb())
        assertThat(innerText.fontStyle!!.size!!.value).isEqualTo(16f)
        assertThat(innerText.fontStyle!!.italic!!.value).isTrue()
        assertThat(innerText.fontStyle!!.weight!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_BOLD)
        assertThat(innerText.fontStyle!!.underline!!.value).isTrue()
        assertThat(innerText.multilineAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.TEXT_ALIGN_END)
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun textWithSizeInflatesInBox() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            Text(
                "Hello World",
                modifier = GlanceModifier.size(100.dp).padding(10.dp),
                style = TextStyle(textAlign = TextAlign.End))
        }.layout

        val innerBox = (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0] as
            androidx.wear.tiles.LayoutElementBuilders.Box
        val innerText = innerBox.contents[0] as androidx.wear.tiles.LayoutElementBuilders.Text

        assertThat(innerBox.width is androidx.wear.tiles.DimensionBuilders.DpProp)
        assertThat((innerBox.width as androidx.wear.tiles.DimensionBuilders.DpProp).value)
            .isEqualTo(100f)
        assertThat(innerBox.height is androidx.wear.tiles.DimensionBuilders.DpProp)
        assertThat((innerBox.height as androidx.wear.tiles.DimensionBuilders.DpProp).value)
            .isEqualTo(100f)
        assertThat(innerBox.horizontalAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_END)

        // Modifiers should apply to the Box
        assertThat(innerBox.modifiers!!.padding).isNotNull()

        // ... and not to the Text
        assertThat(innerText.modifiers?.padding).isNull()
        assertThat(innerText.multilineAlignment!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.TEXT_ALIGN_END)
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun canTranslateCurvedRow() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            CurvedRow(
                anchorDegrees = 20f,
                radialAlignment = RadialAlignment.Inner,
                anchorType = AnchorType.End,
                modifier = GlanceModifier.padding(20.dp)
            ) {}
        }.layout

        val innerArc = (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0]
            as androidx.wear.tiles.LayoutElementBuilders.Arc

        // Remember, 0 degrees is handled differently in Glance (3 o clock) and Tiles (12 o clock).
        assertThat(innerArc.anchorAngle!!.value).isEqualTo(110f)
        assertThat(innerArc.anchorType!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.ARC_ANCHOR_END)
        assertThat(innerArc.modifiers!!.padding).isNotNull()
        assertThat(innerArc.verticalAlign!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun curvedRowWithSizeInflatesInBox() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            CurvedRow(
                anchorDegrees = 20f,
                radialAlignment = RadialAlignment.Inner,
                anchorType = AnchorType.End,
                modifier = GlanceModifier.padding(20.dp).size(10.dp)
            ) {}
        }.layout

        val innerBox = (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0]
            as androidx.wear.tiles.LayoutElementBuilders.Box
        val innerArc = innerBox.contents[0] as androidx.wear.tiles.LayoutElementBuilders.Arc

        assertThat(innerBox.width is androidx.wear.tiles.DimensionBuilders.DpProp)
        assertThat((innerBox.width as androidx.wear.tiles.DimensionBuilders.DpProp).value)
            .isEqualTo(10f)
        assertThat(innerBox.height is androidx.wear.tiles.DimensionBuilders.DpProp)
        assertThat((innerBox.height as androidx.wear.tiles.DimensionBuilders.DpProp).value)
            .isEqualTo(10f)

        // Modifiers should apply to the Box
        assertThat(innerBox.modifiers!!.padding).isNotNull()

        // ... and not to the Arc
        assertThat(innerArc.modifiers?.padding).isNull()
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun canTranslateCurvedText() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            val style = CurvedTextStyle(
                color = ColorProvider(R.color.color1),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
            )

            CurvedRow {
                curvedText(text = "Hello World", style = style)
            }
        }.layout

        val innerArc = (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0]
            as androidx.wear.tiles.LayoutElementBuilders.Arc
        val innerArcText = innerArc.contents[0] as androidx.wear.tiles.LayoutElementBuilders.ArcText

        assertThat(innerArcText.text!!.value).isEqualTo("Hello World")
        assertThat(innerArcText.fontStyle!!.color!!.argb)
            .isEqualTo(android.graphics.Color.rgb(0xC0, 0xFF, 0xEE))
        assertThat(innerArcText.fontStyle!!.size!!.value).isEqualTo(16f)
        assertThat(innerArcText.fontStyle!!.italic!!.value).isTrue()
        assertThat(innerArcText.fontStyle!!.weight!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_BOLD)
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun canTranslateCurvedLine() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            CurvedRow {
                curvedLine(
                    color = ColorProvider(Color(0x11223344)),
                    curvedModifier =
                    GlanceCurvedModifier.sweepAngleDegrees(90f).thickness(10.dp)
                )
            }
        }.layout

        val innerArc = (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0]
            as androidx.wear.tiles.LayoutElementBuilders.Arc
        val innerArcLine = innerArc.contents[0] as androidx.wear.tiles.LayoutElementBuilders.ArcLine

        assertThat(innerArcLine.color!!.argb).isEqualTo(0x11223344)
        assertThat(innerArcLine.length!!.value).isEqualTo(90f)
        assertThat(innerArcLine.thickness!!.value).isEqualTo(10f)
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun canTranslateCurvedSpacer() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            CurvedRow {
                curvedSpacer(
                    curvedModifier =
                    GlanceCurvedModifier.sweepAngleDegrees(60f).thickness(6.dp)
                )
            }
        }.layout

        val innerArc = (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0]
            as androidx.wear.tiles.LayoutElementBuilders.Arc
        val innerArcSpacer = innerArc.contents[0]
            as androidx.wear.tiles.LayoutElementBuilders.ArcSpacer

        assertThat(innerArcSpacer.length!!.value).isEqualTo(60f)
        assertThat(innerArcSpacer.thickness!!.value).isEqualTo(6f)
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun canTranslateActionOnCurvedElement() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            CurvedRow {
                curvedText(
                    text = "hello",
                    curvedModifier = GlanceCurvedModifier.clickable(
                        actionStartActivity(TestActivity::class.java)
                    )
                )
                curvedLine(
                    color = ColorProvider(Color(0x11223344)),
                    curvedModifier =
                    GlanceCurvedModifier.sweepAngleDegrees(60f).thickness(10.dp)
                        .clickable(actionRunCallback<TestCallback>())

                )
            }
        }.layout

        val arc = (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0]
            as androidx.wear.tiles.LayoutElementBuilders.Arc
        val arcText = arc.contents[0] as androidx.wear.tiles.LayoutElementBuilders.ArcText
        val arcLine = arc.contents[1] as androidx.wear.tiles.LayoutElementBuilders.ArcLine

        val launchAction = arcText.modifiers!!.clickable!!.onClick
            as androidx.wear.tiles.ActionBuilders.LaunchAction
        assertThat(launchAction.androidActivity).isNotNull()
        assertThat(launchAction.androidActivity!!.packageName)
            .isEqualTo(getApplicationContext<Context>().packageName)
        assertThat(launchAction.androidActivity!!.className)
            .isEqualTo(TestActivity::class.qualifiedName)

        val arcLineClickable = arcLine.modifiers!!.clickable
            as androidx.wear.tiles.ModifiersBuilders.Clickable
        assertThat(arcLineClickable.onClick as androidx.wear.tiles.ActionBuilders.LoadAction)
            .isNotNull()
        assertThat(arcLineClickable.id).isEqualTo(TestCallback::class.java.canonicalName)
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun canTranslateAndroidLayoutElement() = fakeCoroutineScope.runTest {
        val providedLayoutElement =
            androidx.wear.tiles.LayoutElementBuilders.Text.Builder()
                .setText("Android Layout Element")
                .build()

        val content = runAndTranslate {
            AndroidLayoutElement(providedLayoutElement)
        }.layout

        val box = assertIs<androidx.wear.tiles.LayoutElementBuilders.Box>(content)
        val textElement = assertIs<androidx.wear.tiles.LayoutElementBuilders.Text>(box.contents[0])
        assertThat(textElement.text!!.value).isEqualTo("Android Layout Element")
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun otherElementInArcInflatesInArcAdapter() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            CurvedRow {
                curvedComposable(false) {
                    Box {}
                }
            }
        }.layout

        val innerArc = (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0]
            as androidx.wear.tiles.LayoutElementBuilders.Arc
        val innerArcAdapter = innerArc.contents[0]
            as androidx.wear.tiles.LayoutElementBuilders.ArcAdapter
        assertIs<androidx.wear.tiles.LayoutElementBuilders.Box>(innerArcAdapter.content)
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun canInflateLaunchAction() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            Text(
                modifier = GlanceModifier.clickable(actionStartActivity(TestActivity::class.java)),
                text = "Hello World"
            )
        }.layout

        val innerText = (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0] as
            androidx.wear.tiles.LayoutElementBuilders.Text

        assertThat(innerText.modifiers!!.clickable).isNotNull()
        assertThat(innerText.modifiers!!.clickable!!.onClick)
            .isInstanceOf(androidx.wear.tiles.ActionBuilders.LaunchAction::class.java)

        val launchAction = innerText.modifiers!!.clickable!!.onClick
            as androidx.wear.tiles.ActionBuilders.LaunchAction
        assertThat(launchAction.androidActivity).isNotNull()

        val packageName = getApplicationContext<Context>().packageName
        assertThat(launchAction.androidActivity!!.packageName).isEqualTo(packageName)
        assertThat(launchAction.androidActivity!!.className)
            .isEqualTo(TestActivity::class.qualifiedName)
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun canTranslateButton() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            val style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                textDecoration = TextDecoration.Underline
            )
            Button(
                "Hello World",
                onClick = actionStartActivity(TestActivity::class.java),
                modifier = GlanceModifier.padding(1.dp),
                colors = ButtonColors(
                    backgroundColor = ColorProvider(Color.Black),
                    contentColor = ColorProvider(Color.Magenta)
                ),
                style = style
            )
        }.layout

        val box = assertIs<androidx.wear.tiles.LayoutElementBuilders.Box>(content)
        val innerText = assertIs<androidx.wear.tiles.LayoutElementBuilders.Text>(box.contents[0])

        assertThat(innerText.text!!.value).isEqualTo("Hello World")

        assertThat(innerText.fontStyle!!.color!!.argb).isEqualTo(Color.Magenta.toArgb())
        assertThat(innerText.fontStyle!!.size!!.value).isEqualTo(16f)
        assertThat(innerText.fontStyle!!.italic!!.value).isTrue()
        assertThat(innerText.fontStyle!!.weight!!.value)
            .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_BOLD)
        assertThat(innerText.fontStyle!!.underline!!.value).isTrue()

        assertThat(innerText.modifiers!!.clickable).isNotNull()
        assertThat(innerText.modifiers!!.clickable!!.onClick)
            .isInstanceOf(androidx.wear.tiles.ActionBuilders.LaunchAction::class.java)
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun canTranslateSpacer() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            Spacer(GlanceModifier.width(10.dp))
            Spacer(GlanceModifier.height(15.dp))
            Spacer(GlanceModifier.size(8.dp, 12.dp))
        }.layout

        val spacerWithWidth =
            (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0]
                as androidx.wear.tiles.LayoutElementBuilders.Spacer
        assertThat((spacerWithWidth.width as androidx.wear.tiles.DimensionBuilders.DpProp).value)
            .isEqualTo(10f)

        val spacerWithHeight = content.contents[1]
            as androidx.wear.tiles.LayoutElementBuilders.Spacer
        assertThat((spacerWithHeight.height as androidx.wear.tiles.DimensionBuilders.DpProp).value)
            .isEqualTo(15f)

        val spacerWithSize = content.contents[2] as androidx.wear.tiles.LayoutElementBuilders.Spacer
        assertThat((spacerWithSize.width as androidx.wear.tiles.DimensionBuilders.DpProp).value)
            .isEqualTo(8f)
        assertThat((spacerWithSize.height as androidx.wear.tiles.DimensionBuilders.DpProp).value)
            .isEqualTo(12f)
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun canTranslateImage() = fakeCoroutineScope.runTest {
        val context = getApplicationContext<Context>()
        val bitmap = context.getDrawable(R.drawable.oval)!!.toBitmap()
        val compositionResult = runAndTranslate {
            Image(
                provider = ImageProvider(R.drawable.oval),
                contentDescription = "Oval",
                modifier = GlanceModifier.width(R.dimen.dimension1).height(R.dimen.dimension2),
                contentScale = ContentScale.FillBounds
            )
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = "OvalBitmap",
                modifier = GlanceModifier.width(R.dimen.dimension1).height(R.dimen.dimension2),
                contentScale = ContentScale.Crop
            )
        }
        val content = compositionResult.layout
        val resources = compositionResult.resources

        val image1 = (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0] as
            androidx.wear.tiles.LayoutElementBuilders.Image

        assertThat((image1.width as androidx.wear.tiles.DimensionBuilders.DpProp).value).isEqualTo(
            context.resources.getDimension(R.dimen.dimension1)
        )
        assertThat((image1.height as androidx.wear.tiles.DimensionBuilders.DpProp).value).isEqualTo(
            context.resources.getDimension(R.dimen.dimension2)
        )
        assertThat(image1.contentScaleMode!!.value).isEqualTo(
            androidx.wear.tiles.LayoutElementBuilders.CONTENT_SCALE_MODE_FILL_BOUNDS
        )
        val mappedId1 = "android_" + R.drawable.oval
        assertThat(image1.resourceId!!.value).isEqualTo(mappedId1)

        assertThat(image1.modifiers!!.semantics!!.contentDescription).isEqualTo("Oval")

        val image2 = content.contents[1] as androidx.wear.tiles.LayoutElementBuilders.Image
        assertThat(image2.contentScaleMode!!.value).isEqualTo(
            androidx.wear.tiles.LayoutElementBuilders.CONTENT_SCALE_MODE_CROP
        )

        val buffer = ByteArrayOutputStream().apply {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, this) }
            .toByteArray()
        val mappedId2 = "android_" + Arrays.hashCode(buffer)
        assertThat(image2.resourceId!!.value).isEqualTo(mappedId2)
        assertThat(image2.modifiers!!.semantics!!.contentDescription)
            .isEqualTo("OvalBitmap")

        val idToImageMap = resources.build().idToImageMapping
        assertThat(idToImageMap.containsKey(mappedId1)).isTrue()
        assertThat(idToImageMap[mappedId1]!!.androidResourceByResId).isNotNull()
        assertThat(idToImageMap[mappedId1]!!.inlineResource).isNull()
        assertThat(idToImageMap.containsKey(mappedId2)).isTrue()
        assertThat(idToImageMap[mappedId2]!!.inlineResource).isNotNull()
        assertThat(idToImageMap[mappedId2]!!.androidResourceByResId).isNull()
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun translateImage_noColorFilter() = fakeCoroutineScope.runTest {
        val compositionResult = runAndTranslate {
            Image(
                provider = ImageProvider(R.drawable.oval),
                contentDescription = null,
                modifier = GlanceModifier.width(R.dimen.dimension1).height(R.dimen.dimension2),
            )
        }

        val content = compositionResult.layout
        val image = (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0] as
            androidx.wear.tiles.LayoutElementBuilders.Image
        assertThat(image.colorFilter).isNull()
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun translateImage_colorFilter() = fakeCoroutineScope.runTest {
        val compositionResult = runAndTranslate {
            Image(
                provider = ImageProvider(R.drawable.oval),
                contentDescription = null,
                modifier = GlanceModifier.width(R.dimen.dimension1).height(R.dimen.dimension2),
                colorFilter = ColorFilter.tint(ColorProvider(Color.Gray))
            )
        }

        val content = compositionResult.layout
        val image = (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0] as
            androidx.wear.tiles.LayoutElementBuilders.Image
        val tint = assertNotNull(image.colorFilter?.tint)
        assertThat(tint.argb).isEqualTo(Color.Gray.toArgb())
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun translateImage_colorFilterWithResource() = fakeCoroutineScope.runTest {
        val compositionResult = runAndTranslate {
            Image(
                provider = ImageProvider(R.drawable.oval),
                contentDescription = null,
                modifier = GlanceModifier.width(R.dimen.dimension1).height(R.dimen.dimension2),
                colorFilter = ColorFilter.tint(ColorProvider(R.color.color1))
            )
        }

        val content = compositionResult.layout
        val image = (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0] as
            androidx.wear.tiles.LayoutElementBuilders.Image
        val tint = assertNotNull(image.colorFilter?.tint)
        assertThat(tint.argb).isEqualTo(android.graphics.Color.rgb(0xC0, 0xFF, 0xEE))
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun setSizeFromResource() = fakeCoroutineScope.runTest {
        val content = runAndTranslate {
            Column(
                modifier = GlanceModifier.width(R.dimen.dimension1)
                    .height(R.dimen.dimension2)
            ) {}
        }.layout

        val innerColumn =
            (content as androidx.wear.tiles.LayoutElementBuilders.Box).contents[0]
                as androidx.wear.tiles.LayoutElementBuilders.Column
        val context = getApplicationContext<Context>()

        // Row should inherit the size of the inner Row
        assertThat((innerColumn.width as androidx.wear.tiles.DimensionBuilders.DpProp).value)
            .isEqualTo(
            context.resources.getDimension(R.dimen.dimension1)
        )
        assertThat((innerColumn.height as androidx.wear.tiles.DimensionBuilders.DpProp).value)
            .isEqualTo(
            context.resources.getDimension(R.dimen.dimension2)
        )
    }

    private suspend fun runAndTranslate(
        content: @Composable () -> Unit
    ): CompositionResult {
        val root = runTestingComposition(content)

        return translateTopLevelComposition(getApplicationContext(), root)
    }
}

private class TestActivity : Activity()

private class TestCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId
    ) {
        // Nothing
    }
}
