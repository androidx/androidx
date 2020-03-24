/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.activity

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract

/**
 * A version of [ActivityResultCaller.prepareCall]
 * that additionally takes an input right away, producing a launcher that doesn't take any
 * additional input when called.
 *
 * @see ActivityResultCaller.prepareCall
 */
inline fun <I, O> ActivityResultCaller.prepareCall(
    contract: ActivityResultContract<I, O>,
    input: I,
    registry: ActivityResultRegistry,
    crossinline callback: (O) -> Unit
): () -> Unit {
    return { prepareCall(contract, registry) { callback(it) }.launch(input) }
}

/**
 * A version of [ActivityResultCaller.prepareCall]
 * that additionally takes an input right away, producing a launcher that doesn't take any
 * additional input when called.
 *
 * @see ActivityResultCaller.prepareCall
 */
inline fun <I, O> ActivityResultCaller.prepareCall(
    contract: ActivityResultContract<I, O>,
    input: I,
    crossinline callback: (O) -> Unit
): () -> Unit {
    return { prepareCall(contract) { callback(it) }.launch(input) }
}