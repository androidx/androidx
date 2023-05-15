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
import static androidx.wear.protolayout.DimensionBuilders.dp;
import static androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER;
import static androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_START;
import static androidx.wear.protolayout.material.Utils.areChipColorsEqual;

import static com.google.common.truth.Truth.assertThat;

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
import androidx.wear.protolayout.DimensionBuilders.DpProp;
import androidx.wear.protolayout.LayoutElementBuilders.Box;
import androidx.wear.protolayout.LayoutElementBuilders.Column;
import androidx.wear.protolayout.LayoutElementBuilders.HorizontalAlignment;
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement;
import androidx.wear.protolayout.LayoutElementBuilders.Row;
import androidx.wear.protolayout.ModifiersBuilders.Clickable;
import androidx.wear.protolayout.ModifiersBuilders.ElementMetadata;
import androidx.wear.protolayout.ModifiersBuilders.Modifiers;
import androidx.wear.protolayout.TypeBuilders.StringProp;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
public class ChipTest {
    private static final String MAIN_TEXT = "Primary text";
    private static final Clickable CLICKABLE =
            new Clickable.Builder()
                    .setOnClick(new LaunchAction.Builder().build())
                    .setId("action_id")
                    .build();
    private static final DeviceParameters DEVICE_PARAMETERS =
            new DeviceParameters.Builder().setScreenWidthDp(192).setScreenHeightDp(192).build();
    private static final Context CONTEXT = ApplicationProvider.getApplicationContext();
    private static final DpProp EXPECTED_WIDTH =
            dp(
                    DEVICE_PARAMETERS.getScreenWidthDp()
                            * (100 - 2 * ChipDefaults.DEFAULT_MARGIN_PERCENT)
                            / 100);

    @Test
    public void testChip() {
        StringProp contentDescription = staticString("Chip");
        Chip chip =
                new Chip.Builder(CONTEXT, CLICKABLE, DEVICE_PARAMETERS)
                        .setPrimaryLabelContent(MAIN_TEXT)
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                        .setContentDescription(contentDescription)
                        .build();
        assertChip(
                chip,
                HORIZONTAL_ALIGN_CENTER,
                ChipDefaults.PRIMARY_COLORS,
                contentDescription,
                Chip.METADATA_TAG_TEXT,
                MAIN_TEXT,
                null,
                null,
                null);
    }

    @Test
    public void testFullChipColors() {
        ChipColors colors = new ChipColors(Color.YELLOW, Color.WHITE, Color.BLUE, Color.MAGENTA);
        String secondaryLabel = "Label";
        Chip chip =
                new Chip.Builder(CONTEXT, CLICKABLE, DEVICE_PARAMETERS)
                        .setChipColors(colors)
                        .setPrimaryLabelContent(MAIN_TEXT)
                        .setSecondaryLabelContent(secondaryLabel)
                        .setIconContent("ICON_ID")
                        .build();
        assertChip(
                chip,
                HORIZONTAL_ALIGN_START,
                colors,
                staticString(MAIN_TEXT + "\n" + secondaryLabel),
                Chip.METADATA_TAG_ICON,
                MAIN_TEXT,
                secondaryLabel,
                "ICON_ID",
                null);
    }

