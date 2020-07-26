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

package androidx.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.focus.FocusState2
import androidx.compose.ui.node.ModifiedFocusNode2

/**
 * A [Modifier.Element] that wraps makes the modifiers on the right into a Focusable. Use a
 * different instance of [FocusModifier2] for each focusable component.
 *
 * TODO(b/160923332): Rename FocusModifier2 to FocusModifier
 *
 * TODO(b/152528891): Write tests for [FocusModifier2] after we finalize on the api (API
 *  review tracked by b/152529882).
 */
@OptIn(ExperimentalFocus::class)
internal class FocusModifier2(
    initialFocus: FocusState2
) : Modifier.Element {

    var focusState: FocusState2 = initialFocus
        set(value) {
            field = value
            focusNode.wrappedBy?.propagateFocusStateChange(value)
        }

    var focusedChild: ModifiedFocusNode2? = null

    lateinit var focusNode: ModifiedFocusNode2
}

/**
 * Add this modifier to a component to make it focusable.
 */
@ExperimentalFocus
@Composable
fun Modifier.focus(): Modifier = this.then(remember { FocusModifier2(FocusState2.Inactive) })
