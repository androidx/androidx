/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v7.app;

import android.os.Bundle;
import android.view.Window;

class ActionBarActivityDelegateHC extends ActionBarActivityDelegateBase {

    ActionBarActivityDelegateHC(ActionBarActivity activity) {
        super(activity);
    }

    @Override
    void onCreate(Bundle savedInstanceState) {
        /**
         * A native Action Mode could be displayed (text selection, etc) so we need to make sure it
         * is positioned correctly. Here we request the ACTION_MODE_OVERLAY feature so that it
         * displays over the compat Action Bar.
         * {@link android.support.v7.internal.widget.NativeActionModeAwareLayout} is responsible for
         * making sure that the compat Action Bar is visible when an Action Mode is started
         * (for positioning).
         */
        mActivity.getWindow().requestFeature(Window.FEATURE_ACTION_MODE_OVERLAY);
        super.onCreate(savedInstanceState);
    }

    @Override
    public ActionBar createSupportActionBar() {
        ensureSubDecor();
        return new ActionBarImplHC(mActivity, mActivity);
    }
}
