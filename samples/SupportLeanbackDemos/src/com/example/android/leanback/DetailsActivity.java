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

import android.app.Activity;
import android.os.Bundle;

public class DetailsActivity extends Activity
{
    public static final String EXTRA_ITEM = "item";
    public static final String SHARED_ELEMENT_NAME = "hero";

    private boolean useLegacyFragment() {
        return (DetailsPresenterSelectionActivity.USE_LEGACY_PRESENTER
                && !(this instanceof SearchDetailsActivity));
    }

    protected boolean hasBackgroundVideo() {
        return false;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getFragmentManager().enableDebugLogging(true);
        setContentView(R.layout.details_activity);
        if (savedInstanceState == null) {
            if (useLegacyFragment()) {
                DetailsFragment fragment = new DetailsFragment();
                fragment.setItem((PhotoItem) getIntent().getParcelableExtra(EXTRA_ITEM));
                getFragmentManager().beginTransaction()
                        .replace(R.id.details_fragment, fragment)
                        .commit();
            } else {
                NewDetailsFragment fragment = new NewDetailsFragment();
                fragment.setItem((PhotoItem) getIntent().getParcelableExtra(EXTRA_ITEM));
                fragment.setBackgroundVideo(hasBackgroundVideo());
                getFragmentManager().beginTransaction()
                        .replace(R.id.details_fragment, fragment)
                        .commit();
            }
        } else {
            if (useLegacyFragment()) {
                DetailsFragment fragment = (DetailsFragment) getFragmentManager()
                        .findFragmentById(R.id.details_fragment);
                fragment.setItem((PhotoItem) getIntent().getParcelableExtra(EXTRA_ITEM));
            } else {
                NewDetailsFragment fragment = (NewDetailsFragment) getFragmentManager()
                        .findFragmentById(R.id.details_fragment);
                fragment.setItem((PhotoItem) getIntent().getParcelableExtra(EXTRA_ITEM));
            }
        }
    }
}
