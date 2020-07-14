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

package androidx.ui.core.focus

import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.Modifier

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
            // TODO(b/160923326): Propagate focus state change to all observers
            //  observing this focus modifier.
        }

    var focusedChild: ModifiedFocusNode2? = null

    lateinit var focusNode: ModifiedFocusNode2
}

/**
 * Add this modifier to a component to make it focusable.
 */
@ExperimentalFocus
@Composable
fun Modifier.focus(): Modifier = this + remember { FocusModifier2(FocusState2.Inactive) }