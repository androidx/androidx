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

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.car.R;

/**
 * Common base Activity for car apps that need to present a Drawer.
 *
 * <p>This Activity manages the overall layout. To use it, sub-classes need to:
 *
 * <ul>
 *   <li>Provide the root-items for the Drawer by implementing {@link #getRootAdapter()}.
 *   <li>Add their main content using {@link #setMainContent(int)} or {@link #setMainContent(View)}.
 *       They can also add fragments to the main-content container by obtaining its id using
 *       {@link #getContentContainerId()}
 * </ul>
 *
 * <p>This class will take care of drawer toggling and display.
 *
 * <p>The rootAdapter can implement nested-navigation, in its click-handling, by passing the
 * CarDrawerAdapter for the next level to
 * {@link CarDrawerController#pushAdapter(CarDrawerAdapter)}.
 *
 * <p>Any Activity's based on this class need to set their theme to CarDrawerActivityTheme or a
 * derivative.
 */
public abstract class CarDrawerActivity extends AppCompatActivity {
    private CarDrawerController mDrawerController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.car_drawer_activity);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                this /* activity */,
                drawerLayout, /* DrawerLayout object */
                R.string.car_drawer_open,
                R.string.car_drawer_close);

        Toolbar toolbar = findViewById(R.id.car_toolbar);
        setSupportActionBar(toolbar);

        mDrawerController = new CarDrawerController(toolbar, drawerLayout, drawerToggle);
        mDrawerController.setRootAdapter(getRootAdapter());

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
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
     */
    protected abstract CarDrawerAdapter getRootAdapter();

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
}
