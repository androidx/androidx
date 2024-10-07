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

package androidx.pdf.viewer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.pdf.ViewState;
import androidx.pdf.data.Range;
import androidx.pdf.find.FindInFileView;
import androidx.pdf.models.PageSelection;
import androidx.pdf.models.SelectionBoundary;
import androidx.pdf.select.SelectionActionMode;
import androidx.pdf.util.ObservableValue;
import androidx.pdf.util.Observables;
import androidx.pdf.widget.ZoomView;
import androidx.test.filters.SmallTest;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


@SmallTest
@RunWith(RobolectricTestRunner.class)
//TODO: Remove minsdk check after sdk extension 13 release
@Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class ZoomScrollValueObserverTest {
    private static final ObservableValue<ViewState>
            VIEW_STATE_EXPOSED_VALUE =
            Observables.newExposedValueWithInitialValue(ViewState.NO_VIEW);
    private static final Rect RECT = new Rect(0, 0, 100, 100);
    private static final ZoomView.ZoomScroll OLD_POSITION = new ZoomView.ZoomScroll(1.0f, 0, 0,
            false);
    private static final Range PAGE_RANGE = new Range(0, 100);
    private static final int ANIMATION_DELAY_MILLIS = 200;

    private final PaginatedView mMockPaginatedView = mock(PaginatedView.class);
    private final ZoomView mMockZoomView = mock(ZoomView.class);
    private final PaginationModel mMockPaginationModel = mock(PaginationModel.class);
    private final LayoutHandler mMockLayoutHandler = mock(LayoutHandler.class);
    private final FloatingActionButton mMockAnnotationButton = mock(FloatingActionButton.class);
    private final FindInFileView mMockFindInFileView = mock(FindInFileView.class);
    private final PageRangeHandler mPageRangeHandler = mock(PageRangeHandler.class);
    private final SelectionActionMode mMockSelectionActionMode = mock(SelectionActionMode.class);
    private final PdfSelectionModel mMockSelectionModel = mock(PdfSelectionModel.class);
    private final PageSelection mMockPageSelection = mock(PageSelection.class);
    private final ImmersiveModeRequester mMockImmersiveModeRequester = mock(
            ImmersiveModeRequester.class);
    private boolean mIsAnnotationIntentResolvable;
    private ZoomView.ZoomScroll mNewPosition;

    @Before
    public void setUp() {
        mIsAnnotationIntentResolvable = false;
        mNewPosition = new ZoomView.ZoomScroll(1.0f, 0, 0, false);

        when(mMockPaginatedView.getPageRangeHandler()).thenReturn(mPageRangeHandler);
        when(mMockPaginatedView.getModel()).thenReturn(mMockPaginationModel);
        when(mMockPaginationModel.isInitialized()).thenReturn(true);
        when(mPageRangeHandler.computeVisibleRange(0, 1.0f, 100, false)).thenReturn(PAGE_RANGE);
        when(mMockPaginatedView.createPageViewsForVisiblePageRange()).thenReturn(false);
        when(mPageRangeHandler.getVisiblePages()).thenReturn(PAGE_RANGE);
        when(mMockPaginationModel.isInitialized()).thenReturn(true);
        when(mMockPaginationModel.getSize()).thenReturn(1);
        when(mMockPaginatedView.getSelectionModel()).thenReturn(mMockSelectionModel);
        when(mMockSelectionModel.selection()).thenReturn(new ObservableValue<PageSelection>() {
            @Nullable
            @Override
            public PageSelection get() {
                return mMockPageSelection;
            }

            @NonNull
            @Override
            public Object addObserver(ValueObserver<PageSelection> observer) {
                return 1;
            }

            @Override
            public void removeObserver(@NonNull Object observerKey) {

            }
        });
        when(mMockPageSelection.getStart()).thenReturn(new SelectionBoundary(0, 0, 0, false));
        when(mMockPageSelection.getStop()).thenReturn(new SelectionBoundary(0, 100, 100, false));
        when(mMockPaginatedView.getViewArea()).thenReturn(RECT);
        when(mMockPaginationModel.getLookAtX(0, 0)).thenReturn(1);
        when(mMockPaginationModel.getLookAtX(0, 100)).thenReturn(50);
        when(mMockPaginationModel.getLookAtY(0, 0)).thenReturn(1);
        when(mMockPaginationModel.getLookAtY(0, 100)).thenReturn(50);
    }

    @Test
    public void onChange_stablePosition() {
        mNewPosition = new ZoomView.ZoomScroll(1.0f, 0, 0, true);

        ZoomScrollValueObserver zoomScrollValueObserver = new ZoomScrollValueObserver(mMockZoomView,
                mMockPaginatedView, mMockLayoutHandler, mMockAnnotationButton,
                mMockFindInFileView, mIsAnnotationIntentResolvable, mMockSelectionActionMode,
                VIEW_STATE_EXPOSED_VALUE, mMockImmersiveModeRequester);
        zoomScrollValueObserver.onChange(OLD_POSITION, mNewPosition);

        verify(mMockSelectionActionMode).resume();
    }

    @Test
    public void onChange_exitImmersiveMode() {
        mIsAnnotationIntentResolvable = true;
        when(mMockAnnotationButton.getVisibility()).thenReturn(View.GONE);
        when(mMockFindInFileView.getVisibility()).thenReturn(View.GONE);

        ZoomScrollValueObserver zoomScrollValueObserver = new ZoomScrollValueObserver(mMockZoomView,
                mMockPaginatedView, mMockLayoutHandler, mMockAnnotationButton,
                mMockFindInFileView,
                mIsAnnotationIntentResolvable, mMockSelectionActionMode,
                VIEW_STATE_EXPOSED_VALUE, mMockImmersiveModeRequester);
        zoomScrollValueObserver.onChange(OLD_POSITION, mNewPosition);

        verify(mMockImmersiveModeRequester).requestImmersiveModeChange(false);
    }

    @Test
    public void onChange_enterImmersiveMode() {
        mIsAnnotationIntentResolvable = true;
        mNewPosition = new ZoomView.ZoomScroll(1.0f, 0, 10, false);
        ZoomView.ZoomScroll oldPosition = new ZoomView.ZoomScroll(1.0f, 0, 10, false);
        when(mMockAnnotationButton.getVisibility()).thenReturn(View.VISIBLE);

        ZoomScrollValueObserver zoomScrollValueObserver = new ZoomScrollValueObserver(mMockZoomView,
                mMockPaginatedView, mMockLayoutHandler, mMockAnnotationButton,
                mMockFindInFileView,
                mIsAnnotationIntentResolvable, mMockSelectionActionMode,
                VIEW_STATE_EXPOSED_VALUE, mMockImmersiveModeRequester);
        zoomScrollValueObserver.onChange(oldPosition, mNewPosition);
//        TODO: Remove this hardcode dependency.
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> verify(mMockImmersiveModeRequester).requestImmersiveModeChange(true),
                ANIMATION_DELAY_MILLIS);
    }

}
