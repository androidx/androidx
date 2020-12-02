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

import android.os.Bundle
import androidx.annotation.IdRes

/**
 * Add a fragment to the associated [FragmentManager], inflating
 * the Fragment's view into the container view specified by
 * [containerViewId], to later retrieve via
 * [FragmentManager.findFragmentById].
 *
 * The new fragment to be added will be created via the
 * [FragmentFactory] of the [FragmentManager].
 *
 * @param containerViewId Identifier of the container this fragment is
 * to be placed in.
 * @param tag Optional tag name for the fragment, to later retrieve the
 * fragment with [FragmentManager.findFragmentByTag].
 * @param args Optional arguments to be set on the fragment.
 *
 * @return Returns the same [FragmentTransaction] instance.
 */
public inline fun <reified F : Fragment> FragmentTransaction.add(
    @IdRes containerViewId: Int,
    tag: String? = null,
    args: Bundle? = null
): FragmentTransaction = add(containerViewId, F::class.java, args, tag)

/**
 * Add a fragment to the associated [FragmentManager] without
 * adding the Fragment to any container view.
 *
 * The new fragment to be added will be created via the
 * [FragmentFactory] of the [FragmentManager].
 *
 * @param tag Tag name for the fragment, to later retrieve the
 * fragment with [FragmentManager.findFragmentByTag].
 * @param args Optional arguments to be set on the fragment.
 *
 * @return Returns the same [FragmentTransaction] instance.
 */
public inline fun <reified F : Fragment> FragmentTransaction.add(
    tag: String,
    args: Bundle? = null
): FragmentTransaction = add(F::class.java, args, tag)

/**
 * Replace an existing fragment that was added to a container.  This is
 * essentially the same as calling [remove] for all
 * currently added fragments that were added with the same `containerViewId`
 * and then [add] with the same arguments given here.
 *
 * The new fragment to place in the container will be created via the
 * [FragmentFactory] of the [FragmentManager].
 *
 * @param containerViewId Identifier of the container whose fragment(s) are
 * to be replaced.
 * @param tag Optional tag name for the fragment, to later retrieve the
 * fragment with [FragmentManager.findFragmentByTag].
 * @param args Optional arguments to be set on the fragment.
 *
 * @return Returns the same [FragmentTransaction] instance.
 */
public inline fun <reified F : Fragment> FragmentTransaction.replace(
    @IdRes containerViewId: Int,
    tag: String? = null,
    args: Bundle? = null
): FragmentTransaction = replace(containerViewId, F::class.java, args, tag)
