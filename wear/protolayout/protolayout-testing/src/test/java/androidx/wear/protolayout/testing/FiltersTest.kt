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
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.ElementMetadata
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Semantics
import androidx.wear.protolayout.TypeBuilders.StringProp
import androidx.wear.protolayout.expression.AppDataKey
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicColor
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.expression.PlatformEventSources
import androidx.wear.protolayout.expression.PlatformHealthSources
import androidx.wear.protolayout.expression.PlatformHealthSources.Keys
import androidx.wear.protolayout.expression.dynamicDataMapOf
import androidx.wear.protolayout.expression.intAppDataKey
import androidx.wear.protolayout.expression.mapTo
import androidx.wear.protolayout.layout.basicText
import androidx.wear.protolayout.modifiers.loadAction
import androidx.wear.protolayout.types.LayoutString
import androidx.wear.protolayout.types.asLayoutConstraint
import androidx.wear.protolayout.types.asLayoutString
import androidx.wear.protolayout.types.layoutString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(AndroidJUnit4::class)
@DoNotInstrument
class FiltersTest {
    @Test
    fun notClickable() {
        val testElement = Box.Builder().setModifiers(Modifiers.Builder().build()).build()

        assert(isClickable().not().matches(testElement))
    }

    @Test
    fun clickable() {
        val testElement =
            Box.Builder()
                .setModifiers(Modifiers.Builder().setClickable(Clickable.Builder().build()).build())
                .build()

        assert(isClickable().matches(testElement))
    }

    @Test
    fun hasClickable_matches() {
        val clickable = Clickable.Builder().setOnClick(loadAction()).build()
        val testElement =
            Column.Builder()
                .setModifiers(Modifiers.Builder().setClickable(clickable).build())
                .build()

        assert(hasClickable().matches(testElement))
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

        assert(
            hasClickable(action = loadAction(dynamicDataMapOf(intAppDataKey("key") mapTo 42)))
                .not()
                .matches(testElement)
        )
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

        assert(hasContentDescription(description).matches(testElement))
        assert(hasContentDescription("blabla").not().matches(testElement))
        assert(
            hasContentDescription(Regex(".*TEST.*", RegexOption.IGNORE_CASE)).matches(testElement)
        )
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

        assert(hasTag(tag).matches(testElement))
        assert(containsTag("test").matches(testElement))
    }

    @Test
    fun hasText() {
        val textContent = "random test content"
        val testElement = basicText(textContent.layoutString)

        assert(hasText(textContent).matches(testElement))
        assert(hasText("blabla").not().matches(testElement))
    }

    @Test
    fun hasDynamicText() {
        val staticContent = "static content"
        val textContent =
            DynamicString.constant("dynamic content")
                .asLayoutString(staticContent, staticContent.asLayoutConstraint())
        val testElement = basicText(textContent)

        assert(hasText("dynamic content").matches(testElement))
        assert(hasText(staticContent).not().matches(testElement))
    }

    @Test
    fun hasDynamicTextFromPlatformData() {
        val staticContent = "static content"
        val textContent =
            PlatformHealthSources.heartRateBpm()
                .format()
                .asLayoutString(staticContent, staticContent.asLayoutConstraint())
        val testElement = basicText(textContent)
        val heartRateValue = 76.5F

        assert(
            hasText("$heartRateValue")
                .matches(
                    testElement,
                    TestContext(dynamicDataMapOf(Keys.HEART_RATE_BPM mapTo heartRateValue)),
                )
        )

        // when the dynamic data evaluation fails, due to lack of data in the pipeline, fall back to
        // use the static text
        assert(hasText(staticContent).matches(testElement))
    }

    @Test
    fun hasDynamicTextFromPlatformEvent() {
        val staticContent = "static content"
        val visibleContent = "visible"
        val invisibleContent = "invisible"
        val textContent =
            LayoutString(
                staticContent,
                DynamicString.onCondition(PlatformEventSources.isLayoutVisible())
                    .use(visibleContent)
                    .elseUse(invisibleContent),
                staticContent.asLayoutConstraint(),
            )
        val testElement = basicText(textContent)

        assert(
            hasText(visibleContent)
                .matches(
                    testElement,
                    TestContext(
                        dynamicDataMapOf(PlatformEventSources.Keys.LAYOUT_VISIBILITY mapTo true)
                    ),
                )
        )

        assert(
            hasText(invisibleContent)
                .matches(
                    testElement,
                    TestContext(
                        dynamicDataMapOf(PlatformEventSources.Keys.LAYOUT_VISIBILITY mapTo false)
                    ),
                )
        )

        // when the dynamic data evaluation fails, due to lack of data in the pipeline, fall back to
        // use the static text
        assert(hasText(staticContent).matches(testElement))
    }

