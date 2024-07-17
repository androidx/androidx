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

/*
 * Copyright (C) 2024 Google Inc.
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
package androidx.car.app.testing

import android.annotation.SuppressLint
import androidx.car.app.OnDoneCallback
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.serialization.Bundleable
import androidx.car.app.serialization.ListDelegate

/** Provides a simplified interface for invoking AIDL/delegate APIs in tests */
@SuppressLint("NullAnnotationGroup")
@ExperimentalCarApi
public object TestDelegateInvoker {
    public fun <T> ListDelegate<T>.requestAllItemsForTest(): List<T> =
        requestItemRangeForTest(0, size - 1)

    public fun <T> ListDelegate<T>.requestItemRangeForTest(
        startIndex: Int,
        endIndex: Int
    ): List<T> = runForResult {
        this@requestItemRangeForTest.requestItemRange(startIndex, endIndex, it)
    }

    private fun <T> runForResult(f: (OnDoneCallback) -> Unit): T {
        val callback = ResponseCapturingOnDoneCallback<T>()
        f(callback)

        // Generally, tests run in a single process.
        // Therefore, Host/Client AIDL logic runs synchronously.
        // Therefore, we assume the callback was fulfilled, without waiting.
        return callback.getResponseOrCrash()
    }

    /**
     * [OnDoneCallback] implementation for testing
     *
     * This class captures and stores the [Bundleable] response (if any), and unmarshalls it to the
     * specified type.
     */
    private class ResponseCapturingOnDoneCallback<TResponse> : OnDoneCallback {
        // "null" is a valid response
        private var hasResponse = false
        private var response: Bundleable? = null

        override fun onSuccess(response: Bundleable?) {
            check(!hasResponse) {
                "Callback was invoked multiple times. Please create a new callback for each API call."
            }
            hasResponse = true
            this.response = response
        }

        override fun onFailure(response: Bundleable) {
            error("OnDone callbacks should never fail in tests")
        }

        fun getResponseOrCrash(): TResponse {
            check(hasResponse) { "Callback was never invoked." }

            @Suppress("UNCHECKED_CAST") return response?.get() as TResponse
        }
    }
}
