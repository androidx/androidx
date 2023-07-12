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

package androidx.wear.tiles.material.layouts;

import static androidx.wear.tiles.material.layouts.LayoutDefaults.DEFAULT_VERTICAL_SPACER_HEIGHT;

import static com.google.common.truth.Truth.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.tiles.material.CompactChip;
import androidx.wear.tiles.material.Text;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
@SuppressWarnings("deprecation")
public class PrimaryLayoutTest {
    private static final androidx.wear.tiles.ModifiersBuilders.Clickable CLICKABLE =
            new androidx.wear.tiles.ModifiersBuilders.Clickable.Builder()
                    .setOnClick(
                            new androidx.wear.tiles.ActionBuilders.LaunchAction.Builder().build())
                    .setId("action_id")
                    .build();
    private static final Context CONTEXT = ApplicationProvider.getApplicationContext();
    private static final androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
            DEVICE_PARAMETERS =
                    new androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters.Builder()
                            .setScreenWidthDp(192)
                            .setScreenHeightDp(192)
                            .build();
    private static final androidx.wear.tiles.LayoutElementBuilders.LayoutElement CONTENT =
            new androidx.wear.tiles.LayoutElementBuilders.Box.Builder().build();
    private static final CompactChip PRIMARY_CHIP =
            new CompactChip.Builder(CONTEXT, "Compact", CLICKABLE, DEVICE_PARAMETERS).build();
    private static final Text PRIMARY_LABEL = new Text.Builder(CONTEXT, "Primary label").build();
    private static final Text SECONDARY_LABEL =
            new Text.Builder(CONTEXT, "Secondary label").build();

    @Test
    public void testOnlyContent() {
        PrimaryLayout layout =
                new PrimaryLayout.Builder(DEVICE_PARAMETERS).setContent(CONTENT).build();

        assertLayout(DEFAULT_VERTICAL_SPACER_HEIGHT.getValue(), layout, CONTENT, null, null, null);
    }

    @Test
    public void testContentChip() {
        PrimaryLayout layout =
                new PrimaryLayout.Builder(DEVICE_PARAMETERS)
                        .setContent(CONTENT)
                        .setPrimaryChipContent(PRIMARY_CHIP)
                        .build();

        assertLayout(
                DEFAULT_VERTICAL_SPACER_HEIGHT.getValue(),
                layout,
                CONTENT,
                PRIMARY_CHIP,
                null,
                null);
    }

    @Test
    public void testContentPrimaryLabel() {
        PrimaryLayout layout =
                new PrimaryLayout.Builder(DEVICE_PARAMETERS)
                        .setContent(CONTENT)
                        .setPrimaryLabelTextContent(PRIMARY_LABEL)
                        .build();

        assertLayout(
                DEFAULT_VERTICAL_SPACER_HEIGHT.getValue(),
                layout,
                CONTENT,
                null,
                PRIMARY_LABEL,
                null);
    }

    @Test
    public void testContentSecondaryLabel() {
        PrimaryLayout layout =
                new PrimaryLayout.Builder(DEVICE_PARAMETERS)
                        .setContent(CONTENT)
                        .setSecondaryLabelTextContent(SECONDARY_LABEL)
                        .build();

        assertLayout(
                DEFAULT_VERTICAL_SPACER_HEIGHT.getValue(),
                layout,
                CONTENT,
                null,
                null,
                SECONDARY_LABEL);
    }

    @Test
    public void testAll() {
        float height = 12;
        PrimaryLayout layout =
                new PrimaryLayout.Builder(DEVICE_PARAMETERS)
                        .setContent(CONTENT)
                        .setPrimaryChipContent(PRIMARY_CHIP)
                        .setPrimaryLabelTextContent(PRIMARY_LABEL)
                        .setSecondaryLabelTextContent(SECONDARY_LABEL)
                        .setVerticalSpacerHeight(height)
                        .build();

        assertLayout(height, layout, CONTENT, PRIMARY_CHIP, PRIMARY_LABEL, SECONDARY_LABEL);
    }

