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

import android.os.Bundle;

public class ReentrantFragment extends StrictFragment {
    private static final String FROM_STATE = "fromState";
    private static final String TO_STATE = "toState";
    int mFromState = 0;
    int mToState = 0;
    boolean mIsRestored;

    public static ReentrantFragment create(int fromState, int toState) {
        ReentrantFragment fragment = new ReentrantFragment();
        fragment.mFromState = fromState;
        fragment.mToState = toState;
        fragment.mIsRestored = false;
        return fragment;
    }

    @Override
    public void onStateChanged(int fromState) {
        super.onStateChanged(fromState);
        // We execute the transaction when shutting down or after restoring
        if (fromState == mFromState && mState == mToState
                && (mToState < mFromState || mIsRestored)) {
            executeTransaction();
        }
    }

    private void executeTransaction() {
        getFragmentManager().beginTransaction()
                .add(new StrictFragment(), "should throw")
                .commitNow();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(FROM_STATE, mFromState);
        outState.putInt(TO_STATE, mToState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mFromState = savedInstanceState.getInt(FROM_STATE);
            mToState = savedInstanceState.getInt(TO_STATE);
            mIsRestored = true;
        }
        super.onCreate(savedInstanceState);
    }
}

