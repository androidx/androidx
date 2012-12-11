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

package android.support.v4.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

/**
 * Helper for accessing features in {@link android.app.Activity}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public class ActivityCompat extends ContextCompat {
    /**
     * Invalidate the activity's options menu, if able.
     *
     * <p>Before API level 11 (Android 3.0/Honeycomb) the lifecycle of the
     * options menu was controlled primarily by the user's operation of
     * the hardware menu key. When the user presses down on the menu key
     * for the first time the menu was created and prepared by calls
     * to {@link Activity#onCreateOptionsMenu(android.view.Menu)} and
     * {@link Activity#onPrepareOptionsMenu(android.view.Menu)} respectively.
     * Subsequent presses of the menu key kept the existing instance of the
     * Menu itself and called {@link Activity#onPrepareOptionsMenu(android.view.Menu)}
     * to give the activity an opportunity to contextually alter the menu
     * before the menu panel was shown.</p>
     *
     * <p>In Android 3.0+ the Action Bar forces the options menu to be built early
     * so that items chosen to show as actions may be displayed when the activity
     * first becomes visible. The Activity method invalidateOptionsMenu forces
     * the entire menu to be destroyed and recreated from
     * {@link Activity#onCreateOptionsMenu(android.view.Menu)}, offering a similar
     * though heavier-weight opportunity to change the menu's contents. Normally
     * this functionality is used to support a changing configuration of Fragments.</p>
     *
     * <p>Applications may use this support helper to signal a significant change in
     * activity state that should cause the options menu to be rebuilt. If the app
     * is running on an older platform version that does not support menu invalidation
     * the app will still receive {@link Activity#onPrepareOptionsMenu(android.view.Menu)}
     * the next time the user presses the menu key and this method will return false.
     * If this method returns true the options menu was successfully invalidated.</p>
     *
     * @param activity Invalidate the options menu of this activity
     * @return true if this operation was supported and it completed; false if it was not available.
     */
    public static boolean invalidateOptionsMenu(Activity activity) {
        if (Build.VERSION.SDK_INT >= 11) {
            ActivityCompatHoneycomb.invalidateOptionsMenu(activity);
            return true;
        }
        return false;
    }

    /**
     * Start an activity with additional launch information, if able.
     *
     * <p>In Android 4.1+ additional options were introduced to allow for more
     * control on activity launch animations. Applications can use this method
     * along with {@link ActivityOptionsCompat} to use these animations when
     * available. When run on versions of the platform where this feature does
     * not exist the activity will be launched normally.</p>
     *
     * @param activity Context to launch activity from.
     * @param intent The description of the activity to start.
     * @param options Additional options for how the Activity should be started.
     *                May be null if there are no options. See
     *                {@link ActivityOptionsCompat} for how to build the Bundle
     *                supplied here; there are no supported definitions for
     *                building it manually.
     */
    public static void startActivity(Activity activity, Intent intent, Bundle options) {
        if (Build.VERSION.SDK_INT >= 16) {
            ActivityCompatJB.startActivity(activity, intent, options);
        } else {
            activity.startActivity(intent);
        }
    }

    /**
     * Start new activity with options, if able, for which you would like a
     * result when it finished.
     *
     * <p>In Android 4.1+ additional options were introduced to allow for more
     * control on activity launch animations. Applications can use this method
     * along with {@link ActivityOptionsCompat} to use these animations when
     * available. When run on versions of the platform where this feature does
     * not exist the activity will be launched normally.</p>
     *
     * @param activity Origin activity to launch from.
     * @param intent The description of the activity to start.
     * @param requestCode If >= 0, this code will be returned in
     *                   onActivityResult() when the activity exits.
     * @param options Additional options for how the Activity should be started.
     *                May be null if there are no options. See
     *                {@link ActivityOptionsCompat} for how to build the Bundle
     *                supplied here; there are no supported definitions for
     *                building it manually.
     */
    public static void startActivityForResult(Activity activity, Intent intent, int requestCode, Bundle options) {
        if (Build.VERSION.SDK_INT >= 16) {
            ActivityCompatJB.startActivityForResult(activity, intent, requestCode, options);
        } else {
            activity.startActivityForResult(intent, requestCode);
        }
    }
}
