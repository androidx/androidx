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
public class CompactChipTest {
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

    @Test
    public void testCompactChipDefault() {
        CompactChip compactChip =
                new CompactChip.Builder(CONTEXT, MAIN_TEXT, CLICKABLE, DEVICE_PARAMETERS).build();

        assertChip(compactChip, ChipDefaults.COMPACT_PRIMARY_COLORS);
    }

    @Test
    public void testCompactChipCustomColor() {
        CompactChip compactChip =
                new CompactChip.Builder(CONTEXT, MAIN_TEXT, CLICKABLE, DEVICE_PARAMETERS)
                        .setChipColors(COLORS)
                        .build();

        assertChip(compactChip, COLORS);
    }

    @Test
    public void testWrongElement() {
        androidx.wear.tiles.LayoutElementBuilders.Column box =
                new androidx.wear.tiles.LayoutElementBuilders.Column.Builder().build();

        assertThat(CompactChip.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongBox() {
        androidx.wear.tiles.LayoutElementBuilders.Box box =
                new androidx.wear.tiles.LayoutElementBuilders.Box.Builder().build();

        assertThat(CompactChip.fromLayoutElement(box)).isNull();
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

        assertThat(CompactChip.fromLayoutElement(box)).isNull();
    }

    private void assertChip(CompactChip actualCompactChip, ChipColors colors) {
        assertChipIsEqual(actualCompactChip, colors);
        assertFromLayoutElementChipIsEqual(actualCompactChip, colors);
        assertThat(CompactChip.fromLayoutElement(actualCompactChip)).isEqualTo(actualCompactChip);
    }

    private void assertChipIsEqual(CompactChip actualCompactChip, ChipColors colors) {
        assertThat(actualCompactChip.getMetadataTag()).isEqualTo(CompactChip.METADATA_TAG);
        assertThat(actualCompactChip.getClickable().toProto()).isEqualTo(CLICKABLE.toProto());
        assertThat(areChipColorsEqual(actualCompactChip.getChipColors(), colors)).isTrue();
        assertThat(actualCompactChip.getText()).isEqualTo(MAIN_TEXT);
    }

    private void assertFromLayoutElementChipIsEqual(CompactChip chip, ChipColors colors) {
        androidx.wear.tiles.LayoutElementBuilders.Box box =
                new androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                        .addContent(chip)
                        .build();

        CompactChip newChip = CompactChip.fromLayoutElement(box.getContents().get(0));

        assertThat(newChip).isNotNull();
        assertChipIsEqual(newChip, colors);
    }
}
