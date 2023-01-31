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

import androidx.appactions.interaction.proto.ParamValue;

import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Represents a fulfillment request coming from user tap. */
@SuppressWarnings("AutoValueImmutableFields")
@AutoValue
abstract class TouchEventUpdateRequest {

    static TouchEventUpdateRequest create(Map<String, List<ParamValue>> paramValuesMap) {
        return new AutoValue_TouchEventUpdateRequest(paramValuesMap);
    }

    /**
     * merge two TouchEventUpdateRequest instances. Map entries in newRequest will take priority in
     * case of conflict.
     */
    static TouchEventUpdateRequest merge(
            TouchEventUpdateRequest oldRequest, TouchEventUpdateRequest newRequest) {
        Map<String, List<ParamValue>> mergedParamValuesMap = new HashMap<>(
                oldRequest.paramValuesMap());
        mergedParamValuesMap.putAll(newRequest.paramValuesMap());
        return TouchEventUpdateRequest.create(Collections.unmodifiableMap(mergedParamValuesMap));
    }

    /* the param values from manual input. */
    abstract Map<String, List<ParamValue>> paramValuesMap();
}
