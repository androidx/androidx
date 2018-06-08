/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.car.drawer;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.AnimRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.car.R;
import androidx.car.widget.PagedListView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayDeque;

/**
 * A controller that will handle the set up of the navigation drawer. It will hook up the
 * necessary buttons for up navigation, as well as expose methods to allow for a drill down
 * navigation.
 */
public class CarDrawerController {
    /** An animation for when a user navigates into a submenu. */
    @AnimRes
    private static final int DRILL_DOWN_ANIM = R.anim.fade_in_trans_right_layout_anim;

    /** An animation for when a user navigates up (when the back button is pressed). */
    @AnimRes
    private static final int NAVIGATE_UP_ANIM = R.anim.fade_in_trans_left_layout_anim;

    /**
     * A representation of the hierarchy of navigation being displayed in the list. The ordering of
     * this stack is the order that the user has visited each level. When the user navigates up,
     * the adapters are popped from this list.
     */
    private final ArrayDeque<CarDrawerAdapter> mAdapterStack = new ArrayDeque<>();

    private final Context mContext;

    private final TextView mTitleView;
    private final DrawerLayout mDrawerLayout;
    private final ActionBarDrawerToggle mDrawerToggle;

    private final PagedListView mDrawerList;
    private final ProgressBar mProgressBar;

    /**
     * @deprecated Use {@link #CarDrawerController(DrawerLayout, ActionBarDrawerToggle)} instead.
     *             The {@code Toolbar} is no longer needed and will be ignored.
     */
    @Deprecated
    public CarDrawerController(@Nullable Toolbar toolbar, @NonNull DrawerLayout drawerLayout,
            @NonNull ActionBarDrawerToggle drawerToggle) {
        this(drawerLayout, drawerToggle);
    }

    /**
     * Creates a {@link CarDrawerController} that will control the navigation of the drawer given by
     * {@code drawerLayout}.
     *
     * <p>The given {@code drawerLayout} should either have a child View that is inflated from
     * {@code R.layout.car_drawer} or ensure that it three children that have the IDs found in that
     * layout.
     *
     * @param drawerLayout The top-level container for the window content that shows the
     *                     interactive drawer.
     * @param drawerToggle The {@link ActionBarDrawerToggle} that will open the drawer.
     */
    public CarDrawerController(@NonNull DrawerLayout drawerLayout,
            @NonNull ActionBarDrawerToggle drawerToggle) {
        mContext = drawerLayout.getContext();
        mDrawerToggle = drawerToggle;
        mDrawerLayout = drawerLayout;

        mTitleView = drawerLayout.findViewById(R.id.drawer_title);
        mDrawerList = drawerLayout.findViewById(R.id.drawer_list);
        mDrawerList.setMaxPages(PagedListView.ItemCap.UNLIMITED);
        mProgressBar = drawerLayout.findViewById(R.id.drawer_progress);

        drawerLayout.findViewById(R.id.drawer_back_button).setOnClickListener(v -> {
            if (!maybeHandleUpClick()) {
                closeDrawer();
            }
        });

        setupDrawerToggling();
    }

    /**
     * Sets the {@link CarDrawerAdapter} that will function as the root adapter. The contents of
     * this root adapter are shown when the drawer is first opened. It is also the top-most level of
     * navigation in the drawer.
     *
     * @param rootAdapter The adapter that will act as the root. If this value is {@code null}, then
     *                    this method will do nothing.
     */
    public void setRootAdapter(@Nullable CarDrawerAdapter rootAdapter) {
        if (rootAdapter == null) {
            return;
        }

        // The root adapter is always the last item in the stack.
        if (!mAdapterStack.isEmpty()) {
            mAdapterStack.removeLast();
        }
        mAdapterStack.addLast(rootAdapter);
        setDisplayAdapter(rootAdapter);
    }

    /**
     * Switches to use the given {@link CarDrawerAdapter} as the one to supply the list to display
     * in the navigation drawer. The title will also be updated from the adapter.
     *
     * <p>This switch is treated as a navigation to the next level in the drawer. Navigation away
     * from this level will pop the given adapter off and surface contents of the previous adapter
     * that was set via this method. If no such adapter exists, then the root adapter set by
     * {@link #setRootAdapter(CarDrawerAdapter)} will be used instead.
     *
     * @param adapter Adapter for next level of content in the drawer.
     */
    public final void pushAdapter(CarDrawerAdapter adapter) {
        mAdapterStack.peek().setTitleChangeListener(null);
        mAdapterStack.push(adapter);
        setDisplayAdapter(adapter);
        runLayoutAnimation(DRILL_DOWN_ANIM);
    }

