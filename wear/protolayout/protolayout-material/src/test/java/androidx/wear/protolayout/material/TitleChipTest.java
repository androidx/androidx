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

import static androidx.wear.protolayout.DimensionBuilders.dp;
import static androidx.wear.protolayout.material.Utils.areChipColorsEqual;
import static androidx.wear.protolayout.materialcore.Chip.METADATA_TAG_ICON;
import static androidx.wear.protolayout.materialcore.Chip.METADATA_TAG_TEXT;

import static com.google.common.truth.Truth.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.ActionBuilders.LaunchAction;
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.protolayout.DimensionBuilders.DpProp;
import androidx.wear.protolayout.LayoutElementBuilders.Box;
import androidx.wear.protolayout.LayoutElementBuilders.Column;
import androidx.wear.protolayout.ModifiersBuilders.Clickable;
import androidx.wear.protolayout.ModifiersBuilders.ElementMetadata;
import androidx.wear.protolayout.ModifiersBuilders.Modifiers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
public class TitleChipTest {
    private static final String MAIN_TEXT = "Action";
    private static final Clickable CLICKABLE =
            new Clickable.Builder()
                    .setOnClick(new LaunchAction.Builder().build())
                    .setId("action_id")
                    .build();
    private static final DeviceParameters DEVICE_PARAMETERS =
            new DeviceParameters.Builder().setScreenWidthDp(192).setScreenHeightDp(192).build();
    private static final ChipColors COLORS = new ChipColors(Color.YELLOW, Color.BLUE);
    private static final Context CONTEXT = ApplicationProvider.getApplicationContext();
    private static final DpProp EXPECTED_WIDTH =
            dp(
                    DEVICE_PARAMETERS.getScreenWidthDp()
                            * (100 - 2 * ChipDefaults.DEFAULT_MARGIN_PERCENT)
                            / 100);

    @Test
    public void testTitleChipDefault() {
        TitleChip titleChip =
                new TitleChip.Builder(CONTEXT, MAIN_TEXT, CLICKABLE, DEVICE_PARAMETERS).build();

        assertChip(
                titleChip, ChipDefaults.TITLE_PRIMARY_COLORS, EXPECTED_WIDTH, /* iconId= */ null);
    }

    @Test
    public void testTitleChipCustom() {
        DpProp width = dp(150);
        TitleChip titleChip =
                new TitleChip.Builder(CONTEXT, MAIN_TEXT, CLICKABLE, DEVICE_PARAMETERS)
                        .setChipColors(COLORS)
                        .setWidth(width)
                        .build();

        assertChip(titleChip, COLORS, width, /* iconId= */ null);
    }

    @Test
    public void testIconChipIconCustomColor() {
        String iconId = "icon_id";
        TitleChip titleChip =
                new TitleChip.Builder(CONTEXT, MAIN_TEXT, CLICKABLE, DEVICE_PARAMETERS)
                        .setChipColors(COLORS)
                        .setIconContent(iconId)
                        .build();

        assertChip(titleChip, COLORS, EXPECTED_WIDTH, /* iconId= */ iconId);
    }

    @Test
    public void testWrongElement() {
        Column box = new Column.Builder().build();

        assertThat(TitleChip.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongBox() {
        Box box = new Box.Builder().build();

        assertThat(TitleChip.fromLayoutElement(box)).isNull();
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

        assertThat(TitleChip.fromLayoutElement(box)).isNull();
    }

    private void assertChip(
            TitleChip actualTitleChip, ChipColors colors, DpProp width, @Nullable String iconId) {
        assertChipIsEqual(actualTitleChip, colors, width, iconId);
        assertFromLayoutElementChipIsEqual(actualTitleChip, colors, width, iconId);
        assertThat(TitleChip.fromLayoutElement(actualTitleChip)).isEqualTo(actualTitleChip);
    }

    private void assertChipIsEqual(
            TitleChip actualTitleChip, ChipColors colors, DpProp width, @Nullable String iconId) {
        String expectedTag = iconId == null ? METADATA_TAG_TEXT : METADATA_TAG_ICON;
        assertThat(actualTitleChip.getMetadataTag()).isEqualTo(expectedTag);
        assertThat(actualTitleChip.getClickable().toProto()).isEqualTo(CLICKABLE.toProto());
        assertThat(actualTitleChip.getWidth().toContainerDimensionProto())
                .isEqualTo(width.toContainerDimensionProto());
        assertThat(areChipColorsEqual(actualTitleChip.getChipColors(), colors)).isTrue();
        assertThat(actualTitleChip.getText()).isEqualTo(MAIN_TEXT);
        assertThat(actualTitleChip.getIconContent()).isEqualTo(iconId);
    }

    private void assertFromLayoutElementChipIsEqual(
            TitleChip chip, ChipColors colors, DpProp width, @Nullable String iconId) {
        Box box = new Box.Builder().addContent(chip).build();

        TitleChip newChip = TitleChip.fromLayoutElement(box.getContents().get(0));

        assertThat(newChip).isNotNull();
        assertChipIsEqual(newChip, colors, width, iconId);
    }
}
