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

package com.android.support.lifecycle.viewmodeltest;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.android.support.lifecycle.LifecycleActivity;
import com.android.support.lifecycle.LifecycleFragment;
import com.android.support.lifecycle.LifecycleProvider;
import com.android.support.lifecycle.ViewModelStore;
import com.android.support.lifecycle.extensions.test.R;

public class ViewModelActivity extends LifecycleActivity {
    public static final String KEY_FRAGMENT_MODEL = "fragment-model";
    public static final String KEY_ACTIVITY_MODEL = "activity-model";
    public static final String FRAGMENT_TAG_1 = "f1";
    public static final String FRAGMENT_TAG_2 = "f2";

    public TestViewModel activityModel;
    public TestViewModel defaultActivityModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_model);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new ViewModelFragment(), FRAGMENT_TAG_1)
                    .add(new ViewModelFragment(), FRAGMENT_TAG_2)
                    .commit();
        }
        activityModel = ViewModelStore.get(this, KEY_ACTIVITY_MODEL, TestViewModel.class);
        defaultActivityModel = ViewModelStore.get(this, TestViewModel.class);
    }

    public static class ViewModelFragment extends LifecycleFragment {
        public TestViewModel fragmentModel;
        public TestViewModel activityModel;
        public TestViewModel defaultActivityModel;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            fragmentModel = ViewModelStore.get(this, KEY_FRAGMENT_MODEL,
                    TestViewModel.class);
            activityModel = ViewModelStore.get((LifecycleProvider) getActivity(),
                    KEY_ACTIVITY_MODEL, TestViewModel.class);
            defaultActivityModel = ViewModelStore.get((LifecycleProvider) getActivity(),
                    TestViewModel.class);
        }
    }
}
