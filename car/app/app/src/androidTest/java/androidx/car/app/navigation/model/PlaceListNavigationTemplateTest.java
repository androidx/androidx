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

package androidx.car.app.navigation.model;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.Context;
import android.text.SpannableString;

import androidx.car.app.CarAppPermission;
import androidx.car.app.TestUtils;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Distance;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.LatLng;
import androidx.car.app.model.Metadata;
import androidx.car.app.model.Place;
import androidx.car.app.model.PlaceMarker;
import androidx.car.app.model.Row;
import androidx.car.app.model.Toggle;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link PlaceListNavigationTemplate}. */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class PlaceListNavigationTemplateTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final DistanceSpan mDistanceSpan =
            DistanceSpan.create(
                    Distance.create(/* displayDistance= */ 1, Distance.UNIT_KILOMETERS_P1));

    @Test
    public void createInstance_emptyList_notLoading_Throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PlaceListNavigationTemplate.builder().setTitle("Title").build());

        // Positive case
        PlaceListNavigationTemplate.builder().setTitle("Title").setIsLoading(true).build();
    }

    @Test
    public void createInstance_isLoading_hasList_Throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        PlaceListNavigationTemplate.builder()
                                .setTitle("Title")
                                .setIsLoading(true)
                                .setItemList(ItemList.builder().build())
                                .build());
    }

    @Test
    public void addList_selectable_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        PlaceListNavigationTemplate.builder()
                                .setTitle("Title")
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, true,
                                        mDistanceSpan))
                                .build());

        // Positive cases.
        PlaceListNavigationTemplate.builder()
                .setTitle("Title")
                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                .build();
    }

    @Test
    public void addList_moreThanMaxTexts_throws() {
        SpannableString title = new SpannableString("Title");
        title.setSpan(mDistanceSpan, /* start= */ 0, /* end= */ 1, /* flags= */ 0);
        Row rowExceedsMaxTexts =
                Row.builder().setTitle(title).addText("text1").addText("text2").addText(
                        "text3").build();
        Row rowMeetingMaxTexts =
                Row.builder().setTitle(title).addText("text1").addText("text2").build();
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        PlaceListNavigationTemplate.builder()
                                .setTitle("Title")
                                .setItemList(ItemList.builder().addItem(rowExceedsMaxTexts).build())
                                .build());

        // Positive cases.
        PlaceListNavigationTemplate.builder()
                .setTitle("Title")
                .setItemList(ItemList.builder().addItem(rowMeetingMaxTexts).build())
                .build();
    }

    @Test
    public void addList_hasToggle_throws() {
        SpannableString title = new SpannableString("Title");
        title.setSpan(mDistanceSpan, /* start= */ 0, /* end= */ 1, /* flags= */ 0);
        Row rowWithToggle =
                Row.builder().setTitle(title).setToggle(Toggle.builder(isChecked -> {
                }).build()).build();
        Row rowMeetingRestrictions =
                Row.builder().setTitle(title).addText("text1").addText("text2").build();
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        PlaceListNavigationTemplate.builder()
                                .setTitle("Title")
                                .setItemList(ItemList.builder().addItem(rowWithToggle).build())
                                .build());

        // Positive cases.
        PlaceListNavigationTemplate.builder()
                .setTitle("Title")
                .setItemList(ItemList.builder().addItem(rowMeetingRestrictions).build())
                .build();
    }

    @Test
    public void createInstance_noHeaderTitleOrAction_throws() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        PlaceListNavigationTemplate.builder().setItemList(
                                ItemList.builder().build()).build());

        // Positive cases.
        PlaceListNavigationTemplate.builder()
                .setTitle("Title")
                .setItemList(ItemList.builder().build())
                .build();
        PlaceListNavigationTemplate.builder()
                .setHeaderAction(Action.BACK)
                .setItemList(ItemList.builder().build())
                .build();
    }

    @Test
    public void createEmpty() {
        PlaceListNavigationTemplate template =
                PlaceListNavigationTemplate.builder()
                        .setTitle("Title")
                        .setItemList(ItemList.builder().build())
                        .build();
        assertThat(template.getItemList().getItems()).isEmpty();
        assertThat(template.getTitle().getText()).isEqualTo("Title");
        assertThat(template.getActionStrip()).isNull();
    }

    @Test
    public void createInstance() {
        String title = "title";
        ItemList itemList = TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan);
        ActionStrip actionStrip = ActionStrip.builder().addAction(Action.BACK).build();
        PlaceListNavigationTemplate template =
                PlaceListNavigationTemplate.builder()
                        .setItemList(itemList)
                        .setTitle(title)
                        .setActionStrip(actionStrip)
                        .build();
        assertThat(template.getItemList()).isEqualTo(itemList);
        assertThat(template.getActionStrip()).isEqualTo(actionStrip);
        assertThat(template.getTitle().getText()).isEqualTo(title);
    }

    @Test
    public void createInstance_setHeaderAction_invalidActionThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        PlaceListNavigationTemplate.builder()
                                .setHeaderAction(
                                        Action.builder().setTitle("Action").setOnClickListener(
                                                () -> {
                                                }).build()));
    }

    @Test
    public void createInstance_setHeaderAction() {
        PlaceListNavigationTemplate template =
                PlaceListNavigationTemplate.builder()
                        .setItemList(ItemList.builder().build())
                        .setHeaderAction(Action.BACK)
                        .build();

        assertThat(template.getHeaderAction()).isEqualTo(Action.BACK);
    }

    @Test
    public void createInstance_notAllRowHaveDistances() {
        SpannableString title = new SpannableString("Title");
        title.setSpan(mDistanceSpan, /* start= */ 0, /* end= */ 1, /* flags= */ 0);
        Row rowWithDistance = Row.builder().setTitle(title).build();
        Row rowWithoutDistance = Row.builder().setTitle("Google Kir").build();
        Row browsableRowWithoutDistance =
                Row.builder()
                        .setTitle("Google Kir")
                        .setBrowsable(true)
                        .setOnClickListener(() -> {
                        })
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        PlaceListNavigationTemplate.builder()
                                .setTitle("Title")
                                .setItemList(
                                        ItemList.builder().addItem(rowWithDistance).addItem(
                                                rowWithoutDistance).build())
                                .build());

        // Positive cases
        PlaceListNavigationTemplate.builder()
                .setTitle("Title")
                .setItemList(ItemList.builder().addItem(rowWithDistance).build())
                .build();
        PlaceListNavigationTemplate.builder()
                .setTitle("Title")
                .setItemList(
                        ItemList.builder()
                                .addItem(rowWithDistance)
                                .addItem(browsableRowWithoutDistance)
                                .build())
                .build();
        PlaceListNavigationTemplate.builder()
                .setTitle("Title")
                .setItemList(ItemList.builder().addItem(browsableRowWithoutDistance).build())
                .build();
    }

    @Test
    public void createInstance_rowHasBothMarkerAndImages() {
        SpannableString title = new SpannableString("Title");
        title.setSpan(mDistanceSpan, /* start= */ 0, /* end= */ 1, /* flags= */ 0);
        Row row =
                Row.builder()
                        .setTitle("Google Kir")
                        .setOnClickListener(() -> {
                        })
                        .setImage(CarIcon.ALERT)
                        .setMetadata(
                                Metadata.ofPlace(
                                        Place.builder(LatLng.create(10.f, 10.f))
                                                .setMarker(PlaceMarker.getDefault())
                                                .build()))
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        PlaceListNavigationTemplate.builder()
                                .setTitle("Title")
                                .setItemList(ItemList.builder().addItem(row).build()));
    }

    @Test
    public void equals() {
        PlaceListNavigationTemplate template =
                PlaceListNavigationTemplate.builder()
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setHeaderAction(Action.BACK)
                        .setActionStrip(ActionStrip.builder().addAction(Action.BACK).build())
                        .setTitle("title")
                        .build();

        assertThat(template)
                .isEqualTo(
                        PlaceListNavigationTemplate.builder()
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false,
                                        mDistanceSpan))
                                .setHeaderAction(Action.BACK)
                                .setActionStrip(
                                        ActionStrip.builder().addAction(Action.BACK).build())
                                .setTitle("title")
                                .build());
    }

    @Test
    public void notEquals_differentItemList() {
        PlaceListNavigationTemplate template =
                PlaceListNavigationTemplate.builder()
                        .setTitle("Title")
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        PlaceListNavigationTemplate.builder()
                                .setTitle("Title")
                                .setItemList(TestUtils.createItemListWithDistanceSpan(5, false,
                                        mDistanceSpan))
                                .build());
    }

    @Test
    public void notEquals_differentHeaderAction() {
        PlaceListNavigationTemplate template =
                PlaceListNavigationTemplate.builder()
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setHeaderAction(Action.BACK)
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        PlaceListNavigationTemplate.builder()
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false,
                                        mDistanceSpan))
                                .setHeaderAction(Action.APP_ICON)
                                .build());
    }

    @Test
    public void notEquals_differentActionStrip() {
        PlaceListNavigationTemplate template =
                PlaceListNavigationTemplate.builder()
                        .setTitle("Title")
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setActionStrip(ActionStrip.builder().addAction(Action.BACK).build())
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        PlaceListNavigationTemplate.builder()
                                .setTitle("Title")
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false,
                                        mDistanceSpan))
                                .setActionStrip(
                                        ActionStrip.builder().addAction(Action.APP_ICON).build())
                                .build());
    }

    @Test
    public void notEquals_differentTitle() {
        PlaceListNavigationTemplate template =
                PlaceListNavigationTemplate.builder()
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setTitle("title")
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        PlaceListNavigationTemplate.builder()
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false,
                                        mDistanceSpan))
                                .setTitle("other")
                                .build());
    }

    @Test
    public void checkPermissions_hasPermissions() {
        PlaceListNavigationTemplate template =
                PlaceListNavigationTemplate.builder()
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setTitle("title")
                        .build();

        // Expect that it does not throw
        template.checkPermissions(
                TestUtils.getMockContextWithPermission(CarAppPermission.NAVIGATION_TEMPLATES));
    }

    @Test
    public void checkPermissions_doesNotHavePermissions() {
        PlaceListNavigationTemplate template =
                PlaceListNavigationTemplate.builder()
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setTitle("title")
                        .build();

        assertThrows(SecurityException.class, () -> template.checkPermissions(mContext));
    }
}
