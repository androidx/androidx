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

import static androidx.recyclerview.widget.ItemTouchHelper.RIGHT;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.SelectionTracker.SelectionObserver;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.example.android.supportv7.R;

/**
 * RecyclerView Selection library fancy demo activity. The fancy
 * demo includes support for both touch and mouse (band) driven selection.
 * Use this activity as your example when implementing an activity/fragment
 * that will run on a wide range of devices, including devices like ChromeOS
 * where a pointing device may be present, or even the sole means of input.
 *
 * <p>The key to an implementation that provides mouse support is
 * to provide an {@link ItemKeyProvider} that is
 * {@link ItemKeyProvider#SCOPE_MAPPED}. This means the key provider
 * can supply information about both position and item key at any time,
 * even when an item is not attached to the recycler view. See
 * {@link DemoAdapter.KeyProvider} for an example of a SCOPE_MAPPED
 * provider that uses simple {@link Uri}s as the keys.
 */
public class FancySelectionDemoActivity extends AppCompatActivity {

    private static final String TAG = "SelectionDemos";

    private RecyclerView mRecView;
    private DemoAdapter mAdapter;
    private SelectionTracker<Uri> mSelectionTracker;

    private GridLayoutManager mLayout;
    private boolean mIterceptListenerEnabled = false;
    private boolean mSwipeDuringSelectionEnabled = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.selection_demo_layout);
        mRecView = (RecyclerView) findViewById(R.id.list);

        // Demo how to intercept touch events before selection tracker.
        // In case you need to do something fancy that selection tracker
        // might otherwise interfere with.
        setupCustomTouchListener();

        mLayout = new GridLayoutManager(this, 1);
        mRecView.setLayoutManager(mLayout);
        mAdapter = new DemoAdapter(this);
        mRecView.setAdapter(mAdapter);
        ItemKeyProvider<Uri> keyProvider = mAdapter.getItemKeyProvider();
        final DemoDetailsLookup detailsLookup = new DemoDetailsLookup(mRecView);

        SelectionTracker.Builder<Uri> builder = new SelectionTracker.Builder<>(
                "fancy-demo",
                mRecView,
                keyProvider,
                detailsLookup,
                StorageStrategy.createParcelableStorage(Uri.class));

        // Build a multi-selection enabled tracker with support for many
        // mouse/keyboard centric niceties friendly to ChromeOS users
        // of your app.
        mSelectionTracker = builder
                .withSelectionPredicate(SelectionPredicates.createSelectAnything())
                // Allow users to drag a selection, can be initiated by long pressing
                // on existing selection, or click-dragging with a mouse.
                .withOnDragInitiatedListener(new OnDragInitiatedListener(this))
                // Respond to context clicks allows you to add options for mouse users.
                .withOnContextClickListener(new OnContextClickListener())
                .withOnItemActivatedListener(new OnItemActivatedListener(this))
                // Keep track of item focus which can aid in creating desirable
                // keyboard based experiences for users on laptops.
                .withFocusDelegate(new FocusDelegate())
                // Use a custom band overlay when mouse selection is active.
                // The library provides a default resource.
                .withBandOverlay(R.drawable.selection_demo_band_overlay)
                .build();

        // Lazily bind SelectionTracker. Allows us to defer initialization of the
        // SelectionTracker dependency until after the adapter is created.
        mAdapter.bindSelectionTracker(mSelectionTracker);

        // Adds a selection observer. You can use selection observer to enable
        // action mode in your app when there is an active selection.
        monitorSelectionChanges(mSelectionTracker);

        // Adds swipe support. Demoing how selection and ItemTouchHelper can work together.
        addSwipeSupport();

        // Restore selection from saved state.
        updateFromSavedState(savedInstanceState);
    }

    private void addSwipeSupport() {
        // Add swipe support, just because it's cool!
        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(
                new ItemTouchHelper.Callback() {
                    @Override
                    public int getMovementFlags(@NonNull RecyclerView recyclerView,
                            @NonNull ViewHolder viewHolder) {
                        // Possibly don't allow swipe during active selection.
                        if (mSelectionTracker.hasSelection() && !mSwipeDuringSelectionEnabled) {
                            return 0;
                        }

                        // Don't allow swipe on anything not an DemoItemHolder.
                        if (!(viewHolder instanceof DemoItemHolder)) {
                            return 0;
                        }

                        // Everything else is fair game.
                        return makeMovementFlags(0, RIGHT);
                    }

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                            @NonNull ViewHolder viewHolder,
                            @NonNull ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull ViewHolder viewHolder, int direction) {
                        if (viewHolder instanceof DemoItemHolder) {
                            ItemDetails<Uri> details =
                                    ((DemoItemHolder) viewHolder).getItemDetails();
                            mAdapter.removeItem(details.getSelectionKey());
                        }
                    }
                });
        itemTouchhelper.attachToRecyclerView(mRecView);
    }

    private void monitorSelectionChanges(SelectionTracker<Uri> selectionTracker) {
        // TODO: Glue selection to ActionMode, since that'll be a common practice.
        selectionTracker.addObserver(
                new SelectionObserver<Uri>() {
                    @Override
                    public void onItemStateChanged(@NonNull Uri key, boolean selected) {
                        Log.i(TAG,
                                String.format("Selection item `%s`state changed to %b", key,
                                        selected));
                    }

                    @Override
                    public void onSelectionRefresh() {
                        Log.i(TAG, "Selection refreshed as: " + selectionTracker.getSelection());
                    }

                    @Override
                    public void onSelectionChanged() {
                        Log.i(TAG, "Selection changed to: " + selectionTracker.getSelection());
                    }

                    @Override
                    public void onSelectionCleared() {
                        Log.i(TAG, "No more selection. :(");
                    }

                    @Override
                    public void onSelectionRestored() {
                        Log.i(TAG,
                                "Selection restored as: " + selectionTracker.getSelection());
                    }
                });
    }

    // If you want to provided special handling of clicks on items
    // in RecyclerView (respond to a play button, or show a menu
    // when a three-dot menu is clicked) you can't just add an OnClickListener
    // to the View.  This is because Selection lib installs an
    // OnItemTouchListener w/ RecyclerView, and that listener eats
    // up many of the touch/mouse events RecyclerView sends its way.
    // To work around this install your own OnItemTouchListener *before*
    // you build your SelectionTracker instance. That'll give your listener
    // a chance to intercept events before Selection lib gobbles them up.
    private void setupCustomTouchListener() {
        mRecView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                return mIterceptListenerEnabled
                        && DemoHeaderHolder.isHeader(rv.findChildViewUnder(e.getX(), e.getY()));
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                toast(FancySelectionDemoActivity.this, "Clicked on a header!");
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        mSelectionTracker.onSaveInstanceState(state);
    }

    private void updateFromSavedState(Bundle state) {
        mSelectionTracker.onRestoreInstanceState(state);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean showMenu = super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.selection_demo_actions, menu);
        for (int i = 0; i < menu.size(); i++) {
            updateOptionFromMenu(menu.getItem(i));
        }
        return showMenu;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        item.setChecked(!item.isChecked());
        updateOptionFromMenu(item);
        return true;
    }

    private void updateOptionFromMenu(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_menu_custom_listener:
                mIterceptListenerEnabled = item.isChecked();
                break;
            case R.id.option_menu_swipe_during_select:
                mSwipeDuringSelectionEnabled = item.isChecked();
                break;
        }
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
        public boolean onItemActivated(@NonNull ItemDetails<Uri> item, @NonNull MotionEvent e) {
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
        public boolean onDragInitiated(@NonNull MotionEvent e) {
            toast(mContext, "onDragInitiated received.");
            return true;
        }
    }
}
