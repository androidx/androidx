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

import static com.android.support.lifecycle.Lifecycle.ON_DESTROY;
import static com.android.support.lifecycle.Lifecycle.ON_PAUSE;
import static com.android.support.lifecycle.Lifecycle.ON_STOP;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * When initialized, it hooks into the Activity callback of the Application and observes
 * Activities. It is responsible to hook in child-fragments to activities and fragments to report
 * their lifecycle events. Another responsibility of this class is to mark as stopped all lifecycle
 * providers related to an activity as soon it is not safe to run a fragment transaction in this
 * activity.
 */
class LifecycleDispatcher {

    private static final String REPORT_FRAGMENT_TAG = "com.android.support.lifecycle"
            + ".LifecycleDispatcher.report_fragment_tag";

    private static AtomicBoolean sInitialized = new AtomicBoolean(false);

    static void init(Context context) {
        if (sInitialized.getAndSet(true)) {
            return;
        }
        ((Application) context.getApplicationContext())
                .registerActivityLifecycleCallbacks(new DispatcherActivityCallback());
    }

    static ReportFragment get(Activity activity) {
        return (ReportFragment) activity.getFragmentManager().findFragmentByTag(
                REPORT_FRAGMENT_TAG);
    }

    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    static class DispatcherActivityCallback extends EmptyActivityLifecycleCallbacks {
        private final FragmentCallback mFragmentCallback;

        DispatcherActivityCallback() {
            mFragmentCallback = new FragmentCallback();
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            if (activity instanceof FragmentActivity) {
                ((FragmentActivity) activity).getSupportFragmentManager()
                        .registerFragmentLifecycleCallbacks(mFragmentCallback, true);
            }
            // ProcessLifecycleOwner should always correctly work and some activities may not extend
            // FragmentActivity from support lib, so we use framework fragments for activities
            android.app.FragmentManager manager = activity.getFragmentManager();
            if (manager.findFragmentByTag(REPORT_FRAGMENT_TAG) == null) {
                manager.beginTransaction().add(new ReportFragment(), REPORT_FRAGMENT_TAG).commit();
                // Hopefully, we are the first to make a transaction.
                manager.executePendingTransactions();
            }
        }

        @Override
        public void onActivityStopped(Activity activity) {
            if (activity instanceof FragmentActivity) {
                markState((FragmentActivity) activity, Lifecycle.STOPPED);
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            if (activity instanceof FragmentActivity) {
                markState((FragmentActivity) activity, Lifecycle.STOPPED);
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class DestructionReportFragment extends Fragment {
        @Override
        public void onPause() {
            super.onPause();
            dispatch(ON_PAUSE);
        }

        @Override
        public void onStop() {
            super.onStop();
            dispatch(ON_STOP);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            dispatch(ON_DESTROY);
        }

        protected void dispatch(@Lifecycle.Event int event) {
            dispatchIfLifecycleOwner(getParentFragment(), event);
        }
    }

    private static void markState(FragmentManager manager, @Lifecycle.State int state) {
        Collection<Fragment> fragments = manager.getFragments();
        if (fragments == null) {
            return;
        }
        for (Fragment fragment : fragments) {
            if (fragment == null) {
                continue;
            }
            markStateIn(fragment, state);
            markState(fragment.getChildFragmentManager(), state);
        }
    }

    private static void markStateIn(Object object, @Lifecycle.State int state) {
        if (object instanceof LifecycleRegistryOwner) {
            LifecycleRegistry registry = ((LifecycleRegistryOwner) object).getLifecycle();
            registry.markState(state);
        }
    }

    private static void markState(FragmentActivity activity, @Lifecycle.State int state) {
        markStateIn(activity, state);
        markState(activity.getSupportFragmentManager(), state);
    }

    private static void dispatchIfLifecycleOwner(Fragment fragment, @Lifecycle.Event int event) {
        if (fragment instanceof LifecycleRegistryOwner) {
            ((LifecycleRegistryOwner) fragment).getLifecycle().handleLifecycleEvent(event);
        }
    }

    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    static class FragmentCallback extends FragmentManager.FragmentLifecycleCallbacks {

        @Override
        public void onFragmentCreated(FragmentManager fm, Fragment f, Bundle savedInstanceState) {
            dispatchIfLifecycleOwner(f, Lifecycle.ON_CREATE);

            if (!(f instanceof LifecycleRegistryOwner)) {
                return;
            }

            if (f.getChildFragmentManager().findFragmentByTag(REPORT_FRAGMENT_TAG) == null) {
                f.getChildFragmentManager().beginTransaction().add(new DestructionReportFragment(),
                        REPORT_FRAGMENT_TAG).commit();
            }
        }

        @Override
        public void onFragmentStarted(FragmentManager fm, Fragment f) {
            dispatchIfLifecycleOwner(f, Lifecycle.ON_START);
        }

        @Override
        public void onFragmentResumed(FragmentManager fm, Fragment f) {
            dispatchIfLifecycleOwner(f, Lifecycle.ON_RESUME);
        }
    }
}
