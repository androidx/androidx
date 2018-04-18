/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.example.android.supportv4.app;

//BEGIN_INCLUDE(complete)

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTabHost;

import com.example.android.supportv4.R;

public class FragmentNestingTabsSupport extends FragmentActivity {
    private FragmentTabHost mTabHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTabHost = new FragmentTabHost(this);
        setContentView(mTabHost);
        mTabHost.setup(this, getSupportFragmentManager(), R.id.fragment1);

        mTabHost.addTab(mTabHost.newTabSpec("menus").setIndicator("Menus"),
                FragmentMenuFragmentSupport.class, null);
        mTabHost.addTab(mTabHost.newTabSpec("contacts").setIndicator("Contacts"),
                LoaderCursorSupport.CursorLoaderListFragment.class, null);
        mTabHost.addTab(mTabHost.newTabSpec("stack").setIndicator("Stack"),
                FragmentStackFragmentSupport.class, null);
        mTabHost.addTab(mTabHost.newTabSpec("tabs").setIndicator("Tabs"),
                FragmentTabsFragmentSupport.class, null);
    }
}
//END_INCLUDE(complete)
