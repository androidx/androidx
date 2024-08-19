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

import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.ViewState;
import androidx.pdf.find.FindInFileView;
import androidx.pdf.select.SelectionActionMode;
import androidx.pdf.util.ObservableValue;
import androidx.pdf.widget.ZoomView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ZoomScrollValueObserver implements ObservableValue.ValueObserver<ZoomView.ZoomScroll> {
    private final PaginatedView mPaginatedView;
    private final ZoomView mZoomView;
    private final LayoutHandler mLayoutHandler;
    private final FloatingActionButton mAnnotationButton;
    private final Handler mAnnotationButtonHandler;
    private final FindInFileView mFindInFileView;
    private boolean mIsAnnotationIntentResolvable;
    private final SelectionActionMode mSelectionActionMode;
    private final ObservableValue<ViewState> mViewState;

    private static final int FAB_ANIMATION_DURATION = 200;
    private boolean mIsPageScrollingUp;

    public ZoomScrollValueObserver(@Nullable ZoomView zoomView,
            @Nullable PaginatedView paginatedView,
            @NonNull LayoutHandler layoutHandler, @NonNull FloatingActionButton annotationButton,
            @NonNull FindInFileView findInFileView, boolean isAnnotationIntentResolvable,
            @NonNull SelectionActionMode selectionActionMode,
            @NonNull ObservableValue<ViewState> viewState) {
        mZoomView = zoomView;
        mPaginatedView = paginatedView;
        mLayoutHandler = layoutHandler;
        mAnnotationButton = annotationButton;
        mFindInFileView = findInFileView;
        mIsAnnotationIntentResolvable = isAnnotationIntentResolvable;
        mSelectionActionMode = selectionActionMode;
        mViewState = viewState;
        mAnnotationButtonHandler = new Handler(Looper.getMainLooper());
        mIsPageScrollingUp = false;
    }

    @Override
    public void onChange(@Nullable ZoomView.ZoomScroll oldPosition,
            @Nullable ZoomView.ZoomScroll position) {
        if (mPaginatedView == null || !mPaginatedView.getPaginationModel().isInitialized()
                || position == null || mPaginatedView.getPaginationModel().getSize() == 0) {
            return;
        }
        // Stop showing context menu if there is any change in zoom or scroll, resume only when
        // the new position is stable
        mSelectionActionMode.stopActionMode();
        mZoomView.loadPageAssets(mLayoutHandler, mViewState);

        if (oldPosition.scrollY > position.scrollY) {
            mIsPageScrollingUp = true;
        } else if (oldPosition.scrollY < position.scrollY) {
            mIsPageScrollingUp = false;
        }

        if (mIsAnnotationIntentResolvable && !mPaginatedView.isConfigurationChanged()) {

            if (!isAnnotationButtonVisible() && position.scrollY == 0
                    && mFindInFileView.getVisibility() == View.GONE) {
                editFabExpandAnimation();
            } else if (isAnnotationButtonVisible() && mIsPageScrollingUp) {
                clearAnnotationHandler();
                return;
            }
            if (position.scrollY == oldPosition.scrollY) {
                mAnnotationButtonHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (position.scrollY != 0) {
                            mAnnotationButton.animate()
                                    .alpha(0.0f)
                                    .setDuration(FAB_ANIMATION_DURATION)
                                    .withEndAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            mAnnotationButton.setVisibility(View.GONE);
                                            mAnnotationButton.setAlpha(1.0f);
                                        }
                                    });
                        }
                    }
                });
            }
        } else if (mPaginatedView.isConfigurationChanged()
                && position.scrollY != oldPosition.scrollY) {
            mPaginatedView.setConfigurationChanged(false);
        }

        if (mPaginatedView.getSelectionModel().selection().get() != null && position.stable) {
            setUpContextMenu();
        }
    }

    private boolean isAnnotationButtonVisible() {
        return mAnnotationButton.getVisibility() == View.VISIBLE;
    }

    private void editFabExpandAnimation() {
        mAnnotationButton.setScaleX(0.0f);
        mAnnotationButton.setScaleY(0.0f);
        ValueAnimator scaleAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        scaleAnimator.setDuration(FAB_ANIMATION_DURATION);
        scaleAnimator.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(@NonNull ValueAnimator animation) {
                        float scale = (float) animation.getAnimatedValue();
                        mAnnotationButton.setScaleX(scale);
                        mAnnotationButton.setScaleY(scale);
                        mAnnotationButton.setAlpha(scale);
                    }
                });
        scaleAnimator.start();
        mAnnotationButton.setVisibility(View.VISIBLE);
    }

    /** Exposing a function to clear the handler when PDFViewer Fragment is destroyed. */
    public void clearAnnotationHandler() {
        mAnnotationButtonHandler.removeCallbacksAndMessages(null);
    }

    public void setAnnotationIntentResolvable(boolean annotationIntentResolvable) {
        mIsAnnotationIntentResolvable = annotationIntentResolvable;
    }

    private void setUpContextMenu() {
        // Resume the context menu if selected area is on the current viewing screen
        if (mPaginatedView.getSelectionModel().getPage() != -1) {
            int selectionPage = mPaginatedView.getSelectionModel().getPage();
            int firstPageInVisibleRange =
                    mPaginatedView.getPageRangeHandler().getVisiblePages().getFirst();
            int lastPageInVisisbleRange =
                    mPaginatedView.getPageRangeHandler().getVisiblePages().getLast();

            // If selection is within the range of visible pages
            if (selectionPage >= firstPageInVisibleRange
                    && selectionPage <= lastPageInVisisbleRange) {
                // Start and stop coordinates in a page wrt pagination model
                int startX = mPaginatedView.getPaginationModel().getLookAtX(selectionPage,
                        mPaginatedView.getSelectionModel().selection().get().getStart().getX());
                int startY = mPaginatedView.getPaginationModel().getLookAtY(selectionPage,
                        mPaginatedView.getSelectionModel().selection().get().getStart().getY());
                int stopX = mPaginatedView.getPaginationModel().getLookAtX(selectionPage,
                        mPaginatedView.getSelectionModel().selection().get().getStop().getX());
                int stopY = mPaginatedView.getPaginationModel().getLookAtY(selectionPage,
                        mPaginatedView.getSelectionModel().selection().get().getStop().getY());

                Rect currentViewArea = mPaginatedView.getPaginationModel().getViewArea();

                if (currentViewArea.intersect(startX, startY, stopX, stopY)) {
                    mSelectionActionMode.resume();
                }
            }
        }
    }
}