    @Test
    public void testChipLeftAligned() {
        Chip chip =
                new Chip.Builder(CONTEXT, CLICKABLE, DEVICE_PARAMETERS)
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_START)
                        .setPrimaryLabelContent(MAIN_TEXT)
                        .build();
        assertChip(
                chip,
                HORIZONTAL_ALIGN_START,
                ChipDefaults.PRIMARY_COLORS,
                staticString(MAIN_TEXT),
                Chip.METADATA_TAG_TEXT,
                MAIN_TEXT,
                null,
                null,
                null);
    }

    @Test
    public void testChipCustomContent() {
        ColorProp yellow = argb(Color.YELLOW);
        ColorProp blue = argb(Color.BLUE);
        LayoutElement content =
                new Row.Builder()
                        .addContent(
                                new Text.Builder(CONTEXT, "text1")
                                        .setTypography(Typography.TYPOGRAPHY_TITLE3)
                                        .setColor(yellow)
                                        .setItalic(true)
                                        .build())
                        .addContent(
                                new Text.Builder(CONTEXT, "text2")
                                        .setTypography(Typography.TYPOGRAPHY_TITLE2)
                                        .setColor(blue)
                                        .build())
                        .build();

        StringProp contentDescription = staticString("Custom chip");
        Chip chip =
                new Chip.Builder(CONTEXT, CLICKABLE, DEVICE_PARAMETERS)
                        .setCustomContent(content)
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_START)
                        .setContentDescription(contentDescription)
                        .build();

        assertChip(
                chip,
                HORIZONTAL_ALIGN_START,
                new ChipColors(
                        ChipDefaults.PRIMARY_COLORS.getBackgroundColor(),
                        new ColorProp.Builder(0).build()),
                contentDescription,
                Chip.METADATA_TAG_CUSTOM_CONTENT,
                null,
                null,
                null,
                content);
        assertThat(chip.getCustomContent().toLayoutElementProto())
                .isEqualTo(content.toLayoutElementProto());
    }

    private void assertChip(
            @NonNull Chip actualChip,
            @HorizontalAlignment int hAlign,
            @NonNull ChipColors colors,
            @Nullable StringProp expectedContDesc,
            @NonNull String expectedMetadata,
            @Nullable String expectedPrimaryText,
            @Nullable String expectedLabel,
            @Nullable String expectedIcon,
            @Nullable LayoutElement expectedCustomContent) {
        assertChipIsEqual(
                actualChip,
                hAlign,
                colors,
                expectedContDesc,
                expectedMetadata,
                expectedPrimaryText,
                expectedLabel,
                expectedIcon,
                expectedCustomContent);

        assertFromLayoutElementChipIsEqual(
                actualChip,
                hAlign,
                colors,
                expectedContDesc,
                expectedMetadata,
                expectedPrimaryText,
                expectedLabel,
                expectedIcon,
                expectedCustomContent);

        assertThat(Chip.fromLayoutElement(actualChip)).isEqualTo(actualChip);
    }

    @Test
    public void testWrongElement() {
        Column box = new Column.Builder().build();

        assertThat(Chip.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongBox() {
        Box box = new Box.Builder().build();

        assertThat(Chip.fromLayoutElement(box)).isNull();
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

        assertThat(Chip.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testDynamicContentDescription() {
        StringProp dynamicContentDescription =
                new StringProp.Builder("static")
                        .setDynamicValue(DynamicString.constant("dynamic"))
                        .build();
        Chip chip =
                new Chip.Builder(CONTEXT, CLICKABLE, DEVICE_PARAMETERS)
                        .setPrimaryLabelContent("label")
                        .setContentDescription(dynamicContentDescription)
                        .build();

        assertThat(chip.getContentDescription().toProto())
                .isEqualTo(dynamicContentDescription.toProto());
    }

    private void assertFromLayoutElementChipIsEqual(
            @NonNull Chip chip,
            @HorizontalAlignment int hAlign,
            @NonNull ChipColors colors,
            @Nullable StringProp expectedContDesc,
            @NonNull String expectedMetadata,
            @Nullable String expectedPrimaryText,
            @Nullable String expectedLabel,
            @Nullable String expectedIcon,
            @Nullable LayoutElement expectedCustomContent) {
        Box box = new Box.Builder().addContent(chip).build();

        Chip newChip = Chip.fromLayoutElement(box.getContents().get(0));

        assertThat(newChip).isNotNull();
        assertChipIsEqual(
                newChip,
                hAlign,
                colors,
                expectedContDesc,
                expectedMetadata,
                expectedPrimaryText,
                expectedLabel,
                expectedIcon,
                expectedCustomContent);
    }

    private void assertChipIsEqual(
            @NonNull Chip actualChip,
            @HorizontalAlignment int hAlign,
            @NonNull ChipColors colors,
            @Nullable StringProp expectedContDesc,
            @NonNull String expectedMetadata,
            @Nullable String expectedPrimaryText,
            @Nullable String expectedLabel,
            @Nullable String expectedIcon,
            @Nullable LayoutElement expectedCustomContent) {
        assertThat(actualChip.getMetadataTag()).isEqualTo(expectedMetadata);
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
            assertThat(actualChip.getContentDescription().toProto())
                    .isEqualTo(expectedContDesc.toProto());
        }

        if (expectedPrimaryText == null) {
            assertThat(actualChip.getPrimaryLabelContent()).isNull();
        } else {
            assertThat(actualChip.getPrimaryLabelContent()).isEqualTo(expectedPrimaryText);
        }

        if (expectedLabel == null) {
            assertThat(actualChip.getSecondaryLabelContent()).isNull();
        } else {
            assertThat(actualChip.getSecondaryLabelContent()).isEqualTo(expectedLabel);
        }

        if (expectedIcon == null) {
            assertThat(actualChip.getIconContent()).isNull();
        } else {
            assertThat(actualChip.getIconContent()).isEqualTo(expectedIcon);
        }

        if (expectedCustomContent == null) {
            assertThat(actualChip.getCustomContent()).isNull();
        } else {
            assertThat(actualChip.getCustomContent().toLayoutElementProto())
                    .isEqualTo(expectedCustomContent.toLayoutElementProto());
        }
    }

    private StringProp staticString(String s) {
        return new StringProp.Builder(s).build();
    }
}
