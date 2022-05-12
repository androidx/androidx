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

import static androidx.wear.tiles.DimensionBuilders.dp;
import static androidx.wear.tiles.material.Utils.areChipColorsEqual;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.Color;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.tiles.ActionBuilders.LaunchAction;
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.tiles.DimensionBuilders.DpProp;
import androidx.wear.tiles.ModifiersBuilders.Clickable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
// This test is testing that defaults of skeleton are set. More detailed tests that everything is in
// // place are in Scuba tests.
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
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private static final DpProp EXPECTED_WIDTH =
            dp(
                    DEVICE_PARAMETERS.getScreenWidthDp()
                            * (100 - 2 * ChipDefaults.DEFAULT_MARGIN_PERCENT)
                            / 100);

    @Test
    public void testTitleChipDefault() {
        TitleChip titleChip =
                new TitleChip.Builder(mContext, MAIN_TEXT, CLICKABLE, DEVICE_PARAMETERS).build();

        assertChipSkeletonIsEqual(titleChip, ChipDefaults.TITLE_PRIMARY_COLORS, EXPECTED_WIDTH);
    }

    @Test
    public void testTitleChipCustom() {
        DpProp width = dp(150);
        TitleChip titleChip =
                new TitleChip.Builder(mContext, MAIN_TEXT, CLICKABLE, DEVICE_PARAMETERS)
                        .setChipColors(COLORS)
                        .setWidth(width)
                        .build();

        assertChipSkeletonIsEqual(titleChip, COLORS, width);
    }

    private void assertChipSkeletonIsEqual(
            TitleChip actualTitleChip, ChipColors colors, DpProp width) {
        assertThat(actualTitleChip.getClickable().toProto()).isEqualTo(CLICKABLE.toProto());
        assertThat(actualTitleChip.getWidth().toContainerDimensionProto())
                .isEqualTo(width.toContainerDimensionProto());
        assertThat(areChipColorsEqual(actualTitleChip.getChipColors(), colors)).isTrue();
    }
}
