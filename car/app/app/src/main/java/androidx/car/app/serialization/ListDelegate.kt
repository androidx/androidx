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
package androidx.car.app.serialization

import android.annotation.SuppressLint
import androidx.car.app.OnDoneCallback
import androidx.car.app.annotations.ExperimentalCarApi

/**
 * A host-side interface, for querying portions of a long list.
 *
 * <p> Long lists are stored on the client for performance reasons.
 */
@ExperimentalCarApi
interface ListDelegate<out T> {
    /** The size of the underlying [List] */
    val size: Int

    /**
     * Host-side interface for requesting items in range `[startIndex, endIndex]` (both inclusive).
     *
     * The sublist is returned to the host as a [List], via [OnDoneCallback.onSuccess] on the main
     * thread.
     */
    @SuppressLint("ExecutorRegistration")
    fun requestItemRange(startIndex: Int, endIndex: Int, callback: OnDoneCallback)
}
