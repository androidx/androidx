/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.car.widget;

import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;

/** {@link DefaultItemAnimator} with a few minor changes where it had undesired behavior. */
public class CarItemAnimator extends DefaultItemAnimator {

    private final PagedLayoutManager mLayoutManager;

    public CarItemAnimator(PagedLayoutManager layoutManager) {
        mLayoutManager = layoutManager;
    }

    @Override
    public boolean animateChange(RecyclerView.ViewHolder oldHolder,
            RecyclerView.ViewHolder newHolder,
            int fromX,
            int fromY,
            int toX,
            int toY) {
        // The default behavior will cross fade the old view and the new one. However, if we
        // have a card on a colored background, it will make it appear as if a changing card
        // fades in and out.
        float alpha = 0f;
        if (newHolder != null) {
            alpha = newHolder.itemView.getAlpha();
        }
        boolean ret = super.animateChange(oldHolder, newHolder, fromX, fromY, toX, toY);
        if (newHolder != null) {
            newHolder.itemView.setAlpha(alpha);
        }
        return ret;
    }

    @Override
    public void onMoveFinished(RecyclerView.ViewHolder item) {
        // The item animator uses translation heavily internally. However, we also use translation
        // to create the paging affect. When an item's move is animated, it will mess up the
        // translation we have set on it so we must re-offset the rows once the animations finish.

        // isRunning(ItemAnimationFinishedListener) is the awkward API used to determine when all
        // animations have finished.
        isRunning(mFinishedListener);
    }

    private final ItemAnimatorFinishedListener mFinishedListener =
            new ItemAnimatorFinishedListener() {
                @Override
                public void onAnimationsFinished() {
                    mLayoutManager.offsetRows();
                }
            };
}
