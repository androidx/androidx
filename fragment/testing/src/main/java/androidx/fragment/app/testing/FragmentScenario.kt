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

package androidx.fragment.app.testing

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory

/**
 * Launches a Fragment with given arguments hosted by an empty [FragmentActivity] using
 * given [FragmentFactory] and waits for it to reach a resumed state.
 *
 * This method cannot be called from the main thread.
 *
 * @param fragmentArgs a bundle to passed into fragment
 * @param factory a fragment factory to use or null to use default factory
 */
inline fun <reified F : Fragment> launchFragment(
    fragmentArgs: Bundle? = null,
    factory: FragmentFactory? = null
) = FragmentScenario.launch(F::class.java, fragmentArgs, factory)

/**
 * Launches a Fragment in the Activity's root view container `android.R.id.content`, with
 * given arguments hosted by an empty [FragmentActivity] and waits for it to reach a
 * resumed state.
 *
 * This method cannot be called from the main thread.
 *
 * @param fragmentArgs a bundle to passed into fragment
 * @param factory a fragment factory to use or null to use default factory
 */
inline fun <reified F : Fragment> launchFragmentInContainer(
    fragmentArgs: Bundle? = null,
    factory: FragmentFactory? = null
) = FragmentScenario.launchInContainer(F::class.java, fragmentArgs, factory)
