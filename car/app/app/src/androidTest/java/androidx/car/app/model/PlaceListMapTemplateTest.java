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
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link PlaceListMapTemplate}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class PlaceListMapTemplateTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final DistanceSpan mDistanceSpan =
            DistanceSpan.create(
                    Distance.create(/* displayDistance= */ 1, Distance.UNIT_KILOMETERS_P1));

    @Test
    public void createInstance_emptyList_notLoading_Throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PlaceListMapTemplate.builder().setTitle("Title").build());

        // Positive case
        PlaceListMapTemplate.builder().setTitle("Title").setLoading(true).build();
    }

    @Test
    public void createInstance_isLoading_hasList_Throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        PlaceListMapTemplate.builder()
                                .setTitle("Title")
                                .setLoading(true)
                                .setItemList(ItemList.builder().build())
                                .build());
    }

    @Test
    public void addList_selectable_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        PlaceListMapTemplate.builder()
                                .setTitle("Title")
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, true,
                                        mDistanceSpan))
                                .build());

        // Positive cases.
        PlaceListMapTemplate.builder()
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
                        PlaceListMapTemplate.builder()
                                .setTitle("Title")
                                .setItemList(ItemList.builder().addItem(rowExceedsMaxTexts).build())
                                .build());

        // Positive cases.
        PlaceListMapTemplate.builder()
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
                        PlaceListMapTemplate.builder()
                                .setTitle("Title")
                                .setItemList(ItemList.builder().addItem(rowWithToggle).build())
                                .build());

        // Positive cases.
        PlaceListMapTemplate.builder()
                .setTitle("Title")
                .setItemList(ItemList.builder().addItem(rowMeetingRestrictions).build())
                .build();
    }

    @Test
    public void createEmpty() {
        ItemList itemList = ItemList.builder().build();
        PlaceListMapTemplate template =
                PlaceListMapTemplate.builder().setTitle("Title").setItemList(itemList).build();
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
                        PlaceListMapTemplate.builder()
                                .setHeaderAction(
                                        Action.builder().setTitle("Action").setOnClickListener(
                                                () -> {
                                                }).build()));
    }

    @Test
    public void createInstance_setHeaderAction() {
        PlaceListMapTemplate template =
                PlaceListMapTemplate.builder()
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
                        PlaceListMapTemplate.builder()
                                .setTitle("Title")
                                .setItemList(
                                        ItemList.builder()
                                                .addItem(rowWithDistance)
                                                .addItem(rowWithoutDistance)
                                                .build()));

        // Positive cases
        PlaceListMapTemplate.builder()
                .setTitle("Title")
                .setItemList(ItemList.builder().addItem(rowWithDistance).build())
                .build();
        PlaceListMapTemplate.builder()
                .setTitle("Title")
                .setItemList(
                        ItemList.builder()
                                .addItem(rowWithDistance)
                                .addItem(browsableRowWithoutDistance)
                                .build())
                .build();
        PlaceListMapTemplate.builder()
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
                        PlaceListMapTemplate.builder()
                                .setTitle("Title")
                                .setItemList(ItemList.builder().addItem(row).build()));
    }

    @Test
    public void createInstance_setItemList() {
        ItemList itemList = TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan);
        PlaceListMapTemplate template =
                PlaceListMapTemplate.builder().setTitle("Title").setItemList(itemList).build();
        assertThat(template.getItemList()).isEqualTo(itemList);
    }

    @Test
    public void createInstance_setActionStrip() {
        ItemList itemList = TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan);
        ActionStrip actionStrip = ActionStrip.builder().addAction(Action.BACK).build();
        PlaceListMapTemplate template =
                PlaceListMapTemplate.builder()
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
                PlaceListMapTemplate.builder()
                        .setItemList(itemList)
                        .setTitle(title)
                        .setCurrentLocationEnabled(true)
                        .build();

        assertThat(template.getTitle().getText()).isEqualTo(title);
    }

    @Test
    public void createInstance_noHeaderTitleOrAction_throws() {
        ItemList itemList = TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan);

        assertThrows(
                IllegalStateException.class,
                () ->
                        PlaceListMapTemplate.builder()
                                .setItemList(itemList)
                                .setCurrentLocationEnabled(true)
                                .build());

        // Positive cases.
        PlaceListMapTemplate.builder().setTitle("Title").setItemList(itemList).build();
        PlaceListMapTemplate.builder().setHeaderAction(Action.BACK).setItemList(itemList).build();
    }

    @Test
    public void equals() {
        ActionStrip actionStrip = ActionStrip.builder().addAction(Action.BACK).build();
        String title = "foo";
        Place place =
                Place.builder(LatLng.create(123, 456))
                        .setMarker(PlaceMarker.builder().setLabel("A").build())
                        .build();

        PlaceListMapTemplate template =
                PlaceListMapTemplate.builder()
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
                        PlaceListMapTemplate.builder()
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
                PlaceListMapTemplate.builder()
                        .setTitle("Title")
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        PlaceListMapTemplate.builder()
                                .setTitle("Title")
                                .setItemList(ItemList.builder().build())
                                .build());
    }

    @Test
    public void notEquals_differentHeaderAction() {
        PlaceListMapTemplate template =
                PlaceListMapTemplate.builder()
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setHeaderAction(Action.BACK)
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        PlaceListMapTemplate.builder()
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false,
                                        mDistanceSpan))
                                .setHeaderAction(Action.APP_ICON)
                                .build());
    }

    @Test
    public void notEquals_differentActionStrip() {
        ActionStrip actionStrip = ActionStrip.builder().addAction(Action.BACK).build();

        PlaceListMapTemplate template =
                PlaceListMapTemplate.builder()
                        .setTitle("Title")
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setActionStrip(actionStrip)
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        PlaceListMapTemplate.builder()
                                .setTitle("Title")
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false,
                                        mDistanceSpan))
                                .setActionStrip(
                                        ActionStrip.builder().addAction(Action.APP_ICON).build())
                                .build());
    }

    @Test
    public void notEquals_differentTitle() {
        PlaceListMapTemplate template =
                PlaceListMapTemplate.builder()
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setTitle("foo")
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        PlaceListMapTemplate.builder()
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false,
                                        mDistanceSpan))
                                .setTitle("bar")
                                .build());
    }

    @Test
    public void notEquals_differentAnchor() {
        Place place1 =
                Place.builder(LatLng.create(123, 456))
                        .setMarker(PlaceMarker.builder().setLabel("A").build())
                        .build();

        Place place2 =
                Place.builder(LatLng.create(123, 456))
                        .setMarker(PlaceMarker.builder().setLabel("B").build())
                        .build();

        PlaceListMapTemplate template =
                PlaceListMapTemplate.builder()
                        .setTitle("Title")
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setAnchor(place1)
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        PlaceListMapTemplate.builder()
                                .setTitle("Title")
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false,
                                        mDistanceSpan))
                                .setAnchor(place2)
                                .build());
    }

    @Test
    public void notEquals_differentLocationEnabled() {
        PlaceListMapTemplate template =
                PlaceListMapTemplate.builder()
                        .setTitle("Title")
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setCurrentLocationEnabled(true)
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        PlaceListMapTemplate.builder()
                                .setTitle("Title")
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false,
                                        mDistanceSpan))
                                .setCurrentLocationEnabled(false)
                                .build());
    }

