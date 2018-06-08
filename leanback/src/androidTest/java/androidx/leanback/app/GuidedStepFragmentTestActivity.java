// CHECKSTYLE:OFF Generated code
/* This file is auto-generated from GuidedStepSupportFragmentTestActivity.java.  DO NOT MODIFY. */

/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.leanback.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class GuidedStepFragmentTestActivity extends Activity {

    /**
     * Frst Test that will be included in this Activity
     */
    public static final String EXTRA_TEST_NAME = "testName";
    /**
     * True(default) to addAsRoot() for first Test, false to use add()
     */
    public static final String EXTRA_ADD_AS_ROOT = "addAsRoot";

    /**
     * Layout direction
     */
    public static final String EXTRA_LAYOUT_DIRECTION = "layoutDir";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        int layoutDirection = intent.getIntExtra(EXTRA_LAYOUT_DIRECTION, -1);
        if (layoutDirection != -1) {
            findViewById(android.R.id.content).setLayoutDirection(layoutDirection);
        }
        if (savedInstanceState == null) {
            String firstTestName = intent.getStringExtra(EXTRA_TEST_NAME);
            if (firstTestName != null) {
                GuidedStepTestFragment testFragment = new GuidedStepTestFragment(firstTestName);
                if (intent.getBooleanExtra(EXTRA_ADD_AS_ROOT, true)) {
                    GuidedStepTestFragment.addAsRoot(this, testFragment, android.R.id.content);
                } else {
                    GuidedStepTestFragment.add(getFragmentManager(), testFragment,
                            android.R.id.content);
                }
            }
        }
    }
}
