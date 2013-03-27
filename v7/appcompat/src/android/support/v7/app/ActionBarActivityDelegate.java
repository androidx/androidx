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

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

abstract class ActionBarActivityDelegate {

    static final String METADATA_UI_OPTIONS = "android.support.UI_OPTIONS";
    static final String UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW = "splitActionBarWhenNarrow";

    private static final String TAG = "ActionBarActivityDelegate";

    final ActionBarActivity mActivity;

    ActionBarActivityDelegate(ActionBarActivity activity) {
        mActivity = activity;
    }

    abstract ActionBar createSupportActionBar();

    abstract void onCreate(Bundle savedInstanceState);

    abstract void onPostCreate(Bundle savedInstanceState);

    abstract void onConfigurationChanged(Configuration newConfig);

    abstract void setContentView(View v);

    abstract void setContentView(int resId);

    abstract void setContentView(View v, ViewGroup.LayoutParams lp);

    abstract void addContentView(View v, ViewGroup.LayoutParams lp);

    abstract boolean requestWindowFeature(int featureId);

    abstract void setTitle(CharSequence title);

    abstract void supportInvalidateOptionsMenu();

    // Methods used to create and respond to options menu
    abstract View onCreatePanelView(int featureId);

    abstract boolean onCreatePanelMenu(int featureId, android.view.Menu frameworkMenu);

    abstract boolean onMenuItemSelected(int featureId, android.view.MenuItem frameworkItem);

    abstract boolean onPreparePanel(int featureId, View view, android.view.Menu frameworkMenu);

    protected final String getUiOptionsFromMetadata() {
        try {
            PackageManager pm = mActivity.getPackageManager();
            ActivityInfo info = pm.getActivityInfo(mActivity.getComponentName(),
                    PackageManager.GET_META_DATA);

            String uiOptions = null;
            if (info.metaData != null) {
                uiOptions = info.metaData.getString(METADATA_UI_OPTIONS);
            }
            return uiOptions;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "getUiOptionsFromMetadata: Activity '" + mActivity.getClass()
                    .getSimpleName() + "' not in manifest");
            return null;
        }
    }

}
