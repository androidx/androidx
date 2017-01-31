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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.android.sample.githubbrowser.model.AuthTokenModel;
import com.android.sample.githubbrowser.model.RepositoryListModel;
import com.android.sample.githubbrowser.network.GithubNetworkManager;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelStore;

/**
 * Our main activity.
 */
public class MainActivity extends BaseActivity {
    private static final String AUTH_TOKEN_KEY = "auth_token";
    private AuthTokenLifecycle mAuthTokenLifecycle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SharedPreferences sharedPreferences = getSharedPreferences(BuildConfig.APPLICATION_ID,
                Context.MODE_PRIVATE);
        final AuthTokenModel authTokenModel = ViewModelStore.get(this, "authTokenModel",
                AuthTokenModel.class);
        GithubNetworkManager.getInstance().setAuthTokenModel(authTokenModel);
        authTokenModel.getAuthTokenData().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                // Persist the new auth token
                if (TextUtils.isEmpty(s)) {
                    sharedPreferences.edit().clear().commit();
                } else {
                    sharedPreferences.edit().putString(AUTH_TOKEN_KEY, s).commit();
                }
            }
        });
        if (sharedPreferences.contains(AUTH_TOKEN_KEY)) {
            authTokenModel.getAuthTokenData().setValue(
                    sharedPreferences.getString(AUTH_TOKEN_KEY, ""));
        }

        mAuthTokenLifecycle = new AuthTokenLifecycle() {
            @Override
            public boolean doWeNeedAuthToken() {
                return TextUtils.isEmpty(authTokenModel.getAuthTokenData().getValue());
            }

            @Override
            public void invalidateAuthToken() {
                authTokenModel.getAuthTokenData().setValue(null);
            }

            @Override
            public void getAuthToken() {
                // Pop everything off of the stack except the first entry
                FragmentManager fragmentManager = getSupportFragmentManager();
                while (fragmentManager.getBackStackEntryCount() > 0) {
                    fragmentManager.popBackStackImmediate();
                }

                GetAuthTokenFragment getAuthTokenFragment = new GetAuthTokenFragment();
                getAuthTokenFragment.show(fragmentManager, "get_auth_token");
            }
        };

        final RepositoryListModel mainModel = ViewModelStore.get(this, "mainRepoModel",
                RepositoryListModel.class);
        if (!mainModel.hasSearchTerm()) {
            mainModel.setSearchTerm(this, "google", mAuthTokenLifecycle);
        }

        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.fragment_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Create a new Fragment to be placed in the activity layout
            RepositoryListFragment mainFragment = new RepositoryListFragment();

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, mainFragment, "main").commit();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final EditText search = (EditText) toolbar.findViewById(R.id.search);
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
                    mainModel.setSearchTerm(v.getContext(), query, mAuthTokenLifecycle);
                    return true;
                }
                return false;
            }
        });
    }
}
