/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.car.app;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.car.R;
import androidx.car.widget.PagedListView;
import androidx.car.widget.PagedScrollBarView;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Utility class that helps configure car dialogs.
 */
class CarDialogUtil {
    private static final String TAG = "CarDialogUtil";

    /**
     * Private constructor to prevent instantiation.
     */
    private CarDialogUtil() {
    }

    /**
     * Returns the style that has been assigned to {@code carDialogTheme} for the given
     * {@link Context}. If a style has not been defined, a default style will be returned.
     */
    @StyleRes
    static int getDialogTheme(@NonNull Context context) {
        TypedValue outValue = new TypedValue();
        boolean hasStyle =
                context.getTheme().resolveAttribute(R.attr.carDialogTheme, outValue, true);
        return hasStyle ? outValue.resourceId : R.style.Theme_Car_Dark_Dialog;
    }

    /**
     * Configures the scrollbar that appears off the dialog. This scrollbar is not the one that
     * usually appears with the PagedListView, but mimics it in functionality. This method should
     * only be invoked once when performing the initial layout setup for a dialog list.
     */
    static void setUpDialogList(@NonNull PagedListView list,
            @NonNull PagedScrollBarView scrollBarView) {
        list.getRecyclerView().addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                updateDialogListScrollbar(list, scrollBarView);
            }
        });

        setUpDialogListPagination(list, scrollBarView);

        list.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        updateDialogListScrollbar(list, scrollBarView);
                        // Remove this listener because the listener for the scroll state will be
                        // enough to keep the scrollbar in sync.
                        list.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
    }

    private static void setUpDialogListPagination(@NonNull PagedListView list,
            @NonNull PagedScrollBarView scrollBarView) {
        scrollBarView.setPaginationListener(new PagedScrollBarView.PaginationListener() {
            @Override
            public void onPaginate(int direction) {
                switch (direction) {
                    case PagedScrollBarView.PaginationListener.PAGE_UP:
                        list.pageUp();
                        break;
                    case PagedScrollBarView.PaginationListener.PAGE_DOWN:
                        list.pageDown();
                        break;
                    default:
                        Log.e(TAG, "Unknown pagination direction (" + direction + ")");
                }
            }

            @Override
            public void onAlphaJump() {
            }
        });
    }

    /**
     * Determines if a dialog scrollbar should be visible or not and shows/hides it accordingly.
     *
     * <p>If this is being called as a result of adapter changes, it should be called after the new
     * layout has been calculated because the method of determining scrollbar visibility uses the
     * current layout.
     *
     * <p>If this is called after an adapter change but before the new layout, the visibility
     * determination may not be correct.
     */
    static void updateDialogListScrollbar(@NonNull PagedListView list,
            @NonNull PagedScrollBarView scrollBarView) {
        RecyclerView recyclerView = list.getRecyclerView();

        boolean isAtStart = list.isAtStart();
        boolean isAtEnd = list.isAtEnd();

        if (isAtStart && isAtEnd) {
            scrollBarView.setVisibility(View.INVISIBLE);
            return;
        }

        scrollBarView.setVisibility(View.VISIBLE);
        scrollBarView.setUpEnabled(!isAtStart);
        scrollBarView.setDownEnabled(!isAtEnd);

        // Assume the list scrolls vertically because we control the list and know the
        // LayoutManager cannot change.
        scrollBarView.setParameters(
                recyclerView.computeVerticalScrollRange(),
                recyclerView.computeVerticalScrollOffset(),
                recyclerView.computeVerticalScrollExtent(),
                /* animate= */ false);

        scrollBarView.invalidate();
    }
}
