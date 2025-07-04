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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.ActionMode;

import androidx.pdf.models.PageSelection;
import androidx.pdf.util.ObservableValue;
import androidx.pdf.util.Observables;
import androidx.pdf.viewer.PaginatedView;
import androidx.pdf.widget.ZoomView;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SelectionActionModeTest {

    @Mock
    private Context mContext;

    private SelectionActionMode mSelectionActionMode;

    @Mock
    private SelectionModel<PageSelection> mSelectionModel;

    @Mock
    private PaginatedView mPaginatedView;

    @Mock
    private PageSelection mPageSelection;

    @Mock
    private Observables.ExposedValue<PageSelection> mSelection;

    @Mock
    private ZoomView mZoomView;


    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testStartActionMode() {
        when(mZoomView.post(any())).thenAnswer(invocation -> {
            Runnable action = invocation.getArgument(0);
            action.run();
            return true;
        });
        SelectionModel<PageSelection> selectionModel = new SelectionModel<PageSelection>() {

            @Override
            public @NonNull ObservableValue<PageSelection> selection() {
                return new ObservableValue<PageSelection>() {
                    @Override
                    public @Nullable PageSelection get() {
                        return mPageSelection;
                    }

                    @Override
                    public @NonNull Object addObserver(ValueObserver<PageSelection> observer) {
                        observer.onChange(null, mPageSelection);
                        return observer;
                    }

                    @Override
                    public void removeObserver(@NonNull Object observerKey) {

                    }
                };
            }

            @Override
            public @NonNull String getText() {
                return "";
            }

        };

        selectionModel.setSelection(mPageSelection);

        mSelectionActionMode = new SelectionActionMode(mContext, mPaginatedView, mZoomView,
                selectionModel);

        verify(mZoomView).startActionMode(any(), eq(ActionMode.TYPE_FLOATING));
    }

    @Test
    public void testDestroyRemoveObserver() {
        when(mSelectionModel.selection()).thenReturn(mSelection);

        mSelectionActionMode = new SelectionActionMode(mContext, mPaginatedView, mZoomView,
                mSelectionModel);

        mSelectionActionMode.destroy();

        verify(mSelectionModel.selection()).removeObserver(any());

    }

}

