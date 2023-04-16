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

package androidx.wear.tiles.material;

import static androidx.wear.tiles.material.Typography.TYPOGRAPHY_BODY1;
import static androidx.wear.tiles.material.Typography.TYPOGRAPHY_CAPTION2;
import static androidx.wear.tiles.material.Typography.TYPOGRAPHY_TITLE1;
import static androidx.wear.tiles.material.Typography.getFontStyleBuilder;
import static androidx.wear.tiles.material.Typography.getLineHeightForTypography;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;
import android.graphics.Color;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
@SuppressWarnings("deprecation")
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
        androidx.wear.tiles.LayoutElementBuilders.FontStyle fontStyle =
                getFontStyleBuilder(TYPOGRAPHY_BODY1, CONTEXT).build();
        assertFontStyle(
                fontStyle,
                16,
                androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_NORMAL,
                0.01f,
                20,
                TYPOGRAPHY_BODY1);
    }

    @Test
    public void testTypography_caption2() {
        androidx.wear.tiles.LayoutElementBuilders.FontStyle fontStyle =
                getFontStyleBuilder(TYPOGRAPHY_CAPTION2, CONTEXT).build();
        assertFontStyle(
                fontStyle,
                12,
                androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_MEDIUM,
                0.01f,
                16,
                TYPOGRAPHY_CAPTION2);
    }

    @Test
    public void testWrongElement() {
        androidx.wear.tiles.LayoutElementBuilders.Column box =
                new androidx.wear.tiles.LayoutElementBuilders.Column.Builder().build();

        assertThat(Text.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongBox() {
        androidx.wear.tiles.LayoutElementBuilders.Box box =
                new androidx.wear.tiles.LayoutElementBuilders.Box.Builder().build();

        assertThat(Text.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongTag() {
        androidx.wear.tiles.LayoutElementBuilders.Box box =
                new androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                        .setModifiers(
                                new androidx.wear.tiles.ModifiersBuilders.Modifiers.Builder()
                                        .setMetadata(
                                                new androidx.wear.tiles.ModifiersBuilders
                                                                .ElementMetadata.Builder()
                                                        .setTagData("test".getBytes(UTF_8))
                                                        .build())
                                        .build())
                        .build();

        assertThat(Text.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testText() {
        String textContent = "Testing text.";
        androidx.wear.tiles.ModifiersBuilders.Modifiers modifiers =
                new androidx.wear.tiles.ModifiersBuilders.Modifiers.Builder()
                        .setBackground(
                                new androidx.wear.tiles.ModifiersBuilders.Background.Builder()
                                        .setColor(
                                                androidx.wear.tiles.ColorBuilders.argb(Color.BLUE))
                                        .build())
                        .build();
        int color = Color.YELLOW;
        Text text =
                new Text.Builder(CONTEXT, textContent)
                        .setItalic(true)
                        .setColor(androidx.wear.tiles.ColorBuilders.argb(color))
                        .setTypography(TYPOGRAPHY_TITLE1)
                        .setUnderline(true)
                        .setMaxLines(2)
                        .setModifiers(modifiers)
                        .setOverflow(
                                androidx.wear.tiles.LayoutElementBuilders
                                        .TEXT_OVERFLOW_ELLIPSIZE_END)
                        .setMultilineAlignment(
                                androidx.wear.tiles.LayoutElementBuilders.TEXT_ALIGN_END)
                        .setWeight(androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_BOLD)
                        .build();

        androidx.wear.tiles.LayoutElementBuilders.FontStyle expectedFontStyle =
                getFontStyleBuilder(TYPOGRAPHY_TITLE1, CONTEXT)
                        .setItalic(true)
                        .setUnderline(true)
                        .setColor(androidx.wear.tiles.ColorBuilders.argb(color))
                        .setWeight(androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_BOLD)
                        .build();

        assertTextIsEqual(text, textContent, modifiers, color, expectedFontStyle);

        androidx.wear.tiles.LayoutElementBuilders.Box box =
                new androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                        .addContent(text)
                        .build();
        Text newText = Text.fromLayoutElement(box.getContents().get(0));
        assertThat(newText).isNotNull();
        assertTextIsEqual(newText, textContent, modifiers, color, expectedFontStyle);

        assertThat(Text.fromLayoutElement(text)).isEqualTo(text);
    }

    private void assertTextIsEqual(
            Text actualText,
            String expectedTextContent,
            androidx.wear.tiles.ModifiersBuilders.Modifiers expectedModifiers,
            int expectedColor,
            androidx.wear.tiles.LayoutElementBuilders.FontStyle expectedFontStyle) {
        assertThat(actualText.getFontStyle().toProto()).isEqualTo(expectedFontStyle.toProto());
        assertThat(actualText.getText()).isEqualTo(expectedTextContent);
        assertThat(actualText.getColor().getArgb()).isEqualTo(expectedColor);
        assertThat(actualText.getMetadataTag()).isEqualTo(Text.METADATA_TAG);
        assertThat(actualText.getModifiers().toProto())
                .isEqualTo(Text.Builder.addTagToModifiers(expectedModifiers).toProto());
        assertThat(actualText.getOverflow())
                .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE_END);
        assertThat(actualText.getMultilineAlignment())
                .isEqualTo(androidx.wear.tiles.LayoutElementBuilders.TEXT_ALIGN_END);
        assertThat(actualText.getMaxLines()).isEqualTo(2);
        assertThat(actualText.getLineHeight())
                .isEqualTo(getLineHeightForTypography(TYPOGRAPHY_TITLE1).getValue());
    }

    private void assertFontStyle(
            androidx.wear.tiles.LayoutElementBuilders.FontStyle actualFontStyle,
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
