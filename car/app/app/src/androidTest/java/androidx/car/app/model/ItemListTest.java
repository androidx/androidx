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
import static org.mockito.Mockito.mock;

import android.os.RemoteException;
import android.text.SpannableString;

import androidx.car.app.IOnDoneCallback;
import androidx.car.app.model.ItemList.OnItemVisibilityChangedListener;
import androidx.car.app.model.ItemList.OnSelectedListener;
import androidx.car.app.utils.Logger;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;

/** Tests for {@link ItemListTest}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class ItemListTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private IOnDoneCallback.Stub mMockOnDoneCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void createEmpty() {
        ItemList list = ItemList.builder().build();
        assertThat(list.getItems()).isEqualTo(Collections.emptyList());
    }

    @Test
    public void createRows() {
        Row row1 = Row.builder().setTitle("Row1").build();
        Row row2 = Row.builder().setTitle("Row2").build();
        ItemList list = ItemList.builder().addItem(row1).addItem(row2).build();

        assertThat(list.getItems()).hasSize(2);
        assertThat(list.getItems().get(0)).isEqualTo(row1);
        assertThat(list.getItems().get(1)).isEqualTo(row2);
    }

    @Test
    public void createGridItems() {
        GridItem gridItem1 = GridItem.builder().setImage(BACK).build();
        GridItem gridItem2 = GridItem.builder().setImage(BACK).build();
        ItemList list = ItemList.builder().addItem(gridItem1).addItem(gridItem2).build();

        assertThat(list.getItems()).containsExactly(gridItem1, gridItem2).inOrder();
    }

    @Test
    public void clearRows() {
        Row row1 = Row.builder().setTitle("Row1").build();
        Row row2 = Row.builder().setTitle("Row2").build();
        ItemList list = ItemList.builder().addItem(row1).addItem(row2).clearItems().build();

        assertThat(list.getItems()).isEmpty();
    }

    @Test
    public void clearGridItems() {
        GridItem gridItem1 = GridItem.builder().setImage(BACK).build();
        GridItem gridItem2 = GridItem.builder().setImage(BACK).build();
        ItemList list = ItemList.builder().addItem(gridItem1).addItem(
                gridItem2).clearItems().build();

        assertThat(list.getItems()).isEmpty();
    }

    @Test
    public void setSelectedable_emptyList_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> ItemList.builder().setSelectable(selectedIndex -> {
                }).build());
    }

    @Test
    public void setSelectedIndex_greaterThanListSize_throws() {
        Row row1 = Row.builder().setTitle("Row1").build();
        assertThrows(
                IllegalStateException.class,
                () ->
                        ItemList.builder()
                                .addItem(row1)
                                .setSelectable(selectedIndex -> {
                                })
                                .setSelectedIndex(2)
                                .build());
    }

    @Test
    public void setSelectable() throws RemoteException {
        OnSelectedListener mockListener = mock(OnSelectedListener.class);
        ItemList itemList =
                ItemList.builder()
                        .addItem(Row.builder().setTitle("title").build())
                        .setSelectable(mockListener)
                        .build();

        // TODO(shiufai): revisit the following as the test is not running on the main looper
        //  thread, and thus the verify is failing.
//        itemList.getOnSelectedListener().onSelected(0, mockOnDoneCallback);
//        verify(mockListener).onSelected(eq(0));
    }

    @Test
    public void setSelectable_disallowOnClickListenerInRows() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        ItemList.builder()
                                .addItem(Row.builder().setTitle("foo").setOnClickListener(() -> {
                                }).build())
                                .setSelectable((index) -> {
                                })
                                .build());

        // Positive test.
        ItemList.builder()
                .addItem(Row.builder().setTitle("foo").build())
                .setSelectable((index) -> {
                })
                .build();
    }

    @Test
    public void setSelectable_disallowToggleInRow() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        ItemList.builder()
                                .addItem(Row.builder().setToggle(Toggle.builder(isChecked -> {
                                }).build()).build())
                                .setSelectable((index) -> {
                                })
                                .build());
    }

    @Test
    public void setOnItemVisibilityChangeListener_triggerListener() throws RemoteException {
        OnItemVisibilityChangedListener listener = mock(OnItemVisibilityChangedListener.class);
        ItemList list =
                ItemList.builder()
                        .addItem(Row.builder().setTitle("1").build())
                        .setOnItemsVisibilityChangeListener(listener)
                        .build();

        // TODO(shiufai): revisit the following as the test is not running on the main looper
        //  thread, and thus the verify is failing.
//        list.getOnItemsVisibilityChangeListener().onItemVisibilityChanged(0, 1,
//        mockOnDoneCallback);
//        ArgumentCaptor<Integer> startIndexCaptor = ArgumentCaptor.forClass(Integer.class);
//        ArgumentCaptor<Integer> endIndexCaptor = ArgumentCaptor.forClass(Integer.class);
//        verify(listener).onItemVisibilityChanged(startIndexCaptor.capture(),
//                endIndexCaptor.capture());
//        assertThat(startIndexCaptor.getValue()).isEqualTo(0);
//        assertThat(endIndexCaptor.getValue()).isEqualTo(1);
    }

    @Test
    public void validateRows_isRefresh() {
        Logger logger = message -> {
        };
        Row.Builder row = Row.builder().setTitle("Title1");
        ItemList listWithRows = ItemList.builder().addItem(row.build()).build();

        // Text updates are disallowed.
        ItemList listWithDifferentTitle =
                ItemList.builder().addItem(row.setTitle("Title2").build()).build();
        ItemList listWithDifferentText =
                ItemList.builder().addItem(row.addText("Text").build()).build();
        assertThat(listWithDifferentTitle.isRefresh(listWithRows, logger)).isFalse();
        assertThat(listWithDifferentText.isRefresh(listWithRows, logger)).isFalse();

        // Additional rows are disallowed.
        ItemList listWithTwoRows = ItemList.builder().addItem(row.build()).addItem(
                row.build()).build();
        assertThat(listWithTwoRows.isRefresh(listWithRows, logger)).isFalse();
    }

    @Test
    public void validateGridItems_isRefresh() {
        Logger logger = message -> {
        };
        GridItem.Builder gridItem = GridItem.builder().setImage(BACK).setTitle("Title1");
        ItemList listWithGridItems = ItemList.builder().addItem(gridItem.build()).build();

        // Text updates are disallowed.
        ItemList listWithDifferentTitle =
                ItemList.builder().addItem(gridItem.setTitle("Title2").build()).build();
        ItemList listWithDifferentText =
                ItemList.builder().addItem(gridItem.setText("Text").build()).build();
        assertThat(listWithDifferentTitle.isRefresh(listWithGridItems, logger)).isFalse();
        assertThat(listWithDifferentText.isRefresh(listWithGridItems, logger)).isFalse();

        // Image updates are disallowed.
        ItemList listWithDifferentImage =
                ItemList.builder().addItem(gridItem.setImage(ALERT).build()).build();
        assertThat(listWithDifferentImage.isRefresh(listWithGridItems, logger)).isFalse();

        // Additional grid items are disallowed.
        ItemList listWithTwoGridItems =
                ItemList.builder().addItem(gridItem.build()).addItem(gridItem.build()).build();
        assertThat(listWithTwoGridItems.isRefresh(listWithGridItems, logger)).isFalse();
    }

    @Test
    public void validateRows_isRefresh_differentSpansAreIgnored() {
        Logger logger = message -> {
        };
        SpannableString textWithDistanceSpan = new SpannableString("Text");
        textWithDistanceSpan.setSpan(
                DistanceSpan.create(Distance.create(1000, Distance.UNIT_KILOMETERS)),
                /* start= */ 0,
                /* end= */ 1,
                /* flags= */ 0);
        SpannableString textWithDurationSpan = new SpannableString("Text");
        textWithDurationSpan.setSpan(DurationSpan.create(1), 0, /* end= */ 1, /* flags= */ 0);

        ItemList list1 =
                ItemList.builder()
                        .addItem(
                                Row.builder().setTitle(textWithDistanceSpan).addText(
                                        textWithDurationSpan).build())
                        .build();
        ItemList list2 =
                ItemList.builder()
                        .addItem(
                                Row.builder().setTitle(textWithDurationSpan).addText(
                                        textWithDistanceSpan).build())
                        .build();
        ItemList list3 =
                ItemList.builder()
                        .addItem(Row.builder().setTitle("Text2").addText("Text2").build())
                        .build();

        assertThat(list2.isRefresh(list1, logger)).isTrue();
        assertThat(list3.isRefresh(list1, logger)).isFalse();
    }

    @Test
    public void validateRows_isRefresh_differentToggleStatesAllowTextUpdates() {
        Logger logger = message -> {
        };
        Toggle onToggle = Toggle.builder(isChecked -> {
        }).setChecked(true).build();
        Toggle offToggle = Toggle.builder(isChecked -> {
        }).setChecked(false).build();

        ItemList listWithOnToggle =
                ItemList.builder()
                        .addItem(Row.builder().setTitle("Title1").setToggle(onToggle).build())
                        .build();
        ItemList listWithOffToggle =
                ItemList.builder()
                        .addItem(Row.builder().setTitle("Title1").setToggle(offToggle).build())
                        .build();
        ItemList listWithoutToggle =
                ItemList.builder().addItem(Row.builder().setTitle("Title2").build()).build();
        ItemList listWithOffToggleDifferentText =
                ItemList.builder()
                        .addItem(Row.builder().setTitle("Title2").addText("Text").setToggle(
                                offToggle).build())
                        .build();
        ItemList listWithOnToggleDifferentText =
                ItemList.builder()
                        .addItem(Row.builder().setTitle("Title2").setToggle(onToggle).build())
                        .build();

        // Going from toggle to no toggle is not a refresh.
        assertThat(listWithOnToggle.isRefresh(listWithoutToggle, logger)).isFalse();

        // Going from on toggle to off toggle, or vice versa, is always a refresh
        assertThat(listWithOnToggle.isRefresh(listWithOffToggleDifferentText, logger)).isTrue();
        assertThat(listWithOffToggleDifferentText.isRefresh(listWithOnToggle, logger)).isTrue();

        // If toggle state is the same, then text changes are not considered a refresh.
        assertThat(listWithOnToggle.isRefresh(listWithOnToggleDifferentText, logger)).isFalse();
        assertThat(listWithOffToggle.isRefresh(listWithOffToggleDifferentText, logger)).isFalse();
    }

    @Test
    public void validateGridItems_isRefresh_differentToggleStatesAllowTextUpdates() {
        Logger logger = message -> {
        };
        Toggle onToggle = Toggle.builder(isChecked -> {
        }).setChecked(true).build();
        Toggle offToggle = Toggle.builder(isChecked -> {
        }).setChecked(false).build();

        ItemList listWithOnToggle =
                ItemList.builder()
                        .addItem(
                                GridItem.builder().setImage(BACK).setTitle("Title1").setToggle(
                                        onToggle).build())
                        .build();
        ItemList listWithOffToggle =
                ItemList.builder()
                        .addItem(
                                GridItem.builder().setImage(BACK).setTitle("Title1").setToggle(
                                        offToggle).build())
                        .build();
        ItemList listWithoutToggle =
                ItemList.builder()
                        .addItem(GridItem.builder().setImage(BACK).setTitle("Title2").build())
                        .build();
        ItemList listWithOffToggleDifferentText =
                ItemList.builder()
                        .addItem(
                                GridItem.builder()
                                        .setImage(BACK)
                                        .setTitle("Title2")
                                        .setText("Text")
                                        .setToggle(offToggle)
                                        .build())
                        .build();
        ItemList listWithOnToggleDifferentText =
                ItemList.builder()
                        .addItem(
                                GridItem.builder().setImage(BACK).setTitle("Title2").setToggle(
                                        onToggle).build())
                        .build();

        // Going from toggle to no toggle is not a refresh.
        assertThat(listWithOnToggle.isRefresh(listWithoutToggle, logger)).isFalse();

        // Going from on toggle to off toggle, or vice versa, is always a refresh
        assertThat(listWithOnToggle.isRefresh(listWithOffToggleDifferentText, logger)).isTrue();
        assertThat(listWithOffToggleDifferentText.isRefresh(listWithOnToggle, logger)).isTrue();

        // If toggle state is the same, then text changes are not considered a refresh.
        assertThat(listWithOnToggle.isRefresh(listWithOnToggleDifferentText, logger)).isFalse();
        assertThat(listWithOffToggle.isRefresh(listWithOffToggleDifferentText, logger)).isFalse();
    }

    @Test
    public void validateGridItems_isRefresh_differentToggleStatesAllowImageUpdates() {
        Logger logger = message -> {
        };
        Toggle onToggle = Toggle.builder(isChecked -> {
        }).setChecked(true).build();
        Toggle offToggle = Toggle.builder(isChecked -> {
        }).setChecked(false).build();

        ItemList listWithOnToggle =
                ItemList.builder()
                        .addItem(GridItem.builder().setImage(BACK).setToggle(onToggle).build())
                        .build();
        ItemList listWithOffToggle =
                ItemList.builder()
                        .addItem(GridItem.builder().setImage(BACK).setToggle(offToggle).build())
                        .build();
        ItemList listWithoutToggle =
                ItemList.builder().addItem(GridItem.builder().setImage(ALERT).build()).build();
        ItemList listWithOffToggleDifferentImage =
                ItemList.builder()
                        .addItem(GridItem.builder().setImage(ALERT).setToggle(offToggle).build())
                        .build();
        ItemList listWithOnToggleDifferentImage =
                ItemList.builder()
                        .addItem(GridItem.builder().setImage(ALERT).setToggle(onToggle).build())
                        .build();

        // Going from toggle to no toggle is not a refresh.
        assertThat(listWithOnToggle.isRefresh(listWithoutToggle, logger)).isFalse();

        // Going from on toggle to off toggle, or vice versa, is always a refresh
        assertThat(listWithOnToggle.isRefresh(listWithOffToggleDifferentImage, logger)).isTrue();
        assertThat(listWithOffToggleDifferentImage.isRefresh(listWithOnToggle, logger)).isTrue();

        // If toggle state is the same, then image changes are not considered a refresh.
        assertThat(listWithOnToggle.isRefresh(listWithOnToggleDifferentImage, logger)).isFalse();
        assertThat(listWithOffToggle.isRefresh(listWithOffToggleDifferentImage, logger)).isFalse();
    }

    @Test
    public void validateGridItems_isRefresh_differentSelectedIndexAllowTextUpdates() {
        Logger logger = message -> {
        };
        OnSelectedListener onSelectedListener = mock(OnSelectedListener.class);

        ItemList listWithItem0Selected =
                ItemList.builder()
                        .addItem(GridItem.builder().setImage(BACK).setTitle("Title11").build())
                        .addItem(GridItem.builder().setImage(BACK).setTitle("Title12").build())
                        .setSelectable(onSelectedListener)
                        .setSelectedIndex(0)
                        .build();
        ItemList listWithItem1Selected =
                ItemList.builder()
                        .addItem(GridItem.builder().setImage(BACK).setTitle("Title21").build())
                        .addItem(GridItem.builder().setImage(BACK).setTitle("Title22").build())
                        .setSelectable(onSelectedListener)
                        .setSelectedIndex(1)
                        .build();
        ItemList listWithItem0SelectedDifferentText =
                ItemList.builder()
                        .addItem(GridItem.builder().setImage(BACK).setTitle("Title21").build())
                        .addItem(GridItem.builder().setImage(BACK).setTitle("Title22").build())
                        .setSelectable(onSelectedListener)
                        .setSelectedIndex(0)
                        .build();
        ItemList listWithoutOnSelectedListener =
                ItemList.builder()
                        .addItem(GridItem.builder().setImage(BACK).setTitle("Title21").build())
                        .addItem(GridItem.builder().setImage(BACK).setTitle("Title22").build())
                        .build();

        // Selecting item 1 from item 0, or vice versa, is always a refresh.
        assertThat(listWithItem0Selected.isRefresh(listWithItem1Selected, logger)).isTrue();
        assertThat(listWithItem1Selected.isRefresh(listWithItem0Selected, logger)).isTrue();

        // If item selection is the same, it is not considered a refresh
        assertThat(listWithItem0Selected.isRefresh(listWithItem0SelectedDifferentText, logger))
                .isFalse();

        // If one of the ItemList doesn't have a selectable state, it is not a refresh.
        assertThat(
                listWithItem0Selected.isRefresh(listWithoutOnSelectedListener, logger)).isFalse();
    }

    @Test
    public void equals_itemListWithRows() {
        Row row = Row.builder().setTitle("Title").build();
        ItemList itemList =
                ItemList.builder()
                        .setSelectable((index) -> {
                        })
                        .setNoItemsMessage("no items")
                        .setSelectedIndex(0)
                        .setOnItemsVisibilityChangeListener((start, end) -> {
                        })
                        .addItem(row)
                        .build();
        assertThat(itemList)
                .isEqualTo(
                        ItemList.builder()
                                .setSelectable((index) -> {
                                })
                                .setNoItemsMessage("no items")
                                .setSelectedIndex(0)
                                .setOnItemsVisibilityChangeListener((start, end) -> {
                                })
                                .addItem(row)
                                .build());
    }

    @Test
    public void equals_itemListWithGridItems() {
        GridItem gridItem = GridItem.builder().setImage(BACK).setTitle("Title").build();
        ItemList itemList =
                ItemList.builder()
                        .setSelectable((index) -> {
                        })
                        .setNoItemsMessage("no items")
                        .setSelectedIndex(0)
                        .setOnItemsVisibilityChangeListener((start, end) -> {
                        })
                        .addItem(gridItem)
                        .build();
        assertThat(itemList)
                .isEqualTo(
                        ItemList.builder()
                                .setSelectable((index) -> {
                                })
                                .setNoItemsMessage("no items")
                                .setSelectedIndex(0)
                                .setOnItemsVisibilityChangeListener((start, end) -> {
                                })
                                .addItem(gridItem)
                                .build());
    }

    @Test
    public void notEquals_differentNoItemsMessage() {
        ItemList itemList = ItemList.builder().setNoItemsMessage("no items").build();
        assertThat(itemList).isNotEqualTo(ItemList.builder().setNoItemsMessage("YO").build());
    }

    @Test
    public void notEquals_differentSelectedIndex() {
        Row row = Row.builder().setTitle("Title").build();
        ItemList itemList =
                ItemList.builder().setSelectable((index) -> {
                }).addItem(row).addItem(row).build();
        assertThat(itemList)
                .isNotEqualTo(
                        ItemList.builder()
                                .setSelectable((index) -> {
                                })
                                .setSelectedIndex(1)
                                .addItem(row)
                                .addItem(row)
                                .build());
    }

    @Test
    public void notEquals_missingSelectedListener() {
        Row row = Row.builder().setTitle("Title").build();
        ItemList itemList =
                ItemList.builder().setSelectable((index) -> {
                }).addItem(row).addItem(row).build();
        assertThat(itemList).isNotEqualTo(ItemList.builder().addItem(row).addItem(row).build());
    }

    @Test
    public void notEquals_missingVisibilityChangedListener() {
        Row row = Row.builder().setTitle("Title").build();
        ItemList itemList =
                ItemList.builder()
                        .setOnItemsVisibilityChangeListener((start, end) -> {
                        })
                        .addItem(row)
                        .addItem(row)
                        .build();
        assertThat(itemList).isNotEqualTo(ItemList.builder().addItem(row).addItem(row).build());
    }

    @Test
    public void notEquals_differentRows() {
        Row row = Row.builder().setTitle("Title").build();
        ItemList itemList = ItemList.builder().addItem(row).addItem(row).build();
        assertThat(itemList).isNotEqualTo(ItemList.builder().addItem(row).build());
    }

    @Test
    public void notEquals_differentGridItems() {
        GridItem gridItem = GridItem.builder().setImage(BACK).setTitle("Title").build();
        ItemList itemList = ItemList.builder().addItem(gridItem).addItem(gridItem).build();
        assertThat(itemList).isNotEqualTo(ItemList.builder().addItem(gridItem).build());
    }
}
