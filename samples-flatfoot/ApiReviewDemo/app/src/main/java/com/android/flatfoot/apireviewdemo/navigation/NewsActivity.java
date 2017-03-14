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

package com.android.flatfoot.apireviewdemo.navigation;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import com.android.flatfoot.apireviewdemo.R;

/**
 * Navigation activity and delegates everything to the
 * {@link android.support.navigation.app.nav.NavHostFragment}.
 *
 * <p>This is where you'd set up any global navigation patterns such as a navigation drawer or
 * bottom navigation bar.
 */
public class NewsActivity extends FragmentActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.news_activity);

        // TODO Global navigation code, like configuring a navigation drawer, goes here
    }
}
