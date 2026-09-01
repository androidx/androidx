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
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link TabTemplate}. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
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
                .addTab(getTab("TAB_1", "ID_1"))
                .addTab(getTab("TAB_2", "ID_2"))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId("ID_1")
                .build();

        TabTemplate template2 = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", "ID_1"))
                .addTab(getTab("TAB_2", "ID_2"))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId("ID_2")
                .build();

        assertNotEquals(template1, template2);
    }

    @Test
    public void equals_bothIsLoading() {
        TabTemplate templateWithActiveTab =
                new TabTemplate.Builder(mMockTabCallback)
                        .setLoading(true)
                        .build();
        TabTemplate templateWithoutActiveTab =
                new TabTemplate.Builder(mMockTabCallback)
                        .setLoading(true)
                        .build();

        assertEquals(templateWithActiveTab, templateWithoutActiveTab);
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

    @Test
    public void setTabStyle_getTabStyle_returnsSetTabStyle() {
        TabStyle tabStyle = new TabStyle.Builder().setShape(Shape.CORNER_FULL).build();
        TabTemplate template = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                .addTab(getTab("TAB_2", "ID_2"))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                .setStyle(tabStyle)
                .build();

        assertEquals(tabStyle, template.getStyle());
    }

    @Test
    public void setTabStyle_null_clearsStyle() {
        TabStyle tabStyle = new TabStyle.Builder().setShape(Shape.CORNER_FULL).build();
        TabTemplate template = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                .addTab(getTab("TAB_2", "ID_2"))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                .setStyle(tabStyle)
                .build();

        TabTemplate copy = new TabTemplate.Builder(template).setStyle(null).build();
        assertEquals(null, copy.getStyle());
    }

    @Test
    public void setEndAction_validAction_buildsAndReturnsAction() {
        CarIcon carIcon = TestUtils.getTestCarIcon(
                ApplicationProvider.getApplicationContext(), "ic_test_1");
        Action action = TestUtils.createAction(null, carIcon);

        TabTemplate template = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                .addTab(getTab("TAB_2", "ID_2"))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                .setEndAction(action)
                .build();

        assertThat(template.getEndAction()).isEqualTo(action);
    }

    @Test
    public void setEndAction_overwritesPreviousAction() {
        CarIcon carIcon1 = TestUtils.getTestCarIcon(
                ApplicationProvider.getApplicationContext(), "ic_test_1");
        CarIcon carIcon2 = TestUtils.getTestCarIcon(
                ApplicationProvider.getApplicationContext(), "ic_test_2");
        Action action1 = TestUtils.createAction(null, carIcon1);
        Action action2 = TestUtils.createAction(null, carIcon2);

        TabTemplate template = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                .addTab(getTab("TAB_2", "ID_2"))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                .setEndAction(action1)
                .setEndAction(action2)
                .build();

        assertThat(template.getEndAction()).isEqualTo(action2);
    }

    @Test
    public void setEndAction_null_clearsAction() {
        CarIcon carIcon = TestUtils.getTestCarIcon(
                ApplicationProvider.getApplicationContext(), "ic_test_1");
        Action action = TestUtils.createAction(null, carIcon);

        TabTemplate template = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                .addTab(getTab("TAB_2", "ID_2"))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                .setEndAction(action)
                .build();

        TabTemplate copy = new TabTemplate.Builder(template)
                .setEndAction(null)
                .build();

        assertThat(copy.getEndAction()).isNull();
    }

    @Test
    public void setEndAction_nullOnBuilder_clearsAction() {
        CarIcon carIcon = TestUtils.getTestCarIcon(
                ApplicationProvider.getApplicationContext(), "ic_test_1");
        Action action = TestUtils.createAction(null, carIcon);

        TabTemplate template = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                .addTab(getTab("TAB_2", "ID_2"))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                .setEndAction(action)
                .setEndAction(null)
                .build();

        assertThat(template.getEndAction()).isNull();
    }


    @Test
    public void setEndAction_actionWithTitle_throws() {
        CarIcon carIcon = TestUtils.getTestCarIcon(
                ApplicationProvider.getApplicationContext(), "ic_test_1");
        Action actionWithTitle = TestUtils.createAction("Title", carIcon);

        assertThrows(
                IllegalArgumentException.class,
                () -> new TabTemplate.Builder(mMockTabCallback)
                        .setEndAction(actionWithTitle));
    }

    @Test
    public void setEndAction_actionWithoutIcon_throws() {
        Action actionWithoutIcon = new Action.Builder()
                .setTitle("Title")
                .setOnClickListener(() -> {})
                .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> new TabTemplate.Builder(mMockTabCallback)
                        .setEndAction(actionWithoutIcon));
    }

    @Test
    public void setEndAction_standardAction_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TabTemplate.Builder(mMockTabCallback)
                        .setEndAction(Action.APP_ICON));
        assertThrows(
                IllegalArgumentException.class,
                () -> new TabTemplate.Builder(mMockTabCallback)
                        .setEndAction(Action.BACK));
    }

    @Test
    public void copy_copiesEndAction() {
        CarIcon carIcon1 = TestUtils.getTestCarIcon(
                ApplicationProvider.getApplicationContext(), "ic_test_1");
        Action action = TestUtils.createAction(null, carIcon1);

        TabTemplate template1 = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                .addTab(getTab("TAB_2", "ID_2"))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                .setEndAction(action)
                .build();

        TabTemplate template2 = new TabTemplate.Builder(template1).build();

        assertThat(template2.getEndAction()).isEqualTo(action);
        assertEquals(template1, template2);
    }

    @Test
    public void equals_and_hashCode_withEndAction() {
        CarIcon carIcon1 = TestUtils.getTestCarIcon(
                ApplicationProvider.getApplicationContext(), "ic_test_1");
        CarIcon carIcon2 = TestUtils.getTestCarIcon(
                ApplicationProvider.getApplicationContext(), "ic_test_2");
        Action action1 = TestUtils.createAction(null, carIcon1);
        Action action2 = TestUtils.createAction(null, carIcon2);

        TabTemplate template1 = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                .addTab(getTab("TAB_2", "ID_2"))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                .setEndAction(action1)
                .build();

        TabTemplate template2 = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                .addTab(getTab("TAB_2", "ID_2"))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                .setEndAction(action1)
                .build();

        TabTemplate templateDifferentAction = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                .addTab(getTab("TAB_2", "ID_2"))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                .setEndAction(action2)
                .build();

        TabTemplate templateNoAction = new TabTemplate.Builder(mMockTabCallback)
                .setHeaderAction(Action.APP_ICON)
                .addTab(getTab("TAB_1", ACTIVE_TAB_CONTENT_ID))
                .addTab(getTab("TAB_2", "ID_2"))
                .setTabContents(TAB_CONTENTS)
                .setActiveTabContentId(ACTIVE_TAB_CONTENT_ID)
                .build();

        assertEquals(template1, template2);
        assertEquals(template1.hashCode(), template2.hashCode());
        assertNotEquals(template1, templateDifferentAction);
        assertNotEquals(template1, templateNoAction);
        assertNotEquals(template1.hashCode(), templateNoAction.hashCode());
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
