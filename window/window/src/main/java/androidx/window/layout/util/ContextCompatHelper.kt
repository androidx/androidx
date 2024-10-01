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
import androidx.annotation.UiContext

internal object ContextCompatHelper {
    /**
     * Given a [UiContext], check if it is a [ContextWrapper]. If so, we need to unwrap it and
     * return the actual [UiContext] within.
     */
    @UiContext
    internal fun unwrapUiContext(@UiContext context: Context): Context {
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

        // TODO(b/259148796): This code path is not needed for APIs R and above. However, that is
        //  not clear and also not enforced anywhere. Once we move to version-based implementations,
        //  this ambiguity will no longer exist. Again for clarity, on APIs before R, UiContexts are
        //  Activities or InputMethodServices, so we should never reach this point.
        throw IllegalArgumentException("Context $context is not a UiContext")
    }
}
