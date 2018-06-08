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

package com.example.androidx.car;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemAdapter;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.PagedListView;
import androidx.car.widget.TextListItem;

/**
 * A demo activity that will display a {@link PagedListView} that has less items than can fit
 * on the screen and will vertically center these. This activity will continually add more
 * items as time goes on.
 */
public class VerticallyCenteredListDemo extends Activity {
    private static final int ADD_ITEM_DELAY_MS = 1000;
    private static final int MAX_NUM_OF_ITEMS_TO_ADD = 10;

    private int mNumOfItemsAdded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vertically_centered_paged_list_view);

        PagedListView list = findViewById(R.id.paged_list_view);

        CustomListProvider provider = new CustomListProvider(/* context= */ this);
        ListItemAdapter adapter = new ListItemAdapter(/* context= */ this, provider);

        list.setAdapter(adapter);

        Handler handler = new Handler();

        // Continually add items to the list until MAX_NUM_OF_ITEMS_TO_ADD is reached.
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (++mNumOfItemsAdded >= MAX_NUM_OF_ITEMS_TO_ADD) {
                    return;
                }

                provider.addListItem();
                adapter.notifyDataSetChanged();
                handler.postDelayed(this, ADD_ITEM_DELAY_MS);
            }
        }, ADD_ITEM_DELAY_MS);
    }

    /**
     * A custom {@link ListItemProvider} that allows additional {@link ListItem}s to be added to it.
     */
    private static class CustomListProvider extends ListItemProvider {
        private static final int INITIAL_NUM_OF_ITEMS = 3;

        private int mNumOfItems = INITIAL_NUM_OF_ITEMS;
        private final Context mContext;

        CustomListProvider(Context context) {
            mContext = context;
        }

        public void addListItem() {
            mNumOfItems++;
        }

        public ListItem get(int position) {
            TextListItem item = new TextListItem(mContext);
            item.setTitle("Item " + position);
            return item;
        }

        public int size() {
            return mNumOfItems;
        }

    }
}
