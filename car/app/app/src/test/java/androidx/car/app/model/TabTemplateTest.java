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

package androidx.car.app.model;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import androidx.car.app.TestUtils;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link TabTemplate}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class TabTemplateTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();
    @Mock
    TabTemplate.TabCallback mMockTabCallback;

    private static final TabContents TAB_CONTENTS = new TabContents.Builder(
            new ListTemplate.Builder()
                    .setSingleList(new ItemList.Builder()
                            .addItem(new Row.Builder()
                                    .setTitle("Row").addText("text1").build())
                                    .build())
                            .build())
            .build();

    private static final String ACTIVE_TAB_CONTENT_ID = "ID_ACTIVE";

    @Test
    public void createInstance_emptyTemplate_notLoading_Throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new TabTemplate.Builder(mMockTabCallback).build());

        // Positive case
        new TabTemplate.Builder(mMockTabCallback).setLoading(true).build();
    }

    @Test
    public void createInstance_isLoading_hasTabsAndTabContent_Throws() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        new TabTemplate.Builder(mMockTabCallback)
                                .setLoading(true)
                                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                                .setTabContents(TAB_CONTENTS)
                                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                                .build());
    }

    @Test
    public void createInstance_onlyOneTab_Throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new TabTemplate.Builder(mMockTabCallback)
                                .setHeaderAction(Action.APP_ICON)
                                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                                .setTabContents(TAB_CONTENTS)
                                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                                .build());
    }

    @Test
    public void createInstance_activeTabContentIdNotSet_Throws() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        new TabTemplate.Builder(mMockTabCallback)
                                .setHeaderAction(Action.APP_ICON)
                                .addTab(getTab("TAB_1", "ID_1"))
                                .addTab(getTab("TAB_2", "ID_2"))
                                .setTabContents(TAB_CONTENTS)
                                .build());
    }

    @Test
    public void createInstance_moreThanOneActiveTab_Throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new TabTemplate.Builder(mMockTabCallback)
                                .setHeaderAction(Action.APP_ICON)
                                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                                .addTab(getTab("TAB_2", ACTIVE_TAB_CONTENT_ID))
                                .setTabContents(TAB_CONTENTS)
                                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                                .build());
    }

    @Test
    public void createInstance_noActiveTab_Throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new TabTemplate.Builder(mMockTabCallback)
                                .setHeaderAction(Action.APP_ICON)
                                .addTab(getTab("TAB_1", "ID_1"))
                                .addTab(getTab("TAB_2", "ID_2"))
                                .setTabContents(TAB_CONTENTS)
                                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                                .build());
    }

    @Test
    public void createInstance_moreThanFourTabs_Throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new TabTemplate.Builder(mMockTabCallback)
                                .setHeaderAction(Action.APP_ICON)
                                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                                .addTab(getTab("TAB_2", "ID_2"))
                                .addTab(getTab("TAB_3", "ID_3"))
                                .addTab(getTab("TAB_4", "ID_4"))
                                .addTab(getTab("TAB_5", "ID_5"))
                                .setTabContents(TAB_CONTENTS)
                                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                                .build());
    }

    @Test
    public void createInstance_multipleTabsWithSameContentId_Throws() {
        String duplicateId = "ID_DUPLICATE";
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new TabTemplate.Builder(mMockTabCallback)
                                .setHeaderAction(Action.APP_ICON)
                                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                                .addTab(getTab("TAB_2", duplicateId))
                                .addTab(getTab("TAB_3", duplicateId))
                                .addTab(getTab("TAB_4", "ID_4"))
                                .setTabContents(TAB_CONTENTS)
                                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                                .build());
    }

    @Test
    public void createInstance_invalidHeaderAction_Throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new TabTemplate.Builder(mMockTabCallback)
                                .setHeaderAction(Action.BACK)
                                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                                .addTab(getTab("TAB_2", "ID_2"))
                                .setTabContents(TAB_CONTENTS)
                                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                                .build());
    }

    @Test
    public void createInstance_noTabContents_Throws() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        new TabTemplate.Builder(mMockTabCallback)
                                .setHeaderAction(Action.APP_ICON)
                                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                                .addTab(getTab("TAB_2", "ID_2"))
                                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                                .build());
    }

    @Test
    public void equals() {
        TabTemplate template1 = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                .addTab(getTab("TAB_2", "ID_2"))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                .build();

        TabTemplate template2 = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                .addTab(getTab("TAB_2", "ID_2"))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                .build();

        assertEquals(template1, template2);
    }

    @Test
    public void notEquals_differentTabs() {
        TabTemplate template = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", "ID_1"))
                .addTab(getTab("TAB_2", ACTIVE_TAB_CONTENT_ID))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                .build();

        assertThat(template)
                .isNotEqualTo(
                        new TabTemplate.Builder(mMockTabCallback)
                                .setHeaderAction(Action.APP_ICON)
                                .addTab(getTab("TAB_2", ACTIVE_TAB_CONTENT_ID))
                                .addTab(getTab("TAB_3", "ID_3"))
                                .setTabContents(TAB_CONTENTS)
                                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                                .build());
    }

    @Test
    public void notEquals_differentNumberOfTabs() {
        TabTemplate template = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", "ID_1"))
                .addTab(getTab("TAB_2", ACTIVE_TAB_CONTENT_ID))
                .addTab(getTab("TAB_3", "ID_3"))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                .build();

        assertThat(template)
                .isNotEqualTo(
                        new TabTemplate.Builder(mMockTabCallback)
                                .setHeaderAction(Action.APP_ICON)
                                .addTab(getTab("TAB_2", ACTIVE_TAB_CONTENT_ID))
                                .addTab(getTab("TAB_3", "ID_3"))
                                .setTabContents(TAB_CONTENTS)
                                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                                .build());
    }

    @Test
    public void notEquals_differentActiveTab() {
        TabTemplate template1 = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                .addTab(getTab("TAB_2", "ID_2"))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                .build();

        TabTemplate template2 = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", "ID_1"))
                .addTab(getTab("TAB_2", ACTIVE_TAB_CONTENT_ID))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                .build();

        assertNotEquals(template1, template2);
    }

    @Test
    public void notEquals_differentTabContent() {
        ItemList itemList = new ItemList.Builder().build();

        ListTemplate listTemplate =
                new ListTemplate.Builder().setSingleList(itemList).build();

        TabTemplate template = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                .addTab(getTab("TAB_2", "ID_2"))
                .setTabContents(new TabContents.Builder(listTemplate).build())
                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                .build();

        assertThat(template)
                .isNotEqualTo(
                        new TabTemplate.Builder(mMockTabCallback)
                                .setHeaderAction(Action.APP_ICON)
                                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                                .addTab(getTab("TAB_2", "ID_2"))
                                .setTabContents(TAB_CONTENTS)
                                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                                .build());
    }

    @Test
    public void createInstance_twoTabs_valid() {
        TabTemplate template = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                .addTab(getTab("TAB_2", "ID_2"))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                .build();

        assertEquals(template.getTabs().size(), 2);
    }

    @Test
    public void createInstance_fourTabs_valid() {
        TabTemplate template = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                .addTab(getTab("TAB_2", "ID_2"))
                .addTab(getTab("TAB_3", "ID_3"))
                .addTab(getTab("TAB_4", "ID_4"))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                .build();

        assertEquals(template.getTabs().size(), 4);
    }

    @Test
    public void copy_createsEquivalentInstance() {
        TabTemplate template1 = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                .addTab(getTab("TAB_2", "ID_2"))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                .build();

        TabTemplate template2 = new TabTemplate.Builder(template1).build();

        assertEquals(template1, template2);
    }

    @Test
    public void copy_fieldsCanBeOverwritten() {
        TabTemplate template = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                .addTab(getTab("TAB_2", "ID_2"))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                .build();

        // Verify fields can be overwritten (no crash)
        new TabTemplate.Builder(template)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_3", "ID_3"))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId("ID_3")
                .build();
    }

    private static Tab getTab(String title, String contentId) {
        return new Tab.Builder()
                .setContentId(contentId)
                .setIcon(TestUtils.getTestCarIcon(
                        ApplicationProvider.getApplicationContext(),
                        "ic_test_1"))
                .setTitle(title)
                .build();
    }
}
