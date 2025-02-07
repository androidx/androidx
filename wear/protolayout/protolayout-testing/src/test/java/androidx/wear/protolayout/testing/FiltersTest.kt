/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.protolayout.testing

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.ColorBuilders.ColorProp
import androidx.wear.protolayout.DimensionBuilders.ProportionalDimensionProp
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.ColorFilter
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.ElementMetadata
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Semantics
import androidx.wear.protolayout.TypeBuilders.StringProp
import androidx.wear.protolayout.expression.DynamicBuilders
import androidx.wear.protolayout.expression.dynamicDataMapOf
import androidx.wear.protolayout.expression.intAppDataKey
import androidx.wear.protolayout.expression.mapTo
import androidx.wear.protolayout.layout.basicText
import androidx.wear.protolayout.modifiers.loadAction
import androidx.wear.protolayout.types.LayoutString
import androidx.wear.protolayout.types.asLayoutConstraint
import androidx.wear.protolayout.types.layoutString
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(AndroidJUnit4::class)
@DoNotInstrument
class FiltersTest {
    @Test
    fun notClickable() {
        val testElement = Box.Builder().setModifiers(Modifiers.Builder().build()).build()

        assertThat(isClickable().matches(testElement)).isFalse()
    }

    @Test
    fun clickable() {
        val testElement =
            Box.Builder()
                .setModifiers(Modifiers.Builder().setClickable(Clickable.Builder().build()).build())
                .build()

        assertThat(isClickable().matches(testElement)).isTrue()
    }

    @Test
    fun hasClickable_matches() {
        val clickable = Clickable.Builder().setOnClick(loadAction()).build()
        val testElement =
            Column.Builder()
                .setModifiers(Modifiers.Builder().setClickable(clickable).build())
                .build()

        assertThat(hasClickable().matches(testElement)).isTrue()
    }

    @Test
    fun hasClickable_withStateMap_doesNotMatch() {
        val testElement =
            Column.Builder()
                .setModifiers(
                    Modifiers.Builder()
                        .setClickable(Clickable.Builder().setOnClick(loadAction()).build())
                        .build()
                )
                .build()

        assertThat(
                hasClickable(action = loadAction(dynamicDataMapOf(intAppDataKey("key") mapTo 42)))
                    .matches(testElement)
            )
            .isFalse()
    }

    @Test
    fun hasContentDescription() {
        val description = "random test description"
        val testElement =
            Spacer.Builder()
                .setModifiers(
                    Modifiers.Builder()
                        .setSemantics(
                            Semantics.Builder()
                                .setContentDescription(StringProp.Builder(description).build())
                                .build()
                        )
                        .build()
                )
                .build()

        assertThat(hasContentDescription(description).matches(testElement)).isTrue()
        assertThat(hasContentDescription("blabla").matches(testElement)).isFalse()
        assertThat(
                hasContentDescription(Regex(".*TEST.*", RegexOption.IGNORE_CASE))
                    .matches(testElement)
            )
            .isTrue()
    }

    @Test
    fun hasTag() {
        val tag = "random test tag"
        val testElement =
            Row.Builder()
                .setModifiers(
                    Modifiers.Builder()
                        .setMetadata(
                            ElementMetadata.Builder().setTagData(tag.toByteArray()).build()
                        )
                        .build()
                )
                .build()

        assertThat(hasTag(tag).matches(testElement)).isTrue()
        assertThat(containsTag("test").matches(testElement)).isTrue()
    }

    @Test
    fun hasText() {
        val textContent = "random test content"
        val testElement = basicText(textContent.layoutString)

        assertThat(hasText(textContent).matches(testElement)).isTrue()
        assertThat(hasText("blabla").matches(testElement)).isFalse()
    }

    @Test
    fun hasDynamicText() {
        val textContent =
            LayoutString(
                "static content",
                DynamicBuilders.DynamicString.constant("dynamic content"),
                "static content".asLayoutConstraint()
            )
        val testElement = basicText(textContent)

        assertThat(hasText(textContent).matches(testElement)).isTrue()
        assertThat(hasText("blabla").matches(testElement)).isFalse()
    }

    @Test
    fun hasImage() {
        val resId = "randomRes"
        val testElement = Image.Builder().setResourceId(resId).build()

        assertThat(hasImage(resId).matches(testElement)).isTrue()
        assertThat(hasImage("blabla").matches(testElement)).isFalse()
    }

