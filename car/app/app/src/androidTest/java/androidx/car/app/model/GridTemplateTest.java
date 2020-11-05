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

import static androidx.car.app.model.CarIcon.ALERT;
import static androidx.car.app.model.CarIcon.BACK;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.car.app.TestUtils;
import androidx.car.app.utils.Logger;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link GridTemplate}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class GridTemplateTest {
    private final Logger mLogger = message -> {
    };

    @Test
    public void createInstance_emptyList_notLoading_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> GridTemplate.builder().setTitle("Title").build());

        // Positive case
        GridTemplate.builder().setTitle("Title").setLoading(true).build();
    }

    @Test
    public void createInstance_isLoading_hasList_throws() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        GridTemplate.builder()
                                .setTitle("Title")
                                .setLoading(true)
                                .setSingleList(TestUtils.getGridItemList(2))
                                .build());
    }

    @Test
    public void createInstance_noHeaderTitleOrAction_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> GridTemplate.builder().setSingleList(TestUtils.getGridItemList(2)).build());

        // Positive cases.
        GridTemplate.builder().setTitle("Title").setSingleList(
                TestUtils.getGridItemList(2)).build();
        GridTemplate.builder()
                .setHeaderAction(Action.BACK)
                .setSingleList(TestUtils.getGridItemList(2))
                .build();
    }

    @Test
    public void createInstance_setSingleList() {
        ItemList list = TestUtils.getGridItemList(2);
        GridTemplate template = GridTemplate.builder().setTitle("Title").setSingleList(
                list).build();
        assertThat(template.getSingleList()).isEqualTo(list);
    }

    @Test
    public void createInstance_setHeaderAction_invalidActionThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        GridTemplate.builder()
                                .setHeaderAction(
                                        Action.builder().setTitle("Action").setOnClickListener(
                                                () -> {
                                                }).build()));
    }

    @Test
    public void createInstance_setHeaderAction() {
        GridTemplate template =
                GridTemplate.builder()
                        .setSingleList(TestUtils.getGridItemList(2))
                        .setHeaderAction(Action.BACK)
                        .build();
        assertThat(template.getHeaderAction()).isEqualTo(Action.BACK);
    }

    @Test
    public void createInstance_setActionStrip() {
        ActionStrip actionStrip = ActionStrip.builder().addAction(Action.BACK).build();
        GridTemplate template =
                GridTemplate.builder()
                        .setSingleList(TestUtils.getGridItemList(2))
                        .setTitle("Title")
                        .setActionStrip(actionStrip)
                        .build();
        assertThat(template.getActionStrip()).isEqualTo(actionStrip);
    }

    @Test
    public void createInstance_setBackground() {
        GridTemplate template =
                GridTemplate.builder()
                        .setTitle("Title")
                        .setLoading(true)
                        .setBackgroundImage(BACK)
                        .build();
        assertThat(template.getBackgroundImage()).isEqualTo(BACK);
    }

    @Test
    public void validate_fromLoadingState_isRefresh() {
        GridItem.Builder gridItem = GridItem.builder().setImage(BACK);
        ItemList list = ItemList.builder().addItem(gridItem.build()).build();
        GridTemplate template = GridTemplate.builder().setTitle("Title").setSingleList(
                list).build();

        // Going from loading state to new content is allowed.
        assertThat(
                template.isRefresh(
                        GridTemplate.builder().setTitle("Title").setLoading(true).build(),
                        mLogger))
                .isTrue();
    }

    @Test
    public void validate_mutableProperties_isRefresh() {
        GridItem.Builder gridItem = GridItem.builder().setImage(BACK);
        ItemList list = ItemList.builder().addItem(gridItem.build()).build();
        GridTemplate template = GridTemplate.builder().setTitle("Title").setSingleList(
                list).build();

        // Ensure a template is a refresh of itself.
        assertThat(template.isRefresh(template, mLogger)).isTrue();

        // Allowed mutable states.
        assertThat(
                template.isRefresh(
                        GridTemplate.builder()
                                .setTitle("Title")
                                .setSingleList(
                                        ItemList.builder()
                                                .addItem(gridItem.setOnClickListener(() -> {
                                                }).setImage(BACK).build())
                                                .build())
                                .setHeaderAction(Action.BACK)
                                .setActionStrip(
                                        ActionStrip.builder().addAction(Action.APP_ICON).build())
                                .build(),
                        mLogger))
                .isTrue();
    }

    @Test
    public void validate_titleUpdate_isNotRefresh() {
        ItemList list = ItemList.builder().build();
        GridTemplate template = GridTemplate.builder().setTitle("Title").setSingleList(
                list).build();

        // Title updates are disallowed.
        assertThat(
                template.isRefresh(
                        GridTemplate.builder().setSingleList(list).setTitle("Title2").build(),
                        mLogger))
                .isFalse();
    }

    @Test
    public void validate_gridItemImageAndTextUpdates_isNotRefresh() {
        GridItem.Builder gridItem =
                GridItem.builder().setImage(BACK).setTitle("Title1").setText("Text1");
        ItemList list = ItemList.builder().addItem(gridItem.build()).build();
        GridTemplate template = GridTemplate.builder().setTitle("Title").setSingleList(
                list).build();

        // Ensure a template is a refresh of itself.
        assertThat(template.isRefresh(template, mLogger)).isTrue();

        // Image updates are disallowed.
        assertThat(
                template.isRefresh(
                        GridTemplate.builder()
                                .setTitle("Title")
                                .setSingleList(
                                        ItemList.builder().addItem(
                                                gridItem.setImage(ALERT).build()).build())
                                .build(),
                        mLogger))
                .isFalse();

        // Text updates are disallowed
        assertThat(
                template.isRefresh(
                        GridTemplate.builder()
                                .setTitle("Title")
                                .setSingleList(
                                        ItemList.builder().addItem(
                                                gridItem.setTitle("Title2").build()).build())
                                .build(),
                        mLogger))
                .isFalse();
        assertThat(
                template.isRefresh(
                        GridTemplate.builder()
                                .setTitle("Title")
                                .setSingleList(
                                        ItemList.builder().addItem(
                                                gridItem.setText("Text2").build()).build())
                                .build(),
                        mLogger))
                .isFalse();
    }

    @Test
    public void validate_newGridItem_isNotRefresh() {
        GridItem.Builder gridItem = GridItem.builder().setImage(BACK);
        ItemList list = ItemList.builder().addItem(gridItem.build()).build();
        GridTemplate template = GridTemplate.builder().setTitle("Title").setSingleList(
                list).build();

        // Additional grid items are disallowed.
        assertThat(
                template.isRefresh(
                        GridTemplate.builder()
                                .setTitle("Title")
                                .setSingleList(
                                        ItemList.builder()
                                                .addItem(gridItem.build())
                                                .addItem(gridItem.build())
                                                .build())
                                .build(),
                        mLogger))
                .isFalse();
    }

    @Test
    public void validate_toLoadingState_isNotRefresh() {
        // Going from content to loading state is disallowed.
        assertThat(
                GridTemplate.builder()
                        .setTitle("Title")
                        .setLoading(true)
                        .build()
                        .isRefresh(
                                GridTemplate.builder()
                                        .setTitle("Title")
                                        .setSingleList(ItemList.builder().build())
                                        .build(),
                                mLogger))
                .isFalse();
    }

    @Test
    public void resetList_clearsSingleList() {
        GridTemplate.Builder builder =
                GridTemplate.builder()
                        .setSingleList(TestUtils.getGridItemList(2))
                        .setHeaderAction(Action.BACK);

        assertThrows(IllegalStateException.class, () -> builder.clearAllLists().build());
    }

    @Test
    public void equals() {
        ItemList itemList = ItemList.builder().build();
        String title = "title";
        ActionStrip actionStrip = ActionStrip.builder().addAction(Action.BACK).build();

        GridTemplate template =
                GridTemplate.builder()
                        .setSingleList(itemList)
                        .setHeaderAction(Action.BACK)
                        .setActionStrip(actionStrip)
                        .setTitle(title)
                        .build();

        assertThat(template)
                .isEqualTo(
                        GridTemplate.builder()
                                .setSingleList(itemList)
                                .setHeaderAction(Action.BACK)
                                .setActionStrip(actionStrip)
                                .setTitle(title)
                                .build());
    }

    @Test
    public void notEquals_differentItemList() {
        ItemList itemList = ItemList.builder().build();

        GridTemplate template =
                GridTemplate.builder().setTitle("Title").setSingleList(itemList).build();

        assertThat(template)
                .isNotEqualTo(
                        GridTemplate.builder()
                                .setTitle("Title")
                                .setSingleList(
                                        ItemList.builder().addItem(
                                                GridItem.builder().setImage(BACK).build()).build())
                                .build());
    }

    @Test
    public void notEquals_differentHeaderAction() {
        ItemList itemList = ItemList.builder().build();

        GridTemplate template =
                GridTemplate.builder().setSingleList(itemList).setHeaderAction(Action.BACK).build();

        assertThat(template)
                .isNotEqualTo(
                        GridTemplate.builder()
                                .setSingleList(itemList)
                                .setHeaderAction(Action.APP_ICON)
                                .build());
    }

    @Test
    public void notEquals_differentTitle() {
        ItemList itemList = ItemList.builder().build();
        String title = "title";

        GridTemplate template = GridTemplate.builder().setSingleList(itemList).setTitle(
                title).build();

        assertThat(template)
                .isNotEqualTo(GridTemplate.builder().setSingleList(itemList).setTitle(
                        "foo").build());
    }

    @Test
    public void notEquals_differentActionStrip() {
        ItemList itemList = ItemList.builder().build();
        String title = "title";

        GridTemplate template =
                GridTemplate.builder()
                        .setSingleList(itemList)
                        .setTitle(title)
                        .setActionStrip(ActionStrip.builder().addAction(Action.BACK).build())
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        GridTemplate.builder()
                                .setSingleList(itemList)
                                .setTitle(title)
                                .setActionStrip(
                                        ActionStrip.builder().addAction(Action.APP_ICON).build())
                                .build());
    }
}
