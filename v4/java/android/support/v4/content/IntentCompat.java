/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.support.v4.content;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Helper for accessing features in {@link android.content.Intent}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public class IntentCompat {

    interface IntentCompatImpl {
        Intent makeMainActivity(ComponentName componentName);
    }

    static class IntentCompatImplBase implements IntentCompatImpl {
        @Override
        public Intent makeMainActivity(ComponentName componentName) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(componentName);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            return intent;
        }
    }

    static class IntentCompatImplHC implements IntentCompatImpl {
        @Override
        public Intent makeMainActivity(ComponentName componentName) {
            return IntentCompatHoneycomb.makeMainActivity(componentName);
        }
    }

    private static final IntentCompatImpl IMPL;
    static {
        if (Build.VERSION.SDK_INT >= 11) {
            IMPL = new IntentCompatImplHC();
        } else {
            IMPL = new IntentCompatImplBase();
        }
    }

    private IntentCompat() {
        /* Hide constructor */
    }

    /**
     * Broadcast Action: Resources for a set of packages (which were
     * previously unavailable) are currently
     * available since the media on which they exist is available.
     * The extra data {@link #EXTRA_CHANGED_PACKAGE_LIST} contains a
     * list of packages whose availability changed.
     * The extra data {@link #EXTRA_CHANGED_UID_LIST} contains a
     * list of uids of packages whose availability changed.
     * Note that the
     * packages in this list do <em>not</em> receive this broadcast.
     * The specified set of packages are now available on the system.
     * <p>Includes the following extras:
     * <ul>
     * <li> {@link #EXTRA_CHANGED_PACKAGE_LIST} is the set of packages
     * whose resources(were previously unavailable) are currently available.
     * {@link #EXTRA_CHANGED_UID_LIST} is the set of uids of the
     * packages whose resources(were previously unavailable)
     * are  currently available.
     * </ul>
     *
     * <p class="note">This is a protected intent that can only be sent
     * by the system.
     */
    public static final String ACTION_EXTERNAL_APPLICATIONS_AVAILABLE =
        "android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE";

    /**
     * Broadcast Action: Resources for a set of packages are currently
     * unavailable since the media on which they exist is unavailable.
     * The extra data {@link #EXTRA_CHANGED_PACKAGE_LIST} contains a
     * list of packages whose availability changed.
     * The extra data {@link #EXTRA_CHANGED_UID_LIST} contains a
     * list of uids of packages whose availability changed.
     * The specified set of packages can no longer be
     * launched and are practically unavailable on the system.
     * <p>Inclues the following extras:
     * <ul>
     * <li> {@link #EXTRA_CHANGED_PACKAGE_LIST} is the set of packages
     * whose resources are no longer available.
     * {@link #EXTRA_CHANGED_UID_LIST} is the set of packages
     * whose resources are no longer available.
     * </ul>
     *
     * <p class="note">This is a protected intent that can only be sent
     * by the system.
     */
    public static final String ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE =
        "android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE";

    /**
     * This field is part of
     * {@link android.content.Intent#ACTION_EXTERNAL_APPLICATIONS_AVAILABLE},
     * {@link android.content.Intent#ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE}
     * and contains a string array of all of the components that have changed.
     */
    public static final String EXTRA_CHANGED_PACKAGE_LIST =
            "android.intent.extra.changed_package_list";

    /**
     * This field is part of
     * {@link android.content.Intent#ACTION_EXTERNAL_APPLICATIONS_AVAILABLE},
     * {@link android.content.Intent#ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE}
     * and contains an integer array of uids of all of the components
     * that have changed.
     */
    public static final String EXTRA_CHANGED_UID_LIST =
            "android.intent.extra.changed_uid_list";

    /**
     * A constant String that is associated with the Intent, used with
     * {@link android.content.Intent#ACTION_SEND} to supply an alternative to
     * {@link android.content.Intent#EXTRA_TEXT}
     * as HTML formatted text.  Note that you <em>must</em> also supply
     * {@link android.content.Intent#EXTRA_TEXT}.
     */
    public static final String EXTRA_HTML_TEXT = "android.intent.extra.HTML_TEXT";

    /**
     * If set in an Intent passed to {@link Context#startActivity Context.startActivity()},
     * this flag will cause a newly launching task to be placed on top of the current
     * home activity task (if there is one). That is, pressing back from the task
     * will always return the user to home even if that was not the last activity they
     * saw. This can only be used in conjunction with
     * {@link android.content.Intent#FLAG_ACTIVITY_NEW_TASK}.
     */
    public static final int FLAG_ACTIVITY_TASK_ON_HOME = 0x00004000;

    /**
     * If set in an Intent passed to {@link Context#startActivity Context.startActivity()},
     * this flag will cause any existing task that would be associated with the
     * activity to be cleared before the activity is started.  That is, the activity
     * becomes the new root of an otherwise empty task, and any old activities
     * are finished.  This can only be used in conjunction with
     * {@link android.content.Intent#FLAG_ACTIVITY_NEW_TASK}.
     *
     * <p>This flag will only be obeyed on devices supporting API 11 or higher.</p>
     */
    public static final int FLAG_ACTIVITY_CLEAR_TASK = 0x00008000;

    /**
     * Create an intent to launch the main (root) activity of a task.  This
     * is the Intent that is started when the application's is launched from
     * Home.  For anything else that wants to launch an application in the
     * same way, it is important that they use an Intent structured the same
     * way, and can use this function to ensure this is the case.
     *
     * <p>The returned Intent has the given Activity component as its explicit
     * component, {@link Intent#ACTION_MAIN ACTION_MAIN} as its action, and includes the
     * category {@link Intent#CATEGORY_LAUNCHER CATEGORY_LAUNCHER}.  This does <em>not</em> have
     * {@link Intent#FLAG_ACTIVITY_NEW_TASK FLAG_ACTIVITY_NEW_TASK} set,
     * though typically you will want to do that through {@link Intent#addFlags(int) addFlags(int)}
     * on the returned Intent.
     *
     * @param mainActivity The main activity component that this Intent will
     * launch.
     * @return Returns a newly created Intent that can be used to launch the
     * activity as a main application entry.
     *
     * @see Intent#setClass
     * @see Intent#setComponent
     */
    public static Intent makeMainActivity(ComponentName mainActivity) {
        return IMPL.makeMainActivity(mainActivity);
    }
}
