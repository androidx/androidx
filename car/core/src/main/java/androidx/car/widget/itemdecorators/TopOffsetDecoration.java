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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.RestrictTo;
import androidx.car.util.GridLayoutManagerUtils;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A {@link RecyclerView.ItemDecoration} that will add a top offset to the first item in the
 * RecyclerView it is added to.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class TopOffsetDecoration extends RecyclerView.ItemDecoration {
    private int mTopOffset;

    public TopOffsetDecoration(int topOffset) {
        mTopOffset = topOffset;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
            RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        int position = parent.getChildAdapterPosition(view);
        if (parent.getLayoutManager() instanceof GridLayoutManager
                && position < GridLayoutManagerUtils.getFirstRowItemCount(parent)) {
            // For GridLayoutManager, top offset should be set for all items in the first row.
            // Otherwise the top items will be visually uneven.
            outRect.top = mTopOffset;
        } else if (position == 0) {
            // Only set the offset for the first item.
            outRect.top = mTopOffset;
        } else {
            outRect.top = 0;
        }
    }

    /** Sets the value to use for the top offset. */
    public void setTopOffset(int topOffset) {
        mTopOffset = topOffset;
    }

    /** Returns the set bottom offset. If none has been set, then 0 will be returned. */
    public int getTopOffset() {
        return mTopOffset;
    }
}
