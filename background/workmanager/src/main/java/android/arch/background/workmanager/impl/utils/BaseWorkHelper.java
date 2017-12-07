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
package android.arch.background.workmanager.impl.utils;

import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.Worker;
import android.arch.background.workmanager.impl.BaseWork;
import android.support.annotation.RestrictTo;

/**
 * A helper class for {@link BaseWork} classes.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BaseWorkHelper {

    /**
     * Converts the given {@link Worker} classes to an array of {@link Work}.
     *
     * @param workerClasses An array of {@link Worker} classes
     * @return An array of {@link Work} created with no constraints or arguments
     */
    public static Work[] convertWorkerClassArrayToWorkArray(
            Class<? extends Worker>[] workerClasses) {
        Work[] workArray = new Work[workerClasses.length];
        for (int i = 0; i < workerClasses.length; ++i) {
            workArray[i] = new Work.Builder(workerClasses[i]).build();
        }
        return workArray;
    }

    private BaseWorkHelper() {
    }
}
