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
import androidx.car.app.model.Distance;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.Row;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link RoutePreviewNavigationTemplate}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class RoutePreviewNavigationTemplateTest {
    private static final Header DEFAULT_HEADER = new Header.Builder()
            .setTitle("title")
            .build();
    private static final DistanceSpan DISTANCE =
            DistanceSpan.create(
                    Distance.create(/* displayDistance= */ 1, Distance.UNIT_KILOMETERS_P1));
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final ActionStrip mActionStrip =
            new ActionStrip.Builder().addAction(TestUtils.createAction("test", null)).build();
    private final ActionStrip mMapActionStrip =
            new ActionStrip.Builder().addAction(
                    TestUtils.createAction(null, TestUtils.getTestCarIcon(
                            ApplicationProvider.getApplicationContext(),
                            "ic_test_1"))).build();

    @Test
    public void createInstance_emptyList_notLoading_Throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new RoutePreviewNavigationTemplate.Builder().setHeader(
                        DEFAULT_HEADER).build());

        // Positive case
        new RoutePreviewNavigationTemplate.Builder().setHeader(DEFAULT_HEADER).setLoading(
                true).build();
    }

    @Test
    public void createInstance_isLoading_hasList_Throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new RoutePreviewNavigationTemplate.Builder()
                        .setHeader(DEFAULT_HEADER)
                        .setLoading(true)
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .build());
    }

    @Test
    public void addList_notSelectable_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RoutePreviewNavigationTemplate.Builder()
                        .setHeader(DEFAULT_HEADER)
                        .setItemList(TestUtils.createItemListWithDistanceSpan(3, false,
                                DISTANCE)));

        // Positive case.
        new RoutePreviewNavigationTemplate.Builder()
                .setHeader(DEFAULT_HEADER)
                .setItemList(TestUtils.createItemListWithDistanceSpan(3, true, DISTANCE));
    }

    @Test
    public void addList_moreThanMaxTexts_throws() {
        SpannableString title = new SpannableString("Title");
        title.setSpan(DISTANCE, 0, 1, 0);
        Row rowExceedsMaxTexts =
                new Row.Builder().setTitle(title).addText("text1").addText("text2").addText(
                        "text3").build();
        Row rowMeetingMaxTexts =
                new Row.Builder().setTitle(title).addText("text1").addText("text2").build();
        assertThrows(
                IllegalArgumentException.class,
                () -> new RoutePreviewNavigationTemplate.Builder()
                        .setHeader(DEFAULT_HEADER)
                        .setItemList(
                                new ItemList.Builder()
                                        .addItem(rowExceedsMaxTexts)
                                        .setOnSelectedListener(selectedIndex -> {
                                        })
                                        .build()));

        // Positive case.
        new RoutePreviewNavigationTemplate.Builder()
                .setHeader(DEFAULT_HEADER)
                .setItemList(
                        new ItemList.Builder()
                                .addItem(rowMeetingMaxTexts)
                                .setOnSelectedListener(selectedIndex -> {
                                })
                                .build());
    }

    @Test
    public void createInstance_emptyHeader() {
        RoutePreviewNavigationTemplate template =
                new RoutePreviewNavigationTemplate.Builder().setLoading(true).build();
        assertThat(template.getHeader()).isNull();
    }

    @Test
    public void textButtonInMapActionStrip_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new RoutePreviewNavigationTemplate.Builder().setMapActionStrip(mActionStrip));
    }

    @Test
    public void createInstance() {
        ItemList itemList = TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE);
        String title = "title";
        RoutePreviewNavigationTemplate template =
                new RoutePreviewNavigationTemplate.Builder()
                        .setItemList(itemList)
                        .setHeader(DEFAULT_HEADER)
                        .setNavigateAction(
                                new Action.Builder().setTitle("Navigate").setOnClickListener(() -> {
                                }).build())
                        .setMapActionStrip(mMapActionStrip)
                        .build();
        assertThat(template.getItemList()).isEqualTo(itemList);
        assertThat(template.getHeader().getTitle().toString()).isEqualTo(title);
    }

    @Test
    public void createInstance_setHeader() {
        RoutePreviewNavigationTemplate template =
                new RoutePreviewNavigationTemplate.Builder()
                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .setNavigateAction(
                                new Action.Builder().setTitle("Navigate").setOnClickListener(() -> {
                                }).build())
                        .setHeader(DEFAULT_HEADER)
                        .build();

        assertThat(template.getHeader()).isEqualTo(DEFAULT_HEADER);
    }

    @Test
    public void setOnNavigateAction() {
        OnClickListener mockListener = mock(OnClickListener.class);
        RoutePreviewNavigationTemplate template =
                new RoutePreviewNavigationTemplate.Builder()
                        .setHeader(DEFAULT_HEADER)
                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .setNavigateAction(
                                new Action.Builder().setTitle("Navigate").setOnClickListener(
                                        mockListener).build())
                        .build();

        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);
        template.getNavigateAction()
                .getOnClickDelegate()
                .sendClick(onDoneCallback);
        verify(mockListener).onClick();
        verify(onDoneCallback).onSuccess(null);
    }

    @Test
    public void createInstance_emptyNavigateAction_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new RoutePreviewNavigationTemplate.Builder()
                        .setHeader(DEFAULT_HEADER)
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .build());

        // Positive case
        new RoutePreviewNavigationTemplate.Builder().setHeader(DEFAULT_HEADER).setLoading(
                true).build();
    }

    @Test
    public void createInstance_emptyListeners_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RoutePreviewNavigationTemplate.Builder()
                        .setHeader(DEFAULT_HEADER)
                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, false,
                                DISTANCE))
                        .setNavigateAction(
                                new Action.Builder().setTitle("Navigate").setOnClickListener(
                                        () -> {
                                        }).build())
                        .build());

        // Positive case
        new RoutePreviewNavigationTemplate.Builder().setHeader(DEFAULT_HEADER).setLoading(
                true).build();
    }

    @Test
    public void createInstance_navigateActionNoTitle_throws() {
        CarIcon carIcon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        assertThrows(
                IllegalArgumentException.class,
                () -> new RoutePreviewNavigationTemplate.Builder()
                        .setHeader(DEFAULT_HEADER)
                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .setNavigateAction(new Action.Builder().setIcon(carIcon).setOnClickListener(
                                () -> {
                                }).build())
                        .build());

        // Positive case
        new RoutePreviewNavigationTemplate.Builder()
                .setHeader(DEFAULT_HEADER)
                .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                .setNavigateAction(new Action.Builder()
                        .setIcon(carIcon)
                        .setTitle("Navigate")
                        .setOnClickListener(() -> {
                        })
                        .build())
                .build();
    }

    @Test
    public void createInstance_notAllRowsHaveTime() {
        SpannableString title = new SpannableString("Title");
        title.setSpan(DISTANCE, 0, 1, 0);
        Row rowWithTime = new Row.Builder().setTitle(title).build();
        Row rowWithoutTime = new Row.Builder().setTitle("Google Bve").build();
        Action navigateAction = new Action.Builder()
                .setIcon(TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                        "ic_test_1"))
                .setTitle("Navigate")
                .setOnClickListener(() -> {
                })
                .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> new RoutePreviewNavigationTemplate.Builder()
                        .setHeader(DEFAULT_HEADER)
                        .setItemList(new ItemList.Builder()
                                .addItem(rowWithTime)
                                .addItem(rowWithoutTime)
                                .setOnSelectedListener(index -> {
                                })
                                .build())
                        .setNavigateAction(navigateAction)
                        .build());

        // Positive case
        new RoutePreviewNavigationTemplate.Builder()
                .setHeader(DEFAULT_HEADER)
                .setItemList(new ItemList.Builder().setOnSelectedListener(index -> {
                }).addItem(rowWithTime).build())
                .setNavigateAction(navigateAction)
                .build();
    }

    @Test
    public void equals() {
        RoutePreviewNavigationTemplate template =
                new RoutePreviewNavigationTemplate.Builder()
                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .setActionStrip(new ActionStrip.Builder().addAction(Action.BACK).build())
                        .setHeader(DEFAULT_HEADER)
                        .setNavigateAction(
                                new Action.Builder().setTitle("drive").setOnClickListener(() -> {
                                }).build())
                        .setMapActionStrip(mMapActionStrip)
                        .setPanModeListener((panModechanged) -> {
                        })
                        .build();

        assertThat(template)
                .isEqualTo(
                        new RoutePreviewNavigationTemplate.Builder()
                                .setItemList(
                                        TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                                .setActionStrip(
                                        new ActionStrip.Builder().addAction(Action.BACK).build())
                                .setHeader(DEFAULT_HEADER)
                                .setNavigateAction(
                                        new Action.Builder().setTitle("drive").setOnClickListener(
                                                () -> {
                                                }).build())
                                .setMapActionStrip(mMapActionStrip)
                                .setPanModeListener((panModechanged) -> {
                                })
                                .build());
    }

    @Test
    public void notEquals_differentItemList() {
        RoutePreviewNavigationTemplate template =
                new RoutePreviewNavigationTemplate.Builder()
                        .setHeader(DEFAULT_HEADER)
                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .setNavigateAction(
                                new Action.Builder().setTitle("drive").setOnClickListener(() -> {
                                }).build())
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        new RoutePreviewNavigationTemplate.Builder()
                                .setHeader(DEFAULT_HEADER)
                                .setItemList(
                                        TestUtils.createItemListWithDistanceSpan(1, true, DISTANCE))
                                .setNavigateAction(
                                        new Action.Builder().setTitle("drive").setOnClickListener(
                                                () -> {
                                                }).build())
                                .build());
    }

    @Test
    public void notEquals_differentActionStrip() {
        RoutePreviewNavigationTemplate template =
                new RoutePreviewNavigationTemplate.Builder()
                        .setHeader(DEFAULT_HEADER)
                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .setActionStrip(new ActionStrip.Builder().addAction(Action.BACK).build())
                        .setNavigateAction(
                                new Action.Builder().setTitle("drive").setOnClickListener(() -> {
                                }).build())
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        new RoutePreviewNavigationTemplate.Builder()
                                .setHeader(DEFAULT_HEADER)
                                .setItemList(
                                        TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                                .setActionStrip(
                                        new ActionStrip.Builder().addAction(
                                                Action.APP_ICON).build())
                                .setNavigateAction(
                                        new Action.Builder().setTitle("drive").setOnClickListener(
                                                () -> {
                                                }).build())
                                .build());
    }

    @Test
    public void notEquals_differentMapActionStrip() {
        RoutePreviewNavigationTemplate template =
                new RoutePreviewNavigationTemplate.Builder()
                        .setHeader(DEFAULT_HEADER)
                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .setActionStrip(new ActionStrip.Builder().addAction(Action.BACK).build())
                        .setNavigateAction(
                                new Action.Builder().setTitle("drive").setOnClickListener(() -> {
                                }).build())
                        .setMapActionStrip(mMapActionStrip)
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        new RoutePreviewNavigationTemplate.Builder()
                                .setHeader(DEFAULT_HEADER)
                                .setItemList(
                                        TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                                .setActionStrip(
                                        new ActionStrip.Builder().addAction(
                                                Action.APP_ICON).build())
                                .setNavigateAction(
                                        new Action.Builder().setTitle("drive").setOnClickListener(
                                                () -> {
                                                }).build())
                                .setMapActionStrip(new ActionStrip.Builder().addAction(
                                        TestUtils.createAction(null, TestUtils.getTestCarIcon(
                                                ApplicationProvider.getApplicationContext(),
                                                "ic_test_2"))).build())
                                .build());
    }

    @Test
    public void notEquals_panModeListenerChange() {
        RoutePreviewNavigationTemplate template =
                new RoutePreviewNavigationTemplate.Builder()
                        .setHeader(DEFAULT_HEADER)
                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .setActionStrip(new ActionStrip.Builder().addAction(Action.BACK).build())
                        .setNavigateAction(
                                new Action.Builder().setTitle("drive").setOnClickListener(() -> {
                                }).build())
                        .setMapActionStrip(mMapActionStrip)
                        .setPanModeListener((panModechanged) -> {
                        })
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        new RoutePreviewNavigationTemplate.Builder()
                                .setHeader(DEFAULT_HEADER)
                                .setItemList(
                                        TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                                .setActionStrip(
                                        new ActionStrip.Builder().addAction(Action.BACK).build())
                                .setNavigateAction(
                                        new Action.Builder().setTitle("drive").setOnClickListener(
                                                () -> {
                                                }).build())
                                .setMapActionStrip(mMapActionStrip)
                                .build());
    }

    @Test
    public void notEquals_differentNavigateAction() {
        RoutePreviewNavigationTemplate template =
                new RoutePreviewNavigationTemplate.Builder()
                        .setHeader(DEFAULT_HEADER)
                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .setNavigateAction(
                                new Action.Builder().setTitle("drive").setOnClickListener(() -> {
                                }).build())
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        new RoutePreviewNavigationTemplate.Builder()
                                .setHeader(DEFAULT_HEADER)
                                .setItemList(
                                        TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                                .setNavigateAction(
                                        new Action.Builder().setTitle("stop").setOnClickListener(
                                                () -> {
                                                }).build())
                                .build());
    }

    @Test
    public void notEquals_differentHeader() {
        RoutePreviewNavigationTemplate template =
                new RoutePreviewNavigationTemplate.Builder()
                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .setHeader(DEFAULT_HEADER)
                        .setNavigateAction(
                                new Action.Builder().setTitle("drive").setOnClickListener(() -> {
                                }).build())
                        .build();

        Header newHeader = new Header.Builder()
                .setTitle("new title")
                .build();

        assertThat(template)
                .isNotEqualTo(
                        new RoutePreviewNavigationTemplate.Builder()
                                .setItemList(
                                        TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                                .setHeader(newHeader)
                                .setNavigateAction(
                                        new Action.Builder().setTitle("drive").setOnClickListener(
                                                () -> {
                                                }).build())
                                .build());
    }
}