    @Test
    @Suppress("deprecation")
    fun hasImage() {
        val resId = "randomRes"
        val testElement = Image.Builder().setResourceId(resId).build()

        assert(hasImage(resId).matches(testElement))
        assert(hasImage("blabla").not().matches(testElement))
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

        assert(hasColor(Color.BLUE).matches(testBox))
        assert(hasColor(Color.GREEN).not().matches(testBox))
    }

    @Test
    fun hasDynamicColor_onBackground() {
        val stateKey = AppDataKey<DynamicColor>("color")
        val testBox =
            Box.Builder()
                .setModifiers(
                    Modifiers.Builder()
                        .setBackground(
                            Background.Builder()
                                .setColor(
                                    ColorProp.Builder(Color.BLUE)
                                        .setDynamicValue(DynamicColor.from(stateKey))
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()

        assert(hasColor(Color.BLUE).matches(testBox))
        assert(
            hasColor(Color.MAGENTA)
                .matches(
                    testBox,
                    TestContext(dynamicDataMapOf(stateKey mapTo Color.valueOf(Color.MAGENTA))),
                )
        )
        assert(
            hasColor(Color.CYAN)
                .matches(
                    testBox,
                    TestContext(dynamicDataMapOf(stateKey mapTo Color.valueOf(Color.CYAN))),
                )
        )
    }

    @Test
    fun hasColor_onTextStyle() {
        val testText =
            basicText(
                "text".layoutString,
                fontStyle =
                    FontStyle.Builder().setColor(ColorProp.Builder(Color.CYAN).build()).build(),
            )

        assert(hasColor(Color.CYAN).matches(testText))
        assert(hasColor(Color.GREEN).not().matches(testText))
    }

    @Test
    @Suppress("deprecation")
    fun hasColor_onImageTint() {
        val testImage =
            Image.Builder()
                .setResourceId("resId")
                .setColorFilter(
                    ColorFilter.Builder().setTint(ColorProp.Builder(Color.MAGENTA).build()).build()
                )
                .build()

        assert(hasColor(Color.MAGENTA).matches(testImage))
        assert(hasColor(Color.GREEN).not().matches(testImage))
    }

    @Test
    fun hasSize_box() {
        val width1 = dp(20F)
        val height1 = dp(30F)
        val testBox1 = Box.Builder().setWidth(width1).setHeight(height1).build()
        val width2 = expand()
        val height2 = wrap()
        val testBox2 = Box.Builder().setWidth(width2).setHeight(height2).build()

        assert(hasWidth(width1).matches(testBox1))
        assert(hasHeight(height1).matches(testBox1))
        assert(hasWidth(width2).matches(testBox2))
        assert(hasHeight(height2).matches(testBox2))

        assert(hasWidth(width2).not().matches(testBox1))
        assert(hasHeight(width1).not().matches(testBox1))
        assert(hasWidth(height1).not().matches(testBox2))
        assert(hasHeight(width2).not().matches(testBox2))
    }

    @Test
    fun hasSize_column() {
        val width1 = dp(20F)
        val height1 = dp(30F)
        val testColumn1 = Column.Builder().setWidth(width1).setHeight(height1).build()
        val width2 = expand()
        val height2 = wrap()
        val testColumn2 = Column.Builder().setWidth(width2).setHeight(height2).build()

        assert(hasWidth(width1).matches(testColumn1))
        assert(hasHeight(height1).matches(testColumn1))
        assert(hasWidth(width2).matches(testColumn2))
        assert(hasHeight(height2).matches(testColumn2))

        assert(hasWidth(width2).not().matches(testColumn1))
        assert(hasHeight(width1).not().matches(testColumn1))
        assert(hasWidth(height1).not().matches(testColumn2))
        assert(hasHeight(width2).not().matches(testColumn2))
    }

    @Test
    fun hasSize_row() {
        val width1 = dp(20F)
        val height1 = dp(30F)
        val testRow1 = Row.Builder().setWidth(width1).setHeight(height1).build()
        val width2 = expand()
        val height2 = wrap()
        val testRow2 = Row.Builder().setWidth(width2).setHeight(height2).build()

        assert(hasWidth(width1).matches(testRow1))
        assert(hasHeight(height1).matches(testRow1))
        assert(hasWidth(width2).matches(testRow2))
        assert(hasHeight(height2).matches(testRow2))

        assert(hasWidth(width2).not().matches(testRow1))
        assert(hasHeight(width1).not().matches(testRow1))
        assert(hasWidth(height1).not().matches(testRow2))
        assert(hasHeight(width2).not().matches(testRow2))
    }

    @Test
    @Suppress("deprecation")
    fun hasSize_image() {
        val width1 = dp(20F)
        val height1 = expand()
        val testImage1 =
            Image.Builder().setResourceId("id").setWidth(width1).setHeight(height1).build()
        val width2 = ProportionalDimensionProp.Builder().setAspectRatioWidth(15).build()
        val height2 = ProportionalDimensionProp.Builder().setAspectRatioHeight(5).build()
        val testImage2 =
            Image.Builder().setResourceId("id").setWidth(width2).setHeight(height2).build()

        assert(hasWidth(width1).matches(testImage1))
        assert(hasHeight(height1).matches(testImage1))
        assert(hasWidth(width2).matches(testImage2))
        assert(hasHeight(height2).matches(testImage2))

        assert(hasWidth(width2).not().matches(testImage1))
        assert(hasHeight(width1).not().matches(testImage1))
        assert(hasWidth(height1).not().matches(testImage2))
        assert(hasHeight(width2).not().matches(testImage2))
    }

    @Test
    fun hasSize_spacer() {
        val width1 = dp(20F)
        val height1 = dp(30F)
        val testImage1 = Spacer.Builder().setWidth(width1).setHeight(height1).build()
        val width2 = expand()
        val height2 = expand()
        val testImage2 = Spacer.Builder().setWidth(width2).setHeight(height2).build()

        assert(hasWidth(width1).matches(testImage1))
        assert(hasHeight(height1).matches(testImage1))
        assert(hasWidth(width2).matches(testImage2))
        assert(hasHeight(height2).matches(testImage2))

        assert(hasWidth(width2).not().matches(testImage1))
        assert(hasHeight(width1).not().matches(testImage1))
        assert(hasWidth(height1).not().matches(testImage2))
        assert(hasHeight(width1).not().matches(testImage2))
    }

    @Test
    @Suppress("deprecation")
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

        assert(hasChild(isClickable()).matches(testLayout))
        assert(hasChild(hasWidth(width)).matches(testLayout))
        assert(hasChild(hasImage("image") or isClickable()).matches(testLayout))
        assert(hasChild(hasImage("image")).matches(testLayout.children[0]))
        assert(hasChild(hasText("text")).matches(testLayout.children[1]))
        assert(hasChild(hasImage("image")).not().matches(testLayout))
        assert(hasChild(hasText("text")).not().matches(testLayout))
    }

    @Test
    @Suppress("deprecation")
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

        assert(hasDescendant(isClickable()).matches(testLayout))
        assert(hasDescendant(hasWidth(width)).matches(testLayout))
        assert(hasDescendant(hasImage("image")).matches(testLayout))
        assert(hasDescendant(hasText("text")).matches(testLayout))
        assert(hasDescendant(hasImage("image") and isClickable()).not().matches(testLayout))
    }

    @Test
    fun hasSymmetricCorner() {
        val cornerRadius = 8.5F
        val testLayout =
            Box.Builder()
                .setModifiers(
                    Modifiers.Builder()
                        .setBackground(
                            Background.Builder()
                                .setCorner(Corner.Builder().setRadius(dp(cornerRadius)).build())
                                .build()
                        )
                        .build()
                )
                .build()

        assert(hasAllCorners(cornerRadius).matches(testLayout))
        assert(hasAllCorners(cornerRadius + 1F).not().matches(testLayout))
    }

    @Test
    fun hasAsymmetricCorner() {
        val radii = floatArrayOf(1F, 2F, 3F, 4F, 5F, 6F, 7F, 8F)
        val testLayout =
            Box.Builder()
                .setModifiers(
                    Modifiers.Builder()
                        .setBackground(
                            Background.Builder()
                                .setCorner(
                                    Corner.Builder()
                                        .setTopLeftRadius(dp(radii[0]), dp(radii[1]))
                                        .setTopRightRadius(dp(radii[2]), dp(radii[3]))
                                        .setBottomLeftRadius(dp(radii[4]), dp(radii[5]))
                                        .setBottomRightRadius(dp(radii[6]), dp(radii[7]))
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()

        assert(hasTopLeftCorner(radii[0], radii[1]).matches(testLayout))
        assert(hasTopRightCorner(radii[2], radii[3]).matches(testLayout))
        assert(hasBottomLeftCorner(radii[4], radii[5]).matches(testLayout))
        assert(hasBottomRightCorner(radii[6], radii[7]).matches(testLayout))
    }

    @Test
    fun hasOneOverrideCorner() {
        val cornerRadius = 8.5F
        val bottomLeftXRadius = 9.5F
        val bottomLeftYRadius = 9.5F
        val testLayout =
            Box.Builder()
                .setModifiers(
                    Modifiers.Builder()
                        .setBackground(
                            Background.Builder()
                                .setCorner(
                                    Corner.Builder()
                                        .setRadius(dp(cornerRadius))
                                        .setBottomLeftRadius(
                                            dp(bottomLeftXRadius),
                                            dp(bottomLeftYRadius),
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()

        assert(hasAllCorners(cornerRadius).not().matches(testLayout))
        assert(
            (hasTopLeftCorner(cornerRadius, cornerRadius) and
                    hasTopRightCorner(cornerRadius, cornerRadius) and
                    hasBottomLeftCorner(bottomLeftXRadius, bottomLeftYRadius) and
                    hasBottomRightCorner(cornerRadius, cornerRadius))
                .matches(testLayout)
        )
    }
}
