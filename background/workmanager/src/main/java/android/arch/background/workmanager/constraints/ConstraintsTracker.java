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
package android.arch.background.workmanager.constraints;

import android.arch.background.workmanager.WorkDatabase;
import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;

/**
 * A class to track the current status of various constraints.
 */

public class ConstraintsTracker {

    private LifecycleOwner mLifecycleOwner;
    private ConstraintsState mConstraintsState;

    private ConstraintController mBatteryController;

    public ConstraintsTracker(
            Context context,
            LifecycleOwner lifecycleOwner,
            ConstraintsState.Listener constraintsStateListener,
            WorkDatabase workDatabase) {
        Context appContext = context.getApplicationContext();
        mLifecycleOwner = lifecycleOwner;
        mConstraintsState = new ConstraintsState(constraintsStateListener);

        ConstraintsReceivers constraintsReceivers = ConstraintsReceivers.getInstance(appContext);

        mBatteryController = new ConstraintController(
                workDatabase.workSpecDao().getEnqueuedWorkSpecIdsWithBatteryConstraint(),
                mLifecycleOwner,
                constraintsReceivers.getBatteryReceiver(),
                mConstraintsState);
    }

    /**
     * Shuts down this {@link ConstraintsTracker} and removes all internal observation.
     */
    public void shutdown() {
        mBatteryController.shutdown();
    }
}
