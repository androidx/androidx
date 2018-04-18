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

package com.example.android.supportv7.widget;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.android.supportv7.R;
import com.example.android.supportv7.Shakespeare;

/**
 * This example illustrates a common usage of the DrawerLayout widget combined with Toolbar
 * in the Android support library that respect the
 * <a href="https://www.google.com/design/spec/patterns/navigation-drawer.html">Material design
 * guidelines</a> for the drawer component.
 *
 *
 * <p>A DrawerLayout should be positioned at the top of your view hierarchy, placing it
 * below the action bar but above your content views. The primary content should match_parent
 * in both dimensions. Each drawer should define a reasonable width and match_parent for height.
 * Drawer views should be positioned after the content view in your layout to preserve proper
 * ordering.</p>
 *
 * <p>When a navigation (left) drawer is present, the host activity should detect presses of
 * the action bar's Up affordance as a signal to open and close the navigation drawer.
 * Items within the drawer should fall into one of two categories.</p>
 *
 * <ul>
 *     <li><strong>View switches</strong>. A view switch follows the same basic policies as
 *     list or tab navigation in that a view switch does not create navigation history.
 *     This pattern should only be used at the root activity of a task, leaving some form
 *     of Up navigation active for activities further down the navigation hierarchy.</li>
 *     <li><strong>Selective Up</strong>. The drawer allows the user to choose an alternate
 *     parent for Up navigation. This allows a user to jump across an app's navigation
 *     hierarchy at will. The application should treat this as it treats Up navigation from
 *     a different task, replacing the current task stack using TaskStackBuilder or similar.
 *     This is the only form of navigation drawer that should be used outside of the root
 *     activity of a task.</li>
 * </ul>
 *
 * <p>Right side drawers should be used for actions, not navigation. This follows the pattern
 * established by the Action Bar that navigation should be to the left and actions to the right.
 * An action should be an operation performed on the current contents of the window,
 * for example enabling or disabling a data overlay on top of the current content.</p>
 *
 * <p>When the drawer is open, it is above the application toolbar. On Lollipop versions of the
 * platform and above the drawer spans the full height of the screen, including behind the system
 * status bar.</p>
 */
public class DrawerLayoutActivity extends AppCompatActivity {
    private DrawerLayout mDrawerLayout;
    private ListView mStartDrawer;
    private FrameLayout mEndDrawer;
    private TextView mContent;

    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.drawer_layout);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mStartDrawer = findViewById(R.id.start_drawer);
        mEndDrawer = findViewById(R.id.end_drawer);
        mContent = findViewById(R.id.content_text);

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow_end, GravityCompat.END);

        // The drawer title must be set in order to announce state changes when
        // accessibility is turned on. This is typically a simple description,
        // e.g. "Navigation".
        mDrawerLayout.setDrawerTitle(GravityCompat.START, getString(R.string.drawer_title));

        mStartDrawer.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                Shakespeare.TITLES));
        mStartDrawer.setOnItemClickListener(new DrawerItemClickListener());

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
        final int metalBlueColor = ContextCompat.getColor(this, R.color.drawer_sample_metal_blue);
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

                        final DrawerLayout.LayoutParams startDrawerLp =
                                (DrawerLayout.LayoutParams) mStartDrawer.getLayoutParams();
                        startDrawerLp.width = drawerWidth;
                        mStartDrawer.setLayoutParams(startDrawerLp);

                        final DrawerLayout.LayoutParams endDrawerLp =
                                (DrawerLayout.LayoutParams) mEndDrawer.getLayoutParams();
                        endDrawerLp.width = drawerWidth;
                        mEndDrawer.setLayoutParams(endDrawerLp);

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
        boolean hadOpenDrawer = false;
        // Is the start drawer open?
        if (mDrawerLayout.isDrawerOpen(mStartDrawer)) {
            // Close it
            mDrawerLayout.closeDrawer(mStartDrawer);
            hadOpenDrawer = true;
        }
        // Is the end drawer open?
        if (mDrawerLayout.isDrawerOpen(mEndDrawer)) {
            // Close it
            mDrawerLayout.closeDrawer(mEndDrawer);
            hadOpenDrawer = true;
        }

        if (hadOpenDrawer) {
            // If we had one or both drawers open, now that we've closed it / them, return.
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
            mDrawerLayout.closeDrawer(mStartDrawer);
        }
    }
}
