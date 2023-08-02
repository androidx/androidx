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

import static androidx.wear.tiles.material.Utils.areChipColorsEqual;

import static com.google.common.truth.Truth.assertThat;

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
public class TitleChipTest {
    private static final String MAIN_TEXT = "Action";
    private static final androidx.wear.tiles.ModifiersBuilders.Clickable CLICKABLE =
            new androidx.wear.tiles.ModifiersBuilders.Clickable.Builder()
                    .setOnClick(
                            new androidx.wear.tiles.ActionBuilders.LaunchAction.Builder().build())
                    .setId("action_id")
                    .build();
    private static final androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
            DEVICE_PARAMETERS =
                    new androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters.Builder()
                            .setScreenWidthDp(192)
                            .setScreenHeightDp(192)
                            .build();
    private static final ChipColors COLORS = new ChipColors(Color.YELLOW, Color.BLUE);
    private static final Context CONTEXT = ApplicationProvider.getApplicationContext();
    private static final androidx.wear.tiles.DimensionBuilders.DpProp EXPECTED_WIDTH =
            androidx.wear.tiles.DimensionBuilders.dp(
                    DEVICE_PARAMETERS.getScreenWidthDp()
                            * (100 - 2 * ChipDefaults.DEFAULT_MARGIN_PERCENT)
                            / 100);

    @Test
    public void testTitleChipDefault() {
        TitleChip titleChip =
                new TitleChip.Builder(CONTEXT, MAIN_TEXT, CLICKABLE, DEVICE_PARAMETERS).build();

        assertChip(titleChip, ChipDefaults.TITLE_PRIMARY_COLORS, EXPECTED_WIDTH);
    }

    @Test
    public void testTitleChipCustom() {
        androidx.wear.tiles.DimensionBuilders.DpProp width =
                androidx.wear.tiles.DimensionBuilders.dp(150);
        TitleChip titleChip =
                new TitleChip.Builder(CONTEXT, MAIN_TEXT, CLICKABLE, DEVICE_PARAMETERS)
                        .setChipColors(COLORS)
                        .setWidth(width)
                        .build();

        assertChip(titleChip, COLORS, width);
    }

    @Test
    public void testWrongElement() {
        androidx.wear.tiles.LayoutElementBuilders.Column box =
                new androidx.wear.tiles.LayoutElementBuilders.Column.Builder().build();

        assertThat(TitleChip.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongBox() {
        androidx.wear.tiles.LayoutElementBuilders.Box box =
                new androidx.wear.tiles.LayoutElementBuilders.Box.Builder().build();

        assertThat(TitleChip.fromLayoutElement(box)).isNull();
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

        assertThat(TitleChip.fromLayoutElement(box)).isNull();
    }

    private void assertChip(
            TitleChip actualTitleChip,
            ChipColors colors,
            androidx.wear.tiles.DimensionBuilders.DpProp width) {
        assertChipIsEqual(actualTitleChip, colors, width);
        assertFromLayoutElementChipIsEqual(actualTitleChip, colors, width);
        assertThat(TitleChip.fromLayoutElement(actualTitleChip)).isEqualTo(actualTitleChip);
    }

    private void assertChipIsEqual(
            TitleChip actualTitleChip,
            ChipColors colors,
            androidx.wear.tiles.DimensionBuilders.DpProp width) {
        assertThat(actualTitleChip.getMetadataTag()).isEqualTo(TitleChip.METADATA_TAG);
        assertThat(actualTitleChip.getClickable().toProto()).isEqualTo(CLICKABLE.toProto());
        assertThat(actualTitleChip.getWidth().toContainerDimensionProto())
                .isEqualTo(width.toContainerDimensionProto());
        assertThat(areChipColorsEqual(actualTitleChip.getChipColors(), colors)).isTrue();
        assertThat(actualTitleChip.getText()).isEqualTo(MAIN_TEXT);
    }

    private void assertFromLayoutElementChipIsEqual(
            TitleChip chip, ChipColors colors, androidx.wear.tiles.DimensionBuilders.DpProp width) {
        androidx.wear.tiles.LayoutElementBuilders.Box box =
                new androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                        .addContent(chip)
                        .build();

        TitleChip newChip = TitleChip.fromLayoutElement(box.getContents().get(0));

        assertThat(newChip).isNotNull();
        assertChipIsEqual(newChip, colors, width);
    }
}
