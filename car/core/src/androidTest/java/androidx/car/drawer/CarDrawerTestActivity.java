/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.view.MenuItem;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.car.test.R;
import androidx.drawerlayout.widget.DrawerLayout;

/**
 * Test activity that sets up a drawer using the common drawer components provided by the car
 * library.
 *
 * <p>This class does not provide actual drawer content. To populate the drawer, use
 * {@link #getDrawerController()} to retrieve the controller.
 */
public final class CarDrawerTestActivity extends AppCompatActivity {
    private CarDrawerController mDrawerController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.car_drawer_activity);

        Toolbar toolbar = findViewById(R.id.drawer_toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                /* activity= */ this,
                drawerLayout,
                R.string.car_drawer_open,
                R.string.car_drawer_close);

        mDrawerController = new CarDrawerController(drawerLayout, drawerToggle);
    }

    CarDrawerController getDrawerController() {
        return mDrawerController;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerController.syncState();
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
     * The root drawer view that will delegate clicks to the a sub-drawer.
     */
    private class DrawerRootAdapter extends CarDrawerAdapter {
        private static final int NUM_OF_ITEMS = 10;

        DrawerRootAdapter(Context context) {
            super(context, /* showDisabledListOnEmpty= */ true);
            setTitle("Drawer title");
        }

        @Override
        protected void populateViewHolder(DrawerItemViewHolder holder, int position) {
            holder.getTitleView().setText("Item " + position);
        }

        @Override
        protected int getActualItemCount() {
            return NUM_OF_ITEMS;
        }
    }
}