    @Test
    fun hasColor_onBackground() {
        val testBox =
            Box.Builder()
                .setModifiers(
                    Modifiers.Builder()
                        .setBackground(
                            Background.Builder()
                                .setColor(ColorProp.Builder(Color.BLUE).build())
                                .build()
                        )
                        .build()
                )
                .build()

        assertThat(hasColor(Color.BLUE).matches(testBox)).isTrue()
        assertThat(hasColor(Color.GREEN).matches(testBox)).isFalse()
    }

    @Test
    fun hasColor_onTextStyle() {
        val testText =
            basicText(
                "text".layoutString,
                fontStyle =
                    FontStyle.Builder().setColor(ColorProp.Builder(Color.CYAN).build()).build()
            )

        assertThat(hasColor(Color.CYAN).matches(testText)).isTrue()
        assertThat(hasColor(Color.GREEN).matches(testText)).isFalse()
    }

    @Test
    fun hasColor_onImageTint() {
        val testImage =
            Image.Builder()
                .setResourceId("resId")
                .setColorFilter(
                    ColorFilter.Builder().setTint(ColorProp.Builder(Color.MAGENTA).build()).build()
                )
                .build()

        assertThat(hasColor(Color.MAGENTA).matches(testImage)).isTrue()
        assertThat(hasColor(Color.GREEN).matches(testImage)).isFalse()
    }

    @Test
    fun hasSize_box() {
        val width1 = dp(20F)
        val height1 = dp(30F)
        val testBox1 = Box.Builder().setWidth(width1).setHeight(height1).build()
        val width2 = expand()
        val height2 = wrap()
        val testBox2 = Box.Builder().setWidth(width2).setHeight(height2).build()

        assertThat(hasWidth(width1).matches(testBox1)).isTrue()
        assertThat(hasHeight(height1).matches(testBox1)).isTrue()
        assertThat(hasWidth(width2).matches(testBox2)).isTrue()
        assertThat(hasHeight(height2).matches(testBox2)).isTrue()

        assertThat(hasWidth(width2).matches(testBox1)).isFalse()
        assertThat(hasHeight(width1).matches(testBox1)).isFalse()
        assertThat(hasWidth(height1).matches(testBox2)).isFalse()
        assertThat(hasHeight(width2).matches(testBox2)).isFalse()
    }

    @Test
    fun hasSize_column() {
        val width1 = dp(20F)
        val height1 = dp(30F)
        val testColumn1 = Column.Builder().setWidth(width1).setHeight(height1).build()
        val width2 = expand()
        val height2 = wrap()
        val testColumn2 = Column.Builder().setWidth(width2).setHeight(height2).build()

        assertThat(hasWidth(width1).matches(testColumn1)).isTrue()
        assertThat(hasHeight(height1).matches(testColumn1)).isTrue()
        assertThat(hasWidth(width2).matches(testColumn2)).isTrue()
        assertThat(hasHeight(height2).matches(testColumn2)).isTrue()

        assertThat(hasWidth(width2).matches(testColumn1)).isFalse()
        assertThat(hasHeight(width1).matches(testColumn1)).isFalse()
        assertThat(hasWidth(height1).matches(testColumn2)).isFalse()
        assertThat(hasHeight(width2).matches(testColumn2)).isFalse()
    }

    @Test
    fun hasSize_row() {
        val width1 = dp(20F)
        val height1 = dp(30F)
        val testRow1 = Row.Builder().setWidth(width1).setHeight(height1).build()
        val width2 = expand()
        val height2 = wrap()
        val testRow2 = Row.Builder().setWidth(width2).setHeight(height2).build()

        assertThat(hasWidth(width1).matches(testRow1)).isTrue()
        assertThat(hasHeight(height1).matches(testRow1)).isTrue()
        assertThat(hasWidth(width2).matches(testRow2)).isTrue()
        assertThat(hasHeight(height2).matches(testRow2)).isTrue()

        assertThat(hasWidth(width2).matches(testRow1)).isFalse()
        assertThat(hasHeight(width1).matches(testRow1)).isFalse()
        assertThat(hasWidth(height1).matches(testRow2)).isFalse()
        assertThat(hasHeight(width2).matches(testRow2)).isFalse()
    }

