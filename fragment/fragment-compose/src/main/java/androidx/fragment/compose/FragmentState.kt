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

package androidx.fragment.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.fragment.app.Fragment

/**
 * Creates a [FragmentState] used to store the state of a [Fragment] that is created via
 * [AndroidFragment].
 */
@Composable
fun rememberFragmentState(): FragmentState {
    return rememberSaveable(saver = fragmentStateSaver()) { FragmentState() }
}

/** Holder for the fragment state. */
@Stable
class FragmentState(internal var state: MutableState<Fragment.SavedState?> = mutableStateOf(null))

/** Saver to save and restore the [FragmentState] across config change and process death. */
private fun fragmentStateSaver(): Saver<FragmentState, *> =
    Saver(save = { it.state }, restore = { FragmentState(it) })
