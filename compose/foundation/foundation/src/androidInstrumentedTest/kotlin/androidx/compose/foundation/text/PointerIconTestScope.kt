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

package androidx.compose.foundation.text

import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.google.common.truth.Truth

/** Scope for testing pointer icon is acting as expected. */
class PointerIconTestScope(val rule: ComposeContentTestRule) {
    private lateinit var view: View

    /**
     * Set the content on the rule.
     *
     * Required to call this in your test if you are using [PointerIconTestScope.assertIcon].
     */
    fun setContent(contentBlock: @Composable () -> Unit) {
        rule.setContent {
            view = LocalView.current
            // If nothing sets the pointer icon, then it can end up null in assertIcon.
            // Instead, let's just always have the top level icon be default.
            Box(Modifier.pointerHoverIcon(PointerIcon.Default)) { contentBlock() }
        }
    }

    /**
     * See the `TYPE_*` constants in [android.view.PointerIcon], such as
     * [android.view.PointerIcon.TYPE_DEFAULT].
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun assertIcon(iconType: Int) {
        rule.runOnIdle {
            val actualIcon = view.pointerIcon
            val expectedIcon = android.view.PointerIcon.getSystemIcon(view.context, iconType)
            Truth.assertThat(actualIcon).isEqualTo(expectedIcon)
        }
    }
}
