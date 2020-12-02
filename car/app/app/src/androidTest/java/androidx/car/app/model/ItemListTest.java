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
import static androidx.car.app.model.ItemList.builder;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.os.RemoteException;

import androidx.car.app.IOnDoneCallback;
import androidx.car.app.OnDoneCallback;
import androidx.car.app.WrappedRuntimeException;
import androidx.car.app.model.ItemList.OnItemVisibilityChangedListener;
import androidx.car.app.model.ItemList.OnSelectedListener;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
        ItemList list = builder().build();
        assertThat(list.getItems()).isEqualTo(Collections.emptyList());
    }

    @Test
    public void createRows() {
        Row row1 = Row.builder().setTitle("Row1").build();
        Row row2 = Row.builder().setTitle("Row2").build();
        ItemList list = builder().addItem(row1).addItem(row2).build();

        assertThat(list.getItems()).hasSize(2);
        assertThat(list.getItems().get(0)).isEqualTo(row1);
        assertThat(list.getItems().get(1)).isEqualTo(row2);
    }

    @Test
    public void createGridItems() {
        GridItem gridItem1 = GridItem.builder().setImage(BACK).build();
        GridItem gridItem2 = GridItem.builder().setImage(BACK).build();
        ItemList list = builder().addItem(gridItem1).addItem(gridItem2).build();

        assertThat(list.getItems()).containsExactly(gridItem1, gridItem2).inOrder();
    }

    @Test
    public void clearRows() {
        Row row1 = Row.builder().setTitle("Row1").build();
        Row row2 = Row.builder().setTitle("Row2").build();
        ItemList list = builder().addItem(row1).addItem(row2).clearItems().build();

        assertThat(list.getItems()).isEmpty();
    }

    @Test
    public void clearGridItems() {
        GridItem gridItem1 = GridItem.builder().setImage(BACK).build();
        GridItem gridItem2 = GridItem.builder().setImage(BACK).build();
        ItemList list = builder().addItem(gridItem1).addItem(
                gridItem2).clearItems().build();

        assertThat(list.getItems()).isEmpty();
    }

    @Test
    public void setSelectedable_emptyList_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> builder().setOnSelectedListener(selectedIndex -> {
                }).build());
    }

    @Test
    public void setSelectedIndex_greaterThanListSize_throws() {
        Row row1 = Row.builder().setTitle("Row1").build();
        assertThrows(
                IllegalStateException.class,
                () -> builder()
                        .addItem(row1)
                        .setOnSelectedListener(selectedIndex -> {
                        })
                        .setSelectedIndex(2)
                        .build());
    }

    @Test
    @UiThreadTest
    public void setSelectable() throws RemoteException {
        OnSelectedListener mockListener = mock(OnSelectedListener.class);
        ItemList itemList =
                builder()
                        .addItem(Row.builder().setTitle("title").build())
                        .setOnSelectedListener(mockListener)
                        .build();

        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);


        itemList.getOnSelectedListener().onSelected(0, onDoneCallback);
        verify(mockListener).onSelected(eq(0));
        verify(onDoneCallback).onSuccess(null);
    }

    @Test
    public void setSelectable_disallowOnClickListenerInRows() {
        assertThrows(
                IllegalStateException.class,
                () -> builder()
                        .addItem(Row.builder().setTitle("foo").setOnClickListener(() -> {
                        }).build())
                        .setOnSelectedListener((index) -> {
                        })
                        .build());

        // Positive test.
        builder()
                .addItem(Row.builder().setTitle("foo").build())
                .setOnSelectedListener((index) -> {
                })
                .build();
    }

    @Test
    public void setSelectable_disallowToggleInRow() {
        assertThrows(
                IllegalStateException.class,
                () -> builder()
                        .addItem(Row.builder().setToggle(Toggle.builder(isChecked -> {
                        }).build()).build())
                        .setOnSelectedListener((index) -> {
                        })
                        .build());
    }

    @Test
    @UiThreadTest
    public void setOnItemVisibilityChangeListener_triggerListener() {
        OnItemVisibilityChangedListener listener = mock(OnItemVisibilityChangedListener.class);
        ItemList list =
                builder()
                        .addItem(Row.builder().setTitle("1").build())
                        .setOnItemsVisibilityChangedListener(listener)
                        .build();

        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);
        list.getOnItemsVisibilityChangedListener().onItemVisibilityChanged(0, 1,
                onDoneCallback);
        ArgumentCaptor<Integer> startIndexCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> endIndexCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(listener).onItemVisibilityChanged(startIndexCaptor.capture(),
                endIndexCaptor.capture());
        verify(onDoneCallback).onSuccess(null);
        assertThat(startIndexCaptor.getValue()).isEqualTo(0);
        assertThat(endIndexCaptor.getValue()).isEqualTo(1);
    }

    @Test
    @UiThreadTest
    public void setOnItemVisibilityChangeListener_triggerListenerWithFailure() {
        OnItemVisibilityChangedListener listener = mock(OnItemVisibilityChangedListener.class);
        ItemList list =
                builder()
                        .addItem(Row.builder().setTitle("1").build())
                        .setOnItemsVisibilityChangedListener(listener)
                        .build();

        String testExceptionMessage = "Test exception";
        doThrow(new RuntimeException(testExceptionMessage)).when(listener).onItemVisibilityChanged(
                0, 1);

        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);
        try {
            list.getOnItemsVisibilityChangedListener().onItemVisibilityChanged(0, 1,
                    onDoneCallback);
        } catch (WrappedRuntimeException e) {
            assertThat(e.getMessage()).contains(testExceptionMessage);
        }

        ArgumentCaptor<Integer> startIndexCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> endIndexCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(listener).onItemVisibilityChanged(startIndexCaptor.capture(),
                endIndexCaptor.capture());
        verify(onDoneCallback).onFailure(any());
        assertThat(startIndexCaptor.getValue()).isEqualTo(0);
        assertThat(endIndexCaptor.getValue()).isEqualTo(1);
    }

    @Test
    public void equals_itemListWithRows() {
        Row row = Row.builder().setTitle("Title").build();
        ItemList itemList =
                builder()
                        .setOnSelectedListener((index) -> {
                        })
                        .setNoItemsMessage("no items")
                        .setSelectedIndex(0)
                        .setOnItemsVisibilityChangedListener((start, end) -> {
                        })
                        .addItem(row)
                        .build();
        assertThat(itemList)
                .isEqualTo(
                        builder()
                                .setOnSelectedListener((index) -> {
                                })
                                .setNoItemsMessage("no items")
                                .setSelectedIndex(0)
                                .setOnItemsVisibilityChangedListener((start, end) -> {
                                })
                                .addItem(row)
                                .build());
    }

    @Test
    public void equals_itemListWithGridItems() {
        GridItem gridItem = GridItem.builder().setImage(BACK).setTitle("Title").build();
        ItemList itemList =
                builder()
                        .setOnSelectedListener((index) -> {
                        })
                        .setNoItemsMessage("no items")
                        .setSelectedIndex(0)
                        .setOnItemsVisibilityChangedListener((start, end) -> {
                        })
                        .addItem(gridItem)
                        .build();
        assertThat(itemList)
                .isEqualTo(
                        builder()
                                .setOnSelectedListener((index) -> {
                                })
                                .setNoItemsMessage("no items")
                                .setSelectedIndex(0)
                                .setOnItemsVisibilityChangedListener((start, end) -> {
                                })
                                .addItem(gridItem)
                                .build());
    }

    @Test
    public void notEquals_differentNoItemsMessage() {
        ItemList itemList = builder().setNoItemsMessage("no items").build();
        assertThat(itemList).isNotEqualTo(builder().setNoItemsMessage("YO").build());
    }

    @Test
    public void notEquals_differentSelectedIndex() {
        Row row = Row.builder().setTitle("Title").build();
        ItemList itemList =
                builder().setOnSelectedListener((index) -> {
                }).addItem(row).addItem(row).build();
        assertThat(itemList)
                .isNotEqualTo(
                        builder()
                                .setOnSelectedListener((index) -> {
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
                builder().setOnSelectedListener((index) -> {
                }).addItem(row).addItem(row).build();
        assertThat(itemList).isNotEqualTo(builder().addItem(row).addItem(row).build());
    }

    @Test
    public void notEquals_missingVisibilityChangedListener() {
        Row row = Row.builder().setTitle("Title").build();
        ItemList itemList =
                builder()
                        .setOnItemsVisibilityChangedListener((start, end) -> {
                        })
                        .addItem(row)
                        .addItem(row)
                        .build();
        assertThat(itemList).isNotEqualTo(builder().addItem(row).addItem(row).build());
    }

    @Test
    public void notEquals_differentRows() {
        Row row = Row.builder().setTitle("Title").build();
        ItemList itemList = builder().addItem(row).addItem(row).build();
        assertThat(itemList).isNotEqualTo(builder().addItem(row).build());
    }

    @Test
    public void notEquals_differentGridItems() {
        GridItem gridItem = GridItem.builder().setImage(BACK).setTitle("Title").build();
        ItemList itemList = builder().addItem(gridItem).addItem(gridItem).build();
        assertThat(itemList).isNotEqualTo(builder().addItem(gridItem).build());
    }
}
