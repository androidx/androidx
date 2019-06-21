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

import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.test.R;
import androidx.appcompat.testutils.BaseTestActivity;

public class NightModeActivity extends BaseTestActivity {
    private int mLastNightModeChange = Integer.MIN_VALUE;
    private Configuration mLastConfigurationChange;

    @Override
    protected int getContentViewLayoutResId() {
        return R.layout.activity_night_mode;
    }

    @Override
    public void onNightModeChanged(int mode) {
        mLastNightModeChange = mode;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mLastConfigurationChange = newConfig;
    }

    @Nullable
    Configuration getLastConfigurationChangeAndClear() {
        final Configuration config = mLastConfigurationChange;
        mLastConfigurationChange = null;
        return config;
    }

    int getLastNightModeAndReset() {
        final int mode = mLastNightModeChange;
        mLastNightModeChange = Integer.MIN_VALUE;
        return mode;
    }
}
