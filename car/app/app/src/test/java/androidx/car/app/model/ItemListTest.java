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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.os.RemoteException;

import androidx.car.app.IOnDoneCallback;
import androidx.car.app.OnDoneCallback;
import androidx.car.app.model.ItemList.OnItemVisibilityChangedListener;
import androidx.car.app.model.ItemList.OnSelectedListener;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Collections;

/** Tests for {@link ItemListTest}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
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
        ItemList list = new ItemList.Builder().build();
        assertThat(list.getItems()).isEqualTo(Collections.emptyList());
    }

    @Test
    public void createRows() {
        Row row1 = new Row.Builder().setTitle("Row1").build();
        Row row2 = new Row.Builder().setTitle("Row2").build();
        ItemList list = new ItemList.Builder().addItem(row1).addItem(row2).build();

        assertThat(list.getItems()).hasSize(2);
        assertThat(list.getItems().get(0)).isEqualTo(row1);
        assertThat(list.getItems().get(1)).isEqualTo(row2);
    }

    @Test
    public void createGridItems() {
        GridItem gridItem1 = new GridItem.Builder().setTitle("title 1").setImage(BACK).build();
        GridItem gridItem2 = new GridItem.Builder().setTitle("title 2").setImage(BACK).build();
        ItemList list = new ItemList.Builder().addItem(gridItem1).addItem(gridItem2).build();

        assertThat(list.getItems()).containsExactly(gridItem1, gridItem2).inOrder();
    }

    @Test
    public void setSelectedable_emptyList_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new ItemList.Builder().setOnSelectedListener(selectedIndex -> {
                }).build());
    }

    @Test
    public void setSelectedIndex_greaterThanListSize_throws() {
        Row row1 = new Row.Builder().setTitle("Row1").build();
        assertThrows(
                IllegalStateException.class,
                () -> new ItemList.Builder()
                        .addItem(row1)
                        .setOnSelectedListener(selectedIndex -> {
                        })
                        .setSelectedIndex(2)
                        .build());
    }

    @Test
    public void setSelectable() throws RemoteException {
        OnSelectedListener mockListener = mock(OnSelectedListener.class);
        ItemList itemList =
                new ItemList.Builder()
                        .addItem(new Row.Builder().setTitle("title").build())
                        .setOnSelectedListener(mockListener)
                        .build();

        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);


        itemList.getOnSelectedDelegate().sendSelected(0, onDoneCallback);
        verify(mockListener).onSelected(eq(0));
        verify(onDoneCallback).onSuccess(null);
    }

    @Test
    public void setSelectable_disallowOnClickListenerInRows() {
        assertThrows(
                IllegalStateException.class,
                () -> new ItemList.Builder()
                        .addItem(new Row.Builder().setTitle("foo").setOnClickListener(() -> {
                        }).build())
                        .setOnSelectedListener((index) -> {
                        })
                        .build());

        // Positive test.
        new ItemList.Builder()
                .addItem(new Row.Builder().setTitle("foo").build())
                .setOnSelectedListener((index) -> {
                })
                .build();
    }

    @Test
    public void setSelectable_disallowToggleInRow() {
        assertThrows(
                IllegalStateException.class,
                () -> new ItemList.Builder()
                        .addItem(new Row.Builder().setToggle(new Toggle.Builder(isChecked -> {
                        }).build()).build())
                        .setOnSelectedListener((index) -> {
                        })
                        .build());
    }

    @Test
    public void setOnItemVisibilityChangeListener_triggerListener() {
        OnItemVisibilityChangedListener listener = mock(OnItemVisibilityChangedListener.class);
        ItemList list =
                new ItemList.Builder()
                        .addItem(new Row.Builder().setTitle("1").build())
                        .setOnItemsVisibilityChangedListener(listener)
                        .build();

        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);
        list.getOnItemVisibilityChangedDelegate().sendItemVisibilityChanged(0, 1,
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
    public void setOnItemVisibilityChangeListener_triggerListenerWithFailure() {
        OnItemVisibilityChangedListener listener = mock(OnItemVisibilityChangedListener.class);
        ItemList list =
                new ItemList.Builder()
                        .addItem(new Row.Builder().setTitle("1").build())
                        .setOnItemsVisibilityChangedListener(listener)
                        .build();

        String testExceptionMessage = "Test exception";
        doThrow(new RuntimeException(testExceptionMessage)).when(listener).onItemVisibilityChanged(
                0, 1);

        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);
        try {
            list.getOnItemVisibilityChangedDelegate().sendItemVisibilityChanged(0, 1,
                    onDoneCallback);
        } catch (RuntimeException e) {
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
        Row row = new Row.Builder().setTitle("Title").build();
        ItemList itemList =
                new ItemList.Builder()
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
                        new ItemList.Builder()
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
        GridItem gridItem = new GridItem.Builder().setImage(BACK).setTitle("Title").build();
        ItemList itemList =
                new ItemList.Builder()
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
                        new ItemList.Builder()
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
        ItemList itemList = new ItemList.Builder().setNoItemsMessage("no items").build();
        assertThat(itemList).isNotEqualTo(new ItemList.Builder().setNoItemsMessage("YO").build());
    }

    @Test
    public void notEquals_differentSelectedIndex() {
        Row row = new Row.Builder().setTitle("Title").build();
        ItemList itemList =
                new ItemList.Builder().setOnSelectedListener((index) -> {
                }).addItem(row).addItem(row).build();
        assertThat(itemList)
                .isNotEqualTo(
                        new ItemList.Builder()
                                .setOnSelectedListener((index) -> {
                                })
                                .setSelectedIndex(1)
                                .addItem(row)
                                .addItem(row)
                                .build());
    }

    @Test
    public void notEquals_missingSelectedListener() {
        Row row = new Row.Builder().setTitle("Title").build();
        ItemList itemList =
                new ItemList.Builder().setOnSelectedListener((index) -> {
                }).addItem(row).addItem(row).build();
        assertThat(itemList).isNotEqualTo(new ItemList.Builder().addItem(row).addItem(row).build());
    }

    @Test
    public void notEquals_missingVisibilityChangedListener() {
        Row row = new Row.Builder().setTitle("Title").build();
        ItemList itemList =
                new ItemList.Builder()
                        .setOnItemsVisibilityChangedListener((start, end) -> {
                        })
                        .addItem(row)
                        .addItem(row)
                        .build();
        assertThat(itemList).isNotEqualTo(new ItemList.Builder().addItem(row).addItem(row).build());
    }

    @Test
    public void notEquals_differentRows() {
        Row row = new Row.Builder().setTitle("Title").build();
        ItemList itemList = new ItemList.Builder().addItem(row).addItem(row).build();
        assertThat(itemList).isNotEqualTo(new ItemList.Builder().addItem(row).build());
    }

    @Test
    public void notEquals_differentGridItems() {
        GridItem gridItem = new GridItem.Builder().setImage(BACK).setTitle("Title").build();
        ItemList itemList = new ItemList.Builder().addItem(gridItem).addItem(gridItem).build();
        assertThat(itemList).isNotEqualTo(new ItemList.Builder().addItem(gridItem).build());
    }
}
