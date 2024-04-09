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

import static androidx.wear.protolayout.material.Utils.areChipColorsEqual;
import static androidx.wear.protolayout.materialcore.Chip.METADATA_TAG_ICON;
import static androidx.wear.protolayout.materialcore.Chip.METADATA_TAG_TEXT;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.ActionBuilders.LaunchAction;
import androidx.wear.protolayout.ColorBuilders.ColorProp;
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters;
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
public class CompactChipTest {
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

    @Test
    public void testCompactChipDefault() {
        CompactChip compactChip =
                new CompactChip.Builder(CONTEXT, CLICKABLE, DEVICE_PARAMETERS)
                        .setTextContent(MAIN_TEXT)
                        .build();

        assertChip(compactChip, ChipDefaults.COMPACT_PRIMARY_COLORS, /* iconId= */ null);
        assertThat(compactChip.getText()).isEqualTo(MAIN_TEXT);
    }

    @Test
    public void testCompactChipCustomColor() {
        CompactChip compactChip =
                new CompactChip.Builder(CONTEXT, CLICKABLE, DEVICE_PARAMETERS)
                        .setTextContent(MAIN_TEXT)
                        .setChipColors(COLORS)
                        .build();

        assertChip(compactChip, COLORS, /* iconId= */ null);
        assertThat(compactChip.getText()).isEqualTo(MAIN_TEXT);
    }

    @Test
    public void testCompactChipIconCustomColor() {
        String iconId = "icon_id";
        String description = "This is CompactChip with icon and text";
        CompactChip compactChip =
                new CompactChip.Builder(CONTEXT, CLICKABLE, DEVICE_PARAMETERS)
                        .setChipColors(COLORS)
                        .setTextContent(MAIN_TEXT)
                        .setIconContent(iconId)
                        .setContentDescription(description)
                        .build();

        assertChip(compactChip, COLORS, /* iconId= */ iconId, description);
        assertThat(compactChip.getText()).isEqualTo(MAIN_TEXT);
    }

    @Test
    public void testCompactChipIconOnly() {
        String iconId = "icon_id";
        String description = "This is CompactChip with icon only";
        ColorProp defaultColor = new ColorProp.Builder(0).build();
        CompactChip compactChip =
                new CompactChip.Builder(CONTEXT, CLICKABLE, DEVICE_PARAMETERS)
                        .setChipColors(COLORS)
                        .setIconContent(iconId)
                        .setContentDescription(description)
                        .build();

        assertChip(
                compactChip,
                new ChipColors(
                        COLORS.getBackgroundColor(),
                        COLORS.getIconColor(),
                        defaultColor,
                        defaultColor),
                iconId,
                description);
        assertThrows(NullPointerException.class, compactChip::getText);
    }

    @Test
    public void testWrongElement() {
        Column box = new Column.Builder().build();

        assertThat(CompactChip.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongBox() {
        Box box = new Box.Builder().build();

        assertThat(CompactChip.fromLayoutElement(box)).isNull();
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

        assertThat(CompactChip.fromLayoutElement(box)).isNull();
    }

    private void assertChip(
            CompactChip actualCompactChip, ChipColors colors, @Nullable String iconId) {
        assertChip(actualCompactChip, colors, iconId, /* contentDescription= */ MAIN_TEXT);
    }

    private void assertChip(
            CompactChip actualCompactChip,
            ChipColors colors,
            @Nullable String iconId,
            @NonNull String contentDescription) {
        assertChipIsEqual(actualCompactChip, colors, iconId, contentDescription);
        assertFromLayoutElementChipIsEqual(actualCompactChip, colors, iconId, contentDescription);
        assertThat(CompactChip.fromLayoutElement(actualCompactChip)).isEqualTo(actualCompactChip);
    }

    private void assertChipIsEqual(
            CompactChip actualCompactChip,
            ChipColors colors,
            @Nullable String iconId,
            @NonNull String contentDescription) {
        String expectedTag = iconId == null ? METADATA_TAG_TEXT : METADATA_TAG_ICON;
        assertThat(actualCompactChip.getMetadataTag()).isEqualTo(expectedTag);
        assertThat(actualCompactChip.getClickable().toProto()).isEqualTo(CLICKABLE.toProto());
        assertThat(areChipColorsEqual(actualCompactChip.getChipColors(), colors)).isTrue();
        assertThat(actualCompactChip.getIconContent()).isEqualTo(iconId);
        assertThat(actualCompactChip.getContentDescription().getValue())
                .isEqualTo(contentDescription);
    }

    private void assertFromLayoutElementChipIsEqual(
            CompactChip chip,
            ChipColors colors,
            @Nullable String iconId,
            @NonNull String contentDescription) {
        Box box = new Box.Builder().addContent(chip).build();

        CompactChip newChip = CompactChip.fromLayoutElement(box.getContents().get(0));

        assertThat(newChip).isNotNull();
        assertChipIsEqual(newChip, colors, iconId, contentDescription);
    }
}
