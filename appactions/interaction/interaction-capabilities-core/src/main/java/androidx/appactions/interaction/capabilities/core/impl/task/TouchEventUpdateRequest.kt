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
package androidx.appactions.interaction.capabilities.core.impl.task

import androidx.appactions.interaction.proto.ParamValue

/** Represents a fulfillment request coming from user tap. */
internal data class TouchEventUpdateRequest(val paramValuesMap: Map<String, List<ParamValue>>) {
    /**
     * merge two TouchEventUpdateRequest instances. Map entries in newRequest will take priority in
     * case of conflict.
     */
    fun mergeWith(newRequest: TouchEventUpdateRequest): TouchEventUpdateRequest {
        val mergedParamValuesMap = this.paramValuesMap.toMutableMap()
        mergedParamValuesMap.putAll(newRequest.paramValuesMap)
        return TouchEventUpdateRequest(mergedParamValuesMap.toMap())
    }
}
