// CHECKSTYLE:OFF Generated code
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

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.Fragment;
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
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browse_support);

        testError();
    }

    private void testError() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getSupportFragmentManager().isDestroyed()) {
                    return;
                }
                getSupportFragmentManager().beginTransaction().add(R.id.main_frame,
                        new ErrorSupportFragment()).commit();
            }
        }, 3000);
    }

    static public class SpinnerSupportFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                    Bundle savedInstanceState) {
            ProgressBar progressBar = new ProgressBar(container.getContext());
            if (container instanceof FrameLayout) {
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(100, 100,
                        Gravity.CENTER);
                progressBar.setLayoutParams(layoutParams);
            }
            return progressBar;
        }
    }
}
