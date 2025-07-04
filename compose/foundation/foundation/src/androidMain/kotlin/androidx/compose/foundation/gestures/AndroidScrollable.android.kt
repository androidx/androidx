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

package androidx.compose.foundation.gestures

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.requireView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFold

internal actual fun CompositionLocalConsumerModifierNode.platformScrollConfig(): ScrollConfig =
    AndroidConfig(android.view.ViewConfiguration.get(requireView().context))

internal class AndroidConfig(val viewConfiguration: android.view.ViewConfiguration) : ScrollConfig {
    // 64 dp value is taken from ViewConfiguration.java, replace with better solution

    @VisibleForTesting
    internal fun Density.getVerticalScrollFactor() =
        if (Build.VERSION.SDK_INT > 26) {
            ViewConfigurationApi26Impl.getVerticalScrollFactor(viewConfiguration)
        } else {
            64.dp.toPx()
        }

    @VisibleForTesting
    internal fun Density.getHorizontalScrollFactor() =
        if (Build.VERSION.SDK_INT > 26) {
            ViewConfigurationApi26Impl.getHorizontalScrollFactor(viewConfiguration)
        } else {
            64.dp.toPx()
        }

    override fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset {
        val verticalScrollFactor = -getVerticalScrollFactor()
        val horizontalScrollFactor = -getHorizontalScrollFactor()
        return event.changes
            .fastFold(Offset.Zero) { acc, c -> acc + c.scrollDelta }
            .let { Offset(it.x * horizontalScrollFactor, it.y * verticalScrollFactor) }
    }
}

@RequiresApi(26)
private object ViewConfigurationApi26Impl {
    fun getVerticalScrollFactor(viewConfiguration: android.view.ViewConfiguration): Float {
        return viewConfiguration.scaledVerticalScrollFactor
    }

    fun getHorizontalScrollFactor(viewConfiguration: android.view.ViewConfiguration): Float {
        return viewConfiguration.scaledHorizontalScrollFactor
    }
}
