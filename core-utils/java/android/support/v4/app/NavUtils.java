/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.support.v4.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * NavUtils provides helper functionality for applications implementing
 * recommended Android UI navigation patterns. For information about recommended
 * navigation patterns see
 * <a href="{@docRoot}guide/topics/fundamentals/tasks-and-back-stack.html">Tasks and Back Stack</a>
 * from the developer guide and <a href="{@docRoot}design/patterns/navigation.html">Navigation</a>
 * from the design guide.
 */
public final class NavUtils {
    private static final String TAG = "NavUtils";
    public static final String PARENT_ACTIVITY = "android.support.PARENT_ACTIVITY";

    /**
     * Returns true if sourceActivity should recreate the task when navigating 'up'
     * by using targetIntent.
     *
     * <p>If this method returns false the app can trivially call
     * {@link #navigateUpTo(Activity, Intent)} using the same parameters to correctly perform
     * up navigation. If this method returns true, the app should synthesize a new task stack
     * by using {@link TaskStackBuilder} or another similar mechanism to perform up navigation.</p>
     *
     * @param sourceActivity The current activity from which the user is attempting to navigate up
     * @param targetIntent An intent representing the target destination for up navigation
     * @return true if navigating up should recreate a new task stack, false if the same task
     *         should be used for the destination
     */
    public static boolean shouldUpRecreateTask(Activity sourceActivity, Intent targetIntent) {
        if (Build.VERSION.SDK_INT >= 16) {
            return sourceActivity.shouldUpRecreateTask(targetIntent);
        } else {
            String action = sourceActivity.getIntent().getAction();
            return action != null && !action.equals(Intent.ACTION_MAIN);
        }
    }

    /**
     * Convenience method that is equivalent to calling
     * <code>{@link #navigateUpTo(Activity, Intent) navigateUpTo}(sourceActivity,
     * {@link #getParentActivityIntent(Activity) getParentActivityIntent} (sourceActivity))</code>.
     * sourceActivity will be finished by this call.
     *
     * <p><em>Note:</em> This method should only be used when sourceActivity and the corresponding
     * parent are within the same task. If up navigation should cross tasks in some cases, see
     * {@link #shouldUpRecreateTask(Activity, Intent)}.</p>
     *
     * @param sourceActivity The current activity from which the user is attempting to navigate up
     */
    public static void navigateUpFromSameTask(Activity sourceActivity) {
        Intent upIntent = getParentActivityIntent(sourceActivity);

        if (upIntent == null) {
            throw new IllegalArgumentException("Activity " +
                    sourceActivity.getClass().getSimpleName() +
                    " does not have a parent activity name specified." +
                    " (Did you forget to add the android.support.PARENT_ACTIVITY <meta-data> " +
                    " element in your manifest?)");
        }

        navigateUpTo(sourceActivity, upIntent);
    }

    /**
     * Navigate from sourceActivity to the activity specified by upIntent, finishing sourceActivity
     * in the process. upIntent will have the flag {@link Intent#FLAG_ACTIVITY_CLEAR_TOP} set
     * by this method, along with any others required for proper up navigation as outlined
     * in the Android Design Guide.
     *
     * <p>This method should be used when performing up navigation from within the same task
     * as the destination. If up navigation should cross tasks in some cases, see
     * {@link #shouldUpRecreateTask(Activity, Intent)}.</p>
     *
     * @param sourceActivity The current activity from which the user is attempting to navigate up
     * @param upIntent An intent representing the target destination for up navigation
     */
    public static void navigateUpTo(Activity sourceActivity, Intent upIntent) {
        if (Build.VERSION.SDK_INT >= 16) {
            sourceActivity.navigateUpTo(upIntent);
        } else {
            upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            sourceActivity.startActivity(upIntent);
            sourceActivity.finish();
        }
    }

