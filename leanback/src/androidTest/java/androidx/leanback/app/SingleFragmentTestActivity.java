// CHECKSTYLE:OFF Generated code
/* This file is auto-generated from SingleSupportFragmentTestActivity.java.  DO NOT MODIFY. */

/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.leanback.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.leanback.test.R;

public class SingleFragmentTestActivity extends Activity {

    /**
     * Fragment that will be added to activity
     */
    public static final String EXTRA_FRAGMENT_NAME = "fragmentName";

    public static final String EXTRA_ACTIVITY_LAYOUT = "activityLayout";

    public static final String EXTRA_UI_VISIBILITY = "uiVisibility";

    public static final String EXTRA_OVERRIDDEN_SAVED_INSTANCE_STATE =
            "overriddenSavedInstanceState";

    private static final String TAG = "TestActivity";

    private Bundle overrideSavedInstance(Bundle savedInstance) {
        Intent intent = getIntent();
        if (intent != null) {
            Bundle b = intent.getBundleExtra(EXTRA_OVERRIDDEN_SAVED_INSTANCE_STATE);
            if (b != null) {
                return b;
            }
        }
        return savedInstance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        savedInstanceState = overrideSavedInstance(savedInstanceState);
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate " + this);
        Intent intent = getIntent();

        final int uiOptions = intent.getIntExtra(EXTRA_UI_VISIBILITY, 0);
        if (uiOptions != 0) {
            getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        }

        setContentView(intent.getIntExtra(EXTRA_ACTIVITY_LAYOUT, R.layout.single_fragment));
        if (savedInstanceState == null && findViewById(R.id.main_frame) != null) {
            try {
                Fragment fragment = (Fragment) Class.forName(
                        intent.getStringExtra(EXTRA_FRAGMENT_NAME)).newInstance();
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.main_frame, fragment);
                ft.commit();
            } catch (Exception ex) {
                ex.printStackTrace();
                finish();
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(overrideSavedInstance(savedInstanceState));
    }

    public Bundle performSaveInstanceState() {
        Bundle state = new Bundle();
        onSaveInstanceState(state);
        return state;
    }

    public Fragment getTestFragment() {
        return getFragmentManager().findFragmentById(R.id.main_frame);
    }
}
