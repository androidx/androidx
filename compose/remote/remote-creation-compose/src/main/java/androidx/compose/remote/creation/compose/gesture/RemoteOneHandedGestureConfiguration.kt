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
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.remember
import java.util.Collections
import java.util.WeakHashMap

/**
 * A one-handed gesture action, like Wear Compose's `OneHandedGestureAction`.
 *
 * The value only identifies the action within Remote Compose, for example in generated experience
 * IDs. It is not a Wear SDK value: the host maps each action to the Wear SDK action itself, as Wear
 * Compose does. The host side, the one-handed gesture manager in `remote-material3`, uses the same
 * type.
 *
 * @property value The value that identifies the action.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
@JvmInline
public value class RemoteOneHandedGestureAction private constructor(internal val value: Int) {
    override fun toString(): String = if (this == Primary) "Primary" else "Unknown($value)"

    public companion object {
        /** The primary gesture: a double pinch. */
        public val Primary: RemoteOneHandedGestureAction = RemoteOneHandedGestureAction(1)
    }
}

/**
 * The action and experience ID of a one-handed gesture, like Wear Compose's
 * `OneHandedGestureConfiguration`. In a document, create it with
 * [rememberRemoteOneHandedGestureConfiguration]. The host side, the one-handed gesture manager in
 * `remote-material3`, uses the same type for the gestures that it reads from a document.
 *
 * @property action The gesture action.
 * @property gestureId The experience ID. The Wear SDK keeps the hint history per ID.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
public class RemoteOneHandedGestureConfiguration(
    public val action: RemoteOneHandedGestureAction,
    public val gestureId: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteOneHandedGestureConfiguration) return false

        if (action != other.action) return false
        if (gestureId != other.gestureId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = action.hashCode()
        result = 31 * result + gestureId.hashCode()
        return result
    }

    override fun toString(): String =
        "RemoteOneHandedGestureConfiguration(action=$action, gestureId=$gestureId)"
}

/**
 * Creates and remembers a [RemoteOneHandedGestureConfiguration].
 *
 * Only one configuration per action is supported in a document.
 *
 * @param action The gesture action.
 * @param gestureId The experience ID. If null, one is generated from the position of this call in
 *   the composition and from [action], as in Wear Compose. A generated ID changes when the code
 *   around the call changes, which resets the hint history. Pass an ID to keep the history.
 * @throws IllegalStateException if the document already has a configuration for [action].
 */
@Composable
@RemoteComposable
internal fun rememberRemoteOneHandedGestureConfiguration(
    action: RemoteOneHandedGestureAction,
    gestureId: String? = null,
): RemoteOneHandedGestureConfiguration {
    val compositeKeyHashCode = currentCompositeKeyHashCode
    val creationState = LocalRemoteComposeCreationState.current
    remember(creationState, action) { OneHandedGestureRegistration(creationState, action) }
    return remember(action, gestureId, compositeKeyHashCode) {
        RemoteOneHandedGestureConfiguration(
            action = action,
            gestureId = gestureId ?: generateGestureId(compositeKeyHashCode, action),
        )
    }
}

/**
 * Names of the named variables that describe a one-handed gesture in a document. The host reads
 * them after the document is loaded.
 */
internal object OneHandedGestureNames {
    /** Float: the hint counter. The host adds one to it to play a hint. */
    const val PrimaryHintCount: String = "OneHandedGestureHintCount"

    /** String: the name of the hint counter of the gesture for [action]. */
    fun hintCount(action: RemoteOneHandedGestureAction): String =
        "OneHandedGesture.$action.HintCount"

    /** String: the experience ID of the gesture for [action]. */
    fun id(action: RemoteOneHandedGestureAction): String = "OneHandedGesture.$action.Id"

    /** String: the name of the host action that the gesture for [action] runs. */
    fun hostAction(action: RemoteOneHandedGestureAction): String =
        "OneHandedGesture.$action.HostAction"

    /** String: what the gesture for [action] does, announced by TalkBack. */
    fun label(action: RemoteOneHandedGestureAction): String = "OneHandedGesture.$action.Label"

    /** Float: how long the UI must be idle, in milliseconds, before a hint for [action]. */
    fun delayMillis(action: RemoteOneHandedGestureAction): String =
        "OneHandedGesture.$action.DelayMillis"
}

/**
 * Generates an experience ID from [compositeKeyHashCode] and [action]: the hash code in base 36,
 * followed by the action value on two digits.
 */
internal fun generateGestureId(
    compositeKeyHashCode: Long,
    action: RemoteOneHandedGestureAction,
): String =
    compositeKeyHashCode.toString(Character.MAX_RADIX) + action.value.toString().padStart(2, '0')

/**
 * Registers a gesture for [action] in the document of [creationState] for as long as it is
 * remembered, so that a second gesture for the same action fails when the document is built.
 *
 * @param creationState The creation state of the document being built.
 * @param action The gesture action.
 */
private class OneHandedGestureRegistration(
    private val creationState: RemoteComposeCreationState,
    private val action: RemoteOneHandedGestureAction,
) : RememberObserver {
    init {
        synchronized(registeredActions) {
            check(registeredActions.getOrPut(creationState) { mutableSetOf() }.add(action)) {
                "Only one $action gesture hint per document"
            }
        }
    }

    override fun onRemembered() {}

    override fun onForgotten() = unregister()

    override fun onAbandoned() = unregister()

    private fun unregister() {
        synchronized(registeredActions) { registeredActions[creationState]?.remove(action) }
    }

    private companion object {
        // One entry per document being built, because several documents can be built at the same
        // time (on different threads). Weak keys, so a document that is never disposed can still
        // be garbage collected.
        val registeredActions:
            MutableMap<RemoteComposeCreationState, MutableSet<RemoteOneHandedGestureAction>> =
            Collections.synchronizedMap(WeakHashMap())
    }
}
