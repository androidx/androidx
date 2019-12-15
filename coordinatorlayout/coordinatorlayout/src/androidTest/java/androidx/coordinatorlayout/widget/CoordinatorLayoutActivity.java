/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.coordinatorlayout.widget;

import android.widget.FrameLayout;

import androidx.coordinatorlayout.BaseTestActivity;
import androidx.coordinatorlayout.test.R;

public class CoordinatorLayoutActivity extends BaseTestActivity {

    FrameLayout mContainer;
    CoordinatorLayout mCoordinatorLayout;

    @Override
    protected int getContentViewLayoutResId() {
        return R.layout.activity_coordinator_layout;
    }

    @Override
    protected void onContentViewSet() {
        mContainer = findViewById(R.id.container);
        mCoordinatorLayout = findViewById(R.id.coordinator);
    }

}
