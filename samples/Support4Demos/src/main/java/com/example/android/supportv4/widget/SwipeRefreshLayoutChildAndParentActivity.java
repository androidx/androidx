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

package com.example.android.supportv4.widget;

import com.example.android.supportv4.R;

/**
 * This example exhibits the behavior of SwipeRefreshLayout when it is nested inside of a
 * NestedScrollingParent and has a NestedScrollingChild.
 */
public class SwipeRefreshLayoutChildAndParentActivity extends BaseSwipeRefreshLayoutActivity {

    @Override
    protected int getLayoutId() {
        return R.layout.swipe_refresh_widget_child_and_parent;
    }

}
