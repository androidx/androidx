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

package com.example.androidx.widget;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidx.R;

/**
 * A sample nested RecyclerView activity.
 */
public class RvInNestedScrollViewActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_rv_in_nestedscrollview);

        RecyclerView recyclerView = findViewById(R.id.rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new MyAdapter(this));
    }

    private static class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {

        Context mContext;
        int mItemMinHeight;

        MyAdapter(Context context) {
            mContext = context;
            mItemMinHeight = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 96,
                    context.getResources().getDisplayMetrics());
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent,
                int viewType) {
            TextView textView = new TextView(mContext);
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            textView.setLayoutParams(layoutParams);
            textView.setMinHeight(mItemMinHeight);
            return new MyViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            TextView textView = ((TextView) holder.itemView);
            textView.setText("Position: " + position);
            textView.setOnClickListener(
                    v -> Toast.makeText(v.getContext(), "CLICK!", Toast.LENGTH_SHORT).show());
        }

        @Override
        public int getItemCount() {
            return 50;
        }
    }

    private static class MyViewHolder extends RecyclerView.ViewHolder {

        MyViewHolder(View itemView) {
            super(itemView);
        }
    }
}
