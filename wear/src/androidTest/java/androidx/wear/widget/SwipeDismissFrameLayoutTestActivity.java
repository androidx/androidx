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

package androidx.wear.widget;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.test.R;

public class SwipeDismissFrameLayoutTestActivity extends LayoutTestActivity {

    public static final String EXTRA_LAYOUT_HORIZONTAL = "layout_horizontal";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int layoutId = getIntent().getIntExtra(EXTRA_LAYOUT_RESOURCE_ID, -1);
        boolean horizontal = getIntent().getBooleanExtra(EXTRA_LAYOUT_HORIZONTAL, false);

        if (layoutId == R.layout.swipe_dismiss_layout_testcase_2) {
            createScrollableContent(horizontal);
        }
    }

    private void createScrollableContent(boolean horizontal) {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_container);
        if (recyclerView == null) {
            throw new NullPointerException("There has to be a relevant container defined");
        }
        recyclerView.setLayoutManager(
                new LinearLayoutManager(
                        this,
                        horizontal ? LinearLayoutManager.HORIZONTAL : LinearLayoutManager.VERTICAL,
                        false));
        recyclerView.setAdapter(new MyRecyclerViewAdapter());
    }

    private static class MyRecyclerViewAdapter
            extends RecyclerView.Adapter<MyRecyclerViewAdapter.CustomViewHolder> {
        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView textView = new TextView(parent.getContext());
            textView.setText("A LOT OF TEXT VIEW");
            textView.setGravity(Gravity.CENTER_VERTICAL);
            return new CustomViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(CustomViewHolder holder, int position) {
        }

        @Override
        public int getItemCount() {
            return 100;
        }

        static class CustomViewHolder extends RecyclerView.ViewHolder {

            CustomViewHolder(View view) {
                super(view);
            }
        }
    }
}