    @Test
    public void testWrongElement() {
        androidx.wear.tiles.LayoutElementBuilders.Column box =
                new androidx.wear.tiles.LayoutElementBuilders.Column.Builder().build();

        assertThat(PrimaryLayout.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongBox() {
        androidx.wear.tiles.LayoutElementBuilders.Box box =
                new androidx.wear.tiles.LayoutElementBuilders.Box.Builder().build();

        assertThat(PrimaryLayout.fromLayoutElement(box)).isNull();
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

        assertThat(PrimaryLayout.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongLengthTag() {
        androidx.wear.tiles.LayoutElementBuilders.Box box =
                new androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                        .setModifiers(
                                new androidx.wear.tiles.ModifiersBuilders.Modifiers.Builder()
                                        .setMetadata(
                                                new androidx.wear.tiles.ModifiersBuilders
                                                                .ElementMetadata.Builder()
                                                        .setTagData(
                                                                PrimaryLayout.METADATA_TAG_PREFIX
                                                                        .getBytes(UTF_8))
                                                        .build())
                                        .build())
                        .build();

        assertThat(PrimaryLayout.fromLayoutElement(box)).isNull();
    }

    private void assertLayout(
            float height,
            @NonNull PrimaryLayout actualLayout,
            @Nullable androidx.wear.tiles.LayoutElementBuilders.LayoutElement expectedContent,
            @Nullable androidx.wear.tiles.LayoutElementBuilders.LayoutElement expectedPrimaryChip,
            @Nullable androidx.wear.tiles.LayoutElementBuilders.LayoutElement expectedPrimaryLabel,
            @Nullable
                    androidx.wear.tiles.LayoutElementBuilders.LayoutElement
                            expectedSecondaryLabel) {
        assertLayoutIsEqual(
                height,
                actualLayout,
                expectedContent,
                expectedPrimaryChip,
                expectedPrimaryLabel,
                expectedSecondaryLabel);

        androidx.wear.tiles.LayoutElementBuilders.Box box =
                new androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                        .addContent(actualLayout)
                        .build();

        PrimaryLayout newLayout = PrimaryLayout.fromLayoutElement(box.getContents().get(0));

        assertThat(newLayout).isNotNull();
        assertLayoutIsEqual(
                height,
                newLayout,
                expectedContent,
                expectedPrimaryChip,
                expectedPrimaryLabel,
                expectedSecondaryLabel);

        assertThat(PrimaryLayout.fromLayoutElement(actualLayout)).isEqualTo(actualLayout);
    }

    private void assertLayoutIsEqual(
            float height,
            @NonNull PrimaryLayout actualLayout,
            @Nullable androidx.wear.tiles.LayoutElementBuilders.LayoutElement expectedContent,
            @Nullable androidx.wear.tiles.LayoutElementBuilders.LayoutElement expectedPrimaryChip,
            @Nullable androidx.wear.tiles.LayoutElementBuilders.LayoutElement expectedPrimaryLabel,
            @Nullable
                    androidx.wear.tiles.LayoutElementBuilders.LayoutElement
                            expectedSecondaryLabel) {
        byte[] expectedMetadata = PrimaryLayout.METADATA_TAG_BASE.clone();

        if (expectedContent == null) {
            assertThat(actualLayout.getContent()).isNull();
        } else {
            assertThat(actualLayout.getContent().toLayoutElementProto())
                    .isEqualTo(expectedContent.toLayoutElementProto());
            expectedMetadata[PrimaryLayout.FLAG_INDEX] =
                    (byte)
                            (expectedMetadata[PrimaryLayout.FLAG_INDEX]
                                    | PrimaryLayout.CONTENT_PRESENT);
        }

        if (expectedPrimaryChip == null) {
            assertThat(actualLayout.getPrimaryChipContent()).isNull();
        } else {
            assertThat(actualLayout.getPrimaryChipContent().toLayoutElementProto())
                    .isEqualTo(expectedPrimaryChip.toLayoutElementProto());
            expectedMetadata[PrimaryLayout.FLAG_INDEX] =
                    (byte)
                            (expectedMetadata[PrimaryLayout.FLAG_INDEX]
                                    | PrimaryLayout.CHIP_PRESENT);
        }

        assertThat(actualLayout.getVerticalSpacerHeight()).isEqualTo(height);

        if (expectedPrimaryLabel == null) {
            assertThat(actualLayout.getPrimaryLabelTextContent()).isNull();
        } else {
            assertThat(actualLayout.getPrimaryLabelTextContent().toLayoutElementProto())
                    .isEqualTo(expectedPrimaryLabel.toLayoutElementProto());
            expectedMetadata[PrimaryLayout.FLAG_INDEX] =
                    (byte)
                            (expectedMetadata[PrimaryLayout.FLAG_INDEX]
                                    | PrimaryLayout.PRIMARY_LABEL_PRESENT);
        }

        if (expectedSecondaryLabel == null) {
            assertThat(actualLayout.getSecondaryLabelTextContent()).isNull();
        } else {
            assertThat(actualLayout.getSecondaryLabelTextContent().toLayoutElementProto())
                    .isEqualTo(expectedSecondaryLabel.toLayoutElementProto());
            expectedMetadata[PrimaryLayout.FLAG_INDEX] =
                    (byte)
                            (expectedMetadata[PrimaryLayout.FLAG_INDEX]
                                    | PrimaryLayout.SECONDARY_LABEL_PRESENT);
        }

        assertThat(actualLayout.getMetadataTag()).isEqualTo(expectedMetadata);
    }
}
