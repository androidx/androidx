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

package androidx.compose.ui.focus

import androidx.compose.ui.Modifier
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.ProvidableModifierLocal
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * A [modifier][Modifier.Element] that can be used to observe focus state events.
 */
interface FocusEventModifier : Modifier.Element {
    /**
     * A callback that is called whenever the focus system raises events.
     */
    fun onFocusEvent(focusState: FocusState)
}

internal class FocusEventModifierImpl(
    val onFocusEvent: (FocusState) -> Unit,
    inspectorInfo: InspectorInfo.() -> Unit
) : FocusEventModifier, ModifierLocalProvider<Boolean>, InspectorValueInfo(inspectorInfo) {
    override fun onFocusEvent(focusState: FocusState) {
        onFocusEvent.invoke(focusState)
    }
    override val key: ProvidableModifierLocal<Boolean> get() = ModifierLocalHasFocusEventListener
    override val value: Boolean get() = true
}

/**
 * Add this modifier to a component to observe focus state events.
 */
fun Modifier.onFocusEvent(onFocusEvent: (FocusState) -> Unit): Modifier {
    return this.then(
        FocusEventModifierImpl(
            onFocusEvent = onFocusEvent,
            inspectorInfo = debugInspectorInfo {
                name = "onFocusEvent"
                properties["onFocusEvent"] = onFocusEvent
            }
        )
    )
}
