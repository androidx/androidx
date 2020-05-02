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

package androidx.fragment.app

import android.os.Bundle
import androidx.lifecycle.LifecycleOwner

/**
 * Sets the [FragmentResultListener] for a given requestKey. Once the given [LifecycleOwner] is
 * at least in the [androidx.lifecycle.Lifecycle.State.STARTED] state, any results set by
 * [FragmentResultOwner.setFragmentResult] using the same requestKey will be delivered to the
 * [FragmentResultListener.onFragmentResult] callback. The callback will remain active until the
 * LifecycleOwner reaches the [androidx.lifecycle.Lifecycle.State.DESTROYED] state or a null
 * [FragmentResultListener] is set for the same requestKey.
 *
 * @param requestKey requestKey used to store the result
 * @param lifecycleOwner lifecycleOwner for handling the result
 * @param listener listener for result changes or `null` to remove any previously registered
 * listener.
 */
inline fun FragmentResultOwner.setFragmentResultListener(
    requestKey: String,
    lifecycleOwner: LifecycleOwner,
    crossinline listener: ((resultKey: String, bundle: Bundle) -> Unit)
) {
    setFragmentResultListener(requestKey, lifecycleOwner,
        FragmentResultListener { resultKey, bundle -> listener.invoke(resultKey, bundle) })
}
