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

package androidx.xr.compose.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Utility extension function to fetch the current [Activity] based on the [Context] object. Will
 * throw an exception if not found.
 */
internal tailrec fun Context.requireActivity(): Activity {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.requireActivity()
        else -> error("Unexpected Context type when trying to resolve the context's Activity.")
    }
}

/**
 * Utility extension function to fetch the current [Activity] based on the [Context] object or
 * returns null if not found.
 */
internal tailrec fun Context.getActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.getActivity()
        else -> null
    }
