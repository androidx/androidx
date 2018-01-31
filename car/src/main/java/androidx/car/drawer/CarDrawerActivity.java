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

import android.animation.ValueAnimator;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.car.R;
import androidx.car.widget.ClickThroughToolbar;

/**
 * Common base Activity for car apps that need to present a Drawer.
 *
 * <p>This Activity manages the overall layout. To use it, sub-classes need to:
 *
 * <ul>
 *   <li>Provide the root-items for the drawer by calling {@link #getDrawerController()}.
 *       {@link CarDrawerController#setRootAdapter(CarDrawerAdapter)}.
 *   <li>Add their main content using {@link #setMainContent(int)} or {@link #setMainContent(View)}.
 *       They can also add fragments to the main-content container by obtaining its id using
 *       {@link #getContentContainerId()}
 * </ul>
 *
 * <p>This class will take care of drawer toggling and display.
 *
 * <p>This Activity also exposes the ability to have its toolbar optionally hide if any content
 * in its main view is scrolled. Be default, this ability is turned off. Call
 * {@link #setToolbarCollapsible()} to enable this behavior. Additionally, a user can set elevation
 * on this toolbar by calling the appropriate {@link #setToolbarElevation(float)} method. There is
 * elevation on the toolbar by default.
 *
 * <p>The rootAdapter can implement nested-navigation, in its click-handling, by passing the
 * CarDrawerAdapter for the next level to
 * {@link CarDrawerController#pushAdapter(CarDrawerAdapter)}.
 *
 * <p>Any Activity's based on this class need to set their theme to CarDrawerActivityTheme or a
 * derivative.
 */
public class CarDrawerActivity extends AppCompatActivity {
    private static final int ANIMATION_DURATION_MS = 100;
    private static final float DRAWER_OPEN_OFFSET = 0.25f;

    private CarDrawerController mDrawerController;
    private AppBarLayout mAppBarLayout;

    /**
     * Whether or not the drawer is considered opened. This value is only set and unset when the
     * drawer has passed the {@link #DRAWER_OPEN_OFFSET}.
     */
    private boolean mDrawerOpen;
    private float mAppBarElevation;

