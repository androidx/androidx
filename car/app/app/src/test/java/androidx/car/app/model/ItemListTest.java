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

import androidx.car.app.OnDoneCallback;
import androidx.car.app.model.ItemList.OnItemVisibilityChangedListener;
import androidx.car.app.model.ItemList.OnSelectedListener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Collections;

/** Tests for {@link ItemListTest}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ItemListTest {
    private static final Row row1 = new Row.Builder().setTitle("Row1").build();
    private static final Row row2 = new Row.Builder().setTitle("Row2").build();
    private static final GridItem gridItem1 = new GridItem.Builder().setTitle("title 1").setImage(BACK).build();
    private static final GridItem gridItem2 = new GridItem.Builder().setTitle("title 2").setImage(BACK).build();
    private final OnSelectedListener mockSelectedListener = mock(OnSelectedListener.class);
    private final OnItemVisibilityChangedListener mockItemVisibilityChangedListener = mock(OnItemVisibilityChangedListener.class);
    OnDoneCallback mockOnDoneCallback = mock(OnDoneCallback.class);

    @Test
    public void createEmpty() {
        ItemList list = new ItemList.Builder().build();
        assertThat(list.getItems()).isEqualTo(Collections.emptyList());
    }

    @Test
    public void createRows() {
        ItemList list = new ItemList.Builder().addItem(row1).addItem(row2).build();

        assertThat(list.getItems()).containsExactly(row1, row2).inOrder();
    }

    @Test
    public void createGridItems() {
        ItemList list = new ItemList.Builder().addItem(gridItem1).addItem(gridItem2).build();

        assertThat(list.getItems()).containsExactly(gridItem1, gridItem2).inOrder();
    }

    @Test
    public void clearItems() {
        ItemList list = new ItemList.Builder()
                .addItem(row1)
                .clearItems()
                .addItem(row2)
                .build();

        assertThat(list.getItems()).containsExactly(row2).inOrder();
    }

    @Test
    public void setSelectedable_emptyList_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new ItemList.Builder().setOnSelectedListener(mockSelectedListener).build());
    }

    @Test
    public void setSelectedIndex_greaterThanListSize_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new ItemList.Builder()
                        .addItem(row1)
                        .setOnSelectedListener(mockSelectedListener)
                        .setSelectedIndex(2)
                        .build());
    }

    @Test
    public void setSelectable() throws RemoteException {
        ItemList itemList =
                new ItemList.Builder()
                        .addItem(row1)
                        .setOnSelectedListener(mockSelectedListener)
                        .build();


        itemList.getOnSelectedDelegate().sendSelected(0, mockOnDoneCallback);
        verify(mockSelectedListener).onSelected(eq(0));
        verify(mockOnDoneCallback).onSuccess(null);
    }

    @Test
    public void setSelectable_disallowOnClickListenerInRows() {
        assertThrows(
                IllegalStateException.class,
                () -> new ItemList.Builder()
                        .addItem(new Row.Builder().setTitle("foo").setOnClickListener(() -> {
                        }).build())
                        .setOnSelectedListener(mockSelectedListener)
                        .build());

        // Positive test.
        new ItemList.Builder()
                .addItem(new Row.Builder().setTitle("foo").build())
                .setOnSelectedListener(mockSelectedListener)
                .build();
    }

    @Test
    public void setSelectable_disallowToggleInRow() {
        assertThrows(
                IllegalStateException.class,
                () -> new ItemList.Builder()
                        .addItem(new Row.Builder().setToggle(new Toggle.Builder(isChecked -> {
                        }).build()).build())
                        .setOnSelectedListener(mockSelectedListener)
                        .build());
    }

    @Test
    public void setOnItemVisibilityChangeListener_triggerListener() {
        ItemList list =
                new ItemList.Builder()
                        .addItem(row1)
                        .setOnItemsVisibilityChangedListener(mockItemVisibilityChangedListener)
                        .build();

        list.getOnItemVisibilityChangedDelegate().sendItemVisibilityChanged(0, 1,
                mockOnDoneCallback);
        ArgumentCaptor<Integer> startIndexCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> endIndexCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockItemVisibilityChangedListener).onItemVisibilityChanged(startIndexCaptor.capture(),
                endIndexCaptor.capture());
        verify(mockOnDoneCallback).onSuccess(null);
        assertThat(startIndexCaptor.getValue()).isEqualTo(0);
        assertThat(endIndexCaptor.getValue()).isEqualTo(1);
    }

    @Test
    public void setOnItemVisibilityChangeListener_triggerListenerWithFailure() {
        ItemList list =
                new ItemList.Builder()
                        .addItem(row1)
                        .setOnItemsVisibilityChangedListener(mockItemVisibilityChangedListener)
                        .build();

        String testExceptionMessage = "Test exception";
        doThrow(new RuntimeException(testExceptionMessage)).when(mockItemVisibilityChangedListener).onItemVisibilityChanged(
                0, 1);

        try {
            list.getOnItemVisibilityChangedDelegate().sendItemVisibilityChanged(0, 1,
                    mockOnDoneCallback);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains(testExceptionMessage);
        }

        ArgumentCaptor<Integer> startIndexCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> endIndexCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockItemVisibilityChangedListener).onItemVisibilityChanged(startIndexCaptor.capture(),
                endIndexCaptor.capture());
        verify(mockOnDoneCallback).onFailure(any());
        assertThat(startIndexCaptor.getValue()).isEqualTo(0);
        assertThat(endIndexCaptor.getValue()).isEqualTo(1);
    }

    @Test
    public void equals_itemListWithRows() {
        assertThat(createFullyPopulatedRowItemList()).isEqualTo(createFullyPopulatedRowItemList());
    }

    @Test
    public void equals_itemListWithGridItems() {
        assertThat(createFullyPopulatedGridItemList()).isEqualTo(createFullyPopulatedGridItemList());
    }

    @Test
    public void notEquals_differentNoItemsMessage() {
        ItemList itemList = new ItemList.Builder().setNoItemsMessage("no items").build();
        assertThat(itemList).isNotEqualTo(new ItemList.Builder().setNoItemsMessage("YO").build());
    }

    @Test
    public void notEquals_differentSelectedIndex() {
        ItemList itemList = new ItemList.Builder()
                .setOnSelectedListener(mockSelectedListener)
                .addItem(row1)
                .addItem(row2)
                .build();
        assertThat(itemList)
                .isNotEqualTo(
                        new ItemList.Builder()
                                .setOnSelectedListener(mockSelectedListener)
                                .setSelectedIndex(1)
                                .addItem(row1)
                                .addItem(row2)
                                .build());
    }

    @Test
    public void notEquals_missingSelectedListener() {
        ItemList itemList = new ItemList.Builder()
                .setOnSelectedListener(mockSelectedListener)
                .addItem(row1)
                .addItem(row2)
                .build();
        assertThat(itemList).isNotEqualTo(new ItemList.Builder().addItem(row1).addItem(row2).build());
    }

    @Test
    public void notEquals_missingVisibilityChangedListener() {
        ItemList itemList =
                new ItemList.Builder()
                        .setOnItemsVisibilityChangedListener(mockItemVisibilityChangedListener)
                        .addItem(row1)
                        .addItem(row2)
                        .build();
        assertThat(itemList).isNotEqualTo(new ItemList.Builder().addItem(row1).addItem(row2).build());
    }

    @Test
    public void notEquals_differentRows() {
        ItemList itemList = new ItemList.Builder().addItem(row1).addItem(row1).build();
        assertThat(itemList).isNotEqualTo(new ItemList.Builder().addItem(row1).build());
    }

    @Test
    public void notEquals_differentGridItems() {
        GridItem gridItem = new GridItem.Builder().setImage(BACK).setTitle("Title").build();
        ItemList itemList = new ItemList.Builder().addItem(gridItem).addItem(gridItem).build();
        assertThat(itemList).isNotEqualTo(new ItemList.Builder().addItem(gridItem).build());
    }

    @Test
    public void equals_delegateVsCallback() {
        ItemList itemListWithListeners = new ItemList.Builder().addItem(row1)
                .setOnItemsVisibilityChangedListener(mockItemVisibilityChangedListener)
                .setOnSelectedListener(mockSelectedListener)
                .build();
        ItemList itemListWithDelegates = new ItemList.Builder().addItem(row1)
                .setOnItemsVisibilityChangedDelegate(OnItemVisibilityChangedDelegateImpl.create(mockItemVisibilityChangedListener))
                .setOnSelectedDelegate(OnSelectedDelegateImpl.create(mockSelectedListener))
                .build();

        assertThat(itemListWithListeners).isEqualTo(itemListWithDelegates);
    }

    @Test
    public void toBuilder_createsEquivalentInstance_rows() {
        ItemList itemList = createFullyPopulatedRowItemList();

        assertThat(itemList).isEqualTo(itemList.toBuilder().build());
    }

    @Test
    public void toBuilder_createsEquivalentInstance_grid() {
        ItemList itemList = createFullyPopulatedGridItemList();

        assertThat(itemList).isEqualTo(itemList.toBuilder().build());
    }

    @Test
    public void toBuilder_fieldsCanBeOverwritten() {
        Row row = new Row.Builder().setTitle("Title").build();
        ItemList itemList = new ItemList.Builder()
                .setOnSelectedListener(mockSelectedListener)
                .setNoItemsMessage("no items")
                .setSelectedIndex(0)
                .setOnItemsVisibilityChangedListener(mockItemVisibilityChangedListener)
                .addItem(row)
                .build();

        // Verify fields can be overwritten (no crash)
        itemList.toBuilder()
                .setOnSelectedListener(mockSelectedListener)
                .setNoItemsMessage("no items")
                .setSelectedIndex(0)
                .setOnItemsVisibilityChangedListener(mockItemVisibilityChangedListener)
                .clearItems()
                .addItem(row)
                .build();
    }

    private ItemList createFullyPopulatedRowItemList() {
        return new ItemList.Builder()
                .setOnSelectedListener(mockSelectedListener)
                .setNoItemsMessage("no items")
                .setSelectedIndex(0)
                .setOnItemsVisibilityChangedListener(mockItemVisibilityChangedListener)
                .addItem(row1)
                .build();
    }

    private ItemList createFullyPopulatedGridItemList() {
        return new ItemList.Builder()
                .setOnSelectedListener(mockSelectedListener)
                .setNoItemsMessage("no items")
                .setSelectedIndex(0)
                .setOnItemsVisibilityChangedListener(mockItemVisibilityChangedListener)
                .addItem(gridItem1)
                .build();
    }
}
