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

package androidx.appcompat.app;

import android.view.Menu;
import android.view.MenuInflater;

import androidx.appcompat.test.R;
import androidx.appcompat.testutils.BaseTestActivity;
import androidx.appcompat.widget.Toolbar;

public class AppCompatMenuItemIconTintingTestActivity extends BaseTestActivity {
    private Toolbar mToolbar;

    @Override
    protected int getContentViewLayoutResId() {
        return R.layout.appcompat_toolbar_activity;
    }

    @Override
    protected void onContentViewSet() {
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.appcompat_menu_icon_tinting, menu);

        return true;
    }

    public Menu getToolbarMenu() {
        return mToolbar.getMenu();
    }
}
