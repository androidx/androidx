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

package androidx.savedstate

import android.view.View

/**
 * Locates the [SavedStateRegistryOwner] associated with this [View], if present.
 * This may be used to save and restore the state associated with this view.
 *
 */
@Deprecated(
    message = "Replaced by View.findViewTreeSavedStateRegistryOwner() from savedstate module",
    replaceWith = ReplaceWith(
        "findViewTreeSavedStateRegistryOwner()",
        "androidx.savedstate.findViewTreeSavedStateRegistryOwner"
    ),
    level = DeprecationLevel.HIDDEN
)
public fun findViewTreeSavedStateRegistryOwner(view: View): SavedStateRegistryOwner? =
    view.findViewTreeSavedStateRegistryOwner()
