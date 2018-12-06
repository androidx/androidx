/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.car.widget.itemdecorators;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.RestrictTo;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A {@link RecyclerView.ItemDecoration} that will add spacing between each item in the
 * RecyclerView that it is added to.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class ItemSpacingDecoration extends RecyclerView.ItemDecoration {
    private int mItemSpacing;

    public ItemSpacingDecoration(int itemSpacing) {
        mItemSpacing = itemSpacing;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
            RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        int position = parent.getChildAdapterPosition(view);

        // Skip offset for last item except for GridLayoutManager.
        if (position == state.getItemCount() - 1
                && !(parent.getLayoutManager() instanceof GridLayoutManager)) {
            return;
        }

        outRect.bottom = mItemSpacing;
    }

    /**
     * @param itemSpacing sets spacing between each item.
     */
    public void setItemSpacing(int itemSpacing) {
        mItemSpacing = itemSpacing;
    }
}
