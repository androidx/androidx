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
package com.android.sample.githubbrowser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.android.sample.githubbrowser.di.LifecycleProviderComponent;
import com.android.sample.githubbrowser.model.AuthTokenModel;
import com.android.sample.githubbrowser.navigation.NavigationController;
import com.android.sample.githubbrowser.viewmodel.RepositorySearchModel;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelStore;

import javax.inject.Inject;

/**
 * Our main activity.
 */
public class MainActivity extends BaseActivity {
    private static final String AUTH_TOKEN_FRAGMENT_TAG = "get_auth_token";
    @Inject
    AuthTokenModel mAuthTokenModel;
    private NavigationController mNavigationController;

    @Override
    public void inject(LifecycleProviderComponent component) {
        component.inject(this);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mNavigationController = new NavigationController(this, getSupportFragmentManager(),
                R.id.fragment_container);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final EditText search = (EditText) toolbar.findViewById(R.id.search);
        final RepositorySearchModel mainSearchModel = ViewModelStore.get(this,
                RepositorySearchModel.class);

        String currentSearch = search.getText().toString();
        if (TextUtils.isEmpty(currentSearch)) {
            search.setText("google");
            currentSearch = "google";
        }
        mainSearchModel.setQuery(currentSearch, true);

        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (savedInstanceState == null) {
            // Create a new Fragment to be placed in the activity layout
            RepositoryListFragment mainFragment = new RepositoryListFragment();

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, mainFragment, "main").commit();
        }

        search.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN)
                        && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String query = search.getText().toString();
                    Snackbar.make(findViewById(R.id.col), "Searching for " + query,
                            Snackbar.LENGTH_SHORT).show();

                    // Dismiss keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(search.getWindowToken(), 0);

                    // Pop everything off of the stack except the first entry
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    while (fragmentManager.getBackStackEntryCount() > 0) {
                        fragmentManager.popBackStackImmediate();
                    }

                    // Perform search action on key press
                    mainSearchModel.setQuery(query, false);
                    return true;
                }
                return false;
            }
        });

        mAuthTokenModel.getAuthTokenData().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String token) {
                search.setEnabled(token != null);
                // show get auth token fragment
                // Pop everything off of the stack except the first entry
                FragmentManager fragmentManager = getSupportFragmentManager();
                GetAuthTokenFragment getAuthTokenFragment = getGetAuthTokenFragment(
                        fragmentManager);
                if (token == null) {
                    if (!getAuthTokenFragment.isAdded()) {
                        getAuthTokenFragment.show(fragmentManager, AUTH_TOKEN_FRAGMENT_TAG);
                    }
                } else {
                    if (getAuthTokenFragment.isAdded()) {
                        getAuthTokenFragment.dismiss();
                    }
                }
            }
        });
    }

    @NonNull
    private GetAuthTokenFragment getGetAuthTokenFragment(FragmentManager fragmentManager) {
        Fragment authTokenFragment = fragmentManager
                .findFragmentByTag(AUTH_TOKEN_FRAGMENT_TAG);
        GetAuthTokenFragment getAuthTokenFragment;
        if (authTokenFragment instanceof GetAuthTokenFragment) {
            getAuthTokenFragment = (GetAuthTokenFragment) authTokenFragment;
        } else {
            getAuthTokenFragment = new GetAuthTokenFragment();
        }
        return getAuthTokenFragment;
    }

    public NavigationController getNavigationController() {
        return mNavigationController;
    }
}
