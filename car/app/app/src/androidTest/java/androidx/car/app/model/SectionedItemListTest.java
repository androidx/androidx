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

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link ItemListTest}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class SectionedItemListTest {

    @Test
    public void createInstance() {
        ItemList list = ItemList.builder().build();
        CarText header = CarText.create("header");
        SectionedItemList sectionList = SectionedItemList.create(list, header);

        assertThat(sectionList.getItemList()).isEqualTo(list);
        assertThat(sectionList.getHeader()).isEqualTo(header);
    }

    @Test
    public void equals() {
        ItemList list = ItemList.builder().build();
        CarText header = CarText.create("header");
        SectionedItemList sectionList = SectionedItemList.create(list, header);

        ItemList list2 = ItemList.builder().build();
        CarText header2 = CarText.create("header");
        SectionedItemList sectionList2 = SectionedItemList.create(list2, header2);

        assertThat(sectionList2).isEqualTo(sectionList);
    }

    @Test
    public void notEquals_differentItemList() {
        ItemList list = ItemList.builder().addItem(Row.builder().setTitle("Title").build()).build();
        CarText header = CarText.create("header");
        SectionedItemList sectionList = SectionedItemList.create(list, header);

        ItemList list2 = ItemList.builder().build();
        CarText header2 = CarText.create("header");
        SectionedItemList sectionList2 = SectionedItemList.create(list2, header2);

        assertThat(sectionList2).isNotEqualTo(sectionList);
    }

    @Test
    public void notEquals_differentHeader() {
        ItemList list = ItemList.builder().build();
        CarText header = CarText.create("header1");
        SectionedItemList sectionList = SectionedItemList.create(list, header);

        ItemList list2 = ItemList.builder().build();
        CarText header2 = CarText.create("header2");
        SectionedItemList sectionList2 = SectionedItemList.create(list2, header2);

        assertThat(sectionList2).isNotEqualTo(sectionList);
    }
}
