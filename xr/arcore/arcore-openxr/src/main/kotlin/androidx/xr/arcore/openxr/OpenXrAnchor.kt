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

package androidx.xr.arcore.openxr

import android.os.IBinder
import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.Anchor
import androidx.xr.arcore.runtime.ExportableAnchor
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Wraps a native `XrSpace` with the [ExportableAnchor] interface.
 *
 * @property nativePointer the native pointer to the `XrSpace` instance that backs this anchor
 * @property anchorToken an [IBinder] reference of the anchor
 * @property pose the [Pose] of the anchor
 * @property trackingState the [TrackingState] of the anchor
 * @property persistenceState the [Anchor.PersistenceState] for this anchor
 * @property uuid the [UUID] that identifies this Anchor if it is persisted
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class OpenXrAnchor
internal constructor(
    public override val nativePointer: Long,
    private val xrResources: XrResources,
    loadedUuid: UUID? = null,
) : ExportableAnchor, Updatable {

    public override val anchorToken: IBinder by lazy { nativeGetAnchorToken(nativePointer) }

    override var pose: Pose = Pose()
        private set

    override var trackingState: TrackingState = TrackingState.PAUSED
        private set

    @GuardedBy("lock")
    override var persistenceState: Anchor.PersistenceState = Anchor.PersistenceState.NOT_PERSISTED
        private set

    @GuardedBy("lock")
    override var uuid: UUID? = loadedUuid
        private set

    private val lock = ReentrantLock()

    override fun persist() {
        lock.withLock {
            if (
                persistenceState == Anchor.PersistenceState.PERSISTED ||
                    persistenceState == Anchor.PersistenceState.PENDING
            ) {
                return
            }
            val uuidBytes =
                checkNotNull(nativePersistAnchor(nativePointer)) { "Failed to persist anchor." }
            UUIDFromByteArray(uuidBytes)?.let {
                uuid = it
                persistenceState = Anchor.PersistenceState.PENDING
            }
        }
    }

    override fun detach() {
        check(nativeDestroyAnchor(nativePointer)) { "Failed to destroy anchor." }
        xrResources.removeUpdatable(this)
    }

    /**
     * Updates the entity retrieving its state at [xrTime].
     *
     * @param xrTime the number of nanoseconds since the start of the OpenXR epoch
     */
    override fun update(xrTime: Long) {
        val anchorState = nativeGetAnchorState(nativePointer, xrTime)
        if (anchorState == null) {
            trackingState = TrackingState.PAUSED
            return
        }

        trackingState = anchorState.trackingState
        anchorState.pose?.let { pose = it }
        lock.withLock {
            if (uuid != null && persistenceState == Anchor.PersistenceState.PENDING) {
                persistenceState = nativeGetPersistenceState(uuid!!)
            }
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
                .NOT_PERSISTED // XR_ANCHOR_PERSIST_STATE_PERSIST_NOT_REQUESTED_ANDROID
        1 -> Anchor.PersistenceState.PENDING // XR_ANCHOR_PERSIST_STATE_PERSIST_PENDING_ANDROID
        2 -> Anchor.PersistenceState.PERSISTED // XR_ANCHOR_PERSIST_STATE_PERSISTED_ANDROID
        else -> {
            throw IllegalArgumentException("Invalid persistence state value.")
        }
    }
