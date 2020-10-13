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

package androidx.navigation.fragment

import android.view.View

/**
 * Create a new [FragmentNavigator.Extras] instance with the given shared elements
 *
 * @param sharedElements One or more pairs of View+String names to be passed through to
 * [FragmentNavigator.Extras.Builder.addSharedElement].
 */
@Suppress("FunctionName")
public fun FragmentNavigatorExtras(
    vararg sharedElements: Pair<View, String>
): FragmentNavigator.Extras = FragmentNavigator.Extras.Builder().apply {
    sharedElements.forEach { (view, name) ->
        addSharedElement(view, name)
    }
}.build()
