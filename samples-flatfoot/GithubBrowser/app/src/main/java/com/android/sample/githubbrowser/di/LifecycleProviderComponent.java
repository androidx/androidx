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

package com.android.sample.githubbrowser.di;

import android.databinding.DataBindingComponent;

import com.android.sample.githubbrowser.GetAuthTokenFragment;
import com.android.sample.githubbrowser.MainActivity;
import com.android.sample.githubbrowser.RepositoryListFragment;
import com.android.sample.githubbrowser.databinding.DataBindingAdapters;

import dagger.Subcomponent;

@LifecycleProviderScope
@Subcomponent(modules = {LifecycleProviderModule.class})
public interface LifecycleProviderComponent extends android.databinding.DataBindingComponent {
    void inject(MainActivity mainActivity);
    void inject(GetAuthTokenFragment getAuthTokenFragment);
    void inject(RepositoryListFragment repositoryListFragment);
    @Override
    DataBindingAdapters getDataBindingAdapters();
}
