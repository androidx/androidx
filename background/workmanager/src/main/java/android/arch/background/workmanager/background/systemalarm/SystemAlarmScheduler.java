/*
 * Copyright 2017 The Android Open Source Project
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

package android.arch.background.workmanager.background.systemalarm;

import static android.arch.background.workmanager.utils.PackageManagerHelper
        .isComponentExplicitlyEnabled;
import static android.arch.background.workmanager.utils.PackageManagerHelper.setComponentEnabled;

import android.app.AlarmManager;
import android.arch.background.workmanager.Scheduler;
import android.arch.background.workmanager.model.Constraints;
import android.arch.background.workmanager.model.WorkSpec;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

/**
 * A {@link Scheduler} that schedules work using {@link android.app.AlarmManager}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemAlarmScheduler implements Scheduler {
    private static final String TAG = "SystemAlarmScheduler";

    private final AlarmManager mAlarmManager;
    private final Context mAppContext;

    public SystemAlarmScheduler(Context context) {
        mAppContext = context.getApplicationContext();
        mAlarmManager = (AlarmManager) mAppContext.getSystemService(Context.ALARM_SERVICE);
    }

    @Override
    public void schedule(WorkSpec... workSpecs) {
        // TODO(xbhatnag): Initial delay and periodic work with AlarmManager
        boolean requiresBatteryNotLow = isComponentExplicitlyEnabled(
                mAppContext, ConstraintProxy.BatteryNotLowProxy.class);
        boolean requiresCharging = isComponentExplicitlyEnabled(
                mAppContext, ConstraintProxy.BatteryChargingProxy.class);

        for (WorkSpec workSpec : workSpecs) {
            Constraints constraints = workSpec.getConstraints();
            requiresCharging |= constraints.requiresCharging();
            requiresBatteryNotLow |= constraints.requiresBatteryNotLow();
        }

        if (requiresBatteryNotLow) {
            setComponentEnabled(mAppContext, ConstraintProxy.BatteryNotLowProxy.class, true);
        }

        if (requiresCharging) {
            setComponentEnabled(mAppContext, ConstraintProxy.BatteryChargingProxy.class, true);
        }
    }

    @Override
    public void cancel(@NonNull String workSpecId) {
        // TODO(janclarin): Send intent to SystemAlarmService to cancel job.
        // TODO(xbhatnag): Query WorkSpecs and disable proxies as needed.
        // TODO(xbhatnag): Cancel pending alarms via AlarmManager.
    }
}
