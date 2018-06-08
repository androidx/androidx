// CHECKSTYLE:OFF Generated code
/* This file is auto-generated from DetailsActivity.java.  DO NOT MODIFY. */

/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.android.leanback;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

public class DetailsSupportActivity extends FragmentActivity
{
    public static final String EXTRA_ITEM = "item";
    public static final String SHARED_ELEMENT_NAME = "hero";

    private boolean useLegacyFragment() {
        return (DetailsPresenterSelectionActivity.USE_LEGACY_PRESENTER
                && !(this instanceof SearchDetailsSupportActivity));
    }

    protected boolean hasBackgroundVideo() {
        return false;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().enableDebugLogging(true);
        setContentView(R.layout.details_activity);
        if (savedInstanceState == null) {
            if (useLegacyFragment()) {
                DetailsSupportFragment fragment = new DetailsSupportFragment();
                fragment.setItem((PhotoItem) getIntent().getParcelableExtra(EXTRA_ITEM));
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.details_fragment, fragment)
                        .commit();
            } else {
                NewDetailsSupportFragment fragment = new NewDetailsSupportFragment();
                fragment.setItem((PhotoItem) getIntent().getParcelableExtra(EXTRA_ITEM));
                fragment.setBackgroundVideo(hasBackgroundVideo());
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.details_fragment, fragment)
                        .commit();
            }
        } else {
            if (useLegacyFragment()) {
                DetailsSupportFragment fragment = (DetailsSupportFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.details_fragment);
                fragment.setItem((PhotoItem) getIntent().getParcelableExtra(EXTRA_ITEM));
            } else {
                NewDetailsSupportFragment fragment = (NewDetailsSupportFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.details_fragment);
                fragment.setItem((PhotoItem) getIntent().getParcelableExtra(EXTRA_ITEM));
            }
        }
    }
}
