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
import android.os.RemoteException;
import android.text.SpannableString;

import androidx.car.app.IOnDoneCallback;
import androidx.car.app.TestUtils;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Distance;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.Row;
import androidx.car.app.test.R;
import androidx.car.app.utils.Logger;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link RoutePreviewNavigationTemplate}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class RoutePreviewNavigationTemplateTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private static final DistanceSpan DISTANCE =
            DistanceSpan.create(
                    Distance.create(/* displayDistance= */ 1, Distance.UNIT_KILOMETERS_P1));

    @Test
    public void createInstance_emptyList_notLoading_Throws() {
        assertThrows(
                IllegalStateException.class,
                () -> RoutePreviewNavigationTemplate.builder().setTitle("Title").build());

        // Positive case
        RoutePreviewNavigationTemplate.builder().setTitle("Title").setIsLoading(true).build();
    }

    @Test
    public void createInstance_isLoading_hasList_Throws() {
        assertThrows(
                IllegalStateException.class,
                () -> RoutePreviewNavigationTemplate.builder()
                        .setTitle("Title")
                        .setIsLoading(true)
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .build());
    }

    @Test
    public void addList_notSelectable_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RoutePreviewNavigationTemplate.builder()
                        .setTitle("Title")
                        .setItemList(TestUtils.createItemListWithDistanceSpan(3, false,
                                DISTANCE)));

        // Positive case.
        RoutePreviewNavigationTemplate.builder()
                .setTitle("Title")
                .setItemList(TestUtils.createItemListWithDistanceSpan(3, true, DISTANCE));
    }

    @Test
    public void addList_moreThanMaxTexts_throws() {
        SpannableString title = new SpannableString("Title");
        title.setSpan(DISTANCE, 0, 1, 0);
        Row rowExceedsMaxTexts =
                Row.builder().setTitle(title).addText("text1").addText("text2").addText(
                        "text3").build();
        Row rowMeetingMaxTexts =
                Row.builder().setTitle(title).addText("text1").addText("text2").build();
        assertThrows(
                IllegalArgumentException.class,
                () -> RoutePreviewNavigationTemplate.builder()
                        .setTitle("Title")
                        .setItemList(
                                ItemList.builder()
                                        .addItem(rowExceedsMaxTexts)
                                        .setSelectable(selectedIndex -> {
                                        })
                                        .build()));

        // Positive case.
        RoutePreviewNavigationTemplate.builder()
                .setTitle("Title")
                .setItemList(
                        ItemList.builder()
                                .addItem(rowMeetingMaxTexts)
                                .setSelectable(selectedIndex -> {
                                })
                                .build());
    }

    @Test
    public void noHeaderTitleOrAction_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> RoutePreviewNavigationTemplate.builder().setIsLoading(true).build());

        // Positive cases.
        RoutePreviewNavigationTemplate.builder().setTitle("Title").setIsLoading(true).build();
        RoutePreviewNavigationTemplate.builder()
                .setHeaderAction(Action.BACK)
                .setIsLoading(true)
                .build();
    }

    @Test
    public void createInstance() {
        ItemList itemList = TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE);
        String title = "title";
        RoutePreviewNavigationTemplate template =
                RoutePreviewNavigationTemplate.builder()
                        .setItemList(itemList)
                        .setTitle(title)
                        .setNavigateAction(
                                Action.builder().setTitle("Navigate").setOnClickListener(() -> {
                                }).build())
                        .build();
        assertThat(template.getItemList()).isEqualTo(itemList);
        assertThat(template.getTitle().getText()).isEqualTo(title);
    }

    @Test
    public void createInstance_setHeaderAction_invalidActionThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RoutePreviewNavigationTemplate.builder()
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .setNavigateAction(
                                Action.builder().setTitle("Navigate").setOnClickListener(
                                        () -> {
                                        }).build())
                        .setHeaderAction(
                                Action.builder().setTitle("Action").setOnClickListener(
                                        () -> {
                                        }).build()));
    }

    @Test
    public void createInstance_setHeaderAction() {
        RoutePreviewNavigationTemplate template =
                RoutePreviewNavigationTemplate.builder()
                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .setNavigateAction(
                                Action.builder().setTitle("Navigate").setOnClickListener(() -> {
                                }).build())
                        .setHeaderAction(Action.BACK)
                        .build();

        assertThat(template.getHeaderAction()).isEqualTo(Action.BACK);
    }

    @Test
    @UiThreadTest
    public void setOnNavigateAction() throws RemoteException {
        OnClickListener mockListener = mock(OnClickListener.class);
        RoutePreviewNavigationTemplate template =
                RoutePreviewNavigationTemplate.builder()
                        .setTitle("Title")
                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .setNavigateAction(
                                Action.builder().setTitle("Navigate").setOnClickListener(
                                        mockListener).build())
                        .build();

        template.getNavigateAction()
                .getOnClickListener()
                .getListener()
                .onClick(mock(IOnDoneCallback.class));
        verify(mockListener).onClick();
    }

    @Test
    public void createInstance_emptyNavigateAction_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> RoutePreviewNavigationTemplate.builder()
                        .setTitle("Title")
                        .setItemList(
                                TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .build());

        // Positive case
        RoutePreviewNavigationTemplate.builder().setTitle("Title").setIsLoading(true).build();
    }

    @Test
    public void createInstance_emptyListeners_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RoutePreviewNavigationTemplate.builder()
                        .setTitle("Title")
                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, false,
                                DISTANCE))
                        .setNavigateAction(
                                Action.builder().setTitle("Navigate").setOnClickListener(
                                        () -> {
                                        }).build())
                        .build());

        // Positive case
        RoutePreviewNavigationTemplate.builder().setTitle("Title").setIsLoading(true).build();
    }

    @Test
    public void createInstance_navigateActionNoTitle_throws() {
        CarIcon carIcon = CarIcon.of(IconCompat.createWithResource(
                ApplicationProvider.getApplicationContext(), R.drawable.ic_test_1));
        assertThrows(
                IllegalArgumentException.class,
                () -> RoutePreviewNavigationTemplate.builder()
                        .setTitle("Title")
                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .setNavigateAction(Action.builder().setIcon(carIcon).setOnClickListener(
                                () -> {
                                }).build())
                        .build());

        // Positive case
        RoutePreviewNavigationTemplate.builder()
                .setTitle("Title")
                .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                .setNavigateAction(Action.builder()
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
        Row rowWithTime = Row.builder().setTitle(title).build();
        Row rowWithoutTime = Row.builder().setTitle("Google Bve").build();
        Action navigateAction = Action.builder()
                .setIcon(CarIcon.of(IconCompat.createWithResource(
                        ApplicationProvider.getApplicationContext(),
                        R.drawable.ic_test_1)))
                .setTitle("Navigate")
                .setOnClickListener(() -> {
                })
                .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> RoutePreviewNavigationTemplate.builder()
                        .setTitle("Title")
                        .setItemList(ItemList.builder()
                                .addItem(rowWithTime)
                                .addItem(rowWithoutTime)
                                .setSelectable(index -> {
                                })
                                .build())
                        .setNavigateAction(navigateAction)
                        .build());

        // Positive case
        RoutePreviewNavigationTemplate.builder()
                .setTitle("Title")
                .setItemList(ItemList.builder().setSelectable(index -> {
                }).addItem(rowWithTime).build())
                .setNavigateAction(navigateAction)
                .build();
    }

    @Test
    public void validate_isRefresh() {
        Logger logger = message -> {
        };
        SpannableString title = new SpannableString("Title");
        title.setSpan(DISTANCE, 0, 1, 0);
        Row.Builder row = Row.builder().setTitle(title);
        Action navigateAction = Action.builder().setTitle("Navigate").setOnClickListener(() -> {
        }).build();
        RoutePreviewNavigationTemplate template = RoutePreviewNavigationTemplate.builder()
                .setTitle("Title")
                .setItemList(ItemList.builder().addItem(row.build()).setSelectable(
                        index -> {
                        }).build())
                .setNavigateAction(navigateAction)
                .build();

        assertThat(template.isRefresh(template, logger)).isTrue();

        // Going from loading state to new content is allowed.
        assertThat(template.isRefresh(
                RoutePreviewNavigationTemplate.builder()
                        .setTitle("Title")
                        .setIsLoading(true)
                        .build(),
                logger))
                .isTrue();

        // Other allowed mutable states.
        SpannableString stringWithSpan = new SpannableString("Title");
        stringWithSpan.setSpan(DISTANCE, 1, /* end= */ 2, /* flags= */ 0);
        assertThat(template.isRefresh(
                RoutePreviewNavigationTemplate.builder()
                        .setTitle("Title")
                        .setItemList(ItemList.builder()
                                .addItem(row.setImage(
                                        CarIcon.of(IconCompat.createWithResource(
                                                ApplicationProvider.getApplicationContext(),
                                                R.drawable.ic_test_1)))
                                        .setTitle(stringWithSpan)
                                        .build())
                                .setSelectable(index -> {
                                })
                                .build())
                        .setHeaderAction(Action.BACK)
                        .setNavigateAction(Action.builder().setTitle(
                                "Navigate2").setOnClickListener(() -> {
                                }
                        ).build())
                        .setActionStrip(ActionStrip.builder().addAction(Action.APP_ICON).build())
                        .build(),
                logger))
                .isTrue();

        // Title updates are disallowed.
        assertThat(template.isRefresh(
                RoutePreviewNavigationTemplate.builder()
                        .setItemList(ItemList.builder().addItem(row.build()).setSelectable(
                                index -> {
                                }).build())
                        .setTitle("Title2")
                        .setNavigateAction(navigateAction)
                        .build(),
                logger))
                .isFalse();

        // Text updates are disallowed.
        SpannableString title2 = new SpannableString("Title2");
        title2.setSpan(DISTANCE, 0, 1, 0);
        assertThat(
                template.isRefresh(
                        RoutePreviewNavigationTemplate.builder()
                                .setTitle("Title")
                                .setItemList(
                                        ItemList.builder()
                                                .addItem(row.setTitle(title2).build())
                                                .setSelectable(index -> {
                                                })
                                                .build())
                                .setNavigateAction(navigateAction)
                                .build(),
                        logger))
                .isFalse();
        assertThat(
                template.isRefresh(
                        RoutePreviewNavigationTemplate.builder()
                                .setTitle("Title")
                                .setItemList(
                                        ItemList.builder()
                                                .addItem(row.addText("Text").build())
                                                .setSelectable(index -> {
                                                })
                                                .build())
                                .setNavigateAction(navigateAction)
                                .build(),
                        logger))
                .isFalse();

        // Additional rows are disallowed.
        assertThat(
                template.isRefresh(
                        RoutePreviewNavigationTemplate.builder()
                                .setTitle("Title")
                                .setItemList(
                                        ItemList.builder()
                                                .addItem(row.build())
                                                .addItem(row.build())
                                                .setSelectable(index -> {
                                                })
                                                .build())
                                .setNavigateAction(navigateAction)
                                .build(),
                        logger))
                .isFalse();

        // Going from content to loading state is disallowed.
        assertThat(
                RoutePreviewNavigationTemplate.builder()
                        .setTitle("Title")
                        .setIsLoading(true)
                        .build()
                        .isRefresh(template, logger))
                .isFalse();
    }

    @Test
    public void equals() {
        RoutePreviewNavigationTemplate template =
                RoutePreviewNavigationTemplate.builder()
                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .setActionStrip(ActionStrip.builder().addAction(Action.BACK).build())
                        .setTitle("title")
                        .setHeaderAction(Action.BACK)
                        .setNavigateAction(
                                Action.builder().setTitle("drive").setOnClickListener(() -> {
                                }).build())
                        .build();

        assertThat(template)
                .isEqualTo(
                        RoutePreviewNavigationTemplate.builder()
                                .setItemList(
                                        TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                                .setActionStrip(
                                        ActionStrip.builder().addAction(Action.BACK).build())
                                .setTitle("title")
                                .setHeaderAction(Action.BACK)
                                .setNavigateAction(
                                        Action.builder().setTitle("drive").setOnClickListener(
                                                () -> {
                                                }).build())
                                .build());
    }

    @Test
    public void notEquals_differentItemList() {
        RoutePreviewNavigationTemplate template =
                RoutePreviewNavigationTemplate.builder()
                        .setTitle("Title")
                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .setNavigateAction(
                                Action.builder().setTitle("drive").setOnClickListener(() -> {
                                }).build())
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        RoutePreviewNavigationTemplate.builder()
                                .setTitle("Title")
                                .setItemList(
                                        TestUtils.createItemListWithDistanceSpan(1, true, DISTANCE))
                                .setNavigateAction(
                                        Action.builder().setTitle("drive").setOnClickListener(
                                                () -> {
                                                }).build())
                                .build());
    }

    @Test
    public void notEquals_differentHeaderAction() {
        RoutePreviewNavigationTemplate template =
                RoutePreviewNavigationTemplate.builder()
                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .setHeaderAction(Action.BACK)
                        .setNavigateAction(
                                Action.builder().setTitle("drive").setOnClickListener(() -> {
                                }).build())
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        RoutePreviewNavigationTemplate.builder()
                                .setItemList(
                                        TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                                .setHeaderAction(Action.APP_ICON)
                                .setNavigateAction(
                                        Action.builder().setTitle("drive").setOnClickListener(
                                                () -> {
                                                }).build())
                                .build());
    }

    @Test
    public void notEquals_differentActionStrip() {
        RoutePreviewNavigationTemplate template =
                RoutePreviewNavigationTemplate.builder()
                        .setTitle("Title")
                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .setActionStrip(ActionStrip.builder().addAction(Action.BACK).build())
                        .setNavigateAction(
                                Action.builder().setTitle("drive").setOnClickListener(() -> {
                                }).build())
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        RoutePreviewNavigationTemplate.builder()
                                .setTitle("Title")
                                .setItemList(
                                        TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                                .setActionStrip(
                                        ActionStrip.builder().addAction(Action.APP_ICON).build())
                                .setNavigateAction(
                                        Action.builder().setTitle("drive").setOnClickListener(
                                                () -> {
                                                }).build())
                                .build());
    }

    @Test
    public void notEquals_differentTitle() {
        SpannableString title = new SpannableString("Title");
        title.setSpan(DISTANCE, 0, 1, 0);
        RoutePreviewNavigationTemplate template =
                RoutePreviewNavigationTemplate.builder()
                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .setTitle(title)
                        .setNavigateAction(
                                Action.builder().setTitle("drive").setOnClickListener(() -> {
                                }).build())
                        .build();

        SpannableString title2 = new SpannableString("Title2");
        title2.setSpan(DISTANCE, 0, 1, 0);
        assertThat(template)
                .isNotEqualTo(
                        RoutePreviewNavigationTemplate.builder()
                                .setItemList(
                                        TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                                .setTitle(title2)
                                .setNavigateAction(
                                        Action.builder().setTitle("drive").setOnClickListener(
                                                () -> {
                                                }).build())
                                .build());
    }

    @Test
    public void notEquals_differentNavigateAction() {
        RoutePreviewNavigationTemplate template =
                RoutePreviewNavigationTemplate.builder()
                        .setTitle("Title")
                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .setNavigateAction(
                                Action.builder().setTitle("drive").setOnClickListener(() -> {
                                }).build())
                        .build();

        assertThat(template)
                .isNotEqualTo(
                        RoutePreviewNavigationTemplate.builder()
                                .setTitle("Title")
                                .setItemList(
                                        TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                                .setNavigateAction(
                                        Action.builder().setTitle("stop").setOnClickListener(() -> {
                                        }).build())
                                .build());
    }

    @Test
    public void checkPermissions_hasPermissions() {
        //TODO(rampara): Investigate failure to create ShadowPackageManager
//        RoutePreviewNavigationTemplate template =
//                RoutePreviewNavigationTemplate.builder()
//                        .setTitle("Title")
//                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
//                        .setNavigateAction(
//                                Action.builder().setTitle("drive").setOnClickListener(() -> {
//                                }).build())
//                        .build();
//
//        PackageManager packageManager = mContext.getPackageManager();
//        PackageInfo pi = new PackageInfo();
//        pi.packageName = mContext.getPackageName();
//        pi.versionCode = 1;
//        pi.requestedPermissions = new String[]{CarAppPermission.NAVIGATION_TEMPLATES};
//        shadowOf(packageManager).installPackage(pi);
//
//        // Expect that it does not throw
//        template.checkPermissions(mContext);
    }

    @Test
    public void checkPermissions_doesNotHavePermissions() {
        RoutePreviewNavigationTemplate template =
                RoutePreviewNavigationTemplate.builder()
                        .setTitle("Title")
                        .setItemList(TestUtils.createItemListWithDistanceSpan(2, true, DISTANCE))
                        .setNavigateAction(
                                Action.builder().setTitle("drive").setOnClickListener(() -> {
                                }).build())
                        .build();

        assertThrows(SecurityException.class, () -> template.checkPermissions(mContext));
    }
}
