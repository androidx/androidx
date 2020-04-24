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

package androidx.ui.focus

import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.focus.createFocusModifier

/**
 * Use this function to create an instance of [FocusModifier]. Adding a [FocusModifier] to a
 * [Composable] makes it focusable.
 */
@Composable
fun FocusModifier(): FocusModifier = remember { createFocusModifier(FocusDetailedState.Inactive) }

/**
 * This function returns the [FocusState] for the component wrapped by this [FocusModifier].
 */
val FocusModifier.focusState: FocusState get() = focusDetailedState.focusState()