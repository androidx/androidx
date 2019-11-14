/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.view.View;
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
 * Test activity for testing various APIs and interactions for DrawerLayout with start and end
 * drawers.
 */
public class DrawerLayoutDoubleActivity extends BaseTestActivity {
    private DrawerLayout mDrawerLayout;
    private ListView mStartDrawer;
    private View mEndDrawer;
    private TextView mContent;

    private Toolbar mToolbar;

    @Override
    protected int getContentViewLayoutResId() {
        return R.layout.drawer_double_layout;
    }

    @Override
    protected void onContentViewSet() {
        super.onContentViewSet();

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mStartDrawer = findViewById(R.id.start_drawer);
        mEndDrawer = findViewById(R.id.end_drawer);
        mContent = findViewById(R.id.content_text);

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

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

        // Configure the background color fill of the system status bar (on supported platform
        // versions) and the toolbar itself. We're using the same color, and android:statusBar
        // from the theme makes the status bar slightly darker.
        final int metalBlueColor = getResources().getColor(R.color.drawer_sample_metal_blue);
        mDrawerLayout.setStatusBarBackgroundColor(metalBlueColor);
        mToolbar.setBackgroundColor(metalBlueColor);
    }

    @Override
    public void onBackPressed() {
        // Is the start drawer open?
        if (mDrawerLayout.isDrawerOpen(mStartDrawer)) {
            // Close the drawer and return.
            mDrawerLayout.closeDrawer(mStartDrawer);
            return;
        }

        // Is the end drawer open?
        if (mDrawerLayout.isDrawerOpen(mEndDrawer)) {
            // Close the drawer and return.
            mDrawerLayout.closeDrawer(mEndDrawer);
            return;
        }

        super.onBackPressed();
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