    private ClickThroughToolbar mToolbar;
    private boolean mToolbarCollapsible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.car_drawer_activity);

        mAppBarLayout = findViewById(R.id.appbar);
        mAppBarLayout.setBackgroundColor(getThemeColorPrimary());
        setToolbarElevation(getResources().getDimension(R.dimen.car_app_bar_default_elevation));

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                this /* activity */,
                drawerLayout, /* DrawerLayout object */
                R.string.car_drawer_open,
                R.string.car_drawer_close);

        mToolbar = findViewById(R.id.car_toolbar);
        setSupportActionBar(mToolbar);

        mDrawerController = new CarDrawerController(mToolbar, drawerLayout, drawerToggle);
        CarDrawerAdapter rootAdapter = getRootAdapter();
        if (rootAdapter != null) {
            mDrawerController.setRootAdapter(rootAdapter);
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mDrawerController.addDrawerListener(new DrawerLayout.DrawerListener() {
            private final int mClosedDrawerToolbarColor = getThemeColorPrimary();

            @Override
            public void onDrawerSlide(View view, float slideOffset) {
                if (slideOffset >= DRAWER_OPEN_OFFSET) {
                    // If this is the first time the drawer is considered open, then save the
                    // value of the elevation to restore later.
                    if (!mDrawerOpen) {
                        mDrawerOpen = true;
                        mAppBarElevation = mAppBarLayout.getElevation();
                    }

                    mAppBarLayout.setBackgroundColor(Color.TRANSPARENT);
                    setToolbarElevation(0);
                    setToolbarAlwaysShowInternal();
                } else if (mDrawerOpen) {
                    // Only reset the state of the AppBar if the drawer has reached the open state.
                    mDrawerOpen = false;
                    mAppBarLayout.setBackgroundColor(mClosedDrawerToolbarColor);
                    setToolbarElevation(mAppBarElevation);

                    if (mToolbarCollapsible) {
                        setToolbarCollapsibleInternal();
                    }
                }
            }

            @Override
            public void onDrawerOpened(View view) {}

            @Override
            public void onDrawerClosed(View view) {}

            @Override
            public void onDrawerStateChanged(int i) {}
        });
    }

    /**
     * Returns the {@link CarDrawerController} that is responsible for handling events relating
     * to the drawer in this Activity.
     *
     * @return The {@link CarDrawerController} linked to this Activity. This value will be
     * {@code null} if this method is called before {@code onCreate()} has been called.
     */
    @Nullable
    protected CarDrawerController getDrawerController() {
        return mDrawerController;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerController.syncState();
    }

    /**
     * @return Adapter for root content of the Drawer.
     * @deprecated Do not implement this, instead call {@link #getDrawerController}.
     * {@link CarDrawerController#setRootAdapter(CarDrawerAdapter)} directly.
     */
    @Deprecated
    @Nullable
    protected CarDrawerAdapter getRootAdapter() {
        return null;
    }

    /**
     * Set main content to display in this Activity. It will be added to R.id.content_frame in
     * car_drawer_activity.xml. NOTE: Do not use {@link #setContentView(View)}.
     *
     * @param view View to display as main content.
     */
    public void setMainContent(View view) {
        ViewGroup parent = findViewById(getContentContainerId());
        parent.addView(view);
    }

    /**
     * Set main content to display in this Activity. It will be added to R.id.content_frame in
     * car_drawer_activity.xml. NOTE: Do not use {@link #setContentView(int)}.
     *
     * @param resourceId Layout to display as main content.
     */
    public void setMainContent(@LayoutRes int resourceId) {
        ViewGroup parent = findViewById(getContentContainerId());
        LayoutInflater inflater = getLayoutInflater();
        inflater.inflate(resourceId, parent, true);
    }

    /**
     * Sets whether clicks on the toolbar of this view will pass through to any views underneath it.
     * By default, the toolbar will not allow clicks to pass through. This is the equivalent of
     * passing {@code false} to this method.
     *
     * @param clickThrough {@code true} if clicks will pass through; {@code false} for the toolbar
     *                     to eat all clicks.
     */
    public void setToolbarClickThrough(boolean clickThrough) {
        mToolbar.setClickPassThrough(clickThrough);
    }

    /**
     * Sets the elevation on the toolbar of this Activity.
     *
     * @param elevation The elevation to set.
     */
    public void setToolbarElevation(float elevation) {
        // The AppBar's default animator needs to be set to null to manually change the elevation.
        mAppBarLayout.setStateListAnimator(null);
        mAppBarLayout.setElevation(elevation);
    }

    /**
     * Sets the elevation of the toolbar and animate it from the current elevation value.
     *
     * @param elevation The elevation to set.
     */
    public void setToolbarElevationWithAnimation(float elevation) {
        ValueAnimator elevationAnimator =
                ValueAnimator.ofFloat(mAppBarLayout.getElevation(), elevation);
        elevationAnimator
                .setDuration(ANIMATION_DURATION_MS)
                .addUpdateListener(animation -> setToolbarElevation(
                        (float) animation.getAnimatedValue()));
        elevationAnimator.start();
    }

    /**
     * Sets the toolbar of this Activity as collapsible. When any content in the main view of the
     * Activity is scrolled, the toolbar will collapse and show itself accordingly.
     */
    public void setToolbarCollapsible() {
        mToolbarCollapsible = true;
        setToolbarCollapsibleInternal();
    }

    /**
     * An internal-use method that sets the toolbar as collapsible. This version of the method
     * does not override {@link #mToolbarCollapsible}, and thus retains whatever value a user
     * of this Activity has set.
     */
    private void setToolbarCollapsibleInternal() {
        AppBarLayout.LayoutParams params =
                (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
        params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
    }

    /**
     * Sets the toolbar to always show even if content in the main view of the Activity has been
     * scrolled. This is the default behavior.
     */
    public void setToolbarAlwaysShow() {
        mToolbarCollapsible = false;
        setToolbarAlwaysShowInternal();
    }

    /**
     * An internal-use method that sets the toolbar to always show. This version of the method does
     * not override {@link #mToolbarCollapsible}, and thus retains whatever value a user of this
     * Activity has set.
     */
    private void setToolbarAlwaysShowInternal() {
        AppBarLayout.LayoutParams params =
                (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
        params.setScrollFlags(0);
    }

    /**
     * Get the id of the main content Container which is a FrameLayout. Subclasses can add their own
     * content/fragments inside here.
     *
     * @return Id of FrameLayout where main content of the subclass Activity can be added.
     */
    protected int getContentContainerId() {
        return R.id.content_frame;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDrawerController.closeDrawer();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerController.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerController.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    /**
     * Returns the color that has been set as {@code colorPrimary} on the current Theme of this
     * Activity.
     */
    private int getThemeColorPrimary() {
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorPrimary, value, true);
        return value.data;
    }
}
