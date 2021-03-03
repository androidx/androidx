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

/**
 * Sets the given result for the [requestKey]. This result will be delivered to a
 * [FragmentResultListener] that is called given to [setFragmentResultListener] with the same
 * [requestKey]. If no [FragmentResultListener] with the same key is set or the Lifecycle
 * associated with the listener is not at least [androidx.lifecycle.Lifecycle.State.STARTED], the
 * result is stored until one becomes available, or [clearFragmentResult] is called with the same
 * requestKey.
 *
 * @param requestKey key used to identify the result
 * @param result the result to be passed to another fragment.
 */
public fun Fragment.setFragmentResult(requestKey: String, result: Bundle) {
    parentFragmentManager.setFragmentResult(requestKey, result)
}

/**
 * Clears the stored result for the given requestKey.
 *
 * This clears a result that was previously set a call to [setFragmentResult].
 *
 * If this is called with a requestKey that is not associated with any result, this method
 * does nothing.
 *
 * @param requestKey key used to identify the result
 */
public fun Fragment.clearFragmentResult(requestKey: String) {
    parentFragmentManager.clearFragmentResult(requestKey)
}

/**
 * Sets the [FragmentResultListener] for a given [requestKey]. Once this Fragment is
 * at least in the [androidx.lifecycle.Lifecycle.State.STARTED] state, any results set by
 * [setFragmentResult] using the same [requestKey] will be delivered to the
 * [FragmentResultListener.onFragmentResult] callback. The callback will remain active until this
 * Fragment reaches the [androidx.lifecycle.Lifecycle.State.DESTROYED] state or
 * [clearFragmentResultListener] is called with the same requestKey.
 *
 * @param requestKey requestKey used to store the result
 * @param listener listener for result changes.
 */
public fun Fragment.setFragmentResultListener(
    requestKey: String,
    listener: ((requestKey: String, bundle: Bundle) -> Unit)
) {
    parentFragmentManager.setFragmentResultListener(requestKey, this, listener)
}

/**
 * Clears the stored [FragmentResultListener] for the given requestKey.
 *
 * This clears a [FragmentResultListener] that was previously set a call to
 * [setFragmentResultListener].
 *
 * If this is called with a requestKey that is not associated with any [FragmentResultListener],
 * this method does nothing.
 *
 * @param requestKey key used to identify the result
 */
public fun Fragment.clearFragmentResultListener(requestKey: String) {
    parentFragmentManager.clearFragmentResultListener(requestKey)
}
