/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.fragment.app.testing;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle.State;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A minimum fragment to demonstrate unit testing relating to Fragment's life cycle events. It
 * records its lifecycle state and the number of times itself is recreated by the lifecycle event.
 */
public class StateRecordingFragment extends Fragment {

    private static final String KEY_NUM_OF_CREATION = "NUM_OF_CREATION";

    private int mNumOfCreation = 0;
    private State mCurrentState = State.INITIALIZED;
    private boolean mIsViewAttachedToWindow = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mNumOfCreation = savedInstanceState.getInt(KEY_NUM_OF_CREATION, 0);
        } else {
            mNumOfCreation = 0;
        }

        mCurrentState = State.CREATED;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return new View(inflater.getContext()) {
            @Override
            protected void onAttachedToWindow() {
                super.onAttachedToWindow();
                mIsViewAttachedToWindow = true;
            }

            @Override
            protected void onDetachedFromWindow() {
                super.onDetachedFromWindow();
                mIsViewAttachedToWindow = false;
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        mCurrentState = State.STARTED;
    }

    @Override
    public void onResume() {
        super.onResume();
        mCurrentState = State.RESUMED;
    }

    @Override
    public void onPause() {
        super.onPause();
        mCurrentState = State.STARTED;
    }

    @Override
    public void onStop() {
        super.onStop();
        mCurrentState = State.CREATED;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCurrentState = State.DESTROYED;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_NUM_OF_CREATION, mNumOfCreation + 1);
    }

    public int getNumberOfRecreations() {
        return mNumOfCreation;
    }

    public State getState() {
        return mCurrentState;
    }

    public boolean isViewAttachedToWindow() {
        return mIsViewAttachedToWindow;
    }
}
