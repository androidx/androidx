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
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import static androidx.recyclerview.widget.RecyclerView.VERTICAL;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SwipeRefreshLayoutInRecyclerViewWithRecyclerViewChildActivity extends
        SwipeRefreshLayoutInRecyclerViewBaseActivity {
    @Override
    protected View populateSwipeRefreshLayout(@NonNull SwipeRefreshLayout parent) {
        RecyclerView rv = new RecyclerView(parent.getContext());
        rv.setLayoutParams(matchParent());
        rv.setLayoutManager(new LinearLayoutManager(parent.getContext(), VERTICAL, false));
        rv.setAdapter(new MyAdapter());

        LinearLayout ll = new LinearLayout(parent.getContext());
        ll.setLayoutParams(matchParent());
        ll.addView(rv);

        ViewCompat.setNestedScrollingEnabled(ll, false);
        return ll;
    }

    private static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView mTextView;
        MyViewHolder(TextView itemView) {
            super(itemView);
            mTextView = itemView;
        }
    }

    private static class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
        @Override
        public int getItemCount() {
            return 100;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textView = new TextView(parent.getContext());
            textView.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            textView.setGravity(Gravity.CENTER);
            textView.setTextSize(24f);
            return new MyViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            holder.mTextView.setText("Position " + position);
        }
    }
}
