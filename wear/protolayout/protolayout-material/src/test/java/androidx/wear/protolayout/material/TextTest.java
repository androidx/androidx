/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.protolayout.material;

import static androidx.wear.protolayout.ColorBuilders.argb;
import static androidx.wear.protolayout.LayoutElementBuilders.FONT_WEIGHT_BOLD;
import static androidx.wear.protolayout.LayoutElementBuilders.FONT_WEIGHT_MEDIUM;
import static androidx.wear.protolayout.LayoutElementBuilders.FONT_WEIGHT_NORMAL;
import static androidx.wear.protolayout.LayoutElementBuilders.TEXT_ALIGN_END;
import static androidx.wear.protolayout.LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE_END;
import static androidx.wear.protolayout.material.Typography.TYPOGRAPHY_BODY1;
import static androidx.wear.protolayout.material.Typography.TYPOGRAPHY_CAPTION2;
import static androidx.wear.protolayout.material.Typography.TYPOGRAPHY_TITLE1;
import static androidx.wear.protolayout.material.Typography.getFontStyleBuilder;
import static androidx.wear.protolayout.material.Typography.getLineHeightForTypography;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;
import android.graphics.Color;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.LayoutElementBuilders;
import androidx.wear.protolayout.LayoutElementBuilders.Box;
import androidx.wear.protolayout.LayoutElementBuilders.Column;
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle;
import androidx.wear.protolayout.ModifiersBuilders.Background;
import androidx.wear.protolayout.ModifiersBuilders.ElementMetadata;
import androidx.wear.protolayout.ModifiersBuilders.Modifiers;
import androidx.wear.protolayout.TypeBuilders.StringLayoutConstraint;
import androidx.wear.protolayout.TypeBuilders.StringProp;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString;
import androidx.wear.protolayout.expression.ProtoLayoutExperimental;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
public class TextTest {

    public static final int NUM_OF_FONT_STYLE_CONST = 12;
    private static final Context CONTEXT = ApplicationProvider.getApplicationContext();

    @Test
    public void testTypography_incorrectTypography_negativeValue() {
        assertThrows(IllegalArgumentException.class, () -> getFontStyleBuilder(-1, CONTEXT));
    }

