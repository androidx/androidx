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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.SelectionTracker.SelectionObserver;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.supportv7.R;

/**
 * ContentPager demo activity.
 */
public class FancySelectionDemoActivity extends AppCompatActivity {

    private static final String TAG = "SelectionDemos";
    private static final String EXTRA_COLUMN_COUNT = "demo-column-count";

    private RecyclerView mRecView;
    private FancySelectionDemoAdapter mAdapter;
    private SelectionTracker<Uri> mSelectionTracker;

    private GridLayoutManager mLayout;
    private int mColumnCount = 1;  // This will get updated when layout changes.
    private boolean mIterceptListenerEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.selection_demo_layout);
        mRecView = (RecyclerView) findViewById(R.id.list);

        // If you want to provided special handling of clicks on items
        // in RecyclerView (respond to a play button, or show a menu
        // when a three-dot menu is clicked) you can't just add an OnClickListener
        // to the View.  This is because Selection lib installs an
        // OnItemTouchListener w/ RecyclerView, and that listener eats
        // up many of the touch/mouse events RecyclerView sends its way.
        // To work around this install your own OnItemTouchListener *before*
        // you build your SelectionTracker instance. That'll give your listener
        // a chance to intercept events before Selection lib gobbles them up.
        mRecView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                return mIterceptListenerEnabled
                        && FancyHeaderHolder.isHeader(rv.findChildViewUnder(e.getX(), e.getY()));
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                toast(FancySelectionDemoActivity.this, "Clicked on a header!");
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            }
        });

        mLayout = new GridLayoutManager(this, mColumnCount);
        mRecView.setLayoutManager(mLayout);
        mAdapter = new FancySelectionDemoAdapter(this);
        mRecView.setAdapter(mAdapter);
        ItemKeyProvider<Uri> keyProvider = mAdapter.getItemKeyProvider();

        SelectionTracker.Builder<Uri> builder = new SelectionTracker.Builder<>(
                "fancy-demo",
                mRecView,
                keyProvider,
                new FancyDetailsLookup(mRecView),
                StorageStrategy.createParcelableStorage(Uri.class));

        // Override default behaviors and build in multi select mode.
        // Call .withSelectionPredicate(SelectionTracker.SelectionPredicate.SINGLE_ANYTHING)
        // for single selection mode.
        mSelectionTracker = builder
                .withSelectionPredicate(SelectionPredicates.createSelectAnything())
                .withOnDragInitiatedListener(new OnDragInitiatedListener(this))
                .withOnContextClickListener(new OnContextClickListener())
                .withOnItemActivatedListener(new OnItemActivatedListener(this))
                .withFocusDelegate(new FocusDelegate())
                .withBandOverlay(R.drawable.selection_demo_band_overlay)
                .build();

        // Lazily bind SelectionTracker. Allows us to defer initialization of the
        // SelectionTracker dependency until after the adapter is created.
        mAdapter.bindSelectionHelper(mSelectionTracker);

        // TODO: Glue selection to ActionMode, since that'll be a common practice.
        mSelectionTracker.addObserver(
                new SelectionObserver<Uri>() {
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
        menu.findItem(R.id.option_menu_enable_listener)
                .setEnabled(!mIterceptListenerEnabled);
        menu.findItem(R.id.option_menu_disable_listener)
                .setEnabled(mIterceptListenerEnabled);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mIterceptListenerEnabled = !mIterceptListenerEnabled;
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        int selectionSize = mSelectionTracker.getSelection().size();
        if (selectionSize == 0) {
            return;
        }
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.selection_demo_item_actions, menu);

        MenuItem item = menu.findItem(R.id.option_menu_item_eat_single);
        item.setEnabled(selectionSize == 1);
        item.setVisible(selectionSize == 1);

        item = menu.findItem(R.id.option_menu_item_eat_multiple);
        item.setEnabled(selectionSize > 1);
        item.setVisible(selectionSize > 1);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_menu_item_eat_single:
            case R.id.option_menu_item_eat_multiple:
                toast(this, "Num, num, num...done!");
                return true;
            default:
                return super.onContextItemSelected(item);
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

    // Tracking focus separately from explicit selection
    // can be useful when providing a mouse friendly experience.
    // Observe the behavior of file managers for an example.
    private static final class FocusDelegate extends
            androidx.recyclerview.selection.FocusDelegate<Uri> {

        private ItemDetails<Uri> mFocusedItem;

        @Override
        public void focusItem(@NonNull ItemDetails<Uri> item) {
            mFocusedItem = item;
            Log.i(TAG, "focusItem called for " + item);
        }

        @Override
        public boolean hasFocusedItem() {
            return mFocusedItem != null;
        }

        @Override
        public int getFocusedPosition() {
            return mFocusedItem != null
                    ? mFocusedItem.getPosition()
                    : RecyclerView.NO_POSITION;
        }

        @Override
        public void clearFocus() {
            mFocusedItem = null;
        }
    }

    private static final class OnItemActivatedListener implements
            androidx.recyclerview.selection.OnItemActivatedListener<Uri> {

        private final Context mContext;

        OnItemActivatedListener(Context context) {
            mContext = context;
        }

        @Override
        public boolean onItemActivated(ItemDetails<Uri> item, MotionEvent e) {
            toast(mContext, "Activate item: " + item);
            return true;
        }
    }

    private final class OnContextClickListener implements
            androidx.recyclerview.selection.OnContextClickListener {

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public boolean onContextClick(MotionEvent e) {
            View view = mRecView.findChildViewUnder(e.getX(), e.getY());

            float x = e.getX() - view.getLeft();
            float y = e.getY() - view.getTop();

            registerForContextMenu(view);
            if (view.showContextMenu(x, y)) {
                Log.i(TAG,
                        "showContextMenu on view " + view.getClass().getSimpleName()
                                + " returned "
                                + "true for "
                                + "event: " + e);
            }
            unregisterForContextMenu(view);
            return true;
        }
    }

    private static final class OnDragInitiatedListener implements
            androidx.recyclerview.selection.OnDragInitiatedListener {

        private final Context mContext;

        private OnDragInitiatedListener(Context context) {
            mContext = context;
        }

        @Override
        public boolean onDragInitiated(MotionEvent e) {
            toast(mContext, "onDragInitiated received.");
            return true;
        }
    }
}
