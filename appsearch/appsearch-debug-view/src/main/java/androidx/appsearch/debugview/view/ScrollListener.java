/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appsearch.debugview.view;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Listens for scrolling and loads the next page of results if the end of the view is reached.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class ScrollListener extends RecyclerView.OnScrollListener {
    private final LinearLayoutManager mLayoutManager;

    public ScrollListener(@NonNull LinearLayoutManager layoutManager) {
        mLayoutManager = Preconditions.checkNotNull(layoutManager);
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        int itemsVisible = mLayoutManager.getChildCount();
        int totalItems = mLayoutManager.getItemCount();
        int firstItemInViewIndex = mLayoutManager.findFirstVisibleItemPosition();

        // This value is true when the RecyclerView has additional rows that can be filled and
        // the underlying adapter does not have sufficient items to fill them.
        boolean hasAdditionalRowsToFill = (firstItemInViewIndex + itemsVisible) >= totalItems;

        if (!isLoading() && hasAdditionalPages()) {
            if (hasAdditionalRowsToFill && firstItemInViewIndex >= 0) {
                loadNextPage();
            }
        }
    }

    /**
     * Defines how to load the next page of results to display.
     */
    public abstract void loadNextPage();

    /**
     * Indicates whether a page is currently be loading.
     *
     * <p>{@link #loadNextPage()} will not be called if this is {@code true}.
     */
    public abstract boolean isLoading();

    /**
     * Indicates whether there are additional pages to load.
     *
     * <p>{@link #loadNextPage()} will not be called if this is {@code true}.
     */
    public abstract boolean hasAdditionalPages();
}