// TODO(shiufai): the following shadow is resulting in a ClasscastException.
//  Further investigation is needed.
//    @Test
//    public void checkPermissions_hasPermissions() {
//        PlaceListMapTemplate template =
//                PlaceListMapTemplate.builder()
//                        .setTitle("Title")
//                        .setItemList(
//                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
//                        .setCurrentLocationEnabled(true)
//                        .build();
//
//        PackageManager packageManager = mContext.getPackageManager();
//        PackageInfo pi = new PackageInfo();
//        pi.packageName = mContext.getPackageName();
//        pi.versionCode = 1;
//        pi.requestedPermissions = new String[]{permission.ACCESS_FINE_LOCATION};
//
//        shadowOf(packageManager).installPackage(pi);
//
//        // Expect that it does not throw
//        template.checkPermissions(context);
//    }

// TODO(shiufai): the following shadow is resulting in a ClasscastException.
//  Further investigation is needed.
//    @Test
//    public void checkPermissions_doesNotHaveFineLocationPermission() {
//        PlaceListMapTemplate template =
//                PlaceListMapTemplate.builder()
//                        .setTitle("Title")
//                        .setItemList(
//                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
//                        .setCurrentLocationEnabled(true)
//                        .build();
//
//        PackageManager packageManager = mContext.getPackageManager();
//        PackageInfo pi = new PackageInfo();
//        pi.packageName = mContext.getPackageName();
//        pi.versionCode = 1;
//
//        shadowOf(packageManager).installPackage(pi);
//        assertThrows(SecurityException.class, () -> template.checkPermissions(context));
//    }

    @Test
    public void checkPermissions_doesNotHavePermissions() {
        PlaceListMapTemplate template =
                PlaceListMapTemplate.builder()
                        .setTitle("Title")
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setCurrentLocationEnabled(true)
                        .build();

        assertThrows(SecurityException.class, () -> template.checkPermissions(mContext));
    }
}
