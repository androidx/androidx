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

package com.android.sample.githubbrowser.navigation;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.android.sample.githubbrowser.EditUserDetailsFragment;
import com.android.sample.githubbrowser.RepositoryDetailsFragment;
import com.android.sample.githubbrowser.UserDetailsFragment;
import com.android.sample.githubbrowser.data.PersonData;
import com.android.sample.githubbrowser.data.RepositoryData;
import com.android.support.lifecycle.Lifecycle;
import com.android.support.lifecycle.LifecycleProvider;

public class NavigationController {
    private final FragmentManager mFragmentManager;
    private final int mHostViewId;
    private final Lifecycle mLifecycle;

    public NavigationController(LifecycleProvider lifecycleProvider,
            FragmentManager fragmentManager, int hostViewId) {
        mLifecycle = lifecycleProvider.getLifecycle();
        mFragmentManager = fragmentManager;
        mHostViewId = hostViewId;
    }

    private boolean isActive() {
        return mLifecycle.getCurrentState() >= Lifecycle.STARTED;
    }

    public void openRepositoryDetailsFragment(RepositoryData repo) {
        RepositoryDetailsFragment fragment = RepositoryDetailsFragment.createFor(repo);
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        transaction.add(mHostViewId, fragment, "repoDetails:" + repo.id);
        transaction.addToBackStack("repoDetails:" + repo.id);
        transaction.commitAllowingStateLoss();
    }

    public void openUserDetailsFragment(PersonData person) {
        UserDetailsFragment fragment = UserDetailsFragment.createFor(person);
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        transaction.add(mHostViewId, fragment, "userDetails:" + person.login);
        transaction.addToBackStack("userDetails:" + person.login);
        transaction.commitAllowingStateLoss();
    }

    public void openEditUserDetailsFragment(Fragment target, PersonData user) {
        if (!isActive()) {
            return;
        }
        EditUserDetailsFragment editUserDetailsFragment =
                EditUserDetailsFragment.createFor(target, user);
        editUserDetailsFragment.show(mFragmentManager, "editUser:" + user.login);
    }
}
