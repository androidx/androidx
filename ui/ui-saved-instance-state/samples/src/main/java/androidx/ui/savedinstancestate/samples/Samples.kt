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

@file:Suppress("UNUSED_VARIABLE")

package androidx.ui.savedinstancestate.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.ui.savedinstancestate.Saver
import androidx.ui.savedinstancestate.rememberSavedInstanceState
import androidx.ui.savedinstancestate.savedInstanceState

@Sampled
@Composable
fun SavedInstanceStateSample() {
    var value by savedInstanceState { "value" }
}

@Sampled
@Composable
fun RememberSavedInstanceStateSample() {
    val list = rememberSavedInstanceState { mutableListOf<Int>() }
}

@Sampled
@Composable
fun CustomSaverSample() {
    data class Holder(var value: Int)

    // this Saver implementation converts Holder object which we don't know how to save
    // to Int which we can save
    val HolderSaver = Saver<Holder, Int>(
        save = { it.value },
        restore = { Holder(it) }
    )
}
