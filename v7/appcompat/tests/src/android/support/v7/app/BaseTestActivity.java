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

package android.support.v7.app;

import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.appcompat.test.R;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

abstract class BaseTestActivity extends AppCompatActivity {

    private Menu mMenu;

    private KeyEvent mOnKeyDownEvent;
    private KeyEvent mOnKeyUpEvent;
    private KeyEvent mOnKeyShortcutEvent;

    private MenuItem mOptionsItemSelected;

    private boolean mOnMenuOpenedCalled;
    private boolean mOnPanelClosedCalled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        final int contentView = getContentViewLayoutResId();
        if (contentView > 0) {
            setContentView(contentView);
        }
        onContentViewSet();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    protected abstract int getContentViewLayoutResId();

    protected void onContentViewSet() {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mOptionsItemSelected = item;
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        mOnMenuOpenedCalled = true;
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public void onPanelClosed(int featureId, Menu menu) {
        mOnPanelClosedCalled = true;
        super.onPanelClosed(featureId, menu);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        mOnKeyDownEvent = event;
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        mOnKeyUpEvent = event;
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        mOnKeyShortcutEvent = event;
        return super.onKeyShortcut(keyCode, event);
    }

    public KeyEvent getInvokedKeyShortcutEvent() {
        return mOnKeyShortcutEvent;
    }

    public boolean wasOnMenuOpenedCalled() {
        return mOnMenuOpenedCalled;
    }

    public boolean wasOnPanelClosedCalled() {
        return mOnPanelClosedCalled;
    }

    public KeyEvent getInvokedKeyDownEvent() {
        return mOnKeyDownEvent;
    }

    public KeyEvent getInvokedKeyUpEvent() {
        return mOnKeyUpEvent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        getMenuInflater().inflate(R.menu.sample_actions, menu);
        return true;
    }

    public boolean expandSearchView() {
        return MenuItemCompat.expandActionView(mMenu.findItem(R.id.action_search));
    }

    public boolean collapseSearchView() {
        return MenuItemCompat.collapseActionView(mMenu.findItem(R.id.action_search));
    }

    public boolean isSearchViewExpanded() {
        return MenuItemCompat.isActionViewExpanded(mMenu.findItem(R.id.action_search));
    }

    public MenuItem getOptionsItemSelected() {
        return mOptionsItemSelected;
    }

    public void reset() {
        mOnKeyUpEvent = null;
        mOnKeyDownEvent = null;
        mOnKeyShortcutEvent = null;
        mOnMenuOpenedCalled = false;
        mOnPanelClosedCalled = false;
    }
}
