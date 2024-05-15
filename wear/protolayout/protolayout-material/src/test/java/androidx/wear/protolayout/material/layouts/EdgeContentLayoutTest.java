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

import static org.junit.Assert.assertThrows;

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
public class EdgeContentLayoutTest {
    private static final Context CONTEXT = ApplicationProvider.getApplicationContext();
    private static final DeviceParameters DEVICE_PARAMETERS =
            new DeviceParameters.Builder().setScreenWidthDp(192).setScreenHeightDp(192).build();
    private static final Text PRIMARY_LABEL = new Text.Builder(CONTEXT, "Primary label").build();
    private static final Text SECONDARY_LABEL =
            new Text.Builder(CONTEXT, "Secondary label").build();

    @Test
    public void testAll() {
        LayoutElement content = new Box.Builder().build();
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
        assertThat(layout.isEdgeContentBehindAllOtherContent()).isFalse();
        assertThat(layout.isResponsiveContentInsetEnabled()).isFalse();
    }

    @Test
    public void testAllBehind() {
        LayoutElement content = new Box.Builder().build();
        CircularProgressIndicator progressIndicator =
                new CircularProgressIndicator.Builder().build();
        EdgeContentLayout layout =
                new EdgeContentLayout.Builder(DEVICE_PARAMETERS)
                        .setEdgeContentBehindAllOtherContent(true)
                        .setContent(content)
                        .setEdgeContent(progressIndicator)
                        .setPrimaryLabelTextContent(PRIMARY_LABEL)
                        .setSecondaryLabelTextContent(SECONDARY_LABEL)
                        .build();

        assertLayout(layout, progressIndicator, content, PRIMARY_LABEL, SECONDARY_LABEL);
        assertThat(layout.isEdgeContentBehindAllOtherContent()).isTrue();
        assertThat(layout.isResponsiveContentInsetEnabled()).isFalse();
    }

    @Test
    public void testContentOnly() {
        LayoutElement content = new Box.Builder().build();
        EdgeContentLayout layout =
                new EdgeContentLayout.Builder(DEVICE_PARAMETERS).setContent(content).build();

        assertLayout(layout, null, content, null, null);
        assertThat(layout.isEdgeContentBehindAllOtherContent()).isFalse();
        assertThat(layout.isResponsiveContentInsetEnabled()).isFalse();
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
        assertThat(layout.isEdgeContentBehindAllOtherContent()).isFalse();
        assertThat(layout.isResponsiveContentInsetEnabled()).isFalse();
    }

    @Test
    public void testEmpty() {
        EdgeContentLayout layout = new EdgeContentLayout.Builder(DEVICE_PARAMETERS).build();

        assertLayout(layout, null, null, null, null);
        assertThat(layout.isEdgeContentBehindAllOtherContent()).isFalse();
        assertThat(layout.isResponsiveContentInsetEnabled()).isFalse();
    }

    // Responsive test cases with behaviour with using setResponsiveContentInsetEnabled. They are
    // repeated because the organization of elements inside of the layout is different then without
    // this setter.

    @Test
    public void testAll_responsive() {
        LayoutElement content = new Box.Builder().build();
        CircularProgressIndicator progressIndicator =
                new CircularProgressIndicator.Builder().build();
        int edgeContentThickness = 20;
        EdgeContentLayout layout =
                new EdgeContentLayout.Builder(DEVICE_PARAMETERS)
                        .setResponsiveContentInsetEnabled(true)
                        .setContent(content)
                        .setEdgeContent(progressIndicator)
                        .setPrimaryLabelTextContent(PRIMARY_LABEL)
                        .setSecondaryLabelTextContent(SECONDARY_LABEL)
                        .setEdgeContentThickness(edgeContentThickness)
                        .build();

        assertLayout(
                layout,
                progressIndicator,
                content,
                PRIMARY_LABEL,
                SECONDARY_LABEL);

        assertThat(layout.getEdgeContentThickness()).isEqualTo(edgeContentThickness);
        assertThat(layout.isEdgeContentBehindAllOtherContent()).isTrue();
        assertThat(layout.isResponsiveContentInsetEnabled()).isTrue();
    }

    @Test
    public void testAll_defaultThickness_responsive() {
        LayoutElement content = new Box.Builder().build();
        CircularProgressIndicator progressIndicator =
                new CircularProgressIndicator.Builder().build();
        EdgeContentLayout layout =
                new EdgeContentLayout.Builder(DEVICE_PARAMETERS)
                        .setResponsiveContentInsetEnabled(true)
                        .setContent(content)
                        .setEdgeContent(progressIndicator)
                        .setPrimaryLabelTextContent(PRIMARY_LABEL)
                        .setSecondaryLabelTextContent(SECONDARY_LABEL)
                        .build();

        assertLayout(
                layout,
                progressIndicator,
                content,
                PRIMARY_LABEL,
                SECONDARY_LABEL);

        assertThat(layout.getEdgeContentThickness())
                .isEqualTo(progressIndicator.getStrokeWidth().getValue());
        assertThat(layout.isEdgeContentBehindAllOtherContent()).isTrue();
        assertThat(layout.isResponsiveContentInsetEnabled()).isTrue();
    }

    @Test
    public void testContentOnly_responsive() {
        LayoutElement content = new Box.Builder().build();
        EdgeContentLayout layout =
                new EdgeContentLayout.Builder(DEVICE_PARAMETERS)
                        .setResponsiveContentInsetEnabled(true)
                        .setContent(content)
                        .build();

        assertLayout(
                layout,
                /* expectedProgressIndicator= */ null,
                content,
                /* expectedPrimaryLabel= */ null,
                /* expectedSecondaryLabel= */ null);

        assertThat(layout.getEdgeContentThickness()).isEqualTo(0);
        assertThat(layout.isEdgeContentBehindAllOtherContent()).isTrue();
        assertThat(layout.isResponsiveContentInsetEnabled()).isTrue();
    }

