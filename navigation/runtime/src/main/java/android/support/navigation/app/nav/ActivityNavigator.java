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
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.android.support.navigation.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ActivityNavigator implements cross-activity navigation.
 */
@Navigator.Name("activity")
public class ActivityNavigator extends Navigator<ActivityNavigator.Destination> {
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
    public void navigate(Destination destination, Bundle args, NavOptions navOptions) {
        Intent intent = new Intent(destination.getIntent());
        if (args != null) {
            intent.putExtras(args);
            String dataPattern = destination.getDataPattern();
            if (!TextUtils.isEmpty(dataPattern)) {
                // Fill in the data pattern with the args to build a valid URI
                StringBuffer data = new StringBuffer();
                Pattern fillInPattern = Pattern.compile("\\{(.+?)\\}");
                Matcher matcher = fillInPattern.matcher(dataPattern);
                while (matcher.find()) {
                    String argName = matcher.group(1);
                    if (args.containsKey(argName)) {
                        matcher.appendReplacement(data, "");
                        data.append(Uri.encode(args.getString(argName)));
                    } else {
                        throw new IllegalArgumentException("Could not find " + argName + " in "
                                + args + " to fill data pattern " + dataPattern);
                    }
                }
                matcher.appendTail(data);
                intent.setData(Uri.parse(data.toString()));
            }
        }
        if (navOptions != null && navOptions.shouldClearTask()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        if (navOptions != null && navOptions.shouldLaunchDocument()
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        } else if (!(mContext instanceof Activity)) {
            // If we're not launching from an Activity context we have to launch in a new task.
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        if (navOptions != null && navOptions.shouldLaunchSingleTop()) {
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
        if (navOptions != null && mHostActivity != null) {
            int enterAnim = navOptions.getEnterAnim();
            int exitAnim = navOptions.getExitAnim();
            if (enterAnim != -1 || exitAnim != -1) {
                enterAnim = enterAnim != -1 ? enterAnim : 0;
                exitAnim = exitAnim != -1 ? exitAnim : 0;
                mHostActivity.overridePendingTransition(enterAnim, exitAnim);
            }
        }
        dispatchOnNavigatorNavigated(destId, false);
    }

    /**
     * NavDestination for activity navigation
     */
    public static class Destination extends NavDestination {
        private Intent mIntent;
        private String mDataPattern;

        /**
         * Construct a new activity destination to navigate to the given Intent.
         *
         * @param navigatorProvider The {@link NavController} which this destination
         *                          will be associated with.
         * @param intent Intent this destination should trigger when navigated to
         */
        public Destination(@NonNull NavigatorProvider navigatorProvider, Intent intent) {
            super(navigatorProvider.getNavigator(ActivityNavigator.class));
            setIntent(intent);
        }

        Destination(@NonNull ActivityNavigator navigator) {
            super(navigator);
        }

        @Override
        public void onInflate(Context context, AttributeSet attrs) {
            super.onInflate(context, attrs);
            TypedArray a = context.getResources().obtainAttributes(attrs,
                    R.styleable.ActivityNavigator);
            String cls = a.getString(R.styleable.ActivityNavigator_android_name);
            if (!TextUtils.isEmpty(cls)) {
                setComponentName(new ComponentName(context, cls));
            }
            setAction(a.getString(R.styleable.ActivityNavigator_action));
            String data = a.getString(R.styleable.ActivityNavigator_data);
            if (data != null) {
                setData(Uri.parse(data));
            }
            setDataPattern(a.getString(R.styleable.ActivityNavigator_dataPattern));
            a.recycle();
        }

        /**
         * Set the Intent to start when navigating to this destination.
         * @param intent Intent to associated with this destination.
         */
        public void setIntent(Intent intent) {
            mIntent = intent;
        }

        /**
         * Gets the Intent associated with this destination.
         * @return
         */
        public Intent getIntent() {
            return mIntent;
        }

        /**
         * Set an explicit {@link ComponentName} to navigate to.
         *
         * @param name The component name of the Activity to start.
         */
        public void setComponentName(ComponentName name) {
            if (mIntent == null) {
                mIntent = new Intent();
            }
            mIntent.setComponent(name);
        }

        /**
         * Get the explicit {@link ComponentName} associated with this destination, if any
         * @return
         */
        public ComponentName getComponent() {
            if (mIntent == null) {
                return null;
            }
            return mIntent.getComponent();
        }

        /**
         * Sets the action sent when navigating to this destination.
         * @param action The action string to use.
         */
        public void setAction(String action) {
            if (mIntent == null) {
                mIntent = new Intent();
            }
            mIntent.setAction(action);
        }

        /**
         * Get the action used to start the Activity, if any
         */
        public String getAction() {
            if (mIntent == null) {
                return null;
            }
            return mIntent.getAction();
        }

        /**
         * Sets a static data URI that is sent when navigating to this destination.
         *
         * <p>To use a dynamic URI that changes based on the arguments passed in when navigating,
         * use {@link #setDataPattern(String)}, which will take precedence when arguments are
         * present.</p>
         *
         * @param data A static URI that should always be used.
         * @see #setDataPattern(String)
         */
        public void setData(Uri data) {
            if (mIntent == null) {
                mIntent = new Intent();
            }
            mIntent.setData(data);
        }

        /**
         * Get the data URI used to start the Activity, if any
         */
        public Uri getData() {
            if (mIntent == null) {
                return null;
            }
            return mIntent.getData();
        }

        /**
         * Sets a dynamic data URI pattern that is sent when navigating to this destination.
         *
         * <p>If a non-null arguments Bundle is present when navigating, any segments in the form
         * <code>{argName}</code> will be replaced with a URI encoded string from the arguments.</p>
         * @param dataPattern A URI pattern with segments in the form of <code>{argName}</code> that
         *                    will be replaced with URI encoded versions of the Strings in the
         *                    arguments Bundle.
         * @see #setData
         */
        public void setDataPattern(String dataPattern) {
            mDataPattern = dataPattern;
        }

        /**
         * Gets the dynamic data URI pattern, if any
         */
        public String getDataPattern() {
            return mDataPattern;
        }
    }
}
