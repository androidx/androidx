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
 * A {@link RecyclerView.ItemDecoration} that will add a bottom offset to the last item in the
 * RecyclerView it is added to.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class BottomOffsetDecoration extends RecyclerView.ItemDecoration {
    private int mBottomOffset;

    public BottomOffsetDecoration(int bottomOffset) {
        mBottomOffset = bottomOffset;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
            RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        RecyclerView.Adapter adapter = parent.getAdapter();

        if (adapter == null || adapter.getItemCount() == 0) {
            return;
        }

        if (parent.getLayoutManager() instanceof GridLayoutManager) {
            if (GridLayoutManagerUtils.isOnLastRow(view, parent)) {
                outRect.bottom = mBottomOffset;
            }
        } else if (parent.getChildAdapterPosition(view) == adapter.getItemCount() - 1) {
            // Only set the offset for the last item.
            outRect.bottom = mBottomOffset;
        } else {
            outRect.bottom = 0;
        }
    }

    /** Sets the value to use for the bottom offset. */
    public void setBottomOffset(int bottomOffset) {
        mBottomOffset = bottomOffset;
    }

    /** Returns the set bottom offset. If none has been set, then 0 will be returned. */
    public int getBottomOffset() {
        return mBottomOffset;
    }
}
