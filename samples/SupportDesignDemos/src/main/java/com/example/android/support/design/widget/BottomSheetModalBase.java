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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.support.design.Cheeses;
import com.example.android.support.design.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

abstract class BottomSheetModalBase extends AppCompatActivity {

    private static final String FRAGMENT_MODAL = "modal";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.design_bottom_sheet_modal);
        findViewById(R.id.show_short).setOnClickListener(mOnClickListener);
        findViewById(R.id.show_long).setOnClickListener(mOnClickListener);
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.show_short:
                    ModalFragment.newInstance(5)
                            .show(getSupportFragmentManager(), FRAGMENT_MODAL);
                    break;
                case R.id.show_long:
                    ModalFragment.newInstance(ModalFragment.LENGTH_ALL)
                            .show(getSupportFragmentManager(), FRAGMENT_MODAL);
                    break;
            }
        }
    };

    /**
     * This is the bottom sheet.
     */
    public static class ModalFragment extends BottomSheetDialogFragment {

        private static final String ARG_LENGTH = "length";

        public static final int LENGTH_ALL = Cheeses.sCheeseStrings.length;

        public static ModalFragment newInstance(int length) {
            ModalFragment fragment = new ModalFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_LENGTH, Math.min(LENGTH_ALL, length));
            fragment.setArguments(args);
            return fragment;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.design_bottom_sheet_recyclerview, container, false);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            // For the scrolling content, you can use RecyclerView, NestedScrollView or any other
            // View that inherits NestedScrollingChild
            RecyclerView recyclerView =
                    (RecyclerView) view.findViewById(R.id.bottom_sheet_recyclerview);
            Context context = recyclerView.getContext();
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            int length = getArguments().getInt(ARG_LENGTH);
            String[] array = new String[length];
            System.arraycopy(Cheeses.sCheeseStrings, 0, array, 0, length);
            recyclerView.setAdapter(new SimpleStringRecyclerViewAdapter(context, array));
        }

    }

}
