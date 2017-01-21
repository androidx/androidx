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

package com.android.support.lifecycle;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.RestrictTo;

/**
 * Internal class that dispatches initialization events.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ReportInitializationFragment extends Fragment {

    private static final String FRAGMENT_TAG = "com.android.support.lifecycle.ReportFragment";

    private ActivityInitializationListener mProcessListener;
    private ActivityInitializationListener mActivityListener;

    private void dispatchCreate(ActivityInitializationListener listener) {
        if (listener != null) {
            listener.onCreate();
        }
    }

    private void dispatchStart(ActivityInitializationListener listener) {
        if (listener != null) {
            listener.onStart();
        }
    }

    private void dispatchResume(ActivityInitializationListener listener) {
        if (listener != null) {
            listener.onResume();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        dispatchCreate(mProcessListener);
        dispatchCreate(mActivityListener);
    }

    @Override
    public void onStart() {
        super.onStart();
        dispatchStart(mProcessListener);
        dispatchStart(mActivityListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        dispatchResume(mProcessListener);
        dispatchResume(mActivityListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // just want to be sure that we won't leak reference to an activity
        mProcessListener = null;
        mActivityListener = null;
    }

    void setProcessListener(ActivityInitializationListener processListener) {
        mProcessListener = processListener;
    }

    void setActivityListener(ActivityInitializationListener activityListener) {
        mActivityListener = activityListener;
    }

    static ReportInitializationFragment getOrCreateFor(Activity activity) {
        FragmentManager manager = activity.getFragmentManager();
        ReportInitializationFragment fragment =
                (ReportInitializationFragment) manager.findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            fragment = new ReportInitializationFragment();
            manager.beginTransaction().add(fragment, FRAGMENT_TAG).commit();

            // Hopefully, we are the first to make a transaction.
            manager.executePendingTransactions();
        }
        return fragment;
    }

    interface ActivityInitializationListener {
        void onCreate();

        void onStart();

        void onResume();
    }
}
