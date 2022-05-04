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

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.tiles.LayoutElementBuilders.Box;
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement;
import androidx.wear.tiles.LayoutElementBuilders.Row;

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

        assertThat(layout.getSlotContents()).hasSize(2);
        assertThat(layout.getSlotContents().get(0).toLayoutElementProto())
                .isEqualTo(content1.toLayoutElementProto());
        assertThat(layout.getSlotContents().get(1).toLayoutElementProto())
                .isEqualTo(content2.toLayoutElementProto());
        assertThat(layout.getHorizontalSpacerWidth()).isEqualTo(spacerWidth);
    }
}
