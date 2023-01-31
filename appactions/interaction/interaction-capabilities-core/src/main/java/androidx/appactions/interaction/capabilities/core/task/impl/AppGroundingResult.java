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
import androidx.appactions.interaction.proto.ParamValue;

import com.google.auto.value.AutoOneOf;

/**
 * Helper object to express if the grounding step of argument processing was successul for a single
 * ParamValue.
 */
@AutoOneOf(AppGroundingResult.Kind.class)
abstract class AppGroundingResult {
    static AppGroundingResult ofSuccess(ParamValue paramValue) {
        return AutoOneOf_AppGroundingResult.success(paramValue);
    }

    static AppGroundingResult ofFailure(CurrentValue currentValue) {
        return AutoOneOf_AppGroundingResult.failure(currentValue);
    }

    public abstract Kind getKind();

    abstract ParamValue success();

    abstract CurrentValue failure();

    enum Kind {
        SUCCESS,
        FAILURE
    }
}
