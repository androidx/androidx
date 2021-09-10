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

package androidx.navigation.fragment

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.navigation.NavArgs
import androidx.navigation.NavArgsLazy

/**
 * Returns a [Lazy] delegate to access the Fragment's arguments as an [Args] instance.
 *
 * It is strongly recommended that this method only be used when the Fragment is created
 * by [androidx.navigation.NavController.navigate] with the corresponding
 * [androidx.navigation.NavDirections] object, which ensures that the required
 * arguments are present.
 *
 * ```
 * class MyFragment : Fragment() {
 *     val args: MyFragmentArgs by navArgs()
 * }
 * ```
 *
 * This property can be accessed only after the Fragment's constructor.
 */
@MainThread
public inline fun <reified Args : NavArgs> Fragment.navArgs(): NavArgsLazy<Args> =
    NavArgsLazy(Args::class) {
        arguments ?: throw IllegalStateException("Fragment $this has null arguments")
    }
