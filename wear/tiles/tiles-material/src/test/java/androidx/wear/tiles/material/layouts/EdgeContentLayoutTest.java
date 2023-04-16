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

import static com.google.common.truth.Truth.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.tiles.material.CircularProgressIndicator;
import androidx.wear.tiles.material.Text;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
@SuppressWarnings("deprecation")
public class EdgeContentLayoutTest {
    private static final Context CONTEXT = ApplicationProvider.getApplicationContext();
    private static final androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
            DEVICE_PARAMETERS =
                    new androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters.Builder()
                            .setScreenWidthDp(192)
                            .setScreenHeightDp(192)
                            .build();
    private static final Text PRIMARY_LABEL = new Text.Builder(CONTEXT, "Primary label").build();
    private static final Text SECONDARY_LABEL =
            new Text.Builder(CONTEXT, "Secondary label").build();

    @Test
    public void testAll() {
        androidx.wear.tiles.LayoutElementBuilders.LayoutElement content =
                new androidx.wear.tiles.LayoutElementBuilders.Box.Builder().build();
        CircularProgressIndicator progressIndicator =
                new CircularProgressIndicator.Builder().build();
        EdgeContentLayout layout =
                new EdgeContentLayout.Builder(DEVICE_PARAMETERS)
                        .setContent(content)
                        .setEdgeContent(progressIndicator)
                        .setPrimaryLabelTextContent(PRIMARY_LABEL)
                        .setSecondaryLabelTextContent(SECONDARY_LABEL)
                        .build();

        assertLayout(layout, progressIndicator, content, PRIMARY_LABEL, SECONDARY_LABEL);
    }

    @Test
    public void testContentOnly() {
        androidx.wear.tiles.LayoutElementBuilders.LayoutElement content =
                new androidx.wear.tiles.LayoutElementBuilders.Box.Builder().build();
        EdgeContentLayout layout =
                new EdgeContentLayout.Builder(DEVICE_PARAMETERS).setContent(content).build();

        assertLayout(layout, null, content, null, null);
    }

    @Test
    public void testIndicatorOnly() {
        CircularProgressIndicator progressIndicator =
                new CircularProgressIndicator.Builder().build();
        EdgeContentLayout layout =
                new EdgeContentLayout.Builder(DEVICE_PARAMETERS)
                        .setEdgeContent(progressIndicator)
                        .build();

        assertLayout(layout, progressIndicator, null, null, null);
    }

    @Test
    public void testEmpty() {
        EdgeContentLayout layout = new EdgeContentLayout.Builder(DEVICE_PARAMETERS).build();

        assertLayout(layout, null, null, null, null);
    }

    @Test
    public void testWrongElement() {
        androidx.wear.tiles.LayoutElementBuilders.Column box =
                new androidx.wear.tiles.LayoutElementBuilders.Column.Builder().build();

        assertThat(EdgeContentLayout.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongBox() {
        androidx.wear.tiles.LayoutElementBuilders.Box box =
                new androidx.wear.tiles.LayoutElementBuilders.Box.Builder().build();

        assertThat(EdgeContentLayout.fromLayoutElement(box)).isNull();
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

        assertThat(EdgeContentLayout.fromLayoutElement(box)).isNull();
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
                                                                EdgeContentLayout
                                                                        .METADATA_TAG_PREFIX
                                                                        .getBytes(UTF_8))
                                                        .build())
                                        .build())
                        .build();

