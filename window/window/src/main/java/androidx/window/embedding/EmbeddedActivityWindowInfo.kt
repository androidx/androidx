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

package androidx.window.embedding

import android.graphics.Rect

/**
 * Describes the embedded window related info of an activity.
 *
 * When the activity is embedded, the [ActivityEmbeddingController.embeddedActivityWindowInfo] will
 * be invoked when any fields of [EmbeddedActivityWindowInfo] is changed. When the activity is not
 * embedded, the [ActivityEmbeddingController.embeddedActivityWindowInfo] will not be triggered
 * unless the activity is becoming non-embedded from embedded, in which case [isEmbedded] will be
 * `false`.
 *
 * @see ActivityEmbeddingController.embeddedActivityWindowInfo
 */
class EmbeddedActivityWindowInfo
internal constructor(
    /**
     * Whether this activity is embedded and its presentation may be customized by the host process
     * of the task it is associated with.
     */
    val isEmbedded: Boolean,
    /**
     * The bounds of the host container in display coordinate space, which should be the Task bounds
     * for regular embedding use case, or if the activity is not embedded.
     */
    val parentHostBounds: Rect,
    /**
     * The relative bounds of the embedded [ActivityStack] in the host container coordinate space.
     * It has the same size as [parentHostBounds] if the activity is not embedded.
     */
    val boundsInParentHost: Rect,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmbeddedActivityWindowInfo) return false

        if (isEmbedded != other.isEmbedded) return false
        if (parentHostBounds != other.parentHostBounds) return false
        if (boundsInParentHost != other.boundsInParentHost) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isEmbedded.hashCode()
        result = 31 * result + parentHostBounds.hashCode()
        result = 31 * result + boundsInParentHost.hashCode()
        return result
    }

    override fun toString(): String =
        "EmbeddedActivityWindowInfo{" +
            "isEmbedded=$isEmbedded" +
            ", parentHostBounds=$parentHostBounds" +
            ", boundsInParentHost=$boundsInParentHost" +
            "}"
}
