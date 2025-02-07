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

package androidx.xr.runtime.openxr

import android.os.IBinder
import androidx.xr.runtime.internal.Anchor
import androidx.xr.runtime.internal.TrackingState
import androidx.xr.runtime.math.Pose
import java.nio.ByteBuffer
import java.util.UUID

/** Wraps the native [XrSpace] with the [Anchor] interface. */
public class OpenXrAnchor
internal constructor(
    override public val nativePointer: Long,
    private val xrResources: XrResources,
    loadedUuid: UUID? = null,
) : ExportableAnchor, Updatable {

    override public val anchorToken: IBinder by lazy { nativeGetAnchorToken(nativePointer) }

    override var pose: Pose = Pose()
        private set

    override var trackingState: TrackingState = TrackingState.Paused
        private set

    override var persistenceState: Anchor.PersistenceState = Anchor.PersistenceState.NotPersisted
        private set

    override var uuid: UUID? = loadedUuid
        private set

    override fun persist() {
        if (
            persistenceState == Anchor.PersistenceState.Persisted ||
                persistenceState == Anchor.PersistenceState.Pending
        ) {
            return
        }
        val uuidBytes =
            checkNotNull(nativePersistAnchor(nativePointer)) { "Failed to persist anchor." }
        UUIDFromByteArray(uuidBytes)?.let {
            uuid = it
            persistenceState = Anchor.PersistenceState.Pending
        }
    }

    override fun detach() {
        check(nativeDestroyAnchor(nativePointer)) { "Failed to destroy anchor." }
        xrResources.removeUpdatable(this)
    }

    override fun update(xrTime: Long) {
        val anchorState: AnchorState =
            nativeGetAnchorState(nativePointer, xrTime)
                ?: throw IllegalStateException(
                    "Could not retrieve data for anchor. Is the anchor valid?"
                )

        trackingState = anchorState.trackingState
        anchorState.pose?.let { pose = it }
        if (uuid != null && persistenceState == Anchor.PersistenceState.Pending) {
            persistenceState = nativeGetPersistenceState(uuid!!)
        }
    }

    internal companion object {
        internal fun UUIDFromByteArray(bytes: ByteArray?): UUID? {
            if (bytes == null || bytes.size != 16) {
                return null
            }
            val longBytes = ByteBuffer.wrap(bytes)
            val mostSignificantBits = longBytes.long
            val leastSignificantBits = longBytes.long
            return UUID(mostSignificantBits, leastSignificantBits)
        }
    }

    private external fun nativeGetAnchorState(nativePointer: Long, timestampNs: Long): AnchorState?

    private external fun nativeGetAnchorToken(nativePointer: Long): IBinder

    private external fun nativePersistAnchor(nativePointer: Long): ByteArray?

    private external fun nativeGetPersistenceState(uuid: UUID): Anchor.PersistenceState

    private external fun nativeDestroyAnchor(nativePointer: Long): Boolean
}

internal fun Anchor.PersistenceState.Companion.fromOpenXrPersistenceState(
    value: Int
): Anchor.PersistenceState =
    when (value) {
        0 ->
            Anchor.PersistenceState
                .NotPersisted // XR_ANCHOR_PERSIST_STATE_PERSIST_NOT_REQUESTED_ANDROID
        1 -> Anchor.PersistenceState.Pending // XR_ANCHOR_PERSIST_STATE_PERSIST_PENDING_ANDROID
        2 -> Anchor.PersistenceState.Persisted // XR_ANCHOR_PERSIST_STATE_PERSISTED_ANDROID
        else -> {
            throw IllegalArgumentException("Invalid persistence state value.")
        }
    }
