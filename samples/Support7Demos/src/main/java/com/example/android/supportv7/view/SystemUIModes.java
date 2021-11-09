/*
 * Copyright 2019 The Android Open Source Project
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

package com.example.android.supportv7.view;

import static android.os.Build.VERSION.SDK_INT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.appcompat.widget.ShareActionProvider;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.FragmentTransaction;

import com.example.android.supportv7.R;

/**
 * This activity demonstrates some of the available ways to reduce the size or visual contrast of
 * the system decor, in order to better focus the user's attention or use available screen real
 * estate on the task at hand.
 */
@SuppressLint("UnknownNullness")
public class SystemUIModes extends AppCompatActivity
        implements OnQueryTextListener, ActionBar.TabListener {
    IV mImage;
    CheckBox[] mCheckControls = new CheckBox[8];
    int[] mCheckFlags = new int[]{View.SYSTEM_UI_FLAG_LOW_PROFILE,
            View.SYSTEM_UI_FLAG_FULLSCREEN, View.SYSTEM_UI_FLAG_HIDE_NAVIGATION,
            View.SYSTEM_UI_FLAG_IMMERSIVE, View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY,
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE, View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN,
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    };
    TextView mMetricsText;

    private void setFullscreen(boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    private void setOverscan(boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN;
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    private void setTranslucentStatus(boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    private void setTranslucentNavigation(boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    private String getDisplaySize() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        return String.format("DisplayMetrics = (%d x %d)", dm.widthPixels, dm.heightPixels);
    }

    private String getViewSize() {
        return String.format("View = (%d,%d - %d,%d)",
                mImage.getLeft(), mImage.getTop(),
                mImage.getRight(), mImage.getBottom());
    }

    void refreshSizes() {
        mMetricsText.setText(getDisplaySize() + " " + getViewSize());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.system_ui_modes);

        setSupportActionBar(findViewById(R.id.toolbar));

        mImage = findViewById(R.id.image);
        mImage.setActivity(this);

        CompoundButton.OnCheckedChangeListener checkChangeListener =
                (buttonView, isChecked) -> updateSystemUi();
        mCheckControls[0] = findViewById(R.id.modeLowProfile);
        mCheckControls[1] = findViewById(R.id.modeFullscreen);
        mCheckControls[2] = findViewById(R.id.modeHideNavigation);
        mCheckControls[3] = findViewById(R.id.modeImmersive);
        mCheckControls[4] = findViewById(R.id.modeImmersiveSticky);
        mCheckControls[5] = findViewById(R.id.layoutStable);
        mCheckControls[6] = findViewById(R.id.layoutFullscreen);
        mCheckControls[7] = findViewById(R.id.layoutHideNavigation);
        for (int i = 0; i < mCheckControls.length; i++) {
            mCheckControls[i].setOnCheckedChangeListener(checkChangeListener);
        }
        ((CheckBox) findViewById(R.id.windowFullscreen)).setOnCheckedChangeListener(
                (buttonView, isChecked) -> setFullscreen(isChecked)
        );
        ((CheckBox) findViewById(R.id.windowOverscan)).setOnCheckedChangeListener(
                (buttonView, isChecked) -> setOverscan(isChecked)
        );
        ((CheckBox) findViewById(R.id.windowTranslucentStatus)).setOnCheckedChangeListener(
                (buttonView, isChecked) -> setTranslucentStatus(isChecked)
        );
        ((CheckBox) findViewById(R.id.windowTranslucentNav)).setOnCheckedChangeListener(
                (buttonView, isChecked) -> setTranslucentNavigation(isChecked)
        );
        ((CheckBox) findViewById(R.id.windowHideActionBar)).setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    if (isChecked) {
                        getSupportActionBar().hide();
                    } else {
                        getSupportActionBar().show();
                    }
                }
        );
        ((CheckBox) findViewById(R.id.windowActionMode)).setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    if (isChecked) {
                        mImage.startActionMode();
                    } else {
                        mImage.stopActionMode();
                    }
                }
        );
        mMetricsText = findViewById(R.id.metricsText);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.content_actions, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        if (searchView != null) {
            searchView.setOnQueryTextListener(this);
        }

        // Set file with share history to the provider and set the share intent.
        MenuItem actionItem = menu.findItem(R.id.menu_item_share_action_provider_action_bar);
        ShareActionProvider actionProvider =
                (ShareActionProvider) MenuItemCompat.getActionProvider(actionItem);
        if (actionProvider != null) {
            actionProvider.setShareHistoryFileName(
                    ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
            // Note that you can set/change the intent any time,
            // say when the user has selected an image.
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            Uri uri = Uri.fromFile(getFileStreamPath("shared.png"));
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            actionProvider.setShareIntent(shareIntent);
        }

        return true;
    }

    @Override
    public void onAttachedToWindow() {
        updateCheckControls();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void onSort(MenuItem item) {
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Toast.makeText(this, "Searching for: " + query + "...", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.show_tabs:
                getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
                item.setChecked(true);
                return true;
            case R.id.hide_tabs:
                getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                item.setChecked(true);
                return true;
        }
        return false;
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {}

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {}

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {}

    public void updateCheckControls() {
        int visibility = mImage.getSystemUiVisibility();
        for (int i = 0; i < mCheckControls.length; i++) {
            mCheckControls[i].setChecked((visibility & mCheckFlags[i]) != 0);
        }
    }

    public void updateSystemUi() {
        int visibility = 0;
        for (int i = 0; i < mCheckControls.length; i++) {
            if (mCheckControls[i].isChecked()) {
                visibility |= mCheckFlags[i];
            }
        }
        mImage.setSystemUiVisibility(visibility);
    }

    public void clearActionMode() {
        ((CheckBox) findViewById(R.id.windowActionMode)).setChecked(false);
    }

    /**
     * Instrumented ImageView for use in SystemUIModes activity.
     */
    @SuppressWarnings("AppCompatCustomView")
    public static class IV extends ImageView implements View.OnSystemUiVisibilityChangeListener {
        private SystemUIModes mActivity;
        private ActionMode mActionMode;

        public IV(Context context) {
            super(context);
        }

        public IV(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public void setActivity(SystemUIModes act) {
            setOnSystemUiVisibilityChangeListener(this);
            mActivity = act;
        }

        @Override
        public void onSizeChanged(int w, int h, int oldw, int oldh) {
            mActivity.refreshSizes();
        }

        @Override
        public void onSystemUiVisibilityChange(int visibility) {
            mActivity.updateCheckControls();
            mActivity.refreshSizes();
        }

        public void startActionMode() {
            if (mActionMode == null) {
                mActionMode = mActivity.startSupportActionMode(new MyActionModeCallback());
            }
        }

        public void stopActionMode() {
            if (mActionMode != null) {
                mActionMode.finish();
            }
        }

        class MyActionModeCallback implements ActionMode.Callback {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.setTitle("My Action Mode!");
                mode.setSubtitle(null);
                if (SDK_INT >= 16) {
                    mode.setTitleOptionalHint(false);
                }
                menu.add("Sort By Size").setIcon(android.R.drawable.ic_menu_sort_by_size);
                menu.add("Sort By Alpha").setIcon(android.R.drawable.ic_menu_sort_alphabetically);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mActionMode = null;
                mActivity.clearActionMode();
            }
        }
    }
}

