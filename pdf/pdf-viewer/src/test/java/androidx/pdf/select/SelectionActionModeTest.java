/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.select;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.pdf.models.PageSelection;
import androidx.pdf.util.ObservableValue;
import androidx.pdf.util.Observables;
import androidx.pdf.viewer.PageMosaicView;
import androidx.pdf.viewer.PageViewFactory;
import androidx.pdf.viewer.PaginatedView;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


public class SelectionActionModeTest {

    @Mock
    private Context mContext;

    private SelectionActionMode mSelectionActionMode;

    @Mock
    private SelectionModel<PageSelection> mSelectionModel;

    @Mock
    private PaginatedView mPaginatedView;

    @Mock
    private PageMosaicView mPageMosaicView;

    @Mock
    private PageSelection mPageSelection;

    @Mock
    Observables.ExposedValue<PageSelection> mSelection;

    @Mock
    PageViewFactory.PageView mPageView;


    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testStartActionMode() {
        SelectionModel<PageSelection> selectionModel = new SelectionModel<PageSelection>() {

            @NonNull
            @Override
            public ObservableValue<PageSelection> selection() {
                return new ObservableValue<PageSelection>() {
                    @Nullable
                    @Override
                    public PageSelection get() {
                        return mPageSelection;
                    }

                    @NonNull
                    @Override
                    public Object addObserver(ValueObserver<PageSelection> observer) {
                        observer.onChange(null, mPageSelection);
                        return observer;
                    }

                    @Override
                    public void removeObserver(@NonNull Object observerKey) {

                    }
                };
            }
            @NonNull
            @Override
            public String getText() {
                return "";
            }

        };

        selectionModel.setSelection(mPageSelection);

        when(mPaginatedView.getViewAt(anyInt())).thenReturn(mPageView);
        when(mPageView.getPageView()).thenReturn(mPageMosaicView);

        mSelectionActionMode = new SelectionActionMode(mContext, mPaginatedView, selectionModel);

        verify(mPaginatedView).getViewAt(anyInt());
        verify(mPageMosaicView).startActionMode(any(), anyInt());
    }

    @Test
    public void testDestroyRemoveObserver() {
        when(mSelectionModel.selection()).thenReturn(mSelection);

        mSelectionActionMode = new SelectionActionMode(mContext, mPaginatedView, mSelectionModel);

        mSelectionActionMode.destroy();

        verify(mSelectionModel.selection()).removeObserver(any());

    }

}

