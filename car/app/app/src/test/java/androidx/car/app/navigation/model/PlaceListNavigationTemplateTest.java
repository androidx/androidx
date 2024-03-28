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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.text.SpannableString;

import androidx.car.app.OnDoneCallback;
import androidx.car.app.TestUtils;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarLocation;
import androidx.car.app.model.Distance;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Metadata;
import androidx.car.app.model.OnContentRefreshListener;
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
@SuppressWarnings("deprecation")
public class PlaceListNavigationTemplateTest {
    private static final Header DEFAULT_HEADER = new Header.Builder()
            .setTitle("Title")
            .build();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final DistanceSpan mDistanceSpan =
            DistanceSpan.create(
                    Distance.create(/* displayDistance= */ 1, Distance.UNIT_KILOMETERS_P1));
    private final ActionStrip mActionStrip =
            new ActionStrip.Builder().addAction(TestUtils.createAction("test", null)).build();
    private final ActionStrip mMapActionStrip =
            new ActionStrip.Builder().addAction(
                    TestUtils.createAction(null, TestUtils.getTestCarIcon(
                            ApplicationProvider.getApplicationContext(),
                            "ic_test_1"))).build();

    @Test
    public void textButtonInMapActionStrip_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new PlaceListNavigationTemplate.Builder().setMapActionStrip(mActionStrip));
    }

    @Test
    public void createInstance_emptyList_notLoading_Throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PlaceListNavigationTemplate.Builder().setHeader(DEFAULT_HEADER).build());

        // Positive case
        new PlaceListNavigationTemplate.Builder().setHeader(DEFAULT_HEADER).setLoading(
                true).build();
    }

    @Test
    public void createInstance_isLoading_hasList_Throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new PlaceListNavigationTemplate.Builder()
                                .setHeader(DEFAULT_HEADER)
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
                                .setHeader(DEFAULT_HEADER)
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, true,
                                        mDistanceSpan))
                                .build());

        // Positive cases.
        new PlaceListNavigationTemplate.Builder()
                .setHeader(DEFAULT_HEADER)
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
                                .setHeader(DEFAULT_HEADER)
                                .setItemList(
                                        new ItemList.Builder().addItem(rowExceedsMaxTexts).build())
                                .build());

        // Positive cases.
        new PlaceListNavigationTemplate.Builder()
                .setHeader(DEFAULT_HEADER)
                .setItemList(new ItemList.Builder().addItem(rowMeetingMaxTexts).build())
                .build();
    }

    @Test
    public void addList_hasToggle() {
        SpannableString title = new SpannableString("Title");
        title.setSpan(mDistanceSpan, /* start= */ 0, /* end= */ 1, /* flags= */ 0);
        Row rowWithToggle =
                new Row.Builder().setTitle(title).setToggle(new Toggle.Builder(isChecked -> {
                }).build()).build();
        Row rowMeetingRestrictions =
                new Row.Builder().setTitle(title).addText("text1").addText("text2").build();

        // Positive cases.
        new PlaceListNavigationTemplate.Builder()
                .setHeader(DEFAULT_HEADER)
                .setItemList(new ItemList.Builder().addItem(rowWithToggle).build())
                .build();

        new PlaceListNavigationTemplate.Builder()
                .setHeader(DEFAULT_HEADER)
                .setItemList(new ItemList.Builder().addItem(rowMeetingRestrictions).build())
                .build();
    }

    @Test
    public void createInstance_emptyHeader() {
        ItemList itemList = TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan);
        PlaceListNavigationTemplate template =
                new PlaceListNavigationTemplate.Builder().setItemList(itemList).build();
        assertThat(template.getHeader()).isNull();
    }

    @Test
    public void createEmpty() {
        PlaceListNavigationTemplate template =
                new PlaceListNavigationTemplate.Builder()
                        .setHeader(DEFAULT_HEADER)
                        .setItemList(new ItemList.Builder().build())
                        .build();
        assertThat(template.getItemList().getItems()).isEmpty();
        assertThat(template.getHeader().getTitle().toString()).isEqualTo("Title");
        assertThat(template.getActionStrip()).isNull();
        assertThat(template.getOnContentRefreshDelegate()).isNull();
    }

    @Test
    public void createInstance() {
        ItemList itemList = TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan);
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        PlaceListNavigationTemplate template =
                new PlaceListNavigationTemplate.Builder()
                        .setItemList(itemList)
                        .setHeader(DEFAULT_HEADER)
                        .setActionStrip(actionStrip)
                        .setMapActionStrip(mMapActionStrip)
                        .build();
        assertThat(template.getItemList()).isEqualTo(itemList);
        assertThat(template.getActionStrip()).isEqualTo(actionStrip);
        assertThat(template.getHeader().getTitle().toString()).isEqualTo("Title");
        assertThat(template.getMapActionStrip()).isEqualTo(mMapActionStrip);
    }

    @Test
    public void createInstance_setHeader() {
        PlaceListNavigationTemplate template =
                new PlaceListNavigationTemplate.Builder()
                        .setItemList(new ItemList.Builder().build())
                        .setHeader(DEFAULT_HEADER)
                        .build();

        assertThat(template.getHeader()).isEqualTo(DEFAULT_HEADER);
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
                                .setHeader(DEFAULT_HEADER)
                                .setItemList(
                                        new ItemList.Builder().addItem(rowWithDistance).addItem(
                                                rowWithoutDistance).build())
                                .build());

        // Positive cases
        new PlaceListNavigationTemplate.Builder()
                .setHeader(DEFAULT_HEADER)
                .setItemList(new ItemList.Builder().addItem(rowWithDistance).build())
                .build();
        new PlaceListNavigationTemplate.Builder()
                .setHeader(DEFAULT_HEADER)
                .setItemList(
                        new ItemList.Builder()
                                .addItem(rowWithDistance)
                                .addItem(browsableRowWithoutDistance)
                                .build())
                .build();
        new PlaceListNavigationTemplate.Builder()
                .setHeader(DEFAULT_HEADER)
                .setItemList(new ItemList.Builder().addItem(browsableRowWithoutDistance).build())
                .build();
    }

    @Test
    public void createInstance_rowHasBothMarkerAndImages() {
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
                                .setHeader(DEFAULT_HEADER)
                                .setItemList(new ItemList.Builder().addItem(row).build()));
    }

    @Test
    public void setOnContentRefreshListener_triggersListener() {
        OnContentRefreshListener listener = mock(OnContentRefreshListener.class);
        PlaceListNavigationTemplate template =
                new PlaceListNavigationTemplate.Builder()
                        .setHeader(DEFAULT_HEADER)
                        .setItemList(new ItemList.Builder().build())
                        .setOnContentRefreshListener(listener)
                        .build();

        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);
        template.getOnContentRefreshDelegate().sendContentRefreshRequested(onDoneCallback);
        verify(listener).onContentRefreshRequested();
        verify(onDoneCallback).onSuccess(null);
    }

    @Test
    public void equals() {
        OnContentRefreshListener listener = mock(OnContentRefreshListener.class);
        PlaceListNavigationTemplate template =
                new PlaceListNavigationTemplate.Builder()
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setActionStrip(new ActionStrip.Builder().addAction(Action.BACK).build())
                        .setMapActionStrip(mMapActionStrip)
                        .setPanModeListener((panModechanged) -> {
                        })
                        .setHeader(DEFAULT_HEADER)
                        .setOnContentRefreshListener(listener)
                        .build();

        assertThat(template)
                .isEqualTo(
                        new PlaceListNavigationTemplate.Builder()
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false,
                                        mDistanceSpan))
                                .setActionStrip(
                                        new ActionStrip.Builder().addAction(Action.BACK).build())
                                .setMapActionStrip(mMapActionStrip)
                                .setPanModeListener((panModechanged) -> {
                                })
                                .setHeader(DEFAULT_HEADER)
                                .setOnContentRefreshListener(listener)
                                .build());
    }

    @Test
    public void notEquals_differentItemList() {
        PlaceListNavigationTemplate template =
                new PlaceListNavigationTemplate.Builder()
                        .setHeader(DEFAULT_HEADER)
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        new PlaceListNavigationTemplate.Builder()
                                .setHeader(DEFAULT_HEADER)
                                .setItemList(TestUtils.createItemListWithDistanceSpan(5, false,
                                        mDistanceSpan))
                                .build());
    }

    @Test
    public void notEquals_differentActionStrip() {
        PlaceListNavigationTemplate template =
                new PlaceListNavigationTemplate.Builder()
                        .setHeader(DEFAULT_HEADER)
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setActionStrip(new ActionStrip.Builder().addAction(Action.BACK).build())
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        new PlaceListNavigationTemplate.Builder()
                                .setHeader(DEFAULT_HEADER)
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false,
                                        mDistanceSpan))
                                .setActionStrip(
                                        new ActionStrip.Builder().addAction(
                                                Action.APP_ICON).build())
                                .build());
    }

    @Test
    public void notEquals_differentMapActionStrip() {
        PlaceListNavigationTemplate template = new PlaceListNavigationTemplate.Builder()
                .setHeader(DEFAULT_HEADER)
                .setItemList(
                        TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                .setActionStrip(
                        mActionStrip)
                .setMapActionStrip(mMapActionStrip)
                .build();

        assertThat(template)
                .isNotEqualTo(
                        new PlaceListNavigationTemplate.Builder()
                                .setHeader(DEFAULT_HEADER)
                                .setItemList(
                                        TestUtils.createItemListWithDistanceSpan(6, false,
                                                mDistanceSpan))
                                .setActionStrip(mActionStrip)
                                .setMapActionStrip(new ActionStrip.Builder().addAction(
                                        TestUtils.createAction(null, TestUtils.getTestCarIcon(
                                                ApplicationProvider.getApplicationContext(),
                                                "ic_test_2"))).build())
                                .build());
    }

    @Test
    public void notEquals_panModeListenerChange() {
        PlaceListNavigationTemplate template = new PlaceListNavigationTemplate.Builder()
                .setHeader(DEFAULT_HEADER)
                .setItemList(
                        TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                .setActionStrip(
                        mActionStrip)
                .setMapActionStrip(mMapActionStrip)
                .setPanModeListener((panModechanged) -> {
                })
                .build();

        assertThat(template)
                .isNotEqualTo(
                        new PlaceListNavigationTemplate.Builder()
                                .setHeader(DEFAULT_HEADER)
                                .setItemList(
                                        TestUtils.createItemListWithDistanceSpan(6, false,
                                                mDistanceSpan))
                                .setActionStrip(
                                        mActionStrip)
                                .setMapActionStrip(mMapActionStrip)
                                .build());
    }

    @Test
    public void notEquals_nonAndNonNullOnContentRefreshListeners() {
        OnContentRefreshListener listener = mock(OnContentRefreshListener.class);
        PlaceListNavigationTemplate template =
                new PlaceListNavigationTemplate.Builder()
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setHeader(DEFAULT_HEADER)
                        .setOnContentRefreshListener(listener)
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        new PlaceListNavigationTemplate.Builder()
                                .setItemList(TestUtils.createItemListWithDistanceSpan(6, false,
                                        mDistanceSpan))
                                .setHeader(DEFAULT_HEADER)
                                .build());
    }

    @Test
    public void notEquals_differentHeader() {
        PlaceListNavigationTemplate template =
                new PlaceListNavigationTemplate.Builder()
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setHeader(DEFAULT_HEADER)
                        .build();

        Header newHeader = new Header.Builder()
                .setTitle("new title")
                .build();

        assertThat(template)
                .isNotEqualTo(new PlaceListNavigationTemplate.Builder()
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(6, false, mDistanceSpan))
                        .setHeader(newHeader)
                        .build());
    }
}
