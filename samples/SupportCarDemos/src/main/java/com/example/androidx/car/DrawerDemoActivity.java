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

import android.content.Context;
import android.os.Bundle;

import androidx.car.drawer.CarDrawerActivity;
import androidx.car.drawer.CarDrawerAdapter;
import androidx.car.drawer.DrawerItemViewHolder;

/**
 * A demo activity that will inflate a layout with a drawer.
 */
public class DrawerDemoActivity extends CarDrawerActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMainContent(R.layout.drawer_activity);
        getDrawerController().setRootAdapter(new DrawerRootAdapter(/* context= */ this));
    }

    private void openSubDrawerLevel() {
        getDrawerController().pushAdapter(new SubItemAdapter(/* context= */ this));
    }

    /**
     * The root drawer view that will delegate clicks to the a sub-drawer.
     */
    private class DrawerRootAdapter extends CarDrawerAdapter {
        private static final int NUM_OF_ITEMS = 10;

        DrawerRootAdapter(Context context) {
            super(context, /* showDisabledListOnEmpty= */ true);
            setTitle(getString(R.string.drawer_demo_activity_drawer_title));
        }

        @Override
        protected void populateViewHolder(DrawerItemViewHolder holder, int position) {
            holder.getTitle().setText(getString(R.string.drawer_demo_root_item_label, position));
        }

        @Override
        protected int getActualItemCount() {
            return NUM_OF_ITEMS;
        }

        @Override
        public void onItemClick(int position) {
            openSubDrawerLevel();
        }
    }

    /**
     * A sub-level of the drawer that is opened when any of the items in the root drawer view have
     * been clicked.
     */
    private class SubItemAdapter extends CarDrawerAdapter {
        private static final int NUM_OF_ITEMS = 3;

        SubItemAdapter(Context context) {
            super(context, /* showDisabledListOnEmpty= */ true);
            setTitle(getString(R.string.drawer_demo_activity_drawer_sub_title));
        }

        @Override
        protected void populateViewHolder(DrawerItemViewHolder holder, int position) {
            holder.getTitle().setText(getString(R.string.drawer_demo_subitem_label, position));
        }

        @Override
        protected int getActualItemCount() {
            return NUM_OF_ITEMS;
        }

        @Override
        public void onItemClick(int position) {}

    }
}
