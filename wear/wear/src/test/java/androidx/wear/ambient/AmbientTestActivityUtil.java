/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.ambient;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.pm.ActivityInfo;

import androidx.test.core.app.ActivityScenario;

import org.robolectric.shadows.ShadowPackageManager;

public class AmbientTestActivityUtil {
    /** Register an activity with Robolectric, and launch it. */
    public static <T extends Activity> ActivityScenario<T> launchActivity(Class<T> activity) {
        ShadowPackageManager shadowPackageManager =
                shadowOf(getApplicationContext().getPackageManager());
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.name = activity.getName();
        activityInfo.packageName = getApplicationContext().getPackageName();
        shadowPackageManager.addOrUpdateActivity(activityInfo);

        return ActivityScenario.launch(activity);
    }
}
