/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.focus

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.node.DelegatableNode

/**
 * This modifier node can be used to create a modifier that makes a component focusable.
 */
sealed interface FocusTargetModifierNode : DelegatableNode {
    /**
     * The [FocusState] associated with this [FocusTargetModifierNode]. When you implement a
     * [FocusTargetModifierNode], instead of implementing [FocusEventModifierNode], you can get the
     * state by accessing this variable.
     */
    @ExperimentalComposeUiApi
    val focusState: FocusState
}

/**
 * This modifier node can be used to create a modifier that makes a component focusable.
 * Use a different instance of [FocusTargetModifierNode] for each focusable component.
 */
fun FocusTargetModifierNode(): FocusTargetModifierNode = FocusTargetNode()
