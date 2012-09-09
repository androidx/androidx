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
import com.example.android.supportv4.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragmentTabsFragmentSupport extends Fragment {
    FragmentTabHost mTabHost;
    String mCurrentTabTag;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mTabHost = new FragmentTabHost(getActivity());
        mTabHost.setup(getActivity(), getChildFragmentManager(), R.id.fragment1);

        mTabHost.addTab(mTabHost.newTabSpec("simple").setIndicator("Simple"),
                FragmentStackSupport.CountingFragment.class, null);
        mTabHost.addTab(mTabHost.newTabSpec("contacts").setIndicator("Contacts"),
                LoaderCursorSupport.CursorLoaderListFragment.class, null);
        mTabHost.addTab(mTabHost.newTabSpec("custom").setIndicator("Custom"),
                LoaderCustomSupport.AppListFragment.class, null);
        mTabHost.addTab(mTabHost.newTabSpec("throttle").setIndicator("Throttle"),
                LoaderThrottleSupport.ThrottledLoaderListFragment.class, null);

        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }
        return mTabHost;
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            mCurrentTabTag = savedInstanceState.getString("tab");
        }
        mTabHost.setCurrentTabByTag(mCurrentTabTag);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Need to remember the selected tab so that we can restore it if
        // we later re-create the views.
        mCurrentTabTag = mTabHost.getCurrentTabTag();
        mTabHost = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tab", mTabHost != null
                ? mTabHost.getCurrentTabTag() : mCurrentTabTag);
    }
}
//END_INCLUDE(complete)
