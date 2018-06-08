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

package androidx.viewpager2.widget.swipe;

import static androidx.core.util.Preconditions.checkNotNull;

import android.content.Intent;
import android.os.Bundle;

import androidx.testutils.RecreatedActivity;
import androidx.viewpager2.test.R;
import androidx.viewpager2.widget.ViewPager2;

public abstract class BaseActivity extends RecreatedActivity {
    private static final String ARG_TOTAL_PAGES = "totalPages";

    protected ViewPager2 mViewPager;
    protected int mTotalPages;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_test_layout);

        mViewPager = findViewById(R.id.view_pager);
        mTotalPages = checkNotNull(getIntent().getExtras()).getInt(ARG_TOTAL_PAGES);

        setAdapter();
    }

    protected abstract void setAdapter();

    public abstract void validateState();

    public abstract void updatePage(int pageIx, int newValue);

    public static Intent createIntent(int totalPages) {
        Intent intent = new Intent();
        intent.putExtra(ARG_TOTAL_PAGES, totalPages);
        return intent;
    }
}
