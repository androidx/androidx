/*
 * Copyright (C) 2024 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.platform

import android.util.Log
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.Platform

public interface RCLogger {
    public fun log(category: Platform.LogCategory, message: String)

    public object None : RCLogger {
        public override fun log(category: Platform.LogCategory, message: String) {}
    }

    public object AndroidLog : RCLogger {
        public override fun log(category: Platform.LogCategory, message: String) {
            when (category) {
                Platform.LogCategory.DEBUG -> Log.d(Tag, message)
                Platform.LogCategory.INFO -> Log.i(Tag, message)
                Platform.LogCategory.WARN -> Log.w(Tag, message)
                else -> Log.e(Tag, message)
            }
        }

        public val Tag: String = "RemoteCompose"
    }
}
