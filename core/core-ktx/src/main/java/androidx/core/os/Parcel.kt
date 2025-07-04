/*
 * Copyright 2025 The Android Open Source Project
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

@file:OptIn(ExperimentalContracts::class)

package androidx.core.os

import android.os.Parcel
import kotlin.OptIn
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Executes the given [block] function on [Parcel] resource and then [Parcel.recycle].
 *
 * @param block a function to process this [Parcel] resource.
 * @return the result of [block] function invoked on [Parcel] resource.
 */
inline fun <T> Parcel.use(block: (Parcel) -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return block(this).also { recycle() }
}
