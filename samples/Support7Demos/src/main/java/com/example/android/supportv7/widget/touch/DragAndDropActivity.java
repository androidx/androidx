/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.supportv7.widget.touch;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.supportv7.R;
import com.example.android.supportv7.widget.util.ConfigToggle;

public class DragAndDropActivity extends ItemTouchHelperActivity {

    boolean mDragUpEnabled = true;
    boolean mDragDownEnabled = true;
    boolean mLongPressDragEnabled = true;

    @Override
    ConfigToggle[] createConfigToggles() {
        return new ConfigToggle[]{
                new ConfigToggle(this, R.string.drag_up) {
                    @Override
                    public boolean isChecked() {
                        return mDragUpEnabled;
                    }

                    @Override
                    public void onChange(boolean newValue) {
                        mDragUpEnabled = newValue;
                    }
                },
                new ConfigToggle(this, R.string.drag_down) {
                    @Override
                    public boolean isChecked() {
                        return mDragDownEnabled;
                    }

                    @Override
                    public void onChange(boolean newValue) {
                        mDragDownEnabled = newValue;
                    }
                },
                new ConfigToggle(this, R.string.long_press_drag) {
                    @Override
                    public boolean isChecked() {
                        return mLongPressDragEnabled;
                    }

                    @Override
                    public void onChange(boolean newValue) {
                        mLongPressDragEnabled = newValue;
                        mAdapter.notifyDataSetChanged();
                    }
                }
        };
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return mLongPressDragEnabled;
    }

    @Override
    public void onBind(ItemTouchViewHolder viewHolder) {
        super.onBind(viewHolder);
        viewHolder.actionButton.setVisibility(mLongPressDragEnabled ? View.GONE : View.VISIBLE);
    }

    @Override
    public void clearView(RecyclerView.ViewHolder viewHolder) {
        super.clearView(viewHolder);
        ItemTouchViewHolder touchVH = (ItemTouchViewHolder) viewHolder;
        touchVH.cardView.setCardBackgroundColor(
                ContextCompat.getColor(this, android.R.color.white));
        touchVH.overlay.setVisibility(View.GONE);
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        ItemTouchViewHolder touchVH = (ItemTouchViewHolder) viewHolder;
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            touchVH.cardView.setCardBackgroundColor(
                    ContextCompat.getColor(this, R.color.card_aquatic));
        }
        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public ItemTouchViewHolder onCreateViewHolder(ViewGroup parent) {
        final ItemTouchViewHolder vh = super.onCreateViewHolder(parent);
        vh.actionButton.setText(R.string.drag);
        vh.actionButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    mItemTouchHelper.startDrag(vh);
                }
                return false;
            }
        });
        return vh;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return mCallback.makeMovementFlags(
                (mDragUpEnabled ? ItemTouchHelper.UP : 0) |
                        (mDragDownEnabled ? ItemTouchHelper.DOWN : 0), 0);
    }
}
