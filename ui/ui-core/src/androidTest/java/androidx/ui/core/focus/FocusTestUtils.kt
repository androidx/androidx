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
import androidx.ui.core.Modifier
import androidx.ui.foundation.Box
import androidx.ui.layout.size
import androidx.ui.test.ComposeTestRule
import androidx.ui.unit.dp

internal val FocusModifier.focusNode get() = (this as FocusModifierImpl).focusNode!!

internal var FocusModifier.focusedChild
    get() = (this as FocusModifierImpl).focusedChild
    set(value) {
        (this as FocusModifierImpl).focusedChild = value
    }

/**
 * This function adds a parent composable which has size. [View.requestFocus()][android.view.View
 * .requestFocus] will not take focus if the view has no size.
 */
internal fun ComposeTestRule.setFocusableContent(children: @Composable () -> Unit) {
    setContent {
        Box(modifier = Modifier.size(10.dp, 10.dp), children = children)
    }
}
