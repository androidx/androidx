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

package androidx.compose.mpp.demo.bugs

import androidx.compose.mpp.demo.Screen
import androidx.compose.mpp.demo.bug.BackspaceIssue
import androidx.compose.mpp.demo.bug.DropdownMenuIssue
import androidx.compose.mpp.demo.bug.KeyboardIMEActionPopup

val IosBugs = Screen.Selection(
    "IosBugs",
    UIKitViewAndDropDownMenu,
    UIKitViewMatryoshka,
    KeyboardEmptyWhiteSpace,
    KeyboardPasswordType,
    DispatchersMainDelayCheck,
    StartRecompositionCheck,
    BackspaceIssue,
    DropdownMenuIssue,
    KeyboardIMEActionPopup,
    ProperContainmentDisposal,
    ComposeAndNativeScroll,
    MeasureAndLayoutCrash,
    AnimationFreezeBug,
    ModalMemoryLeak,
    ModalCrash,
    PopupStretching
)