    /** Close the drawer. */
    public void closeDrawer() {
        if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
            mDrawerLayout.closeDrawer(Gravity.LEFT);
        }
    }

    /** Opens the drawer. */
    public void openDrawer() {
        if (!mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
            mDrawerLayout.openDrawer(Gravity.LEFT);
        }
    }

    /** Sets a listener to be notified of Drawer events. */
    public void addDrawerListener(@NonNull DrawerLayout.DrawerListener listener) {
        mDrawerLayout.addDrawerListener(listener);
    }

    /** Removes a listener to be notified of Drawer events. */
    public void removeDrawerListener(@NonNull DrawerLayout.DrawerListener listener) {
        mDrawerLayout.removeDrawerListener(listener);
    }

    /**
     * Sets whether the loading progress bar is displayed in the navigation drawer. If {@code true},
     * the progress bar is displayed and the navigation list is hidden and vice versa.
     */
    public void showLoadingProgressBar(boolean show) {
        mDrawerList.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
        mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /** Scroll to given position in the list. */
    public void scrollToPosition(int position) {
        mDrawerList.getRecyclerView().smoothScrollToPosition(position);
    }

    /**
     * Retrieves the title from the given {@link CarDrawerAdapter} and set its as the title of this
     * controller's internal Toolbar.
     */
    private void setToolbarTitleFrom(CarDrawerAdapter adapter) {
        mTitleView.setText(adapter.getTitle());
        adapter.setTitleChangeListener(mTitleView::setText);
    }

    /**
     * Sets up the necessary listeners for {@link DrawerLayout} so that the navigation drawer
     * hierarchy is properly displayed.
     */
    private void setupDrawerToggling() {
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerLayout.addDrawerListener(
                new DrawerLayout.DrawerListener() {
                    @Override
                    public void onDrawerSlide(View drawerView, float slideOffset) {}

                    @Override
                    public void onDrawerClosed(View drawerView) {
                        // If drawer is closed, revert stack/drawer to initial root state.
                        cleanupStackAndShowRoot();
                        scrollToPosition(0);
                    }

                    @Override
                    public void onDrawerOpened(View drawerView) {}

                    @Override
                    public void onDrawerStateChanged(int newState) {}
                });
    }

    /**
     * Synchronizes the display of the drawer with its linked {@link DrawerLayout}.
     *
     * <p>This should be called from the associated Activity's
     * {@link androidx.appcompat.app.AppCompatActivity#onPostCreate(Bundle)} method to synchronize
     * after teh DRawerLayout's instance state has been restored, and any other time when the
     * state may have diverged in such a way that this controller's associated
     * {@link ActionBarDrawerToggle} had not been notified.
     */
    public void syncState() {
        mDrawerToggle.syncState();
    }

    /**
     * Notify this controller that device configurations may have changed.
     *
     * <p>This method should be called from the associated Activity's
     * {@code onConfigurationChanged()} method.
     */
    public void onConfigurationChanged(Configuration newConfig) {
        // Pass any configuration change to the drawer toggle.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Sets the given adapter as the one displaying the current contents of the drawer.
     *
     * <p>The drawer's title will also be derived from the given adapter.
     */
    private void setDisplayAdapter(CarDrawerAdapter adapter) {
        setToolbarTitleFrom(adapter);
        // NOTE: We don't use swapAdapter() since different levels in the Drawer may switch between
        // car_drawer_list_item_normal, car_drawer_list_item_small and car_list_empty layouts.
        mDrawerList.getRecyclerView().setAdapter(adapter);
    }

    /**
     * An analog to an Activity's {@code onOptionsItemSelected()}. This method should be called
     * when the Activity's method is called and will return {@code true} if the selection has
     * been handled.
     *
     * @return {@code true} if the item processing was handled by this class.
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle home-click and see if we can navigate up in the drawer.
        if (item != null && item.getItemId() == android.R.id.home && maybeHandleUpClick()) {
            return true;
        }

        // DrawerToggle gets next chance to handle up-clicks (and any other clicks).
        return mDrawerToggle.onOptionsItemSelected(item);
    }

    /**
     * Switches to the previous level in the drawer hierarchy if the current list being displayed
     * is not the root adapter. This is analogous to a navigate up.
     *
     * @return {@code true} if a navigate up was possible and executed. {@code false} otherwise.
     */
    private boolean maybeHandleUpClick() {
        // Check if already at the root level.
        if (mAdapterStack.size() <= 1) {
            return false;
        }

        CarDrawerAdapter adapter = mAdapterStack.pop();
        adapter.setTitleChangeListener(null);
        adapter.cleanup();
        setDisplayAdapter(mAdapterStack.peek());
        runLayoutAnimation(NAVIGATE_UP_ANIM);
        return true;
    }

    /** Clears stack down to root adapter and switches to root adapter. */
    private void cleanupStackAndShowRoot() {
        while (mAdapterStack.size() > 1) {
            CarDrawerAdapter adapter = mAdapterStack.pop();
            adapter.setTitleChangeListener(null);
            adapter.cleanup();
        }
        setDisplayAdapter(mAdapterStack.peek());
        runLayoutAnimation(NAVIGATE_UP_ANIM);
    }

    /**
     * Runs the given layout animation on the PagedListView. Running this animation will also
     * refresh the contents of the list.
     */
    private void runLayoutAnimation(@AnimRes int animation) {
        RecyclerView recyclerView = mDrawerList.getRecyclerView();
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(mContext, animation));
        recyclerView.getAdapter().notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();
    }
}
