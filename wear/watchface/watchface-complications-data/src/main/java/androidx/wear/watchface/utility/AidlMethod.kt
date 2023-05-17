/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.watchface.utility

import android.util.Log
import androidx.annotation.RestrictTo

/**
 * Wrapper for all AIDL method implementations.
 *
 * Logs and rethrow failures.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <T> aidlMethod(classTag: String, methodName: String, block: () -> T): T {
    try {
        return block()
    } catch (e: Throwable) {
        Log.e(classTag, "$methodName failed", e)
        throw e
    }
}
