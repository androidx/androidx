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

import android.app.Activity;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.supportv7.Cheeses;
import com.example.android.supportv7.R;
import com.example.android.supportv7.widget.util.ConfigToggle;
import com.example.android.supportv7.widget.util.ConfigViewHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Sample activity from which ItemTouchHelper demo activities inherit.
 */
abstract public class ItemTouchHelperActivity extends Activity {

    public RecyclerView mRecyclerView;

    public ItemTouchAdapter mAdapter;

    public ItemTouchHelper mItemTouchHelper;

    public ItemTouchHelper.Callback mCallback;

    private ConfigToggle[] mConfigToggles;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_touch);
        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mAdapter = createAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mItemTouchHelper = createItemTouchHelper();
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
        initToggles();
    }

    private void initToggles() {
        mConfigToggles = createConfigToggles();
        RecyclerView configView = findViewById(R.id.config_recycler_view);
        configView.setAdapter(new RecyclerView.Adapter<ConfigViewHolder>() {
            @Override
            public ConfigViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new ConfigViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.config_view_toggle, parent, false));
            }

            @Override
            public void onBindViewHolder(ConfigViewHolder holder, int position) {
                ConfigToggle toggle = mConfigToggles[position];
                holder.bind(toggle);
            }

            @Override
            public int getItemCount() {
                return mConfigToggles.length;
            }
        });
        configView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,
                false));
        configView.setHasFixedSize(true);
    }

    abstract ConfigToggle[] createConfigToggles();

    public ItemTouchHelper createItemTouchHelper() {
        mCallback = createCallback();
        return new ItemTouchHelper(mCallback);
    }

    public ItemTouchHelper.Callback createCallback() {
        return new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder) {
                return ItemTouchHelperActivity.this.getMovementFlags(recyclerView, viewHolder);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder,
                    @NonNull RecyclerView.ViewHolder target) {
                mAdapter.move(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                mAdapter.delete(viewHolder.getAdapterPosition());
            }

            @Override
            public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder,
                    int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                ItemTouchHelperActivity.this.onSelectedChanged(viewHolder, actionState);
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder,
                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (ItemTouchHelperActivity.this.onChildDraw(c, recyclerView, viewHolder,
                        dX, dY, actionState, isCurrentlyActive)) {
                    return;
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState,
                        isCurrentlyActive);
            }

            @Override
            public void onChildDrawOver(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                    RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState,
                    boolean isCurrentlyActive) {
                if (ItemTouchHelperActivity.this.onChildDrawOver(c, recyclerView, viewHolder,
                        dX, dY, actionState, isCurrentlyActive)) {
                    return;
                }
                super.onChildDrawOver(c, recyclerView, viewHolder, dX, dY, actionState,
                        isCurrentlyActive);
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return ItemTouchHelperActivity.this.isLongPressDragEnabled();
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return ItemTouchHelperActivity.this.isPointerSwipeEnabled();
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                ItemTouchHelperActivity.this.clearView(viewHolder);
            }
        };
    }

    /**
     * @return True if we should NOT call parent
     */
    public boolean onChildDraw(Canvas c, RecyclerView recyclerView,
            RecyclerView.ViewHolder viewHolder,
            float dX, float dY, int actionState, boolean isCurrentlyActive) {
        return false;
    }

    /**
     * @return True if we should NOT call parent
     */
    public boolean onChildDrawOver(Canvas c, RecyclerView recyclerView,
            RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState,
            boolean isCurrentlyActive) {
        return false;
    }

    public void clearView(RecyclerView.ViewHolder viewHolder) {

    }

    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {

    }

    public boolean isLongPressDragEnabled() {
        return true;
    }

    public boolean isPointerSwipeEnabled() {
        return true;
    }

    public ItemTouchViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ItemTouchViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.touch_item, parent, false));
    }

    abstract public int getMovementFlags(RecyclerView recyclerView,
            RecyclerView.ViewHolder viewHolder);

    public ItemTouchAdapter createAdapter() {
        return new ItemTouchAdapter();
    }

    public void onBind(ItemTouchViewHolder viewHolder) {

    }

    public void onCreateViewHolder(ItemTouchViewHolder viewHolder) {

    }

    public class ItemTouchViewHolder extends RecyclerView.ViewHolder {

        public final TextView textView;

        public final Button actionButton;

        public final CardView cardView;

        public final View overlay;

        public ItemTouchViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            textView = (TextView) itemView.findViewById(R.id.text_view);
            actionButton = (Button) itemView.findViewById(R.id.action_button);
            overlay = itemView.findViewById(R.id.overlay);
        }
    }

    public class ItemTouchAdapter extends RecyclerView.Adapter<ItemTouchViewHolder> {

        private List<String> mItems = new ArrayList<String>();

        public ItemTouchAdapter() {
            mItems.addAll(Arrays.asList(Cheeses.sCheeseStrings));
        }

        @Override
        public ItemTouchViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return ItemTouchHelperActivity.this.onCreateViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(ItemTouchViewHolder holder, int position) {
            holder.textView.setText(mItems.get(position));
            onBind(holder);
        }

        public void delete(int position) {
            mItems.remove(position);
            notifyItemRemoved(position);
        }

        public void move(int from, int to) {
            String prev = mItems.remove(from);
            mItems.add(to > from ? to - 1 : to, prev);
            notifyItemMoved(from, to);
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }


}
