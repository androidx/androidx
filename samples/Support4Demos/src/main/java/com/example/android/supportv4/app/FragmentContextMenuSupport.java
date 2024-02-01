/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.example.android.supportv4.app;

import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.android.supportv4.R;

/**
 * Demonstration of displaying a context menu from a fragment.
 */
public class FragmentContextMenuSupport extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create the list fragment and add it as our sole content.
        ContextMenuFragment content = new ContextMenuFragment();
        getSupportFragmentManager().beginTransaction().add(
                android.R.id.content, content).commit();
    }

    public static class ContextMenuFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View root = inflater.inflate(R.layout.fragment_context_menu, container, false);
            registerForContextMenu(root.findViewById(R.id.long_press));
            return root;
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            super.onCreateContextMenu(menu, v, menuInfo);
            menu.add(Menu.NONE, R.id.a_item, Menu.NONE, "Menu A");
            menu.add(Menu.NONE, R.id.b_item, Menu.NONE, "Menu B");
        }

        @Override
        public boolean onContextItemSelected(MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.a_item) {
                Log.i("ContextMenu", "Item 1a was chosen");
                return true;
            } else if (itemId == R.id.b_item) {
                Log.i("ContextMenu", "Item 1b was chosen");
                return true;
            }
            return super.onContextItemSelected(item);
        }
    }
}
