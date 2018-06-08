/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.navigation

import android.app.Activity
import android.support.annotation.IdRes

/**
 * Find a [NavController] given the id of a View and its containing
 * [Activity].
 *
 * Calling this on a View that is not a [NavHost] or within a [NavHost]
 * will result in an [IllegalStateException]
 */
fun Activity.findNavController(@IdRes viewId: Int): NavController =
        Navigation.findNavController(this, viewId)
