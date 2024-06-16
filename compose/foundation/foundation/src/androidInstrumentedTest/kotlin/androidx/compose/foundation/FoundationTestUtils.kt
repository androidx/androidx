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

package androidx.compose.foundation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.dp

/**
 * This function adds a parent composable which has size.
 * [View.requestFocus()][android.view.View.requestFocus] will not take focus if the view has no
 * size.
 *
 * @param extraItemForInitialFocus Includes an extra item that takes focus initially. This is useful
 *   in cases where we need tests that could be affected by initial focus. Eg. When there is only
 *   one focusable item and we clear focus, that item could end up being focused on again by the
 *   initial focus logic.
 */
internal fun ComposeContentTestRule.setFocusableContent(
    extraItemForInitialFocus: Boolean = true,
    content: @Composable () -> Unit
) {
    setContent {
        if (extraItemForInitialFocus) {
            Row {
                Box(modifier = Modifier.requiredSize(10.dp, 10.dp).focusable())
                Box { content() }
            }
        } else {
            Box(modifier = Modifier.requiredSize(100.dp, 100.dp)) { content() }
        }
    }
}
