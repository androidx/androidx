/*
 * Copyright 2021 The Android Open Source Project
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

import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.requireView

internal actual fun DelegatableNode.isComposeRootInScrollableContainer(): Boolean {
    return requireView().isInScrollableViewGroup()
}

private fun View.isInScrollableViewGroup(): Boolean {
    var p = parent
    while (p != null && p is ViewGroup) {
        if (p.shouldDelayChildPressedState()) {
            return true
        }
        p = p.parent
    }
    return false
}

internal actual val TapIndicationDelay: Long = ViewConfiguration.getTapTimeout().toLong()
