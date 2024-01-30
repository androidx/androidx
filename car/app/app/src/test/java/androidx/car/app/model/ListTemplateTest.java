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
import androidx.car.app.messaging.model.CarMessage;
import androidx.car.app.messaging.model.ConversationItem;
import androidx.car.app.messaging.model.TestConversationFactory;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.List;

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
    public void createInstance_emptyHeader() {
        ItemList list = getList();
        ListTemplate template = new ListTemplate.Builder().setSingleList(list).build();

        assertThat(template.getHeaderAction()).isNull();
        assertThat(template.getTitle()).isNull();
        assertThat(template.getActionStrip()).isNull();
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
    public void clearSectionedLists() {
        ItemList list1 = getList();
        ItemList list2 = getList();
        ItemList list3 = getList();

        ListTemplate template =
                new ListTemplate.Builder()
                        .setTitle("Title")
                        .addSectionedList(SectionedItemList.create(list1, "header1"))
                        .addSectionedList(SectionedItemList.create(list2, "header2"))
                        .clearSectionedLists()
                        // At least one list is required to build
                        .addSectionedList(SectionedItemList.create(list3, "header3"))
                        .build();

        assertThat(template.getSectionedLists()).hasSize(1);
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
    public void createInstance_addAction() {
        CarIcon icon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        Action customAction = TestUtils.createAction(icon, CarColor.BLUE);
        ListTemplate template =
                new ListTemplate.Builder()
                        .setSingleList(getList())
                        .addAction(customAction)
                        .build();
        assertThat(template.getActions()).containsExactly(customAction);
    }

    @Test
    public void createInstance_addComposeAction() {
        CarIcon icon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        Action composeAction = Action.COMPOSE_MESSAGE;
        ListTemplate template =
                new ListTemplate.Builder()
                        .setSingleList(getList())
                        .addAction(composeAction)
                        .build();
        assertThat(template.getActions()).containsExactly(composeAction);
    }

    @Test
    public void createInstance_addAction_appIconInvalid_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ListTemplate.Builder()
                        .setSingleList(getList()).addAction(Action.APP_ICON).build());
    }

    @Test
    public void createInstance_addAction_backInvalid_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ListTemplate.Builder()
                        .setSingleList(getList()).addAction(Action.BACK).build());
    }

    @Test
    public void createInstance_addAction_panInvalid_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ListTemplate.Builder()
                        .setSingleList(getList()).addAction(Action.PAN).build());
    }

    @Test
    public void createInstance_addAction_manyActions_throws() {
        CarIcon icon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        Action customAction = TestUtils.createAction(icon, CarColor.BLUE);

        assertThrows(
                IllegalArgumentException.class,
                () -> new ListTemplate.Builder()
                        .setSingleList(getList())
                        .addAction(customAction)
                        .addAction(customAction)
                        .addAction(customAction)
                        .build());
    }

    @Test
    public void createInstance_addAction_invalidActionNullBackgroundColor_throws() {
        CarIcon icon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        Action customAction = TestUtils.createAction(icon, null);

        assertThrows(
                IllegalArgumentException.class,
                () -> new ListTemplate.Builder()
                        .setSingleList(getList())
                        .addAction(customAction)
                        .build());
    }

    @Test
    public void createInstance_addAction_invalidActionDefaultBackgroundColor_throws() {
        CarIcon icon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        Action customAction = TestUtils.createAction(icon, CarColor.DEFAULT);

        assertThrows(
                IllegalArgumentException.class,
                () -> new ListTemplate.Builder()
                        .setSingleList(getList())
                        .addAction(customAction)
                        .build());
    }

    @Test
    public void createInstance_addAction_invalidActionNullIcon_throws() {
        Action customAction = TestUtils.createAction("title", null);

        assertThrows(
                IllegalArgumentException.class,
                () -> new ListTemplate.Builder()
                        .setSingleList(getList())
                        .addAction(customAction)
                        .build());
    }

    @Test
    public void equals() {
        assertThat(createFullyPopulatedListTemplate())
                .isEqualTo(createFullyPopulatedListTemplate());
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

    @Test
    public void notEquals_differentAction() {
        ItemList itemList = new ItemList.Builder().build();
        CarIcon icon1 = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        CarIcon icon2 = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_2");

        ListTemplate template =
                new ListTemplate.Builder().setSingleList(itemList).addAction(
                        TestUtils.createAction(icon1, CarColor.BLUE)).build();

        assertThat(template)
                .isNotEqualTo(new ListTemplate.Builder().setSingleList(itemList).addAction(
                        TestUtils.createAction(icon2, CarColor.RED)).build());
    }

    @Test
    public void toBuilder_createsEquivalentInstance() {
        ListTemplate listTemplate = createFullyPopulatedListTemplate();

        assertThat(listTemplate).isEqualTo(listTemplate.toBuilder().build());
    }

    @Test
    public void toBuilder_fieldsCanBeOverwritten() {
        ItemList itemList = new ItemList.Builder().build();
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        String title = "title";

        ListTemplate listTemplate = new ListTemplate.Builder()
                .setSingleList(itemList)
                .setActionStrip(actionStrip)
                .setHeaderAction(Action.BACK)
                .setTitle(title)
                .build();

        // Verify fields can be overwritten (no crash)
        listTemplate.toBuilder()
                .setSingleList(itemList)
                .setActionStrip(actionStrip)
                .setHeaderAction(Action.BACK)
                .setTitle(title)
                .build();
    }

    @Test
    public void build_addingMoreThanMaxAllowedItemsInSingleList_truncates() {
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        String title = "title";
        ItemList.Builder itemListBuilder = new ItemList.Builder();
        int moreThanMaxAllowedItems = ListTemplate.MAX_ALLOWED_ITEMS + 1;
        for (int i = 0; i < moreThanMaxAllowedItems; i++) {
            itemListBuilder.addItem(new Row.Builder().setTitle(Integer.toString(i)).build());
        }
        ListTemplate listTemplate = new ListTemplate.Builder()
                .setSingleList(itemListBuilder.build())
                .setActionStrip(actionStrip)
                .setHeaderAction(Action.BACK)
                .setTitle(title)
                .build();

        assertThat(listTemplate.getSingleList().getItems()).hasSize(ListTemplate.MAX_ALLOWED_ITEMS);
    }

    @Test
    public void build_addingMoreThanMaxAllowedItemsInSectionedList_truncates() {
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        String title = "title";

        ItemList.Builder firstListBuilder = new ItemList.Builder();
        addRowsToItemList(firstListBuilder, 10);
        SectionedItemList firstList =
                SectionedItemList.create(firstListBuilder.build(), "First List");

        ItemList.Builder secondListBuilder = new ItemList.Builder();
        addRowsToItemList(secondListBuilder, ListTemplate.MAX_ALLOWED_ITEMS);
        SectionedItemList secondList =
                SectionedItemList.create(secondListBuilder.build(), "Second list");

        ItemList.Builder thirdListBuilder = new ItemList.Builder();
        addRowsToItemList(thirdListBuilder, 10);
        SectionedItemList thirdList =
                SectionedItemList.create(thirdListBuilder.build(), "Third list");

        // Add 3 lists, where the first list fits, second list is truncated, and the last list is
        // dropped
        ListTemplate listTemplate = new ListTemplate.Builder()
                .addSectionedList(firstList)
                .addSectionedList(secondList)
                .addSectionedList(thirdList)
                .setActionStrip(actionStrip)
                .setHeaderAction(Action.BACK)
                .setTitle(title)
                .build();

        assertThat(listTemplate.getSectionedLists()).hasSize(2);
        assertThat(listTemplate.getSectionedLists().get(0).getItemList().getItems()).hasSize(10);
        assertThat(listTemplate.getSectionedLists().get(1).getItemList().getItems()).hasSize(
                ListTemplate.MAX_ALLOWED_ITEMS - 10);
    }

    @Test
    public void build_aLotOfConversationMessages_truncates() {
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        String title = "title";
        ItemList.Builder builder = new ItemList.Builder();

        // Create a conversation with more than "max" messages. This should count as
        // 11 items (conversation item + 10 messages)
        ConversationItem.Builder conversationBuilder =
                TestConversationFactory.createMinimalConversationItemBuilder();
        List<CarMessage> messages = new ArrayList<>();
        for (int i = 0; i < ListTemplate.MAX_MESSAGES_PER_CONVERSATION + 1; i++) {
            messages.add(TestConversationFactory.createMinimalMessage());
        }
        conversationBuilder.setMessages(messages);
        builder.addItem(conversationBuilder.build());

        // Fill the item list with other conversations with padding (8 messages + 1 conversation
        // = 9 items) x 9 = 81 items being added.
        messages = messages.subList(0, 8);
        for (int i = 0; i < 9; i++) {
            builder.addItem(TestConversationFactory.createMinimalConversationItemBuilder()
                    .setMessages(messages)
                    .build());
        }

        // Which means there should be 92 items total here, so if we add another conversation with
        // 10 messages, it should truncate it to 7 message to fill 8 spaces.
        messages.clear();
        for (int i = 0; i < 10; i++) {
            messages.add(TestConversationFactory.createMinimalMessage());
        }
        builder.addItem(TestConversationFactory.createMinimalConversationItemBuilder()
                .setMessages(messages)
                .build());

        // Just for good measure, add another conversation that will be dropped
        builder.addItem(TestConversationFactory.createFullyPopulatedConversationItem());

        // Build
        ListTemplate listTemplate = new ListTemplate.Builder()
                .setSingleList(builder.build())
                .setActionStrip(actionStrip)
                .setHeaderAction(Action.BACK)
                .setTitle(title)
                .build();

        // 11 conversations should have been saved with the last 12th being dropped
        assertThat(listTemplate.getSingleList().getItems()).hasSize(11);
        // Expect that the first item (which originally had 11 messages), should have its message
        // count truncated to the max limit
        assertThat(((ConversationItem) listTemplate.getSingleList().getItems().get(
                0)).getMessages())
                .hasSize(ListTemplate.MAX_MESSAGES_PER_CONVERSATION);
        // Expect that the last item (which originally had 10 messages), should have its message
        // count truncated to fill the remaining spaces
        assertThat(((ConversationItem) listTemplate.getSingleList().getItems().get(
                10)).getMessages())
                .hasSize(7);
    }

    @Test
    public void build_addingConversations_neverResultsInAnEmptyConversation() {
        // Add 10 conversations with 8 messages each to fill 99 items
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        String title = "title";
        ItemList.Builder builder = new ItemList.Builder();

        List<CarMessage> messages = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            messages.add(TestConversationFactory.createMinimalMessage());
        }
        for (int i = 0; i < 9; i++) {
            builder.addItem(TestConversationFactory.createMinimalConversationItemBuilder()
                    .setMessages(messages)
                    .build());
        }

        // Add an 11th conversation which at minimum needs 2 spaces (1 for the conversation, 1
        // for the message).
        builder.addItem(TestConversationFactory.createMinimalConversationItemBuilder().build());

        // And try to build
        ListTemplate listTemplate = new ListTemplate.Builder()
                .setSingleList(builder.build())
                .setActionStrip(actionStrip)
                .setHeaderAction(Action.BACK)
                .setTitle(title)
                .build();

        // Assert that the last conversation was not added despite the item count only being 99
        assertThat(listTemplate.getSingleList().getItems()).hasSize(10);
    }

    private static ListTemplate createFullyPopulatedListTemplate() {
        ItemList itemList = new ItemList.Builder().build();
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        String title = "title";
        CarIcon icon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");

        return new ListTemplate.Builder()
                .setSingleList(itemList)
                .setActionStrip(actionStrip)
                .setHeaderAction(Action.BACK)
                .setTitle(title)
                .addAction(TestUtils.createAction(icon, CarColor.BLUE))
                .build();
    }

    private static ItemList getList() {
        Row row1 = new Row.Builder().setTitle("Bananas").build();
        Row row2 = new Row.Builder().setTitle("Oranges").build();
        return new ItemList.Builder().addItem(row1).addItem(row2).build();
    }

    private static void addRowsToItemList(ItemList.Builder itemListBuilder, int rowsToAdd) {
        for (int i = 0; i < rowsToAdd; i++) {
            itemListBuilder.addItem(new Row.Builder().setTitle(Integer.toString(i)).build());
        }
    }
}
