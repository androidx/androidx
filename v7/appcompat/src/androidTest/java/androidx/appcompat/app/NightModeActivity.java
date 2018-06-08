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

import androidx.appcompat.test.R;
import androidx.appcompat.testutils.BaseTestActivity;

public class NightModeActivity extends BaseTestActivity {

    /**
     * Warning, gross hack here. Since night mode uses recreate(), we need a way to be able to
     * grab the top activity. The test runner only keeps reference to the original Activity which
     * is no good for these tests. Fixed by keeping a static reference to the 'top' instance, and
     * updating it in onResume and onPause. I said it was gross.
     */
    static NightModeActivity TOP_ACTIVITY = null;

    @Override
    protected int getContentViewLayoutResId() {
        return R.layout.activity_night_mode;
    }

    @Override
    protected void onResume() {
        super.onResume();
        TOP_ACTIVITY = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (TOP_ACTIVITY == this) {
            TOP_ACTIVITY = null;
        }
    }
}
