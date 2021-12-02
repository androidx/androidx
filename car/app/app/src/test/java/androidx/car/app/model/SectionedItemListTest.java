/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app.model;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.car.app.TestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link SectionedItemList}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class SectionedItemListTest {

    @Test
    public void createInstance_header_unsupportedSpans_throws() {
        ItemList list = new ItemList.Builder().build();
        CharSequence title = TestUtils.getCharSequenceWithColorSpan("Title");
        assertThrows(
                IllegalArgumentException.class,
                () -> SectionedItemList.create(list, title));

        // DistanceSpan and DurationSpan are supported.
        CharSequence title2 = TestUtils.getCharSequenceWithDistanceAndDurationSpans("Title");
        SectionedItemList.create(list, title2);
    }

    @Test
    public void createInstance() {
        ItemList list = new ItemList.Builder().build();
        SectionedItemList sectionList = SectionedItemList.create(list, "header");

        assertThat(sectionList.getItemList()).isEqualTo(list);
        assertThat(sectionList.getHeader().toString()).isEqualTo("header");
    }

    @Test
    public void equals() {
        ItemList list = new ItemList.Builder().build();
        SectionedItemList sectionList = SectionedItemList.create(list, "header");

        ItemList list2 = new ItemList.Builder().build();
        SectionedItemList sectionList2 = SectionedItemList.create(list2, "header");

        assertThat(sectionList2).isEqualTo(sectionList);
    }

    @Test
    public void notEquals_differentItemList() {
        ItemList list = new ItemList.Builder().addItem(
                new Row.Builder().setTitle("Title").build()).build();
        SectionedItemList sectionList = SectionedItemList.create(list, "header");

        ItemList list2 = new ItemList.Builder().build();
        SectionedItemList sectionList2 = SectionedItemList.create(list2, "header");

        assertThat(sectionList2).isNotEqualTo(sectionList);
    }

    @Test
    public void notEquals_differentHeader() {
        ItemList list = new ItemList.Builder().build();
        SectionedItemList sectionList = SectionedItemList.create(list, "header1");

        ItemList list2 = new ItemList.Builder().build();
        SectionedItemList sectionList2 = SectionedItemList.create(list2, "header2");

        assertThat(sectionList2).isNotEqualTo(sectionList);
    }
}
