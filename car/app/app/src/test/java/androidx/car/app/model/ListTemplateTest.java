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

/** Tests for {@link ListTemplate}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ListTemplateTest {
    @Test
    public void createInstance_emptyList_notLoading_Throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new ListTemplate.Builder().setTitle("Title").build());

        // Positive case
        new ListTemplate.Builder().setTitle("Title").setLoading(true).build();
    }

    @Test
    public void createInstance_isLoading_hasList_Throws() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        new ListTemplate.Builder()
                                .setTitle("Title")
                                .setLoading(true)
                                .setSingleList(getList())
                                .build());
    }

    @Test
    public void addEmptyList_throws() {
        ItemList emptyList = new ItemList.Builder().build();
        assertThrows(
                IllegalArgumentException.class,
                () -> new ListTemplate.Builder().setTitle("Title").addSectionedList(
                        SectionedItemList.create(emptyList,
                                "header")).build());
    }

    @Test
    public void addSectionedList_emptyHeader_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ListTemplate.Builder().setTitle("Title").addSectionedList(
                        SectionedItemList.create(getList(),
                                "")).build());
    }

    @Test
    public void addSectionedList_withVisibilityListener_throws() {
        ItemList list =
                new ItemList.Builder()
                        .addItem(new Row.Builder().setTitle("Title").build())
                        .setOnItemsVisibilityChangedListener((start, end) -> {
                        })
                        .build();
        assertThrows(
                IllegalArgumentException.class,
                () -> new ListTemplate.Builder().setTitle("Title").addSectionedList(
                        SectionedItemList.create(list,
                                "header")).build());
    }

    @Test
    public void addSectionedList_moreThanMaxTexts_throws() {
        Row rowExceedsMaxTexts =
                new Row.Builder().setTitle("Title").addText("text1").addText("text2").addText(
                        "text3").build();
        Row rowMeetingMaxTexts =
                new Row.Builder().setTitle("Title").addText("text1").addText("text2").build();
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ListTemplate.Builder()
                                .setTitle("Title")
                                .setSingleList(
                                        new ItemList.Builder().addItem(rowExceedsMaxTexts).build())
                                .build());

        // Positive case.
        new ListTemplate.Builder()
                .setTitle("Title")
                .setSingleList(new ItemList.Builder().addItem(rowMeetingMaxTexts).build())
                .build();
    }

    @Test
    public void createInstance_noHeaderTitleOrAction_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new ListTemplate.Builder().setSingleList(getList()).build());

        // Positive cases/.
        new ListTemplate.Builder().setTitle("Title").setSingleList(getList()).build();
        new ListTemplate.Builder().setHeaderAction(Action.BACK).setSingleList(getList()).build();
    }

    @Test
    public void createInstance_header_unsupportedSpans_throws() {
        CharSequence title = TestUtils.getCharSequenceWithColorSpan("Title");
        assertThrows(
                IllegalArgumentException.class,
                () -> new ListTemplate.Builder().setTitle(title));

        // DurationSpan and DistanceSpan do not throw
        CharSequence title2 = TestUtils.getCharSequenceWithDistanceAndDurationSpans("Title");
        new ListTemplate.Builder().setTitle(title2).setSingleList(getList()).build();
    }

    @Test
    public void createInstance_setSingleList() {
        ItemList list = getList();
        ListTemplate template = new ListTemplate.Builder().setTitle("Title").setSingleList(
                list).build();
        assertThat(template.getSingleList()).isEqualTo(list);
        assertThat(template.getSectionedLists()).isEmpty();
    }

    @Test
    public void createInstance_addSectionedList() {
        ItemList list1 = getList();
        ItemList list2 = getList();
        ListTemplate template =
                new ListTemplate.Builder()
                        .setTitle("Title")
                        .addSectionedList(SectionedItemList.create(list1, "header1"))
                        .addSectionedList(SectionedItemList.create(list2, "header2"))
                        .build();
        assertThat(template.getSingleList()).isNull();
        assertThat(template.getSectionedLists()).hasSize(2);
        assertThat(template.getSectionedLists().get(0).getItemList()).isEqualTo(list1);
        assertThat(template.getSectionedLists().get(0).getHeader().toString()).isEqualTo("header1");
        assertThat(template.getSectionedLists().get(1).getItemList()).isEqualTo(list2);
        assertThat(template.getSectionedLists().get(1).getHeader().toString()).isEqualTo("header2");
    }

    @Test
    public void setSingleList_clearLists() {
        ItemList list1 = getList();
        ItemList list2 = getList();
        ItemList list3 = getList();
        ListTemplate template =
                new ListTemplate.Builder()
                        .setTitle("Title")
                        .addSectionedList(SectionedItemList.create(list1, "header1"))
                        .addSectionedList(SectionedItemList.create(list2, "header2"))
                        .setSingleList(list3)
                        .build();
        assertThat(template.getSingleList()).isEqualTo(list3);
        assertThat(template.getSectionedLists()).isEmpty();
    }

    @Test
    public void addSectionedList_clearSingleList() {
        ItemList list1 = getList();
        ItemList list2 = getList();
        ItemList list3 = getList();
        ListTemplate template =
                new ListTemplate.Builder()
                        .setTitle("Title")
                        .setSingleList(list1)
                        .addSectionedList(SectionedItemList.create(list2, "header1"))
                        .addSectionedList(SectionedItemList.create(list3, "header2"))
                        .build();
        assertThat(template.getSingleList()).isNull();
        assertThat(template.getSectionedLists()).hasSize(2);
    }

    @Test
    public void createInstance_setHeaderAction_invalidActionThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ListTemplate.Builder()
                                .setHeaderAction(
                                        new Action.Builder().setTitle("Action").setOnClickListener(
                                                () -> {
                                                }).build()));
    }

    @Test
    public void createInstance_setHeaderAction() {
        ListTemplate template =
                new ListTemplate.Builder().setSingleList(getList()).setHeaderAction(
                        Action.BACK).build();
        assertThat(template.getHeaderAction()).isEqualTo(Action.BACK);
    }

    @Test
    public void createInstance_setActionStrip() {
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        ListTemplate template =
                new ListTemplate.Builder()
                        .setTitle("Title")
                        .setSingleList(getList())
                        .setActionStrip(actionStrip)
                        .build();
        assertThat(template.getActionStrip()).isEqualTo(actionStrip);
    }

    @Test
    public void equals() {
        ItemList itemList = new ItemList.Builder().build();
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        String title = "title";

        ListTemplate template =
                new ListTemplate.Builder()
                        .setSingleList(itemList)
                        .setActionStrip(actionStrip)
                        .setHeaderAction(Action.BACK)
                        .setTitle(title)
                        .build();

        assertThat(template)
                .isEqualTo(
                        new ListTemplate.Builder()
                                .setSingleList(itemList)
                                .setActionStrip(actionStrip)
                                .setHeaderAction(Action.BACK)
                                .setTitle(title)
                                .build());
    }

    @Test
    public void notEquals_differentItemList() {
        ItemList itemList = new ItemList.Builder().build();

        ListTemplate template =
                new ListTemplate.Builder().setTitle("Title").setSingleList(itemList).build();

        assertThat(template)
                .isNotEqualTo(
                        new ListTemplate.Builder()
                                .setTitle("Title")
                                .setSingleList(
                                        new ItemList.Builder().addItem(
                                                new Row.Builder().setTitle(
                                                        "Title").build()).build())
                                .build());
    }

    @Test
    public void notEquals_differentHeaderAction() {
        ItemList itemList = new ItemList.Builder().build();

        ListTemplate template =
                new ListTemplate.Builder().setSingleList(itemList).setHeaderAction(
                        Action.BACK).build();

        assertThat(template)
                .isNotEqualTo(
                        new ListTemplate.Builder()
                                .setSingleList(itemList)
                                .setHeaderAction(Action.APP_ICON)
                                .build());
    }

    @Test
    public void notEquals_differentActionStrip() {
        ItemList itemList = new ItemList.Builder().build();
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();

        ListTemplate template =
                new ListTemplate.Builder()
                        .setTitle("Title")
                        .setSingleList(itemList)
                        .setActionStrip(actionStrip)
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        new ListTemplate.Builder()
                                .setTitle("Title")
                                .setSingleList(itemList)
                                .setActionStrip(
                                        new ActionStrip.Builder().addAction(
                                                Action.APP_ICON).build())
                                .build());
    }

    @Test
    public void notEquals_differentTitle() {
        ItemList itemList = new ItemList.Builder().build();
        String title = "title";

        ListTemplate template = new ListTemplate.Builder().setSingleList(itemList).setTitle(
                title).build();

        assertThat(template)
                .isNotEqualTo(new ListTemplate.Builder().setSingleList(itemList).setTitle(
                        "yo").build());
    }

    private static ItemList getList() {
        Row row1 = new Row.Builder().setTitle("Bananas").build();
        Row row2 = new Row.Builder().setTitle("Oranges").build();
        return new ItemList.Builder().addItem(row1).addItem(row2).build();
    }
}
