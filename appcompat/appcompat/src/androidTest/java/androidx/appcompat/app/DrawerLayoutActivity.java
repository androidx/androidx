/*
 * Copyright (C) 2015 The Android Open Source Project
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

package androidx.appcompat.app;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.test.R;
import androidx.appcompat.testutils.BaseTestActivity;
import androidx.appcompat.testutils.Shakespeare;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

/**
 * Test activity for testing various APIs and interactions for DrawerLayout. It follows
 * a common usage of the DrawerLayout widget combined with Toolbar in the Android support library
 * that respect the
 * <a href="https://www.google.com/design/spec/patterns/navigation-drawer.html">Material design
 * guidelines</a> for the drawer component.
 */
public class DrawerLayoutActivity extends BaseTestActivity {
    private DrawerLayout mDrawerLayout;
    private ListView mDrawer;
    private TextView mContent;

    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar mToolbar;

    @Override
    protected int getContentViewLayoutResId() {
        return R.layout.drawer_layout;
    }

    @Override
    protected void onContentViewSet() {
        super.onContentViewSet();

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawer = findViewById(R.id.start_drawer);
        mContent = findViewById(R.id.content_text);

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        // The drawer title must be set in order to announce state changes when
        // accessibility is turned on. This is typically a simple description,
        // e.g. "Navigation".
        mDrawerLayout.setDrawerTitle(GravityCompat.START, getString(R.string.drawer_title));

        mDrawer.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                Shakespeare.TITLES));
        mDrawer.setOnItemClickListener(new DrawerItemClickListener());

        // Find the toolbar in our layout and set it as the support action bar on the activity.
        // This is required to have the drawer slide "over" the toolbar.
        mToolbar = findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.drawer_title);
        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);

        // ActionBarDrawerToggle provides convenient helpers for tying together the
        // prescribed interactions between a top-level sliding drawer and the action bar.
        // Note that, as the Javadocs of ActionBarDrawerToggle constructors say, we are
        // *not* using a constructor that gets a Toolbar since we're setting our toolbar
        // dynamically at runtime. Furthermore, as the drawer is sliding over the toolbar,
        // we are suppressing the morphing animation from hamburger to back arrow by
        // calling super.onDrawerSlide with slideOffset=0.0f. In case your app only has
        // top-level pages and doesn't need back arrow visuals at all, you can set up
        // your activity theme to have attribute named "drawerArrowStyle" that points
        // to an extension of Widget.AppCompat.DrawerArrowToggle that has its "spinBars"
        // attribute set to false.
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                super.onDrawerSlide(drawerView, 0.0f);
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, 0.0f);
            }
        };

        mDrawerLayout.addDrawerListener(mDrawerToggle);

        // Configure the background color fill of the system status bar (on supported platform
        // versions) and the toolbar itself. We're using the same color, and android:statusBar
        // from the theme makes the status bar slightly darker.
        final int metalBlueColor = getResources().getColor(R.color.drawer_sample_metal_blue);
        mDrawerLayout.setStatusBarBackgroundColor(metalBlueColor);
        mToolbar.setBackgroundColor(metalBlueColor);

        // Register a pre-draw listener to get the initial width of the DrawerLayout so
        // that we can determine the width of the drawer based on the Material spec at
        // https://www.google.com/design/spec/patterns/navigation-drawer.html#navigation-drawer-specs
        mDrawerLayout.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        // What is the width of the entire DrawerLayout?
                        final int drawerLayoutWidth = mDrawerLayout.getWidth();

                        // What is the action bar size?
                        final Resources.Theme theme = mDrawerLayout.getContext().getTheme();
                        final TypedArray a = theme.obtainStyledAttributes(
                                new int[] { androidx.appcompat.R.attr.actionBarSize });
                        final int actionBarSize = a.getDimensionPixelSize(0, 0);
                        if (a != null) {
                            a.recycle();
                        }

                        // Compute the width of the drawer and set it on the layout params.
                        final int idealDrawerWidth = 5 * actionBarSize;
                        final int maxDrawerWidth = Math.max(0, drawerLayoutWidth - actionBarSize);
                        final int drawerWidth = Math.min(idealDrawerWidth, maxDrawerWidth);

                        final DrawerLayout.LayoutParams drawerLp =
                                (DrawerLayout.LayoutParams) mDrawer.getLayoutParams();
                        drawerLp.width = drawerWidth;
                        mDrawer.setLayoutParams(drawerLp);

                        // Remove ourselves as the pre-draw listener since this is a one-time
                        // configuration.
                        mDrawerLayout.getViewTreeObserver().removeOnPreDrawListener(this);
                        return true;
                    }
                });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*
         * The action bar home/up action should open or close the drawer.
         * The drawer toggle will take care of this.
         */
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Is the drawer open?
        if (mDrawerLayout.isDrawerOpen(mDrawer)) {
            // Close the drawer and return.
            mDrawerLayout.closeDrawer(mDrawer);
            return;
        }

        super.onBackPressed();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * This list item click listener implements very simple view switching by changing
     * the primary content text. The drawer is closed when a selection is made.
     */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mContent.setText(Shakespeare.DIALOGUE[position]);
            mToolbar.setTitle(Shakespeare.TITLES[position]);
            mDrawerLayout.closeDrawer(mDrawer);
        }
    }
}
