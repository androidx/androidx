/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work.test;

import android.support.annotation.NonNull;

import androidx.work.Worker;

import java.util.UUID;

/**
 * Additional functionality exposed for {@link androidx.work.WorkManager}
 * that are useful in the context of testing.
 */
public interface TestDriver {

    /**
     * Tells {@link TestDriver} to pretend that all constraints on the
     * {@link Worker} with the given {@code workSpecId} are met.
     *
     * The {@link Worker} is scheduled for execution.
     *
     * @param workSpecId is the {@link Worker}s id.
     */
    void setAllConstraintsMet(@NonNull UUID workSpecId);
}