    @Test
    fun hasSize_image() {
        val width1 = dp(20F)
        val height1 = expand()
        val testImage1 =
            Image.Builder().setResourceId("id").setWidth(width1).setHeight(height1).build()
        val width2 = ProportionalDimensionProp.Builder().setAspectRatioWidth(15).build()
        val height2 = ProportionalDimensionProp.Builder().setAspectRatioHeight(5).build()
        val testImage2 =
            Image.Builder().setResourceId("id").setWidth(width2).setHeight(height2).build()

        assertThat(hasWidth(width1).matches(testImage1)).isTrue()
        assertThat(hasHeight(height1).matches(testImage1)).isTrue()
        assertThat(hasWidth(width2).matches(testImage2)).isTrue()
        assertThat(hasHeight(height2).matches(testImage2)).isTrue()

        assertThat(hasWidth(width2).matches(testImage1)).isFalse()
        assertThat(hasHeight(width1).matches(testImage1)).isFalse()
        assertThat(hasWidth(height1).matches(testImage2)).isFalse()
        assertThat(hasHeight(width2).matches(testImage2)).isFalse()
    }

    @Test
    fun hasSize_spacer() {
        val width1 = dp(20F)
        val height1 = dp(30F)
        val testImage1 = Spacer.Builder().setWidth(width1).setHeight(height1).build()
        val width2 = expand()
        val height2 = expand()
        val testImage2 = Spacer.Builder().setWidth(width2).setHeight(height2).build()

        assertThat(hasWidth(width1).matches(testImage1)).isTrue()
        assertThat(hasHeight(height1).matches(testImage1)).isTrue()
        assertThat(hasWidth(width2).matches(testImage2)).isTrue()
        assertThat(hasHeight(height2).matches(testImage2)).isTrue()

        assertThat(hasWidth(width2).matches(testImage1)).isFalse()
        assertThat(hasHeight(width1).matches(testImage1)).isFalse()
        assertThat(hasWidth(height1).matches(testImage2)).isFalse()
        assertThat(hasHeight(width1).matches(testImage2)).isFalse()
    }

    @Test
    fun hasChild() {
        val width = dp(20F)
        val testLayout =
            Box.Builder()
                .addContent(
                    Row.Builder()
                        .setModifiers(
                            Modifiers.Builder().setClickable(Clickable.Builder().build()).build()
                        )
                        .addContent(Image.Builder().setResourceId("image").build())
                        .build()
                )
                .addContent(
                    Column.Builder()
                        .setWidth(width)
                        .addContent(basicText("text".layoutString))
                        .build()
                )
                .build()

        assertThat(hasChild(isClickable()).matches(testLayout)).isTrue()
        assertThat(hasChild(hasWidth(width)).matches(testLayout)).isTrue()
        assertThat(hasChild(hasImage("image") or isClickable()).matches(testLayout)).isTrue()
        assertThat(hasChild(hasImage("image")).matches(testLayout.children[0])).isTrue()
        assertThat(hasChild(hasText("text")).matches(testLayout.children[1])).isTrue()
        assertThat(hasChild(hasImage("image")).matches(testLayout)).isFalse()
        assertThat(hasChild(hasText("text")).matches(testLayout)).isFalse()
    }

    @Test
    fun hasDescendant() {
        val width = dp(20F)
        val testLayout =
            Box.Builder()
                .addContent(
                    Row.Builder()
                        .setModifiers(
                            Modifiers.Builder().setClickable(Clickable.Builder().build()).build()
                        )
                        .addContent(Image.Builder().setResourceId("image").build())
                        .build()
                )
                .addContent(
                    Column.Builder()
                        .setWidth(width)
                        .addContent(basicText("text".layoutString))
                        .build()
                )
                .build()

        assertThat(hasDescendant(isClickable()).matches(testLayout)).isTrue()
        assertThat(hasDescendant(hasWidth(width)).matches(testLayout)).isTrue()
        assertThat(hasDescendant(hasImage("image")).matches(testLayout)).isTrue()
        assertThat(hasDescendant(hasText("text")).matches(testLayout)).isTrue()
        assertThat(hasDescendant(hasImage("image") and isClickable()).matches(testLayout)).isFalse()
    }
}
