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

package androidx.wear.protolayout.material.layouts;

import static androidx.wear.protolayout.material.layouts.LayoutDefaults.DEFAULT_VERTICAL_SPACER_HEIGHT;

import static com.google.common.truth.Truth.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.ActionBuilders.LaunchAction;
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.protolayout.LayoutElementBuilders.Box;
import androidx.wear.protolayout.LayoutElementBuilders.Column;
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement;
import androidx.wear.protolayout.ModifiersBuilders.Clickable;
import androidx.wear.protolayout.ModifiersBuilders.ElementMetadata;
import androidx.wear.protolayout.ModifiersBuilders.Modifiers;
import androidx.wear.protolayout.material.CompactChip;
import androidx.wear.protolayout.material.Text;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
public class PrimaryLayoutTest {
    private static final Clickable CLICKABLE =
            new Clickable.Builder()
                    .setOnClick(new LaunchAction.Builder().build())
                    .setId("action_id")
                    .build();
    private static final Context CONTEXT = ApplicationProvider.getApplicationContext();
    private static final DeviceParameters DEVICE_PARAMETERS =
            new DeviceParameters.Builder().setScreenWidthDp(192).setScreenHeightDp(192).build();
    private static final LayoutElement CONTENT = new Box.Builder().build();
    private static final CompactChip PRIMARY_CHIP =
            new CompactChip.Builder(CONTEXT, "Compact", CLICKABLE, DEVICE_PARAMETERS)
                    .build();
    private static final Text PRIMARY_LABEL = new Text.Builder(CONTEXT, "Primary label").build();
    private static final Text SECONDARY_LABEL =
            new Text.Builder(CONTEXT, "Secondary label").build();

    @Test
    public void testOnlyContent() {
        PrimaryLayout layout =
                new PrimaryLayout.Builder(DEVICE_PARAMETERS).setContent(CONTENT).build();

        assertLayout(
                DEFAULT_VERTICAL_SPACER_HEIGHT.getValue(),
                layout,
                CONTENT,
                /* expectedPrimaryChip= */ null,
                /* expectedPrimaryLabel= */ null,
                /* expectedSecondaryLabel= */ null,
                /* isResponsive= */ false);
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
                /* expectedPrimaryLabel= */ null,
                /* expectedSecondaryLabel= */ null,
                /* isResponsive= */ false);
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
                /* expectedPrimaryChip= */ null,
                PRIMARY_LABEL,
                /* expectedSecondaryLabel= */ null,
                /* isResponsive= */ false);
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
                /* expectedPrimaryChip= */ null,
                /* expectedPrimaryLabel= */ null,
                SECONDARY_LABEL,
                /* isResponsive= */ false);
    }

    @Test
    public void testContentSecondaryLabel_responsiveSecondaryLabel() {
        PrimaryLayout layout =
                new PrimaryLayout.Builder(DEVICE_PARAMETERS)
                        .setContent(CONTENT)
                        .setSecondaryLabelTextContent(SECONDARY_LABEL)
                        .setResponsiveContentInsetEnabled(true)
                        .build();

        assertLayout(
                DEFAULT_VERTICAL_SPACER_HEIGHT.getValue(),
                layout,
                CONTENT,
                /* expectedPrimaryChip= */ null,
                /* expectedPrimaryLabel= */ null,
                SECONDARY_LABEL,
                /* isResponsive= */ true);
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
                        .setResponsiveContentInsetEnabled(true)
                        // Test that the bit was flipped correctly.
                        .setResponsiveContentInsetEnabled(false)
                        .build();

        assertLayout(
                height,
                layout,
                CONTENT,
                PRIMARY_CHIP,
                PRIMARY_LABEL,
                SECONDARY_LABEL,
                /* isResponsive= */ false);
    }

    @Test
    public void testAll_responsive() {
        float height = 12;
        PrimaryLayout layout =
                new PrimaryLayout.Builder(DEVICE_PARAMETERS)
                        .setContent(CONTENT)
                        .setPrimaryChipContent(PRIMARY_CHIP)
                        .setPrimaryLabelTextContent(PRIMARY_LABEL)
                        .setSecondaryLabelTextContent(SECONDARY_LABEL)
                        .setVerticalSpacerHeight(height)
                        .setResponsiveContentInsetEnabled(true)
                        .build();

        // Secondary label doesn't have extra padding.
        assertLayout(
                height,
                layout,
                CONTENT,
                PRIMARY_CHIP,
                PRIMARY_LABEL,
                SECONDARY_LABEL,
                /* isResponsive= */ true);
    }

    @Test
    public void testWrongElement() {
        Column box = new Column.Builder().build();

        assertThat(PrimaryLayout.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongBox() {
        Box box = new Box.Builder().build();

        assertThat(PrimaryLayout.fromLayoutElement(box)).isNull();
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

        assertThat(PrimaryLayout.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongLengthTag() {
        Box box =
                new Box.Builder()
                        .setModifiers(
                                new Modifiers.Builder()
                                        .setMetadata(
                                                new ElementMetadata.Builder()
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
            @Nullable LayoutElement expectedContent,
            @Nullable LayoutElement expectedPrimaryChip,
            @Nullable LayoutElement expectedPrimaryLabel,
            @Nullable LayoutElement expectedSecondaryLabel,
            boolean isResponsive) {
        assertLayoutIsEqual(
                height,
                actualLayout,
                expectedContent,
                expectedPrimaryChip,
                expectedPrimaryLabel,
                expectedSecondaryLabel,
                isResponsive);

        Box box = new Box.Builder().addContent(actualLayout).build();

        PrimaryLayout newLayout = PrimaryLayout.fromLayoutElement(box.getContents().get(0));

        assertThat(newLayout).isNotNull();
        assertLayoutIsEqual(
                height,
                newLayout,
                expectedContent,
                expectedPrimaryChip,
                expectedPrimaryLabel,
                expectedSecondaryLabel,
                isResponsive);

        assertThat(PrimaryLayout.fromLayoutElement(actualLayout)).isEqualTo(actualLayout);
    }

    private void assertLayoutIsEqual(
            float height,
            @NonNull PrimaryLayout actualLayout,
            @Nullable LayoutElement expectedContent,
            @Nullable LayoutElement expectedPrimaryChip,
            @Nullable LayoutElement expectedPrimaryLabel,
            @Nullable LayoutElement expectedSecondaryLabel,
            boolean isResponsive) {
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

        if (isResponsive) {
            expectedMetadata[PrimaryLayout.FLAG_INDEX] =
                    (byte) (expectedMetadata[PrimaryLayout.FLAG_INDEX]
                            | PrimaryLayout.CONTENT_INSET_USED);
        }

        assertThat(actualLayout.getMetadataTag()).isEqualTo(expectedMetadata);
        assertThat(actualLayout.isResponsiveContentInsetEnabled()).isEqualTo(isResponsive);
    }
}
