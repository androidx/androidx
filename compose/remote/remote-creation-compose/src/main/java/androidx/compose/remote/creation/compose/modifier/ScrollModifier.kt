/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.modifier

import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.ScrollModifier as CoreScrollModifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.remember

/**
 * Scroll state for remote components.
 *
 * @property positionState remote float state tracking scroll position
 * @property notches The number of equally spaced snap points (notches) along the scroll range. If
 *   greater than 0, the scroll position will snap to the nearest notch when the scroll gesture
 *   ends. If 0, scrolling is continuous.
 */
public class RemoteScrollState
@RememberInComposition
constructor(public val positionState: MutableRemoteFloat, public val notches: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteScrollState) return false
        if (positionState != other.positionState) return false
        if (notches != other.notches) return false
        return true
    }

    override fun hashCode(): Int {
        var result = positionState.hashCode()
        result = 31 * result + notches
        return result
    }

    override fun toString(): String {
        return "RemoteScrollState(positionState=$positionState, notches=$notches)"
    }
}

/**
 * Creates and remembers a [RemoteScrollState].
 *
 * @param notches The number of equally spaced snap points (notches) along the scroll range. If
 *   greater than 0, the scroll position will snap to the nearest notch when the scroll gesture
 *   ends. If 0, scrolling is continuous.
 * @return remembered [RemoteScrollState]
 */
@Composable
public fun rememberRemoteScrollState(notches: Int = 0): RemoteScrollState {
    val state = LocalRemoteComposeCreationState.current
    val scrollState = remember {
        // TODO(b/520313106) - It shouldn't be writing id at this point.
        val positionId = state.document.nextId()
        val position = MutableRemoteFloat(positionId)
        RemoteScrollState(position, notches)
    }
    return scrollState
}

internal data class ScrollModifier(val direction: Int, val state: RemoteScrollState) :
    RemoteModifier.Element {

    // Not used
    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element {
        return CoreScrollModifier(direction, state.positionState.floatId, state.notches)
    }
}

/**
 * Modify element to allow it to scroll vertically when its content is larger than its constraints.
 *
 * @param state The [RemoteScrollState] that tracks and controls the scroll position. It can be
 *   created and remembered using [rememberRemoteScrollState].
 * @return The modified [RemoteModifier].
 * @sample androidx.compose.remote.creation.compose.samples.VerticalScrollSample
 * @see rememberRemoteScrollState
 */
public fun RemoteModifier.verticalScroll(state: RemoteScrollState): RemoteModifier {
    return this.then(ClipModifier()).then(ScrollModifier(CoreScrollModifier.VERTICAL, state))
}

/**
 * Modify element to allow it to scroll horizontally when its content is larger than its
 * constraints.
 *
 * @param state The [RemoteScrollState] that tracks and controls the scroll position. It can be
 *   created and remembered using [rememberRemoteScrollState].
 * @return The modified [RemoteModifier].
 * @sample androidx.compose.remote.creation.compose.samples.HorizontalScrollSample
 * @see rememberRemoteScrollState
 */
public fun RemoteModifier.horizontalScroll(state: RemoteScrollState): RemoteModifier {
    return this.then(ClipModifier()).then(ScrollModifier(CoreScrollModifier.HORIZONTAL, state))
}
