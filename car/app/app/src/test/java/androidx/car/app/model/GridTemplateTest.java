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

import static androidx.car.app.model.CarIcon.BACK;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.car.app.TestUtils;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link GridTemplate}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class GridTemplateTest {
    @Test
    public void createInstance_emptyList_notLoading_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new GridTemplate.Builder().setTitle("Title").build());

        // Positive case
        new GridTemplate.Builder().setTitle("Title").setLoading(true).build();
    }

    @Test
    public void createInstance_isLoading_hasList_throws() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        new GridTemplate.Builder()
                                .setTitle("Title")
                                .setLoading(true)
                                .setSingleList(TestUtils.getGridItemList(2))
                                .build());
    }

    @Test
    public void createInstance_emptyHeader() {
        GridTemplate template = new GridTemplate.Builder().setSingleList(
                        TestUtils.getGridItemList(2)).build();

        assertThat(template.getTitle()).isNull();
        assertThat(template.getActionStrip()).isNull();
        assertThat(template.getHeaderAction()).isNull();
    }

    @Test
    public void createInstance_header_unsupportedSpans_throws() {
        CharSequence title = TestUtils.getCharSequenceWithColorSpan("Title");
        assertThrows(
                IllegalArgumentException.class,
                () -> new GridTemplate.Builder().setTitle(title));

        // DurationSpan and DistanceSpan do not throw
        CharSequence title2 = TestUtils.getCharSequenceWithDistanceAndDurationSpans("Title");
        new GridTemplate.Builder().setTitle(title2).setSingleList(
                TestUtils.getGridItemList(2)).build();
    }

    @Test
    public void createInstance_setSingleList() {
        ItemList list = TestUtils.getGridItemList(2);
        GridTemplate template = new GridTemplate.Builder().setTitle("Title").setSingleList(
                list).build();
        assertThat(template.getSingleList()).isEqualTo(list);
    }

    @Test
    public void createInstance_setHeaderAction_invalidActionThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new GridTemplate.Builder()
                                .setHeaderAction(
                                        new Action.Builder().setTitle("Action").setOnClickListener(
                                                () -> {
                                                }).build()));
    }

    @Test
    public void createInstance_setHeaderAction() {
        GridTemplate template =
                new GridTemplate.Builder()
                        .setSingleList(TestUtils.getGridItemList(2))
                        .setHeaderAction(Action.BACK)
                        .build();
        assertThat(template.getHeaderAction()).isEqualTo(Action.BACK);
    }

    @Test
    public void createInstance_setActionStrip() {
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        GridTemplate template =
                new GridTemplate.Builder()
                        .setSingleList(TestUtils.getGridItemList(2))
                        .setTitle("Title")
                        .setActionStrip(actionStrip)
                        .build();
        assertThat(template.getActionStrip()).isEqualTo(actionStrip);
    }

    @Test
    public void createInstance_addAction() {
        CarIcon icon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        Action customAction = TestUtils.createAction(icon, CarColor.BLUE);
        GridTemplate template =
                new GridTemplate.Builder()
                        .setSingleList(TestUtils.getGridItemList(2))
                        .setHeaderAction(Action.BACK)
                        .addAction(customAction)
                        .build();
        assertThat(template.getActions()).containsExactly(customAction);
    }

    @Test
    public void createInstance_addAction_appIconInvalid_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new GridTemplate.Builder()
                        .setSingleList(TestUtils.getGridItemList(2))
                        .addAction(Action.APP_ICON).build());
    }

    @Test
    public void createInstance_addAction_backInvalid_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new GridTemplate.Builder()
                        .setSingleList(TestUtils.getGridItemList(2))
                        .addAction(Action.BACK).build());
    }

    @Test
    public void createInstance_addAction_panInvalid_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new GridTemplate.Builder()
                        .setSingleList(TestUtils.getGridItemList(2))
                        .addAction(Action.PAN).build());
    }

    @Test
    public void createInstance_addAction_manyActions_throws() {
        CarIcon icon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        Action customAction = TestUtils.createAction(icon, CarColor.BLUE);

        assertThrows(
                IllegalArgumentException.class,
                () -> new GridTemplate.Builder()
                        .setSingleList(TestUtils.getGridItemList(2))
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
                () -> new GridTemplate.Builder()
                        .setSingleList(TestUtils.getGridItemList(2))
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
                () -> new GridTemplate.Builder()
                        .setSingleList(TestUtils.getGridItemList(2))
                        .addAction(customAction)
                        .build());
    }

    @Test
    public void createInstance_addAction_invalidActionNullIcon_throws() {
        Action customAction = TestUtils.createAction("title", null);

        assertThrows(
                IllegalArgumentException.class,
                () -> new GridTemplate.Builder()
                        .setSingleList(TestUtils.getGridItemList(2))
                        .addAction(customAction)
                        .build());
    }

    @Test
    public void createInstance_setItemSize() {
        ItemList list = TestUtils.getGridItemList(2);
        GridTemplate template = new GridTemplate.Builder()
                .setTitle("Title")
                .setSingleList(list)
                .setItemSize(GridTemplate.ITEM_SIZE_LARGE)
                .build();

        assertThat(template.getItemSize()).isEqualTo(GridTemplate.ITEM_SIZE_LARGE);
    }

    @Test
    public void createInstance_defaultItemSizeIsSmall() {
        GridTemplate template = new GridTemplate.Builder()
                .setTitle("Title")
                .setLoading(true)
                .build();

        assertThat(template.getItemSize()).isEqualTo(GridTemplate.ITEM_SIZE_SMALL);
    }

    @Test
    public void createInstance_defaultItemImageShape() {
        ItemList list = TestUtils.getGridItemList(2);
        GridTemplate template = new GridTemplate.Builder().setSingleList(list).build();
        assertThat(template.getItemImageShape()).isEqualTo(GridTemplate.ITEM_IMAGE_SHAPE_UNSET);
    }

    @Test
    public void createInstance_setItemImageShape() {
        ItemList list = TestUtils.getGridItemList(2);
        GridTemplate template =
                new GridTemplate.Builder()
                        .setSingleList(list)
                        .setItemImageShape(GridTemplate.ITEM_IMAGE_SHAPE_CIRCLE).build();
        assertThat(template.getItemImageShape()).isEqualTo(GridTemplate.ITEM_IMAGE_SHAPE_CIRCLE);
    }

    @Test
    public void equals() {
        ItemList itemList = new ItemList.Builder().build();
        String title = "title";
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();

        GridTemplate template =
                new GridTemplate.Builder()
                        .setSingleList(itemList)
                        .setHeaderAction(Action.BACK)
                        .setActionStrip(actionStrip)
                        .setTitle(title)
                        .setItemSize(GridTemplate.ITEM_SIZE_MEDIUM)
                        .build();

        assertThat(template)
                .isEqualTo(
                        new GridTemplate.Builder()
                                .setSingleList(itemList)
                                .setHeaderAction(Action.BACK)
                                .setActionStrip(actionStrip)
                                .setTitle(title)
                                .setItemSize(GridTemplate.ITEM_SIZE_MEDIUM)
                                .build());
    }

    @Test
    public void notEquals_differentItemList() {
        ItemList itemList = new ItemList.Builder().build();

        GridTemplate template =
                new GridTemplate.Builder().setTitle("Title 1").setSingleList(itemList).build();

        assertThat(template)
                .isNotEqualTo(
                        new GridTemplate.Builder()
                                .setTitle("Title")
                                .setSingleList(
                                        new ItemList.Builder().addItem(
                                                new GridItem.Builder().setTitle("Title 2").setImage(
                                                        BACK).build()).build())
                                .build());
    }

    @Test
    public void notEquals_differentHeaderAction() {
        ItemList itemList = new ItemList.Builder().build();

        GridTemplate template =
                new GridTemplate.Builder().setSingleList(itemList).setHeaderAction(
                        Action.BACK).build();

        assertThat(template)
                .isNotEqualTo(
                        new GridTemplate.Builder()
                                .setSingleList(itemList)
                                .setHeaderAction(Action.APP_ICON)
                                .build());
    }

    @Test
    public void notEquals_differentTitle() {
        ItemList itemList = new ItemList.Builder().build();
        String title = "title";

        GridTemplate template = new GridTemplate.Builder().setSingleList(itemList).setTitle(
                title).build();

        assertThat(template)
                .isNotEqualTo(new GridTemplate.Builder().setSingleList(itemList).setTitle(
                        "foo").build());
    }

    @Test
    public void notEquals_differentActionStrip() {
        ItemList itemList = new ItemList.Builder().build();
        String title = "title";

        GridTemplate template =
                new GridTemplate.Builder()
                        .setSingleList(itemList)
                        .setTitle(title)
                        .setActionStrip(new ActionStrip.Builder().addAction(Action.BACK).build())
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        new GridTemplate.Builder()
                                .setSingleList(itemList)
                                .setTitle(title)
                                .setActionStrip(
                                        new ActionStrip.Builder().addAction(
                                                Action.APP_ICON).build())
                                .build());
    }

    @Test
    public void notEquals_differentAction() {
        ItemList itemList = new ItemList.Builder().build();
        CarIcon icon1 = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        CarIcon icon2 = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_2");

        GridTemplate template =
                new GridTemplate.Builder()
                        .setSingleList(itemList)
                        .addAction(TestUtils.createAction(icon1, CarColor.BLUE))
                        .build();

        assertThat(template)
                .isNotEqualTo(new GridTemplate.Builder().setSingleList(itemList).addAction(
                        TestUtils.createAction(icon2, CarColor.RED)).build());
    }

    @Test
    public void notEquals_differentSize() {
        GridTemplate template1 =
                new GridTemplate.Builder()
                        .setLoading(true)
                        .setItemSize(GridTemplate.ITEM_SIZE_MEDIUM)
                        .build();
        GridTemplate template2 =
                new GridTemplate.Builder()
                        .setLoading(true)
                        .setItemSize(GridTemplate.ITEM_SIZE_SMALL)
                        .build();
        assertThat(template1).isNotEqualTo(template2);
    }

    @Test
    public void notEquals_differentItemImageShape() {
        ItemList itemList = new ItemList.Builder().build();

        GridTemplate template1 =
                new GridTemplate.Builder()
                        .setSingleList(itemList)
                        .setItemImageShape(GridTemplate.ITEM_IMAGE_SHAPE_CIRCLE)
                        .build();
        GridTemplate template2 =
                new GridTemplate.Builder()
                        .setSingleList(itemList)
                        .setItemImageShape(GridTemplate.ITEM_IMAGE_SHAPE_UNSET)
                        .build();
        assertThat(template1).isNotEqualTo(template2);
    }
}
