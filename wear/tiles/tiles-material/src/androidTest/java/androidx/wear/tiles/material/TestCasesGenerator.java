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

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("deprecation")
public class TestCasesGenerator {
    private TestCasesGenerator() {}

    public static final String NORMAL_SCALE_SUFFIX = "";
    public static final String XXXL_SCALE_SUFFIX = "_xxxl";
    private static final String ICON_ID = "tile_icon";
    private static final String AVATAR = "avatar_image";

    /**
     * This function will append goldenSuffix on the name of the golden images that should be
     * different for different user font sizes. Note that some of the golden will have the same name
     * as it should point on the same size independent image.
     */
    @NonNull
    static Map<String, androidx.wear.tiles.LayoutElementBuilders.LayoutElement> generateTestCases(
            @NonNull Context context,
            @NonNull androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters deviceParameters,
            @NonNull String goldenSuffix) {
        androidx.wear.tiles.ModifiersBuilders.Clickable clickable =
                new androidx.wear.tiles.ModifiersBuilders.Clickable.Builder()
                        .setOnClick(
                                new androidx.wear.tiles.ActionBuilders.LaunchAction.Builder()
                                        .build())
                        .setId("action_id")
                        .build();
        String mainText = "Primary label";
        String labelText = "Secondary label";
        String largeChipText = "Action";
        HashMap<String, androidx.wear.tiles.LayoutElementBuilders.LayoutElement> testCases =
                new HashMap<>();

        testCases.put(
                "default_icon_button_golden" + NORMAL_SCALE_SUFFIX,
                new Button.Builder(context, clickable).setIconContent(ICON_ID).build());
        testCases.put(
                "extralarge_secondary_icon_after_button_golden" + NORMAL_SCALE_SUFFIX,
                new Button.Builder(context, clickable)
                        .setButtonColors(ButtonDefaults.SECONDARY_COLORS)
                        .setIconContent(ICON_ID)
                        .setSize(ButtonDefaults.EXTRA_LARGE_SIZE)
                        .build());
        testCases.put(
                "large_secondary_icon_40size_button_golden" + NORMAL_SCALE_SUFFIX,
                new Button.Builder(context, clickable)
                        .setSize(ButtonDefaults.LARGE_SIZE)
                        .setButtonColors(ButtonDefaults.SECONDARY_COLORS)
                        .setIconContent(ICON_ID, androidx.wear.tiles.DimensionBuilders.dp(40))
                        .build());
        testCases.put(
                "extralarge_custom_text_custom_sizefont_button_golden" + goldenSuffix,
                new Button.Builder(context, clickable)
                        .setButtonColors(new ButtonColors(Color.YELLOW, Color.GREEN))
                        .setSize(ButtonDefaults.EXTRA_LARGE_SIZE)
                        .setCustomContent(
                                new Text.Builder(context, "ABC")
                                        .setTypography(Typography.TYPOGRAPHY_DISPLAY1)
                                        .setItalic(true)
                                        .setColor(
                                                androidx.wear.tiles.ColorBuilders.argb(Color.GREEN))
                                        .build())
                        .build());
        testCases.put(
                "default_text_button_golden" + goldenSuffix,
                new Button.Builder(context, clickable).setTextContent("ABC").build());
        testCases.put(
                "default_image_button_golden" + NORMAL_SCALE_SUFFIX,
                new Button.Builder(context, clickable).setImageContent(AVATAR).build());

        testCases.put(
                "default_chip_maintext_golden" + goldenSuffix,
                new Chip.Builder(context, clickable, deviceParameters)
                        .setPrimaryLabelContent(mainText)
                        .build());
        testCases.put(
                "default_chip_maintextlabeltext_golden" + goldenSuffix,
                new Chip.Builder(context, clickable, deviceParameters)
                        .setPrimaryLabelContent(mainText)
                        .setSecondaryLabelContent(labelText)
                        .setHorizontalAlignment(
                                androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_START)
                        .build());
        testCases.put(
                "default_chip_maintexticon_golden" + goldenSuffix,
                new Chip.Builder(context, clickable, deviceParameters)
                        .setPrimaryLabelContent(mainText)
                        .setIconContent(ICON_ID)
                        .build());
        testCases.put(
                "secondary_chip_maintext_centered_golden" + goldenSuffix,
                new Chip.Builder(context, clickable, deviceParameters)
                        .setHorizontalAlignment(
                                androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                        .setChipColors(ChipDefaults.SECONDARY_COLORS)
                        .setPrimaryLabelContent(mainText)
                        .build());
        testCases.put(
                "custom_chip_all_overflows_golden" + goldenSuffix,
                new Chip.Builder(context, clickable, deviceParameters)
                        .setWidth(130)
                        .setPrimaryLabelContent(mainText)
                        .setSecondaryLabelContent(labelText)
                        .setIconContent(ICON_ID)
                        .setChipColors(
                                new ChipColors(Color.YELLOW, Color.GREEN, Color.BLACK, Color.GRAY))
                        .build());
        testCases.put(
                "default_chip_all_centered_golden" + goldenSuffix,
                new Chip.Builder(context, clickable, deviceParameters)
                        .setHorizontalAlignment(
                                androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                        .setPrimaryLabelContent(mainText)
                        .setSecondaryLabelContent(labelText)
                        .setIconContent(ICON_ID)
                        .build());
        testCases.put(
                "default_chip_all_rigthalign_golden" + goldenSuffix,
                new Chip.Builder(context, clickable, deviceParameters)
                        .setHorizontalAlignment(
                                androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_END)
                        .setPrimaryLabelContent(mainText)
                        .setSecondaryLabelContent(labelText)
                        .setIconContent(ICON_ID)
                        .build());
        testCases.put(
                "custom_chip_icon_primary_overflows_golden" + goldenSuffix,
                new Chip.Builder(context, clickable, deviceParameters)
                        .setPrimaryLabelContent(mainText)
                        .setIconContent(ICON_ID)
                        .setWidth(150)
                        .setHorizontalAlignment(
                                androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_START)
                        .setChipColors(
                                new ChipColors(Color.YELLOW, Color.GREEN, Color.BLACK, Color.GRAY))
                        .build());
        testCases.put(
                "chip_custom_content_centered_golden" + goldenSuffix,
                new Chip.Builder(context, clickable, deviceParameters)
                        .setHorizontalAlignment(
                                androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                        .setCustomContent(
                                new androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                                        .addContent(
                                                new Text.Builder(context, "random text")
                                                        .setTypography(Typography.TYPOGRAPHY_TITLE3)
                                                        .setItalic(true)
                                                        .build())
                                        .build())
                        .build());
        testCases.put(
                "chip_custom_content_leftaligned_golden" + goldenSuffix,
                new Chip.Builder(context, clickable, deviceParameters)
                        .setChipColors(ChipDefaults.SECONDARY_COLORS)
                        .setHorizontalAlignment(
                                androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_START)
                        .setCustomContent(
                                new androidx.wear.tiles.LayoutElementBuilders.Row.Builder()
                                        .addContent(
                                                new Text.Builder(context, "text1")
                                                        .setTypography(Typography.TYPOGRAPHY_TITLE3)
                                                        .setItalic(true)
                                                        .setColor(
                                                                androidx.wear.tiles.ColorBuilders
                                                                        .argb(Color.WHITE))
                                                        .build())
                                        .addContent(
                                                new Text.Builder(context, "text2")
                                                        .setTypography(Typography.TYPOGRAPHY_TITLE2)
                                                        .setColor(
                                                                androidx.wear.tiles.ColorBuilders
                                                                        .argb(Color.YELLOW))
                                                        .build())
                                        .build())
                        .setWidth(150)
                        .build());
        testCases.put(
                "chip_2lines_primary_overflows_golden" + goldenSuffix,
                new Chip.Builder(context, clickable, deviceParameters)
                        .setPrimaryLabelContent("abcdeabcdeabcdeabcdeabcdeabcdeabcdeabcde")
                        .build());

        // Different text lengths to test expanding the width based on the size of text. If it's
        // more than 9, the rest will be deleted.
        testCases.put(
                "compactchip_default_len2_golden" + goldenSuffix,
                new CompactChip.Builder(context, "Ab", clickable, deviceParameters).build());
        testCases.put(
                "compactchip_default_len5_golden" + goldenSuffix,
                new CompactChip.Builder(context, "Abcde", clickable, deviceParameters).build());
        testCases.put(
                "compactchip_default_len9_golden" + goldenSuffix,
                new CompactChip.Builder(context, "Abcdefghi", clickable, deviceParameters).build());
        testCases.put(
                "compactchip_default_toolong_golden" + goldenSuffix,
                new CompactChip.Builder(
                                context, "AbcdefghiEXTRAEXTRAEXTRA", clickable, deviceParameters)
                        .build());
        testCases.put(
                "compactchip_custom_default_golden" + goldenSuffix,
                new CompactChip.Builder(context, "Action", clickable, deviceParameters)
                        .setChipColors(new ChipColors(Color.YELLOW, Color.BLACK))
                        .build());

        testCases.put(
                "titlechip_default_golden" + goldenSuffix,
                new TitleChip.Builder(context, largeChipText, clickable, deviceParameters).build());
        testCases.put(
                "titlechip_default_texttoolong_golden" + goldenSuffix,
                new TitleChip.Builder(context, "abcdeabcdeabcdeEXTRA", clickable, deviceParameters)
                        .build());
        testCases.put(
                "titlechip_leftalign_secondary_default_golden" + goldenSuffix,
                new TitleChip.Builder(context, largeChipText, clickable, deviceParameters)
                        .setHorizontalAlignment(
                                androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_START)
                        .setChipColors(ChipDefaults.TITLE_SECONDARY_COLORS)
                        .build());
        testCases.put(
                "titlechip_centered_custom_150_secondary_default_golden" + goldenSuffix,
                new TitleChip.Builder(context, largeChipText, clickable, deviceParameters)
                        .setHorizontalAlignment(
                                androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                        .setChipColors(new ChipColors(Color.YELLOW, Color.BLUE))
                        .setWidth(150)
                        .build());

        testCases.put(
                "default_full_circularprogressindicator",
                new CircularProgressIndicator.Builder().build());
        testCases.put(
                "default_gap_circularprogressindicator",
                new CircularProgressIndicator.Builder()
                        .setStartAngle(ProgressIndicatorDefaults.GAP_START_ANGLE)
                        .setEndAngle(ProgressIndicatorDefaults.GAP_END_ANGLE)
                        .build());
        testCases.put(
                "default_full_90_circularprogressindicator",
                new CircularProgressIndicator.Builder().setProgress(0.25f).build());
        testCases.put(
                "default_gap_90_circularprogressindicator",
                new CircularProgressIndicator.Builder()
                        .setProgress(0.25f)
                        .setStartAngle(ProgressIndicatorDefaults.GAP_START_ANGLE)
                        .setEndAngle(ProgressIndicatorDefaults.GAP_END_ANGLE)
                        .build());
        testCases.put(
                "custom_gap_45_circularprogressindicator",
                new CircularProgressIndicator.Builder()
                        .setStartAngle(45)
                        .setEndAngle(270)
                        .setProgress(0.2f)
                        .setStrokeWidth(12)
                        .setCircularProgressIndicatorColors(
                                new ProgressIndicatorColors(Color.BLUE, Color.YELLOW))
                        .build());

        testCases.put(
                "default_text_golden" + goldenSuffix, new Text.Builder(context, "Testing").build());
        testCases.put(
                "custom_text_golden" + goldenSuffix,
                new Text.Builder(context, "Testing text.")
                        .setItalic(true)
                        .setColor(androidx.wear.tiles.ColorBuilders.argb(Color.YELLOW))
                        .setWeight(androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_BOLD)
                        .setTypography(Typography.TYPOGRAPHY_BODY2)
                        .build());
        testCases.put(
                "overflow_text_golden" + goldenSuffix,
                new Text.Builder(context, "abcdeabcdeabcde").build());

        return testCases;
    }
}
