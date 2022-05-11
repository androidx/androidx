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

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.tiles.ActionBuilders.LaunchAction;
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.tiles.LayoutElementBuilders.Box;
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement;
import androidx.wear.tiles.ModifiersBuilders.Clickable;
import androidx.wear.tiles.material.CompactChip;
import androidx.wear.tiles.material.Text;

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
            new CompactChip.Builder(CONTEXT, "Compact", CLICKABLE, DEVICE_PARAMETERS).build();
    private static final Text PRIMARY_LABEL = new Text.Builder(CONTEXT, "Primary label").build();
    private static final Text SECONDARY_LABEL =
            new Text.Builder(CONTEXT, "Secondary label").build();

    @Test
    public void testOnlyContent() {
        PrimaryLayout layout =
                new PrimaryLayout.Builder(DEVICE_PARAMETERS).setContent(CONTENT).build();

        assertThat(layout.getContent()).isNotNull();
        assertThat(layout.getContent().toLayoutElementProto())
                .isEqualTo(CONTENT.toLayoutElementProto());
        assertThat(layout.getVerticalSpacerHeight())
                .isEqualTo(DEFAULT_VERTICAL_SPACER_HEIGHT.getValue());

        assertThat(layout.getPrimaryChipContent()).isNull();
        assertThat(layout.getPrimaryLabelTextContent()).isNull();
        assertThat(layout.getSecondaryLabelTextContent()).isNull();
    }

    @Test
    public void testContentChip() {
        PrimaryLayout layout =
                new PrimaryLayout.Builder(DEVICE_PARAMETERS)
                        .setContent(CONTENT)
                        .setPrimaryChipContent(PRIMARY_CHIP)
                        .build();

        assertThat(layout.getContent()).isNotNull();
        assertThat(layout.getContent().toLayoutElementProto())
                .isEqualTo(CONTENT.toLayoutElementProto());
        assertThat(layout.getVerticalSpacerHeight())
                .isEqualTo(DEFAULT_VERTICAL_SPACER_HEIGHT.getValue());
        assertThat(layout.getPrimaryChipContent()).isNotNull();
        assertThat(layout.getPrimaryChipContent().toLayoutElementProto())
                .isEqualTo(PRIMARY_CHIP.toLayoutElementProto());

        assertThat(layout.getPrimaryLabelTextContent()).isNull();
        assertThat(layout.getSecondaryLabelTextContent()).isNull();
    }

    @Test
    public void testContentPrimaryLabel() {
        PrimaryLayout layout =
                new PrimaryLayout.Builder(DEVICE_PARAMETERS)
                        .setContent(CONTENT)
                        .setPrimaryLabelTextContent(PRIMARY_LABEL)
                        .build();

        assertThat(layout.getContent()).isNotNull();
        assertThat(layout.getContent().toLayoutElementProto())
                .isEqualTo(CONTENT.toLayoutElementProto());
        assertThat(layout.getVerticalSpacerHeight())
                .isEqualTo(DEFAULT_VERTICAL_SPACER_HEIGHT.getValue());
        assertThat(layout.getPrimaryLabelTextContent()).isNotNull();
        assertThat(layout.getPrimaryLabelTextContent().toLayoutElementProto())
                .isEqualTo(PRIMARY_LABEL.toLayoutElementProto());

        assertThat(layout.getPrimaryChipContent()).isNull();
        assertThat(layout.getSecondaryLabelTextContent()).isNull();
    }

    @Test
    public void testContentSecondaryLabel() {
        PrimaryLayout layout =
                new PrimaryLayout.Builder(DEVICE_PARAMETERS)
                        .setContent(CONTENT)
                        .setSecondaryLabelTextContent(SECONDARY_LABEL)
                        .build();

        assertThat(layout.getContent()).isNotNull();
        assertThat(layout.getContent().toLayoutElementProto())
                .isEqualTo(CONTENT.toLayoutElementProto());
        assertThat(layout.getVerticalSpacerHeight())
                .isEqualTo(DEFAULT_VERTICAL_SPACER_HEIGHT.getValue());
        assertThat(layout.getSecondaryLabelTextContent()).isNotNull();
        assertThat(layout.getSecondaryLabelTextContent().toLayoutElementProto())
                .isEqualTo(SECONDARY_LABEL.toLayoutElementProto());

        assertThat(layout.getPrimaryChipContent()).isNull();
        assertThat(layout.getPrimaryLabelTextContent()).isNull();
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

        assertThat(layout.getContent()).isNotNull();
        assertThat(layout.getContent().toLayoutElementProto())
                .isEqualTo(CONTENT.toLayoutElementProto());
        assertThat(layout.getPrimaryChipContent()).isNotNull();
        assertThat(layout.getPrimaryChipContent().toLayoutElementProto())
                .isEqualTo(PRIMARY_CHIP.toLayoutElementProto());
        assertThat(layout.getVerticalSpacerHeight()).isEqualTo(height);
        assertThat(layout.getPrimaryLabelTextContent()).isNotNull();
        assertThat(layout.getPrimaryLabelTextContent().toLayoutElementProto())
                .isEqualTo(PRIMARY_LABEL.toLayoutElementProto());
        assertThat(layout.getSecondaryLabelTextContent()).isNotNull();
        assertThat(layout.getSecondaryLabelTextContent().toLayoutElementProto())
                .isEqualTo(SECONDARY_LABEL.toLayoutElementProto());
    }
}
