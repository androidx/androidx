/*
 * Copyright 2022 The Android Open Source Project
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

@file:JvmName("UwbClientSessionScopeRx")

package androidx.core.uwb.rxjava3

import androidx.core.uwb.RangingParameters
import androidx.core.uwb.RangingResult
import androidx.core.uwb.UwbClientSessionScope
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.rx3.asFlowable
import kotlinx.coroutines.rx3.asObservable

/**
 * Returns an [Observable] stream of [RangingResult].
 * @see UwbClientSessionScope.prepareSession
 */
public fun UwbClientSessionScope.rangingResultsObservable(parameters: RangingParameters):
    Observable<RangingResult> {
    return prepareSession(parameters).conflate().asObservable()
}

/**
 * Returns a [Flowable] of [RangingResult].
 * @see UwbClientSessionScope.prepareSession
 */
public fun UwbClientSessionScope.rangingResultsFlowable(parameters: RangingParameters):
    Flowable<RangingResult> {
    return prepareSession(parameters).conflate().asFlowable()
}
