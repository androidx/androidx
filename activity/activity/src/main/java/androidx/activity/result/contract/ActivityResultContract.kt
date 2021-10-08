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
package androidx.activity.result.contract

import android.content.Context
import android.content.Intent

/**
 * A contract specifying that an activity can be called with an input of type [I]
 * and produce an output of type [O].
 *
 * Makes calling an activity for result type-safe.
 *
 * @see androidx.activity.result.ActivityResultCaller
 */
abstract class ActivityResultContract<I, O> {
    /**
     * Create an intent that can be used for [android.app.Activity.startActivityForResult].
     */
    abstract fun createIntent(context: Context, input: I): Intent

    /**
     * Convert result obtained from [android.app.Activity.onActivityResult] to [O].
     */
    abstract fun parseResult(resultCode: Int, intent: Intent?): O

    /**
     * An optional method you can implement that can be used to potentially provide a result in
     * lieu of starting an activity.
     *
     * @return the result wrapped in a [SynchronousResult] or `null` if the call
     * should proceed to start an activity.
     */
    open fun getSynchronousResult(context: Context, input: I): SynchronousResult<O>? {
        return null
    }

    /**
     * The wrapper for a result provided in [getSynchronousResult]. This allows differentiating
     * between a null [T] synchronous result and no synchronous result at all.
     */
    class SynchronousResult<T>(val value: T)
}
