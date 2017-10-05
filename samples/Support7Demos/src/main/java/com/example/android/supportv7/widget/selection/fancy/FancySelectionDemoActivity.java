/*
 * Copyright 2017 The Android Open Source Project
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

package com.example.android.supportv7.widget.selection.fancy;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import com.example.android.supportv7.R;

import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionHelper;
import androidx.recyclerview.selection.SelectionHelper.SelectionObserver;
import androidx.recyclerview.selection.SelectionHelperBuilder;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionStorage;

/**
 * ContentPager demo activity.
 */
public class FancySelectionDemoActivity extends AppCompatActivity {

    private static final String TAG = "SelectionDemos";
    private static final String EXTRA_COLUMN_COUNT = "demo-column-count";

    private FancySelectionDemoAdapter mAdapter;
    private SelectionHelper<Uri> mSelectionHelper;
    private SelectionStorage<Uri> mSelectionStorage;

    private GridLayoutManager mLayout;
    private int mColumnCount = 1;  // This will get updated when layout changes.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.selection_demo_layout);
        RecyclerView recView = (RecyclerView) findViewById(R.id.list);

        mLayout = new GridLayoutManager(this, mColumnCount);
        recView.setLayoutManager(mLayout);
        mAdapter = new FancySelectionDemoAdapter(this);
        recView.setAdapter(mAdapter);
        ItemKeyProvider<Uri> keyProvider = mAdapter.getItemKeyProvider();

        SelectionHelperBuilder<Uri> builder = new SelectionHelperBuilder<>(
                recView,
                keyProvider,
                new FancyDetailsLookup(recView));

        // Override default behaviors and build in multi select mode.
        // Call .withSelectionPredicate(SelectionHelper.SelectionPredicate.SINGLE_ANYTHING)
        // for single selection mode.
        mSelectionHelper = builder
                .withSelectionPredicate(SelectionPredicates.selectAnything())
                .withTouchCallbacks(new TouchCallbacks(this))
                .withMouseCallbacks(new MouseCallbacks(this))
                .withActivationCallbacks(new ActivationCallbacks(this))
                .withFocusCallbacks(new FocusCallbacks(this))
                .withBandOverlay(R.drawable.selection_demo_band_overlay)
                .build();

        // Provide glue between activity lifecycle and selection for purposes
        // restoring selection.
        mSelectionStorage = new SelectionStorage<>(
                SelectionStorage.TYPE_STRING, mSelectionHelper);

        // Lazily bind SelectionHelper. Allows us to defer initialization of the
        // SelectionHelper dependency until after the adapter is created.
        mAdapter.bindSelectionHelper(mSelectionHelper);

        // TODO: Glue selection to ActionMode, since that'll be a common practice.
        mSelectionHelper.addObserver(
                new SelectionObserver<Long>() {
                    @Override
                    public void onSelectionChanged() {
                        Log.i(TAG, "Selection changed to: " + mSelectionHelper.getSelection());
                    }
                });

        // Restore selection from saved state.
        updateFromSavedState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        mSelectionStorage.onSaveInstanceState(state);
        state.putInt(EXTRA_COLUMN_COUNT, mColumnCount);
    }

    private void updateFromSavedState(Bundle state) {
        mSelectionStorage.onRestoreInstanceState(state);

        if (state != null) {
            if (state.containsKey(EXTRA_COLUMN_COUNT)) {
                mColumnCount = state.getInt(EXTRA_COLUMN_COUNT);
                mLayout.setSpanCount(mColumnCount);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean showMenu = super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.selection_demo_actions, menu);
        return showMenu;
    }

    @Override
    @CallSuper
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.option_menu_add_column).setEnabled(mColumnCount <= 3);
        menu.findItem(R.id.option_menu_remove_column).setEnabled(mColumnCount > 1);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_menu_add_column:
                // TODO: Add columns
                mLayout.setSpanCount(++mColumnCount);
                return true;

            case R.id.option_menu_remove_column:
                mLayout.setSpanCount(--mColumnCount);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (mSelectionHelper.clear()) {
            return;
        } else {
            super.onBackPressed();
        }
    }

    private static void toast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        mSelectionHelper.clearSelection();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAdapter.loadData();
    }

    // Implementation of MouseInputHandler.Callbacks allows handling
    // of higher level events, like onActivated.
    private static final class ActivationCallbacks extends
            androidx.recyclerview.selection.ActivationCallbacks<Uri> {

        private final Context mContext;

        ActivationCallbacks(Context context) {
            mContext = context;
        }

        @Override
        public boolean onItemActivated(ItemDetails<Uri> item, MotionEvent e) {
            toast(mContext, "Activate item: " + item.getSelectionKey());
            return true;
        }
    }

    private static final class FocusCallbacks extends
            androidx.recyclerview.selection.FocusCallbacks<Uri> {

        private final Context mContext;

        private FocusCallbacks(Context context) {
            mContext = context;
        }

        @Override
        public void focusItem(ItemDetails<Uri> item) {
            toast(mContext, "Focused item: " + item.getSelectionKey());
        }

        @Override
        public boolean hasFocusedItem() {
            return false;
        }

        @Override
        public int getFocusedPosition() {
            return 0;
        }

        @Override
        public void clearFocus() {
            toast(mContext, "Cleared focus.");
        }
    }

    // Implementation of MouseInputHandler.Callbacks allows handling
    // of higher level events, like onActivated.
    private static final class MouseCallbacks extends
            androidx.recyclerview.selection.MouseCallbacks {

        private final Context mContext;

        MouseCallbacks(Context context) {
            mContext = context;
        }

        @Override
        public boolean onContextClick(MotionEvent e) {
            toast(mContext, "Context click received.");
            return true;
        }
    };

    private static final class TouchCallbacks extends
            androidx.recyclerview.selection.TouchCallbacks {

        private final Context mContext;

        private TouchCallbacks(Context context) {
            mContext = context;
        }

        public boolean onDragInitiated(MotionEvent e) {
            toast(mContext, "onDragInitiated received.");
            return true;
        }
    }
}
