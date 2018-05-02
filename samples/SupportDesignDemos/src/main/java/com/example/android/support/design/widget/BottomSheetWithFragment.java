/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.example.android.support.design.widget;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.support.design.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

/**
 * This demonstrates basic usage of {@link BottomSheetBehavior} with Fragment.
 */
public class BottomSheetWithFragment extends AppCompatActivity {

    private BottomSheetBehavior mBottomSheetBehavior;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.design_bottom_sheet_with_fragment);
        setUpRecyclerView((RecyclerView) findViewById(R.id.list1));
        mBottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));
    }

    @Override
    public void onBackPressed() {
        if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    private static void setUpRecyclerView(RecyclerView recyclerView) {
        Context context = recyclerView.getContext();
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new DummyAdapter(context, 30));
    }

    public static class BottomSheetFragment extends Fragment {

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.design_bottom_sheet_fragment, container, false);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            setUpRecyclerView((RecyclerView) view.findViewById(R.id.list2));
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView text;

        public ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(android.R.layout.simple_list_item_1, parent, false));
            text = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }

    public static class DummyAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final Context mContext;

        private final int mItemCount;

        public DummyAdapter(Context context, int itemCount) {
            mContext = context;
            mItemCount = itemCount;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(mContext), parent);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.text.setText(mContext.getString(R.string.item_n, position + 1));
        }

        @Override
        public int getItemCount() {
            return mItemCount;
        }
    }

}
