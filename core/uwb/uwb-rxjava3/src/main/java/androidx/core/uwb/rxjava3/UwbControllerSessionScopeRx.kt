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

@file:JvmName("UwbControllerSessionScopeRx")

package androidx.core.uwb.rxjava3

import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbControllerSessionScope
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.rxSingle

/**
 * Returns a [Single] that will run in a given block to add a controlee.
 *
 * @see UwbControllerSessionScope.addControlee
 */
public fun UwbControllerSessionScope.addControleeSingle(address: UwbAddress): Single<Unit> {
    return rxSingle { addControlee(address) }
}

/**
 * Returns a [Single] that will run in a given block to remove a controlee.
 *
 * @see UwbControllerSessionScope.removeControlee
 */
public fun UwbControllerSessionScope.removeControleeSingle(address: UwbAddress): Single<Unit> {
    return rxSingle { removeControlee(address) }
}
