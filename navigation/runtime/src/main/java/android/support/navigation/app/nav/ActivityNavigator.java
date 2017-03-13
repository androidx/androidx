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

package android.support.navigation.app.nav;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;

/**
 * ActivityNavigator implements cross-activity navigation.
 */
public class ActivityNavigator extends Navigator<ActivityNavigator.Destination> {
    public static final String NAME = "activity";

    private static final String EXTRA_NAV_SOURCE =
            "android-support-navigation:ActivityNavigator:source";
    private static final String EXTRA_NAV_CURRENT =
            "android-support-navigation:ActivityNavigator:current";

    private Context mContext;
    private Activity mHostActivity;

    public ActivityNavigator(Context context) {
        mContext = context;
    }

    public ActivityNavigator(Activity hostActivity) {
        mContext = mHostActivity = hostActivity;
    }

    @Override
    public Destination createDestination() {
        return new Destination(this);
    }

    @Override
    public boolean popBackStack() {
        if (mHostActivity != null) {
            int destId = 0;
            final Intent intent = mHostActivity.getIntent();
            if (intent != null) {
                destId = intent.getIntExtra(EXTRA_NAV_SOURCE, 0);
            }
            mHostActivity.finish();
            dispatchOnNavigatorNavigated(destId, true);
            return true;
        }
        return false;
    }

    @Override
    public boolean navigate(Destination destination, Bundle args, NavOptions navOptions) {
        Intent intent = new Intent(destination.getIntent());
        if (args != null) {
            intent.getExtras().putAll(args);
        }
        if (navOptions.shouldClearTask()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        if (navOptions.shouldLaunchDocument()
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        } else if (!(mContext instanceof Activity)) {
            // If we're not launching from an Activity context we have to launch in a new task.
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        if (navOptions.shouldLaunchSingleTop()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        if (mHostActivity != null) {
            final Intent hostIntent = mHostActivity.getIntent();
            if (hostIntent != null) {
                final int hostCurrentId = hostIntent.getIntExtra(EXTRA_NAV_CURRENT, 0);
                if (hostCurrentId != 0) {
                    intent.putExtra(EXTRA_NAV_SOURCE, hostCurrentId);
                }
            }
        }
        final int destId = destination.getId();
        intent.putExtra(EXTRA_NAV_CURRENT, destId);
        mContext.startActivity(intent);
        dispatchOnNavigatorNavigated(destId, false);

        // Always return false. You can't pop the back stack from the caller of a new
        // Activity, so we don't add this navigator to the controller's back stack.
        return false;
    }

    /**
     * NavDestination for activity navigation
     */
    public static class Destination extends NavDestination {
        private Intent mIntent;

        Destination(ActivityNavigator navigator) {
            super(navigator);
        }

        @Override
        public void onInflate(Context context, AttributeSet attrs) {
            super.onInflate(context, attrs);
            // TODO Implement me!
        }

        public void setIntent(Intent intent) {
            mIntent = intent;
        }

        public Intent getIntent() {
            return mIntent;
        }

        void setComponentName(ComponentName name) {
            if (mIntent == null) {
                mIntent = new Intent();
            }
            mIntent.setComponent(name);
        }

        ComponentName getComponent() {
            if (mIntent == null) {
                return null;
            }
            return mIntent.getComponent();
        }
    }
}
