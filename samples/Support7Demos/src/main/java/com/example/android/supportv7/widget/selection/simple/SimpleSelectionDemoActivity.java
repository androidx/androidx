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

package com.example.android.supportv7.widget.selection.simple;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.SelectionTracker.SelectionObserver;
import androidx.recyclerview.selection.StableIdKeyProvider;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.supportv7.R;

/**
 * ContentPager demo activity.
 */
public class SimpleSelectionDemoActivity extends AppCompatActivity {

    private static final String TAG = "SelectionDemos";
    private static final String EXTRA_COLUMN_COUNT = "demo-column-count";

    private SimpleSelectionDemoAdapter mAdapter;
    private SelectionTracker<Long> mSelectionTracker;

    private GridLayoutManager mLayout;
    private int mColumnCount = 1;  // This will get updated when layout changes.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.selection_demo_layout);
        RecyclerView recView = (RecyclerView) findViewById(R.id.list);

        // keyProvider depends on mAdapter.setHasStableIds(true).
        ItemKeyProvider<Long> keyProvider = new StableIdKeyProvider(recView);

        mLayout = new GridLayoutManager(this, mColumnCount);
        recView.setLayoutManager(mLayout);

        mAdapter = new SimpleSelectionDemoAdapter(this, keyProvider);
        // The adapter is paired with a key provider that supports
        // the native RecyclerView stableId. For this to work correctly
        // the adapter must report that it supports stable ids.
        mAdapter.setHasStableIds(true);

        recView.setAdapter(mAdapter);

        SelectionTracker.Builder<Long> builder = new SelectionTracker.Builder<>(
                "simple-demo",
                recView,
                keyProvider,
                new DemoDetailsLookup(recView),
                StorageStrategy.createLongStorage());

        // Override default behaviors and build in multi select mode.
        // Call .withSelectionPredicate(SelectionTracker.SelectionPredicate.SINGLE_ANYTHING)
        // for single selection mode.
        mSelectionTracker = builder
                .withSelectionPredicate(SelectionPredicates.createSelectAnything())
                .withOnDragInitiatedListener(new OnDragInitiatedListener(this))
                .withOnContextClickListener(new OnContextClickListener(this))
                .withOnItemActivatedListener(new OnItemActivatedListener(this))
                .withFocusDelegate(new FocusDelegate(this))
                .withBandOverlay(R.drawable.selection_demo_band_overlay)
                .build();

        // Lazily bind SelectionTracker. Allows us to defer initialization of the
        // SelectionTracker dependency until after the adapter is created.
        mAdapter.bindSelectionHelper(mSelectionTracker);

        // TODO: Glue selection to ActionMode, since that'll be a common practice.
        mSelectionTracker.addObserver(
                new SelectionObserver<Long>() {
                    @Override
                    public void onSelectionChanged() {
                        Log.i(TAG, "Selection changed to: " + mSelectionTracker.getSelection());
                    }
                });

        // Restore selection from saved state.
        updateFromSavedState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        mSelectionTracker.onSaveInstanceState(state);
        state.putInt(EXTRA_COLUMN_COUNT, mColumnCount);
    }

    private void updateFromSavedState(Bundle state) {
        mSelectionTracker.onRestoreInstanceState(state);

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
        if (mSelectionTracker.clearSelection()) {
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
        mSelectionTracker.clearSelection();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAdapter.loadData();
    }

    // Implementation of MouseInputHandler.Callbacks allows handling
    // of higher level events, like onActivated.
    private static final class OnItemActivatedListener implements
            androidx.recyclerview.selection.OnItemActivatedListener<Long> {

        private final Context mContext;

        OnItemActivatedListener(Context context) {
            mContext = context;
        }

        @Override
        public boolean onItemActivated(ItemDetails<Long> item, MotionEvent e) {
            toast(mContext, "Activate item: " + item.getSelectionKey());
            return true;
        }
    }

    private static final class FocusDelegate extends
            androidx.recyclerview.selection.FocusDelegate<Long> {

        private final Context mContext;

        private FocusDelegate(Context context) {
            mContext = context;
        }

        @Override
        public void focusItem(ItemDetails<Long> item) {
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
    private static final class OnContextClickListener implements
            androidx.recyclerview.selection.OnContextClickListener {

        private final Context mContext;

        OnContextClickListener(Context context) {
            mContext = context;
        }

        @Override
        public boolean onContextClick(MotionEvent e) {
            toast(mContext, "Context click received.");
            return true;
        }
    };

    private static final class OnDragInitiatedListener implements
            androidx.recyclerview.selection.OnDragInitiatedListener {

        private final Context mContext;

        private OnDragInitiatedListener(Context context) {
            mContext = context;
        }

        public boolean onDragInitiated(MotionEvent e) {
            toast(mContext, "onDragInitiated received.");
            return true;
        }
    }
}
