/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.window.layout.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.inputmethodservice.InputMethodService

internal object ContextCompatHelper {

    /** Return the base context from a context wrapper. */
    internal fun unwrapContext(context: Context): Context {
        var iterator = context

        while (iterator is ContextWrapper) {
            if (iterator is Activity) {
                // Activities are always ContextWrappers
                return iterator
            } else if (iterator is InputMethodService) {
                // InputMethodService are always ContextWrappers
                return iterator
            } else if (iterator.baseContext == null) {
                return iterator
            }

            iterator = iterator.baseContext
        }

        return context
    }
}
