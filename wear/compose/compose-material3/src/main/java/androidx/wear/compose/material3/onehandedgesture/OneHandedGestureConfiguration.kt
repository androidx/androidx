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

package androidx.wear.compose.material3.onehandedgesture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.remember

/**
 * A [OneHandedGestureConfiguration] should be created for each component that supports a gesture,
 * such as a Button, Card or scrollable container.
 *
 * **Note:** It is not recommended to register multiple gestures for the same action and priority.
 * If multiple gestures are registered with identical actions and priorities, all of them will be
 * actioned simultaneously.
 *
 * @param action The gesture action to handle.
 * @param key A unique identifier for this gesture instance. This ID allows the system to track user
 *   interactions - for example, to mute gesture indicators that have been frequently shown or
 *   successfully performed, in accordance with user preferences. If the same key is reused across
 *   multiple gestures, they will share a common interaction history (such as frequency-based
 *   gesture indicator display logic). Note that this only affects the presentation of the UI; the
 *   underlying logic and handling remain independent for each instance.
 * @param priority The priority value; higher values take precedence if multiple handlers are
 *   registered for the same [action].
 * @return A remembered [OneHandedGestureConfiguration].
 */
@Composable
public fun rememberOneHandedGestureConfiguration(
    action: GestureAction,
    key: String? = null,
    priority: GesturePriority = GesturePriority.Unspecified,
): OneHandedGestureConfiguration {
    val compositeKey = currentCompositeKeyHashCode
    val finalKey =
        remember(key, compositeKey, action, priority) {
            key
                ?: (compositeKey.toString(MaxSupportedRadix) +
                    action.value.toString().padStart(2, '0') +
                    priority.value.toString().padStart(3, '0'))
        }

    return remember(action, key, priority) {
        OneHandedGestureConfiguration(action, finalKey, priority)
    }
}

/**
 * Represents the persistent specification for a one-handed gesture.
 *
 * @property action The [GestureAction] associated with this gesture specification.
 * @property key A unique identifier for this gesture instance. This ID allows the system to track
 *   user interactions - for example, to mute gesture indicators that have been frequently shown or
 *   successfully performed, in accordance with user preferences. If the same key is reused across
 *   multiple gestures, they will share a common interaction history (such as frequency-based
 *   gesture indicator display logic). Note that this only affects the presentation of the UI; the
 *   underlying logic and handling remain independent for each instance.
 * @property priority The priority value; higher values take precedence if multiple handlers are
 *   registered for the same [action]. It is not recommended to register multiple gestures for the
 *   same action and priority (but if that is the case, all of them will be actioned).
 */
@Stable
public class OneHandedGestureConfiguration(
    public val action: GestureAction,
    public val key: String,
    public val priority: GesturePriority = GesturePriority.Unspecified,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is OneHandedGestureConfiguration) return false

        if (action != other.action) return false
        if (key != other.key) return false
        if (priority != other.priority) return false

        return true
    }

    override fun hashCode(): Int {
        var result = action.hashCode()
        result = 31 * result + key.hashCode()
        result = 31 * result + priority.hashCode()
        return result
    }
}

/** The maximum radix available for conversion to and from strings. */
private const val MaxSupportedRadix = 36
