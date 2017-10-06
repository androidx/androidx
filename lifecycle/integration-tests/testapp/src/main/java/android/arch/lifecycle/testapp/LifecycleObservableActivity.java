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

package android.arch.lifecycle.testapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

/**
 * Simple test activity
 */
public class LifecycleObservableActivity
        extends FragmentActivity
        implements OnSaveInstanceStateObservable {

    private OnSaveInstanceStateListener mOnSaveInstanceStateListener;

    /**
     * Runs a replace fragment transaction with 'fragment' on this Activity.
     */
    public void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.activityFrameLayout, fragment)
                .commitNow();
    }

    @Override
    public void setOnSaveInstanceStateListener(
            OnSaveInstanceStateListener onSaveInstanceStateListener) {
        mOnSaveInstanceStateListener = onSaveInstanceStateListener;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mOnSaveInstanceStateListener != null) {
            mOnSaveInstanceStateListener.onSaveInstanceState();
        }
    }
}
