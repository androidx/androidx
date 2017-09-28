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

package android.arch.background.workmanager;

import android.arch.background.workmanager.model.Constraints;
import android.arch.background.workmanager.model.WorkSpec;

/**
 * Converts a {@link WorkSpec} into type T.
 * @param <T> The type to convert to.
 */
public interface WorkSpecConverter<T> {
    /**
     * Converts a {@link WorkSpec} into type T.
     * @param workSpec The {@link WorkSpec} to convert to type T.
     * @return The converted {@link WorkSpec} as type T.
     */
    T convert(WorkSpec workSpec);

    /**
     * Converts a {@link Constraints.NetworkType} into an appropriate int mapping.
     * @param networkType The {@link Constraints.NetworkType} to convert to an int.
     * @return The converted {@link Constraints.NetworkType} as an int.
     */
    int convertNetworkType(@Constraints.NetworkType int networkType);
}
