/*
 * Copyright 2018 The Android Open Source Project
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


package androidx.fragment.app;

import android.content.Context;
import android.os.Bundle;

/**
 * This fragment watches its primary lifecycle events and throws IllegalStateException
 * if any of them are called out of order or from a bad/unexpected state.
 */
public class StrictFragment extends Fragment {
    public static final int DETACHED = 0;
    public static final int ATTACHED = 1;
    public static final int CREATED = 2;
    public static final int ACTIVITY_CREATED = 3;
    public static final int STARTED = 4;
    public static final int RESUMED = 5;

    int mState;

    boolean mCalledOnAttach, mCalledOnCreate, mCalledOnActivityCreated,
            mCalledOnStart, mCalledOnResume, mCalledOnSaveInstanceState,
            mCalledOnPause, mCalledOnStop, mCalledOnDestroy, mCalledOnDetach,
            mCalledOnAttachFragment;

    static String stateToString(int state) {
        switch (state) {
            case DETACHED: return "DETACHED";
            case ATTACHED: return "ATTACHED";
            case CREATED: return "CREATED";
            case ACTIVITY_CREATED: return "ACTIVITY_CREATED";
            case STARTED: return "STARTED";
            case RESUMED: return "RESUMED";
        }
        return "(unknown " + state + ")";
    }

    public void onStateChanged(int fromState) {
        checkGetActivity();
    }

    public void checkGetActivity() {
        if (getActivity() == null) {
            throw new IllegalStateException("getActivity() returned null at unexpected time");
        }
    }

    public void checkState(String caller, int... expected) {
        if (expected == null || expected.length == 0) {
            throw new IllegalArgumentException("must supply at least one expected state");
        }
        for (int expect : expected) {
            if (mState == expect) {
                return;
            }
        }
        final StringBuilder expectString = new StringBuilder(stateToString(expected[0]));
        for (int i = 1; i < expected.length; i++) {
            expectString.append(" or ").append(stateToString(expected[i]));
        }
        throw new IllegalStateException(caller + " called while fragment was "
                + stateToString(mState) + "; expected " + expectString.toString());
    }

    public void checkStateAtLeast(String caller, int minState) {
        if (mState < minState) {
            throw new IllegalStateException(caller + " called while fragment was "
                    + stateToString(mState) + "; expected at least " + stateToString(minState));
        }
    }

    @Override
    public void onAttachFragment(Fragment childFragment) {
        mCalledOnAttachFragment = true;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCalledOnAttach = true;
        checkState("onAttach", DETACHED);
        mState = ATTACHED;
        onStateChanged(DETACHED);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mCalledOnCreate && !mCalledOnDestroy) {
            throw new IllegalStateException("onCreate called more than once with no onDestroy");
        }
        mCalledOnCreate = true;
        checkState("onCreate", ATTACHED);
        mState = CREATED;
        onStateChanged(ATTACHED);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mCalledOnActivityCreated = true;
        checkState("onActivityCreated", ATTACHED, CREATED);
        int fromState = mState;
        mState = ACTIVITY_CREATED;
        onStateChanged(fromState);
    }

    @Override
    public void onStart() {
        super.onStart();
        mCalledOnStart = true;
        checkState("onStart", ACTIVITY_CREATED);
        mState = STARTED;
        onStateChanged(ACTIVITY_CREATED);
    }

    @Override
    public void onResume() {
        super.onResume();
        mCalledOnResume = true;
        checkState("onResume", STARTED);
        mState = RESUMED;
        onStateChanged(STARTED);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mCalledOnSaveInstanceState = true;
        checkGetActivity();
        // FIXME: We should not allow onSaveInstanceState except when STARTED or greater.
        // But FragmentManager currently does it in saveAllState for fragments on the
        // back stack, so fragments may be in the CREATED state.
        checkStateAtLeast("onSaveInstanceState", CREATED);
    }

    @Override
    public void onPause() {
        super.onPause();
        mCalledOnPause = true;
        checkState("onPause", RESUMED);
        mState = STARTED;
        onStateChanged(RESUMED);
    }

    @Override
    public void onStop() {
        super.onStop();
        mCalledOnStop = true;
        checkState("onStop", STARTED);
        mState = CREATED;
        onStateChanged(STARTED);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCalledOnDestroy = true;
        checkState("onDestroy", CREATED);
        mState = ATTACHED;
        onStateChanged(CREATED);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCalledOnDetach = true;
        checkState("onDestroy", CREATED, ATTACHED);
        int fromState = mState;
        mState = DETACHED;
        onStateChanged(fromState);
    }
}
