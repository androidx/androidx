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

import android.content.Context;
import android.text.SpannableString;

import androidx.car.app.TestUtils;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link PlaceListMapTemplate}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class PlaceListMapTemplateTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final DistanceSpan mDistanceSpan =
            DistanceSpan.create(
                    Distance.create(/* displayDistance= */ 1, Distance.UNIT_KILOMETERS_P1));

    @Test
    public void createInstance_emptyList_notLoading_Throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PlaceListMapTemplate.Builder().setTitle("Title").build());

        // Positive case
        new PlaceListMapTemplate.Builder().setTitle("Title").setLoading(true).build();
    }

    @Test
    public void createInstance_isLoading_hasList_Throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new PlaceListMapTemplate.Builder()
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
                        new PlaceListMapTemplate.Builder()
                                .setTitle("Title")
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, true,
                                        mDistanceSpan))
                                .build());

        // Positive cases.
        new PlaceListMapTemplate.Builder()
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
                        new PlaceListMapTemplate.Builder()
                                .setTitle("Title")
                                .setItemList(
                                        new ItemList.Builder().addItem(rowExceedsMaxTexts).build())
                                .build());

        // Positive cases.
        new PlaceListMapTemplate.Builder()
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
                        new PlaceListMapTemplate.Builder()
                                .setTitle("Title")
                                .setItemList(new ItemList.Builder().addItem(rowWithToggle).build())
                                .build());

        // Positive cases.
        new PlaceListMapTemplate.Builder()
                .setTitle("Title")
                .setItemList(new ItemList.Builder().addItem(rowMeetingRestrictions).build())
                .build();
    }

    @Test
    public void createEmpty() {
        ItemList itemList = new ItemList.Builder().build();
        PlaceListMapTemplate template =
                new PlaceListMapTemplate.Builder().setTitle("Title").setItemList(itemList).build();
        assertThat(template.getItemList()).isEqualTo(itemList);
        assertThat(template.getHeaderAction()).isNull();
        assertThat(template.getActionStrip()).isNull();
        assertThat(template.isCurrentLocationEnabled()).isFalse();
    }

    @Test
    public void createInstance_setHeaderAction_invalidActionThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new PlaceListMapTemplate.Builder()
                                .setHeaderAction(
                                        new Action.Builder().setTitle("Action").setOnClickListener(
                                                () -> {
                                                }).build()));
    }

    @Test
    public void createInstance_setHeaderAction() {
        PlaceListMapTemplate template =
                new PlaceListMapTemplate.Builder()
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
                        new PlaceListMapTemplate.Builder()
                                .setTitle("Title")
                                .setItemList(
                                        new ItemList.Builder()
                                                .addItem(rowWithDistance)
                                                .addItem(rowWithoutDistance)
                                                .build()));

        // Positive cases
        new PlaceListMapTemplate.Builder()
                .setTitle("Title")
                .setItemList(new ItemList.Builder().addItem(rowWithDistance).build())
                .build();
        new PlaceListMapTemplate.Builder()
                .setTitle("Title")
                .setItemList(
                        new ItemList.Builder()
                                .addItem(rowWithDistance)
                                .addItem(browsableRowWithoutDistance)
                                .build())
                .build();
        new PlaceListMapTemplate.Builder()
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
                        new PlaceListMapTemplate.Builder()
                                .setTitle("Title")
                                .setItemList(new ItemList.Builder().addItem(row).build()));
    }

    @Test
    public void createInstance_setItemList() {
        ItemList itemList = TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan);
        PlaceListMapTemplate template =
                new PlaceListMapTemplate.Builder().setTitle("Title").setItemList(itemList).build();
        assertThat(template.getItemList()).isEqualTo(itemList);
    }

    @Test
    public void createInstance_setActionStrip() {
        ItemList itemList = TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan);
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        PlaceListMapTemplate template =
                new PlaceListMapTemplate.Builder()
                        .setTitle("Title")
                        .setItemList(itemList)
                        .setActionStrip(actionStrip)
                        .build();
        assertThat(template.getActionStrip()).isEqualTo(actionStrip);
    }

    @Test
    public void createInstance_setTitle() {
        ItemList itemList = TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan);
        String title = "title";
        PlaceListMapTemplate template =
                new PlaceListMapTemplate.Builder()
                        .setItemList(itemList)
                        .setTitle(title)
                        .setCurrentLocationEnabled(true)
                        .build();

        assertThat(template.getTitle().toString()).isEqualTo(title);
    }

    @Test
    public void createInstance_title_variants() {
        CarText title = new CarText.Builder("Very Long Title").addVariant("Short Title").build();
        ItemList itemList = TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan);

        PlaceListMapTemplate template =
                new PlaceListMapTemplate.Builder()
                        .setItemList(itemList)
                        .setTitle(title)
                        .setCurrentLocationEnabled(true)
                        .build();
        assertThat(template.getTitle()).isNotNull();
        assertThat(template.getTitle().toString()).isEqualTo("Very Long Title");
        assertThat(template.getTitle().getVariants().get(0).toString()).isEqualTo("Short Title");
    }

    @Test
    public void createInstance_noHeaderTitleOrAction_throws() {
        ItemList itemList = TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan);

        assertThrows(
                IllegalStateException.class,
                () ->
                        new PlaceListMapTemplate.Builder()
                                .setItemList(itemList)
                                .setCurrentLocationEnabled(true)
                                .build());

        // Positive cases.
        new PlaceListMapTemplate.Builder().setTitle("Title").setItemList(itemList).build();
        new PlaceListMapTemplate.Builder().setHeaderAction(Action.BACK).setItemList(
                itemList).build();
    }

    @Test
    public void equals() {
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        String title = "foo";
        Place place =
                new Place.Builder(CarLocation.create(123, 456))
                        .setMarker(new PlaceMarker.Builder().setLabel("A").build())
                        .build();

        PlaceListMapTemplate template =
                new PlaceListMapTemplate.Builder()
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setHeaderAction(Action.BACK)
                        .setActionStrip(actionStrip)
                        .setTitle(title)
                        .setAnchor(place)
                        .setCurrentLocationEnabled(true)
                        .build();

        assertThat(template)
                .isEqualTo(
                        new PlaceListMapTemplate.Builder()
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false,
                                        mDistanceSpan))
                                .setHeaderAction(Action.BACK)
                                .setActionStrip(actionStrip)
                                .setTitle(title)
                                .setAnchor(place)
                                .setCurrentLocationEnabled(true)
                                .build());
    }

    @Test
    public void notEquals_differentList() {
        PlaceListMapTemplate template =
                new PlaceListMapTemplate.Builder()
                        .setTitle("Title")
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        new PlaceListMapTemplate.Builder()
                                .setTitle("Title")
                                .setItemList(new ItemList.Builder().build())
                                .build());
    }

    @Test
    public void notEquals_differentHeaderAction() {
        PlaceListMapTemplate template =
                new PlaceListMapTemplate.Builder()
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setHeaderAction(Action.BACK)
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        new PlaceListMapTemplate.Builder()
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false,
                                        mDistanceSpan))
                                .setHeaderAction(Action.APP_ICON)
                                .build());
    }

    @Test
    public void notEquals_differentActionStrip() {
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();

        PlaceListMapTemplate template =
                new PlaceListMapTemplate.Builder()
                        .setTitle("Title")
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setActionStrip(actionStrip)
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        new PlaceListMapTemplate.Builder()
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
        PlaceListMapTemplate template =
                new PlaceListMapTemplate.Builder()
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setTitle("foo")
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        new PlaceListMapTemplate.Builder()
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false,
                                        mDistanceSpan))
                                .setTitle("bar")
                                .build());
    }

    @Test
    public void notEquals_differentAnchor() {
        Place place1 =
                new Place.Builder(CarLocation.create(123, 456))
                        .setMarker(new PlaceMarker.Builder().setLabel("A").build())
                        .build();

        Place place2 =
                new Place.Builder(CarLocation.create(123, 456))
                        .setMarker(new PlaceMarker.Builder().setLabel("B").build())
                        .build();

        PlaceListMapTemplate template =
                new PlaceListMapTemplate.Builder()
                        .setTitle("Title")
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setAnchor(place1)
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        new PlaceListMapTemplate.Builder()
                                .setTitle("Title")
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false,
                                        mDistanceSpan))
                                .setAnchor(place2)
                                .build());
    }

    @Test
    public void notEquals_differentLocationEnabled() {
        PlaceListMapTemplate template =
                new PlaceListMapTemplate.Builder()
                        .setTitle("Title")
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setCurrentLocationEnabled(true)
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        new PlaceListMapTemplate.Builder()
                                .setTitle("Title")
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false,
                                        mDistanceSpan))
                                .setCurrentLocationEnabled(false)
                                .build());
    }
}
