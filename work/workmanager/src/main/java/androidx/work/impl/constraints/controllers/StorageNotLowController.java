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
package androidx.work.impl.constraints.controllers;

import android.content.Context;
import android.support.annotation.NonNull;

import androidx.work.impl.constraints.trackers.Trackers;
import androidx.work.impl.model.WorkSpec;

/**
 * A {@link ConstraintController} for storage not low events.
 */

public class StorageNotLowController extends ConstraintController<Boolean> {
    public StorageNotLowController(Context context, OnConstraintUpdatedCallback callback) {
        super(Trackers.getInstance(context).getStorageNotLowTracker(), callback);
    }

    @Override
    boolean hasConstraint(@NonNull WorkSpec workSpec) {
        return workSpec.constraints.requiresStorageNotLow();
    }

    @Override
    boolean isConstrained(@NonNull Boolean isStorageNotLow) {
        return !isStorageNotLow;
    }
}