    /**
     * Obtain an {@link Intent} that will launch an explicit target activity
     * specified by sourceActivity's {@link #PARENT_ACTIVITY} &lt;meta-data&gt;
     * element in the application's manifest. If the device is running
     * Jellybean or newer, the android:parentActivityName attribute will be preferred
     * if it is present.
     *
     * @param sourceActivity Activity to fetch a parent intent for
     * @return a new Intent targeting the defined parent activity of sourceActivity
     */
    public static Intent getParentActivityIntent(Activity sourceActivity) {
        if (Build.VERSION.SDK_INT >= 16) {
            // Prefer the "real" JB definition if available,
            // else fall back to the meta-data element.
            Intent result = sourceActivity.getParentActivityIntent();
            if (result != null) {
                return result;
            }
        }
        String parentName = NavUtils.getParentActivityName(sourceActivity);
        if (parentName == null) return null;

        // If the parent itself has no parent, generate a main activity intent.
        final ComponentName target = new ComponentName(sourceActivity, parentName);
        try {
            final String grandparent = NavUtils.getParentActivityName(sourceActivity, target);
            return grandparent == null
                    ? Intent.makeMainActivity(target)
                    : new Intent().setComponent(target);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "getParentActivityIntent: bad parentActivityName '" + parentName
                    + "' in manifest");
            return null;
        }
    }

    /**
     * Obtain an {@link Intent} that will launch an explicit target activity
     * specified by sourceActivityClass's {@link #PARENT_ACTIVITY} &lt;meta-data&gt;
     * element in the application's manifest.
     *
     * @param context Context for looking up the activity component for sourceActivityClass
     * @param sourceActivityClass {@link java.lang.Class} object for an Activity class
     * @return a new Intent targeting the defined parent activity of sourceActivity
     * @throws NameNotFoundException if the ComponentName for sourceActivityClass is invalid
     */
    public static Intent getParentActivityIntent(Context context, Class<?> sourceActivityClass)
            throws NameNotFoundException {
        String parentActivity = getParentActivityName(context,
                new ComponentName(context, sourceActivityClass));
        if (parentActivity == null) return null;

        // If the parent itself has no parent, generate a main activity intent.
        final ComponentName target = new ComponentName(context, parentActivity);
        final String grandparent = getParentActivityName(context, target);
        final Intent parentIntent = grandparent == null
                ? Intent.makeMainActivity(target)
                : new Intent().setComponent(target);
        return parentIntent;
    }

    /**
     * Obtain an {@link Intent} that will launch an explicit target activity
     * specified by sourceActivityClass's {@link #PARENT_ACTIVITY} &lt;meta-data&gt;
     * element in the application's manifest.
     *
     * @param context Context for looking up the activity component for the source activity
     * @param componentName ComponentName for the source Activity
     * @return a new Intent targeting the defined parent activity of sourceActivity
     * @throws NameNotFoundException if the ComponentName for sourceActivityClass is invalid
     */
    public static Intent getParentActivityIntent(Context context, ComponentName componentName)
            throws NameNotFoundException {
        String parentActivity = getParentActivityName(context, componentName);
        if (parentActivity == null) return null;

        // If the parent itself has no parent, generate a main activity intent.
        final ComponentName target = new ComponentName(
                componentName.getPackageName(), parentActivity);
        final String grandparent = getParentActivityName(context, target);
        final Intent parentIntent = grandparent == null
                ? Intent.makeMainActivity(target)
                : new Intent().setComponent(target);
        return parentIntent;
    }

    /**
     * Return the fully qualified class name of sourceActivity's parent activity as specified by
     * a {@link #PARENT_ACTIVITY} &lt;meta-data&gt; element within the activity element in
     * the application's manifest.
     *
     * @param sourceActivity Activity to fetch a parent class name for
     * @return The fully qualified class name of sourceActivity's parent activity or null if
     *         it was not specified
     */
    @Nullable
    public static String getParentActivityName(Activity sourceActivity) {
        try {
            return getParentActivityName(sourceActivity, sourceActivity.getComponentName());
        } catch (NameNotFoundException e) {
            // Component name of supplied activity does not exist...?
            throw new IllegalArgumentException(e);
        }
    }
    /**
     * Return the fully qualified class name of a source activity's parent activity as specified by
     * a {@link #PARENT_ACTIVITY} &lt;meta-data&gt; element within the activity element in
     * the application's manifest. The source activity is provided by componentName.
     *
     * @param context Context for looking up the activity component for the source activity
     * @param componentName ComponentName for the source Activity
     * @return The fully qualified class name of sourceActivity's parent activity or null if
     *         it was not specified
     */
    @Nullable
    public static String getParentActivityName(Context context, ComponentName componentName)
            throws NameNotFoundException {
        PackageManager pm = context.getPackageManager();
        ActivityInfo info = pm.getActivityInfo(componentName, PackageManager.GET_META_DATA);
        if (Build.VERSION.SDK_INT >= 16) {
            String result = info.parentActivityName;
            if (result != null) {
                return result;
            }
        }
        if (info.metaData == null) {
            return null;
        }
        String parentActivity = info.metaData.getString(PARENT_ACTIVITY);
        if (parentActivity == null) {
            return null;
        }
        if (parentActivity.charAt(0) == '.') {
            parentActivity = context.getPackageName() + parentActivity;
        }
        return parentActivity;
    }

    /** No instances! */
    private NavUtils() {
    }
}
