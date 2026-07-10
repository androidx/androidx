/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.health.connect.client.matchmaking

import androidx.health.connect.client.ExperimentalMatchmakingApi

/**
 * Response class for matchmaking possible check.
 *
 * @property isMatchmakingPossible `true` if the flow would display at least one matching app or
 *   device, `false` otherwise.
 */
@ExperimentalMatchmakingApi
class MatchmakingResponse(val isMatchmakingPossible: Boolean) {
    override fun toString(): String =
        "MatchmakingResponse(isMatchmakingPossible=$isMatchmakingPossible)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MatchmakingResponse) return false
        if (isMatchmakingPossible != other.isMatchmakingPossible) return false
        return true
    }

    override fun hashCode(): Int = isMatchmakingPossible.hashCode()
}
