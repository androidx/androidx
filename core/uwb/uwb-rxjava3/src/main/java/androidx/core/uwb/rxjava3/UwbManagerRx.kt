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

@file:JvmName("UwbManagerRx")

package androidx.core.uwb.rxjava3

import androidx.core.uwb.UwbClientSessionScope
import androidx.core.uwb.UwbControleeSessionScope
import androidx.core.uwb.UwbControllerSessionScope
import androidx.core.uwb.UwbManager
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.rxSingle

/**
 * Returns a [Single] of [UwbClientSessionScope].
 *
 * @see UwbManager.clientSessionScope
 */
@Suppress("DEPRECATION")
@Deprecated("Renamed to controleeSessionScopeSingle")
public fun UwbManager.clientSessionScopeSingle(): Single<UwbClientSessionScope> {
    return rxSingle { clientSessionScope() }
}

/**
 * Returns a [Single] of [UwbControleeSessionScope].
 *
 * @see UwbManager.controleeSessionScope
 */
public fun UwbManager.controleeSessionScopeSingle(): Single<UwbControleeSessionScope> {
    return rxSingle { controleeSessionScope() }
}

/**
 * Returns a [Single] of [UwbControllerSessionScope].
 *
 * @see UwbManager.controllerSessionScope
 */
public fun UwbManager.controllerSessionScopeSingle(): Single<UwbControllerSessionScope> {
    return rxSingle { controllerSessionScope() }
}
