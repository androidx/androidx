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

package androidx.leanback.widget.picker;

import android.app.Activity;
import android.os.Bundle;

import androidx.leanback.test.R;


public class TimePickerActivity extends Activity {

    public static final String EXTRA_LAYOUT_RESOURCE_ID = "layoutResourceId";

    int mLayoutId;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLayoutId = getIntent().getIntExtra(EXTRA_LAYOUT_RESOURCE_ID,
                R.layout.timepicker_with_other_widgets);
        setContentView(mLayoutId);
    }
}
