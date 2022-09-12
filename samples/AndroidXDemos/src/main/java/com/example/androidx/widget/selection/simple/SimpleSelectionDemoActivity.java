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

package com.example.androidx.widget.selection.simple;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

import com.example.androidx.R;

/**
 * RecyclerView Selection library simple demo activity.
 */
public class SimpleSelectionDemoActivity extends AppCompatActivity {

    private static final String TAG = "SelectionDemos";

    private DemoAdapter mAdapter;
    private SelectionTracker<Long> mSelectionTracker;
    private GridLayoutManager mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.selection_demo_layout);
        RecyclerView recView = findViewById(R.id.list);

        mLayout = new GridLayoutManager(this, 1);
        recView.setLayoutManager(mLayout);

        mAdapter = new DemoAdapter(this);
        // The adapter is paired with a key provider that supports
        // the native RecyclerView stableId. For this to work correctly
        // the adapter must report that it supports stable ids.
        mAdapter.setHasStableIds(true);

        recView.setAdapter(mAdapter);

        // keyProvider depends on mAdapter.setHasStableIds(true).
        ItemKeyProvider<Long> keyProvider = new StableIdKeyProvider(recView);

        SelectionTracker.Builder<Long> builder = new SelectionTracker.Builder<>(
                "simple-demo",
                recView,
                keyProvider,
                new DemoDetailsLookup(recView),
                StorageStrategy.createLongStorage());

        mSelectionTracker = builder
                .withSelectionPredicate(SelectionPredicates.createSelectAnything())
                .withOnItemActivatedListener(new OnItemActivatedListener(this))
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
    protected void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        mSelectionTracker.onSaveInstanceState(state);
    }

    private void updateFromSavedState(Bundle state) {
        mSelectionTracker.onRestoreInstanceState(state);
    }

    @SuppressWarnings("deprecation")
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
        public boolean onItemActivated(@NonNull ItemDetails<Long> item, @NonNull MotionEvent e) {
            toast(mContext, "Activate item: " + item.getSelectionKey());
            return true;
        }
    }
}
