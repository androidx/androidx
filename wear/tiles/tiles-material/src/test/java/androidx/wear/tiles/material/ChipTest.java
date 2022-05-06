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
import static androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_START;
import static androidx.wear.tiles.material.Utils.areChipColorsEqual;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.tiles.ActionBuilders.LaunchAction;
import androidx.wear.tiles.ColorBuilders.ColorProp;
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.tiles.DimensionBuilders.DpProp;
import androidx.wear.tiles.LayoutElementBuilders.HorizontalAlignment;
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement;
import androidx.wear.tiles.LayoutElementBuilders.Row;
import androidx.wear.tiles.ModifiersBuilders.Clickable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
// This test is testing that defaults of skeleton are set. More detailed tests that everything is in
// place are in Scuba tests.
public class ChipTest {
    private static final String MAIN_TEXT = "Primary text";
    private static final Clickable CLICKABLE =
            new Clickable.Builder()
                    .setOnClick(new LaunchAction.Builder().build())
                    .setId("action_id")
                    .build();
    private static final DeviceParameters DEVICE_PARAMETERS =
            new DeviceParameters.Builder().setScreenWidthDp(192).setScreenHeightDp(192).build();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private static final DpProp EXPECTED_WIDTH =
            dp(
               DEVICE_PARAMETERS.getScreenWidthDp()
                   * (100 - 2 * ChipDefaults.DEFAULT_MARGIN_PERCENT) / 100);

    @Test
    public void testChipSkeleton() {
        String contentDescription = "Chip";
        Chip chip =
                new Chip.Builder(mContext, CLICKABLE, DEVICE_PARAMETERS)
                        .setPrimaryTextContent(MAIN_TEXT)
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                        .setContentDescription(contentDescription)
                        .build();
        assertChipSkeletonIsEqual(
                chip, HORIZONTAL_ALIGN_CENTER, ChipDefaults.PRIMARY_COLORS, contentDescription);
    }

    @Test
    public void testFullChipColors() {
        ChipColors colors = new ChipColors(Color.YELLOW, Color.WHITE, Color.BLUE, Color.MAGENTA);
        Chip chip =
                new Chip.Builder(mContext, CLICKABLE, DEVICE_PARAMETERS)
                        .setChipColors(colors)
                        .setPrimaryTextLabelIconContent(MAIN_TEXT, "Label", "ICON_ID")
                        .build();
        assertChipSkeletonIsEqual(chip, HORIZONTAL_ALIGN_START, colors);
    }

    @Test
    public void testChipSkeletonLeftAligned() {
        Chip chip =
                new Chip.Builder(mContext, CLICKABLE, DEVICE_PARAMETERS)
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_START)
                        .setPrimaryTextContent(MAIN_TEXT)
                        .build();
        assertChipSkeletonIsEqual(chip, HORIZONTAL_ALIGN_START, ChipDefaults.PRIMARY_COLORS);
    }

    @Test
    public void testChipEmptyFails() {
        assertThrows(
                IllegalStateException.class,
                () -> new Chip.Builder(mContext, CLICKABLE, DEVICE_PARAMETERS).build());
    }

    @Test
    public void testChipCustomContent() {
        ColorProp yellow = argb(Color.YELLOW);
        ColorProp blue = argb(Color.BLUE);
        LayoutElement content =
                new Row.Builder()
                        .addContent(
                                new Text.Builder(mContext, "text1")
                                        .setTypography(Typography.TYPOGRAPHY_TITLE3)
                                        .setColor(yellow)
                                        .setItalic(true)
                                        .build())
                        .addContent(
                                new Text.Builder(mContext, "text2")
                                        .setTypography(Typography.TYPOGRAPHY_TITLE2)
                                        .setColor(blue)
                                        .build())
                        .build();

        Chip chip =
                new Chip.Builder(mContext, CLICKABLE, DEVICE_PARAMETERS)
                        .setContent(content)
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_START)
                        .build();

        assertChipSkeletonIsEqual(
                chip,
                HORIZONTAL_ALIGN_START,
                new ChipColors(
                        ChipDefaults.PRIMARY_COLORS.getBackgroundColor(),
                        new ColorProp.Builder().build()));
        assertThat(chip.getContent().toLayoutElementProto())
                .isEqualTo(content.toLayoutElementProto());
    }

    private void assertChipSkeletonIsEqual(
            @NonNull Chip actualChip, @HorizontalAlignment int hAlign, @NonNull ChipColors colors) {
        assertChipSkeletonIsEqual(actualChip, hAlign, colors, null);
    }

    private void assertChipSkeletonIsEqual(
            @NonNull Chip actualChip,
            @HorizontalAlignment int hAlign,
            @NonNull ChipColors colors,
            @Nullable String expectedContDesc) {
        assertThat(actualChip.getClickable().toProto()).isEqualTo(CLICKABLE.toProto());
        assertThat(actualChip.getWidth().toContainerDimensionProto())
                .isEqualTo(EXPECTED_WIDTH.toContainerDimensionProto());
        assertThat(actualChip.getHeight().toContainerDimensionProto())
                .isEqualTo(ChipDefaults.DEFAULT_HEIGHT.toContainerDimensionProto());
        assertThat(areChipColorsEqual(actualChip.getChipColors(), colors)).isTrue();
        assertThat(actualChip.getHorizontalAlignment()).isEqualTo(hAlign);

        if (expectedContDesc == null) {
            assertThat(actualChip.getContentDescription()).isNull();
        } else {
            assertThat(actualChip.getContentDescription().toString()).isEqualTo(expectedContDesc);
        }
    }
}