    @Test
    public void testTypography_incorrectTypography_positiveValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> getFontStyleBuilder(NUM_OF_FONT_STYLE_CONST + 1, CONTEXT));
    }

    @Test
    public void testLineHeight_incorrectTypography_negativeValue() {
        assertThrows(IllegalArgumentException.class, () -> getLineHeightForTypography(-1));
    }

    @Test
    public void testLineHeight_incorrectTypography_positiveValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> getLineHeightForTypography(NUM_OF_FONT_STYLE_CONST + 1));
    }

    @Test
    public void testTypography_body1() {
        FontStyle fontStyle = getFontStyleBuilder(TYPOGRAPHY_BODY1, CONTEXT).build();
        assertFontStyle(fontStyle, 16, FONT_WEIGHT_NORMAL, 0.01f, 20, TYPOGRAPHY_BODY1);
    }

    @Test
    public void testTypography_caption2() {
        FontStyle fontStyle = getFontStyleBuilder(TYPOGRAPHY_CAPTION2, CONTEXT).build();
        assertFontStyle(fontStyle, 12, FONT_WEIGHT_MEDIUM, 0.01f, 16, TYPOGRAPHY_CAPTION2);
    }

    @Test
    public void testWrongElement() {
        Column box = new Column.Builder().build();

        assertThat(Text.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongBox() {
        Box box = new Box.Builder().build();

        assertThat(Text.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongTag() {
        Box box =
                new Box.Builder()
                        .setModifiers(
                                new Modifiers.Builder()
                                        .setMetadata(
                                                new ElementMetadata.Builder()
                                                        .setTagData("test".getBytes(UTF_8))
                                                        .build())
                                        .build())
                        .build();

        assertThat(Text.fromLayoutElement(box)).isNull();
    }

    @Test
    @ProtoLayoutExperimental
    public void testText() {
        String textContent = "Testing text.";
        Modifiers modifiers =
                new Modifiers.Builder()
                        .setBackground(new Background.Builder().setColor(argb(Color.BLUE)).build())
                        .build();
        int color = Color.YELLOW;
        Text text =
                new Text.Builder(CONTEXT, textContent)
                        .setItalic(true)
                        .setColor(argb(color))
                        .setTypography(TYPOGRAPHY_TITLE1)
                        .setUnderline(true)
                        .setMaxLines(2)
                        .setModifiers(modifiers)
                        .setOverflow(TEXT_OVERFLOW_ELLIPSIZE_END)
                        .setMultilineAlignment(TEXT_ALIGN_END)
                        .setWeight(FONT_WEIGHT_BOLD)
                        .setExcludeFontPadding(true)
                        .build();

        FontStyle expectedFontStyle =
                getFontStyleBuilder(TYPOGRAPHY_TITLE1, CONTEXT)
                        .setItalic(true)
                        .setUnderline(true)
                        .setColor(argb(color))
                        .setWeight(FONT_WEIGHT_BOLD)
                        .build();

        assertTextIsEqual(text, textContent, modifiers, color, expectedFontStyle);

        Box box = new Box.Builder().addContent(text).build();
        Text newText = Text.fromLayoutElement(box.getContents().get(0));
        assertThat(newText).isNotNull();
        assertTextIsEqual(newText, textContent, modifiers, color, expectedFontStyle);

        assertThat(Text.fromLayoutElement(text)).isEqualTo(text);
    }

    @Test
    public void testDynamicText() {
        String textContent = "Testing text.";
        String valueForLayout = "PLACEHOLDER";
        Text text =
                new Text.Builder(
                                CONTEXT,
                                new StringProp.Builder(textContent)
                                        .setDynamicValue(DynamicString.constant(textContent))
                                        .build(),
                                new StringLayoutConstraint.Builder(valueForLayout)
                                        .setAlignment(TEXT_ALIGN_END)
                                        .build())
                        .build();

        Box box = new Box.Builder().addContent(text).build();
        Text newText = Text.fromLayoutElement(box.getContents().get(0));
        assertThat(newText).isNotNull();
        StringProp newProp = newText.getText();
        assertThat(newProp.getValue()).isEqualTo(textContent);
        assertThat(newProp.getDynamicValue()).isNotNull();
        assertThat(newProp.getDynamicValue().toDynamicStringProto().hasFixed()).isTrue();
        assertThat(newProp.getDynamicValue().toDynamicStringProto().getFixed().getValue())
                .isEqualTo(textContent);
        StringLayoutConstraint constraint =
                ((LayoutElementBuilders.Text) box.getContents().get(0))
                        .getLayoutConstraintsForDynamicText();
        assertThat(constraint.getPatternForLayout()).isEqualTo(valueForLayout);
        assertThat(constraint.getAlignment()).isEqualTo(TEXT_ALIGN_END);
    }

    @ProtoLayoutExperimental
    private void assertTextIsEqual(
            Text actualText,
            String expectedTextContent,
            Modifiers expectedModifiers,
            int expectedColor,
            FontStyle expectedFontStyle) {
        assertThat(actualText.getFontStyle().toProto()).isEqualTo(expectedFontStyle.toProto());
        assertThat(actualText.getText().getValue()).isEqualTo(expectedTextContent);
        assertThat(actualText.getColor().getArgb()).isEqualTo(expectedColor);
        assertThat(actualText.getOverflow()).isEqualTo(TEXT_OVERFLOW_ELLIPSIZE_END);
        assertThat(actualText.getMultilineAlignment()).isEqualTo(TEXT_ALIGN_END);
        assertThat(actualText.getMaxLines()).isEqualTo(2);
        assertThat(actualText.getLineHeight())
                .isEqualTo(getLineHeightForTypography(TYPOGRAPHY_TITLE1).getValue());
        assertThat(actualText.hasExcludeFontPadding()).isTrue();
    }

    private void assertFontStyle(
            FontStyle actualFontStyle,
            int expectedSize,
            int expectedWeight,
            float expectedLetterSpacing,
            float expectedLineHeight,
            int typographyCode) {
        assertThat(actualFontStyle.getSize()).isNotNull();
        assertThat(actualFontStyle.getWeight()).isNotNull();
        assertThat(actualFontStyle.getLetterSpacing()).isNotNull();
        assertThat(actualFontStyle.getSize().getValue()).isEqualTo(expectedSize);
        assertThat(actualFontStyle.getWeight().getValue()).isEqualTo(expectedWeight);
        assertThat(actualFontStyle.getLetterSpacing().getValue()).isEqualTo(expectedLetterSpacing);
        assertThat(getLineHeightForTypography(typographyCode).getValue())
                .isEqualTo(expectedLineHeight);
    }
}
