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

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.LayoutElementBuilders.Box;
import androidx.wear.protolayout.LayoutElementBuilders.Column;
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement;
import androidx.wear.protolayout.LayoutElementBuilders.Row;
import androidx.wear.protolayout.ModifiersBuilders.ElementMetadata;
import androidx.wear.protolayout.ModifiersBuilders.Modifiers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
public class MultiSlotLayoutTest {

    @Test
    public void test() {
        LayoutElement content1 = new Box.Builder().build();
        LayoutElement content2 = new Row.Builder().build();
        float spacerWidth = 10f;

        MultiSlotLayout layout =
                new MultiSlotLayout.Builder()
                        .addSlotContent(content1)
                        .addSlotContent(content2)
                        .setHorizontalSpacerWidth(spacerWidth)
                        .build();

        assertLayoutIsEqual(content1, content2, spacerWidth, layout);

        Box box = new Box.Builder().addContent(layout).build();

        MultiSlotLayout newLayout = MultiSlotLayout.fromLayoutElement(box.getContents().get(0));

        assertThat(newLayout).isNotNull();
        assertLayoutIsEqual(content1, content2, spacerWidth, newLayout);

        assertThat(MultiSlotLayout.fromLayoutElement(layout)).isEqualTo(layout);
    }

    @Test
    public void testWrongElement() {
        Column box = new Column.Builder().build();

        assertThat(MultiSlotLayout.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongBox() {
        Box box = new Box.Builder().build();

        assertThat(MultiSlotLayout.fromLayoutElement(box)).isNull();
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

        assertThat(MultiSlotLayout.fromLayoutElement(box)).isNull();
    }

    private void assertLayoutIsEqual(
            LayoutElement content1,
            LayoutElement content2,
            float spacerWidth,
            MultiSlotLayout layout) {
        assertThat(layout.getSlotContents()).hasSize(2);
        assertThat(layout.getMetadataTag()).isEqualTo(MultiSlotLayout.METADATA_TAG);
        assertThat(layout.getSlotContents().get(0).toLayoutElementProto())
                .isEqualTo(content1.toLayoutElementProto());
        assertThat(layout.getSlotContents().get(1).toLayoutElementProto())
                .isEqualTo(content2.toLayoutElementProto());
        assertThat(layout.getHorizontalSpacerWidth()).isEqualTo(spacerWidth);
    }
}
