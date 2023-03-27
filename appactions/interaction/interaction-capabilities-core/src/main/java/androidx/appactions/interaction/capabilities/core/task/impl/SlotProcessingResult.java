/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core.task.impl;

import androidx.appactions.interaction.proto.CurrentValue;

import com.google.auto.value.AutoValue;

import java.util.List;

@SuppressWarnings("AutoValueImmutableFields")
@AutoValue
abstract class SlotProcessingResult {
    static SlotProcessingResult create(Boolean isSuccessful, List<CurrentValue> processedValues) {
        return new AutoValue_SlotProcessingResult(isSuccessful, processedValues);
    }

    /**
     * Whether or not the next slot should be processed.
     *
     * <p>This is true if the following conditions were met during processing.
     *
     * <ul>
     *   <li>there are no ungroundedValues remaining (either rejected or disambig)
     *   <li>listener#onReceived returned ACCEPTED for all grounded values (which could be empty
     *   list)
     * </ul>
     */
    abstract Boolean isSuccessful();

    /** Processed CurrentValue objects. */
    abstract List<CurrentValue> processedValues();
}
