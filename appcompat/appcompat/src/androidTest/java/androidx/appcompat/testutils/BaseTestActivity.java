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

package androidx.appcompat.testutils;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatCallback;
import androidx.appcompat.test.R;
import androidx.appcompat.view.ActionMode;
import androidx.testutils.LocaleTestUtils;
import androidx.testutils.RecreatedAppCompatActivity;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

public abstract class BaseTestActivity extends RecreatedAppCompatActivity {
    private static Locale sOverrideLocale;

    private Menu mMenu;

    private KeyEvent mOnKeyDownEvent;
    private KeyEvent mOnKeyUpEvent;
    private KeyEvent mOnKeyShortcutEvent;

    private MenuItem mOptionsItemSelected;

    private boolean mOnMenuOpenedCalled;
    private boolean mOnPanelClosedCalled;

    private boolean mShouldPopulateOptionsMenu = true;

    private boolean mOnBackPressedCalled;
    private boolean mDestroyed;

    private AppCompatCallback mAppCompatCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(0, 0);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        final int contentView = getContentViewLayoutResId();
        if (contentView > 0) {
            setContentView(contentView);
        }
        onContentViewSet();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * Recreates the activity with an RTL locale.
     */
    public void setRtlOneShotAndRecreate() {
        sOverrideLocale = new Locale(LocaleTestUtils.RTL_LANGUAGE);
        recreate();
    }

    /**
     * Clears any specified one-shot RTL override.
     */
    public void resetRtlOneShot() {
        sOverrideLocale = null;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        if (sOverrideLocale != null) {
            Configuration overrideConfig = new Configuration();
            overrideConfig.fontScale = 0;
            overrideConfig.locale = sOverrideLocale;

            ContextThemeWrapper wrappedBase = new ContextThemeWrapper(
                    newBase, androidx.appcompat.R.style.Theme_AppCompat_Empty);
            wrappedBase.applyOverrideConfiguration(overrideConfig);

            newBase = wrappedBase;

            sOverrideLocale = null;
        }

        super.attachBaseContext(newBase);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
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
        if (mShouldPopulateOptionsMenu) {
            getMenuInflater().inflate(R.menu.sample_actions, menu);
            return true;
        } else {
            menu.clear();
            return super.onCreateOptionsMenu(menu);
        }
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
        mMenu = null;
        mOptionsItemSelected = null;
    }

    public void setShouldPopulateOptionsMenu(boolean populate) {
        mShouldPopulateOptionsMenu = populate;
        if (mMenu != null) {
            supportInvalidateOptionsMenu();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDestroyed = true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mOnBackPressedCalled = true;
    }

    public boolean wasOnBackPressedCalled() {
        return mOnBackPressedCalled;
    }

    public Menu getMenu() {
        return mMenu;
    }

    @Override
    public boolean isDestroyed() {
        return mDestroyed;
    }

    @Override
    public void onSupportActionModeStarted(@NonNull ActionMode mode) {
        if (mAppCompatCallback != null) {
            mAppCompatCallback.onSupportActionModeStarted(mode);
        }
    }

    @Override
    public void onSupportActionModeFinished(@NonNull ActionMode mode) {
        if (mAppCompatCallback != null) {
            mAppCompatCallback.onSupportActionModeFinished(mode);
        }
    }

    @Override
    public @Nullable ActionMode onWindowStartingSupportActionMode(
            ActionMode.@NonNull Callback callback) {
        if (mAppCompatCallback != null) {
            return mAppCompatCallback.onWindowStartingSupportActionMode(callback);
        }
        return super.onWindowStartingSupportActionMode(callback);
    }

    public void setAppCompatCallback(AppCompatCallback callback) {
        mAppCompatCallback = callback;
    }
}
