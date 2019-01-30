/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.baseui

import androidx.ui.core.Layout
import androidx.ui.core.Semantics
import androidx.ui.core.gesture.PressReleasedGestureDetector
import androidx.ui.core.semantics.SemanticsAction
import androidx.ui.core.semantics.SemanticsActionType
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.composer

/**
 * Combines [PressReleasedGestureDetector] and [Semantics] for the clickable
 * components like Button.
 */
@Composable
fun Clickable(
    /**
     * Will be called when user clicked on the children [Layout]
     */
    onClick: (() -> Unit)? = null,
    /**
     * Defines the enabled state.
     * The children [Layout] will not be clickable when it set to false or when [onClick] is null.
     */
    enabled: Boolean = true,
    /**
     * Should [PressReleasedGestureDetector] consume down events. You shouldn't if you have
     * some visual feedback like Ripples, as it will consume this events instead.
     */
    consumeDownOnStart: Boolean = false,
    @Children children: () -> Unit
) {
    <Semantics
        button=true
        enabled=enabled
        actions=if (enabled && onClick != null) {
            // TODO(ryanmentley): The unnecessary generic type specification works around an IR bug
            listOf<SemanticsAction<*>>(SemanticsAction(SemanticsActionType.Tap, onClick))
        } else {
            emptyList<SemanticsAction<*>>()
        }>
        <PressReleasedGestureDetector
            onRelease=if (enabled) onClick else null
            consumeDownOnStart>
            <children />
        </PressReleasedGestureDetector>
    </Semantics>
}
