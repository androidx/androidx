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

import androidx.appactions.interaction.proto.CurrentValue
import androidx.appactions.interaction.proto.ParamValue

data class AppGroundingResult private constructor(
    val kind: Kind,
    val success: ParamValue?,
    val failure: CurrentValue?
) {
    enum class Kind {
        SUCCESS,
        FAILURE
    }

    companion object {
        @JvmStatic
        fun ofSuccess(paramValue: ParamValue) = AppGroundingResult(Kind.SUCCESS, paramValue, null)

        @JvmStatic
        fun ofFailure(currentValue: CurrentValue) =
            AppGroundingResult(Kind.FAILURE, null, currentValue)
    }
}