    @Test
    public void testIndicatorOnly_responsive() {
        CircularProgressIndicator progressIndicator =
                new CircularProgressIndicator.Builder().build();
        EdgeContentLayout layout =
                new EdgeContentLayout.Builder(DEVICE_PARAMETERS)
                        .setResponsiveContentInsetEnabled(true)
                        .setEdgeContent(progressIndicator)
                        .build();

        assertLayout(
                layout,
                progressIndicator,
                /* expectedContent= */ null,
                /* expectedPrimaryLabel= */ null,
                /* expectedSecondaryLabel= */ null);

        assertThat(layout.getEdgeContentThickness())
                .isEqualTo(progressIndicator.getStrokeWidth().getValue());
        assertThat(layout.isEdgeContentBehindAllOtherContent()).isTrue();
        assertThat(layout.isResponsiveContentInsetEnabled()).isTrue();
    }

    @Test
    public void testEmpty_responsive() {
        EdgeContentLayout layout = new EdgeContentLayout.Builder(DEVICE_PARAMETERS)
                .setResponsiveContentInsetEnabled(true)
                .build();

        assertLayout(
                layout,
                /* expectedProgressIndicator= */ null,
                /* expectedContent= */ null,
                /* expectedPrimaryLabel= */ null,
                /* expectedSecondaryLabel= */ null);

        assertThat(layout.getEdgeContentThickness()).isEqualTo(0);
        assertThat(layout.isEdgeContentBehindAllOtherContent()).isTrue();
        assertThat(layout.isResponsiveContentInsetEnabled()).isTrue();
    }

    @Test
    public void testWrongElement() {
        Column box = new Column.Builder().build();

        assertThat(EdgeContentLayout.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongBox() {
        Box box = new Box.Builder().build();

        assertThat(EdgeContentLayout.fromLayoutElement(box)).isNull();
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

        assertThat(EdgeContentLayout.fromLayoutElement(box)).isNull();
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
                                                                EdgeContentLayout
                                                                        .METADATA_TAG_PREFIX
                                                                        .getBytes(UTF_8))
                                                        .build())
                                        .build())
                        .build();

        assertThat(EdgeContentLayout.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testResponsiveAndAboveContentSettersMixed() {
        EdgeContentLayout.Builder builder =
                new EdgeContentLayout.Builder(DEVICE_PARAMETERS)
                        .setResponsiveContentInsetEnabled(true);

        assertThrows(
                IllegalStateException.class,
                () -> builder.setEdgeContentBehindAllOtherContent(false));
    }

    @Test
    public void testResponsiveAndBehindContentSettersMixed() {
        // This shouldn't throw.
        new EdgeContentLayout.Builder(DEVICE_PARAMETERS)
                .setResponsiveContentInsetEnabled(true)
                // This is fine as it's set to true.
                .setEdgeContentBehindAllOtherContent(true)
                .build();
    }

    @Test
    public void testBehindContentAndResponsiveSettersMixed() {
        EdgeContentLayout.Builder builder =
                new EdgeContentLayout.Builder(DEVICE_PARAMETERS)
                        .setEdgeContentBehindAllOtherContent(false);

        assertThrows(
                IllegalStateException.class,
                () -> builder.setResponsiveContentInsetEnabled(true));
    }

    @Test
    public void testAboveContentAndResponsiveSettersMixed() {
        // This shouldn't throw.
        new EdgeContentLayout.Builder(DEVICE_PARAMETERS)
                .setEdgeContentBehindAllOtherContent(true)
                .setResponsiveContentInsetEnabled(true);
    }

    private void assertLayout(
            @NonNull EdgeContentLayout actualLayout,
            @Nullable LayoutElement expectedProgressIndicator,
            @Nullable LayoutElement expectedContent,
            @Nullable LayoutElement expectedPrimaryLabel,
            @Nullable LayoutElement expectedSecondaryLabel) {
        assertLayoutIsEqual(
                actualLayout,
                expectedProgressIndicator,
                expectedContent,
                expectedPrimaryLabel,
                expectedSecondaryLabel);

        Box box = new Box.Builder().addContent(actualLayout).build();

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
            @Nullable LayoutElement expectedProgressIndicator,
            @Nullable LayoutElement expectedContent,
            @Nullable LayoutElement expectedPrimaryLabel,
            @Nullable LayoutElement expectedSecondaryLabel) {
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

        // Reset bit for edge content position. If that bit is wrong, the above checks around
        // content will fail, so we don't need to specifically check it here.
        resetEdgeContentPositionAndResponsiveFlag(expectedMetadata);
        byte[] actualMetadata = actualLayout.getMetadataTag();
        resetEdgeContentPositionAndResponsiveFlag(actualMetadata);

        assertThat(actualMetadata).isEqualTo(expectedMetadata);
    }

    private static void resetEdgeContentPositionAndResponsiveFlag(byte[] expectedMetadata) {
        expectedMetadata[EdgeContentLayout.FLAG_INDEX] =
                (byte)
                        (expectedMetadata[EdgeContentLayout.FLAG_INDEX]
                                & ~EdgeContentLayout.EDGE_CONTENT_POSITION);
        expectedMetadata[EdgeContentLayout.FLAG_INDEX] =
                (byte)
                        (expectedMetadata[EdgeContentLayout.FLAG_INDEX]
                                & ~EdgeContentLayout.CONTENT_INSET_USED);
    }
}
