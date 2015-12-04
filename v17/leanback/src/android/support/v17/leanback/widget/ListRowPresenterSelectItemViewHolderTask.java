/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import android.support.v7.widget.RecyclerView;

/**
 * A task on the ListRowPresenter.ViewHolder that can select an item by position in the
 * HorizontalGridView and perform an optional item task on it.
 */
public class ListRowPresenterSelectItemViewHolderTask extends PresenterViewHolderTask {

    private int mItemPosition;
    private boolean mSmooth = true;
    private PresenterViewHolderTask mItemTask;

    public ListRowPresenterSelectItemViewHolderTask(int itemPosition) {
        setItemPosition(itemPosition);
    }

    /**
     * Sets the adapter position of item to select.
     * @param itemPosition Position of the item in adapter.
     */
    public void setItemPosition(int itemPosition) {
        mItemPosition = itemPosition;
    }

    /**
     * Returns the adapter position of item to select.
     * @return The adapter position of item to select.
     */
    public int getItemPosition() {
        return mItemPosition;
    }

    /**
     * Sets smooth scrolling to the item or jump to the item without scrolling.  By default it is
     * true.
     * @param smooth True for smooth scrolling to the item, false otherwise.
     */
    public void setSmooth(boolean smooth) {
        mSmooth = smooth;
    }

    /**
     * Returns true if smooth scrolling to the item false otherwise.  By default it is true.
     * @return True for smooth scrolling to the item, false otherwise.
     */
    public boolean isSmooth() {
        return mSmooth;
    }

    /**
     * Returns optional task to run when the item is selected, null for no task.
     * @return Optional task to run when the item is selected, null for no task.
     */
    public PresenterViewHolderTask getItemTask() {
        return mItemTask;
    }

    /**
     * Sets task to run when the item is selected, null for no task.
     * @param itemTask Optional task to run when the item is selected, null for no task.
     */
    public void setItemTask(PresenterViewHolderTask itemTask) {
        mItemTask = itemTask;
    }

    @Override
    public void run(Presenter.ViewHolder holder) {
        if (holder instanceof ListRowPresenter.ViewHolder) {
            HorizontalGridView gridView = ((ListRowPresenter.ViewHolder) holder).getGridView();
            ViewHolderTask task = null;
            if (mItemTask != null) {
                task = new ViewHolderTask() {
                    final PresenterViewHolderTask itemTask = mItemTask;
                    @Override
                    public void run(RecyclerView.ViewHolder rvh) {
                        ItemBridgeAdapter.ViewHolder ibvh = (ItemBridgeAdapter.ViewHolder) rvh;
                        itemTask.run(ibvh.getViewHolder());
                    }
                };
            }
            if (mSmooth) {
                gridView.setSelectedPositionSmooth(mItemPosition, task);
            } else {
                gridView.setSelectedPosition(mItemPosition, task);
            }
        }
    }
}