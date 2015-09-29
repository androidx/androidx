/* This file is auto-generated from BrowseErrorActivity.java.  DO NOT MODIFY. */

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
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

public class BrowseErrorSupportActivity extends FragmentActivity
{
    private ErrorSupportFragment mErrorSupportFragment;
    private SpinnerSupportFragment mSpinnerSupportFragment;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browse_support);

        testError();
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

    private void testError() {
        mErrorSupportFragment = new ErrorSupportFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.main_frame, mErrorSupportFragment).commit();

        mSpinnerSupportFragment = new SpinnerSupportFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.main_frame, mSpinnerSupportFragment).commit();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getSupportFragmentManager().isDestroyed()) {
                    return;
                }
                getSupportFragmentManager().beginTransaction().remove(mSpinnerSupportFragment).commit();
                mErrorSupportFragment.setErrorContent(getResources());
            }
        }, 3000);
    }

    static public class SpinnerSupportFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                    Bundle savedInstanceState) {
            ProgressBar progressBar = new ProgressBar(container.getContext());
            if (container instanceof FrameLayout) {
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(100, 100, Gravity.CENTER);
                progressBar.setLayoutParams(layoutParams);
            }
            return progressBar;
        }
    }
}
