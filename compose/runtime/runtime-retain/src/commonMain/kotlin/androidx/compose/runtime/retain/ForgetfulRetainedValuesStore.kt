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

package androidx.compose.runtime.retain

import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember

/**
 * The ForgetfulRetainedValuesStore is an implementation of [RetainedValuesStore] that is incapable
 * of retaining any exited values. When installed as the [LocalRetainedValuesStore], all invocations
 * of [retain] will behave like a standard [remember]. [RetainObserver] callbacks are still
 * dispatched instead of [RememberObserver] callbacks, meaning that this class will always
 * immediately [retire][RetainObserver.onRetired] a value as soon as it exits composition.
 */
public object ForgetfulRetainedValuesStore : RetainedValuesStore() {
    override fun onStartRetainingExitedValues() {
        throw UnsupportedOperationException(
            "ForgetfulRetainedValuesStore can never retain exited values."
        )
    }

    override fun onStopRetainingExitedValues() {
        // Do nothing. This implementation never retains exited values.
    }

    override fun getExitedValueOrElse(key: Any, defaultValue: Any?): Any? {
        return defaultValue
    }

    override fun saveExitingValue(key: Any, value: Any?) {
        throw UnsupportedOperationException(
            "ForgetfulRetainedValuesStore can never retain exited values."
        )
    }
}
