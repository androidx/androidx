/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.materialcore;

import static androidx.wear.protolayout.ColorBuilders.argb;
import static androidx.wear.protolayout.DimensionBuilders.dp;
import static androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER;
import static androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_START;

import static com.google.common.truth.Truth.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.ActionBuilders.LaunchAction;
import androidx.wear.protolayout.LayoutElementBuilders.Box;
import androidx.wear.protolayout.LayoutElementBuilders.Column;
import androidx.wear.protolayout.LayoutElementBuilders.HorizontalAlignment;
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement;
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
    private static final Box PRIMARY_LABEL = new Box.Builder().build();
    private static final Box SECONDARY_LABEL = new Box.Builder().build();
    private static final Box ICON = new Box.Builder().build();
    private static final Box CUSTOM_CONTENT = new Box.Builder().build();
    private static final String CONTENT_DESCRIPTION = "Content description";
    private static final Clickable CLICKABLE =
            new Clickable.Builder()
                    .setOnClick(new LaunchAction.Builder().build())
                    .setId("action_id")
                    .build();
    private static final float WIDTH_DP = 300;
    private static final float HEIGHT_DP = 300;
    @ColorInt private static final int BACKGROUND_COLOR = Color.YELLOW;

    @Test
    public void testChip() {
        StringProp contentDescription = staticString("Chip");
        Chip chip =
                new Chip.Builder(CLICKABLE)
                        .setWidth(dp(WIDTH_DP))
                        .setHeight(dp(HEIGHT_DP))
                        .setPrimaryLabelContent(PRIMARY_LABEL)
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                        .setContentDescription(contentDescription)
                        .build();
        assertChip(
                chip,
                HORIZONTAL_ALIGN_CENTER,
                Color.BLACK,
                contentDescription,
                Chip.METADATA_TAG_TEXT,
                PRIMARY_LABEL,
                null,
                null,
                null);
    }

    @Test
    public void testFullWithColors() {
        Chip chip =
                new Chip.Builder(CLICKABLE)
                        .setWidth(dp(WIDTH_DP))
                        .setHeight(dp(HEIGHT_DP))
                        .setBackgroundColor(argb(BACKGROUND_COLOR))
                        .setContentDescription(staticString(CONTENT_DESCRIPTION))
                        .setPrimaryLabelContent(PRIMARY_LABEL)
                        .setSecondaryLabelContent(SECONDARY_LABEL)
                        .setIconContent(ICON)
                        .build();

        assertChip(
                chip,
                HORIZONTAL_ALIGN_START,
                BACKGROUND_COLOR,
                staticString(CONTENT_DESCRIPTION),
                Chip.METADATA_TAG_ICON,
                PRIMARY_LABEL,
                SECONDARY_LABEL,
                ICON,
                null);
    }

    @Test
    public void testChipLeftAligned() {
        Chip chip =
                new Chip.Builder(CLICKABLE)
                        .setWidth(dp(WIDTH_DP))
                        .setHeight(dp(HEIGHT_DP))
                        .setBackgroundColor(argb(BACKGROUND_COLOR))
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_START)
                        .setPrimaryLabelContent(PRIMARY_LABEL)
                        .setContentDescription(staticString(CONTENT_DESCRIPTION))
                        .build();
        assertChip(
                chip,
                HORIZONTAL_ALIGN_START,
                BACKGROUND_COLOR,
                staticString(CONTENT_DESCRIPTION),
                Chip.METADATA_TAG_TEXT,
                PRIMARY_LABEL,
                null,
                null,
                null);
    }

    @Test
    public void testChipCustomContentRightAlign() {
        StringProp contentDescription = staticString("Custom chip");
        Chip chip =
                new Chip.Builder(CLICKABLE)
                        .setWidth(dp(WIDTH_DP))
                        .setHeight(dp(HEIGHT_DP))
                        .setBackgroundColor(argb(BACKGROUND_COLOR))
                        .setCustomContent(CUSTOM_CONTENT)
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                        .setContentDescription(contentDescription)
                        .build();

        assertChip(
                chip,
                HORIZONTAL_ALIGN_CENTER,
                BACKGROUND_COLOR,
                contentDescription,
                Chip.METADATA_TAG_CUSTOM_CONTENT,
                null,
                null,
                null,
                CUSTOM_CONTENT);
    }

    private void assertChip(
            @NonNull Chip actualChip,
            @HorizontalAlignment int hAlign,
            @ColorInt int expectedBackgroundColor,
            @Nullable StringProp expectedContDesc,
            @NonNull String expectedMetadata,
            @Nullable LayoutElement expectedPrimaryText,
            @Nullable LayoutElement expectedLabel,
            @Nullable LayoutElement expectedIcon,
            @Nullable LayoutElement expectedCustomContent) {
        assertChipIsEqual(
                actualChip,
                hAlign,
                expectedBackgroundColor,
                expectedContDesc,
                expectedMetadata,
                expectedPrimaryText,
                expectedLabel,
                expectedIcon,
                expectedCustomContent);

        assertFromLayoutElementChipIsEqual(
                actualChip,
                hAlign,
                expectedBackgroundColor,
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
                new Chip.Builder(CLICKABLE)
                        .setWidth(dp(WIDTH_DP))
                        .setHeight(dp(HEIGHT_DP))
                        .setPrimaryLabelContent(PRIMARY_LABEL)
                        .setContentDescription(dynamicContentDescription)
                        .build();

        assertThat(chip.getContentDescription().toProto())
                .isEqualTo(dynamicContentDescription.toProto());
    }

    private void assertFromLayoutElementChipIsEqual(
            @NonNull Chip chip,
            @HorizontalAlignment int hAlign,
            @ColorInt int expectedBackgroundColor,
            @Nullable StringProp expectedContDesc,
            @NonNull String expectedMetadata,
            @Nullable LayoutElement expectedPrimaryText,
            @Nullable LayoutElement expectedLabel,
            @Nullable LayoutElement expectedIcon,
            @Nullable LayoutElement expectedCustomContent) {
        Box box = new Box.Builder().addContent(chip).build();

        Chip newChip = Chip.fromLayoutElement(box.getContents().get(0));

        assertThat(newChip).isNotNull();
        assertChipIsEqual(
                newChip,
                hAlign,
                expectedBackgroundColor,
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
            @ColorInt int expectedBackgroundColor,
            @Nullable StringProp expectedContDesc,
            @NonNull String expectedMetadata,
            @Nullable LayoutElement expectedPrimaryText,
            @Nullable LayoutElement expectedLabel,
            @Nullable LayoutElement expectedIcon,
            @Nullable LayoutElement expectedCustomContent) {
        assertThat(actualChip.getMetadataTag()).isEqualTo(expectedMetadata);
        assertThat(actualChip.getClickable().toProto()).isEqualTo(CLICKABLE.toProto());
        assertThat(
                        actualChip
                                .getWidth()
                                .toContainerDimensionProto()
                                .getLinearDimension()
                                .getValue())
                .isEqualTo(WIDTH_DP);
        assertThat(
                        actualChip
                                .getHeight()
                                .toContainerDimensionProto()
                                .getLinearDimension()
                                .getValue())
                .isEqualTo(HEIGHT_DP);
        assertThat(actualChip.getBackgroundColor().getArgb()).isEqualTo(expectedBackgroundColor);
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
            assertThat(actualChip.getPrimaryLabelContent().toLayoutElementProto())
                    .isEqualTo(expectedPrimaryText.toLayoutElementProto());
        }

        if (expectedLabel == null) {
            assertThat(actualChip.getSecondaryLabelContent()).isNull();
        } else {
            assertThat(actualChip.getSecondaryLabelContent().toLayoutElementProto())
                    .isEqualTo(expectedLabel.toLayoutElementProto());
        }

        if (expectedIcon == null) {
            assertThat(actualChip.getIconContent()).isNull();
        } else {
            assertThat(actualChip.getIconContent().toLayoutElementProto())
                    .isEqualTo(expectedIcon.toLayoutElementProto());
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
