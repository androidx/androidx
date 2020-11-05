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

import android.text.SpannableString;

import androidx.car.app.test.R;
import androidx.car.app.utils.Logger;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link ListTemplate}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class ListTemplateTest {
    private final Logger mLogger = message -> {
    };

    @Test
    public void createInstance_emptyList_notLoading_Throws() {
        assertThrows(
                IllegalStateException.class,
                () -> ListTemplate.builder().setTitle("Title").build());

        // Positive case
        ListTemplate.builder().setTitle("Title").setLoading(true).build();
    }

    @Test
    public void createInstance_isLoading_hasList_Throws() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        ListTemplate.builder()
                                .setTitle("Title")
                                .setLoading(true)
                                .setSingleList(getList())
                                .build());
    }

    @Test
    public void addEmptyList_throws() {
        ItemList emptyList = ItemList.builder().build();
        assertThrows(
                IllegalArgumentException.class,
                () -> ListTemplate.builder().setTitle("Title").addList(emptyList,
                        "header").build());
    }

    @Test
    public void addList_emptyHeader_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ListTemplate.builder().setTitle("Title").addList(getList(), "").build());
    }

    @Test
    public void resetList_clearsSingleList() {
        ListTemplate.Builder builder =
                ListTemplate.builder().setTitle("Title").setSingleList(getList());
        assertThrows(IllegalStateException.class, () -> builder.clearAllLists().build());
    }

    @Test
    public void resetList_clearsMultipleLists() {
        ListTemplate.Builder builder =
                ListTemplate.builder()
                        .setTitle("Title")
                        .addList(getList(), "header1")
                        .addList(getList(), "header2");
        assertThrows(IllegalStateException.class, () -> builder.clearAllLists().build());
    }

    @Test
    public void addList_withVisibilityListener_throws() {
        ItemList list =
                ItemList.builder()
                        .addItem(Row.builder().setTitle("Title").build())
                        .setOnItemsVisibilityChangeListener((start, end) -> {
                        })
                        .build();
        assertThrows(
                IllegalArgumentException.class,
                () -> ListTemplate.builder().setTitle("Title").addList(list, "header").build());
    }

    @Test
    public void addList_moreThanMaxTexts_throws() {
        Row rowExceedsMaxTexts =
                Row.builder().setTitle("Title").addText("text1").addText("text2").addText(
                        "text3").build();
        Row rowMeetingMaxTexts =
                Row.builder().setTitle("Title").addText("text1").addText("text2").build();
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ListTemplate.builder()
                                .setTitle("Title")
                                .setSingleList(
                                        ItemList.builder().addItem(rowExceedsMaxTexts).build())
                                .build());

        // Positive case.
        ListTemplate.builder()
                .setTitle("Title")
                .setSingleList(ItemList.builder().addItem(rowMeetingMaxTexts).build())
                .build();
    }

    @Test
    public void createInstance_noHeaderTitleOrAction_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> ListTemplate.builder().setSingleList(getList()).build());

        // Positive cases/.
        ListTemplate.builder().setTitle("Title").setSingleList(getList()).build();
        ListTemplate.builder().setHeaderAction(Action.BACK).setSingleList(getList()).build();
    }

    @Test
    public void createInstance_setSingleList() {
        ItemList list = getList();
        ListTemplate template = ListTemplate.builder().setTitle("Title").setSingleList(
                list).build();
        assertThat(template.getSingleList()).isEqualTo(list);
        assertThat(template.getSectionLists()).isEmpty();
    }

    @Test
    public void createInstance_addList() {
        ItemList list1 = getList();
        ItemList list2 = getList();
        ListTemplate template =
                ListTemplate.builder()
                        .setTitle("Title")
                        .addList(list1, "header1")
                        .addList(list2, "header2")
                        .build();
        assertThat(template.getSingleList()).isNull();
        assertThat(template.getSectionLists()).hasSize(2);
        assertThat(template.getSectionLists().get(0).getItemList()).isEqualTo(list1);
        assertThat(template.getSectionLists().get(0).getHeader().getText()).isEqualTo("header1");
        assertThat(template.getSectionLists().get(1).getItemList()).isEqualTo(list2);
        assertThat(template.getSectionLists().get(1).getHeader().getText()).isEqualTo("header2");
    }

    @Test
    public void setSingleList_clearLists() {
        ItemList list1 = getList();
        ItemList list2 = getList();
        ItemList list3 = getList();
        ListTemplate template =
                ListTemplate.builder()
                        .setTitle("Title")
                        .addList(list1, "header1")
                        .addList(list2, "header2")
                        .setSingleList(list3)
                        .build();
        assertThat(template.getSingleList()).isEqualTo(list3);
        assertThat(template.getSectionLists()).isEmpty();
    }

    @Test
    public void addList_clearSingleList() {
        ItemList list1 = getList();
        ItemList list2 = getList();
        ItemList list3 = getList();
        ListTemplate template =
                ListTemplate.builder()
                        .setTitle("Title")
                        .setSingleList(list1)
                        .addList(list2, "header1")
                        .addList(list3, "header2")
                        .build();
        assertThat(template.getSingleList()).isNull();
        assertThat(template.getSectionLists()).hasSize(2);
    }

    @Test
    public void createInstance_setHeaderAction_invalidActionThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ListTemplate.builder()
                                .setHeaderAction(
                                        Action.builder().setTitle("Action").setOnClickListener(
                                                () -> {
                                                }).build()));
    }

    @Test
    public void createInstance_setHeaderAction() {
        ListTemplate template =
                ListTemplate.builder().setSingleList(getList()).setHeaderAction(
                        Action.BACK).build();
        assertThat(template.getHeaderAction()).isEqualTo(Action.BACK);
    }

    @Test
    public void createInstance_setActionStrip() {
        ActionStrip actionStrip = ActionStrip.builder().addAction(Action.BACK).build();
        ListTemplate template =
                ListTemplate.builder()
                        .setTitle("Title")
                        .setSingleList(getList())
                        .setActionStrip(actionStrip)
                        .build();
        assertThat(template.getActionStrip()).isEqualTo(actionStrip);
    }

    @Test
    public void validate_fromLoadingState_isRefresh() {
        Row.Builder row = Row.builder().setTitle("Row1");
        ItemList list = ItemList.builder().addItem(row.build()).build();
        ListTemplate template = ListTemplate.builder().setTitle("Title").setSingleList(
                list).build();

        // Going from loading state to new content is allowed.
        assertThat(
                template.isRefresh(
                        ListTemplate.builder().setTitle("Title").setLoading(true).build(),
                        mLogger))
                .isTrue();
    }

    @Test
    public void validate_mutableProperties_isRefresh() {
        Row.Builder row = Row.builder().setTitle("Row1");
        ItemList list = ItemList.builder().addItem(row.build()).build();
        ListTemplate template = ListTemplate.builder().setTitle("Title").setSingleList(
                list).build();

        // Ensure a template is a refresh of itself.
        assertThat(template.isRefresh(template, mLogger)).isTrue();

        // Allowed mutable states.
        SpannableString stringWithSpan = new SpannableString("Row1");
        stringWithSpan.setSpan(DurationSpan.create(1), 0, /* end= */ 1, /* flags= */ 0);
        IconCompat icon = IconCompat.createWithResource(
                ApplicationProvider.getApplicationContext(), R.drawable.ic_test_1);
        assertThat(
                template.isRefresh(
                        ListTemplate.builder()
                                .setTitle("Title")
                                .setSingleList(
                                        ItemList.builder()
                                                .addItem(
                                                        row.setOnClickListener(() -> {
                                                        })
                                                                .setBrowsable(true)
                                                                .setTitle(stringWithSpan)
                                                                .setImage(CarIcon.of(icon))
                                                                .build())
                                                .build())
                                .setHeaderAction(Action.BACK)
                                .setActionStrip(
                                        ActionStrip.builder().addAction(Action.APP_ICON).build())
                                .build(),
                        mLogger))
                .isTrue();
    }

    @Test
    public void validate_multipleListMutableProperties_isRefresh() {
        Row row = Row.builder().setTitle("Row1").build();
        Row refreshRow =
                Row.builder()
                        .setTitle("Row1")
                        .setOnClickListener(() -> {
                        })
                        .setBrowsable(true)
                        .setImage(
                                CarIcon.of(
                                        IconCompat.createWithResource(
                                                ApplicationProvider.getApplicationContext(),
                                                R.drawable.ic_test_1)))
                        .build();
        ItemList list = ItemList.builder().addItem(row).build();
        ListTemplate template =
                ListTemplate.builder()
                        .setTitle("Title")
                        .addList(list, "header1")
                        .addList(list, "header2")
                        .build();

        // Sublist refreshes are allowed as long as headers and  number of sections remain the same.
        assertThat(
                template.isRefresh(
                        ListTemplate.builder()
                                .setTitle("Title")
                                .addList(ItemList.builder().addItem(refreshRow).build(), "header1")
                                .addList(ItemList.builder().addItem(refreshRow).build(), "header2")
                                .build(),
                        mLogger))
                .isTrue();
    }

    @Test
    public void validate_titleUpdate_isNotRefresh() {
        ItemList list = ItemList.builder().build();
        ListTemplate template = ListTemplate.builder().setTitle("Title").setSingleList(
                list).build();

        // Title updates are disallowed.
        assertThat(
                template.isRefresh(
                        ListTemplate.builder().setSingleList(list).setTitle("Title2").build(),
                        mLogger))
                .isFalse();
    }

    @Test
    public void validate_rowTextUpdate_isNotRefresh() {
        Row.Builder row = Row.builder().setTitle("Row1");
        ItemList list = ItemList.builder().addItem(row.build()).build();
        ListTemplate template = ListTemplate.builder().setTitle("Title").setSingleList(
                list).build();

        // Text updates are disallowed.
        assertThat(
                template.isRefresh(
                        ListTemplate.builder()
                                .setTitle("Title")
                                .setSingleList(ItemList.builder().addItem(
                                        row.setTitle("Row2").build()).build())
                                .build(),
                        mLogger))
                .isFalse();
        assertThat(
                template.isRefresh(
                        ListTemplate.builder()
                                .setTitle("Title")
                                .setSingleList(ItemList.builder().addItem(
                                        row.addText("Text").build()).build())
                                .build(),
                        mLogger))
                .isFalse();
    }

    @Test
    public void validate_newRow_isNotRefresh() {
        Row.Builder row = Row.builder().setTitle("Row1");
        ItemList list = ItemList.builder().addItem(row.build()).build();
        ListTemplate template = ListTemplate.builder().setTitle("Title").setSingleList(
                list).build();

        // Additional rows are disallowed.
        assertThat(
                template.isRefresh(
                        ListTemplate.builder()
                                .setTitle("Title")
                                .setSingleList(
                                        ItemList.builder().addItem(row.build()).addItem(
                                                row.build()).build())
                                .build(),
                        mLogger))
                .isFalse();
    }

    @Test
    public void validate_multipleList_headerChange_isNotRefresh() {
        Row.Builder row = Row.builder().setTitle("Row1");
        ItemList list = ItemList.builder().addItem(row.build()).build();
        ListTemplate template =
                ListTemplate.builder().setTitle("Title").addList(list, "header1").build();

        // Addition of lists are disallowed.
        assertThat(
                template.isRefresh(
                        ListTemplate.builder().setTitle("Title").addList(list, "header2").build(),
                        mLogger))
                .isFalse();
    }

    @Test
    public void validate_newList_isNotRefresh() {
        Row.Builder row = Row.builder().setTitle("Row1");
        ItemList list = ItemList.builder().addItem(row.build()).build();
        ListTemplate template = ListTemplate.builder().setTitle("Title").setSingleList(
                list).build();

        // Addition of lists are disallowed.
        assertThat(
                template.isRefresh(
                        ListTemplate.builder()
                                .setTitle("Title")
                                .addList(list, "header1")
                                .addList(list, "header2")
                                .build(),
                        mLogger))
                .isFalse();
    }

    @Test
    public void validate_toLoadingState_isNotRefresh() {
        // Going from content to loading state is disallowed.
        assertThat(
                ListTemplate.builder()
                        .setTitle("Title")
                        .setLoading(true)
                        .build()
                        .isRefresh(
                                ListTemplate.builder()
                                        .setTitle("Title")
                                        .setSingleList(ItemList.builder().build())
                                        .build(),
                                mLogger))
                .isFalse();
    }

    @Test
    public void equals() {
        ItemList itemList = ItemList.builder().build();
        ActionStrip actionStrip = ActionStrip.builder().addAction(Action.BACK).build();
        String title = "title";

        ListTemplate template =
                ListTemplate.builder()
                        .setSingleList(itemList)
                        .setActionStrip(actionStrip)
                        .setHeaderAction(Action.BACK)
                        .setTitle(title)
                        .build();

        assertThat(template)
                .isEqualTo(
                        ListTemplate.builder()
                                .setSingleList(itemList)
                                .setActionStrip(actionStrip)
                                .setHeaderAction(Action.BACK)
                                .setTitle(title)
                                .build());
    }

    @Test
    public void notEquals_differentItemList() {
        ItemList itemList = ItemList.builder().build();

        ListTemplate template =
                ListTemplate.builder().setTitle("Title").setSingleList(itemList).build();

        assertThat(template)
                .isNotEqualTo(
                        ListTemplate.builder()
                                .setTitle("Title")
                                .setSingleList(
                                        ItemList.builder().addItem(
                                                Row.builder().setTitle("Title").build()).build())
                                .build());
    }

    @Test
    public void notEquals_differentHeaderAction() {
        ItemList itemList = ItemList.builder().build();

        ListTemplate template =
                ListTemplate.builder().setSingleList(itemList).setHeaderAction(Action.BACK).build();

        assertThat(template)
                .isNotEqualTo(
                        ListTemplate.builder()
                                .setSingleList(itemList)
                                .setHeaderAction(Action.APP_ICON)
                                .build());
    }

    @Test
    public void notEquals_differentActionStrip() {
        ItemList itemList = ItemList.builder().build();
        ActionStrip actionStrip = ActionStrip.builder().addAction(Action.BACK).build();

        ListTemplate template =
                ListTemplate.builder()
                        .setTitle("Title")
                        .setSingleList(itemList)
                        .setActionStrip(actionStrip)
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        ListTemplate.builder()
                                .setTitle("Title")
                                .setSingleList(itemList)
                                .setActionStrip(
                                        ActionStrip.builder().addAction(Action.APP_ICON).build())
                                .build());
    }

    @Test
    public void notEquals_differentTitle() {
        ItemList itemList = ItemList.builder().build();
        String title = "title";

        ListTemplate template = ListTemplate.builder().setSingleList(itemList).setTitle(
                title).build();

        assertThat(template)
                .isNotEqualTo(ListTemplate.builder().setSingleList(itemList).setTitle(
                        "yo").build());
    }

    private static ItemList getList() {
        Row row1 = Row.builder().setTitle("Bananas").build();
        Row row2 = Row.builder().setTitle("Oranges").build();
        return ItemList.builder().addItem(row1).addItem(row2).build();
    }
}
