/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.gesture

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.actions.Action as CreationAction
import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.state.RemoteStateScope

/**
 * The hint state's `showIndicator`, passed as `onGestureAvailable` to
 * `RemoteModifier.oneHandedGesture`. It means "play a hint", which is adding 1 to the hint counter.
 *
 * Unlike other actions, it adds no operation to the document: the host adds 1 to the counter
 * itself. It only carries the counter's name, [hintCountName], which the modifier records in the
 * document for the host. Passing it to anything else, such as `RemoteModifier.clickable`, does
 * nothing.
 *
 * @property hintCountName The name of the hint counter, a named float in the `User` domain.
 */
internal class OneHandedGestureShowIndicatorAction(val hintCountName: String) : RemoteAction {

    /** Writes nothing: the host adds 1 to the counter. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRemoteAction(): CreationAction = CreationAction {}

    override fun equals(other: Any?): Boolean =
        other is OneHandedGestureShowIndicatorAction && hintCountName == other.hintCountName

    override fun hashCode(): Int = hintCountName.hashCode()

    override fun toString(): String = "OneHandedGestureShowIndicatorAction($hintCountName)"
}
