/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.widget.drawer;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.test.R;

public class DrawerRecyclerViewTestActivity extends Activity {
    private static final int DRAWER_SIZE = 5;
    private static final int RECYCLER_SIZE = 5;

    private final WearableNavigationDrawerView.WearableNavigationDrawerAdapter mDrawerAdapter =
            new WearableNavigationDrawerView.WearableNavigationDrawerAdapter() {
                @Override
                public String getItemText(int pos) {
                    return Integer.toString(pos);
                }

                @Override
                public Drawable getItemDrawable(int pos) {
                    return getDrawable(android.R.drawable.star_on);
                }

                @Override
                public int getCount() {
                    return DRAWER_SIZE;
                }
            };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.test_drawer_horizontal_scroll);

        WearableNavigationDrawerView navDrawer = findViewById(R.id.recycler_navigation_drawer);
        navDrawer.setAdapter(mDrawerAdapter);

        RecyclerView rv = findViewById(R.id.recycler);
        rv.setAdapter(new ScrollAdapter());
        LinearLayoutManager rvLayoutManager = new LinearLayoutManager(this);
        rvLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        rv.setLayoutManager(rvLayoutManager);
    }

    private class ScrollAdapter extends RecyclerView.Adapter<ScrollAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(
                    getLayoutInflater().inflate(
                            R.layout.test_drawer_horizontal_scroll_fragment,
                            parent,
                            /* attachToRoot= */false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.mTextView.setText(Integer.toString(position));
        }

        @Override
        public int getItemCount() {
            return RECYCLER_SIZE;
        }

        private class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView mTextView;

            ViewHolder(@NonNull View itemView) {
                super(itemView);

                mTextView = itemView.findViewById(R.id.drawer_recycler_fragment);
            }
        }
    }
}
