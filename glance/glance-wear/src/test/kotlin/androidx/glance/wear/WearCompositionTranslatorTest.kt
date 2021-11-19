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

package androidx.glance.wear

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionLaunchActivity
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
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.wear.curved.AnchorType
import androidx.glance.wear.curved.CurvedRow
import androidx.glance.wear.curved.CurvedTextStyle
import androidx.glance.wear.curved.RadialAlignment
import androidx.glance.wear.test.R
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.wear.tiles.ActionBuilders
import androidx.wear.tiles.DimensionBuilders
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.LayoutElementBuilders.ARC_ANCHOR_END
import androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_BOLD
import androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_END
import androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM
import androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_TOP
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WearCompositionTranslatorTest {
    private lateinit var fakeCoroutineScope: TestCoroutineScope

    @Before
    fun setUp() {
        fakeCoroutineScope = TestCoroutineScope()
    }

    @Test
    fun canTranslateBox() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            Box {}
        }.layout

        // runAndTranslate wraps the result in a Box...ensure that the layout generated two Boxes
        val outerBox = content as LayoutElementBuilders.Box
        assertThat(outerBox.contents).hasSize(1)

        assertIs<LayoutElementBuilders.Box>(outerBox.contents[0])
    }

    @Test
    fun canTranslateBoxWithAlignment() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            Box(contentAlignment = Alignment.Center) {}
        }.layout

        val innerBox =
            (content as LayoutElementBuilders.Box).contents[0] as LayoutElementBuilders.Box

        assertThat(innerBox.verticalAlignment!!.value).isEqualTo(VERTICAL_ALIGN_CENTER)
        assertThat(innerBox.horizontalAlignment!!.value).isEqualTo(HORIZONTAL_ALIGN_CENTER)
    }

    @Test
    fun canTranslateBoxWithChildren() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            Box {
                Box(contentAlignment = Alignment.TopCenter) {}
                Box(contentAlignment = Alignment.BottomEnd) {}
            }
        }.layout

        val innerBox =
            (content as LayoutElementBuilders.Box).contents[0] as LayoutElementBuilders.Box
        val leaf0 = innerBox.contents[0] as LayoutElementBuilders.Box
        val leaf1 = innerBox.contents[1] as LayoutElementBuilders.Box

        assertThat(leaf0.verticalAlignment!!.value).isEqualTo(VERTICAL_ALIGN_TOP)
        assertThat(leaf0.horizontalAlignment!!.value).isEqualTo(HORIZONTAL_ALIGN_CENTER)

        assertThat(leaf1.verticalAlignment!!.value).isEqualTo(VERTICAL_ALIGN_BOTTOM)
        assertThat(leaf1.horizontalAlignment!!.value).isEqualTo(HORIZONTAL_ALIGN_END)
    }

    @Test
    fun canTranslatePaddingModifier() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            Box(modifier =
                GlanceModifier.padding(start = 1.dp, top = 2.dp, end = 3.dp, bottom = 4.dp)) {}
        }.layout

        val innerBox =
            (content as LayoutElementBuilders.Box).contents[0] as LayoutElementBuilders.Box
        val padding = requireNotNull(innerBox.modifiers!!.padding)

        assertThat(padding.start!!.value).isEqualTo(1f)
        assertThat(padding.top!!.value).isEqualTo(2f)
        assertThat(padding.end!!.value).isEqualTo(3f)
        assertThat(padding.bottom!!.value).isEqualTo(4f)
    }

    @Test
    fun canTranslateBorderModifier() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            Box(modifier = GlanceModifier.border(
                    width = 3.dp,
                    color = ColorProvider(color = Color.Blue)
            )) {}
            Box(modifier = GlanceModifier.border(
                    width = R.dimen.dimension1,
                color = ColorProvider(color = Color.Red)
            )) {}
        }.layout

        val innerBox1 =
            (content as LayoutElementBuilders.Box).contents[0] as LayoutElementBuilders.Box
        val innerBox2 = content.contents[1] as LayoutElementBuilders.Box

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
    fun canTranslateBackgroundModifier() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            Box(modifier = GlanceModifier.background(Color(0x11223344))) {}
        }.layout

        val innerBox =
            (content as LayoutElementBuilders.Box).contents[0] as LayoutElementBuilders.Box
        val background = requireNotNull(innerBox.modifiers!!.background)

        assertThat(background.color!!.argb).isEqualTo(0x11223344)
    }

    @Test
    fun canTranslateBackgroundModifier_resId() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            Box(modifier = GlanceModifier.background(R.color.color1)) {}
        }.layout

        val innerBox =
            (content as LayoutElementBuilders.Box).contents[0] as LayoutElementBuilders.Box
        val background = requireNotNull(innerBox.modifiers!!.background)

        assertThat(background.color!!.argb)
            .isEqualTo(android.graphics.Color.rgb(0xC0, 0xFF, 0xEE))
    }

    @Test
    fun canTranslateRow() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Box(contentAlignment = Alignment.TopCenter) {}
                Box(contentAlignment = Alignment.BottomEnd) {}
            }
        }.layout

        val innerRow =
            (content as LayoutElementBuilders.Box).contents[0] as LayoutElementBuilders.Row

        assertThat(innerRow.verticalAlignment!!.value).isEqualTo(VERTICAL_ALIGN_CENTER)

        val leaf0 = innerRow.contents[0] as LayoutElementBuilders.Box
        val leaf1 = innerRow.contents[1] as LayoutElementBuilders.Box

        assertThat(leaf0.verticalAlignment!!.value).isEqualTo(VERTICAL_ALIGN_TOP)
        assertThat(leaf0.horizontalAlignment!!.value).isEqualTo(HORIZONTAL_ALIGN_CENTER)

        assertThat(leaf1.verticalAlignment!!.value).isEqualTo(VERTICAL_ALIGN_BOTTOM)
        assertThat(leaf1.horizontalAlignment!!.value).isEqualTo(HORIZONTAL_ALIGN_END)
    }

    @Test
    fun rowWithHorizontalAlignmentInflatesInColumn() = fakeCoroutineScope.runBlockingTest {
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
            (content as LayoutElementBuilders.Box).contents[0] as LayoutElementBuilders.Column
        val innerRow = innerColumn.contents[0] as LayoutElementBuilders.Row

        assertThat(innerColumn.horizontalAlignment!!.value).isEqualTo(HORIZONTAL_ALIGN_CENTER)

        // Column should inherit the size of the inner Row
        assertIs<DimensionBuilders.ExpandedDimensionProp>(innerColumn.width)
        assertThat((innerColumn.height as DimensionBuilders.DpProp).value).isEqualTo(100f)

        // Column should also inherit the modifiers
        assertThat(innerColumn.modifiers!!.background!!.color!!.argb).isEqualTo(0x11223344)

        // The row should have a wrapped width, but still use the height
        assertIs<DimensionBuilders.WrappedDimensionProp>(innerRow.width)
        assertThat((innerRow.height as DimensionBuilders.DpProp).value).isEqualTo(100f)

        // And no modifiers.
        assertThat(innerRow.modifiers).isNull()

        // Should have the vertical alignment set still though
        assertThat(innerRow.verticalAlignment!!.value).isEqualTo(VERTICAL_ALIGN_CENTER)
    }

    @Test
    fun canTranslateColumn() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                Box(contentAlignment = Alignment.TopCenter) {}
                Box(contentAlignment = Alignment.BottomEnd) {}
            }
        }.layout

        val innerColumn =
            (content as LayoutElementBuilders.Box).contents[0] as LayoutElementBuilders.Column

        assertThat(innerColumn.horizontalAlignment!!.value).isEqualTo(HORIZONTAL_ALIGN_CENTER)

        val leaf0 = innerColumn.contents[0] as LayoutElementBuilders.Box
        val leaf1 = innerColumn.contents[1] as LayoutElementBuilders.Box

        assertThat(leaf0.verticalAlignment!!.value).isEqualTo(VERTICAL_ALIGN_TOP)
        assertThat(leaf0.horizontalAlignment!!.value).isEqualTo(HORIZONTAL_ALIGN_CENTER)

        assertThat(leaf1.verticalAlignment!!.value).isEqualTo(VERTICAL_ALIGN_BOTTOM)
        assertThat(leaf1.horizontalAlignment!!.value).isEqualTo(HORIZONTAL_ALIGN_END)
    }

    @Test
    fun columnWithVerticalAlignmentInflatesInRow() = fakeCoroutineScope.runBlockingTest {
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
            (content as LayoutElementBuilders.Box).contents[0] as LayoutElementBuilders.Row
        val innerColumn = innerRow.contents[0] as LayoutElementBuilders.Column

        assertThat(innerRow.verticalAlignment!!.value).isEqualTo(VERTICAL_ALIGN_CENTER)

        // Row should inherit the size of the inner Row
        assertThat((innerRow.width as DimensionBuilders.DpProp).value).isEqualTo(100f)
        assertIs<DimensionBuilders.ExpandedDimensionProp>(innerRow.height)

        // Row should also inherit the modifiers
        assertThat(innerRow.modifiers!!.background!!.color!!.argb).isEqualTo(0x11223344)

        // The Column should have a wrapped width, but still use the height
        assertThat((innerColumn.width as DimensionBuilders.DpProp).value).isEqualTo(100f)
        assertIs<DimensionBuilders.WrappedDimensionProp>(innerColumn.height)

        // And no modifiers.
        assertThat(innerColumn.modifiers).isNull()

        // Should have the horizontal alignment set still though
        assertThat(innerColumn.horizontalAlignment!!.value).isEqualTo(HORIZONTAL_ALIGN_CENTER)
    }

    @Test
    fun canInflateText() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            val style = TextStyle(
                color = ColorProvider(Color.Gray),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                textDecoration = TextDecoration.Underline
            )
            Text("Hello World", modifier = GlanceModifier.padding(1.dp), style = style)
        }.layout

        val innerText = (content as LayoutElementBuilders.Box).contents[0]
            as LayoutElementBuilders.Text

        assertThat(innerText.text!!.value).isEqualTo("Hello World")
        assertThat(innerText.fontStyle!!.color!!.argb).isEqualTo(Color.Gray.toArgb())
        assertThat(innerText.fontStyle!!.size!!.value).isEqualTo(16f)
        assertThat(innerText.fontStyle!!.italic!!.value).isTrue()
        assertThat(innerText.fontStyle!!.weight!!.value).isEqualTo(FONT_WEIGHT_BOLD)
        assertThat(innerText.fontStyle!!.underline!!.value).isTrue()
    }

    @Test
    fun textWithSizeInflatesInBox() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            Text("Hello World", modifier = GlanceModifier.size(100.dp).padding(10.dp))
        }.layout

        val innerBox = (content as LayoutElementBuilders.Box).contents[0] as
            LayoutElementBuilders.Box
        val innerText = innerBox.contents[0] as LayoutElementBuilders.Text

        assertThat(innerBox.width is DimensionBuilders.DpProp)
        assertThat((innerBox.width as DimensionBuilders.DpProp).value).isEqualTo(100f)
        assertThat(innerBox.height is DimensionBuilders.DpProp)
        assertThat((innerBox.height as DimensionBuilders.DpProp).value).isEqualTo(100f)

        // Modifiers should apply to the Box
        assertThat(innerBox.modifiers!!.padding).isNotNull()

        // ... and not to the Text
        assertThat(innerText.modifiers?.padding).isNull()
    }

    @Test
    fun canTranslateCurvedRow() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            CurvedRow(
                anchorDegrees = 20f,
                radialAlignment = RadialAlignment.Inner,
                anchorType = AnchorType.End,
                modifier = GlanceModifier.padding(20.dp)
            ) {}
        }.layout

        val innerArc = (content as LayoutElementBuilders.Box).contents[0]
            as LayoutElementBuilders.Arc

        // Remember, 0 degrees is handled differently in Glance (3 o clock) and Tiles (12 o clock).
        assertThat(innerArc.anchorAngle!!.value).isEqualTo(110f)
        assertThat(innerArc.anchorType!!.value).isEqualTo(ARC_ANCHOR_END)
        assertThat(innerArc.modifiers!!.padding).isNotNull()
        assertThat(innerArc.verticalAlign!!.value).isEqualTo(VERTICAL_ALIGN_BOTTOM)
    }

    @Test
    fun curvedRowWithSizeInflatesInBox() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            CurvedRow(
                anchorDegrees = 20f,
                radialAlignment = RadialAlignment.Inner,
                anchorType = AnchorType.End,
                modifier = GlanceModifier.padding(20.dp).size(10.dp)
            ) {}
        }.layout

        val innerBox = (content as LayoutElementBuilders.Box).contents[0]
            as LayoutElementBuilders.Box
        val innerArc = innerBox.contents[0] as LayoutElementBuilders.Arc

        assertThat(innerBox.width is DimensionBuilders.DpProp)
        assertThat((innerBox.width as DimensionBuilders.DpProp).value).isEqualTo(10f)
        assertThat(innerBox.height is DimensionBuilders.DpProp)
        assertThat((innerBox.height as DimensionBuilders.DpProp).value).isEqualTo(10f)

        // Modifiers should apply to the Box
        assertThat(innerBox.modifiers!!.padding).isNotNull()

        // ... and not to the Arc
        assertThat(innerArc.modifiers?.padding).isNull()
    }

    @Test
    fun canTranslateCurvedText() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            val style = CurvedTextStyle(
                color = ColorProvider(R.color.color1),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
            )

            CurvedRow {
                CurvedText(text = "Hello World", textStyle = style)
            }
        }.layout

        val innerArc = (content as LayoutElementBuilders.Box).contents[0]
            as LayoutElementBuilders.Arc
        val innerArcText = innerArc.contents[0] as LayoutElementBuilders.ArcText

        assertThat(innerArcText.text!!.value).isEqualTo("Hello World")
        assertThat(innerArcText.fontStyle!!.color!!.argb)
            .isEqualTo(android.graphics.Color.rgb(0xC0, 0xFF, 0xEE))
        assertThat(innerArcText.fontStyle!!.size!!.value).isEqualTo(16f)
        assertThat(innerArcText.fontStyle!!.italic!!.value).isTrue()
        assertThat(innerArcText.fontStyle!!.weight!!.value).isEqualTo(FONT_WEIGHT_BOLD)
    }

    @Test
    fun canTranslateAndroidLayoutElement() = fakeCoroutineScope.runBlockingTest {
        val providedLayoutElement =
            LayoutElementBuilders.Text.Builder().setText("Android Layout Element").build()

        val content = runAndTranslate {
            AndroidLayoutElement(providedLayoutElement)
        }.layout

        val box = assertIs<LayoutElementBuilders.Box>(content)
        val textElement = assertIs<LayoutElementBuilders.Text>(box.contents[0])
        assertThat(textElement.text!!.value).isEqualTo("Android Layout Element")
    }

    @Test
    fun otherElementInArcInflatesInArcAdapter() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            CurvedRow {
                Box {}
            }
        }.layout

        val innerArc = (content as LayoutElementBuilders.Box).contents[0]
            as LayoutElementBuilders.Arc
        val innerArcAdapter = innerArc.contents[0] as LayoutElementBuilders.ArcAdapter
        assertIs<LayoutElementBuilders.Box>(innerArcAdapter.content)
    }

    @Test
    fun canInflateLaunchAction() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            Text(
                modifier = GlanceModifier.clickable(actionLaunchActivity(TestActivity::class.java)),
                text = "Hello World"
            )
        }.layout

        val innerText = (content as LayoutElementBuilders.Box).contents[0] as
            LayoutElementBuilders.Text

        assertThat(innerText.modifiers!!.clickable).isNotNull()
        assertThat(innerText.modifiers!!.clickable!!.onClick)
            .isInstanceOf(ActionBuilders.LaunchAction::class.java)

        val launchAction = innerText.modifiers!!.clickable!!.onClick as ActionBuilders.LaunchAction
        assertThat(launchAction.androidActivity).isNotNull()

        val packageName = getApplicationContext<Context>().packageName
        assertThat(launchAction.androidActivity!!.packageName).isEqualTo(packageName)
        assertThat(launchAction.androidActivity!!.className)
            .isEqualTo(TestActivity::class.qualifiedName)
    }

    @Test
    fun canTranslateButton() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            val style = TextStyle(
                color = ColorProvider(Color.Magenta),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                textDecoration = TextDecoration.Underline
            )
            Button(
                "Hello World",
                onClick = actionLaunchActivity(TestActivity::class.java),
                modifier = GlanceModifier.padding(1.dp),
                style = style
            )
        }.layout

        val box = assertIs<LayoutElementBuilders.Box>(content)
        val innerText = assertIs<LayoutElementBuilders.Text>(box.contents[0])

        assertThat(innerText.text!!.value).isEqualTo("Hello World")

        assertThat(innerText.fontStyle!!.color!!.argb).isEqualTo(Color.Magenta.toArgb())
        assertThat(innerText.fontStyle!!.size!!.value).isEqualTo(16f)
        assertThat(innerText.fontStyle!!.italic!!.value).isTrue()
        assertThat(innerText.fontStyle!!.weight!!.value).isEqualTo(FONT_WEIGHT_BOLD)
        assertThat(innerText.fontStyle!!.underline!!.value).isTrue()

        assertThat(innerText.modifiers!!.clickable).isNotNull()
        assertThat(innerText.modifiers!!.clickable!!.onClick)
            .isInstanceOf(ActionBuilders.LaunchAction::class.java)
    }

    @Test
    fun canTranslateSpacer() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            Spacer(GlanceModifier.width(10.dp))
            Spacer(GlanceModifier.height(15.dp))
            Spacer(GlanceModifier.size(8.dp, 12.dp))
        }.layout

        val spacerWithWidth = (content as LayoutElementBuilders.Box).contents[0] as
            LayoutElementBuilders.Spacer
        assertThat((spacerWithWidth.width as DimensionBuilders.DpProp).value).isEqualTo(10f)

        val spacerWithHeight = content.contents[1] as LayoutElementBuilders.Spacer
        assertThat((spacerWithHeight.height as DimensionBuilders.DpProp).value).isEqualTo(15f)

        val spacerWithSize = content.contents[2] as LayoutElementBuilders.Spacer
        assertThat((spacerWithSize.width as DimensionBuilders.DpProp).value).isEqualTo(8f)
        assertThat((spacerWithSize.height as DimensionBuilders.DpProp).value).isEqualTo(12f)
    }

    @Test
    fun canTranslateImage() = fakeCoroutineScope.runBlockingTest {
        val compositionResult = runAndTranslate {
          Image(
              provider = ImageProvider(R.drawable.oval),
              contentDescription = "Oval",
              modifier = GlanceModifier.width(R.dimen.dimension1).height(R.dimen.dimension2),
              contentScale = ContentScale.FillBounds
          )
        }
        val content = compositionResult.layout
        val resources = compositionResult.resources

        val image = (content as LayoutElementBuilders.Box).contents[0] as
            LayoutElementBuilders.Image

        val context = getApplicationContext<Context>()
        assertThat((image.width as DimensionBuilders.DpProp).value).isEqualTo(
            context.resources.getDimension(R.dimen.dimension1)
        )
        assertThat((image.height as DimensionBuilders.DpProp).value).isEqualTo(
                context.resources.getDimension(R.dimen.dimension2)
        )
        assertThat(image.contentScaleMode!!.value).isEqualTo(
            LayoutElementBuilders.CONTENT_SCALE_MODE_FILL_BOUNDS
        )
        assertThat(image.resourceId!!.value).isEqualTo("android_" + R.drawable.oval)

        assertThat(
            resources.build().idToImageMapping.containsKey("android_" + R.drawable.oval)
        ).isTrue()

        assertThat(image.modifiers!!.semantics!!.contentDescription).isEqualTo("Oval")
    }

    @Test
    fun setSizeFromResource() = fakeCoroutineScope.runBlockingTest {
        val content = runAndTranslate {
            Column(
                modifier = GlanceModifier.width(R.dimen.dimension1)
                    .height(R.dimen.dimension2)
            ) {}
        }.layout

        val innerColumn =
            (content as LayoutElementBuilders.Box).contents[0] as LayoutElementBuilders.Column
        val context = getApplicationContext<Context>()

        // Row should inherit the size of the inner Row
        assertThat((innerColumn.width as DimensionBuilders.DpProp).value).isEqualTo(
            context.resources.getDimension(R.dimen.dimension1)
        )
        assertThat((innerColumn.height as DimensionBuilders.DpProp).value).isEqualTo(
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
