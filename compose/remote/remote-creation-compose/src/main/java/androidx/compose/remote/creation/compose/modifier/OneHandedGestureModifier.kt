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

package androidx.compose.remote.creation.compose.modifier

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.action.HostAction
import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.gesture.OneHandedGestureNames
import androidx.compose.remote.creation.compose.gesture.OneHandedGestureShowIndicatorAction
import androidx.compose.remote.creation.compose.gesture.RemoteOneHandedGestureConfiguration
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.UnsupportedModifier

/** Default values for [oneHandedGesture]. */
internal object RemoteOneHandedGestureModifierDefaults {
    /**
     * How long the UI must be idle, in milliseconds, before a hint is offered. Same as Wear
     * Compose's `OneHandedGestureDefaults.ShowIndicatorDelayMillis`.
     */
    const val ShowIndicatorDelayMillis: Long = 1000L
}

/**
 * Records a one-handed gesture into the document as named variables (see [OneHandedGestureNames]):
 * the name of the hint counter, the experience ID, the name of the host action that the gesture
 * runs, the TalkBack label and the hint delay. The action is part of each name. The host reads them
 * after the document is loaded.
 *
 * Nothing is drawn and the layout is not changed.
 */
internal class OneHandedGestureModifier(
    val gestureConfiguration: RemoteOneHandedGestureConfiguration,
    val hintCountName: String,
    val hostActionName: String,
    val onGestureLabel: String,
    val showIndicatorDelayMillis: Long,
) : RemoteModifier.Element {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element {
        val action = gestureConfiguration.action
        // Global, so that the variables are written before the root and not discarded with the
        // component (b/533137513). Reading the ids writes the variables into the document.
        document.beginGlobal()
        try {
            RemoteString.createNamedRemoteString(
                    OneHandedGestureNames.hintCount(action),
                    hintCountName,
                )
                .id
            RemoteString.createNamedRemoteString(
                    OneHandedGestureNames.id(action),
                    gestureConfiguration.gestureId,
                )
                .id
            RemoteString.createNamedRemoteString(
                    OneHandedGestureNames.hostAction(action),
                    hostActionName,
                )
                .id
            RemoteString.createNamedRemoteString(
                    OneHandedGestureNames.label(action),
                    onGestureLabel,
                )
                .id
            RemoteFloat.createNamedRemoteFloat(
                    OneHandedGestureNames.delayMillis(action),
                    showIndicatorDelayMillis.toFloat(),
                )
                .id
        } finally {
            document.endGlobal()
        }
        return UnsupportedModifier(NoOpElementName)
    }

    private companion object {
        const val NoOpElementName = "OneHandedGesture"
    }
}

/**
 * Declares a one-handed gesture on this component, like Wear Compose's `Modifier.oneHandedGesture`.
 *
 * Records the gesture into the document (see [OneHandedGestureModifier]). The host waits until the
 * UI has been idle for [showIndicatorDelayMillis], asks the Wear SDK whether to show a hint, and if
 * so runs [onGestureAvailable]. When the user makes the gesture, the host runs [onGesture].
 *
 * Only one gesture per action is supported in a document, see
 * `rememberRemoteOneHandedGestureConfiguration`.
 *
 * @param gestureConfiguration The action and experience ID of the gesture.
 * @param onGestureLabel What the gesture does, announced by TalkBack, for example "start or pause
 *   the stopwatch".
 * @param showIndicatorDelayMillis How long the UI must be idle before a hint is offered.
 * @param onGestureAvailable The `showIndicator` of the hint state. It adds 1 to the hint counter.
 * @param onGesture What the gesture does. It must be a host action with a constant name, for
 *   example `hostAction("startOrPause".rs)`.
 * @throws IllegalArgumentException if [onGestureAvailable] isn't the `showIndicator` of a hint
 *   state, if [onGesture] isn't a host action with a constant name, or if
 *   [showIndicatorDelayMillis] is negative.
 */
internal fun RemoteModifier.oneHandedGesture(
    gestureConfiguration: RemoteOneHandedGestureConfiguration,
    onGestureLabel: String? = null,
    showIndicatorDelayMillis: Long =
        RemoteOneHandedGestureModifierDefaults.ShowIndicatorDelayMillis,
    onGestureAvailable: RemoteAction,
    onGesture: RemoteAction,
): RemoteModifier {
    require(onGestureAvailable is OneHandedGestureShowIndicatorAction) {
        "onGestureAvailable must be the showIndicator of a hint state"
    }
    val hostActionName =
        requireNotNull((onGesture as? HostAction)?.name?.constantValueOrNull) {
            "onGesture must be a host action with a constant name, for example " +
                "hostAction(\"name\".rs)"
        }
    require(showIndicatorDelayMillis >= 0) { "showIndicatorDelayMillis must not be negative" }
    return then(
        OneHandedGestureModifier(
            gestureConfiguration = gestureConfiguration,
            hintCountName = onGestureAvailable.hintCountName,
            hostActionName = hostActionName,
            onGestureLabel = onGestureLabel.orEmpty(),
            showIndicatorDelayMillis = showIndicatorDelayMillis,
        )
    )
}
