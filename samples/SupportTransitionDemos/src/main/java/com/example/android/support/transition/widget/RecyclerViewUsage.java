/*
 * Copyright 2018 The Android Open Source Project
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

package com.example.android.support.transition.widget;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.example.android.support.transition.R;

/**
 * This demonstrates usage of Transitions applied to RecyclerView items.
 */
public class RecyclerViewUsage extends TransitionUsageBase {

    private RecyclerView mRecyclerView;

    @Override
    int getLayoutResId() {
        return R.layout.recycler_transition;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRecyclerView = findViewById(R.id.recycler);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        final TransitionAdapter adapter = new TransitionAdapter();
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.setItemAnimator(null);
    }

    class TransitionAdapter extends RecyclerView.Adapter<TransitionHolder> {

        private int mExpandedPosition = RecyclerView.NO_POSITION;

        @NonNull
        @Override
        public TransitionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new TransitionHolder(
                    (ViewGroup) inflater.inflate(R.layout.recycler_view_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull TransitionHolder holder, int position) {
            holder.itemView.setOnClickListener(v -> {
                final int clickedPosition = holder.getBindingAdapterPosition();
                if (clickedPosition == RecyclerView.NO_POSITION) return;

                TransitionManager.beginDelayedTransition(mRecyclerView,
                        new AutoTransition()
                                .setOrdering(TransitionSet.ORDERING_TOGETHER)
                                .setDuration(300));

                // collapse any currently expanded items
                if (RecyclerView.NO_POSITION != mExpandedPosition) {
                    notifyItemChanged(mExpandedPosition);
                }

                // expand this item (if it wasn't already)
                if (mExpandedPosition != clickedPosition) {
                    mExpandedPosition = clickedPosition;
                    notifyItemChanged(clickedPosition);
                } else {
                    mExpandedPosition = RecyclerView.NO_POSITION;
                }
            });
            holder.mDescription.setVisibility(position == mExpandedPosition
                    ? View.VISIBLE : View.GONE);
        }

        @Override
        public int getItemCount() {
            return 100;
        }

    }

    static class TransitionHolder extends RecyclerView.ViewHolder {

        final View mTitle;
        final View mDescription;

        TransitionHolder(ViewGroup view) {
            super(view);
            mTitle = view.findViewById(R.id.title);
            mDescription = view.findViewById(R.id.description);
        }
    }
}
