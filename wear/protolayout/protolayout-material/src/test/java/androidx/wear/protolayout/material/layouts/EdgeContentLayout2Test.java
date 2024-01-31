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

import static com.google.common.truth.Truth.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.protolayout.LayoutElementBuilders.Box;
import androidx.wear.protolayout.LayoutElementBuilders.Column;
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement;
import androidx.wear.protolayout.ModifiersBuilders.ElementMetadata;
import androidx.wear.protolayout.ModifiersBuilders.Modifiers;
import androidx.wear.protolayout.material.CircularProgressIndicator;
import androidx.wear.protolayout.material.Text;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
public class EdgeContentLayout2Test {
    private static final Context CONTEXT = ApplicationProvider.getApplicationContext();
    private static final DeviceParameters DEVICE_PARAMETERS =
            new DeviceParameters.Builder().setScreenWidthDp(192).setScreenHeightDp(192).build();
    private static final Text PRIMARY_LABEL =
            new Text.Builder(CONTEXT, "Primary label").build();
    private static final Text SECONDARY_LABEL =
            new Text.Builder(CONTEXT, "Secondary label").build();

    @Test
    public void testAll() {
        LayoutElement content = new Box.Builder().build();
        CircularProgressIndicator progressIndicator =
                new CircularProgressIndicator.Builder().build();
        int edgeContentThickness = 20;
        EdgeContentLayout2 layout =
                new EdgeContentLayout2.Builder(DEVICE_PARAMETERS)
                        .setContent(content)
                        .setEdgeContent(progressIndicator)
                        .setPrimaryLabelContent(PRIMARY_LABEL)
                        .setSecondaryLabelContent(SECONDARY_LABEL)
                        .setEdgeContentThickness(edgeContentThickness)
                        .build();

        assertLayout(
                layout,
                progressIndicator,
                content,
                PRIMARY_LABEL,
                SECONDARY_LABEL,
                edgeContentThickness);
    }

    @Test
    public void testAll_defaultThickness() {
        LayoutElement content = new Box.Builder().build();
        CircularProgressIndicator progressIndicator =
                new CircularProgressIndicator.Builder().build();
        EdgeContentLayout2 layout =
                new EdgeContentLayout2.Builder(DEVICE_PARAMETERS)
                        .setContent(content)
                        .setEdgeContent(progressIndicator)
                        .setPrimaryLabelContent(PRIMARY_LABEL)
                        .setSecondaryLabelContent(SECONDARY_LABEL)
                        .build();

        assertLayout(
                layout,
                progressIndicator,
                content,
                PRIMARY_LABEL,
                SECONDARY_LABEL,
                progressIndicator.getStrokeWidth().getValue());
    }

    @Test
    public void testContentOnly() {
        LayoutElement content = new Box.Builder().build();
        EdgeContentLayout2 layout =
                new EdgeContentLayout2.Builder(DEVICE_PARAMETERS).setContent(content).build();

        assertLayout(
                layout,
                /* expectedProgressIndicator= */ null,
                content,
                /* expectedPrimaryLabel= */ null,
                /* expectedSecondaryLabel= */ null,
                /* expectedEdgeContentThickness= */ 0);
    }

    @Test
    public void testIndicatorOnly() {
        CircularProgressIndicator progressIndicator =
                new CircularProgressIndicator.Builder().build();
        EdgeContentLayout2 layout =
                new EdgeContentLayout2.Builder(DEVICE_PARAMETERS)
                        .setEdgeContent(progressIndicator).build();

        assertLayout(
                layout,
                progressIndicator,
                /* expectedContent= */ null,
                /* expectedPrimaryLabel= */ null,
                /* expectedSecondaryLabel= */ null,
                progressIndicator.getStrokeWidth().getValue());
    }

    @Test
    public void testEmpty() {
        EdgeContentLayout2 layout = new EdgeContentLayout2.Builder(DEVICE_PARAMETERS).build();

        assertLayout(
                layout,
                /* expectedProgressIndicator= */ null,
                /* expectedContent= */ null,
                /* expectedPrimaryLabel= */ null,
                /* expectedSecondaryLabel= */ null,
                /* expectedEdgeContentThickness= */ 0);
    }

