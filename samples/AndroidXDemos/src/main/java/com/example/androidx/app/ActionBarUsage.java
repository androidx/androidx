/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.example.androidx.app;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;

import com.example.androidx.R;

/**
 * This demonstrates idiomatic usage of the Action Bar. The default Honeycomb theme
 * includes the action bar by default and a menu resource is used to populate the
 * menu data itself. If you'd like to see how these things work under the hood, see
 * ActionBarMechanics.
 */
public class ActionBarUsage extends AppCompatActivity {
    TextView mSearchText;
    int mSortMode = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSearchText = new TextView(this);
        setContentView(mSearchText);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setOnQueryTextListener(mOnQueryTextListener);
        final MenuItem editItem = menu.findItem(R.id.action_edit);
        MenuItemCompat.setContentDescription(editItem,
                getString(R.string.action_bar_edit_description));
        MenuItemCompat.setTooltipText(editItem,
                getString(R.string.action_bar_edit_tooltip));
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mSortMode != -1) {
            Drawable icon = menu.findItem(mSortMode).getIcon();
            menu.findItem(R.id.action_sort).setIcon(icon);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_sort_alpha || itemId == R.id.action_sort_size) {
            onSort(item);
        }

        Toast.makeText(this, "Selected Item: " + item.getTitle(), Toast.LENGTH_SHORT).show();

        return true;
    }

    private void onSort(MenuItem item) {
        mSortMode = item.getItemId();
        // Request a call to onPrepareOptionsMenu so we can change the sort icon
        supportInvalidateOptionsMenu();
    }

    // The following callbacks are called for the SearchView.OnQueryChangeListener
    // For more about using SearchView, see src/.../view/SearchView1.java and SearchView2.java
    private final SearchView.OnQueryTextListener mOnQueryTextListener =
            new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextChange(String newText) {
            newText = TextUtils.isEmpty(newText) ? "" : "Query so far: " + newText;
            mSearchText.setText(newText);
            return true;
        }

        @Override
        public boolean onQueryTextSubmit(String query) {
            Toast.makeText(ActionBarUsage.this,
                    "Searching for: " + query + "...", Toast.LENGTH_SHORT).show();
            return true;
        }
    };
}
