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

import android.view.KeyEvent;
import android.view.Menu;

import androidx.appcompat.test.R;
import androidx.appcompat.testutils.BaseTestActivity;
import androidx.appcompat.widget.Toolbar;

public class ToolbarAppCompatActivity extends BaseTestActivity {

    private Toolbar mToolbar;

    public int mCreateMenuCount;
    public int mPrepareMenuCount;
    public int mKeyShortcutCount;

    @Override
    protected int getContentViewLayoutResId() {
        return R.layout.toolbar_decor_content;
    }

    @Override
    protected void onContentViewSet() {
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
    }

    public Toolbar getToolbar() {
        return mToolbar;
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        ++mKeyShortcutCount;
        return super.onKeyShortcut(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        ++mCreateMenuCount;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        ++mPrepareMenuCount;
        return super.onPrepareOptionsMenu(menu);
    }

    public void resetCounters() {
        mCreateMenuCount = mPrepareMenuCount = mKeyShortcutCount = 0;
    }
}
