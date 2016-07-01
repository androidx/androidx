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

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

public class DetailsSupportActivity extends FragmentActivity
{
    public static final String EXTRA_ITEM = "item";
    public static final String SHARED_ELEMENT_NAME = "hero";

    private boolean useLegacyFragment() {
        return (DetailsPresenterSelectionActivity.USE_LEGACY_PRESENTER
                && !(this instanceof SearchDetailsSupportActivity));
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(useLegacyFragment() ? R.layout.legacy_details_support : R.layout.details_support);
        if (savedInstanceState == null) {
            // Only pass object to fragment when activity is first time created,
            // later object is modified and persisted with fragment state.
            if (useLegacyFragment()) {
                ((DetailsSupportFragment)getSupportFragmentManager().findFragmentById(R.id.details_fragment))
                    .setItem((PhotoItem) getIntent().getParcelableExtra(EXTRA_ITEM));
            } else {
                ((NewDetailsSupportFragment)getSupportFragmentManager().findFragmentById(R.id.details_fragment))
                    .setItem((PhotoItem) getIntent().getParcelableExtra(EXTRA_ITEM));
            }
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        BackgroundHelper.attach(this);
    }

    @Override
    public void onStop() {
        BackgroundHelper.release(this);
        super.onStop();
    }
}
