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

import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.supportv7.R;
import com.example.android.supportv7.widget.util.ConfigToggle;

public class SwipeToDismissActivity extends ItemTouchHelperActivity {
    boolean mSwipeStartEnabled = true;
    boolean mSwipeEndEnabled = true;
    boolean mPointerSwipeEnabled = true;
    boolean mCustomSwipeEnabled = false;

    @Override
    ConfigToggle[] createConfigToggles() {
        ConfigToggle[] configToggles = {
                new ConfigToggle(this, R.string.swipe_start) {
                    @Override
                    public boolean isChecked() {
                        return mSwipeStartEnabled;
                    }

                    @Override
                    public void onChange(boolean newValue) {
                        mSwipeStartEnabled = newValue;
                    }
                },
                new ConfigToggle(this, R.string.swipe_end) {
                    @Override
                    public boolean isChecked() {
                        return mSwipeEndEnabled;
                    }

                    @Override
                    public void onChange(boolean newValue) {
                        mSwipeEndEnabled = newValue;
                    }
                },
                new ConfigToggle(this, R.string.pointer_swipe_enabled) {
                    @Override
                    public boolean isChecked() {
                        return mPointerSwipeEnabled;
                    }

                    @Override
                    public void onChange(boolean newValue) {
                        mPointerSwipeEnabled = newValue;
                        mAdapter.notifyDataSetChanged();
                    }
                }
        };
        ConfigToggle[] copy = new ConfigToggle[configToggles.length + 1];
        System.arraycopy(configToggles, 0, copy, 0, configToggles.length);
        copy[copy.length - 1] = new ConfigToggle(this, R.string.custom_swipe_enabled) {
            @Override
            public boolean isChecked() {
                return mCustomSwipeEnabled;
            }

            @Override
            public void onChange(boolean newValue) {
                mCustomSwipeEnabled = newValue;
            }
        };
        return copy;
    }

    @Override
    public void onBind(ItemTouchViewHolder viewHolder) {
        super.onBind(viewHolder);
        viewHolder.actionButton.setVisibility(mPointerSwipeEnabled ? View.GONE : View.VISIBLE);
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
            if (mCustomSwipeEnabled) {
                // hide it
                touchVH.overlay.setTranslationX(viewHolder.itemView.getWidth());
                touchVH.overlay.setVisibility(View.VISIBLE);
            }
        }
        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public boolean onChildDraw(Canvas c, RecyclerView recyclerView,
            RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState,
            boolean isCurrentlyActive) {
        if (!mCustomSwipeEnabled) {
            return false;
        }
        ItemTouchViewHolder touchVH = (ItemTouchViewHolder) viewHolder;
        final float dir = Math.signum(dX);
        if (dir == 0) {
            touchVH.overlay.setTranslationX(-touchVH.overlay.getWidth());
        } else {
            final float overlayOffset = dX - dir * viewHolder.itemView.getWidth();
            touchVH.overlay.setTranslationX(overlayOffset);
        }
        float alpha = (float) (.2 + .8 * Math.abs(dX) / viewHolder.itemView.getWidth());
        touchVH.overlay.setAlpha(alpha);
        return true;
    }

    @Override
    public ItemTouchViewHolder onCreateViewHolder(ViewGroup parent) {
        final ItemTouchViewHolder vh = super.onCreateViewHolder(parent);
        vh.actionButton.setText(R.string.swipe);
        vh.actionButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    mItemTouchHelper.startSwipe(vh);
                }
                return false;
            }
        });
        return vh;
    }

    @Override
    public boolean isPointerSwipeEnabled() {
        return mPointerSwipeEnabled;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return mCallback.makeMovementFlags(0,
                (mSwipeStartEnabled ? ItemTouchHelper.START : 0) |
                        (mSwipeEndEnabled ? ItemTouchHelper.END : 0));
    }
}
