/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.support.v17.leanback.app;

import android.app.Activity;
import android.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.tests.R;

/**
 * @hide from javadoc
 */
public class BrowseFragmentTestActivity extends Activity {

    public static final String EXTRA_ADD_TO_BACKSTACK = "addToBackStack";
    public static final String EXTRA_NUM_ROWS = "numRows";
    public static final String EXTRA_REPEAT_PER_ROW = "repeatPerRow";
    public static final String EXTRA_LOAD_DATA_DELAY = "loadDataDelay";
    public static final String EXTRA_TEST_ENTRANCE_TRANSITION = "testEntranceTransition";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        BrowseTestFragment.NUM_ROWS = intent.getIntExtra(EXTRA_NUM_ROWS,
                BrowseTestFragment.DEFAULT_NUM_ROWS);
        BrowseTestFragment.REPEAT_PER_ROW = intent.getIntExtra(EXTRA_REPEAT_PER_ROW,
                BrowseTestFragment.DEFAULT_REPEAT_PER_ROW);
        BrowseTestFragment.LOAD_DATA_DELAY = intent.getLongExtra(EXTRA_LOAD_DATA_DELAY,
                BrowseTestFragment.DEFAULT_LOAD_DATA_DELAY);
        BrowseTestFragment.TEST_ENTRANCE_TRANSITION = intent.getBooleanExtra(
                EXTRA_TEST_ENTRANCE_TRANSITION,
                BrowseTestFragment.DEFAULT_TEST_ENTRANCE_TRANSITION);
        setContentView(R.layout.browse);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.main_frame, new BrowseTestFragment());
        if (intent.getBooleanExtra(EXTRA_ADD_TO_BACKSTACK, false)) {
            ft.addToBackStack(null);
        }
        ft.commit();
    }
}
