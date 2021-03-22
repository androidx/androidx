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

import androidx.car.app.TestUtils;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarLocation;
import androidx.car.app.model.CarText;
import androidx.car.app.model.Distance;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Metadata;
import androidx.car.app.model.Place;
import androidx.car.app.model.PlaceMarker;
import androidx.car.app.model.Row;
import androidx.car.app.model.Toggle;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link PlaceListNavigationTemplate}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class PlaceListNavigationTemplateTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final DistanceSpan mDistanceSpan =
            DistanceSpan.create(
                    Distance.create(/* displayDistance= */ 1, Distance.UNIT_KILOMETERS_P1));

    @Test
    public void createInstance_emptyList_notLoading_Throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PlaceListNavigationTemplate.Builder().setTitle("Title").build());

        // Positive case
        new PlaceListNavigationTemplate.Builder().setTitle("Title").setLoading(true).build();
    }

    @Test
    public void createInstance_isLoading_hasList_Throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new PlaceListNavigationTemplate.Builder()
                                .setTitle("Title")
                                .setLoading(true)
                                .setItemList(new ItemList.Builder().build())
                                .build());
    }

    @Test
    public void addList_selectable_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new PlaceListNavigationTemplate.Builder()
                                .setTitle("Title")
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, true,
                                        mDistanceSpan))
                                .build());

        // Positive cases.
        new PlaceListNavigationTemplate.Builder()
                .setTitle("Title")
                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                .build();
    }

    @Test
    public void addList_moreThanMaxTexts_throws() {
        SpannableString title = new SpannableString("Title");
        title.setSpan(mDistanceSpan, /* start= */ 0, /* end= */ 1, /* flags= */ 0);
        Row rowExceedsMaxTexts =
                new Row.Builder().setTitle(title).addText("text1").addText("text2").addText(
                        "text3").build();
        Row rowMeetingMaxTexts =
                new Row.Builder().setTitle(title).addText("text1").addText("text2").build();
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new PlaceListNavigationTemplate.Builder()
                                .setTitle("Title")
                                .setItemList(
                                        new ItemList.Builder().addItem(rowExceedsMaxTexts).build())
                                .build());

        // Positive cases.
        new PlaceListNavigationTemplate.Builder()
                .setTitle("Title")
                .setItemList(new ItemList.Builder().addItem(rowMeetingMaxTexts).build())
                .build();
    }

    @Test
    public void addList_hasToggle_throws() {
        SpannableString title = new SpannableString("Title");
        title.setSpan(mDistanceSpan, /* start= */ 0, /* end= */ 1, /* flags= */ 0);
        Row rowWithToggle =
                new Row.Builder().setTitle(title).setToggle(new Toggle.Builder(isChecked -> {
                }).build()).build();
        Row rowMeetingRestrictions =
                new Row.Builder().setTitle(title).addText("text1").addText("text2").build();
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new PlaceListNavigationTemplate.Builder()
                                .setTitle("Title")
                                .setItemList(new ItemList.Builder().addItem(rowWithToggle).build())
                                .build());

        // Positive cases.
        new PlaceListNavigationTemplate.Builder()
                .setTitle("Title")
                .setItemList(new ItemList.Builder().addItem(rowMeetingRestrictions).build())
                .build();
    }

    @Test
    public void createInstance_noHeaderTitleOrAction_throws() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        new PlaceListNavigationTemplate.Builder().setItemList(
                                new ItemList.Builder().build()).build());

        // Positive cases.
        new PlaceListNavigationTemplate.Builder()
                .setTitle("Title")
                .setItemList(new ItemList.Builder().build())
                .build();
        new PlaceListNavigationTemplate.Builder()
                .setHeaderAction(Action.BACK)
                .setItemList(new ItemList.Builder().build())
                .build();
    }

    @Test
    public void createEmpty() {
        PlaceListNavigationTemplate template =
                new PlaceListNavigationTemplate.Builder()
                        .setTitle("Title")
                        .setItemList(new ItemList.Builder().build())
                        .build();
        assertThat(template.getItemList().getItems()).isEmpty();
        assertThat(template.getTitle().toString()).isEqualTo("Title");
        assertThat(template.getActionStrip()).isNull();
    }

    @Test
    public void createInstance() {
        String title = "title";
        ItemList itemList = TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan);
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        PlaceListNavigationTemplate template =
                new PlaceListNavigationTemplate.Builder()
                        .setItemList(itemList)
                        .setTitle(title)
                        .setActionStrip(actionStrip)
                        .build();
        assertThat(template.getItemList()).isEqualTo(itemList);
        assertThat(template.getActionStrip()).isEqualTo(actionStrip);
        assertThat(template.getTitle().toString()).isEqualTo(title);
    }

    @Test
    public void createInstance_setHeaderAction_invalidActionThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new PlaceListNavigationTemplate.Builder()
                                .setHeaderAction(
                                        new Action.Builder().setTitle("Action").setOnClickListener(
                                                () -> {
                                                }).build()));
    }

    @Test
    public void createInstance_setHeaderAction() {
        PlaceListNavigationTemplate template =
                new PlaceListNavigationTemplate.Builder()
                        .setItemList(new ItemList.Builder().build())
                        .setHeaderAction(Action.BACK)
                        .build();

        assertThat(template.getHeaderAction()).isEqualTo(Action.BACK);
    }

    @Test
    public void createInstance_notAllRowHaveDistances() {
        SpannableString title = new SpannableString("Title");
        title.setSpan(mDistanceSpan, /* start= */ 0, /* end= */ 1, /* flags= */ 0);
        Row rowWithDistance = new Row.Builder().setTitle(title).build();
        Row rowWithoutDistance = new Row.Builder().setTitle("Google Kir").build();
        Row browsableRowWithoutDistance =
                new Row.Builder()
                        .setTitle("Google Kir")
                        .setBrowsable(true)
                        .setOnClickListener(() -> {
                        })
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new PlaceListNavigationTemplate.Builder()
                                .setTitle("Title")
                                .setItemList(
                                        new ItemList.Builder().addItem(rowWithDistance).addItem(
                                                rowWithoutDistance).build())
                                .build());

        // Positive cases
        new PlaceListNavigationTemplate.Builder()
                .setTitle("Title")
                .setItemList(new ItemList.Builder().addItem(rowWithDistance).build())
                .build();
        new PlaceListNavigationTemplate.Builder()
                .setTitle("Title")
                .setItemList(
                        new ItemList.Builder()
                                .addItem(rowWithDistance)
                                .addItem(browsableRowWithoutDistance)
                                .build())
                .build();
        new PlaceListNavigationTemplate.Builder()
                .setTitle("Title")
                .setItemList(new ItemList.Builder().addItem(browsableRowWithoutDistance).build())
                .build();
    }

    @Test
    public void createInstance_rowHasBothMarkerAndImages() {
        SpannableString title = new SpannableString("Title");
        title.setSpan(mDistanceSpan, /* start= */ 0, /* end= */ 1, /* flags= */ 0);
        Row row =
                new Row.Builder()
                        .setTitle("Google Kir")
                        .setOnClickListener(() -> {
                        })
                        .setImage(CarIcon.ALERT)
                        .setMetadata(
                                new Metadata.Builder().setPlace(
                                        new Place.Builder(CarLocation.create(10.f, 10.f))
                                                .setMarker(new PlaceMarker.Builder().build())
                                                .build()).build())
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new PlaceListNavigationTemplate.Builder()
                                .setTitle("Title")
                                .setItemList(new ItemList.Builder().addItem(row).build()));
    }

    @Test
    public void createInstance_title_variants() {
        CarText title = new CarText.Builder("Very Long Title").addVariant("Short Title").build();

        PlaceListNavigationTemplate template =
                new PlaceListNavigationTemplate.Builder()
                        .setTitle(title)
                        .setItemList(new ItemList.Builder().build())
                        .build();
        assertThat(template.getTitle()).isNotNull();
        assertThat(template.getTitle().toString()).isEqualTo("Very Long Title");
        assertThat(template.getTitle().getVariants().get(0).toString()).isEqualTo("Short Title");
    }

    @Test
    public void equals() {
        PlaceListNavigationTemplate template =
                new PlaceListNavigationTemplate.Builder()
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setHeaderAction(Action.BACK)
                        .setActionStrip(new ActionStrip.Builder().addAction(Action.BACK).build())
                        .setTitle("title")
                        .build();

        assertThat(template)
                .isEqualTo(
                        new PlaceListNavigationTemplate.Builder()
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false,
                                        mDistanceSpan))
                                .setHeaderAction(Action.BACK)
                                .setActionStrip(
                                        new ActionStrip.Builder().addAction(Action.BACK).build())
                                .setTitle("title")
                                .build());
    }

    @Test
    public void notEquals_differentItemList() {
        PlaceListNavigationTemplate template =
                new PlaceListNavigationTemplate.Builder()
                        .setTitle("Title")
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        new PlaceListNavigationTemplate.Builder()
                                .setTitle("Title")
                                .setItemList(TestUtils.createItemListWithDistanceSpan(5, false,
                                        mDistanceSpan))
                                .build());
    }

    @Test
    public void notEquals_differentHeaderAction() {
        PlaceListNavigationTemplate template =
                new PlaceListNavigationTemplate.Builder()
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setHeaderAction(Action.BACK)
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        new PlaceListNavigationTemplate.Builder()
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false,
                                        mDistanceSpan))
                                .setHeaderAction(Action.APP_ICON)
                                .build());
    }

    @Test
    public void notEquals_differentActionStrip() {
        PlaceListNavigationTemplate template =
                new PlaceListNavigationTemplate.Builder()
                        .setTitle("Title")
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setActionStrip(new ActionStrip.Builder().addAction(Action.BACK).build())
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        new PlaceListNavigationTemplate.Builder()
                                .setTitle("Title")
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false,
                                        mDistanceSpan))
                                .setActionStrip(
                                        new ActionStrip.Builder().addAction(
                                                Action.APP_ICON).build())
                                .build());
    }

    @Test
    public void notEquals_differentTitle() {
        PlaceListNavigationTemplate template =
                new PlaceListNavigationTemplate.Builder()
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setTitle("title")
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        new PlaceListNavigationTemplate.Builder()
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false,
                                        mDistanceSpan))
                                .setTitle("other")
                                .build());
    }
}
