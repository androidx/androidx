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

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;

public class ErrorFragment extends androidx.leanback.app.ErrorFragment {
    private static final String TAG = "leanback.ErrorFragment";
    private static final boolean TRANSLUCENT = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setTitle("Leanback Sample App");
        final Context context = getActivity();
        setImageDrawable(ResourcesCompat.getDrawable(context.getResources(),
                R.drawable.lb_ic_sad_cloud, context.getTheme()));
        setMessage("An error occurred.");
        setDefaultBackground(TRANSLUCENT);

        setButtonText("Dismiss");
        setButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Log.i(TAG, "button clicked");
                getFragmentManager().beginTransaction().remove(ErrorFragment.this).commit();
            }
        });
    }
}
