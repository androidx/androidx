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

package android.arch.background.workmanager.impl;

import android.arch.background.workmanager.impl.model.WorkSpec;

import java.util.Set;

/**
 * Defines an internal implementation of work.
 */

public interface InternalWorkImpl {

    /**
     * @return The unique identifier associated with this unit of work
     */
    String getId();

    /**
     * @return The database {@link WorkSpec} associated with this unit of work
     */
    WorkSpec getWorkSpec();

    /**
     * @return The set of tags associated with this unit of work
     */
    Set<String> getTags();
}
