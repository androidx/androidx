/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.runtime.testing

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.ActivityPanelEntity

/** Test-only implementation of [ActivityPanelEntity] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeActivityPanelEntity : FakePanelEntity(), ActivityPanelEntity {

    /** The intent that was last used to launch an activity. */
    public var launchIntent: Intent = Intent()
        private set

    /** The bundle that was last used to launch an activity. */
    public var launchBundle: Bundle? = null
        private set

    /**
     * Launches the given activity into the panel.
     *
     * @param intent Intent to launch the activity.
     * @param bundle Bundle to pass to the activity, can be null.
     */
    override fun launchActivity(intent: Intent, bundle: Bundle?) {
        launchIntent = intent
        launchBundle = bundle
    }

    /** The activity that was last moved into the panel. */
    public var movedActivity: Activity = Activity()
        private set

    /**
     * Moves the given activity into the panel.
     *
     * @param activity Activity to move into the ActivityPanel.
     */
    override fun moveActivity(activity: Activity) {
        movedActivity = activity
    }
}
