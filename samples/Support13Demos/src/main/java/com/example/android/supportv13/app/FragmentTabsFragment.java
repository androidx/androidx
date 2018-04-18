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
package com.example.android.supportv13.app;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.legacy.app.FragmentTabHost;

import com.example.android.supportv13.R;

public class FragmentTabsFragment extends Fragment {
    private FragmentTabHost mTabHost;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mTabHost = new FragmentTabHost(getActivity());
        mTabHost.setup(getActivity(), getChildFragmentManager(), R.id.pager);

        mTabHost.addTab(mTabHost.newTabSpec("simple").setIndicator("Simple"),
                CountingFragment.class, null);
        mTabHost.addTab(mTabHost.newTabSpec("array").setIndicator("Array"),
                FragmentPagerSupport.ArrayListFragment.class, null);
        mTabHost.addTab(mTabHost.newTabSpec("cursor").setIndicator("Cursor"),
                CursorFragment.class, null);

        return mTabHost;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mTabHost = null;
    }
}
//END_INCLUDE(complete)
