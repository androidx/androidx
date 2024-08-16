/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.core.telecom.extensions

import androidx.core.telecom.util.ExperimentalAppActions

/**
 * Provides this application with the ability to notify remote surfaces (automotive, watch, etc..)
 * when [Participant]s in the call have raised or lowered their hands.
 */
@ExperimentalAppActions
public interface RaiseHandState {
    /**
     * Notify the remote surfaces of an update to the [Participant]s that have their hands raised at
     * the current time. Any [Participant] that is in the call and is in [raisedHands] is considered
     * to have their hand raised and any [Participant] that is in the call that is not in
     * [raisedHands] is considered to have their hand lowered. The order of the [Participant]s in
     * [raisedHands] MUST be in the order that the hands were raised, earliest raised hand first.
     *
     * @param raisedHands The updated List of [Participant]s that have their hands raised, ordered
     *   as earliest raised hand to newest raised hand.
     */
    public suspend fun updateRaisedHands(raisedHands: List<Participant>)
}
