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

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.test.R;

public class WearableRecyclerViewTestActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wearable_recycler_view_basic);
        WearableRecyclerView wrv = findViewById(R.id.wrv);
        wrv.setLayoutManager(new WearableLinearLayoutManager(this));
        wrv.setAdapter(new TestAdapter());
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        TextView mView;
        ViewHolder(TextView itemView) {
            super(itemView);
            mView = itemView;
        }
    }

    private class TestAdapter extends WearableRecyclerView.Adapter<ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView view = new TextView(parent.getContext());
            view.setLayoutParams(new RecyclerView.LayoutParams(200, 50));
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.mView.setText("holder at position " + position);
            holder.mView.setTag(position);
        }

        @Override
        public int getItemCount() {
            return 100;
        }
    }
}
