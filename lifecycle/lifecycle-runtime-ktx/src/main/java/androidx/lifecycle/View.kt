/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.lifecycle

import android.view.View

/**
 * Locates the [LifecycleOwner] responsible for managing this [View], if present.
 * This may be used to scope work or heavyweight resources associated with the view
 * that may span cycles of the view becoming detached and reattached from a window.
 */
@Deprecated(
    message = "Replaced by View.findViewTreeLifecycleOwner() from lifecycle module",
    replaceWith = ReplaceWith(
        "findViewTreeLifecycleOwner()",
        "androidx.lifecycle.findViewTreeLifecycleOwner"
    ),
    level = DeprecationLevel.HIDDEN
)
public fun View.findViewTreeLifecycleOwner(): LifecycleOwner? = findViewTreeLifecycleOwner()
