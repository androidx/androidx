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

package android.arch.background.workmanager.systemalarm;

import android.app.AlarmManager;
import android.arch.background.workmanager.Scheduler;
import android.arch.background.workmanager.model.WorkSpec;
import android.content.Context;
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

    public SystemAlarmScheduler(Context context) {
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    @Override
    public void schedule(WorkSpec... workSpecs) {
        // TODO(janclarin): Schedule with AlarmManager and pass Work info to SystemAlarmService.
    }
}
