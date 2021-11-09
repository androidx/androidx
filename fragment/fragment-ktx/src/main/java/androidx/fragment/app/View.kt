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

package androidx.fragment.app

import android.view.View

/**
 * Find a [Fragment] associated with a [View].
 *
 * This method will locate the [Fragment] associated with this view. This is automatically
 * populated for the View returned by [Fragment.onCreateView] and its children.
 *
 * Calling this on a View that does not have a Fragment set will result in an
 * [IllegalStateException]
 */
public fun <F : Fragment> View.findFragment(): F = FragmentManager.findFragment(this)
