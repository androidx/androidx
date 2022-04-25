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

import static androidx.wear.tiles.ColorBuilders.argb;
import static androidx.wear.tiles.DimensionBuilders.dp;
import static androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER;
import static androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_END;
import static androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_START;
import static androidx.wear.tiles.material.ProgressIndicatorDefaults.GAP_END_ANGLE;
import static androidx.wear.tiles.material.ProgressIndicatorDefaults.GAP_START_ANGLE;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.wear.tiles.ActionBuilders.LaunchAction;
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.tiles.LayoutElementBuilders;
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement;
import androidx.wear.tiles.ModifiersBuilders.Clickable;

import java.util.HashMap;

public class TestCasesGenerator {
    private TestCasesGenerator() {}

    private static final String ICON_ID = "tile_icon";
    private static final String AVATAR = "avatar_image";
    public static final String NORMAL_SCALE_SUFFIX = "";
    public static final String XXXL_SCALE_SUFFIX = "_xxxl";

    /**
     * This function will append goldenSuffix on the name of the golden images that should be
     * different for different user font sizes. Note that some of the golden will have the same name
     * as it should point on the same size independent image.
     */
    @NonNull
    static HashMap<LayoutElement, String> generateTestCases(
            @NonNull Context context,
            @NonNull DeviceParameters deviceParameters,
            @NonNull String goldenSuffix) {
        Clickable clickable =
                new Clickable.Builder()
                        .setOnClick(new LaunchAction.Builder().build())
                        .setId("action_id")
                        .build();
        String mainText = "Primary label";
        String labelText = "Secondary label";
        String largeChipText = "Action";
        HashMap<LayoutElement, String> testCases = new HashMap<>();

        testCases.put(
                new Button.Builder(context, clickable).setIconContent(ICON_ID).build(),
                "default_icon_button_golden" + NORMAL_SCALE_SUFFIX);
        testCases.put(
                new Button.Builder(context, clickable)
                        .setButtonColors(ButtonDefaults.SECONDARY_BUTTON_COLORS)
                        .setIconContent(ICON_ID)
                        .setSize(ButtonDefaults.EXTRA_LARGE_BUTTON_SIZE)
                        .build(),
                "extralarge_secondary_icon_after_button_golden" + NORMAL_SCALE_SUFFIX);
        testCases.put(
                new Button.Builder(context, clickable)
                        .setSize(ButtonDefaults.LARGE_BUTTON_SIZE)
                        .setButtonColors(ButtonDefaults.SECONDARY_BUTTON_COLORS)
                        .setIconContent(ICON_ID, dp(40))
                        .build(),
                "large_secondary_icon_40size_button_golden" + NORMAL_SCALE_SUFFIX);
        testCases.put(
                new Button.Builder(context, clickable)
                        .setButtonColors(new ButtonColors(Color.YELLOW, Color.GREEN))
                        .setSize(ButtonDefaults.EXTRA_LARGE_BUTTON_SIZE)
                        .setContent(
                                new Text.Builder(context, "ABC")
                                        .setTypography(Typography.TYPOGRAPHY_DISPLAY1)
                                        .setItalic(true)
                                        .setColor(argb(Color.GREEN))
                                        .build())
                        .build(),
                "extralarge_custom_text_custom_sizefont_button_golden" + goldenSuffix);
        testCases.put(
                new Button.Builder(context, clickable).setTextContent("ABC").build(),
                "default_text_button_golden" + goldenSuffix);
        testCases.put(
                new Button.Builder(context, clickable).setImageContent(AVATAR).build(),
                "default_image_button_golden" + NORMAL_SCALE_SUFFIX);

        testCases.put(
                new Chip.Builder(context, clickable, deviceParameters)
                        .setPrimaryTextContent(mainText)
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_START)
                        .build(),
                "default_chip_maintext_golden" + goldenSuffix);
        testCases.put(
                new Chip.Builder(context, clickable, deviceParameters)
                        .setPrimaryTextLabelContent(mainText, labelText)
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_START)
                        .build(),
                "default_chip_maintextlabeltext_golden" + goldenSuffix);
        testCases.put(
                new Chip.Builder(context, clickable, deviceParameters)
                        .setPrimaryTextIconContent(mainText, ICON_ID)
                        .build(),
                "default_chip_maintexticon_golden" + goldenSuffix);
        testCases.put(
                new Chip.Builder(context, clickable, deviceParameters)
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                        .setChipColors(ChipDefaults.SECONDARY_COLORS)
                        .setPrimaryTextContent(mainText)
                        .build(),
                "secondary_chip_maintext_centered_golden" + goldenSuffix);
        testCases.put(
                new Chip.Builder(context, clickable, deviceParameters)
                        .setWidth(130)
                        .setPrimaryTextLabelIconContent(mainText, labelText, ICON_ID)
                        .setChipColors(
                                new ChipColors(Color.YELLOW, Color.GREEN, Color.BLACK, Color.GRAY))
                        .build(),
                "custom_chip_all_overflows_golden" + goldenSuffix);
        testCases.put(
                new Chip.Builder(context, clickable, deviceParameters)
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                        .setPrimaryTextLabelIconContent(mainText, labelText, ICON_ID)
                        .build(),
                "default_chip_all_centered_golden" + goldenSuffix);
        testCases.put(
                new Chip.Builder(context, clickable, deviceParameters)
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_END)
                        .setPrimaryTextLabelIconContent(mainText, labelText, ICON_ID)
                        .build(),
                "default_chip_all_rigthalign_golden" + goldenSuffix);
        testCases.put(
                new Chip.Builder(context, clickable, deviceParameters)
                        .setPrimaryTextIconContent(mainText, ICON_ID)
                        .setWidth(150)
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_START)
                        .setChipColors(
                                new ChipColors(Color.YELLOW, Color.GREEN, Color.BLACK, Color.GRAY))
                        .build(),
                "custom_chip_icon_primary_overflows_golden" + goldenSuffix);
        testCases.put(
                new Chip.Builder(context, clickable, deviceParameters)
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                        .setContent(
                                new LayoutElementBuilders.Box.Builder()
                                        .addContent(
                                                new Text.Builder(context, "random text")
                                                        .setTypography(Typography.TYPOGRAPHY_TITLE3)
                                                        .setItalic(true)
                                                        .build())
                                        .build())
                        .build(),
                "chip_custom_content_centered_golden" + goldenSuffix);
        testCases.put(
                new Chip.Builder(context, clickable, deviceParameters)
                        .setChipColors(ChipDefaults.SECONDARY_COLORS)
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_START)
                        .setContent(
                                new LayoutElementBuilders.Row.Builder()
                                        .addContent(
                                                new Text.Builder(context, "text1")
                                                        .setTypography(Typography.TYPOGRAPHY_TITLE3)
                                                        .setItalic(true)
                                                        .setColor(argb(Color.WHITE))
                                                        .build())
                                        .addContent(
                                                new Text.Builder(context, "text2")
                                                        .setTypography(Typography.TYPOGRAPHY_TITLE2)
                                                        .setColor(argb(Color.YELLOW))
                                                        .build())
                                        .build())
                        .setWidth(150)
                        .build(),
                "chip_custom_content_leftaligned_golden" + goldenSuffix);
        testCases.put(
                new Chip.Builder(context, clickable, deviceParameters)
                        .setPrimaryTextContent("abcdeabcdeabcdeabcdeabcdeabcdeabcdeabcde")
                        .build(),
                "chip_2lines_primary_overflows_golden" + goldenSuffix);

        // Different text lengths to test expanding the width based on the size of text. If it's
        // more than 9, the rest will be deleted.
        testCases.put(
                new CompactChip.Builder(context, "Ab", clickable, deviceParameters).build(),
                "compactchip_default_len2_golden" + goldenSuffix);
        testCases.put(
                new CompactChip.Builder(context, "Abcde", clickable, deviceParameters).build(),
                "compactchip_default_len5_golden" + goldenSuffix);
        testCases.put(
                new CompactChip.Builder(context, "Abcdefghi", clickable, deviceParameters).build(),
                "compactchip_default_len9_golden" + goldenSuffix);
        testCases.put(
                new CompactChip.Builder(context, "AbcdefghiEXTRA", clickable,
                        deviceParameters).build(),
                "compactchip_default_toolong_golden" + goldenSuffix);
        testCases.put(
                new CompactChip.Builder(context, "Action", clickable, deviceParameters)
                        .setChipColors(new ChipColors(Color.YELLOW, Color.BLACK))
                        .build(),
                "compactchip_custom_default_golden" + goldenSuffix);

        testCases.put(
                new TitleChip.Builder(context, largeChipText, clickable, deviceParameters).build(),
                "titlechip_default_golden" + goldenSuffix);
        testCases.put(
                new TitleChip.Builder(context, "abcdeabcdeabcdeEXTRA", clickable,
                        deviceParameters).build(),
                "titlechip_default_texttoolong_golden" + goldenSuffix);
        testCases.put(
                new TitleChip.Builder(context, largeChipText, clickable, deviceParameters)
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_START)
                        .setChipColors(ChipDefaults.TITLE_SECONDARY_COLORS)
                        .build(),
                "titlechip_leftalign_secondary_default_golden" + goldenSuffix);
        testCases.put(
                new TitleChip.Builder(context, largeChipText, clickable, deviceParameters)
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                        .setChipColors(new ChipColors(Color.YELLOW, Color.BLUE))
                        .setWidth(150)
                        .build(),
                "titlechip_centered_custom_150_secondary_default_golden" + goldenSuffix);

        testCases.put(
                new CircularProgressIndicator.Builder().build(),
                "default_full_circularprogressindicator");
        testCases.put(
                new CircularProgressIndicator.Builder()
                        .setStartAngle(GAP_START_ANGLE)
                        .setEndAngle(GAP_END_ANGLE)
                        .build(),
                "default_gap_circularprogressindicator");
        testCases.put(
                new CircularProgressIndicator.Builder().setProgress(0.25f).build(),
                "default_full_90_circularprogressindicator");
        testCases.put(
                new CircularProgressIndicator.Builder()
                        .setProgress(0.25f)
                        .setStartAngle(GAP_START_ANGLE)
                        .setEndAngle(GAP_END_ANGLE)
                        .build(),
                "default_gap_90_circularprogressindicator");
        testCases.put(
                new CircularProgressIndicator.Builder()
                        .setStartAngle(45)
                        .setEndAngle(270)
                        .setProgress(0.2f)
                        .setStrokeWidth(12)
                        .setCircularProgressIndicatorColors(
                                new ProgressIndicatorColors(Color.BLUE, Color.YELLOW))
                        .build(),
                "custom_gap_45_circularprogressindicator");

        testCases.put(
                new Text.Builder(context, "Testing").build(),
                "default_text_golden" + goldenSuffix);
        testCases.put(
                new Text.Builder(context, "Testing text.")
                        .setItalic(true)
                        .setColor(argb(Color.YELLOW))
                        .setTypography(Typography.TYPOGRAPHY_BODY2)
                        .build(),
                "custom_text_golden" + goldenSuffix);
        testCases.put(
                new Text.Builder(context, "abcdeabcdeabcde").build(),
                "overflow_text_golden" + goldenSuffix);

        return testCases;
    }
}
