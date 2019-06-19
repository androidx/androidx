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

package androidx.swiperefreshlayout.widget;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import static androidx.recyclerview.widget.RecyclerView.HORIZONTAL;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SwipeRefreshLayoutInRecyclerViewActivity extends FragmentActivity {

    RecyclerView mRecyclerView;
    SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRecyclerView = new RecyclerView(this);
        mRecyclerView.setLayoutParams(matchParent());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, HORIZONTAL, false));
        mRecyclerView.setAdapter(new Adapter());
        setContentView(mRecyclerView);
    }

    private static ViewGroup.LayoutParams matchParent() {
        return new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT);
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private static final int SRL_ITEM = 0;
        private static final int OTHER_ITEM = 1;

        @Override
        public int getItemCount() {
            return 3;
        }

        @Override
        public int getItemViewType(int position) {
            return position == 1 ? SRL_ITEM : OTHER_ITEM;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return viewType == SRL_ITEM ? createSrlItem(parent) : createOtherItem(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            super.onViewRecycled(holder);
            if (holder.getItemViewType() == SRL_ITEM) {
                mSwipeRefreshLayout = null;
            }
        }

        private ViewHolder createSrlItem(@NonNull ViewGroup parent) {
            View child = new View(parent.getContext());
            child.setBackgroundColor(0xFF0000FF);
            mSwipeRefreshLayout = new SwipeRefreshLayout(parent.getContext());
            mSwipeRefreshLayout.setLayoutParams(matchParent());
            mSwipeRefreshLayout.addView(child);
            return new ViewHolder(mSwipeRefreshLayout);
        }

        private ViewHolder createOtherItem(@NonNull ViewGroup parent) {
            View view = new View(parent.getContext());
            view.setBackgroundColor(0xFFFF0000);
            return new ViewHolder(view);
        }
    }
}
