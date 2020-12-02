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
package androidx.leanback.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.leanback.test.R;

public class BrowseSupportFragmentTestActivity extends FragmentActivity {

    public static final String EXTRA_ADD_TO_BACKSTACK = "addToBackStack";
    public static final String EXTRA_NUM_ROWS = "numRows";
    public static final String EXTRA_REPEAT_PER_ROW = "repeatPerRow";
    public static final String EXTRA_LOAD_DATA_DELAY = "loadDataDelay";
    public static final String EXTRA_TEST_ENTRANCE_TRANSITION = "testEntranceTransition";
    public static final String EXTRA_SET_ADAPTER_AFTER_DATA_LOAD = "set_adapter_after_data_load";
    public static final String EXTRA_HEADERS_STATE = "headers_state";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        setContentView(R.layout.browse);
        if (savedInstanceState == null) {
            Bundle arguments = new Bundle();
            arguments.putAll(intent.getExtras());
            BrowseTestSupportFragment fragment = new BrowseTestSupportFragment();
            fragment.setArguments(arguments);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.main_frame, fragment);
            if (intent.getBooleanExtra(EXTRA_ADD_TO_BACKSTACK, false)) {
                ft.addToBackStack(null);
            }
            ft.commit();
        }
    }

    public BrowseTestSupportFragment getBrowseTestSupportFragment() {
        return (BrowseTestSupportFragment) getSupportFragmentManager().findFragmentById(R.id.main_frame);
    }
}
