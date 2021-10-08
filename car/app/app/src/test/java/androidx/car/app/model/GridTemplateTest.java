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
    public void createInstance_noHeaderTitleOrAction_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new GridTemplate.Builder().setSingleList(
                        TestUtils.getGridItemList(2)).build());

        // Positive cases.
        new GridTemplate.Builder().setTitle("Title").setSingleList(
                TestUtils.getGridItemList(2)).build();
        new GridTemplate.Builder()
                .setHeaderAction(Action.BACK)
                .setSingleList(TestUtils.getGridItemList(2))
                .build();
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
                        .build();

        assertThat(template)
                .isEqualTo(
                        new GridTemplate.Builder()
                                .setSingleList(itemList)
                                .setHeaderAction(Action.BACK)
                                .setActionStrip(actionStrip)
                                .setTitle(title)
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
}
