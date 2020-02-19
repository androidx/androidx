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

package androidx.leanback.app.wizard;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

import androidx.leanback.app.GuidedStepFragment;

public class GuidedStepAttributesTestActivity extends Activity {

    private GuidedStepAttributesTestFragment mGuidedStepAttributesTestFragment;

    public static String EXTRA_GUIDANCE = "guidance";
    public static String EXTRA_ACTION_LIST = "actionList";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGuidedStepAttributesTestFragment = new GuidedStepAttributesTestFragment();
        GuidedStepFragment.addAsRoot(this, mGuidedStepAttributesTestFragment, android.R.id.content);
    }
    public Fragment getGuidedStepTestFragment() {
        return getFragmentManager().findFragmentById(android.R.id.content);
    }
}