    @Test
    public void testWrongElement() {
        Column box = new Column.Builder().build();

        assertThat(EdgeContentLayout2.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongBox() {
        Box box = new Box.Builder().build();

        assertThat(EdgeContentLayout2.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongTag() {
        Box box =
                new Box.Builder()
                        .setModifiers(
                                new Modifiers.Builder()
                                        .setMetadata(
                                                new ElementMetadata.Builder()
                                                        .setTagData("test".getBytes(UTF_8)).build())
                                        .build())
                        .build();

        assertThat(EdgeContentLayout2.fromLayoutElement(box)).isNull();
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
                                                                EdgeContentLayout2
                                                                        .METADATA_TAG_PREFIX
                                                                        .getBytes(UTF_8))
                                                        .build())
                                        .build())
                        .build();

        assertThat(EdgeContentLayout2.fromLayoutElement(box)).isNull();
    }

    private void assertLayout(
            @NonNull EdgeContentLayout2 actualLayout,
            @Nullable LayoutElement expectedProgressIndicator,
            @Nullable LayoutElement expectedContent,
            @Nullable LayoutElement expectedPrimaryLabel,
            @Nullable LayoutElement expectedSecondaryLabel,
            float expectedEdgeContentThickness) {
        assertLayoutIsEqual(
                actualLayout,
                expectedProgressIndicator,
                expectedContent,
                expectedPrimaryLabel,
                expectedSecondaryLabel,
                expectedEdgeContentThickness);

        Box box = new Box.Builder().addContent(actualLayout).build();

        EdgeContentLayout2 newLayout = EdgeContentLayout2
                .fromLayoutElement(box.getContents().get(0));

        assertThat(newLayout).isNotNull();
        assertLayoutIsEqual(
                newLayout,
                expectedProgressIndicator,
                expectedContent,
                expectedPrimaryLabel,
                expectedSecondaryLabel,
                expectedEdgeContentThickness);

        assertThat(EdgeContentLayout2.fromLayoutElement(actualLayout)).isEqualTo(actualLayout);
    }

    private void assertLayoutIsEqual(
            @NonNull EdgeContentLayout2 actualLayout,
            @Nullable LayoutElement expectedProgressIndicator,
            @Nullable LayoutElement expectedContent,
            @Nullable LayoutElement expectedPrimaryLabel,
            @Nullable LayoutElement expectedSecondaryLabel,
            float expectedEdgeContentThickness) {
        byte[] expectedMetadata = EdgeContentLayout2.METADATA_TAG_BASE.clone();

        if (expectedProgressIndicator == null) {
            assertThat(actualLayout.getEdgeContent()).isNull();
        } else {
            assertThat(actualLayout.getEdgeContent().toLayoutElementProto())
                    .isEqualTo(expectedProgressIndicator.toLayoutElementProto());
            expectedMetadata[EdgeContentLayout2.FLAG_INDEX] =
                    (byte)
                            (expectedMetadata[EdgeContentLayout2.FLAG_INDEX]
                                    | EdgeContentLayout2.EDGE_CONTENT_PRESENT);
        }

        if (expectedContent == null) {
            assertThat(actualLayout.getContent()).isNull();
        } else {
            assertThat(actualLayout.getContent().toLayoutElementProto())
                    .isEqualTo(expectedContent.toLayoutElementProto());
            expectedMetadata[EdgeContentLayout2.FLAG_INDEX] =
                    (byte)
                            (expectedMetadata[EdgeContentLayout2.FLAG_INDEX]
                                    | EdgeContentLayout2.CONTENT_PRESENT);
        }

        if (expectedPrimaryLabel == null) {
            assertThat(actualLayout.getPrimaryLabelContent()).isNull();
        } else {
            assertThat(actualLayout.getPrimaryLabelContent().toLayoutElementProto())
                    .isEqualTo(expectedPrimaryLabel.toLayoutElementProto());
            expectedMetadata[EdgeContentLayout2.FLAG_INDEX] =
                    (byte)
                            (expectedMetadata[EdgeContentLayout2.FLAG_INDEX]
                                    | EdgeContentLayout2.PRIMARY_LABEL_PRESENT);
        }

        if (expectedSecondaryLabel == null) {
            assertThat(actualLayout.getSecondaryLabelContent()).isNull();
        } else {
            assertThat(actualLayout.getSecondaryLabelContent().toLayoutElementProto())
                    .isEqualTo(expectedSecondaryLabel.toLayoutElementProto());
            expectedMetadata[EdgeContentLayout2.FLAG_INDEX] =
                    (byte)
                            (expectedMetadata[EdgeContentLayout2.FLAG_INDEX]
                                    | EdgeContentLayout2.SECONDARY_LABEL_PRESENT);
        }

        assertThat(actualLayout.getEdgeContentThickness()).isEqualTo(expectedEdgeContentThickness);

        assertThat(actualLayout.getMetadataTag()).isEqualTo(expectedMetadata);
    }
}