        assertThat(EdgeContentLayout.fromLayoutElement(box)).isNull();
    }

    private void assertLayout(
            @NonNull EdgeContentLayout actualLayout,
            @Nullable
                    androidx.wear.tiles.LayoutElementBuilders.LayoutElement
                            expectedProgressIndicator,
            @Nullable androidx.wear.tiles.LayoutElementBuilders.LayoutElement expectedContent,
            @Nullable androidx.wear.tiles.LayoutElementBuilders.LayoutElement expectedPrimaryLabel,
            @Nullable
                    androidx.wear.tiles.LayoutElementBuilders.LayoutElement
                            expectedSecondaryLabel) {
        assertLayoutIsEqual(
                actualLayout,
                expectedProgressIndicator,
                expectedContent,
                expectedPrimaryLabel,
                expectedSecondaryLabel);

        androidx.wear.tiles.LayoutElementBuilders.Box box =
                new androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                        .addContent(actualLayout)
                        .build();

        EdgeContentLayout newLayout = EdgeContentLayout.fromLayoutElement(box.getContents().get(0));

        assertThat(newLayout).isNotNull();
        assertLayoutIsEqual(
                newLayout,
                expectedProgressIndicator,
                expectedContent,
                expectedPrimaryLabel,
                expectedSecondaryLabel);

        assertThat(EdgeContentLayout.fromLayoutElement(actualLayout)).isEqualTo(actualLayout);
    }

    private void assertLayoutIsEqual(
            @NonNull EdgeContentLayout actualLayout,
            @Nullable
                    androidx.wear.tiles.LayoutElementBuilders.LayoutElement
                            expectedProgressIndicator,
            @Nullable androidx.wear.tiles.LayoutElementBuilders.LayoutElement expectedContent,
            @Nullable androidx.wear.tiles.LayoutElementBuilders.LayoutElement expectedPrimaryLabel,
            @Nullable
                    androidx.wear.tiles.LayoutElementBuilders.LayoutElement
                            expectedSecondaryLabel) {
        byte[] expectedMetadata = EdgeContentLayout.METADATA_TAG_BASE.clone();

        if (expectedProgressIndicator == null) {
            assertThat(actualLayout.getEdgeContent()).isNull();
        } else {
            assertThat(actualLayout.getEdgeContent().toLayoutElementProto())
                    .isEqualTo(expectedProgressIndicator.toLayoutElementProto());
            expectedMetadata[EdgeContentLayout.FLAG_INDEX] =
                    (byte)
                            (expectedMetadata[EdgeContentLayout.FLAG_INDEX]
                                    | EdgeContentLayout.EDGE_CONTENT_PRESENT);
        }

        if (expectedContent == null) {
            assertThat(actualLayout.getContent()).isNull();
        } else {
            assertThat(actualLayout.getContent().toLayoutElementProto())
                    .isEqualTo(expectedContent.toLayoutElementProto());
            expectedMetadata[EdgeContentLayout.FLAG_INDEX] =
                    (byte)
                            (expectedMetadata[EdgeContentLayout.FLAG_INDEX]
                                    | EdgeContentLayout.CONTENT_PRESENT);
        }

        if (expectedPrimaryLabel == null) {
            assertThat(actualLayout.getPrimaryLabelTextContent()).isNull();
        } else {
            assertThat(actualLayout.getPrimaryLabelTextContent().toLayoutElementProto())
                    .isEqualTo(expectedPrimaryLabel.toLayoutElementProto());
            expectedMetadata[EdgeContentLayout.FLAG_INDEX] =
                    (byte)
                            (expectedMetadata[EdgeContentLayout.FLAG_INDEX]
                                    | EdgeContentLayout.PRIMARY_LABEL_PRESENT);
        }

        if (expectedSecondaryLabel == null) {
            assertThat(actualLayout.getSecondaryLabelTextContent()).isNull();
        } else {
            assertThat(actualLayout.getSecondaryLabelTextContent().toLayoutElementProto())
                    .isEqualTo(expectedSecondaryLabel.toLayoutElementProto());
            expectedMetadata[EdgeContentLayout.FLAG_INDEX] =
                    (byte)
                            (expectedMetadata[EdgeContentLayout.FLAG_INDEX]
                                    | EdgeContentLayout.SECONDARY_LABEL_PRESENT);
        }

        assertThat(actualLayout.getMetadataTag()).isEqualTo(expectedMetadata);
    }
}
